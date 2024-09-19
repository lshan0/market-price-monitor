package com.spglobal.coding.producers.dto;

import com.spglobal.coding.utils.dto.UpdatePriceRecordRequest;

import java.util.List;

public record BatchCancellationResponse(String batchId,
                                        List<UpdatePriceRecordRequest> cancelledRequests)
{
}
