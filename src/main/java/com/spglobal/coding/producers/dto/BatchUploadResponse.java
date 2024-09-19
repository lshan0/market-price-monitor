package com.spglobal.coding.producers.dto;

import com.spglobal.coding.utils.enums.BatchStatus;

public record BatchUploadResponse(String batchId,
                                  BatchStatus batchStatus,
                                  int uploadedCount)
{
}
