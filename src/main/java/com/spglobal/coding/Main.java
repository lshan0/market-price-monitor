package com.spglobal.coding;

import com.spglobal.coding.producers.InstrumentProducer;
import com.spglobal.coding.services.InstrumentPriceService;
import com.spglobal.coding.services.model.PriceRecord;
import com.spglobal.coding.utils.PriceRecordFactory;
import com.spglobal.coding.utils.enums.InstrumentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        InstrumentPriceService instrumentPriceService = new InstrumentPriceService();
        InstrumentProducer producer = new InstrumentProducer(instrumentPriceService);
        Map<InstrumentType, Map<String, PriceRecord>> map = InstrumentPriceService.latestPrices;

        while (true) {
            List<PriceRecord> priceRecordList = PriceRecordFactory.generatePriceRecordBatch();

            try {
                String batchId = producer.startNewBatch();
                producer.uploadRecords(batchId, priceRecordList);
                producer.completeBatch(batchId);
                return;
            } catch (Exception e) {
                logger.info("Unexpected error while producer generation: {}", e.getMessage());
            }
        }
    }
}