package com.btcs.fix;

import quickfix.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.btcs.web.WebServer;

import java.io.FileInputStream;

public class FixEngineMain {
	
	private static final Logger logger = LoggerFactory.getLogger("FixEngineMain");

    public static void main(String[] args) throws Exception {
    	
        int port = 8081; // default

        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.out.println("Invalid port provided. Using default 8081.");
            }
        }
        // start web server
        WebServer webServer = new WebServer(port);
        webServer.start();
    	
        SessionSettings settings =
                new SessionSettings(new FileInputStream("config/acceptor.cfg"));

        // Your FIX application logic
        Application application = new FixExchangeSimulator();

        // Message store
        MessageStoreFactory storeFactory =
                new FileStoreFactory(settings);

        // Logging
        LogFactory logFactory =
                new FileLogFactory(settings);

        // Message factory
        MessageFactory messageFactory =
                new DefaultMessageFactory();

        // Create Acceptor
        Acceptor acceptor =
                new SocketAcceptor(
                        application,
                        storeFactory,
                        settings,
                        logFactory,
                        messageFactory
                );

        // Start FIX engine
        acceptor.start();
        logger.info("FIX Acceptor started...");
        
        // Write PID file
        String pid = java.lang.management.ManagementFactory
                .getRuntimeMXBean()
                .getName()
                .split("@")[0];

        java.nio.file.Files.write(
                java.nio.file.Paths.get("exchange.pid"),
                pid.getBytes()
        );

        System.out.println("PID: " + pid);

        
        // Shutdown logic
        
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                System.out.println("Shutting down FIX Acceptor...");
                acceptor.stop();

                java.nio.file.Files.deleteIfExists(
                        java.nio.file.Paths.get("exchange.pid")
                );

            } catch (Exception e) {
                e.printStackTrace();
            }
        }));
        
        synchronized (FixEngineMain.class) {
            FixEngineMain.class.wait();
        }
    }
}
