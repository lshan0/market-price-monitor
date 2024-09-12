package com.spglobal.coding.consumers.dto;

import com.spglobal.coding.services.model.PriceRecord;

import java.util.List;
import java.util.Optional;

public record GetPriceRecordsListResponse(List<PriceRecord> priceRecordList) {
}
