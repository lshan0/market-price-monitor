package com.spglobal.coding.services;

import com.spglobal.coding.producers.dto.BatchProcessRequest;
import com.spglobal.coding.services.dto.BatchProcessResponse;
import com.spglobal.coding.services.model.PriceRecord;
import com.spglobal.coding.utils.Exceptions.RecordProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InstrumentPriceService implements PriceService {

    private static final Logger logger = LoggerFactory.getLogger(InstrumentPriceService.class);

    // Map to store latest price per instrument ID
    private static final Map<String, PriceRecord> latestPrices = new ConcurrentHashMap<>();

    @Override
    public BatchProcessResponse processBatch(BatchProcessRequest batchProcessRequest) {
        logger.info("Started processing for batchId {}", batchProcessRequest.batchId());
        List<PriceRecord> failedRecords = new ArrayList<>();

        for (PriceRecord record : batchProcessRequest.priceRecordList()) {
            try {
                updateLatestPrice(batchProcessRequest.batchId(), record);
            } catch (RecordProcessingException e) {
                failedRecords.add(record);
                logger.error("Failed to process record for instrumentId: {} in batchId: {}. Error: {}", record.getId(), batchProcessRequest.batchId(), e.getMessage());
            }
        }

        logger.info("Batch processing for batchId {} completed with {} failed records.", batchProcessRequest.batchId(), failedRecords.size());
        return new BatchProcessResponse(failedRecords.isEmpty(), failedRecords);
    }

    @Override
    public void updateLatestPrice(String batchId, PriceRecord priceRecord) {
        if (priceRecord == null || priceRecord.getAsOf() == null) {
            logger.error("Received invalid Price Record in batchId {}", batchId);
            throw new RecordProcessingException("Invalid PriceRecord");
        }

        // Fetch the current latest price for the given Instrument ID
        latestPrices.compute(priceRecord.getId(), (instrumentId, currentRecord) -> {
            if (currentRecord == null || priceRecord.getAsOf().isAfter(currentRecord.getAsOf())) {
                logger.debug("New record for ID: {} is more recent. Adding/Updating record.", priceRecord.getId());
                return priceRecord;
            } else {
                logger.debug("Current record for ID: {} is more recent than the new record in batchId: {}", priceRecord.getId(), batchId);
                return currentRecord;
            }
        });
    }

    @Override
    public Optional<PriceRecord> getLatestPrice(String instrumentId) {
        return Optional.ofNullable(latestPrices.get(instrumentId));
    }

    @Override
    public void clearAllPrices() {
        logger.info("Clearing all prices from memory.");
        latestPrices.clear();
    }

    @Override
    public void clearPriceForInstrumentId(String instrumentId) {
        logger.info("Clearing price for instrumentId: {}", instrumentId);
        latestPrices.remove(instrumentId);
    }
}
