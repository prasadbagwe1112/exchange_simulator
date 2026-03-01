package com.btcs.fix;

import quickfix.*;
import quickfix.field.*;
import quickfix.fix44.*;
import quickfix.fix44.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

public class Executions {

    private static final Logger logger = LoggerFactory.getLogger("Executions");

    /* ---------------- NEW ORDER ACK ---------------- */
    public void sendNewAck(
            SessionID sessionId,
            ClOrdID clOrdID,
            OrderID orderID,
            Side side,
            OrderQty orderQty,
            Price price,
            Symbol symbol,
            OrdType ordType,
            TimeInForce timeInForce,
            StopPx stopPx
    ) {
        ExecutionReport execReport = new ExecutionReport(
                orderID,
                new ExecID(generateExecId()),
                new ExecType(ExecType.NEW),
                new OrdStatus(OrdStatus.NEW),
                side,
                new LeavesQty(orderQty.getValue()),
                new CumQty(0),
                new AvgPx(0)
        );

        execReport.set(clOrdID);
        execReport.set(orderQty);
        execReport.set(symbol);
        execReport.set(timeInForce);
        execReport.set(ordType);
        execReport.set(new TransactTime());
        
        if(ordType.getValue() == OrdType.LIMIT || ordType.getValue() == OrdType.STOP_LIMIT)
            execReport.set(price);
        if((ordType.getValue() == OrdType.STOP_STOP_LOSS || ordType.getValue() == OrdType.STOP_LIMIT) && stopPx != null)
            execReport.set(stopPx);

        send(execReport, sessionId);
    }

    /* ---------------- REPLACE ACK ---------------- */
    public ExecutionReport sendReplaceAck(
            SessionID sessionId,
            Order storedOrder,
            ClOrdID clOrdID,
            OrigClOrdID origClOrdID,
            OrderQty orderQty,
            Price price,
            StopPx stopPx,
            OrdType ordType
    ) {
        BigDecimal leavesQty = new BigDecimal(orderQty.getValue()).subtract(storedOrder.getCumQty());

        ExecutionReport execReport = new ExecutionReport(
                new OrderID(storedOrder.getOrderId()),
                new ExecID(generateExecId()),
                new ExecType(ExecType.REPLACED),
                new OrdStatus(storedOrder.getStatus()),
                new Side(storedOrder.getSide()),
                new LeavesQty(leavesQty.doubleValue()),
                new CumQty(storedOrder.getCumQty().doubleValue()),
                new AvgPx(storedOrder.getAvgPrice().doubleValue())
        );

        execReport.set(clOrdID);
        execReport.set(origClOrdID);
        execReport.set(orderQty);
        execReport.set(ordType);
        execReport.set(new TimeInForce(storedOrder.getTimeInForce()));
        execReport.set(new Symbol(storedOrder.getSymbol()));
        execReport.set(new TransactTime());
        
        if(ordType.getValue() == OrdType.LIMIT || ordType.getValue() == OrdType.STOP_LIMIT)
            execReport.set(price);

        send(execReport, sessionId);
        
        return execReport;
    }

    /* ---------------- CANCEL ACK ---------------- */
    public void sendCancelAck(
            SessionID sessionId,
            Order storedOrder,
            ClOrdID clOrdID,
            OrigClOrdID origClOrdID
    ) {
        ExecutionReport execReport = new ExecutionReport(
                new OrderID(storedOrder.getOrderId()),
                new ExecID(generateExecId()),
                new ExecType(ExecType.CANCELED),
                new OrdStatus(OrdStatus.CANCELED),
                new Side(storedOrder.getSide()),
                new LeavesQty(0),
                new CumQty(storedOrder.getCumQty().doubleValue()),
                new AvgPx(storedOrder.getAvgPrice().doubleValue())
        );

        execReport.set(clOrdID);
        execReport.set(origClOrdID);
        execReport.set(new OrderQty(storedOrder.getOrderQty().doubleValue()));
        execReport.set(new Symbol(storedOrder.getSymbol()));
        execReport.set(new TransactTime());
        execReport.set(new TimeInForce(storedOrder.getTimeInForce()));
        execReport.set(new OrdType(storedOrder.getOrdType()));
        
        if(storedOrder.getOrdType() == OrdType.LIMIT || storedOrder.getOrdType() == OrdType.STOP_LIMIT)
        	execReport.set(new Price(storedOrder.getPrice().doubleValue()));

        send(execReport, sessionId);
    }
    
