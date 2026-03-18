package com.btcs.utils;

import java.math.BigDecimal;

public class SymbolConfig {

    private String symbol;
    private BigDecimal refPrice;
    private BigDecimal tickSize;

    public SymbolConfig(String symbol, BigDecimal refPrice, BigDecimal tickSize) {
        this.symbol = symbol;
        this.refPrice = refPrice;
        this.tickSize = tickSize;
    }

    public String getSymbol() {
        return symbol;
    }

    public BigDecimal getRefPrice() {
        return refPrice;
    }

    public BigDecimal getTickSize() {
        return tickSize;
    }
}