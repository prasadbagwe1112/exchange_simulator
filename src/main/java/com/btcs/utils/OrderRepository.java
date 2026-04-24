package com.btcs.utils;

import java.math.BigDecimal;

import quickfix.SessionID;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OrderRepository {
	
	private static final Logger logger = LoggerFactory.getLogger("OrderRepository");
	private static final OrderRepository INSTANCE = new OrderRepository();
	
    private OrderRepository() {
        // prevent new
    }

    public static OrderRepository getInstance() {
        return INSTANCE;
    }

    // In-memory store (lives as long as JVM runs)
    private final Map<String, Order> ordersByClOrdId =
            new ConcurrentHashMap<>();

    public void saveNewOrder(
            String account,
            String orderId,
            String clOrdId,
            String symbol,
            char side,
            BigDecimal price,
            BigDecimal stopPx,
            BigDecimal quantity,
            BigDecimal leavesQty,
            BigDecimal cumQty,
            BigDecimal avgPx,
            char status,
            char tif,
            char ordType,
            SessionID sessionId

    ) {

        Order order = new Order(
                account,
                orderId,
                clOrdId,
                symbol,
                side,
                price,
                stopPx,
                quantity,
                leavesQty,
                cumQty,
                avgPx,
                status,
                tif,
                ordType,
                sessionId
        );

        // store using clOrdId as key
        ordersByClOrdId.put(clOrdId, order);
        
        //VERIFICATION LOG
        logger.info("Order is added. clOrdId=" + clOrdId + ", totalOrders=" + ordersByClOrdId.size());
    }

    public Order getOrder(String clOrdId) {
        return ordersByClOrdId.get(clOrdId);
    }
    
    public void removeOrder(String clOrdId) {
    	ordersByClOrdId.remove(clOrdId);
    	logger.info("Order is removed. clOrdId=" + clOrdId + ", totalOrders=" + ordersByClOrdId.size()); 
    }
    
    public void addOrder(String clOrdId, Order order) {
    	ordersByClOrdId.put(clOrdId, order);
    	logger.info("Order is updated. clOrdId=" + clOrdId + ", totalOrders=" + ordersByClOrdId.size()); 
    }
}