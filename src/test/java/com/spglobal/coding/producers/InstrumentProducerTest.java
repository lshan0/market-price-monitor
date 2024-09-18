package com.spglobal.coding.producers;

import com.spglobal.coding.producers.dto.BatchProcessResponse;
import com.spglobal.coding.producers.model.PriceRecordUpdateRequestBatch;
import com.spglobal.coding.utils.ChunkProcessor;
import com.spglobal.coding.utils.UpdatePriceRecordRequestFactory;
import com.spglobal.coding.utils.dto.UpdatePriceRecordRequest;
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
        PriceRecordUpdateRequestBatch createdBatch = instrumentProducer.getBatchById(batchId);
        assertNotNull(createdBatch);
        assertEquals(BatchStatus.STARTED, createdBatch.getStatus());
    }

    @Test
    void testUploadRecordsToExistingBatch() {
        // Arrange
        String batchId = instrumentProducer.startNewBatch();
        List<UpdatePriceRecordRequest> updatePriceRecordRequests = createRandomUpdatePriceRecordRequest(5);

        // Act
        instrumentProducer.uploadRequests(batchId, updatePriceRecordRequests);

        // Assert
        PriceRecordUpdateRequestBatch batch = instrumentProducer.getBatchById(batchId);
        assertNotNull(batch);
        assertEquals(BatchStatus.UPLOADING_REQUESTS, batch.getStatus());
        assertEquals(5, batch.getRequests().size());
    }

    @Test
    void testUploadRecordsToNonExistentBatch() {
        // Arrange
        List<UpdatePriceRecordRequest> updatePriceRecordRequests = createRandomUpdatePriceRecordRequest(5);
        String invalidBatchId = UUID.randomUUID().toString();

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            // Only the potentially exception-throwing operation should be inside the lambda
            uploadRecordsToBatch(invalidBatchId, updatePriceRecordRequests);
        });
        assertTrue(exception.getMessage().contains("does not exist"));
    }

    // Helper method to isolate the uploadRecords operation
    private void uploadRecordsToBatch(String batchId, List<UpdatePriceRecordRequest> priceRecords) {
        instrumentProducer.uploadRequests(batchId, priceRecords);
    }

    @Test
    void testCompleteBatchSuccessfully() {
        // Arrange
        String batchId = instrumentProducer.startNewBatch();
        List<UpdatePriceRecordRequest> updatePriceRecordRequests = createRandomUpdatePriceRecordRequest(5);
        instrumentProducer.uploadRequests(batchId, updatePriceRecordRequests);

        BatchProcessResponse mockResponse = new BatchProcessResponse(true, new ArrayList<>());
        CompletableFuture<BatchProcessResponse> future = CompletableFuture.completedFuture(mockResponse);

        when(chunkProcessor.processBatch(batchId, updatePriceRecordRequests)).thenReturn(future);

        // Act
        instrumentProducer.completeBatch(batchId);

        // Assert
        verify(chunkProcessor, times(1)).processBatch(batchId, updatePriceRecordRequests);
        PriceRecordUpdateRequestBatch batch = instrumentProducer.getBatchById(batchId);
        assertEquals(BatchStatus.COMPLETED, batch.getStatus());
    }

    @Test
    void testCompleteBatchWithErrors() {
        // Arrange
        String batchId = instrumentProducer.startNewBatch();
        List<UpdatePriceRecordRequest> updatePriceRecordRequests = createRandomUpdatePriceRecordRequest(5);
        instrumentProducer.uploadRequests(batchId, updatePriceRecordRequests);

        List<UpdatePriceRecordRequest> failedUpdateRequests = createRandomUpdatePriceRecordRequest(2);
        BatchProcessResponse mockResponse = new BatchProcessResponse(false, failedUpdateRequests);
        CompletableFuture<BatchProcessResponse> future = CompletableFuture.completedFuture(mockResponse);

        when(chunkProcessor.processBatch(batchId, updatePriceRecordRequests)).thenReturn(future);

        // Act
        instrumentProducer.completeBatch(batchId);

        // Assert
        verify(chunkProcessor, times(1)).processBatch(batchId, updatePriceRecordRequests);
        PriceRecordUpdateRequestBatch batch = instrumentProducer.getBatchById(batchId);
        assertEquals(BatchStatus.PROCESSED_WITH_ERRORS, batch.getStatus());
    }

    @Test
    void testCompleteBatchFails() {
        // Arrange
        String batchId = instrumentProducer.startNewBatch();
        List<UpdatePriceRecordRequest> updatePriceRecordRequests = createRandomUpdatePriceRecordRequest(5);
        instrumentProducer.uploadRequests(batchId, updatePriceRecordRequests);

        CompletableFuture<BatchProcessResponse> future = CompletableFuture.failedFuture(new RuntimeException("Processing error"));

        when(chunkProcessor.processBatch(batchId, updatePriceRecordRequests)).thenReturn(future);

        // Act
        instrumentProducer.completeBatch(batchId);

        // Assert
        PriceRecordUpdateRequestBatch batch = instrumentProducer.getBatchById(batchId);
        assertEquals(BatchStatus.FAILED, batch.getStatus());
    }

    @Test
    void testCancelInProgressBatch() {
        // Arrange
        String batchId = instrumentProducer.startNewBatch();
        List<UpdatePriceRecordRequest> priceRecords = createRandomUpdatePriceRecordRequest(5);
        instrumentProducer.uploadRequests(batchId, priceRecords);

        // Act
        instrumentProducer.cancelBatch(batchId);

        // Assert
        PriceRecordUpdateRequestBatch batch = instrumentProducer.getBatchById(batchId);
        assertEquals(BatchStatus.CANCELLED, batch.getStatus());
    }

    @Test
    void testCancelStartedBatch() {
        // Arrange
        String batchId = instrumentProducer.startNewBatch();

        // Act & Assert
        instrumentProducer.cancelBatch(batchId);

        // Assert
        PriceRecordUpdateRequestBatch batch = instrumentProducer.getBatchById(batchId);
        assertEquals(BatchStatus.CANCELLED, batch.getStatus());
    }

    @Test
    void testCancelCompletedBatch() {
        String batchId = instrumentProducer.startNewBatch();
        List<UpdatePriceRecordRequest> updatePriceRecordRequests = createRandomUpdatePriceRecordRequest(5);
        instrumentProducer.uploadRequests(batchId, updatePriceRecordRequests);

        BatchProcessResponse mockResponse = new BatchProcessResponse(true, new ArrayList<>());
        CompletableFuture<BatchProcessResponse> future = CompletableFuture.completedFuture(mockResponse);

        when(chunkProcessor.processBatch(batchId, updatePriceRecordRequests)).thenReturn(future);
        instrumentProducer.completeBatch(batchId);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            instrumentProducer.cancelBatch(batchId);
        });
        assertTrue(exception.getMessage().contains("cannot be cancelled as it is already completed,processed or cancelled."));
    }

    // Helper method to create price records using PriceRecordFactory
    private List<UpdatePriceRecordRequest> createRandomUpdatePriceRecordRequest(int count) {
        List<UpdatePriceRecordRequest> records = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            records.add(UpdatePriceRecordRequestFactory.createRandomUpdatePriceRecordRequest());
        }
        return records;
    }
}