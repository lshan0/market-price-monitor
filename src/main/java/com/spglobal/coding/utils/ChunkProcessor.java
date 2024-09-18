package com.spglobal.coding.utils;

import com.spglobal.coding.producers.dto.BatchProcessResponse;
import com.spglobal.coding.producers.dto.ChunkProcessRequest;
import com.spglobal.coding.services.PriceService;
import com.spglobal.coding.services.dto.ChunkProcessResponse;
import com.spglobal.coding.utils.dto.UpdatePriceRecordRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The ChunkProcessor class handles the processing of large batches of Update Requests by splitting them into smaller chunks
 * and processing them asynchronously using a thread pool.
 */
public class ChunkProcessor {

    private static final Logger logger = LoggerFactory.getLogger(ChunkProcessor.class);
    private static final int CHUNK_SIZE = 1000;
    private final ExecutorService executorService;
    private final PriceService priceService;

    public ChunkProcessor(PriceService priceService) {
        this.priceService = priceService;
        this.executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        logger.info("ChunkProcessor initialized with a fixed thread pool of size: {}", Runtime.getRuntime().availableProcessors());
    }

    /**
     * Processes a large batch of UpdateRequests by splitting them into smaller chunks and processing them asynchronously.
     * Returns a CompletableFuture that completes when all chunks are processed, providing a BatchProcessResponse.
     *
     * @param batchId    the unique identifier of the batch being processed.
     * @param allRequests the list of UpdateRequests to be processed.
     * @return a CompletableFuture containing a BatchProcessResponse with the status of the batch and any failed records.
     * @throws NullPointerException if batchId or allRequests is null.
     */
    public CompletableFuture<BatchProcessResponse> processBatch(String batchId, List<UpdatePriceRecordRequest> allRequests) {
        if (batchId == null || allRequests == null) {
            throw new NullPointerException("batchId/Records cannot be null");
        }

        // Split records into chunks
        List<List<UpdatePriceRecordRequest>> chunks = partitionBatchIntoChunks(allRequests);
        logger.info("Partitioned batch with batchId {} into {} chunks", batchId, chunks.size());

        // Create a list of CompletableFuture for processing each chunk
        List<CompletableFuture<ChunkProcessResponse>> futures = chunks.stream()
                .map(chunk -> CompletableFuture.supplyAsync(() -> processChunk(batchId, chunk), executorService)
                        .exceptionally(ex -> {
                            logger.error("Exception occurred while processing chunk for batchId {}: {}", batchId, ex.getMessage());
                            return new ChunkProcessResponse(false, chunk); // Handle the exception and return a response with the failed chunk
                        }))
                .toList();

        // Combine all futures into one CompletableFuture
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(result -> {
                    // Combine all failed records from futures
                    List<UpdatePriceRecordRequest> allFailedRequests = futures.stream()
                            .map(CompletableFuture::join)
                            .flatMap(response -> response.failedRequests().stream())
                            .toList();

                    // Return the BatchProcessResponse with the combined failed records
                    logger.info("Batch processing for batchId {} completed with {} failed records", batchId, allFailedRequests.size());
                    return new BatchProcessResponse(allFailedRequests.isEmpty(), allFailedRequests);
                });
    }

    /**
     * Processes a single chunk of UpdateRequests for the given batchId.
     *
     * @param batchId the unique identifier of the batch to which the chunk belongs.
     * @param chunk   the list of UpdateRequests in this chunk.
     * @return a ChunkProcessResponse indicating the success or failure of the chunk processing, along with any failed requests.
     */
    private ChunkProcessResponse processChunk(String batchId, List<UpdatePriceRecordRequest> chunk) {
        logger.info("Processing chunk for batchId {} with {} requests", batchId, chunk.size());
        try {
            // Process the chunk and return the response
            return priceService.processChunk(new ChunkProcessRequest(batchId, chunk));
        } catch (Exception e) {
            return new ChunkProcessResponse(false, chunk);
        }
    }

    /**
     * Partitions a large list of UpdateRequests into smaller chunks of a predefined size.
     *
     * @param list the full list of PriceRecords to be partitioned.
     * @return a list of lists, where each inner list is a chunk of UpdateRequests.
     */
     List<List<UpdatePriceRecordRequest>> partitionBatchIntoChunks(List<UpdatePriceRecordRequest> list) {
        List<List<UpdatePriceRecordRequest>> chunks = new ArrayList<>();
        for (int i = 0; i < list.size(); i += CHUNK_SIZE) { // Create sublist for each chunk
            chunks.add(list.subList(i, Math.min(i + CHUNK_SIZE, list.size())));
        }
        return chunks;
    }

    public void shutdown() {
        logger.info("Shutting down executor service");
        executorService.shutdown(); // Shutdown the executor service
    }
}
