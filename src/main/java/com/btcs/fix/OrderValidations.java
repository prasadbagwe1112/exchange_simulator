package com.btcs.fix;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import com.btcs.utils.*;
import quickfix.field.*;

public class OrderValidations {
	
	private static final List<String> ALLOWED_SYMBOLS = List.of("RELIANCE", "HDFCBANK", "BHARTIARTL", "TCS", "MRF");
	private static final Set<Character> ALLOWED_ORD_TYPES = Set.of(OrdType.LIMIT, OrdType.MARKET, 
			OrdType.STOP_LIMIT, OrdType.STOP_STOP_LOSS);
    private static final Set<Character> LIMIT_MKT = Set.of(OrdType.LIMIT, OrdType.MARKET); // allowed ORD Types for IOC/FOK
	private static final Set<Character> STOP_ORD = Set.of(OrdType.STOP_LIMIT, OrdType.STOP_STOP_LOSS);
	private static final Set<Character> ALLOWED_TIFS = Set.of(TimeInForce.DAY, TimeInForce.GOOD_TILL_CANCEL, 
			TimeInForce.IMMEDIATE_OR_CANCEL, TimeInForce.FILL_OR_KILL);
    private static final Set<Character> IOC_FOK = Set.of(TimeInForce.IMMEDIATE_OR_CANCEL, TimeInForce.FILL_OR_KILL);

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

        if ((IOC_FOK.contains(tif.getValue())) && !LIMIT_MKT.contains(ordType.getValue())) {
            return ValidationResult.reject("Invalid Order Type for IOC/FOK Order");
        }
        
        if (isPriceRequired(ordType.getValue()) && (price == null || price.getValue() <= 0)) {
        	return ValidationResult.reject("Invalid Price");
        }
        
        if (storedOrder != null) {
        	return ValidationResult.reject("Duplicate ClOrdID");
        }

        SymbolConfig config = SymbolConfigLoader.get(symbol.getValue());
        if (config == null) {
            return ValidationResult.reject("Symbol not supported, use any one from - " + ALLOWED_SYMBOLS);
        }

        ValidationResult priceCheck = getValidationResultForPrice(ordType, symbol, price, config);
        if (priceCheck != null) return priceCheck;

        if (side.getValue() == Side.BUY && STOP_ORD.contains(ordType.getValue())
        		&& book.getLastTradedPrice() != null
        		&& stopPx.getValue() != 0
        		&& book.getLastTradedPrice().compareTo(BigDecimal.valueOf(stopPx.getValue())) >= 0) {
			return ValidationResult.reject("Invalid Trigger Price - Buy Stop PX cannot be <= LTP");
		}
		
		if (side.getValue() == Side.SELL && STOP_ORD.contains(ordType.getValue())
            	&& book.getLastTradedPrice() != null
            	&& stopPx.getValue() != 0
            	&& book.getLastTradedPrice().compareTo(BigDecimal.valueOf(stopPx.getValue())) <= 0) {
			return ValidationResult.reject("Invalid Trigger Price - Sell Stop PX cannot be >= LTP");
		}
		
		if ((side.getValue() == Side.BUY 
				&& ordType.getValue() == OrdType.STOP_LIMIT
        		&& price.getValue() < stopPx.getValue()) ||
			(side.getValue() == Side.SELL 
				&& ordType.getValue() == OrdType.STOP_LIMIT
        		&& price.getValue() > stopPx.getValue())) {
			return ValidationResult.reject("Invalid Trigger Price, Please check Price and StopPX");
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
            OrderQty orderQty,
            Price price
    ) {
        if (storedOrder == null) {
            return ValidationResult.reject("Order not found");
        }

        if (ordType != null && (storedOrder.getOrdType() != ordType.getValue())) {
            return ValidationResult.reject("Order Type cannot be replaced");
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

        SymbolConfig config = SymbolConfigLoader.get(symbol.getValue());
        if (config == null) {
            if (!storedOrder.getSymbol().equals(symbol.getValue())) {
                return ValidationResult.reject("Symbol cannot be replaced");
            }
        }

        ValidationResult priceCheck = getValidationResultForPrice(ordType, symbol, price, config);
        if (priceCheck != null) return priceCheck;

        return ValidationResult.ok();
    }


    /* -------- CANCEL VALIDATION -------- */
    public ValidationResult validateCancelorOSR(Order storedOrder) {
        if (storedOrder == null) {
            return ValidationResult.reject("Order not found");
        }
        return ValidationResult.ok();
    }

    /*----------------- Helper methods -------------------------*/

    private boolean isPriceRequired(char ordType) {
    	return ordType == OrdType.LIMIT || ordType == OrdType.STOP_LIMIT;
    }

    private boolean isValidTick(BigDecimal price, BigDecimal tickSize) {

        BigDecimal remainder = price.remainder(tickSize);

        return remainder.compareTo(BigDecimal.ZERO) == 0;
    }

    private ValidationResult getValidationResultForPrice(OrdType ordType, Symbol symbol, Price price, SymbolConfig config) {
        if (price != null && isPriceRequired(ordType.getValue())) {

            BigDecimal px = BigDecimal.valueOf(price.getValue());
            BigDecimal ref = config.getRefPrice();
            BigDecimal upper = ref.multiply(new BigDecimal("1.10"));
            BigDecimal lower = ref.multiply(new BigDecimal("0.90"));

            if (!isValidTick(px, config.getTickSize())) {
                return ValidationResult.reject(
                        "Invalid Tick Size, " + symbol.getValue() + ":" + config.getTickSize()
                );
            }

            // validate if price is > or < 10% of Ref Price
            if (px.compareTo(upper) > 0 || px.compareTo(lower) < 0)
                return ValidationResult.reject("Invalid price: order price is outside the permitted ±10% band of the reference price (" + ref + ").");
        }
        return null;
    }
}
