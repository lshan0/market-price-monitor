package com.spglobal.coding.services.model;

import com.spglobal.coding.utils.enums.Currency;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents the payload of a financial instrument's price record, consisting of the value and currency.
 */
public class Payload {
    private final int id;
    private final BigDecimal value;
    private final Currency currency;
    private final LocalDateTime asOf;

    public Payload(int id, BigDecimal value, Currency currency, LocalDateTime asOf) {
        this.id = id;
        this.value = value;
        this.currency = currency;
        this.asOf = asOf;
    }

    public int getId() {
        return id;
    }

    public BigDecimal getValue() {
        return value;
    }

    public Currency getCurrency() {
        return currency;
    }

    public LocalDateTime getAsOf() {
        return asOf;
    }
}
