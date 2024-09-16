package com.spglobal.coding.producers.model;

import com.spglobal.coding.utils.dto.UpdatePriceRecordRequest;
import com.spglobal.coding.utils.enums.BatchStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a batch of {@link UpdatePriceRecordRequest} objects that are processed together.
 * <p>
 * This class manages a list of price records and tracks the status of the batch using {@link BatchStatus}.
 * The batch starts in the {@code STARTED} status and can be updated as needed.
 * New requests can be added to the batch using the {@code addAll} method.
 */
public class PriceRecordUpdateRequestBatch {
    private final List<UpdatePriceRecordRequest> requests;
    private BatchStatus status;

    public PriceRecordUpdateRequestBatch() {
        this.requests = new ArrayList<>();
        this.status = BatchStatus.STARTED;
    }

    public List<UpdatePriceRecordRequest> getRequests() {
        return requests;
    }

    public BatchStatus getStatus() {
        return status;
    }

    public void setStatus(BatchStatus status) {
        this.status = status;
    }

    public void addAll(List<UpdatePriceRecordRequest> requests) {
        this.requests.addAll(requests);
    }
}
