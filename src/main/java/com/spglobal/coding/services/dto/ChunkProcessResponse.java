package com.spglobal.coding.services.dto;

import com.spglobal.coding.utils.dto.UpdatePriceRecordRequest;

import java.util.List;

public record ChunkProcessResponse(boolean isSuccess,
                                   List<UpdatePriceRecordRequest> failedRequests)
{ }
