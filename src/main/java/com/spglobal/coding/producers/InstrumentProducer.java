package com.spglobal.coding.producers;

import com.spglobal.coding.producers.dto.BatchCancellationResponse;
import com.spglobal.coding.producers.dto.BatchCompletionResponse;
import com.spglobal.coding.producers.dto.BatchStartResponse;
import com.spglobal.coding.producers.dto.BatchUploadResponse;
import com.spglobal.coding.utils.dto.BatchProcessResponse;
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
    public BatchStartResponse startNewBatch() {
        final String batchId = UUID.randomUUID().toString();

        // Check if the batchId already exists before adding a new one
        batchMap.compute(batchId, (id, existingBatch) -> {
            if (existingBatch != null) {
                throw new IllegalStateException(BATCH_ID_ERROR_MESSAGE_PREFIX + batchId + " is already started.");
            }
            logger.info("Started new batch with ID: {}", batchId);
            return new PriceRecordUpdateRequestBatch();
        });

        return new BatchStartResponse(batchId);
    }

    /**
     * Uploads update requests to an active batch.
     * Ensures that the batch exists and is active before adding requests.
     *
     * @param batchId the ID of the batch to upload records to
     * @param requests the list of PriceRecord objects to be added to the batch
     */
    @Override
    public BatchUploadResponse uploadRequests(String batchId, List<UpdatePriceRecordRequest> requests) {
        PriceRecordUpdateRequestBatch updatedBatch = batchMap.compute(batchId, (id, batch) -> {
            if (batch == null) {
                throw new IllegalStateException(BATCH_ID_ERROR_MESSAGE_PREFIX + batchId + BATCH_NOT_FOUND_ERROR_MESSAGE_SUFFIX);
            }
            if (batch.getStatus() != BatchStatus.STARTED && batch.getStatus() != BatchStatus.UPLOADING_REQUESTS) {
                throw new IllegalStateException(BATCH_ID_ERROR_MESSAGE_PREFIX + batchId + " is not active or already in progress.");
            }

            batch.addAll(requests);
            batch.setStatus(BatchStatus.UPLOADING_REQUESTS);

            logger.info("Uploaded {} records to batch with ID: {}", requests.size(), batchId);
            return batch;
        });

        return new BatchUploadResponse(batchId, updatedBatch.getStatus(), requests.size());
    }

    /**
     * Processes the batch using asynchronous processing
     * The batch status is updated based on the success or failure of the batch processing.
     *
     * @param batchId the ID of the batch to complete
     */
    @Override
    public BatchCompletionResponse completeBatch(String batchId) {
        batchMap.compute(batchId, (id, batch) -> {
            if (batch == null) {
                throw new IllegalArgumentException(BATCH_ID_ERROR_MESSAGE_PREFIX + batchId + BATCH_NOT_FOUND_ERROR_MESSAGE_SUFFIX);
            }
            if (batch.getStatus() != BatchStatus.UPLOADING_REQUESTS) {
                throw new IllegalStateException(BATCH_ID_ERROR_MESSAGE_PREFIX + batchId + " cannot be processed. Uploading requests is not in progress.");
            }
            if (batch.getRequests().isEmpty()) {
                throw new IllegalStateException(BATCH_ID_ERROR_MESSAGE_PREFIX + batchId + " does not contain valid update requests.");
            }

            batch.setStatus(BatchStatus.PROCESSING);
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

        return new BatchCompletionResponse(batchId, "Batch processing has started.");
    }

    /**
     * Cancels the batch with the given batch ID if it is in a cancellable state.
     *
     * <p>The requests associated with the cancelled batch are moved to the {@code failedRequestsMap},
     * and the cancellation action is logged for auditing purposes.
     *
     * @param batchId the unique identifier of the batch to cancel. Must not be {@code null}.
     */
    @Override
    public BatchCancellationResponse cancelBatch(String batchId) {
        batchMap.compute(batchId, (id, batch) -> {
            if (batch == null) {
                throw new IllegalStateException(BATCH_ID_ERROR_MESSAGE_PREFIX + batchId + BATCH_NOT_FOUND_ERROR_MESSAGE_SUFFIX);
            }

            if (batch.getStatus() != BatchStatus.STARTED && batch.getStatus() != BatchStatus.UPLOADING_REQUESTS) {
                throw new IllegalStateException(BATCH_ID_ERROR_MESSAGE_PREFIX + batchId + " cannot be cancelled as it is not in a cancellable state.");
            }

            // Update the status to CANCELLED and return the updated batch
            batch.setStatus(BatchStatus.CANCELLED);
            failedRequestsMap.put(batchId, batch.getRequests());

            logger.info("Cancelled batch with ID: {}", batchId);

            // Return the updated batch
            return batch;
        });

        return new BatchCancellationResponse(batchId, failedRequestsMap.getOrDefault(batchId, Collections.emptyList()));
    }

    // Public method for testing purposes
    public PriceRecordUpdateRequestBatch getBatchById(String batchId) {
        return batchMap.get(batchId);
    }
    public static Map<String, PriceRecordUpdateRequestBatch> getBatchMap() {
        return batchMap;
    }
}
