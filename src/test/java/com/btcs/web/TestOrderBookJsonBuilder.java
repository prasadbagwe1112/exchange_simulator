package com.btcs.web;

import static org.junit.jupiter.api.Assertions.*;

import com.btcs.utils.OrderBookSnapshot;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class TestOrderBookJsonBuilder {

    @Test
    void build_shouldSerializeBidAndAskLevels() {
        OrderBookSnapshot snap = new OrderBookSnapshot();
        snap.symbol = "RELIANCE";
        snap.ltp = new BigDecimal("100.75");

        snap.bids = List.of(level(new BigDecimal("101.00"), new BigDecimal("12")),
                level(new BigDecimal("100.50"), new BigDecimal("7")));
        snap.asks = List.of(level(new BigDecimal("101.25"), new BigDecimal("9")));

        String json = OrderBookJsonBuilder.build(snap);

        assertTrue(json.contains("\"symbol\":\"RELIANCE\""));
        assertTrue(json.contains("\"ltp\":100.75"));
        assertTrue(json.contains("\"buy\":["));
        assertTrue(json.contains("\"sell\":["));
        assertTrue(json.contains("\"price\":101.00"));
        assertTrue(json.contains("\"qty\":12"));
    }

    private static OrderBookSnapshot.Level level(BigDecimal price, BigDecimal quantity) {
        OrderBookSnapshot.Level level = new OrderBookSnapshot.Level();
        level.price = price;
        level.quantity = quantity;
        return level;
    }
}