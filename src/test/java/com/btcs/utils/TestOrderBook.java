package com.btcs.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import quickfix.SessionID;

class TestOrderBook {

    @Test
    void addOrder_shouldPutBuyOrderInBestBidQueue() {
        OrderBook book = new OrderBook("RELIANCE");

        book.add(createOrder("B1", '1', new BigDecimal("100.50"), new BigDecimal("10")));

        assertEquals(1, book.getBuyBook().size());
        assertEquals(new BigDecimal("100.50"), book.getBuyBook().firstKey());
        assertEquals(1, book.getBuyBook().firstEntry().getValue().size());
    }

    @Test
    void removeOrder_shouldRemoveOrderAndClearEmptyLevel() {
        OrderBook book = new OrderBook("RELIANCE");
        Order buyOrder = createOrder("B1", '1', new BigDecimal("100.50"), new BigDecimal("10"));

        book.add(buyOrder);

        assertTrue(book.removeOrder("B1"));
        assertTrue(book.getBuyBook().isEmpty());
    }

    @Test
    void snapshotTop5_shouldAggregateQtyPerPriceLevelAndSortByPrice() {
        OrderBook book = new OrderBook("RELIANCE");

        book.add(createOrder("B1", '1', new BigDecimal("100.50"), new BigDecimal("10")));
        book.add(createOrder("B2", '1', new BigDecimal("100.50"), new BigDecimal("5")));
        book.add(createOrder("B3", '1', new BigDecimal("101.00"), new BigDecimal("12")));
        book.add(createOrder("S1", '2', new BigDecimal("101.50"), new BigDecimal("8")));

        OrderBookSnapshot snapshot = book.snapshotTop5();

        assertEquals("RELIANCE", snapshot.symbol);
        assertEquals(2, snapshot.bids.size());
        assertEquals(new BigDecimal("101.00"), snapshot.bids.get(0).price);
        assertEquals(new BigDecimal("12"), snapshot.bids.get(0).quantity);
        assertEquals(new BigDecimal("100.50"), snapshot.bids.get(1).price);
        assertEquals(new BigDecimal("15"), snapshot.bids.get(1).quantity);

        assertEquals(1, snapshot.asks.size());
        assertEquals(new BigDecimal("101.50"), snapshot.asks.get(0).price);
        assertEquals(new BigDecimal("8"), snapshot.asks.get(0).quantity);
    }

    private Order createOrder(String clOrdId, char side, BigDecimal price, BigDecimal qty) {
        return new Order(
                "ACC-1",
                "ORD-" + clOrdId,
                clOrdId,
                "RELIANCE",
                side,
                price,
                BigDecimal.ZERO,
                qty,
                qty,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                '0',
                '0',
                '2',
                null
        );
    }
}