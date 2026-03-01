package com.btcs.fix;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OrderBook {
	
	private static final Logger logger = LoggerFactory.getLogger("OrderBook");
	private volatile BigDecimal lastTradedPrice;
	
	private final String symbol;
	
	public OrderBook(String symbol) {
	    this.symbol = symbol;
	}
	
	public String getSymbol() {
	    return symbol;
	}
	
	public void setLastTradedPrice(BigDecimal price) {
	    this.lastTradedPrice = price;
	}
	public BigDecimal getLastTradedPrice() {
	    return lastTradedPrice;
	}



    // BUY: highest price first
    private final NavigableMap<BigDecimal, Deque<Order>> buyBook =
            new TreeMap<>(Comparator.reverseOrder());

    // SELL: lowest price first
    private final NavigableMap<BigDecimal, Deque<Order>> sellBook =
            new TreeMap<>();
    
 // clOrdId -> price level queue
    private final Map<String, Deque<Order>> orderIndex = new HashMap<>();

    public synchronized void add(Order order) {
    	logger.info("ADDING to OrderBook: clOrdId={}, symbol={}, side={}, price={}, qty={}",
                order.getClOrdId(), order.getSymbol(), order.getSide(), order.getPrice(), order.getLeavesQty()
        );
        NavigableMap<BigDecimal, Deque<Order>> book =
                order.getSide() == '1' ? buyBook : sellBook;

        Deque<Order> level = book.computeIfAbsent(order.getPrice(), p -> new ArrayDeque<>());
        level.addLast(order); // FIFO
        
        // index it
        orderIndex.put(order.getClOrdId(), level);
        
        logBook("AFTER ADD " + order.getClOrdId());
    }

    public synchronized boolean removeOrder(String clOrdId) {
    	logger.info("REMOVE ORDER REQUEST -> clOrdId={}", clOrdId);
    	
        Deque<Order> level = orderIndex.remove(clOrdId);
        if (level == null) {
        	logger.warn("Order not found in level: {}", clOrdId);
            return false;
        }

        Order removed = null;
        for (Order o : level) {
            if (o.getClOrdId().equals(clOrdId)) {
                removed = o;
                break;
            }
        }

        if (removed == null) {
        	logger.warn("Order not found in level: {}", clOrdId);
            return false;
        }

        level.remove(removed);

        // clean empty price level
        if (level.isEmpty()) {
            if (removed.getSide() == '1') {
                buyBook.remove(removed.getPrice());
            } else {
                sellBook.remove(removed.getPrice());
            }
        }

        //logger.info("Order removed from book: clOrdId={}", clOrdId);
        //logBook("AFTER REMOVE " + clOrdId);
        return true;
    }
    
    public NavigableMap<BigDecimal, Deque<Order>> getBuyBook() {
        return buyBook;
    }

    public NavigableMap<BigDecimal, Deque<Order>> getSellBook() {
        return sellBook;
    }

    //////////////////////////////////////////////// To display Order Book //////////////////////////
    public synchronized OrderBookSnapshot snapshotTop5() {

        OrderBookSnapshot snap = new OrderBookSnapshot();
        snap.symbol = this.symbol;
        snap.ltp = this.lastTradedPrice;

        snap.bids = buyBook.entrySet().stream()
                .limit(5)
                .map(e -> {
                    BigDecimal qty = e.getValue().stream()
                            .map(Order::getLeavesQty)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    OrderBookSnapshot.Level l = new OrderBookSnapshot.Level();
                    l.price = e.getKey();
                    l.quantity = qty;
                    return l;
                })
                .collect(Collectors.toList());

        snap.asks = sellBook.entrySet().stream()
                .limit(5)
                .map(e -> {
                    BigDecimal qty = e.getValue().stream()
                            .map(Order::getLeavesQty)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    OrderBookSnapshot.Level l = new OrderBookSnapshot.Level();
                    l.price = e.getKey();
                    l.quantity = qty;
                    return l;
                })
                .collect(Collectors.toList());

        return snap;
    }
    
    public void logBook(String reason) {

        String tmpSymbol;
        if (!buyBook.isEmpty()) {
        	tmpSymbol = buyBook.firstEntry().getValue().peekFirst().getSymbol();
        } else if (!sellBook.isEmpty()) {
        	tmpSymbol = sellBook.firstEntry().getValue().peekFirst().getSymbol();
        } else {
        	tmpSymbol = "EMPTY_BOOK";
        }
        
        String symbol = tmpSymbol;
        logger.info(
            "========== ORDER BOOK [{}] | SYMBOL={} ==========",
            reason, symbol
        );

        logger.info("----- BUY BOOK [{}] (Best → Worst) -----", symbol);
        buyBook.descendingMap().forEach((price, orders) -> {
            logger.info("BUY  {} @ {} -> {}",
                    symbol,
                    price,
                    orders.stream()
                          .map(o -> o.getClOrdId() + "(" + o.getLeavesQty() + ")")
                          .collect(Collectors.toList()));
        });

        logger.info("----- SELL BOOK [{}] (Best → Worst) -----", symbol);
        sellBook.forEach((price, orders) -> {
            logger.info("SELL {} @ {} -> {}",
                    symbol,
                    price,
                    orders.stream()
                          .map(o -> o.getClOrdId() + "(" + o.getLeavesQty() + ")")
                          .collect(Collectors.toList()));
        });

        logger.info("===============================================");
    }
}