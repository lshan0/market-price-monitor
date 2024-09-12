package com.spglobal.coding.services.model;

import com.spglobal.coding.utils.enums.Currency;

import java.math.BigDecimal;

/**
 * Represents the payload of a financial instrument's price record, consisting of the value and currency.
 */
public class Payload {
    private final BigDecimal value;
    private final Currency currency;

    public Payload(BigDecimal value, Currency currency) {
        this.value = value;
        this.currency = currency;
    }

    public BigDecimal getValue() {
        return value;
    }

    public Currency getCurrency() {
        return currency;
    }
}
