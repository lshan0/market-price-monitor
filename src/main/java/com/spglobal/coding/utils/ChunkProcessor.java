package com.spglobal.coding.utils;

import com.spglobal.coding.producers.dto.BatchProcessRequest;
import com.spglobal.coding.services.InstrumentPriceService;
import com.spglobal.coding.services.dto.BatchProcessResponse;
import com.spglobal.coding.services.model.PriceRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChunkProcessor {

    private static final Logger logger = LoggerFactory.getLogger(ChunkProcessor.class);
    private static final int CHUNK_SIZE = 1000;
    private final ExecutorService executorService;
    private final InstrumentPriceService instrumentPriceService;

    public ChunkProcessor(InstrumentPriceService instrumentPriceService) {
        this.instrumentPriceService = instrumentPriceService;
        this.executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        logger.info("ChunkProcessor initialized with a fixed thread pool of size: {}", Runtime.getRuntime().availableProcessors());
    }

    public CompletableFuture<BatchProcessResponse> processBatch(String batchId, List<PriceRecord> allRecords) {
        // Split records into chunks
        List<List<PriceRecord>> chunks = partitionBatchIntoChunks(allRecords);
        logger.info("Partitioned batch with batchId {} into {} chunks", batchId, chunks.size());

        // Create a list of CompletableFuture for processing each chunk
        List<CompletableFuture<BatchProcessResponse>> futures = chunks.stream()
                .map(chunk -> CompletableFuture.supplyAsync(() -> processChunk(batchId, chunk), executorService))
                .toList();

        // Combine all futures into one CompletableFuture
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(result -> {
                    // Combine all failed records from futures
                    List<PriceRecord> allFailedRecords = futures.stream()
                            .map(CompletableFuture::join)
                            .flatMap(response -> response.failedRecords().stream())
                            .toList();

                    // Return the BatchProcessResponse with the combined failed records
                    logger.info("Batch processing for batchId {} completed with {} failed records", batchId, allFailedRecords.size());
                    return new BatchProcessResponse(allFailedRecords.isEmpty(), allFailedRecords);
                });
    }

    private BatchProcessResponse processChunk(String batchId, List<PriceRecord> chunk) {
        logger.info("Processing chunk for batchId {} with {} records", batchId, chunk.size());
        try {
            return instrumentPriceService.processChunk(new BatchProcessRequest(batchId, chunk));
        } catch (Exception e) {
            logger.error("Exception occurred while processing chunk for batchId {}: {}", batchId, e.getMessage());
            return new BatchProcessResponse(false, chunk);
        }
    }

    private List<List<PriceRecord>> partitionBatchIntoChunks(List<PriceRecord> list) {
        List<List<PriceRecord>> chunks = new ArrayList<>();
        for (int i = 0; i < list.size(); i += CHUNK_SIZE) {
            chunks.add(list.subList(i, Math.min(i + CHUNK_SIZE, list.size())));
        }
        return chunks;
    }

    public void shutdown() {
        logger.info("Shutting down executor service");
        executorService.shutdown();
    }
}
