package com.spglobal.coding.producers;

import com.spglobal.coding.utils.dto.UpdatePriceRecordRequest;

import java.util.List;

public interface Producer {

    String startNewBatch();

    void uploadRequests(String batchId, List<UpdatePriceRecordRequest> requests);

    void completeBatch(String batchId);

    void cancelBatch(String batchId);
}
