package com.btcs.fix;

import quickfix.field.*;
import quickfix.fix44.*;
import quickfix.Message;
import quickfix.SessionID;
import quickfix.FieldNotFound;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HandleIncomingMsg {

    private final OrderRepository orderRepo;
    private final Executions executions;
    private final OrderValidations validations;
    private final OrderBookManager orderBookManager;
    private final MatchingEngine matchingEngine;
    private final StopOrderManager stopOrderManager;

    public HandleIncomingMsg(
            OrderRepository orderRepo,
            Executions executions,
            OrderValidations validations,
            OrderBookManager orderBookManager,
            MatchingEngine matchingEngine,
            StopOrderManager stopOrderManager
            
    ) {
        this.orderRepo = orderRepo;
        this.executions = executions;
        this.validations = validations;
        this.orderBookManager = orderBookManager;
        this.matchingEngine = matchingEngine;
        this.stopOrderManager = stopOrderManager;
    }
    
	private static final Logger logger = LoggerFactory.getLogger("HandleIncomingMsg");
	
    /* -------- NEW ORDER -------- */
    public void handleNewOrder(Message order, SessionID sessionId) throws FieldNotFound {
    	
    	char orderType = order.getChar(OrdType.FIELD);
    	
        ClOrdID clOrdID = new ClOrdID(order.getString(ClOrdID.FIELD));
        Symbol symbol = new Symbol(order.getString(Symbol.FIELD));
        Side side = new Side(order.getChar(Side.FIELD));
        OrderQty qty = new OrderQty(order.getDouble(OrderQty.FIELD));
        OrdType ordType = new OrdType(orderType);
        Price price = getPriceForLimitOrder(order, orderType, side); 
        TimeInForce tif = new TimeInForce(order.getChar(TimeInForce.FIELD));
        OrderID orderID = new OrderID("ORD-" + System.currentTimeMillis());
        StopPx stopPx = getStopPriceForStopOrder(order, orderType);

        Order existingOrder = orderRepo.getOrder(clOrdID.getValue());  
        OrderBook book = orderBookManager.getBook(symbol.getValue());
        ValidationResult result = validations.validateNewOrder(ordType, tif, symbol, existingOrder, price, stopPx, book, side);
        if (!result.isValid()) {
            rejectOrder(sessionId, clOrdID, symbol, side, ordType, tif, qty, price, stopPx, OrdRejReason.OTHER, result.getRejectReason());
            return;
        }
        orderRepo.saveNewOrder(
                orderID.getValue(),
                clOrdID.getValue(),
                symbol.getValue(),
                side.getValue(),
                BigDecimal.valueOf(price.getValue()),
                BigDecimal.valueOf(stopPx.getValue()),
                BigDecimal.valueOf(qty.getValue()),
                BigDecimal.valueOf(qty.getValue()),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                OrdStatus.NEW,
                tif.getValue(),
                ordType.getValue(),
                sessionId
        );
        Order newOrder = orderRepo.getOrder(clOrdID.getValue()); 
        logger.info("order price, stopPx >> " + newOrder.getPrice() + " " + newOrder.getStopPx());
        executions.sendNewAck(sessionId, clOrdID, orderID, side, qty, price, symbol, ordType, tif, stopPx);
        addToBookAndMatch(side, ordType, stopPx, newOrder, book); 
    }

	private void addToBookAndMatch(Side side, OrdType ordType, StopPx stopPx, Order order, OrderBook book) {

		if (ordType.getValue() == OrdType.LIMIT || ordType.getValue() == OrdType.MARKET) {

			book.add(order);
			matchingEngine.match(book, order.getSide());
			return;
		}

		// STOP ORDERS → DO NOT TOUCH BOOK
		if (ordType.getValue() == OrdType.STOP_LIMIT || ordType.getValue() == OrdType.STOP_STOP_LOSS) { {
			stopOrderManager.addStopOrder(order, BigDecimal.valueOf(stopPx.getValue()));			
			logger.info("Stop order parked: {}", order.getClOrdId());
	        }
		}
	}

    /* -------- REPLACE -------- */
    public void handleReplace(OrderCancelReplaceRequest replace, SessionID sessionId)
            throws FieldNotFound {

        OrigClOrdID origClOrdID = new OrigClOrdID(replace.getString(OrigClOrdID.FIELD));
        ClOrdID clOrdID = new ClOrdID(replace.getString(ClOrdID.FIELD));
        OrdType ordType = new OrdType(replace.getChar(OrdType.FIELD));
        Symbol symbol = new Symbol(replace.getString(Symbol.FIELD));
        Side side = new Side(replace.getChar(Side.FIELD));
        TimeInForce tif = new TimeInForce(replace.getChar(TimeInForce.FIELD));
        OrderQty quantity = new OrderQty(replace.getDouble(OrderQty.FIELD));

        Order storedOrder = orderRepo.getOrder(origClOrdID.getValue());

        ValidationResult result = validations.validateReplace(storedOrder, ordType, symbol, side, tif, quantity);
        if (!result.isValid()) {
        	rejectCxl(sessionId, clOrdID, origClOrdID, CxlRejResponseTo.ORDER_CANCEL_REPLACE_REQUEST,
            		CxlRejReason.OTHER, result.getRejectReason());
            return;
        }
        
        //Send ER replace
        ExecutionReport er = executions.sendReplaceAck(
                sessionId,
                storedOrder,
                clOrdID,
                origClOrdID,
                new OrderQty(replace.getDouble(OrderQty.FIELD)),
                new Price(replace.getDouble(Price.FIELD)),
                new StopPx(replace.getDouble(StopPx.FIELD)),
                ordType
        );
        // remove old order from order repository
        orderRepo.removeOrder(origClOrdID.getValue());
        
        //add replaced order in repository
        orderRepo.saveNewOrder(
                storedOrder.getOrderId(),
                clOrdID.getValue(),
                symbol.getValue(),
                storedOrder.getSide(),
                BigDecimal.valueOf(er.getPrice().getValue()),
                BigDecimal.valueOf(er.getStopPx().getValue()),
                BigDecimal.valueOf(er.getOrderQty().getValue()),
                BigDecimal.valueOf(er.getLeavesQty().getValue()),
                storedOrder.getCumQty(),
                storedOrder.getAvgPrice(),
                storedOrder.getStatus(),
                tif.getValue(),
                ordType.getValue(),
                sessionId
        );
        
        logger.info("REPLACE RECEIVED -> origClOrdId={} newClOrdId={} newPrice={} newQty={}",
                origClOrdID.getValue(),
                clOrdID.getValue(),
                er.getPrice().getValue(),
                er.getOrderQty().getValue());
        
        // remove old order from book
        OrderBook book = orderBookManager.getBook(symbol.getValue());
        book.removeOrder(origClOrdID.getValue());
        
        // add as fresh order in book
        Order replacedOrder = orderRepo.getOrder(clOrdID.getValue()); 
        book.add(replacedOrder);
        // try match
        matchingEngine.match(book, replacedOrder.getSide());
     
    }

    /* -------- CANCEL -------- */
    public void handleCancel(OrderCancelRequest cancel, SessionID sessionId)
            throws FieldNotFound {

        OrigClOrdID origClOrdID = new OrigClOrdID(cancel.getString(OrigClOrdID.FIELD));
        ClOrdID clOrdID = new ClOrdID(cancel.getString(ClOrdID.FIELD));

        Order storedOrder = orderRepo.getOrder(origClOrdID.getValue());

        ValidationResult result = validations.validateCancelorOSR(storedOrder);
        if (!result.isValid()) {
        	rejectCxl(sessionId, clOrdID, origClOrdID, CxlRejResponseTo.ORDER_CANCEL_REQUEST,
        			CxlRejReason.TOO_LATE_TO_CANCEL, result.getRejectReason());
        	return;
        }
        
        // remove from repository
        orderRepo.removeOrder(origClOrdID.getValue());
        
        // remove from book
        OrderBook book = orderBookManager.getBook(storedOrder.getSymbol());
        book.removeOrder(origClOrdID.getValue());

        executions.sendCancelAck(sessionId, storedOrder, clOrdID, origClOrdID);
    }
    
    public void handleOrderStatusRequest(OrderStatusRequest ordStatusReq, SessionID sessionId)
            throws FieldNotFound {

        ClOrdID clOrdID = new ClOrdID(ordStatusReq.getString(ClOrdID.FIELD));
        Symbol symbol = new Symbol(ordStatusReq.getString(Symbol.FIELD));
        Side side = new Side(ordStatusReq.getChar(Side.FIELD));

        Order storedOrder = orderRepo.getOrder(clOrdID.getValue());
        ValidationResult result = validations.validateCancelorOSR(storedOrder);
        if (!result.isValid()) {
        	logger.info("no order found");
        	rejectOrderOSR(sessionId, clOrdID, symbol, side, OrdRejReason.OTHER, result.getRejectReason());
            return;
        }
        
        executions.sendOrderStatus(storedOrder);
    }
    

    /* -------- Reject Helpers -------- */
    private void rejectOrder(
            SessionID sessionId,
            ClOrdID clOrdID,
            Symbol symbol,
            Side side,
            OrdType ordType,
            TimeInForce tif,
            OrderQty qty,
            Price price,
            StopPx stopPx,
            int reason,
            String text
    ) {
        executions.sendOrderReject(
                sessionId,
                clOrdID,
                symbol, side, ordType, tif, qty, price, stopPx,
                reason,
                text
        );
    }
    // For Order Status Request
    private void rejectOrderOSR(
            SessionID sessionId,
            ClOrdID clOrdID,
            Symbol symbol,
            Side side,
            int reason,
            String text
    ) {
        executions.sendOrderReject(
                sessionId,
                clOrdID,
                symbol, side,
                reason,
                text
        );
    }

    /* -------- Helper Method For Cancel / Replace Reject -------- */
    private void rejectCxl(
            SessionID sessionId,
            ClOrdID clOrdID,
            OrigClOrdID origClOrdID,
            char responseTo,
            int rejReason,
            String text
    ) {
        executions.sendCancelReplaceReject(
                sessionId,
                new OrderID("NONE"),
                clOrdID,
                origClOrdID,
                new CxlRejResponseTo(responseTo),
                rejReason,
                text
        );
    }
    
	private Price getPriceForLimitOrder(Message order, char orderType, Side side) throws FieldNotFound {
		if (orderType == OrdType.LIMIT || orderType == OrdType.STOP_LIMIT) {
        Price price = new Price(order.getDouble(Price.FIELD));
        return price;
        } else if ((orderType == OrdType.MARKET || orderType == OrdType.STOP_STOP_LOSS) && side.getValue() == '1') {
    		return new Price(100000);	
        } else {
        	return new Price(0);
        }
	}
	
	private StopPx getStopPriceForStopOrder(Message order, char orderType) throws FieldNotFound {
		if (orderType == OrdType.STOP_STOP_LOSS || orderType == OrdType.STOP_LIMIT) {
			StopPx stopPx = new StopPx(order.getDouble(StopPx.FIELD));
        return stopPx;
        } else {
        	return new StopPx(0);
        }
	}
	
}