    /* ---------------- UNSOL CANCEL ACK ---------------- */
    public void sendUnSolCancel(
            SessionID sessionId,
            Order storedOrder,
            String clOrdID,
            String text
    ) {
        ExecutionReport execReport = new ExecutionReport(
                new OrderID(storedOrder.getOrderId()),
                new ExecID(generateExecId()),
                new ExecType(ExecType.CANCELED),
                new OrdStatus(OrdStatus.CANCELED),
                new Side(storedOrder.getSide()),
                new LeavesQty(0),
                new CumQty(storedOrder.getCumQty().doubleValue()),
                new AvgPx(storedOrder.getAvgPrice().doubleValue())
        );

        execReport.set(new ClOrdID(clOrdID));
        execReport.set(new OrderQty(storedOrder.getOrderQty().doubleValue()));
        execReport.set(new Symbol(storedOrder.getSymbol()));
        execReport.set(new TransactTime());
        execReport.set(new TimeInForce(storedOrder.getTimeInForce()));
        execReport.set(new OrdType(storedOrder.getOrdType()));;
        execReport.set(new Text(text));
        
        if(storedOrder.getOrdType() == OrdType.LIMIT || storedOrder.getOrdType() == OrdType.STOP_LIMIT)
        	execReport.set(new Price(storedOrder.getPrice().doubleValue()));

        send(execReport, sessionId);
    }

    /* ---------------- CANCEL / REPLACE REJECT (35=9) ---------------- */
    public void sendCancelReplaceReject(
            SessionID sessionId,
            OrderID orderID,
            ClOrdID clOrdID,
            OrigClOrdID origClOrdID,
            CxlRejResponseTo rejectResponseTo,
            int rejectReason,
            String text
    ) {
        OrderCancelReject reject = new OrderCancelReject(orderID, clOrdID, origClOrdID
        		, new OrdStatus(OrdStatus.REJECTED), rejectResponseTo);

        reject.set(new CxlRejReason(rejectReason));
        reject.set(new Text(text));

        send(reject, sessionId);
    }

    /* ---------------- ORDER REJECT ---------------- */
    public void sendOrderReject(
            SessionID sessionId,
            ClOrdID clOrdID,
            Symbol symbol,
            Side side,
    		OrdType ordType,
    		TimeInForce timeInForce,
    		OrderQty quantity,
    		Price price,
    		StopPx stopPx,
            int ordRejReason,
            String text
    ) {
        ExecutionReport execReport = new ExecutionReport(
                new OrderID("NONE"),
                new ExecID(generateExecId()),
                new ExecType(ExecType.REJECTED),
                new OrdStatus(OrdStatus.REJECTED),
                side,
                new LeavesQty(0),
                new CumQty(0),
                new AvgPx(0)
        );

        execReport.set(clOrdID);
        execReport.set(symbol);
        execReport.set(ordType);
        execReport.set(timeInForce);
        execReport.set(quantity);
        execReport.set(new OrdRejReason(ordRejReason));
        execReport.set(new Text(text));
        execReport.set(new TransactTime());
        
        if((ordType.getValue() == OrdType.STOP_STOP_LOSS || ordType.getValue() == OrdType.STOP_LIMIT) && stopPx != null)
            execReport.set(stopPx);
        
        if(ordType.getValue() == OrdType.LIMIT || ordType.getValue() == OrdType.STOP_LIMIT)
            execReport.set(price);

        send(execReport, sessionId);
    }
    
