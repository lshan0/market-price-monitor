package com.spglobal.coding.consumers;

import com.spglobal.coding.services.InstrumentPriceService;
import com.spglobal.coding.services.model.PriceRecord;
import com.spglobal.coding.utils.enums.InstrumentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

public class InstrumentConsumer implements Consumer {

    private static final Logger logger = LoggerFactory.getLogger(InstrumentConsumer.class);

    private final InstrumentPriceService instrumentPriceService;

    // Constructor to inject the InstrumentPriceService dependency
    public InstrumentConsumer(InstrumentPriceService instrumentPriceService) {
        this.instrumentPriceService = instrumentPriceService;
    }

    // Retrieves the latest price record for a given record ID and optional InstrumentType
    @Override
    public Optional<PriceRecord> getPriceRecordById(String id, InstrumentType instrumentType) {
        logger.info("Fetching PriceRecord for recordId: {} with InstrumentType: {}", id, instrumentType);
        Optional<PriceRecord> priceRecord = instrumentPriceService.getPriceRecordWithRecordId(id, instrumentType);

        if (priceRecord.isPresent()) {
            logger.info("PriceRecord found for recordId: {}", id);
        } else {
            logger.warn("No PriceRecord found for recordId: {}", id);
        }
        return priceRecord;
    }

    // Overloaded method to retrieve price record by ID without InstrumentType
    @Override
    public Optional<PriceRecord> getPriceRecordById(String id) {
        return getPriceRecordById(id, null);
    }

    // Retrieves the latest price record for a given instrument ID and optional InstrumentType
    @Override
    public Optional<PriceRecord> getPriceRecordByInstrumentId(String instrumentId, InstrumentType instrumentType) {
        logger.info("Fetching PriceRecord for instrumentId: {} with InstrumentType: {}", instrumentId, instrumentType);
        Optional<PriceRecord> priceRecord = instrumentPriceService.getPriceRecordWithInstrumentId(instrumentId, instrumentType);

        if (priceRecord.isPresent()) {
            logger.info("PriceRecord found for instrumentId: {}", instrumentId);
        } else {
            logger.warn("No PriceRecord found for instrumentId: {}", instrumentId);
        }
        return priceRecord;
    }

    // Overloaded method to retrieve price record by instrument ID without InstrumentType
    @Override
    public Optional<PriceRecord> getPriceRecordByInstrumentId(String instrumentId) {
        return getPriceRecordByInstrumentId(instrumentId, null);
    }

    // Retrieves all price records for a specific InstrumentType
    @Override
    public List<PriceRecord> getPriceRecordsByInstrumentType(InstrumentType instrumentType) {
        logger.info("Fetching all PriceRecords for InstrumentType: {}", instrumentType);
        List<PriceRecord> priceRecords = instrumentPriceService.getPriceRecordsWithInstrumentType(instrumentType);

        if (priceRecords.isEmpty()) {
            logger.warn("No PriceRecords found for InstrumentType: {}", instrumentType);
        } else {
            logger.info("Found {} PriceRecords for InstrumentType: {}", priceRecords.size(), instrumentType);
        }
        return priceRecords;
    }

    // Fetches price records within a given duration
    @Override
    public List<PriceRecord> getPriceRecordsInLastDuration(Duration duration) {
        logger.info("Fetching PriceRecords within the last {} duration", duration);
        return instrumentPriceService.getPriceRecordsWithDuration(duration);
    }
}
