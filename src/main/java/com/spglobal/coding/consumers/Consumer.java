package com.spglobal.coding.consumers;

import com.spglobal.coding.services.model.PriceRecord;
import com.spglobal.coding.utils.enums.InstrumentType;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

public interface Consumer {

    Optional<PriceRecord> getPriceRecordById(String id, InstrumentType instrumentType);

    Optional<PriceRecord> getPriceRecordById(String id);

    Optional<PriceRecord> getPriceRecordByInstrumentId(String instrumentId, InstrumentType instrumentType);

    Optional<PriceRecord> getPriceRecordByInstrumentId(String instrumentId);

    List<PriceRecord> getPriceRecordsByInstrumentType(InstrumentType instrumentType);

    List<PriceRecord> getPriceRecordsInLastDuration(Duration duration);
}