    /* ---------------- Restated ---------------- */
    public void sendRestated(Order order, String text) {
        ExecutionReport execReport = new ExecutionReport(
                new OrderID(order.getOrderId()),
                new ExecID(generateExecId()),
                new ExecType(ExecType.RESTATED),
                new OrdStatus(OrdStatus.NEW),
                new Side(order.getSide()),
                new LeavesQty(order.getLeavesQty().doubleValue()),
                new CumQty(order.getCumQty().doubleValue()),
                new AvgPx(order.getAvgPrice().doubleValue())
        );

        execReport.set(new ClOrdID(order.getClOrdId()));
        execReport.set(new Symbol(order.getSymbol()));
        execReport.set(new OrderQty(order.getOrderQty().doubleValue()));
        execReport.set(new TimeInForce(order.getTimeInForce()));
        execReport.set(new OrdType(order.getOrdType()));
        execReport.set(new ExecRestatementReason(ExecRestatementReason.OTHER));
        execReport.set(new Text(text));
        
        if(order.getOrdType() == OrdType.LIMIT || order.getOrdType() == OrdType.STOP_LIMIT)
        	execReport.set(new Price(order.getPrice().doubleValue()));
        
        // Send to correct FIX session
        send(execReport, order.getSessionID());
    }
    
    /* ---------------- TARDE ---------------- */
    public void trade(Order buy, Order sell, BigDecimal tradeQty, BigDecimal tradePrice) {

        // BUY side execution
        sendFill(
            buy,
            tradeQty,
            tradePrice,
            buy.getLeavesQty().compareTo(BigDecimal.ZERO) == 0
                ? OrdStatus.FILLED
                : OrdStatus.PARTIALLY_FILLED
        );

        // SELL side execution
        sendFill(
            sell,
            tradeQty,
            tradePrice,
            sell.getLeavesQty().compareTo(BigDecimal.ZERO) == 0
                ? OrdStatus.FILLED
                : OrdStatus.PARTIALLY_FILLED
        );
    }
    
    private void sendFill(
            Order order,
            BigDecimal lastQty,
            BigDecimal lastPx,
            char ordStatus) {
    	
        ExecutionReport execReport = new ExecutionReport(
                new OrderID(order.getOrderId()),
                new ExecID(generateExecId()),
                new ExecType(ExecType.TRADE),
                new OrdStatus(ordStatus),
                new Side(order.getSide()),
                new LeavesQty(order.getLeavesQty().doubleValue()),
                new CumQty(order.getCumQty().doubleValue()),
                new AvgPx(order.getAvgPrice().doubleValue())
        );

        execReport.set(new ClOrdID(order.getClOrdId()));
        execReport.set(new Symbol(order.getSymbol()));
        execReport.set(new LastQty(lastQty.doubleValue()));
        execReport.set(new LastPx(lastPx.doubleValue()));
        execReport.set(new OrderQty(order.getOrderQty().doubleValue()));
        execReport.set(new TimeInForce(order.getTimeInForce()));
        execReport.set(new OrdType(order.getOrdType()));
        
        if(order.getOrdType() == OrdType.LIMIT || order.getOrdType() == OrdType.STOP_LIMIT)
        	execReport.set(new Price(order.getPrice().doubleValue()));
        
        // Send to correct FIX session
        send(execReport, order.getSessionID());
    }
    
    /* ---------------- COMMON SEND ---------------- */
    private void send(Message msg, SessionID sessionId) {
        try {
            Session.sendToTarget(msg, sessionId);
        } catch (SessionNotFound e) {
            logger.error("Session not found: " + sessionId, e);
        }
    }

    private String generateExecId() {
        return "EXEC-" + System.currentTimeMillis();
    }
}