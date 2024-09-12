package com.spglobal.coding;

import com.spglobal.coding.consumers.InstrumentConsumer;
import com.spglobal.coding.consumers.dto.GetPriceRecordResponse;
import com.spglobal.coding.consumers.dto.GetPriceRecordsListResponse;
import com.spglobal.coding.producers.InstrumentProducer;
import com.spglobal.coding.services.InstrumentPriceService;
import com.spglobal.coding.services.model.PriceRecord;
import com.spglobal.coding.utils.ChunkProcessor;
import com.spglobal.coding.utils.PriceRecordFactory;
import com.spglobal.coding.utils.enums.InstrumentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws InterruptedException {
        InstrumentPriceService instrumentPriceService = new InstrumentPriceService();
        ChunkProcessor chunkProcessor = new ChunkProcessor(instrumentPriceService);

        InstrumentProducer producer = new InstrumentProducer(chunkProcessor);
        InstrumentConsumer consumer = new InstrumentConsumer(instrumentPriceService);

        for (int i = 0; i < 2; i++) {
            List<PriceRecord> priceRecordList = PriceRecordFactory.generatePriceRecordBatch();
            try {
                String batchId = producer.startNewBatch();
                producer.uploadRecords(batchId, priceRecordList);
                producer.completeBatch(batchId);
            } catch (Exception e) {
                logger.info("Unexpected error while producer generation: {}", e.getMessage());
            }
        }

        Thread.sleep(3000);

        GetPriceRecordsListResponse priceRecordList = consumer.getPriceRecordsByInstrumentType(InstrumentType.STOCK);

        String randomInstrumentId = PriceRecordFactory.getRandomInstrumentId();
        GetPriceRecordResponse priceRecordWithInstrumentId = consumer.getPriceRecordByInstrumentId(randomInstrumentId);

        randomInstrumentId = PriceRecordFactory.getRandomInstrumentId();
        GetPriceRecordResponse priceRecordWithInstrumentIdAndType = consumer.getPriceRecordByInstrumentId(randomInstrumentId, InstrumentType.STOCK);

        GetPriceRecordsListResponse priceRecordsWithinDuration = consumer.getPriceRecordsInLastDuration(Duration.of(10, ChronoUnit.SECONDS));

        logger.debug("{} {} {} {}", priceRecordsWithinDuration, priceRecordList, priceRecordWithInstrumentIdAndType, priceRecordWithInstrumentId);

    }

    //Unit test
    // Scaling Throughput options
}