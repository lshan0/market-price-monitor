package com.spglobal.coding.utils;

import com.spglobal.coding.services.model.PriceRecord;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PriceRecordFactoryTest {

    @Test
    void testGeneratePriceRecordBatch() {
        List<PriceRecord> priceRecords = PriceRecordFactory.generatePriceRecordBatch();
        assertEquals(10000, priceRecords.size());
    }

    @Test
    void testGetRandomInstrumentId() {
        String instrumentId = PriceRecordFactory.getRandomInstrumentId();
        assertNotNull(instrumentId);
    }

    @Test
    void testCreateRandomPriceRecord() {
        PriceRecord priceRecord = PriceRecordFactory.createRandomPriceRecord();
        assertNotNull(priceRecord);
        assertNotNull(priceRecord.getId());
        assertNotNull(priceRecord.getPayload());
    }
}

