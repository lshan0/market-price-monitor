package com.spglobal.coding.utils.dto;

import com.spglobal.coding.utils.enums.Currency;
import com.spglobal.coding.utils.enums.InstrumentType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO representing a request to update a price record.
 * <p>
 * This class is immutable, meaning once an instance is created, its state cannot be modified.
 * It follows the Builder pattern to allow flexible and readable construction of instances.
 * </p>
 * <p>
 * The {@code UpdatePriceRecordRequest} contains information about an instrument's price update,
 * including the instrument's ID, instrument's name, its type, the value of the price,
 * the currency in which the price is expressed, and the time when the request was made.
 * </p>
 */
public class UpdatePriceRecordRequest {
    private final int id;
    private final String instrument;
    private final InstrumentType instrumentType;
    private final BigDecimal value;
    private final Currency currency;
    private final LocalDateTime requestTime;

    private UpdatePriceRecordRequest(Builder builder) {
        this.id = builder.id;
        this.instrument = builder.instrument;
        this.instrumentType = builder.instrumentType;
        this.value = builder.value;
        this.currency = builder.currency;
        this.requestTime = builder.requestTime;
    }

    public int getId() {
        return id;
    }

    public String getInstrument() {
        return instrument;
    }

    public InstrumentType getInstrumentType() {
        return instrumentType;
    }

    public BigDecimal getValue() {
        return value;
    }

    public Currency getCurrency() {
        return currency;
    }

    public LocalDateTime getRequestTime() {
        return requestTime;
    }

    @Override
    public String toString() {
        return "UpdatePriceRecordRequest{" +
                "id=" + id +
                ", instrument='" + instrument + '\'' +
                ", instrumentType=" + instrumentType +
                ", value=" + value +
                ", currency=" + currency +
                ", requestTime=" + requestTime +
                '}';
    }

    public static class Builder {
        private int id;
        private String instrument;
        private InstrumentType instrumentType;
        private BigDecimal value;
        private Currency currency;
        private LocalDateTime requestTime;

        public Builder setId(int id) {
            this.id = id;
            return this;
        }

        public Builder setInstrument(String instrument) {
            this.instrument = instrument;
            return this;
        }

        public Builder setInstrumentType(InstrumentType instrumentType) {
            this.instrumentType = instrumentType;
            return this;
        }

        public Builder setValue(BigDecimal value) {
            this.value = value;
            return this;
        }

        public Builder setCurrency(Currency currency) {
            this.currency = currency;
            return this;
        }

        public Builder setRequestTime(LocalDateTime requestTime) {
            this.requestTime = requestTime;
            return this;
        }

        public UpdatePriceRecordRequest build() {
            return new UpdatePriceRecordRequest(this);
        }
    }
}
