package com.spglobal.coding.services;

import com.spglobal.coding.consumers.dto.GetPriceRecordsListResponse;
import com.spglobal.coding.producers.dto.ChunkProcessRequest;
import com.spglobal.coding.services.dto.ChunkProcessResponse;
import com.spglobal.coding.services.model.Payload;
import com.spglobal.coding.services.model.PriceRecord;
import com.spglobal.coding.utils.dto.UpdatePriceRecordRequest;
import com.spglobal.coding.utils.exceptions.UpdateRequestProcessingException;
import com.spglobal.coding.utils.enums.InstrumentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The InstrumentPriceService class provides operations for processing and retrieving price records of financial instruments.
 * It maintains the latest price for each instrument and handles chunk processing requests.
 * <p>
 * The class uses a ConcurrentHashMap to store the latest prices for each instrument type and instrument ID, ensuring thread safety.
 * It includes methods for updating prices, fetching prices by record or instrument ID, and clearing stored price data.
 */

public class InstrumentPriceService implements PriceService {

    private static final Logger logger = LoggerFactory.getLogger(InstrumentPriceService.class);

    // First we're mapping InstrumentType with their values and then their Instrument ID's with their Records.
    protected static final Map<InstrumentType, Map<String, PriceRecord>> latestPrices = new ConcurrentHashMap<>();

    private static final int HISTORY_SIZE = 10;

    /**
     * Processes a chunk of update requests from a batch and updates the latest prices for each instrument.
     * Any requests that fail to process are logged and returned in the response.
     *
     * @param chunkProcessRequest Request containing the batch ID and list of update requests to be processed.
     * @return A response indicating whether the chunk processing was successful and containing any failed request.
     */
    @Override
    public ChunkProcessResponse processChunk(ChunkProcessRequest chunkProcessRequest) {
        List<UpdatePriceRecordRequest> failedRequests = new ArrayList<>();

        logger.info("Processing Started for {} records in chunk from batchId {}", chunkProcessRequest.updateRequestList().size(), chunkProcessRequest.batchId());
        for (UpdatePriceRecordRequest updateRequest : chunkProcessRequest.updateRequestList()) {
            try {
                updateLatestPrice(chunkProcessRequest.batchId(), updateRequest); // Process each record in the batch
            } catch (UpdateRequestProcessingException e) {
                failedRequests.add(updateRequest); // Add the failed process to a list for future assessment
                logger.error("Failed to process record for instrument: {} in batchId: {}. Error: {}",
                        updateRequest.getInstrument(), chunkProcessRequest.batchId(), e.getMessage());
            }
        }

        logger.info("Chunk processing for batchId {} completed with {} failed requests.", chunkProcessRequest.batchId(), failedRequests.size());
        return new ChunkProcessResponse(failedRequests.isEmpty(), failedRequests);
    }

    /**
     * Updates the latest price for a given price record if it is more recent than the current record.
     *
     * @param batchId     The batch ID for which the price record is being updated.
     * @param updateRequest The new price record to be updated.
     * @throws UpdateRequestProcessingException If the price record is invalid.
     */
    @Override
    public void updateLatestPrice(String batchId, UpdatePriceRecordRequest updateRequest) {
        if (updateRequest.getRequestTime() == null || updateRequest.getInstrument() == null) {
            String errorMessage = String.format("Received invalid UpdateRequest in batchId %s. RequestTime or Instrument is null.", batchId);
            logger.error(errorMessage);
            throw new UpdateRequestProcessingException(errorMessage);
        }

        InstrumentType instrumentType = updateRequest.getInstrumentType();
        Map<String, PriceRecord> priceMap = latestPrices.computeIfAbsent(instrumentType, k -> new ConcurrentHashMap<>());

        // Generate instrument ID
        String instrumentId = generateIdFromInstrument(updateRequest.getInstrument());

        priceMap.compute(instrumentId, (id, currentRecord) -> {
            Payload newPayload = new Payload(
                    updateRequest.getId(),
                    updateRequest.getValue(),
                    updateRequest.getCurrency(),
                    updateRequest.getRequestTime()
            );

            if (currentRecord == null) {
                logger.info("Creating new PriceRecord for Instrument ID: {} in batchId {}", instrumentId, batchId);
                return new PriceRecord(updateRequest.getInstrument(), instrumentId, instrumentType, updateRequest.getRequestTime(), newPayload.getValue(), newPayload);
            } else {
                logger.info("Updating existing PriceRecord for Instrument ID: {} in batchId {}", instrumentId, batchId);
                try {
                    addRequestToPayloadHistory(currentRecord, newPayload);
                    return currentRecord;
                } catch (UpdateRequestProcessingException e) {
                    logger.error("Failed to update PriceRecord for Instrument ID: {} due to outdated request. Error: {}", instrumentId, e.getMessage());
                    throw new UpdateRequestProcessingException("Outdated request for Instrument ID: " + instrumentId);
                }
            }
        });
    }

