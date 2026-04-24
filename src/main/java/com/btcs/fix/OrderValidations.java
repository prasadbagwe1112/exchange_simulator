package com.btcs.fix;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.btcs.utils.*;
import quickfix.field.*;

public class OrderValidations {

    private static final List<String> ALLOWED_SYMBOLS =
            List.of("RELIANCE", "HDFCBANK", "BHARTIARTL", "TCS", "MRF");

    private static final Set<Character> ALLOWED_ORD_TYPES =
            Set.of(OrdType.LIMIT, OrdType.MARKET, OrdType.STOP_LIMIT, OrdType.STOP_STOP_LOSS);

    private static final Set<Character> LIMIT_MKT =
            Set.of(OrdType.LIMIT, OrdType.MARKET);

    private static final Set<Character> STOP_ORD =
            Set.of(OrdType.STOP_LIMIT, OrdType.STOP_STOP_LOSS);

    private static final Set<Character> ALLOWED_TIFS =
            Set.of(TimeInForce.DAY, TimeInForce.GOOD_TILL_CANCEL,
                    TimeInForce.IMMEDIATE_OR_CANCEL, TimeInForce.FILL_OR_KILL);

    private static final Set<Character> IOC_FOK =
            Set.of(TimeInForce.IMMEDIATE_OR_CANCEL, TimeInForce.FILL_OR_KILL);

    /* -------- NEW ORDER VALIDATION -------- */
    public ValidationResult validateNewOrder(
            OrdType ordType,
            TimeInForce tif,
            Symbol symbol,
            Order storedOrder,
            Price price,
            StopPx stopPx,
            OrderBook book,
            Side side
    ) {

        char ordTypeVal = ordType.getValue();
        char tifVal = tif.getValue();
        char sideVal = side.getValue();

        // basic validations
        ValidationResult result = validateBasic(ordTypeVal, tifVal, storedOrder);
        if (result != null) return result;

        // symbol config
        SymbolConfig config = getSymbolConfig(symbol);
        if (config == null) {
            return ValidationResult.reject(
                    "Symbol not supported, use any one from - " + ALLOWED_SYMBOLS);
        }

        // price validations
        result = validatePrice(ordTypeVal, symbol, price, stopPx, config);
        if (result != null) return result;

        // stop validations
        result = validateStopConditions(ordTypeVal, sideVal, price, stopPx, book);
        if (result != null) return result;

        return ValidationResult.ok();
    }

    /* -------- REPLACE VALIDATION -------- */
    public ValidationResult validateReplace(
            Order storedOrder,
            OrdType ordType,
            Symbol symbol,
            Side side,
            TimeInForce tif,
            OrderQty orderQty,
            Price price,
            StopPx stopPx,
            Account account
    ) {

        if (storedOrder == null) {
            return ValidationResult.reject("Order not found");
        }

        // immutable fields validation
        ValidationResult result = validateImmutableFields(storedOrder, ordType, side, tif, account);
        if (result != null) return result;

        // quantity validation
        result = validateQuantity(storedOrder, orderQty);
        if (result != null) return result;

        // symbol validation
        if (!storedOrder.getSymbol().equals(symbol.getValue())) {
            return ValidationResult.reject("Symbol cannot be replaced");
        }

        SymbolConfig config = SymbolConfigLoader.get(symbol.getValue());

        // price validation
        result = validatePrice(ordType.getValue(), symbol, price, stopPx, config);
        if (result != null) return result;

        return ValidationResult.ok();
    }

    /* -------- CANCEL VALIDATION -------- */
    public ValidationResult validateCancelorOSR(Order storedOrder) {
        return (storedOrder == null)
                ? ValidationResult.reject("Order not found")
                : ValidationResult.ok();
    }

    /* ----------------- Helper methods ------------------------- */

    private ValidationResult validateBasic(char ordType, char tif, Order storedOrder) {

        if (!ALLOWED_ORD_TYPES.contains(ordType)) {
            return ValidationResult.reject("Invalid Order Type");
        }

        if (!ALLOWED_TIFS.contains(tif)) {
            return ValidationResult.reject("Invalid Time In Force");
        }

        if (IOC_FOK.contains(tif) && !LIMIT_MKT.contains(ordType)) {
            return ValidationResult.reject("Invalid Order Type for IOC/FOK Order");
        }

        if (storedOrder != null) {
            return ValidationResult.reject("Duplicate ClOrdID");
        }

        return null;
    }

    private SymbolConfig getSymbolConfig(Symbol symbol) {
        return SymbolConfigLoader.get(symbol.getValue());
    }

