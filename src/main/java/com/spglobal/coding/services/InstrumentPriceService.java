package com.spglobal.coding.services;

import com.spglobal.coding.consumers.dto.GetPriceRecordsListResponse;
import com.spglobal.coding.producers.dto.ChunkProcessRequest;
import com.spglobal.coding.services.dto.ChunkProcessResponse;
import com.spglobal.coding.services.model.PriceRecord;
import com.spglobal.coding.utils.exceptions.RecordProcessingException;
import com.spglobal.coding.utils.enums.InstrumentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class InstrumentPriceService implements PriceService {

    private static final Logger logger = LoggerFactory.getLogger(InstrumentPriceService.class);

    // Map to store latest price per instrument ID. First we're mapping InstrumentType with their values and then their Instrument ID's with their Records.
    protected static final Map<InstrumentType, Map<String, PriceRecord>> latestPrices = new ConcurrentHashMap<>();

    @Override
    public ChunkProcessResponse processChunk(ChunkProcessRequest chunkProcessRequest) {
        List<PriceRecord> failedRecords = new ArrayList<>();

        logger.info("Processing Started for {} records in chunk from batchId {}", chunkProcessRequest.priceRecordList().size(), chunkProcessRequest.batchId());
        for (PriceRecord priceRecord : chunkProcessRequest.priceRecordList()) {
            try {
                updateLatestPrice(chunkProcessRequest.batchId(), priceRecord); // Process each record in the batch
            } catch (RecordProcessingException e) {
                failedRecords.add(priceRecord); // Add the failed process to a list for future assessment
                logger.error("Failed to process record for instrumentId: {} in batchId: {}. Error: {}", priceRecord.getId(), chunkProcessRequest.batchId(), e.getMessage());
            }
        }

        logger.info("Chunk processing for batchId {} completed with {} failed records.", chunkProcessRequest.batchId(), failedRecords.size());
        return new ChunkProcessResponse(failedRecords.isEmpty(), failedRecords);
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
        priceMap.compute(priceRecord.getInstrumentId(), (id, currentRecord) -> {
            if (currentRecord == null || priceRecord.getAsOf().isAfter(currentRecord.getAsOf())) {
                logger.info("New record for Instrument ID: {} is more recent. Adding/Updating record.", priceRecord.getInstrumentId());
                return priceRecord;
            } else {
                logger.info("Current record for Instrument ID: {} is more recent than the new record in batchId: {}. Current time: {}, New time: {}",
                        priceRecord.getInstrumentId(), batchId, currentRecord.getAsOf(), priceRecord.getAsOf());
                return currentRecord;
            }
        });
    }

    @Override
    public Optional<PriceRecord> getPriceRecordWithRecordId(String recordId, InstrumentType instrumentType) {
        if (instrumentType == null) {  // // If instrumentType is absent, search all maps and stream over their values
            return latestPrices.values().stream()
                    .flatMap(map -> map.values().stream())
                    .filter(priceRecord -> recordId.equals(priceRecord.getId()))
                    .findFirst();
        }
        // If instrumentType is present, get the map for that type and stream the values
        return latestPrices.getOrDefault(instrumentType, Map.of()).values()
                .stream()
                .filter(priceRecord -> recordId.equals(priceRecord.getId()))
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
    public GetPriceRecordsListResponse getPriceRecordsWithDuration(Duration duration) {
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
