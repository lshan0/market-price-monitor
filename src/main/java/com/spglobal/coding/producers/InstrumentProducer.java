package com.spglobal.coding.producers;

import com.spglobal.coding.producers.dto.BatchProcessRequest;
import com.spglobal.coding.producers.dto.PriceRecordBatch;
import com.spglobal.coding.services.InstrumentPriceService;
import com.spglobal.coding.services.dto.BatchProcessResponse;
import com.spglobal.coding.services.model.PriceRecord;
import com.spglobal.coding.utils.enums.BatchStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class InstrumentProducer implements Producer {

    private static final Logger logger = LoggerFactory.getLogger(InstrumentProducer.class);

    private static final Map<String, PriceRecordBatch> batchMap = new ConcurrentHashMap<>(); // map to pair the batchId with their records

    private static final Map<String, List<PriceRecord>> failedRecordsMap = new ConcurrentHashMap<>(); // map to store all the failed records with their batchId

    InstrumentPriceService instrumentPriceService;

    // TODO: Implement IOC using dependency injection to handle services
    public InstrumentProducer(InstrumentPriceService instrumentPriceService) {
        this.instrumentPriceService = instrumentPriceService;
    }

    // Starts a new batch and returns the unique batch ID.
    @Override
    public String startNewBatch() {
        final String batchId = UUID.randomUUID().toString();

        // Check if the batchId already exists before adding a new one
        batchMap.compute(batchId, (key, existingBatch) -> {
            if (existingBatch != null) {
                throw new IllegalStateException("Batch run with ID " + batchId + " is already started.");
            }
            logger.info("Started new batch with ID: {}", batchId);
            return new PriceRecordBatch();
        });

        return batchId;
    }

    // Uploads records to an active batch. Ensures that the batch exists and is active.
    @Override
    public void uploadRecords(String batchId, List<PriceRecord> records) {
        batchMap.compute(batchId, (id, batch) -> {
            if (batch == null) {
                throw new IllegalStateException("Batch run with ID " + batchId + " does not exist.");
            }
            if (records.size() > 1000) {
                throw new IllegalArgumentException("Chunk size exceeds 1000 records.");
            }
            if (batch.getStatus() != BatchStatus.STARTED) {
                throw new IllegalStateException("Batch run with ID " + batchId + " is not active.");
            }
            batch.addAll(records);
            batch.setStatus(BatchStatus.IN_PROGRESS);
            logger.info("Uploaded {} records to batch with ID: {}", records.size(), batchId);
            return batch;
        });
    }

    // Completes the batch and processes the records. Removes the batch from the active list after completion.
    @Override
    public void completeBatch(String batchId) {
        batchMap.compute(batchId, (id, batch) -> {
            if (batch == null) {
                throw new IllegalArgumentException("Batch run with ID " + batchId + " does not exist.");
            }
            if (batch.getStatus() != BatchStatus.IN_PROGRESS) {
                throw new IllegalStateException("Batch run with ID " + batchId + " cannot be processed. Batch is not in progress.");
            }
            if (batch.getRecords().isEmpty()) {
                throw new IllegalStateException("Batch run with ID " + batchId + " does not contain valid price records.");
            }

            BatchProcessResponse response = instrumentPriceService.processBatch(new BatchProcessRequest(batchId, batch.getRecords()));

            if (response.isSuccess() && response.failedRecords().isEmpty()) {
                batch.setStatus(BatchStatus.COMPLETED);
                logger.info("Completed batch with ID: {}", batchId);
            } else {
                batch.setStatus(BatchStatus.PROCESSED_WITH_ERRORS);
                failedRecordsMap.put(batchId, response.failedRecords());
                logger.info("Partially completed batch with ID: {}. Failed price records are stored", batchId);
            }
            return batch;
        });
    }

    // Cancels the batch and discards the records.
    @Override
    public void cancelBatch(String batchId) {
        if (!batchMap.containsKey(batchId)) {
            throw new IllegalStateException("Batch run with ID " + batchId + " does not exist.");
        }

        // Check if the batch status is IN_PROGRESS
        PriceRecordBatch batch = batchMap.get(batchId);
        if (batch.getStatus() != BatchStatus.IN_PROGRESS) {
            throw new IllegalStateException("Batch run with ID " + batchId + " cannot be cancelled as it is not in progress.");
        }

        batchMap.remove(batchId);
        logger.info("Cancelled batch with ID: {}", batchId);
    }
}
