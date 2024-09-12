package com.spglobal.coding.services.model;

import java.time.LocalDateTime;

public class PriceRecord {
    private final String id;
    private final LocalDateTime asOf;
    private final Payload payload;

    public PriceRecord(String id, LocalDateTime asOf, Payload payload) {
        this.id = id;
        this.asOf = asOf;
        this.payload = payload;
    }

    public String getId() {
        return id;
    }

    public LocalDateTime getAsOf() {
        return asOf;
    }

    public Payload getPayload() {
        return payload;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PriceRecord record = (PriceRecord) o;
        return id.equals(record.id) &&
               asOf.equals(record.asOf) &&
               payload.getValue().equals(record.getPayload().getValue());
    }
}
