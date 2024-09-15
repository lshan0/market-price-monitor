package com.spglobal.coding.utils;

import com.spglobal.coding.services.model.PriceRecord;
import com.spglobal.coding.utils.dto.UpdatePriceRecordRequest;
import com.spglobal.coding.utils.enums.Currency;
import com.spglobal.coding.utils.enums.InstrumentType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Utility class for generating random {@link PriceRecord} and {@link UpdatePriceRecordRequest} objects.
 * <p>
 * This class is responsible for creating random price records and update requests in batches.
 * It is primarily used to simulate large sets of data for testing purposes.
 * </p>
 */
public class UpdatePriceRecordRequestFactory {
    private static final String[] stocks = {"Apple Inc.", "Microsoft Corporation", "Amazon.com Inc.", "Alphabet Inc.", "Tesla Inc.", "Johnson & Johnson", "Berkshire Hathaway Inc.", "Visa Inc.", "Procter & Gamble Co.", "NVIDIA Corporation"};
    private static final String[] commodities = {"Gold", "Silver", "Crude Oil", "Natural Gas", "Copper", "Aluminum", "Platinum", "Palladium", "Corn", "Wheat"};

    private static final Random RANDOM = new Random();
    private static final Map<InstrumentType, String[]> map = generateInstrumentsMap();

    // Private constructor to prevent instantiation
    private UpdatePriceRecordRequestFactory() {
    }

    // Map stocks and commodities to their respective InstrumentType
    private static Map<InstrumentType, String[]> generateInstrumentsMap() {
        Map<InstrumentType, String[]> instrumentsMap = new EnumMap<>(InstrumentType.class);
        instrumentsMap.put(InstrumentType.STOCK, stocks);
        instrumentsMap.put(InstrumentType.COMMODITIES, commodities);
        return Collections.unmodifiableMap(instrumentsMap);
    }

    // Generate a batch of random UpdatePriceRecordRequest instances
    public static List<UpdatePriceRecordRequest> generateUpdatePriceRecordRequestBatch() {
        final List<UpdatePriceRecordRequest> requests = new ArrayList<>();
        for (int i = 0; i < 10000; i++) {  // Change the batch size as per your requirement
            final UpdatePriceRecordRequest request = createRandomUpdatePriceRecordRequest();
            requests.add(request);
        }
        return requests;
    }

    // Generate a random UpdatePriceRecordRequest
    public static UpdatePriceRecordRequest createRandomUpdatePriceRecordRequest() {
        final int id = RANDOM.nextInt(10000); // Generate a random id
        final InstrumentType instrumentType = getRandomInstrumentType();
        final String instrument = getRandomInstrument(instrumentType);
        final BigDecimal value = BigDecimal.valueOf(RANDOM.nextDouble() * 1000);
        final Currency currency = Currency.values()[RANDOM.nextInt(Currency.values().length)];
        final LocalDateTime requestTime = LocalDateTime.now()
                .minusDays(RANDOM.nextInt(20))
                .minusHours(RANDOM.nextInt(24))
                .minusMinutes(RANDOM.nextInt(60));

        return new UpdatePriceRecordRequest.Builder()
                .setId(id)
                .setInstrument(instrument)
                .setInstrumentType(instrumentType)
                .setValue(value)
                .setCurrency(currency)
                .setRequestTime(requestTime)
                .build();
    }

    // Generate a consistent instrument ID based on the instrument name
    static String generateIdFromInstrument(final String instrumentName) {
        return instrumentName.replaceAll("\\s+", "_").toUpperCase();
    }

    // Get a random instrument type, either STOCK or COMMODITIES
    private static InstrumentType getRandomInstrumentType() {
        return RANDOM.nextBoolean() ? InstrumentType.STOCK : InstrumentType.COMMODITIES;
    }

    // Get a random instrument from the map based on the type
    private static String getRandomInstrument(final InstrumentType type) {
        final String[] items = map.get(type);
        return items[RANDOM.nextInt(items.length)];
    }

    public static InstrumentType getInstrumentTypeFromId(String instrumentId) {
        // Check if the instrument ID belongs to the stocks or commodities list
        if (instrumentId != null) {
            for (String stock : stocks) {
                if (generateIdFromInstrument(stock).equals(instrumentId)) {
                    return InstrumentType.STOCK;
                }
            }
            for (String commodity : commodities) {
                if (generateIdFromInstrument(commodity).equals(instrumentId)) {
                    return InstrumentType.COMMODITIES;
                }
            }
        }
        throw new IllegalArgumentException("Instrument ID not found or unknown type: " + instrumentId);
    }

    public static String getRandomInstrumentId() {
        InstrumentType type = getRandomInstrumentType();
        String instrumentName = getRandomInstrument(type);
        return generateIdFromInstrument(instrumentName);
    }
}
