package com.btcs.fix;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

import quickfix.field.OrdType;

public class StopOrderManager {

    private final OrderBookManager bookManager;
    private final Executions executions;
    private final OrderRepository orderRepo;
    

    // BUY stop → trigger when LTP >= stopPx
    private final NavigableMap<BigDecimal, List<Order>> stopBuys = new TreeMap<>();

    // SELL stop → trigger when LTP <= stopPx
    private final NavigableMap<BigDecimal, List<Order>> stopSells = new TreeMap<>();

    public StopOrderManager(OrderBookManager bookManager,             
                            Executions executions,
                            OrderRepository orderRepo) {
        this.bookManager = bookManager;       
        this.executions = executions;
        this.orderRepo = orderRepo;
    }

    // ================================
    // Add new stop order
    // ================================
    public synchronized void addStopOrder(Order order, BigDecimal stopPx) {

        NavigableMap<BigDecimal, List<Order>> map =
                order.getSide() == '1' ? stopBuys : stopSells;

        map.computeIfAbsent(stopPx, k -> new ArrayList<>()).add(order);
    }

    // ================================
    // Called AFTER EVERY TRADE
    // ================================
    public synchronized void onTrade(String symbol, BigDecimal ltp) {
    	
    	if (ltp == null) {
    		return;
    	}
        triggerStopBuys(symbol, ltp);
        triggerStopSells(symbol, ltp);
    }

    private void triggerStopBuys(String symbol, BigDecimal ltp) {

        var triggered = stopBuys.headMap(ltp, true);

        List<BigDecimal> toRemove = new ArrayList<>();

        for (var entry : triggered.entrySet()) {
            for (Order order : entry.getValue()) {
            	activate(order, symbol);
            }
            toRemove.add(entry.getKey());
        }
        toRemove.forEach(stopBuys::remove);
    }

    private void triggerStopSells(String symbol, BigDecimal ltp) {

        var triggered = stopSells.tailMap(ltp, true);

        List<BigDecimal> toRemove = new ArrayList<>();

        for (var entry : triggered.entrySet()) {
            for (Order order : entry.getValue()) {
                activate(order, symbol);
            }
            toRemove.add(entry.getKey());
        }
        toRemove.forEach(stopSells::remove);
    }

    private void activate(Order order, String symbol) {

        OrderBook book = bookManager.getBook(symbol);
        Order restatedOrder = updateTriggeredOrder(order);

        executions.sendRestated(restatedOrder, "Stop Order triggered");
        book.add(restatedOrder);
        //return restatedOrder;
        //matchingEngine.match(book, restatedOrder.getSide());
    }
    
	private Order updateTriggeredOrder(Order order) {
		Order updatedOrder = orderRepo.getOrder(order.getClOrdId()); 
		if(order.getOrdType() == OrdType.STOP_STOP_LOSS)
			updatedOrder.setOrdType('1');
		else if(order.getOrdType() == OrdType.STOP_LIMIT)
			updatedOrder.setOrdType('2');
		orderRepo.addOrder(updatedOrder.getClOrdId(), updatedOrder);
		return updatedOrder;
	}
}
