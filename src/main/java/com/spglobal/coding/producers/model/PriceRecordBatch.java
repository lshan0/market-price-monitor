package com.spglobal.coding.producers.model;

import com.spglobal.coding.services.model.PriceRecord;
import com.spglobal.coding.utils.enums.BatchStatus;

import java.util.ArrayList;
import java.util.List;

public class PriceRecordBatch {
    private final List<PriceRecord> records;
    private BatchStatus status;

    public PriceRecordBatch() {
        this.records = new ArrayList<>();
        this.status = BatchStatus.STARTED;
    }

    public List<PriceRecord> getRecords() {
        return records;
    }

    public BatchStatus getStatus() {
        return status;
    }

    public void setStatus(BatchStatus status) {
        this.status = status;
    }

    public void addAll(List<PriceRecord> records) {
        this.records.addAll(records);
    }
}
