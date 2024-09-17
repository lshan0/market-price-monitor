package com.spglobal.coding.utils;

import com.spglobal.coding.producers.dto.BatchProcessResponse;
import com.spglobal.coding.services.InstrumentPriceService;
import com.spglobal.coding.services.dto.ChunkProcessResponse;
import com.spglobal.coding.utils.dto.UpdatePriceRecordRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ChunkProcessorTest {

    private ChunkProcessor chunkProcessor;
    private InstrumentPriceService mockInstrumentPriceService;

    @BeforeEach
    void setUp() {
        mockInstrumentPriceService = mock(InstrumentPriceService.class);
        chunkProcessor = new ChunkProcessor(mockInstrumentPriceService);
    }

    @Test
    void testProcessBatchWithValidRequests() {
        String batchId = "batch123";
        List<UpdatePriceRecordRequest> requests = createMockRequests(2000); // Mock 2000 requests

        // Mock successful chunk processing
        when(mockInstrumentPriceService.processChunk(any())).thenReturn(new ChunkProcessResponse(true, Collections.emptyList()));

        // Process batch
        CompletableFuture<BatchProcessResponse> future = chunkProcessor.processBatch(batchId, requests);
        BatchProcessResponse response = future.join(); // Block until future completes

        // Verify interactions
        verify(mockInstrumentPriceService, times(2)).processChunk(any()); // 2 chunks of 1000 requests each
        assertTrue(response.isSuccess());
        assertTrue(response.failedRequests().isEmpty());
    }

    @Test
    void testProcessBatchWithFailures() {
        String batchId = "batch456";
        List<UpdatePriceRecordRequest> requests = createMockRequests(1500); // Mock 1500 requests

        // Mock chunk processing with failures
        List<UpdatePriceRecordRequest> failedRequests = createMockRequests(500);
        when(mockInstrumentPriceService.processChunk(any()))
                .thenReturn(new ChunkProcessResponse(true, Collections.emptyList())) // First chunk success
                .thenReturn(new ChunkProcessResponse(false, failedRequests));       // Second chunk failure

        // Process batch
        CompletableFuture<BatchProcessResponse> future = chunkProcessor.processBatch(batchId, requests);
        BatchProcessResponse response = future.join(); // Block until future completes

        // Verify interactions
        verify(mockInstrumentPriceService, times(2)).processChunk(any()); // 2 chunks
        assertFalse(response.isSuccess());
        assertEquals(500, response.failedRequests().size());
    }

    @Test
    void testProcessBatchWithException() {
        String batchId = "batch789";
        List<UpdatePriceRecordRequest> requests = createMockRequests(500);

        // Simulate exception during chunk processing
        when(mockInstrumentPriceService.processChunk(any())).thenThrow(new RuntimeException("Processing error"));

        // Process batch
        CompletableFuture<BatchProcessResponse> future = chunkProcessor.processBatch(batchId, requests);
        BatchProcessResponse response = future.join(); // Block until future completes

        // Verify interactions
        verify(mockInstrumentPriceService, times(1)).processChunk(any());
        assertFalse(response.isSuccess());
        assertEquals(500, response.failedRequests().size());
    }

    @Test
    void testPartitionBatchIntoChunks() {
        List<UpdatePriceRecordRequest> requests = createMockRequests(2050); // Mock 2050 requests
        List<List<UpdatePriceRecordRequest>> chunks = chunkProcessor.partitionBatchIntoChunks(requests);

        // Check that the requests are partitioned into 3 chunks (1000, 1000, 50)
        assertEquals(3, chunks.size());
        assertEquals(1000, chunks.get(0).size());
        assertEquals(1000, chunks.get(1).size());
        assertEquals(50, chunks.get(2).size());
    }

    @Test
    void testProcessBatchWithNullBatchId() {
        List<UpdatePriceRecordRequest> requests = createMockRequests(100);
        assertThrows(NullPointerException.class, () -> chunkProcessor.processBatch(null, requests));
    }

    @Test
    void testProcessBatchWithNullRequests() {
        assertThrows(NullPointerException.class, () -> chunkProcessor.processBatch("batch123", null));
    }

    @Test
    void testShutdown() {
        chunkProcessor.shutdown();
        verify(mockInstrumentPriceService, never()).processChunk(any());
    }

    // Helper method to create mock UpdatePriceRecordRequest objects
    private List<UpdatePriceRecordRequest> createMockRequests(int count) {
        List<UpdatePriceRecordRequest> requests = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            UpdatePriceRecordRequest request = Mockito.mock(UpdatePriceRecordRequest.class);
            requests.add(request);
        }
        return requests;
    }
}
