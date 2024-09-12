package com.spglobal.coding.producers.dto;

import com.spglobal.coding.services.model.PriceRecord;

import java.util.List;

public record BatchProcessRequest(String batchId,
                                  List<PriceRecord> priceRecordList)
{ }
