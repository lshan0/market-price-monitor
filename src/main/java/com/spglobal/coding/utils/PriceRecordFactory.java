package com.spglobal.coding.utils;

import com.spglobal.coding.services.model.Payload;
import com.spglobal.coding.services.model.PriceRecord;
import com.spglobal.coding.utils.enums.Currency;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class PriceRecordFactory {

    private static final Random RANDOM = new Random();

    public static List<PriceRecord> generatePriceRecordBatch() {
        List<PriceRecord> priceRecords = new ArrayList<>();

        for (int i = 0; i < 1000; i++) { // Generates 1000 random price records
            PriceRecord record = createRandomPriceRecord();
            priceRecords.add(record);
        }

        return priceRecords;
    }

    private static PriceRecord createRandomPriceRecord() {
        String id = UUID.randomUUID().toString(); // Generates a random UUID for the id
        LocalDateTime asOf = LocalDateTime.now().minusDays(RANDOM.nextInt(30)); // Random timestamp within the past 30 days

        BigDecimal value = BigDecimal.valueOf(RANDOM.nextDouble() * 1000); // Random value between 0 and 1000
        Currency currency = Currency.values()[RANDOM.nextInt(Currency.values().length)]; // Random currency from the enum

        Payload payload = new Payload(value, currency);
        return new PriceRecord(id, asOf, payload);
    }
}
