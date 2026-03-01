
package com.btcs.web;

import com.btcs.fix.OrderBook;
import com.btcs.fix.OrderBookManager;
import com.btcs.fix.OrderBookSnapshot;
import com.sun.net.httpserver.HttpServer;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class WebServer {

    private HttpServer server;
    private final int port;

    public WebServer(int port) {
        this.port = port;
    }

    public void start() throws Exception {

        server = HttpServer.create(new InetSocketAddress(port), 0);
        
        server.createContext("/orderbooks", exchange -> {

            String json = OrderBookManager.getInstance().snapshotAllTop5Json();
            //System.out.print("json::" + json);

            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, json.getBytes().length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(json.getBytes());
            }
        });

        server.createContext("/orderbook/stream", exchange -> {
            exchange.getResponseHeaders().add("Content-Type", "text/event-stream");
            exchange.getResponseHeaders().add("Cache-Control", "no-cache");
            exchange.getResponseHeaders().add("Connection", "keep-alive");

            exchange.sendResponseHeaders(200, 0);
            // keep connection open
        });
        
        server.createContext("/", exchange -> {

        	InputStream is = WebServer.class.getResourceAsStream("index.html");

            if (is == null) {
                String error = "index.html not found";
                exchange.sendResponseHeaders(500, error.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(error.getBytes());
                }
                return;
            }

            byte[] bytes = is.readAllBytes();

            exchange.getResponseHeaders().add("Content-Type", "text/html");
            exchange.sendResponseHeaders(200, bytes.length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        
        server.createContext("/orderbook", exchange -> {

            String query = exchange.getRequestURI().getQuery();
            String symbol = query != null && query.contains("=")
                    ? query.split("=")[1]
                    : "";

            OrderBook book = OrderBookManager.getInstance().getBook(symbol);
            if (book == null) {
                String empty = "{}";
                exchange.sendResponseHeaders(200, empty.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(empty.getBytes());
                }
                return;
            }
            OrderBookSnapshot snap = book.snapshotTop5();
            String json = OrderBookJsonBuilder.build(snap);

            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, json.getBytes().length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(json.getBytes());
            }
        });

        server.setExecutor(null); // default executor
        server.start();

        System.out.println("WebServer started on http://localhost:" + port);
    }
}