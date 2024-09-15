package com.spglobal.coding.producers.model;

import com.spglobal.coding.services.model.PriceRecord;
import com.spglobal.coding.utils.dto.UpdatePriceRecordRequest;
import com.spglobal.coding.utils.enums.BatchStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a batch of {@link PriceRecord} objects that are processed together.
 * <p>
 * This class manages a list of price records and tracks the status of the batch using {@link BatchStatus}.
 * The batch starts in the {@code STARTED} status and can be updated as needed.
 * New records can be added to the batch using the {@code addAll} method.
 */
public class PriceRecordUpdateRequestBatch {
    private final List<UpdatePriceRecordRequest> records;
    private BatchStatus status;

    public PriceRecordUpdateRequestBatch() {
        this.records = new ArrayList<>();
        this.status = BatchStatus.STARTED;
    }

    public List<UpdatePriceRecordRequest> getRecords() {
        return records;
    }

    public BatchStatus getStatus() {
        return status;
    }

    public void setStatus(BatchStatus status) {
        this.status = status;
    }

    public void addAll(List<UpdatePriceRecordRequest> records) {
        this.records.addAll(records);
    }
}
