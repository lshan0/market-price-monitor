package com.spglobal.coding.services;

import com.spglobal.coding.producers.dto.BatchProcessRequest;
import com.spglobal.coding.services.dto.BatchProcessResponse;
import com.spglobal.coding.services.model.PriceRecord;
import com.spglobal.coding.utils.enums.InstrumentType;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

public interface PriceService {

    BatchProcessResponse processChunk(BatchProcessRequest batchProcessRequest);

    void updateLatestPrice(String batchId, PriceRecord priceRecord);

    Optional<PriceRecord> getPriceRecordWithRecordId(String recordId, InstrumentType instrumentType);

    Optional<PriceRecord> getPriceRecordWithRecordId(String recordId);  // No instrumentType

    Optional<PriceRecord> getPriceRecordWithInstrumentId(String instrumentId, InstrumentType instrumentType);

    Optional<PriceRecord> getPriceRecordWithInstrumentId(String instrumentId);  // No instrumentType

    List<PriceRecord> getPriceRecordsWithInstrumentType(InstrumentType instrumentType);

    List<PriceRecord> getPriceRecordsWithDuration(Duration duration);

    void clearAllPrices();

    void clearPriceForInstrumentId(String instrumentId);
}
