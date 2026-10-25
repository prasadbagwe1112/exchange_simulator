package com.btcs.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class TestOrderRepository {

    @Test
    void saveNewOrder_shouldPersistByClOrdId() {
        OrderRepository repo = OrderRepository.getInstance();
        String clOrdId = "REPO-TEST-1";

        repo.removeOrder(clOrdId);

        repo.saveNewOrder(
                "ACC-1",
                "ORD-1",
                clOrdId,
                "RELIANCE",
                '1',
                new BigDecimal("100.00"),
                BigDecimal.ZERO,
                new BigDecimal("10"),
                new BigDecimal("10"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                '0',
                '0',
                '2',
                null
        );

        Order stored = repo.getOrder(clOrdId);

        assertNotNull(stored);
        assertEquals("RELIANCE", stored.getSymbol());
        assertEquals(clOrdId, stored.getClOrdId());
        assertEquals(new BigDecimal("10"), stored.getOrderQty());
    }

    @Test
    void addOrder_shouldReplaceExistingOrder() {
        OrderRepository repo = OrderRepository.getInstance();
        String clOrdId = "REPO-TEST-2";

        repo.removeOrder(clOrdId);

        Order first = new Order(
                "ACC-1", "ORD-1", clOrdId, "RELIANCE",
                '1', new BigDecimal("100.00"), BigDecimal.ZERO,
                new BigDecimal("10"), new BigDecimal("10"), BigDecimal.ZERO,
                BigDecimal.ZERO, '0', '0', '2', null
        );

        repo.addOrder(clOrdId, first);

        Order updated = new Order(
                "ACC-1", "ORD-2", clOrdId, "RELIANCE",
                '1', new BigDecimal("101.00"), BigDecimal.ZERO,
                new BigDecimal("15"), new BigDecimal("15"), BigDecimal.ZERO,
                BigDecimal.ZERO, '0', '0', '2', null
        );

        repo.addOrder(clOrdId, updated);

        assertEquals("ORD-2", repo.getOrder(clOrdId).getOrderId());
        assertEquals(new BigDecimal("101.00"), repo.getOrder(clOrdId).getPrice());
    }
}