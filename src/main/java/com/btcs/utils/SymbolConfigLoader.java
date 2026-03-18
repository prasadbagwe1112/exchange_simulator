package com.btcs.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class SymbolConfigLoader {

    private static final Map<String, SymbolConfig> SYMBOL_MAP = new HashMap<>();

    public static void load(String file) throws Exception {

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {

            String line = br.readLine(); // skip header

            while ((line = br.readLine()) != null) {

                String[] parts = line.split(",");

                String symbol = parts[0];
                BigDecimal ref = new BigDecimal(parts[1]);
                BigDecimal tick = new BigDecimal(parts[2]);

                SYMBOL_MAP.put(symbol, new SymbolConfig(symbol, ref, tick));
            }
        }
    }

    public static SymbolConfig get(String symbol) {
        return SYMBOL_MAP.get(symbol);
    }
}