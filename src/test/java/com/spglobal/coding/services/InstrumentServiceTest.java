package com.spglobal.coding.services;

import com.spglobal.coding.consumers.dto.GetPriceRecordsListResponse;
import com.spglobal.coding.producers.dto.ChunkProcessRequest;
import com.spglobal.coding.services.dto.ChunkProcessResponse;
import com.spglobal.coding.services.model.PriceRecord;
import com.spglobal.coding.utils.dto.UpdatePriceRecordRequest;
import com.spglobal.coding.utils.enums.Currency;
import com.spglobal.coding.utils.exceptions.UpdateRequestProcessingException;
import com.spglobal.coding.utils.enums.InstrumentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class InstrumentServiceTest {

    @InjectMocks
    private InstrumentPriceService instrumentPriceService;

    @Mock
    private Logger logger;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // Reset static fields if needed (e.g., latestPrices)
        // Note: If you're using an IDE, sometimes you may need to use reflection to reset static fields for tests.
    }

    @Test
    void testProcessChunkSuccess() {
        String batchId = "batch123";

        // Create UpdatePriceRecordRequest objects
        UpdatePriceRecordRequest updateRequest1 = new UpdatePriceRecordRequest.Builder()
                .setId(1)
                .setInstrument("instrument1")
                .setInstrumentType(InstrumentType.STOCK)
                .setValue(BigDecimal.valueOf(100))
                .setCurrency(Currency.USD)
                .setRequestTime(LocalDateTime.now())
                .build();

        UpdatePriceRecordRequest updateRequest2 = new UpdatePriceRecordRequest.Builder()
                .setId(2)
                .setInstrument("instrument2")
                .setInstrumentType(InstrumentType.BOND)
                .setValue(BigDecimal.valueOf(200))
                .setCurrency(Currency.USD)
                .setRequestTime(LocalDateTime.now())
                .build();

        ChunkProcessRequest request = new ChunkProcessRequest(batchId, List.of(updateRequest1, updateRequest2));

        // Act
        ChunkProcessResponse response = instrumentPriceService.processChunk(request);

        // Assert
        assertTrue(response.isSuccess());
        assertTrue(response.failedRecords().isEmpty());
        assertNotNull(InstrumentPriceService.latestPrices.get(InstrumentType.STOCK));
        assertNotNull(InstrumentPriceService.latestPrices.get(InstrumentType.BOND));
    }

    @Test
    void testProcessChunkFailure() {
        String batchId = "batch123";

        // Create an invalid UpdatePriceRecordRequest with null or invalid values
        UpdatePriceRecordRequest invalidRequest = new UpdatePriceRecordRequest.Builder()
                .setId(0)
                .setInstrument(null)
                .setInstrumentType(null)
                .setValue(null)
                .setCurrency(null)
                .setRequestTime(null)
                .build();

        // Create the ChunkProcessRequest with the invalid request
        ChunkProcessRequest request = new ChunkProcessRequest(batchId, List.of(invalidRequest));
        ChunkProcessResponse response = instrumentPriceService.processChunk(request);

        // Assert
        assertFalse(response.isSuccess());
        assertEquals(1, response.failedRecords().size());
        assertEquals(invalidRequest, response.failedRecords().get(0));
    }


    @Test
    void testUpdateLatestPriceWithValidUpdateRequest() {
        UpdatePriceRecordRequest updateRequest = new UpdatePriceRecordRequest.Builder()
                .setId(1)
                .setInstrument("instrument1")
                .setInstrumentType(InstrumentType.STOCK)
                .setValue(BigDecimal.valueOf(100))
                .setCurrency(Currency.USD)
                .setRequestTime(LocalDateTime.now())
                .build();

        instrumentPriceService.updateLatestPrice("batch123", updateRequest);

        Map<String, PriceRecord> priceMap = InstrumentPriceService.latestPrices.get(InstrumentType.STOCK);
        assertNotNull(priceMap);

        String generatedInstrumentId = InstrumentPriceService.generateIdFromInstrument(updateRequest.getInstrument());
        PriceRecord updatedPriceRecord = priceMap.get(generatedInstrumentId);

        assertNotNull(updatedPriceRecord); // Ensure the record is updated
        assertEquals(updateRequest.getInstrument(), updatedPriceRecord.getInstrument());
        assertEquals(updateRequest.getValue(), updatedPriceRecord.getPayloadHistory().first().getValue());
        assertEquals(updateRequest.getCurrency(), updatedPriceRecord.getPayloadHistory().first().getCurrency());
        assertEquals(updateRequest.getRequestTime(), updatedPriceRecord.getPayloadHistory().first().getAsOf());
    }


    @Test
    void testUpdateLatestPriceWithInvalidUpdateRequest() {
        UpdatePriceRecordRequest updateRequest = new UpdatePriceRecordRequest.Builder()
                .setId(1)
                .setInstrument(null)
                .setInstrumentType(null)
                .setValue(null)
                .setCurrency(null)
                .setRequestTime(null)
                .build();

        assertThrows(UpdateRequestProcessingException.class, () -> {
            instrumentPriceService.updateLatestPrice("batch123", updateRequest);
        });
    }

    @Test
    void testGetPriceRecordWithRecordId() {
        UpdatePriceRecordRequest updateRequest = new UpdatePriceRecordRequest.Builder()
                .setId(1)
                .setInstrument("Silver")
                .setInstrumentType(InstrumentType.STOCK)
                .setValue(BigDecimal.valueOf(100))
                .setCurrency(Currency.USD)
                .setRequestTime(LocalDateTime.now())
                .build();

        instrumentPriceService.updateLatestPrice("batch123", updateRequest);

        Map<InstrumentType, Map<String, PriceRecord>> latestPrices = InstrumentPriceService.getLatestPrices();
        String recordId = latestPrices.get(InstrumentType.STOCK).get("SILVER").getId();

        Optional<PriceRecord> result1 = instrumentPriceService.getPriceRecordWithRecordId(recordId, InstrumentType.STOCK);
        Optional<PriceRecord> result2 = instrumentPriceService.getPriceRecordWithRecordId(recordId, null);
        Optional<PriceRecord> invalidResult = instrumentPriceService.getPriceRecordWithRecordId(recordId + "test", InstrumentType.BOND);

        assertTrue(result1.isPresent());
        assertTrue(result2.isPresent());
        assertTrue(invalidResult.isEmpty());

        assertEquals(updateRequest.getInstrument(), result1.get().getInstrument());
        assertEquals(updateRequest.getInstrument(), result2.get().getInstrument());
    }

    @Test
    void testGetPriceRecordWithInstrumentId() {
        UpdatePriceRecordRequest updateRequest = new UpdatePriceRecordRequest.Builder()
                .setId(1)
                .setInstrument("Silver")
                .setInstrumentType(InstrumentType.COMMODITIES)
                .setValue(BigDecimal.valueOf(100))
                .setCurrency(Currency.USD)
                .setRequestTime(LocalDateTime.now())
                .build();

        instrumentPriceService.clearAllPrices();
        instrumentPriceService.updateLatestPrice("batch123", updateRequest);

        Optional<PriceRecord> result1 = instrumentPriceService.getPriceRecordWithInstrumentId("SILVER", InstrumentType.COMMODITIES);
        Optional<PriceRecord> result2 = instrumentPriceService.getPriceRecordWithInstrumentId("SILVER", null);
        Optional<PriceRecord> invalidRequest = instrumentPriceService.getPriceRecordWithInstrumentId("GOLD", InstrumentType.BOND);

        assertTrue(result1.isPresent());
        assertTrue(result2.isPresent());
        assertTrue(invalidRequest.isEmpty());
        assertEquals(updateRequest.getInstrument(), result1.get().getInstrument());
        assertEquals(updateRequest.getInstrument(), result2.get().getInstrument());
    }


    @Test
    void testGetPriceRecordsWithInstrumentType() {
        UpdatePriceRecordRequest updateRequest1 = new UpdatePriceRecordRequest.Builder()
                .setId(1)
                .setInstrument("instrument1")
                .setInstrumentType(InstrumentType.STOCK)
                .setValue(BigDecimal.valueOf(100))
                .setCurrency(Currency.USD)
                .setRequestTime(LocalDateTime.now())
                .build();

        UpdatePriceRecordRequest updateRequest2 = new UpdatePriceRecordRequest.Builder()
                .setId(2)
                .setInstrument("instrument2")
                .setInstrumentType(InstrumentType.STOCK)
                .setValue(BigDecimal.valueOf(200))
                .setCurrency(Currency.USD)
                .setRequestTime(LocalDateTime.now())
                .build();

        instrumentPriceService.clearAllPrices();

        instrumentPriceService.updateLatestPrice("batch123", updateRequest1);
        instrumentPriceService.updateLatestPrice("batch123", updateRequest2);

        List<PriceRecord> results1 = instrumentPriceService.getPriceRecordsWithInstrumentType(InstrumentType.STOCK);
        List<PriceRecord> results2 = instrumentPriceService.getPriceRecordsWithInstrumentType(InstrumentType.BOND);

        assertEquals(2, results1.size());
        assertEquals(0, results2.size());

        assertTrue(results1.stream().anyMatch(priceRecord -> priceRecord.getInstrument().equals(updateRequest1.getInstrument())));
        assertTrue(results1.stream().anyMatch(priceRecord -> priceRecord.getInstrument().equals(updateRequest2.getInstrument())));
    }

    @Test
    void testGetPriceRecordsWithDuration() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime pastTime1 = now.minusHours(2);
        LocalDateTime pastTime2 = now.minusHours(4);

        UpdatePriceRecordRequest recentRequest = new UpdatePriceRecordRequest.Builder()
                .setId(1)
                .setInstrument("instrument1")
                .setInstrumentType(InstrumentType.STOCK)
                .setValue(BigDecimal.valueOf(100))
                .setCurrency(Currency.USD)
                .setRequestTime(pastTime1)
                .build();

        UpdatePriceRecordRequest oldRequest = new UpdatePriceRecordRequest.Builder()
                .setId(2)
                .setInstrument("instrument2")
                .setInstrumentType(InstrumentType.BOND)
                .setValue(BigDecimal.valueOf(200))
                .setCurrency(Currency.USD)
                .setRequestTime(pastTime2)
                .build();

        InstrumentPriceService.latestPrices.clear();

        instrumentPriceService.updateLatestPrice("batch123", recentRequest);
        instrumentPriceService.updateLatestPrice("batch123", oldRequest);

        Duration duration1 = Duration.ofHours(1);
        Duration duration2 = Duration.ofHours(3);
        Duration duration3 = Duration.ofHours(5);

        GetPriceRecordsListResponse response1 = instrumentPriceService.getPriceRecordsWithDuration(duration1);
        GetPriceRecordsListResponse response2 = instrumentPriceService.getPriceRecordsWithDuration(duration2);
        GetPriceRecordsListResponse response3 = instrumentPriceService.getPriceRecordsWithDuration(duration3);

        assertNotNull(response1);
        assertNotNull(response2);
        assertNotNull(response3);

        assertEquals(0, response1.priceRecordList().size());
        assertEquals(1, response2.priceRecordList().size());
        assertEquals(2, response3.priceRecordList().size());

        assertTrue(response2.priceRecordList().stream().anyMatch(priceRecord -> priceRecord.getInstrument().equals(recentRequest.getInstrument())));
        assertTrue(response3.priceRecordList().stream().anyMatch(priceRecord -> priceRecord.getInstrument().equals(oldRequest.getInstrument())));

    }


    @Test
    void testClearAllPrices() {
        UpdatePriceRecordRequest updateRequest = new UpdatePriceRecordRequest.Builder()
                .setId(1)
                .setInstrument("instrument1")
                .setInstrumentType(InstrumentType.STOCK)
                .setValue(BigDecimal.valueOf(100))
                .setCurrency(Currency.USD)
                .setRequestTime(LocalDateTime.now())
                .build();

        instrumentPriceService.updateLatestPrice("batch123", updateRequest);
        instrumentPriceService.clearAllPrices();

        assertTrue(InstrumentPriceService.latestPrices.isEmpty());
    }

    @Test
    void testClearPriceForInstrumentId() {
        UpdatePriceRecordRequest updateRequest1 = new UpdatePriceRecordRequest.Builder()
                .setId(1)
                .setInstrument("Silver")
                .setInstrumentType(InstrumentType.COMMODITIES)
                .setValue(BigDecimal.valueOf(100))
                .setCurrency(Currency.USD)
                .setRequestTime(LocalDateTime.now())
                .build();

        UpdatePriceRecordRequest updateRequest2 = new UpdatePriceRecordRequest.Builder()
                .setId(1)
                .setInstrument("Gold")
                .setInstrumentType(InstrumentType.COMMODITIES)
                .setValue(BigDecimal.valueOf(100))
                .setCurrency(Currency.USD)
                .setRequestTime(LocalDateTime.now())
                .build();

        instrumentPriceService.updateLatestPrice("batch123", updateRequest1);
        instrumentPriceService.updateLatestPrice("batch123", updateRequest2);

        instrumentPriceService.clearPriceForInstrumentId("SILVER");

        assertNull(InstrumentPriceService.latestPrices.get(InstrumentType.STOCK).get("SILVER"));
        assertNull(InstrumentPriceService.latestPrices.get(InstrumentType.STOCK).get("GOLD"));
    }

}
