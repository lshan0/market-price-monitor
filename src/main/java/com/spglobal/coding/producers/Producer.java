package com.spglobal.coding.producers;

import com.spglobal.coding.services.model.PriceRecord;
import com.spglobal.coding.utils.dto.UpdatePriceRecordRequest;

import java.util.List;

public interface Producer {

    String startNewBatch();

    void uploadRecords(String batchId, List<UpdatePriceRecordRequest> records);

    void completeBatch(String batchId);

    void cancelBatch(String batchId);
}