    /**
     * Adds a new payload to the price record's payload history and updates the record if the new payload
     * is the most recent.
     * @param priceRecord the {@link PriceRecord} to update with the new payload
     * @param newPayload the new {@link Payload} to add to the history and possibly update the record
     * @throws UpdateRequestProcessingException if the payload history is null
     */
    private void addRequestToPayloadHistory(PriceRecord priceRecord, Payload newPayload) {
        SortedSet<Payload> payloadHistory = priceRecord.getPayloadHistory();

        // Validate if the history exists and is not empty
        if (payloadHistory == null) {
            logger.info("Unexpected Exception: payload history is null for Instrument ID: {}", priceRecord.getInstrumentId());
            throw new UpdateRequestProcessingException("Payload history is null for an existing record");
        }

        // Check if the new payload is the most recent
        if (newPayload.getAsOf().isAfter(priceRecord.getLastUpdateTime())) {
            logger.info("New payload is the latest for Instrument ID: {}. Updating record and adding to history.", priceRecord.getInstrumentId());

            // Update the price record with the new latest payload's timestamp and value
            priceRecord.setLastUpdateTime(newPayload.getAsOf());
            priceRecord.setLatestPrice(newPayload.getValue());
        }

        payloadHistory.add(newPayload);

        // Ensure the history size is maintained (remove the oldest entry if it exceeds the limit)
        if (payloadHistory.size() > HISTORY_SIZE) {
            Payload removedPayload = payloadHistory.last();
            payloadHistory.remove(removedPayload); // Remove the oldest payload
            logger.debug("Payload history exceeded limit, removing oldest payload for Instrument ID: {}: {}", priceRecord.getInstrumentId(), removedPayload.getId());
        }

        logger.info("Successfully updated PriceRecord for Instrument ID: {}", priceRecord.getInstrumentId());
    }

    /**
     * Retrieves a PriceRecord by its unique record ID and instrument type.
     * <p>
     * If the instrument type is null, the method searches through all available instrument types.
     * If the instrument type is specified, it only searches within the map for that type.
     *
     * @param recordId      The unique ID of the PriceRecord to be retrieved. Must not be null.
     * @param instrumentType The type of the financial instrument. If null, all instrument types will be searched.
     * @return An Optional containing the PriceRecord if found, or an empty Optional if not found.
     * @throws NullPointerException if recordId is null.
     */
    @Override
    public Optional<PriceRecord> getPriceRecordWithRecordId(String recordId, InstrumentType instrumentType) {
        Objects.requireNonNull(recordId, "recordId cannot be null");

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

    /**
     * Retrieves a PriceRecord by its unique instrument ID and instrument type.
     * <p>
     * If the instrument type is null, the method searches through all available instrument types.
     * If the instrument type is specified, it only searches within the map for that type.
     *
     * @param instrumentId   The unique ID of the financial instrument. Must not be null.
     * @param instrumentType The type of the financial instrument. If null, all instrument types will be searched.
     * @return An Optional containing the PriceRecord if found, or an empty Optional if not found.
     * @throws NullPointerException if instrumentId is null.
     */
    @Override
    public Optional<PriceRecord> getPriceRecordWithInstrumentId(String instrumentId, InstrumentType instrumentType) {
        Objects.requireNonNull(instrumentId, "instrumentId cannot be null");

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

    /**
     * Retrieves all PriceRecords of a specified instrument type.
     *
     * @param instrumentType The type of the financial instrument.
     * @return A list of PriceRecords associated with the given instrument type. If no records are found, an empty list is returned.
     */
    @Override
    public List<PriceRecord> getPriceRecordsWithInstrumentType(InstrumentType instrumentType) {
        return Optional.ofNullable(latestPrices.get(instrumentType))
                .map(priceMap -> priceMap.values().stream().toList()) // Convert values to a stream and collect into a list
                .orElse(List.of()); // Return an empty list if the priceMap is null
    }

    /**
     * Retrieves a list of PriceRecords that have been updated within the specified duration.
     * <p>
     * The duration is subtracted from the current time, and only PriceRecords updated after that point are included.
     *
     * @param duration The time duration to filter the PriceRecords (e.g., past 24 hours).
     * @return A GetPriceRecordsListResponse containing a list of PriceRecords updated within the given duration.
     */
    @Override
    public GetPriceRecordsListResponse getPriceRecordsWithDuration(Duration duration) {
        // Get the threshold date-time, which is the current time minus the duration
        LocalDateTime threshold = LocalDateTime.now().minus(duration);

        final List<PriceRecord> priceRecordList = latestPrices.values().stream()
                .flatMap(priceMap -> priceMap.values().stream())  // Stream all PriceRecord values
                .filter(priceRecord -> priceRecord.getLastUpdateTime().isAfter(threshold))  // Filter by duration
                .toList();

        return new GetPriceRecordsListResponse(priceRecordList);
    }

    /**
     * Clears all stored price records from memory.
     * <p>
     * This operation will remove all price records for all instrument types.
     */
    @Override
    public void clearAllPrices() {
        logger.info("Clearing all prices from memory.");
        latestPrices.clear();
    }

    /**
     * Clears the price record for a specific instrument ID across all instrument types.
     *
     * @param instrumentId The unique ID of the instrument whose price record needs to be cleared.
     */
    @Override
    public void clearPriceForInstrumentId(String instrumentId) {
        logger.info("Clearing price for instrumentId: {}", instrumentId);

        // Iterate over the map entries to clear the price for the given instrumentId
        for (Map<String, PriceRecord> priceMap : latestPrices.values()) {
            priceMap.remove(instrumentId);
        }
    }

    // Helper method to Generate a consistent instrument ID based on the instrument name
    static String generateIdFromInstrument(final String instrumentName) {
        return instrumentName.replaceAll("\\s+", "_").toUpperCase();
    }

    public static Map<InstrumentType, Map<String, PriceRecord>> getLatestPrices() {
        return latestPrices;
    }
}
