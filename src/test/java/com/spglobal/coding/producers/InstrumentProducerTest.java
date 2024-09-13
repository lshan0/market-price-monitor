package com.spglobal.coding.producers;

import com.spglobal.coding.producers.dto.BatchProcessResponse;
import com.spglobal.coding.producers.model.PriceRecordBatch;
import com.spglobal.coding.services.model.PriceRecord;
import com.spglobal.coding.utils.ChunkProcessor;
import com.spglobal.coding.utils.PriceRecordFactory;
import com.spglobal.coding.utils.enums.BatchStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InstrumentProducerTest {

    private InstrumentProducer instrumentProducer;

    @Mock
    private ChunkProcessor chunkProcessor;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        instrumentProducer = new InstrumentProducer(chunkProcessor);
    }

    @Test
    void testStartNewBatch() {
        // Act
        String batchId = instrumentProducer.startNewBatch();

        // Assert
        assertNotNull(batchId);
        assertFalse(batchId.isEmpty());
        PriceRecordBatch createdBatch = instrumentProducer.getBatchById(batchId);
        assertNotNull(createdBatch);
        assertEquals(BatchStatus.STARTED, createdBatch.getStatus());
    }

    @Test
    void testUploadRecordsToExistingBatch() {
        // Arrange
        String batchId = instrumentProducer.startNewBatch();
        List<PriceRecord> priceRecords = createPriceRecords(5);

        // Act
        instrumentProducer.uploadRecords(batchId, priceRecords);

        // Assert
        PriceRecordBatch batch = instrumentProducer.getBatchById(batchId);
        assertNotNull(batch);
        assertEquals(BatchStatus.IN_PROGRESS, batch.getStatus());
        assertEquals(5, batch.getRecords().size());
    }

    @Test
    void testUploadRecordsToNonExistentBatch() {
        // Arrange
        List<PriceRecord> priceRecords = createPriceRecords(5);
        String invalidBatchId = UUID.randomUUID().toString();

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            // Only the potentially exception-throwing operation should be inside the lambda
            uploadRecordsToBatch(invalidBatchId, priceRecords);
        });
        assertTrue(exception.getMessage().contains("does not exist"));
    }

    // Helper method to isolate the uploadRecords operation
    private void uploadRecordsToBatch(String batchId, List<PriceRecord> priceRecords) {
        instrumentProducer.uploadRecords(batchId, priceRecords);
    }

    @Test
    void testCompleteBatchSuccessfully() {
        // Arrange
        String batchId = instrumentProducer.startNewBatch();
        List<PriceRecord> priceRecords = createPriceRecords(5);
        instrumentProducer.uploadRecords(batchId, priceRecords);

        BatchProcessResponse mockResponse = new BatchProcessResponse(true, new ArrayList<>());
        CompletableFuture<BatchProcessResponse> future = CompletableFuture.completedFuture(mockResponse);

        when(chunkProcessor.processBatch(batchId, priceRecords)).thenReturn(future);

        // Act
        instrumentProducer.completeBatch(batchId);

        // Assert
        verify(chunkProcessor, times(1)).processBatch(batchId, priceRecords);
        PriceRecordBatch batch = instrumentProducer.getBatchById(batchId);
        assertEquals(BatchStatus.COMPLETED, batch.getStatus());
    }

    @Test
    void testCompleteBatchWithErrors() {
        // Arrange
        String batchId = instrumentProducer.startNewBatch();
        List<PriceRecord> priceRecords = createPriceRecords(5);
        instrumentProducer.uploadRecords(batchId, priceRecords);

        List<PriceRecord> failedRecords = createPriceRecords(2);
        BatchProcessResponse mockResponse = new BatchProcessResponse(false, failedRecords);
        CompletableFuture<BatchProcessResponse> future = CompletableFuture.completedFuture(mockResponse);

        when(chunkProcessor.processBatch(batchId, priceRecords)).thenReturn(future);

        // Act
        instrumentProducer.completeBatch(batchId);

        // Assert
        verify(chunkProcessor, times(1)).processBatch(batchId, priceRecords);
        PriceRecordBatch batch = instrumentProducer.getBatchById(batchId);
        assertEquals(BatchStatus.PROCESSED_WITH_ERRORS, batch.getStatus());
    }

    @Test
    void testCompleteBatchFails() {
        // Arrange
        String batchId = instrumentProducer.startNewBatch();
        List<PriceRecord> priceRecords = createPriceRecords(5);
        instrumentProducer.uploadRecords(batchId, priceRecords);

        CompletableFuture<BatchProcessResponse> future = CompletableFuture.failedFuture(new RuntimeException("Processing error"));

        when(chunkProcessor.processBatch(batchId, priceRecords)).thenReturn(future);

        // Act
        instrumentProducer.completeBatch(batchId);

        // Assert
        PriceRecordBatch batch = instrumentProducer.getBatchById(batchId);
        assertEquals(BatchStatus.FAILED, batch.getStatus());
    }

    @Test
    void testCancelBatchSuccessfully() {
        // Arrange
        String batchId = instrumentProducer.startNewBatch();
        List<PriceRecord> priceRecords = createPriceRecords(5);
        instrumentProducer.uploadRecords(batchId, priceRecords);

        // Act
        instrumentProducer.cancelBatch(batchId);

        // Assert
        PriceRecordBatch batch = instrumentProducer.getBatchById(batchId);
        assertEquals(BatchStatus.CANCELLED, batch.getStatus());
    }

    @Test
    void testCancelBatchWithInvalidStatus() {
        // Arrange
        String batchId = instrumentProducer.startNewBatch();

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            instrumentProducer.cancelBatch(batchId);
        });
        assertTrue(exception.getMessage().contains("cannot be cancelled"));
    }

    // Helper method to create price records using PriceRecordFactory
    private List<PriceRecord> createPriceRecords(int count) {
        List<PriceRecord> records = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            records.add(PriceRecordFactory.createRandomPriceRecord());
        }
        return records;
    }
}