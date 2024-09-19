package com.spglobal.coding.producers;

import com.spglobal.coding.producers.dto.BatchStartResponse;
import com.spglobal.coding.utils.dto.BatchProcessResponse;
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
        BatchStartResponse startResponse = instrumentProducer.startNewBatch();

        // Assert
        assertNotNull(startResponse.batchId());
        assertFalse(startResponse.batchId().isEmpty());
        PriceRecordUpdateRequestBatch createdBatch = instrumentProducer.getBatchById(startResponse.batchId());
        assertNotNull(createdBatch);
        assertEquals(BatchStatus.STARTED, createdBatch.getStatus());
    }

    @Test
    void testUploadRecordsToExistingBatch() {
        // Arrange
        BatchStartResponse startResponse = instrumentProducer.startNewBatch();
        List<UpdatePriceRecordRequest> updatePriceRecordRequests = createRandomUpdatePriceRecordRequest(5);

        // Act
        instrumentProducer.uploadRequests(startResponse.batchId(), updatePriceRecordRequests);

        // Assert
        PriceRecordUpdateRequestBatch batch = instrumentProducer.getBatchById(startResponse.batchId());
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
        BatchStartResponse startResponse = instrumentProducer.startNewBatch();
        List<UpdatePriceRecordRequest> updatePriceRecordRequests = createRandomUpdatePriceRecordRequest(5);
        instrumentProducer.uploadRequests(startResponse.batchId(), updatePriceRecordRequests);

        BatchProcessResponse mockResponse = new BatchProcessResponse(true, new ArrayList<>());
        CompletableFuture<BatchProcessResponse> future = CompletableFuture.completedFuture(mockResponse);

        when(chunkProcessor.processBatch(startResponse.batchId(), updatePriceRecordRequests)).thenReturn(future);

        // Act
        instrumentProducer.completeBatch(startResponse.batchId());

        // Assert
        verify(chunkProcessor, times(1)).processBatch(startResponse.batchId(), updatePriceRecordRequests);
        PriceRecordUpdateRequestBatch batch = instrumentProducer.getBatchById(startResponse.batchId());
        assertEquals(BatchStatus.COMPLETED, batch.getStatus());
    }

    @Test
    void testCompleteBatchWithErrors() {
        // Arrange
        BatchStartResponse startResponse = instrumentProducer.startNewBatch();
        List<UpdatePriceRecordRequest> updatePriceRecordRequests = createRandomUpdatePriceRecordRequest(5);
        instrumentProducer.uploadRequests(startResponse.batchId(), updatePriceRecordRequests);

        List<UpdatePriceRecordRequest> failedUpdateRequests = createRandomUpdatePriceRecordRequest(2);
        BatchProcessResponse mockResponse = new BatchProcessResponse(false, failedUpdateRequests);
        CompletableFuture<BatchProcessResponse> future = CompletableFuture.completedFuture(mockResponse);

        when(chunkProcessor.processBatch(startResponse.batchId(), updatePriceRecordRequests)).thenReturn(future);

        // Act
        instrumentProducer.completeBatch(startResponse.batchId());

        // Assert
        verify(chunkProcessor, times(1)).processBatch(startResponse.batchId(), updatePriceRecordRequests);
        PriceRecordUpdateRequestBatch batch = instrumentProducer.getBatchById(startResponse.batchId());
        assertEquals(BatchStatus.PROCESSED_WITH_ERRORS, batch.getStatus());
    }

    @Test
    void testCompleteBatchFails() {
        // Arrange
        BatchStartResponse startResponse = instrumentProducer.startNewBatch();
        List<UpdatePriceRecordRequest> updatePriceRecordRequests = createRandomUpdatePriceRecordRequest(5);
        instrumentProducer.uploadRequests(startResponse.batchId(), updatePriceRecordRequests);

        CompletableFuture<BatchProcessResponse> future = CompletableFuture.failedFuture(new RuntimeException("Processing error"));

        when(chunkProcessor.processBatch(startResponse.batchId(), updatePriceRecordRequests)).thenReturn(future);

        // Act
        instrumentProducer.completeBatch(startResponse.batchId());

        // Assert
        PriceRecordUpdateRequestBatch batch = instrumentProducer.getBatchById(startResponse.batchId());
        assertEquals(BatchStatus.FAILED, batch.getStatus());
    }

    @Test
    void testCancelInProgressBatch() {
        // Arrange
        BatchStartResponse startResponse = instrumentProducer.startNewBatch();
        List<UpdatePriceRecordRequest> priceRecords = createRandomUpdatePriceRecordRequest(5);
        instrumentProducer.uploadRequests(startResponse.batchId(), priceRecords);

        // Act
        instrumentProducer.cancelBatch(startResponse.batchId());

        // Assert
        PriceRecordUpdateRequestBatch batch = instrumentProducer.getBatchById(startResponse.batchId());
        assertEquals(BatchStatus.CANCELLED, batch.getStatus());
    }

    @Test
    void testCancelStartedBatch() {
        // Arrange
        BatchStartResponse startResponse = instrumentProducer.startNewBatch();

        // Act & Assert
        instrumentProducer.cancelBatch(startResponse.batchId());

        // Assert
        PriceRecordUpdateRequestBatch batch = instrumentProducer.getBatchById(startResponse.batchId());
        assertEquals(BatchStatus.CANCELLED, batch.getStatus());
    }

    @Test
    void testCancelCompletedBatch() {
        BatchStartResponse startResponse = instrumentProducer.startNewBatch();
        String batchId = startResponse.batchId();

        List<UpdatePriceRecordRequest> updatePriceRecordRequests = createRandomUpdatePriceRecordRequest(5);
        instrumentProducer.uploadRequests(startResponse.batchId(), updatePriceRecordRequests);

        BatchProcessResponse mockResponse = new BatchProcessResponse(true, new ArrayList<>());
        CompletableFuture<BatchProcessResponse> future = CompletableFuture.completedFuture(mockResponse);

        when(chunkProcessor.processBatch(startResponse.batchId(), updatePriceRecordRequests)).thenReturn(future);
        instrumentProducer.completeBatch(startResponse.batchId());

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> instrumentProducer.cancelBatch(batchId)
        );
        assertTrue(exception.getMessage().contains("cannot be cancelled as it is not in a cancellable state."));
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