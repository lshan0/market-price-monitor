package com.spglobal.coding.utils;

import com.spglobal.coding.utils.dto.UpdatePriceRecordRequest;
import com.spglobal.coding.utils.enums.InstrumentType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PriceRecordFactoryTest {

    @Test
    void testGeneratePriceRecordBatch() {
        List<UpdatePriceRecordRequest> updatePriceRecordRequests = UpdatePriceRecordRequestFactory.generateUpdatePriceRecordRequestBatch();
        assertEquals(10000, updatePriceRecordRequests.size()); // Verify the correct number of price records in the batch

        // Check that the batch contains both stocks and commodities
        boolean containsStock = updatePriceRecordRequests.stream().anyMatch(pr -> pr.getInstrumentType() == InstrumentType.STOCK);
        boolean containsCommodity = updatePriceRecordRequests.stream().anyMatch(pr -> pr.getInstrumentType() == InstrumentType.COMMODITIES);

        assertTrue(containsStock, "Batch should contain stock price records.");
        assertTrue(containsCommodity, "Batch should contain commodity price records.");
    }

    @Test
    void testGetRandomInstrumentId() {
        String instrumentId = UpdatePriceRecordRequestFactory.getRandomInstrumentId();
        assertNotNull(instrumentId);
        assertFalse(instrumentId.isEmpty(), "Instrument ID should not be empty.");
        assertTrue(instrumentId.equals(instrumentId.toUpperCase()), "Instrument ID should be in uppercase.");
    }

    @Test
    void testCreateRandomPriceRecord() {
        UpdatePriceRecordRequest randomUpdatePriceRecordRequest = UpdatePriceRecordRequestFactory.createRandomUpdatePriceRecordRequest();
        assertNotNull(randomUpdatePriceRecordRequest);
        assertNotNull(randomUpdatePriceRecordRequest.getRequestTime());
        assertNotNull(randomUpdatePriceRecordRequest.getInstrument());
        assertNotNull(randomUpdatePriceRecordRequest.getCurrency());
        assertNotNull(randomUpdatePriceRecordRequest.getValue());
        assertNotNull(randomUpdatePriceRecordRequest.getInstrumentType());

        // Verify that the generated asOf timestamp is within the last 30 days
        assertTrue(randomUpdatePriceRecordRequest.getRequestTime().isBefore(LocalDateTime.now()) &&
                        randomUpdatePriceRecordRequest.getRequestTime().isAfter(LocalDateTime.now().minusDays(20)),
                "The 'requestTime' time should be within the last 20 days.");
    }

    @Test
    void testGetInstrumentTypeFromId() {
        // Test stock instrument type
        String stockId = UpdatePriceRecordRequestFactory.generateIdFromInstrument("Apple Inc.");
        InstrumentType stockType = UpdatePriceRecordRequestFactory.getInstrumentTypeFromId(stockId);
        assertEquals(InstrumentType.STOCK, stockType, "Apple Inc. should return InstrumentType.STOCK.");

        // Test commodity instrument type
        String commodityId = UpdatePriceRecordRequestFactory.generateIdFromInstrument("Gold");
        InstrumentType commodityType = UpdatePriceRecordRequestFactory.getInstrumentTypeFromId(commodityId);
        assertEquals(InstrumentType.COMMODITIES, commodityType, "Gold should return InstrumentType.COMMODITIES.");

        // Test invalid instrument ID
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            UpdatePriceRecordRequestFactory.getInstrumentTypeFromId("UNKNOWN_INSTRUMENT");
        });
        assertEquals("Instrument ID not found or unknown type: UNKNOWN_INSTRUMENT", exception.getMessage());
    }
}

