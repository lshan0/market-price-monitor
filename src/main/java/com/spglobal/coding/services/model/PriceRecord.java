package com.spglobal.coding.services.model;

import com.spglobal.coding.utils.enums.InstrumentType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Queue;
import java.util.SortedSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * Represents a record of a financial instrument's price at a specific point in time.
 * <p>
 * This class holds details about the price record, including a unique identifier,
 * the name and unique ID of the instrument, the type of instrument, the timestamp of the record,
 * and the payload containing the value and currency of the price.
 * <p>
 * The class is designed to be immutable and uses a constructor to initialize all fields.
 * It is recommended to use the Builder pattern for future extensibility.
 */
public class PriceRecord {
    private final String id;                // Unique ID for each PriceRecord
    private final String instrument;        // Name of the instrument
    private final String instrumentId;      // Unique ID for the instrument
    private final InstrumentType instrumentType;
    private  LocalDateTime lastUpdateTime;
    private  BigDecimal latestPrice;
    private final SortedSet<Payload> payloadHistory;

    private static final int HISTORY_SIZE = 10;

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
