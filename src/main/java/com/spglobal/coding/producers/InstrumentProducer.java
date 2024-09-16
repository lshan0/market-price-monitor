package com.spglobal.coding.producers;

import com.spglobal.coding.producers.dto.BatchProcessResponse;
import com.spglobal.coding.producers.model.PriceRecordUpdateRequestBatch;
import com.spglobal.coding.utils.ChunkProcessor;
import com.spglobal.coding.utils.dto.UpdatePriceRecordRequest;
import com.spglobal.coding.utils.enums.BatchStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The InstrumentProducer class manages the lifecycle of update price record requests batches.
 * It handles operations such as starting new batches, uploading records,
 * completing batches, and cancelling them.
 * <p>
 * It uses thread-safe collections (ConcurrentHashMap) to store batch requests
 * and failed records, ensuring thread-safe operations in a multi-threaded environment.
 * The class interacts with the ChunkProcessor to handle the processing of chunks of records
 * and updates the batch status accordingly.
 */

public class InstrumentProducer implements Producer {

    private static final Logger logger = LoggerFactory.getLogger(InstrumentProducer.class);

    protected static final Map<String, PriceRecordUpdateRequestBatch> batchMap = new ConcurrentHashMap<>(); // map to pair the batchId with their records
    protected static final Map<String, List<UpdatePriceRecordRequest>> failedRequestsMap = new ConcurrentHashMap<>(); // map to store all the failed records with their batchId

    private static final String BATCH_ID_ERROR_MESSAGE_PREFIX = "Batch run with ID ";
    private static final String BATCH_NOT_FOUND_ERROR_MESSAGE_SUFFIX = " does not exist.";

    private final ChunkProcessor chunkProcessor;

    public InstrumentProducer(ChunkProcessor chunkProcessor) {
        this.chunkProcessor = chunkProcessor;
    }

    /**
     * Starts a new batch and returns a unique batch ID.
     * Throws an exception if a batch with the same ID already exists.
     *
     * @return the batch ID of the newly started batch
     */
    @Override
    public String startNewBatch() {
        final String batchId = UUID.randomUUID().toString();

        // Check if the batchId already exists before adding a new one
        batchMap.compute(batchId, (id, existingBatch) -> {
            if (existingBatch != null) {
                throw new IllegalStateException(BATCH_ID_ERROR_MESSAGE_PREFIX + batchId + " is already started.");
            }
            logger.info("Started new batch with ID: {}", batchId);
            return new PriceRecordUpdateRequestBatch();
        });

        return batchId;
    }

    /**
     * Uploads update requests to an active batch.
     * Ensures that the batch exists and is active before adding requests.
     *
     * @param batchId the ID of the batch to upload records to
     * @param requests the list of PriceRecord objects to be added to the batch
     */
    @Override
    public void uploadRequests(String batchId, List<UpdatePriceRecordRequest> requests) {
        batchMap.compute(batchId, (id, batch) -> {
            if (batch == null) {
                throw new IllegalStateException(BATCH_ID_ERROR_MESSAGE_PREFIX + batchId + BATCH_NOT_FOUND_ERROR_MESSAGE_SUFFIX);
            }
            if (batch.getStatus() != BatchStatus.STARTED) {
                throw new IllegalStateException(BATCH_ID_ERROR_MESSAGE_PREFIX + batchId + " is not active.");
            }
            batch.addAll(requests);
            batch.setStatus(BatchStatus.IN_PROGRESS);

            logger.info("Uploaded {} records to batch with ID: {}", requests.size(), batchId);
            return batch;
        });
    }

    /**
     * Processes the batch using asynchronous processing
     * The batch status is updated based on the success or failure of the batch processing.
     *
     * @param batchId the ID of the batch to complete
     */
    @Override
    public void completeBatch(String batchId) {
        batchMap.compute(batchId, (id, batch) -> {
            if (batch == null) {
                throw new IllegalArgumentException(BATCH_ID_ERROR_MESSAGE_PREFIX + batchId + BATCH_NOT_FOUND_ERROR_MESSAGE_SUFFIX);
            }
            if (batch.getStatus() != BatchStatus.IN_PROGRESS) {
                throw new IllegalStateException(BATCH_ID_ERROR_MESSAGE_PREFIX + batchId + " cannot be processed. Batch is not in progress.");
            }
            if (batch.getRequests().isEmpty()) {
                throw new IllegalStateException(BATCH_ID_ERROR_MESSAGE_PREFIX + batchId + " does not contain valid update requests.");
            }

            List<UpdatePriceRecordRequest> requests = batch.getRequests();
            CompletableFuture<BatchProcessResponse> batchProcessResponse = chunkProcessor.processBatch(batchId, requests);

            // Handle the result of chunk processing
            batchProcessResponse.thenAccept(response -> {
                if (response.isSuccess() && response.failedRequests().isEmpty()) {
                    batch.setStatus(BatchStatus.COMPLETED);
                    logger.info("Completed batch with ID: {}", batchId);
                } else {
                    batch.setStatus(BatchStatus.PROCESSED_WITH_ERRORS);
                    failedRequestsMap.put(batchId, response.failedRequests());
                    logger.info("Partially completed batch with ID: {}. Failed update requests are stored.", batchId);
                }
            }).exceptionally(ex -> {
                logger.error("Error processing batch with ID: {}. Error: {}", batchId, ex.getMessage());
                batch.setStatus(BatchStatus.FAILED);
                return null;
            });

            return batch;
        });
    }

    @Override
    public void cancelBatch(String batchId) {
        // Use compute to handle batch retrieval, status check, and update atomically
        batchMap.compute(batchId, (id, batch) -> {
            if (batch == null) {
                throw new IllegalStateException(BATCH_ID_ERROR_MESSAGE_PREFIX + batchId + BATCH_NOT_FOUND_ERROR_MESSAGE_SUFFIX);
            }

            // Update the status to CANCELLED and return the updated batch
            batch.setStatus(BatchStatus.CANCELLED);
            failedRequestsMap.put(batchId, batch.getRequests());

            logger.info("Cancelled batch with ID: {}", batchId);

            // Return the updated batch
            return batch;
        });
    }

    // Public method for testing purposes
    public PriceRecordUpdateRequestBatch getBatchById(String batchId) {
        return batchMap.get(batchId);
    }
    public static Map<String, PriceRecordUpdateRequestBatch> getBatchMap() {
        return batchMap;
    }
}
