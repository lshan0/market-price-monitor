package com.spglobal.coding.utils;

import com.spglobal.coding.services.model.Payload;
import com.spglobal.coding.services.model.PriceRecord;
import com.spglobal.coding.utils.enums.Currency;
import com.spglobal.coding.utils.enums.InstrumentType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class PriceRecordFactory {

    private static final Random RANDOM = new Random();
    private static final List<String> stocks = getStocks();

    private PriceRecordFactory() {

    }

    public static List<PriceRecord> generatePriceRecordBatch() {
        final List<PriceRecord> priceRecords = new ArrayList<>();

        for (int i = 0; i < 10000; i++) { // Generates 10000 random price records
            final PriceRecord priceRecord = createRandomPriceRecord();
            priceRecords.add(priceRecord);
        }

        return priceRecords;
    }

    public static String getRandomInstrumentId() {
        final String instrument = stocks.get(RANDOM.nextInt(stocks.size()));
        return generateIdFromInstrument(instrument);
    }

    private static PriceRecord createRandomPriceRecord() {
        final String id = UUID.randomUUID().toString(); // Generates a random UUID for the id
        final String instrument = stocks.get(RANDOM.nextInt(stocks.size()));
        final LocalDateTime asOf = LocalDateTime.now();

        final BigDecimal value = BigDecimal.valueOf(RANDOM.nextDouble() * 1000); // Random value between 0 and 1000
        final Currency currency = Currency.values()[RANDOM.nextInt(Currency.values().length)]; // Random currency from the enum

        final Payload payload = new Payload(value, currency);
        return new PriceRecord(id, instrument, generateIdFromInstrument(instrument), InstrumentType.STOCK, asOf, payload);
    }

    private static String generateIdFromInstrument(final String instrumentName) {
        // Generate a consistent instrument ID based on the instrument name
        return instrumentName.replaceAll("\\s+", "_").toUpperCase();
    }



    private static List<String> getStocks() {
        return List.of(
                "Apple Inc.",
                "Microsoft Corporation",
                "Amazon.com Inc.",
                "Alphabet Inc.",
                "Tesla Inc.",
                "Johnson & Johnson",
                "Berkshire Hathaway Inc.",
                "Visa Inc.",
                "Procter & Gamble Co.",
                "NVIDIA Corporation"
        );
    }
}
