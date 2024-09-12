package com.spglobal.coding.consumers;

import com.spglobal.coding.consumers.dto.GetPriceRecordResponse;
import com.spglobal.coding.consumers.dto.GetPriceRecordsListResponse;
import com.spglobal.coding.services.InstrumentPriceService;
import com.spglobal.coding.services.model.PriceRecord;
import com.spglobal.coding.utils.enums.InstrumentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * The InstrumentConsumer class is responsible for retrieving price records
 * using various parameters such as record IDs, instrument IDs, instrument types,
 * and time durations. It logs all relevant actions and results for auditing
 * and debugging purposes.
 */

public class InstrumentConsumer implements Consumer {

    private static final Logger logger = LoggerFactory.getLogger(InstrumentConsumer.class);

    private final InstrumentPriceService instrumentPriceService;

    /**
     * Constructor to inject the InstrumentPriceService dependency.
     *
     * @param instrumentPriceService the service responsible for handling price records
     */
    public InstrumentConsumer(InstrumentPriceService instrumentPriceService) {
        this.instrumentPriceService = instrumentPriceService;
    }

    /**
     * Retrieves the latest price record for a given record ID and optional InstrumentType.
     *
     * @param id the record ID of the price record
     * @param instrumentType the optional InstrumentType
     * @return a GetPriceRecordResponse containing the price record if found
     */
    @Override
    public GetPriceRecordResponse getPriceRecordById(String id, InstrumentType instrumentType) {
        logger.info("Fetching PriceRecord for recordId: {} with InstrumentType: {}", id, instrumentType);
        Optional<PriceRecord> priceRecord = instrumentPriceService.getPriceRecordWithRecordId(id, instrumentType);

        if (priceRecord.isEmpty()) {
            logger.warn("No PriceRecord found for recordId: {}", id);
            return new GetPriceRecordResponse(Optional.empty());
        }

        logger.info("PriceRecord found for recordId: {}", id);
        return new GetPriceRecordResponse(priceRecord);
    }

    @Override
    public GetPriceRecordResponse getPriceRecordById(String id) {
        return getPriceRecordById(id, null);
    }

    /**
     * Retrieves the latest price record for a given instrument ID and optional InstrumentType.
     *
     * @param instrumentId the instrument ID of the price record
     * @param instrumentType the optional InstrumentType
     * @return a GetPriceRecordResponse containing the price record if found
     */
    @Override
    public GetPriceRecordResponse getPriceRecordByInstrumentId(String instrumentId, InstrumentType instrumentType) {
        logger.info("Fetching PriceRecord for instrumentId: {} with InstrumentType: {}", instrumentId, instrumentType);
        Optional<PriceRecord> priceRecord = instrumentPriceService.getPriceRecordWithInstrumentId(instrumentId, instrumentType);

        if (priceRecord.isEmpty()) {
            logger.warn("No PriceRecord found for instrumentId: {}", instrumentId);
            return new GetPriceRecordResponse(Optional.empty());
        }

        logger.info("Found {} PriceRecords for instrumentId: {}", priceRecord, instrumentId);
        return new GetPriceRecordResponse(priceRecord);
    }

    // Overloaded method to retrieve price record by instrument ID without InstrumentType
    @Override
    public GetPriceRecordResponse getPriceRecordByInstrumentId(String instrumentId) {
        return getPriceRecordByInstrumentId(instrumentId, null);
    }

    /**
     * Retrieves all price records for a specific InstrumentType.
     *
     * @param instrumentType the InstrumentType to filter price records
     * @return a GetPriceRecordsListResponse containing the list of price records
     */
    @Override
    public GetPriceRecordsListResponse getPriceRecordsByInstrumentType(InstrumentType instrumentType) {
        logger.info("Fetching all PriceRecords for InstrumentType: {}", instrumentType);
        List<PriceRecord> priceRecords = instrumentPriceService.getPriceRecordsWithInstrumentType(instrumentType);

        if (priceRecords.isEmpty()) {
            logger.warn("No PriceRecords found for InstrumentType: {}", instrumentType);
        }

        logger.info("Found {} PriceRecords for InstrumentType: {}", priceRecords.size(), instrumentType);
        return new GetPriceRecordsListResponse(priceRecords);
    }

    /**
     * Fetches price records within a given duration.
     *
     * @param duration the time duration to filter price records
     * @return a GetPriceRecordsListResponse containing the list of price records found within the duration
     */
    @Override
    public GetPriceRecordsListResponse getPriceRecordsInLastDuration(Duration duration) {
        logger.info("Fetching PriceRecords within the last {} duration", duration);
        return instrumentPriceService.getPriceRecordsWithDuration(duration);
    }
}
