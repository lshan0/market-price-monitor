package com.spglobal.coding.consumers;

import com.spglobal.coding.consumers.dto.GetPriceRecordResponse;
import com.spglobal.coding.consumers.dto.GetPriceRecordsListResponse;
import com.spglobal.coding.utils.enums.InstrumentType;

import java.time.Duration;

public interface Consumer {

    GetPriceRecordResponse getPriceRecordById(String id, InstrumentType instrumentType);

    GetPriceRecordResponse getPriceRecordById(String id);

    GetPriceRecordResponse getPriceRecordByInstrumentId(String instrumentId, InstrumentType instrumentType);

    GetPriceRecordResponse getPriceRecordByInstrumentId(String instrumentId);

    GetPriceRecordsListResponse getPriceRecordsByInstrumentType(InstrumentType instrumentType);

    GetPriceRecordsListResponse getPriceRecordsInLastDuration(Duration duration);
}
