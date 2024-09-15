package com.spglobal.coding.services;

import com.spglobal.coding.consumers.dto.GetPriceRecordsListResponse;
import com.spglobal.coding.producers.dto.ChunkProcessRequest;
import com.spglobal.coding.services.dto.ChunkProcessResponse;
import com.spglobal.coding.services.model.PriceRecord;
import com.spglobal.coding.utils.UpdatePriceRecordRequestFactory;
import com.spglobal.coding.utils.dto.UpdatePriceRecordRequest;
import com.spglobal.coding.utils.enums.InstrumentType;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

public interface PriceService {

    ChunkProcessResponse processChunk(ChunkProcessRequest chunkProcessRequest);

    void updateLatestPrice(String batchId, UpdatePriceRecordRequest updateRequest);

    Optional<PriceRecord> getPriceRecordWithRecordId(String recordId, InstrumentType instrumentType);

    Optional<PriceRecord> getPriceRecordWithRecordId(String recordId);  // No instrumentType

    Optional<PriceRecord> getPriceRecordWithInstrumentId(String instrumentId, InstrumentType instrumentType);

    Optional<PriceRecord> getPriceRecordWithInstrumentId(String instrumentId);  // No instrumentType

    List<PriceRecord> getPriceRecordsWithInstrumentType(InstrumentType instrumentType);

    GetPriceRecordsListResponse getPriceRecordsWithDuration(Duration duration);

    void clearAllPrices();

    void clearPriceForInstrumentId(String instrumentId);
}