    private ValidationResult validatePrice(char ordType, Symbol symbol,
                                           Price price, StopPx stopPx, SymbolConfig config) {

        BigDecimal px = BigDecimal.valueOf(price.getValue());
        BigDecimal stpPx = BigDecimal.valueOf(stopPx.getValue());
        BigDecimal ref = config.getRefPrice();
        BigDecimal tickSize = config.getTickSize();
        BigDecimal upper = ref.multiply(BigDecimal.valueOf(1.10));
        BigDecimal lower = ref.multiply(BigDecimal.valueOf(0.90));

        if (isPriceRequired(ordType)) {
            if (price.getValue() <= 0) {
                return ValidationResult.reject("Invalid Price");
            }

            if (!isValidTick(px, tickSize)) {
                return ValidationResult.reject(
                        "Invalid Tick Size, " + symbol.getValue() + ":" + tickSize);
            }

            if (px.compareTo(upper) > 0 || px.compareTo(lower) < 0) {
                return ValidationResult.reject(
                        "Invalid Price: outside ±10% band of reference price (" + ref + ")");
            }
        } else if (isStopPriceRequired(ordType)) {
            if (stopPx.getValue() <= 0) {
                return ValidationResult.reject("Invalid Stop Price");
            }

            if (!isValidTick(stpPx, tickSize)) {
                return ValidationResult.reject(
                        "Invalid Tick Size, " + symbol.getValue() + ":" + tickSize);
            }

            if ((stpPx.compareTo(upper) > 0 || stpPx.compareTo(lower) < 0)) {
                return ValidationResult.reject(
                        "Invalid Stop Price: outside ±10% band of reference price (" + ref + ")");
            }
        }

        return null;
    }

    private ValidationResult validateStopConditions(char ordType, char side,
                                                    Price price, StopPx stopPx,
                                                    OrderBook book) {

        if (!STOP_ORD.contains(ordType) || stopPx == null || book.getLastTradedPrice() == null) {
            return null;
        }

        BigDecimal ltp = book.getLastTradedPrice();
        BigDecimal stop = BigDecimal.valueOf(stopPx.getValue());

        if (side == Side.BUY && ltp.compareTo(stop) >= 0) {
            return ValidationResult.reject("Buy StopPx must be > LTP");
        }

        if (side == Side.SELL && ltp.compareTo(stop) <= 0) {
            return ValidationResult.reject("Sell StopPx must be < LTP");
        }

        if (ordType == OrdType.STOP_LIMIT && price != null) {
            double px = price.getValue();

            if ((side == Side.BUY && px < stopPx.getValue()) ||
                    (side == Side.SELL && px > stopPx.getValue())) {
                return ValidationResult.reject("Invalid Price vs StopPx relation");
            }
        }

        return null;
    }

    private ValidationResult validateImmutableFields(Order storedOrder,
                                                     OrdType ordType,
                                                     Side side,
                                                     TimeInForce tif, Account account) {

        if (ordType != null && storedOrder.getOrdType() != ordType.getValue()) {
            return ValidationResult.reject("Order Type cannot be replaced");
        }

        if (side != null && storedOrder.getSide() != side.getValue()) {
            return ValidationResult.reject("Side cannot be replaced");
        }

        if (tif != null && storedOrder.getTimeInForce() != tif.getValue()) {
            return ValidationResult.reject("Time In Force cannot be replaced");
        }

        if (account != null && !storedOrder.getAccount().equals(account.getValue())) {
            return ValidationResult.reject("Account cannot be replaced");
        }

        return null;
    }

    private ValidationResult validateQuantity(Order storedOrder, OrderQty orderQty) {

        BigDecimal leavesQty = BigDecimal.valueOf(orderQty.getValue())
                .subtract(storedOrder.getCumQty());

        if (leavesQty.compareTo(BigDecimal.ZERO) <= 0) {
            return ValidationResult.reject("Invalid Order Quantity");
        }

        return null;
    }

    private boolean isPriceRequired(char ordType) {
        return ordType == OrdType.LIMIT || ordType == OrdType.STOP_LIMIT;
    }

    private boolean isStopPriceRequired(char ordType) {
        return ordType == OrdType.STOP_STOP_LOSS || ordType == OrdType.STOP_LIMIT;
    }

    private boolean isValidTick(BigDecimal price, BigDecimal tickSize) {
        return price.remainder(tickSize).compareTo(BigDecimal.ZERO) == 0;
    }
}