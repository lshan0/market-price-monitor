package com.spglobal.coding.services.dto;

import com.spglobal.coding.services.model.PriceRecord;

import java.util.List;

public record BatchProcessResponse(boolean isSuccess,
                                   List<PriceRecord> failedRecords)
{ }
