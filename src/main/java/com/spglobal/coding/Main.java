package com.spglobal.coding;

import com.spglobal.coding.consumers.InstrumentConsumer;
import com.spglobal.coding.consumers.dto.GetPriceRecordResponse;
import com.spglobal.coding.consumers.dto.GetPriceRecordsListResponse;
import com.spglobal.coding.producers.InstrumentProducer;
import com.spglobal.coding.services.InstrumentPriceService;
import com.spglobal.coding.services.model.PriceRecord;
import com.spglobal.coding.utils.ChunkProcessor;
import com.spglobal.coding.utils.UpdatePriceRecordRequestFactory;
import com.spglobal.coding.utils.dto.UpdatePriceRecordRequest;
import com.spglobal.coding.utils.enums.InstrumentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws InterruptedException {
        InstrumentPriceService instrumentPriceService = new InstrumentPriceService();
        ChunkProcessor chunkProcessor = new ChunkProcessor(instrumentPriceService);

        InstrumentProducer producer = new InstrumentProducer(chunkProcessor);
        InstrumentConsumer consumer = new InstrumentConsumer(instrumentPriceService);

        Map<InstrumentType, Map<String, PriceRecord>> latestPrices = InstrumentPriceService.getLatestPrices();

        List<UpdatePriceRecordRequest> updatePriceRecordRequestList = UpdatePriceRecordRequestFactory.generateUpdatePriceRecordRequestBatch();
        try {
            String batchId = producer.startNewBatch();
            producer.uploadRequests(batchId, updatePriceRecordRequestList);
            producer.completeBatch(batchId);
        } catch (Exception e) {
            logger.info("Unexpected error while producer generation: {}", e.getMessage());
        }

        Thread.sleep(2000);

        GetPriceRecordsListResponse getPriceRecordsListResponse1 = consumer.getPriceRecordsByInstrumentType(InstrumentType.STOCK);
        GetPriceRecordsListResponse getPriceRecordsListResponse2 = consumer.getPriceRecordsByInstrumentType(InstrumentType.COMMODITIES);

        String randomInstrumentId = UpdatePriceRecordRequestFactory.getRandomInstrumentId();
        GetPriceRecordResponse priceRecordWithInstrumentId = consumer.getPriceRecordByInstrumentId(randomInstrumentId);

        randomInstrumentId = UpdatePriceRecordRequestFactory.getRandomInstrumentId();
        InstrumentType type = UpdatePriceRecordRequestFactory.getInstrumentTypeFromId(randomInstrumentId);
        GetPriceRecordResponse priceRecordWithInstrumentIdAndType = consumer.getPriceRecordByInstrumentId(randomInstrumentId, type);

        GetPriceRecordsListResponse priceRecordsWithinDuration = consumer.getPriceRecordsInLastDuration(Duration.of(10, ChronoUnit.SECONDS));

        logger.debug("{} {} {} {} {}", priceRecordsWithinDuration, getPriceRecordsListResponse1, getPriceRecordsListResponse2, priceRecordWithInstrumentIdAndType, priceRecordWithInstrumentId);

    }
}