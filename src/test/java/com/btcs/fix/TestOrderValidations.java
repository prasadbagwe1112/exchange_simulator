package com.btcs.fix;

import static org.junit.jupiter.api.Assertions.*;

import com.btcs.utils.Order;
import com.btcs.utils.OrderBook;
import com.btcs.utils.OrderRepository;
import com.btcs.utils.SymbolConfigLoader;
import com.btcs.utils.ValidationResult;
import java.math.BigDecimal;
import java.util.Date;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import quickfix.field.OrderQty;
import quickfix.field.OrdType;
import quickfix.field.Price;
import quickfix.field.Side;
import quickfix.field.StopPx;
import quickfix.field.Symbol;
import quickfix.field.TimeInForce;
import quickfix.field.TransactTime;

class TestOrderValidations {

    @BeforeAll
    static void setup() throws Exception {
        SymbolConfigLoader.load("config/instruments.csv");
    }

    @Test
    void validLimitOrder_shouldPass() {
        OrderValidations validations = new OrderValidations();

        ValidationResult result = validations.validateNewOrder(
                new OrdType(OrdType.LIMIT),
                new TimeInForce(TimeInForce.DAY),
                new Symbol("RELIANCE"),
                null,
                new Price(1419.00),
                new OrderQty(100),
                new StopPx(0), // passing 0 for limit order
                new OrderBook("RELIANCE"),
                new Side(Side.BUY),
                new TransactTime()
        );

        assertTrue(result.isValid(), result.getRejectReason());
    }

    @Test
    void invalidSymbol_shouldReject() {
        OrderValidations validations = new OrderValidations();

        ValidationResult result = validations.validateNewOrder(
                new OrdType(OrdType.LIMIT),
                new TimeInForce(TimeInForce.DAY),
                new Symbol("UNKNOWN"),
                null,
                new Price(100.00),
                new OrderQty(10),
                null,
                new OrderBook("UNKNOWN"),
                new Side(Side.BUY),
                new TransactTime()
        );

        assertFalse(result.isValid());
        assertTrue(result.getRejectReason().contains("Symbol not supported"));
    }

    @Test
    void buyStopOrder_shouldRejectWhenStopPriceIsNotAboveLtp() {
        OrderValidations validations = new OrderValidations();
        OrderBook book = new OrderBook("RELIANCE");
        book.setLastTradedPrice(new BigDecimal("1419.00"));

        ValidationResult result = validations.validateNewOrder(
                new OrdType(OrdType.STOP_LIMIT),
                new TimeInForce(TimeInForce.DAY),
                new Symbol("RELIANCE"),
                null,
                new Price(1418.50),
                new OrderQty(50),
                new StopPx(1419.00),
                book,
                new Side(Side.BUY),
                new TransactTime()
        );

        assertFalse(result.isValid());
        assertTrue(result.getRejectReason().contains("Buy StopPx must be > LTP"));
    }
}