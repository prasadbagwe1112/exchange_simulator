package com.btcs.fix;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import quickfix.field.*;

public class OrderValidations {
	
	private static final List<String> ALLOWED_SYMBOLS = List.of("RELIANCE", "HDFCBANK", "BHARTIARTL", "TCS", "MRF");
	private static final Set<Character> ALLOWED_ORD_TYPES = Set.of(OrdType.LIMIT, OrdType.MARKET, 
			OrdType.STOP_LIMIT, OrdType.STOP_STOP_LOSS);
	private static final Set<Character> STOP_ORD_TYPES = Set.of(OrdType.STOP_LIMIT, OrdType.STOP_STOP_LOSS);
	private static final Set<Character> ALLOWED_TIFS = Set.of(TimeInForce.DAY, TimeInForce.GOOD_TILL_CANCEL, 
			TimeInForce.IMMEDIATE_OR_CANCEL, TimeInForce.FILL_OR_KILL);

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
        if (!ALLOWED_ORD_TYPES.contains(ordType.getValue())) {
            return ValidationResult.reject("Invalid Order Type");
        }

        if (!ALLOWED_TIFS.contains(tif.getValue())) {
            return ValidationResult.reject("Invalid Time In Force");
        }
        
        if (isPriceRequired(ordType.getValue()) && (price == null || price.getValue() <= 0)) {
        	return ValidationResult.reject("Invalid Price");
        }
        
        if (storedOrder != null) {
        	return ValidationResult.reject("Duplicate ClOrdID");
        }
        
        if (!ALLOWED_SYMBOLS.contains(symbol.getValue())) {
        	return ValidationResult.reject("Symbol not supported, use any one from - " + ALLOWED_SYMBOLS);
        }
        
		if (side.getValue() == Side.BUY && STOP_ORD_TYPES.contains(ordType.getValue())
        		&& book.getLastTradedPrice() != null
        		&& stopPx.getValue() != 0
        		&& book.getLastTradedPrice().compareTo(BigDecimal.valueOf(stopPx.getValue())) >= 0) {
			return ValidationResult.reject("Invalid Trigger Price");
		}
		
		if (side.getValue() == Side.SELL && STOP_ORD_TYPES.contains(ordType.getValue())
            	&& book.getLastTradedPrice() != null
            	&& stopPx.getValue() != 0
            	&& book.getLastTradedPrice().compareTo(BigDecimal.valueOf(stopPx.getValue())) <= 0) {
			return ValidationResult.reject("Invalid Trigger Price");
		}
		
		if ((side.getValue() == Side.BUY 
				&& ordType.getValue() == OrdType.STOP_LIMIT
        		&& price.getValue() < stopPx.getValue()) ||
			(side.getValue() == Side.SELL 
				&& ordType.getValue() == OrdType.STOP_LIMIT
        		&& price.getValue() > stopPx.getValue())) {
			return ValidationResult.reject("Invalid Trigger Price");
		}
		
        return ValidationResult.ok();
    }
    
    /* -------- NEW ORDER VALIDATION -------- */
    public ValidationResult validateNewStopOrder(
            OrdType ordType,
            TimeInForce tif,
            Symbol symbol,
            Order storedOrder,
            Price price
    ) {
        if (!ALLOWED_ORD_TYPES.contains(ordType.getValue())) {
            return ValidationResult.reject("Invalid Order Type");
        }

        if (!ALLOWED_TIFS.contains(tif.getValue())) {
            return ValidationResult.reject("Invalid Time In Force");
        }
        
        if (isPriceRequired(ordType.getValue()) && (price == null || price.getValue() <= 0)) {
        	return ValidationResult.reject("Invalid Price");
        }
        
        if (storedOrder != null) {
        	return ValidationResult.reject("Duplicate ClOrdID");
        }
        
        if (!ALLOWED_SYMBOLS.contains(symbol.getValue())) {
        	return ValidationResult.reject("Symbol not supported, use any one from - " + ALLOWED_SYMBOLS);
        }
        return ValidationResult.ok();
    }

    /* -------- REPLACE VALIDATION -------- */
    public ValidationResult validateReplace(
            Order storedOrder,
            OrdType ordType,
            Symbol symbol,
            Side side,
            TimeInForce tif,
            OrderQty orderQty
    ) {
        if (storedOrder == null) {
            return ValidationResult.reject("Order not found");
        }

        if (ordType != null && (storedOrder.getOrdType() != ordType.getValue())) {
            return ValidationResult.reject("Order Type cannot be replaced");
        }

        if (symbol != null && !storedOrder.getSymbol().equals(symbol.getValue())) {
            return ValidationResult.reject("Symbol cannot be replaced");
        }

        if (side != null && side.getValue() != storedOrder.getSide()) {
            return ValidationResult.reject("Side cannot be replaced");
        }

        if (tif != null && tif.getValue() != storedOrder.getTimeInForce()) {
            return ValidationResult.reject("Time In Force cannot be replaced");
        }
        
        BigDecimal leavesQty = new BigDecimal(orderQty.getValue()).subtract(storedOrder.getCumQty());
        if (leavesQty.compareTo(BigDecimal.ZERO) <= 0) {
            return ValidationResult.reject("Invalid Order Quantity");
        }
        
        return ValidationResult.ok();
    }

    /* -------- CANCEL VALIDATION -------- */
    public ValidationResult validateCancelorOSR(Order storedOrder) {
        if (storedOrder == null) {
            return ValidationResult.reject("Order not found");
        }
        return ValidationResult.ok();
    }
    
    private boolean isPriceRequired(char ordType) {
    	return ordType == OrdType.LIMIT || ordType == OrdType.STOP_LIMIT;
    }
}
