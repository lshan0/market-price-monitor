package com.spglobal.coding.consumers;

import com.spglobal.coding.consumers.dto.GetPriceRecordResponse;
import com.spglobal.coding.consumers.dto.GetPriceRecordsListResponse;
import com.spglobal.coding.services.InstrumentPriceService;
import com.spglobal.coding.services.model.PriceRecord;
import com.spglobal.coding.utils.enums.InstrumentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InstrumentConsumerTest {

    @InjectMocks
    private InstrumentConsumer instrumentConsumer;

    @Mock
    private InstrumentPriceService instrumentPriceService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetPriceRecordByIdFound() {
        // Arrange
        String recordId = "record123";
        PriceRecord priceRecord = mock(PriceRecord.class);
        when(instrumentPriceService.getPriceRecordWithRecordId(recordId, null))
                .thenReturn(Optional.of(priceRecord));

        // Act
        GetPriceRecordResponse response = instrumentConsumer.getPriceRecordById(recordId);

        // Assert
        assertTrue(response.priceRecord().isPresent());
        assertEquals(priceRecord, response.priceRecord().get());
        verify(instrumentPriceService).getPriceRecordWithRecordId(recordId, null);
    }

    @Test
    void testGetPriceRecordByIdNotFound() {
        // Arrange
        String recordId = "record123";
        when(instrumentPriceService.getPriceRecordWithRecordId(recordId, null))
                .thenReturn(Optional.empty());

        // Act
        GetPriceRecordResponse response = instrumentConsumer.getPriceRecordById(recordId);

        // Assert
        assertFalse(response.priceRecord().isPresent());
        verify(instrumentPriceService).getPriceRecordWithRecordId(recordId, null);
    }

    @Test
    void testGetPriceRecordByIdWithInstrumentType() {
        // Arrange
        String recordId = "record123";
        InstrumentType instrumentType = InstrumentType.STOCK;
        PriceRecord priceRecord = mock(PriceRecord.class);
        when(instrumentPriceService.getPriceRecordWithRecordId(recordId, instrumentType))
                .thenReturn(Optional.of(priceRecord));

        // Act
        GetPriceRecordResponse response = instrumentConsumer.getPriceRecordById(recordId, instrumentType);

        // Assert
        assertTrue(response.priceRecord().isPresent());
        assertEquals(priceRecord, response.priceRecord().get());
        verify(instrumentPriceService).getPriceRecordWithRecordId(recordId, instrumentType);
    }

    @Test
    void testGetPriceRecordByInstrumentIdFound() {
        // Arrange
        String instrumentId = "instrument123";
        PriceRecord priceRecord = mock(PriceRecord.class);
        when(instrumentPriceService.getPriceRecordWithInstrumentId(instrumentId, null))
                .thenReturn(Optional.of(priceRecord));

        // Act
        GetPriceRecordResponse response = instrumentConsumer.getPriceRecordByInstrumentId(instrumentId);

        // Assert
        assertTrue(response.priceRecord().isPresent());
        assertEquals(priceRecord, response.priceRecord().get());
        verify(instrumentPriceService).getPriceRecordWithInstrumentId(instrumentId, null);
    }

    @Test
    void testGetPriceRecordByInstrumentIdNotFound() {
        // Arrange
        String instrumentId = "instrument123";
        when(instrumentPriceService.getPriceRecordWithInstrumentId(instrumentId, null))
                .thenReturn(Optional.empty());

        // Act
        GetPriceRecordResponse response = instrumentConsumer.getPriceRecordByInstrumentId(instrumentId);

        // Assert
        assertFalse(response.priceRecord().isPresent());
        verify(instrumentPriceService).getPriceRecordWithInstrumentId(instrumentId, null);
    }

    @Test
    void testGetPriceRecordByInstrumentIdWithInstrumentType() {
        // Arrange
        String instrumentId = "instrument123";
        InstrumentType instrumentType = InstrumentType.STOCK;
        PriceRecord priceRecord = mock(PriceRecord.class);
        when(instrumentPriceService.getPriceRecordWithInstrumentId(instrumentId, instrumentType))
                .thenReturn(Optional.of(priceRecord));

        // Act
        GetPriceRecordResponse response = instrumentConsumer.getPriceRecordByInstrumentId(instrumentId, instrumentType);

        // Assert
        assertTrue(response.priceRecord().isPresent());
        assertEquals(priceRecord, response.priceRecord().get());
        verify(instrumentPriceService).getPriceRecordWithInstrumentId(instrumentId, instrumentType);
    }

    @Test
    void testGetPriceRecordsByInstrumentType() {
        // Arrange
        InstrumentType instrumentType = InstrumentType.STOCK;
        List<PriceRecord> priceRecords = List.of(mock(PriceRecord.class), mock(PriceRecord.class));
        when(instrumentPriceService.getPriceRecordsWithInstrumentType(instrumentType))
                .thenReturn(priceRecords);

        // Act
        GetPriceRecordsListResponse response = instrumentConsumer.getPriceRecordsByInstrumentType(instrumentType);

        // Assert
        assertNotNull(response);
        assertEquals(priceRecords, response.priceRecordList());
        verify(instrumentPriceService).getPriceRecordsWithInstrumentType(instrumentType);
    }

    @Test
    void testGetPriceRecordsByInstrumentTypeNoRecords() {
        // Arrange
        InstrumentType instrumentType = InstrumentType.STOCK;
        when(instrumentPriceService.getPriceRecordsWithInstrumentType(instrumentType))
                .thenReturn(List.of());

        // Act
        GetPriceRecordsListResponse response = instrumentConsumer.getPriceRecordsByInstrumentType(instrumentType);

        // Assert
        assertNotNull(response);
        assertTrue(response.priceRecordList().isEmpty());
        verify(instrumentPriceService).getPriceRecordsWithInstrumentType(instrumentType);
    }

    @Test
    void testGetPriceRecordsInLastDuration() {
        // Arrange
        Duration duration = Duration.ofHours(1);
        List<PriceRecord> priceRecords = List.of(mock(PriceRecord.class), mock(PriceRecord.class));
        when(instrumentPriceService.getPriceRecordsWithDuration(duration))
                .thenReturn(new GetPriceRecordsListResponse(priceRecords));

        // Act
        GetPriceRecordsListResponse response = instrumentConsumer.getPriceRecordsInLastDuration(duration);

        // Assert
        assertNotNull(response);
        assertEquals(priceRecords, response.priceRecordList());
        verify(instrumentPriceService).getPriceRecordsWithDuration(duration);
    }
}
