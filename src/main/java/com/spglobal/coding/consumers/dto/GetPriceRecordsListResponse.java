package com.spglobal.coding.consumers.dto;

import com.spglobal.coding.services.model.PriceRecord;

import java.util.List;

public record GetPriceRecordsListResponse(List<PriceRecord> priceRecordList) {
}
