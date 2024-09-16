package com.spglobal.coding.producers.dto;

import com.spglobal.coding.services.model.PriceRecord;
import com.spglobal.coding.utils.dto.UpdatePriceRecordRequest;

import java.util.List;

public record ChunkProcessRequest(String batchId,
                                  List<UpdatePriceRecordRequest> updateRequestList)
{ }
