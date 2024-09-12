package com.spglobal.coding.consumers.dto;

import com.spglobal.coding.services.model.PriceRecord;

import java.util.Optional;

public record GetPriceRecordResponse(Optional<PriceRecord> priceRecord) {
}
