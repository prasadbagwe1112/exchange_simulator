package com.btcs.fix;

import static org.junit.jupiter.api.Assertions.*;

import com.btcs.utils.Order;
import com.btcs.utils.OrderBook;
import com.btcs.utils.OrderBookManager;
import com.btcs.utils.OrderRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import quickfix.SessionID;
import quickfix.field.OrdType;
import quickfix.field.TimeInForce;

class TestStopOrderManager {

    private StopOrderManager manager;
    private OrderBook book;

    @BeforeEach
    void setup() {
        OrderRepository repo = OrderRepository.getInstance();
        manager = new StopOrderManager(OrderBookManager.getInstance(), new Executions(), repo);

        book = OrderBookManager.getInstance().getBook("RELIANCE");
        book.getBuyBook().clear();
        book.getSellBook().clear();
    }

    @Test
    void onTrade_shouldActivateTriggeredBuyStopOrder() {
        String clOrdId = "STOP-BUY-1";
        OrderRepository repo = OrderRepository.getInstance();
        repo.removeOrder(clOrdId);

        Order stopOrder = new Order(
                "ACC-1",
                "ORD-STOP-1",
                clOrdId,
                "RELIANCE",
                '1',
                new BigDecimal("105.00"),
                new BigDecimal("100.00"),
                new BigDecimal("10"),
                new BigDecimal("10"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                '0',
                TimeInForce.DAY,
                OrdType.STOP_LIMIT,
                new SessionID("FIX.4.4", "CLIENT", "SERVER")
        );

        repo.saveNewOrder(
                "ACC-1",
                "ORD-STOP-1",
                clOrdId,
                "RELIANCE",
                '1',
                new BigDecimal("105.00"),
                new BigDecimal("100.00"),
                new BigDecimal("10"),
                new BigDecimal("10"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                '0',
                TimeInForce.DAY,
                OrdType.STOP_LIMIT,
                new SessionID("FIX.4.4", "CLIENT", "SERVER")
        );

        manager.addStopOrder(stopOrder, new BigDecimal("100.00"));
        manager.onTrade("RELIANCE", new BigDecimal("100.00"));

        Order triggered = repo.getOrder(clOrdId);
        assertNotNull(triggered);
        assertEquals('2', triggered.getOrdType());
        assertTrue(book.getBuyBook().containsKey(new BigDecimal("105.00")));
    }
}