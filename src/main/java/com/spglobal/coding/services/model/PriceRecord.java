package com.spglobal.coding.services.model;

import com.spglobal.coding.utils.enums.InstrumentType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Represents a record of a financial instrument's price at a specific point in time.
 * <p>
 * This class holds details about a price record, including:
 * <ul>
 *     <li>A unique identifier for the price record ({@code id}).</li>
 *     <li>The name of the financial instrument ({@code instrument}).</li>
 *     <li>The unique identifier for the instrument ({@code instrumentId}).</li>
 *     <li>The type of instrument ({@code instrumentType}).</li>
 *     <li>The last time the price was updated ({@code lastUpdateTime}).</li>
 *     <li>The latest recorded price of the instrument ({@code latestPrice}).</li>
 *     <li>A history of the price's payload, which consists of the value and currency ({@code payloadHistory}).</li>
 * </ul>
 * <p>
 * The class is immutable except for the fields {@code lastUpdateTime} and {@code latestPrice},
 * which can be updated using setter methods to reflect new price data. The price history is maintained using
 * a {@link ConcurrentSkipListSet}, ordered by the timestamp of each payload in reverse chronological order.
 * The history keeps track of the last 10 payloads.
 * <p>
 * It is recommended to use the Builder pattern to extend this class in the future for more flexible object construction.
 */
public class PriceRecord {
    private final String id;                // Unique ID for each PriceRecord
    private final String instrument;        // Name of the instrument
    private final String instrumentId;      // Unique ID for the instrument
    private final InstrumentType instrumentType;
    private  LocalDateTime lastUpdateTime;
    private  BigDecimal latestPrice;
    private final SortedSet<Payload> payloadHistory;

    public PriceRecord(String instrument,
                       String instrumentId,
                       InstrumentType instrumentType,
                       LocalDateTime lastUpdateTime,
                       BigDecimal latestPrice,
                       Payload initialPayload) {
        this.id = UUID.randomUUID().toString();
        this.instrument = instrument;
        this.instrumentId = instrumentId;
        this.instrumentType = instrumentType;
        this.lastUpdateTime = lastUpdateTime;
        this.latestPrice = latestPrice;
        this.payloadHistory = new ConcurrentSkipListSet<>(Comparator.comparing(Payload::getAsOf).reversed());
        payloadHistory.add(initialPayload);
    }

    public String getId() {
        return id;
    }

    public String getInstrument() {
        return instrument;
    }

    public String getInstrumentId() {
        return instrumentId;
    }

    public InstrumentType getInstrumentType() {
        return instrumentType;
    }

    public LocalDateTime getLastUpdateTime() {
        return lastUpdateTime;
    }

    public BigDecimal getLatestPrice() {
        return latestPrice;
    }

    public SortedSet<Payload> getPayloadHistory() {
        return payloadHistory;
    }

    public void setLastUpdateTime(LocalDateTime lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }

    public void setLatestPrice(BigDecimal latestPrice) {
        this.latestPrice = latestPrice;
    }

    @Override
    public String toString() {
        return "PriceRecord{" +
                "id='" + id + '\'' +
                ", instrument='" + instrument + '\'' +
                ", instrumentId='" + instrumentId + '\'' +
                ", instrumentType=" + instrumentType +
                ", lastUpdateTime=" + lastUpdateTime +
                ", payload=" + payloadHistory +
                '}';
    }
}
