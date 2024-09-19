package com.spglobal.coding.utils.dto;

import java.util.List;

public record ChunkProcessRequest(String batchId,
                                  List<UpdatePriceRecordRequest> updateRequestList)
{
}
