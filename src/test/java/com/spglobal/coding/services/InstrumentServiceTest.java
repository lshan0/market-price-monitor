package com.spglobal.coding.services;

import com.spglobal.coding.consumers.dto.GetPriceRecordsListResponse;
import com.spglobal.coding.producers.dto.ChunkProcessRequest;
import com.spglobal.coding.services.dto.ChunkProcessResponse;
import com.spglobal.coding.services.model.Payload;
import com.spglobal.coding.services.model.PriceRecord;
import com.spglobal.coding.utils.enums.Currency;
import com.spglobal.coding.utils.exceptions.RecordProcessingException;
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
        // Arrange
        String batchId = "batch123";
        Payload payload1 = new Payload(BigDecimal.valueOf(100), Currency.USD);
        Payload payload2 = new Payload(BigDecimal.valueOf(200), Currency.USD);
        PriceRecord priceRecord1 = new PriceRecord("id1", "instrument1", "instId1", InstrumentType.STOCK, LocalDateTime.now(), payload1);
        PriceRecord priceRecord2 = new PriceRecord("id2", "instrument2", "instId2", InstrumentType.BOND, LocalDateTime.now(), payload2);
        ChunkProcessRequest request = new ChunkProcessRequest(batchId, List.of(priceRecord1, priceRecord2));

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
        // Arrange
        String batchId = "batch123";
        PriceRecord invalidRecord = new PriceRecord(null, null, null, null, null, null);
        ChunkProcessRequest request = new ChunkProcessRequest(batchId, List.of(invalidRecord));

        // Act
        ChunkProcessResponse response = instrumentPriceService.processChunk(request);

        // Assert
        assertFalse(response.isSuccess());
        assertEquals(1, response.failedRecords().size());
    }

    @Test
    void testUpdateLatestPriceWithValidRecord() {
        // Arrange
        Payload payload = new Payload(BigDecimal.valueOf(100), Currency.USD);
        PriceRecord priceRecord = new PriceRecord("id1", "instrument1", "instId1", InstrumentType.STOCK, LocalDateTime.now(), payload);
        InstrumentPriceService.latestPrices.clear();  // Ensure the map is empty before the test

        // Act
        instrumentPriceService.updateLatestPrice("batch123", priceRecord);

        // Assert
        Map<String, PriceRecord> priceMap = InstrumentPriceService.latestPrices.get(InstrumentType.STOCK);
        assertNotNull(priceMap);
        assertEquals(priceRecord, priceMap.get("instId1"));
    }


    @Test
    void testUpdateLatestPriceWithInvalidRecord() {
        // Arrange
        PriceRecord invalidRecord = new PriceRecord(null, null, null, null, null, null);

        // Act & Assert
        assertThrows(RecordProcessingException.class, () -> {
            instrumentPriceService.updateLatestPrice("batch123", invalidRecord);
        });
    }

    @Test
    void testGetPriceRecordWithRecordId() {
        // Arrange
        Payload payload = new Payload(BigDecimal.valueOf(100), Currency.USD);
        PriceRecord priceRecord = new PriceRecord("id1", "instrument1", "instId1", InstrumentType.STOCK, LocalDateTime.now(), payload);
        instrumentPriceService.updateLatestPrice("batch123", priceRecord);

        // Act
        Optional<PriceRecord> result = instrumentPriceService.getPriceRecordWithRecordId("id1", InstrumentType.STOCK);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(priceRecord, result.get());
    }

    @Test
    void testGetPriceRecordWithInstrumentId() {
        // Arrange
        Payload payload = new Payload(BigDecimal.valueOf(100), Currency.USD);
        PriceRecord priceRecord = new PriceRecord("id1", "instrument1", "instId1", InstrumentType.STOCK, LocalDateTime.now(), payload);
        instrumentPriceService.updateLatestPrice("batch123", priceRecord);

        // Act
        Optional<PriceRecord> result = instrumentPriceService.getPriceRecordWithInstrumentId("instId1", InstrumentType.STOCK);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(priceRecord, result.get());
    }

    @Test
    void testGetPriceRecordsWithInstrumentType() {
        // Arrange
        Payload payload1 = new Payload(BigDecimal.valueOf(100), Currency.USD);
        Payload payload2 = new Payload(BigDecimal.valueOf(200), Currency.USD);
        PriceRecord priceRecord1 = new PriceRecord("id1", "instrument1", "instId1", InstrumentType.STOCK, LocalDateTime.now(), payload1);
        PriceRecord priceRecord2 = new PriceRecord("id2", "instrument2", "instId2", InstrumentType.STOCK, LocalDateTime.now(), payload2);
        instrumentPriceService.updateLatestPrice("batch123", priceRecord1);
        instrumentPriceService.updateLatestPrice("batch123", priceRecord2);

        // Act
        List<PriceRecord> results = instrumentPriceService.getPriceRecordsWithInstrumentType(InstrumentType.STOCK);

        // Assert
        assertEquals(2, results.size());
        assertTrue(results.contains(priceRecord1));
        assertTrue(results.contains(priceRecord2));
    }


    @Test
    void testGetPriceRecordsWithDuration() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime pastTime = now.minusHours(2);
        PriceRecord recentRecord = new PriceRecord("id1", "instrument1", "instId1", InstrumentType.STOCK, now, new Payload(BigDecimal.valueOf(100), Currency.USD));
        PriceRecord oldRecord = new PriceRecord("id2", "instrument2", "instId2", InstrumentType.BOND, pastTime, new Payload(BigDecimal.valueOf(200), Currency.USD));

        // Clear the map before test
        InstrumentPriceService.latestPrices.clear();

        // Add records to the service
        instrumentPriceService.updateLatestPrice("batch123", recentRecord);
        instrumentPriceService.updateLatestPrice("batch123", oldRecord);

        Duration duration = Duration.ofHours(1); // Set the duration for filtering

        // Act
        GetPriceRecordsListResponse response = instrumentPriceService.getPriceRecordsWithDuration(duration);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.priceRecordList().size());
        assertTrue(response.priceRecordList().contains(recentRecord));
    }

    @Test
    void testClearAllPrices() {
        // Arrange
        Payload payload = new Payload(BigDecimal.valueOf(100), Currency.USD);
        PriceRecord priceRecord = new PriceRecord("id1", "instrument1", "instId1", InstrumentType.STOCK, LocalDateTime.now(), payload);
        instrumentPriceService.updateLatestPrice("batch123", priceRecord);

        // Act
        instrumentPriceService.clearAllPrices();

        // Assert
        assertTrue(InstrumentPriceService.latestPrices.isEmpty());
    }

    @Test
    void testClearPriceForInstrumentId() {
        // Arrange
        Payload payload = new Payload(BigDecimal.valueOf(100), Currency.USD);
        PriceRecord priceRecord = new PriceRecord("id1", "instrument1", "instId1", InstrumentType.STOCK, LocalDateTime.now(), payload);
        instrumentPriceService.updateLatestPrice("batch123", priceRecord);

        // Act
        instrumentPriceService.clearPriceForInstrumentId("instId1");

        // Assert
        assertNull(InstrumentPriceService.latestPrices.get(InstrumentType.STOCK).get("instId1"));
    }
}
