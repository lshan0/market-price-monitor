package com.spglobal.coding.producers;

import com.spglobal.coding.producers.dto.BatchCancellationResponse;
import com.spglobal.coding.producers.dto.BatchCompletionResponse;
import com.spglobal.coding.producers.dto.BatchStartResponse;
import com.spglobal.coding.producers.dto.BatchUploadResponse;
import com.spglobal.coding.utils.dto.UpdatePriceRecordRequest;

import java.util.List;

public interface Producer {

    BatchStartResponse startNewBatch();

    BatchUploadResponse uploadRequests(String batchId, List<UpdatePriceRecordRequest> requests);

    BatchCompletionResponse completeBatch(String batchId);

    BatchCancellationResponse cancelBatch(String batchId);
}
