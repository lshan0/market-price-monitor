package com.spglobal.coding.services.model;

import com.spglobal.coding.utils.enums.InstrumentType;

import java.time.LocalDateTime;

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
    private final LocalDateTime asOf;
    private final Payload payload;

    public PriceRecord(String id,
                       String instrument,
                       String instrumentId,
                       InstrumentType instrumentType,
                       LocalDateTime asOf,
                       Payload payload) {
        this.id = id;
        this.instrument = instrument;
        this.instrumentId = instrumentId;
        this.instrumentType = instrumentType;
        this.asOf = asOf;
        this.payload = payload;
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

    public LocalDateTime getAsOf() {
        return asOf;
    }

    public Payload getPayload() {
        return payload;
    }

    @Override
    public String toString() {
        return "PriceRecord{" +
                "id='" + id + '\'' +
                ", instrument='" + instrument + '\'' +
                ", instrumentId='" + instrumentId + '\'' +
                ", instrumentType=" + instrumentType +
                ", asOf=" + asOf +
                ", payload=" + payload +
                '}';
    }
}
