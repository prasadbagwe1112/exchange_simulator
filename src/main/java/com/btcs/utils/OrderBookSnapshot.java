package com.btcs.utils;

import java.math.BigDecimal;
import java.util.List;

public class OrderBookSnapshot {
	public BigDecimal ltp;
    public String symbol;
    public List<Level> bids;
    public List<Level> asks;

    public static class Level {
        public BigDecimal price;
        public BigDecimal quantity;
    }
}
