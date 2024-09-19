package com.spglobal.coding.utils.dto;

import java.util.List;

public record BatchProcessResponse(boolean isSuccess,
                                   List<UpdatePriceRecordRequest> failedRequests)
{
}
