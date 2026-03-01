package com.btcs.fix;

import java.util.LinkedHashMap;
import java.util.Map;

import com.btcs.web.OrderBookJsonBuilder;

public class OrderBookManager {
	
    private static final String[] SYMBOLS = {
            "RELIANCE", "HDFCBANK", "BHARTIARTL", "TCS", "MRF"
    };

    private static final OrderBookManager INSTANCE = new OrderBookManager();

    private final Map<String, OrderBook> books = new LinkedHashMap<>();

    private OrderBookManager() {
        // Pre-create books
        for (String sym : SYMBOLS) {
            books.put(sym, new OrderBook(sym));
        }
    }
    
    public String snapshotAllTop5Json() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");

        boolean first = true;

        for (String symbol : SYMBOLS) {   // <-- iterate fixed array, NOT map
            OrderBook book = books.get(symbol);

            if (book != null) {
                if (!first) {
                    sb.append(",");
                }

                OrderBookSnapshot snap = book.snapshotTop5();
                sb.append(OrderBookJsonBuilder.build(snap));
                first = false;
            }
        }

        sb.append("]");
        return sb.toString();
    }


    public static OrderBookManager getInstance() {
        return INSTANCE;
    }

    public OrderBook getBook(String symbol) {
        return books.get(symbol);
    }

    public OrderBook getOrCreateBook(String symbol) {
        return books.computeIfAbsent(symbol, OrderBook::new);
    }

    public Map<String, OrderBook> getAllBooks() {
        return books;
    }
}