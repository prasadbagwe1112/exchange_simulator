package com.btcs.fix;

import java.math.BigDecimal;
import quickfix.SessionID;
import quickfix.field.OrdType;

public class Order {

    private final String orderId;
    private final String clOrdId;
    private final String symbol;
    private final char side;
    private final BigDecimal price;
    private final BigDecimal stopPx;
    private final BigDecimal quantity;
    private BigDecimal leavesQty;
    private BigDecimal cumQty;
    private BigDecimal avgpx;
    private char status;
    private char tif;
    private char ordType;
    private SessionID sessionID;

    public Order(
            String orderId,
            String clOrdId,
            String symbol,
            char side,
            BigDecimal price,
            BigDecimal stopPx,
            BigDecimal quantity,
            BigDecimal leavesQty,
            BigDecimal cumQty,
            BigDecimal avgpx,
            char status,
            char tif,
            char ordType,
            SessionID sessionID
            
    ) {
        this.orderId = orderId;
        this.clOrdId = clOrdId;
        this.symbol = symbol;
        this.side = side;
        this.price = price;
        this.stopPx = stopPx;
        this.quantity = quantity;
        this.leavesQty = leavesQty;
        this.cumQty = cumQty;
        this.avgpx = avgpx;
        this.status = status;
        this.tif = tif;
        this.ordType = ordType;
        this.sessionID = sessionID; 
    }

    // getters (only if needed later)
    public String getClOrdId() { return clOrdId; }
    public String getOrderId() { return orderId; }
    public String getSymbol() { return symbol; }
    public char getSide() { return side; }
    public char getStatus() { return status; }
    public char getTimeInForce() { return tif; }
    public char getOrdType() { return ordType; }
    public BigDecimal getPrice() { return price; }
    public BigDecimal getStopPx() { return stopPx; }
    public BigDecimal getAvgPrice() { return avgpx; }
    public BigDecimal getLeavesQty() { return leavesQty; }
    public BigDecimal getCumQty() { return cumQty; }
    public BigDecimal getOrderQty() { return quantity; }

    public void setLeavesQty(BigDecimal leavesQty) {
        this.leavesQty = leavesQty;
    }

    public void setCumQty(BigDecimal cumQty) {
        this.cumQty = cumQty;
    }
    
    public void setAvgPrice(BigDecimal avgpx) {
        this.avgpx = avgpx;
    }
    
    public void setOrdType(char ordType) {
        this.ordType = ordType;
    }
    
    public void setOrdStatus(char status) {
        this.status = status;
    }
    
    public SessionID getSessionID() {
        return sessionID;
    }

    public void setSessionID(SessionID sessionID) {
        this.sessionID = sessionID;
    }
    
    public boolean isMarket() {
    	if (this.ordType == OrdType.MARKET) {
    		return true;
    	} else {
    		return false;
    	}
    }
}