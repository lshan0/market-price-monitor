package com.spglobal.coding.services;

import com.spglobal.coding.producers.dto.BatchProcessRequest;
import com.spglobal.coding.producers.dto.PriceRecordBatch;
import com.spglobal.coding.services.dto.BatchProcessResponse;
import com.spglobal.coding.services.model.PriceRecord;

import java.util.Optional;

public interface PriceService {

    BatchProcessResponse processBatch(BatchProcessRequest batchProcessRequest);

    void updateLatestPrice(String batchId, PriceRecord priceRecord);

    Optional<PriceRecord> getLatestPrice(String instrumentId);

    void clearAllPrices();

    void clearPriceForInstrumentId(String instrumentId);
}
