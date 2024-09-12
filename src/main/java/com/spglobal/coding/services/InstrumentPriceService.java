package com.spglobal.coding.services;

import com.spglobal.coding.producers.dto.BatchProcessRequest;
import com.spglobal.coding.services.dto.BatchProcessResponse;
import com.spglobal.coding.services.model.PriceRecord;
import com.spglobal.coding.utils.Exceptions.RecordProcessingException;
import com.spglobal.coding.utils.enums.InstrumentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class InstrumentPriceService implements PriceService {

    private static final Logger logger = LoggerFactory.getLogger(InstrumentPriceService.class);

    // Map to store latest price per instrument ID. First we're mapping InstrumentType with their values and then their Instrument ID's with their Records.
    public static final Map<InstrumentType, Map<String, PriceRecord>> latestPrices = new ConcurrentHashMap<>();

    @Override
    public BatchProcessResponse processBatch(BatchProcessRequest batchProcessRequest) {
        logger.info("Started processing for batchId {}", batchProcessRequest.batchId());
        List<PriceRecord> failedRecords = new ArrayList<>();

        for (PriceRecord record : batchProcessRequest.priceRecordList()) {
            try {
                updateLatestPrice(batchProcessRequest.batchId(), record); // Process each record in the batch
            } catch (RecordProcessingException e) {
                failedRecords.add(record); // Add the failed process to a list for future assessment
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

        // Add a new entry to the map for the received instrument key, if not already present
        InstrumentType instrumentType = priceRecord.getInstrumentType();
        Map<String, PriceRecord> priceMap = latestPrices.computeIfAbsent(instrumentType, k -> new ConcurrentHashMap<>());

        // Fetch the current latest price for the given Instrument ID
        priceMap.compute(priceRecord.getInstrumentId(), (_, currentRecord) -> {
            if (currentRecord == null || priceRecord.getAsOf().isAfter(currentRecord.getAsOf())) {
                logger.info("New record for ID: {} is more recent. Adding/Updating record.", priceRecord.getId());
                return priceRecord;
            } else {
                logger.info("Current record for ID: {} is more recent than the new record in batchId: {}", priceRecord.getId(), batchId);
                return currentRecord;
            }
        });
    }

    @Override
    public Optional<PriceRecord> getPriceRecordWithRecordId(String recordId, InstrumentType instrumentType) {
        if (instrumentType == null) {  // // If instrumentType is absent, search all maps and stream over their values
            return latestPrices.values().stream()
                    .flatMap(map -> map.values().stream())
                    .filter(record -> recordId.equals(record.getId()))
                    .findFirst();
        }
        // If instrumentType is present, get the map for that type and stream the values
        return latestPrices.getOrDefault(instrumentType, Map.of()).values()
                .stream()
                .filter(record -> recordId.equals(record.getId()))
                .findFirst();
    }

    @Override
    public Optional<PriceRecord> getPriceRecordWithRecordId(String recordId) {
        return getPriceRecordWithRecordId(recordId, null);
    }

    @Override
    public Optional<PriceRecord> getPriceRecordWithInstrumentId(String instrumentId, InstrumentType instrumentType) {
        if (instrumentType == null) { // If instrumentType is not present, search all maps for the PriceRecord
            return latestPrices.values().stream()
                    .map(priceMap -> priceMap.get(instrumentId))
                    .findFirst(); // Return the first record if any

        }
        // If instrumentType is present, get the corresponding map and look up the PriceRecord
        Map<String, PriceRecord> priceMap = latestPrices.get(instrumentType);
        return Optional.ofNullable(priceMap).map(map -> map.get(instrumentId));
    }

    @Override
    public Optional<PriceRecord> getPriceRecordWithInstrumentId(String instrumentId) {
        return getPriceRecordWithInstrumentId(instrumentId, null);
    }

    @Override
    public List<PriceRecord> getPriceRecordsWithInstrumentType(InstrumentType instrumentType) {
        return Optional.ofNullable(latestPrices.get(instrumentType))
                .map(priceMap -> priceMap.values().stream().toList()) // Convert values to a stream and collect into a list
                .orElse(List.of()); // Return an empty list if the priceMap is null
    }

    @Override
    public List<PriceRecord> getPriceRecordsWithDuration(Duration duration) {
        // Get the threshold date-time, which is the current time minus the duration
        LocalDateTime threshold = LocalDateTime.now().minus(duration);

        return latestPrices.values().stream()
                .flatMap(priceMap -> priceMap.values().stream())  // Stream all PriceRecord values
                .filter(priceRecord -> priceRecord.getAsOf().isAfter(threshold))  // Filter by duration
                .toList();
    }

    @Override
    public void clearAllPrices() {
        logger.info("Clearing all prices from memory.");
        latestPrices.clear();
    }

    @Override
    public void clearPriceForInstrumentId(String instrumentId) {
        logger.info("Clearing price for instrumentId: {}", instrumentId);

        // Iterate over the map entries to clear the price for the given instrumentId
        for (Map<String, PriceRecord> priceMap : latestPrices.values()) {
            priceMap.remove(instrumentId);
        }
    }
}
