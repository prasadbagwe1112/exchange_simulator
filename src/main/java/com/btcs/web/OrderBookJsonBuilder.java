package com.btcs.web;

import com.btcs.fix.OrderBookSnapshot;
import java.math.BigDecimal;
import java.util.List;

public class OrderBookJsonBuilder {

    public static String build(OrderBookSnapshot snap) {

        if (snap == null) {
            return "{}";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{");

        sb.append("\"symbol\":\"")
          .append(snap.symbol)
          .append("\",");
        
        sb.append("\"ltp\":")
        .append(snap.ltp != null ? snap.ltp : "null")
        .append(",");

        sb.append("\"buy\":[");
        appendSide(sb, snap.bids);
        sb.append("],");

        sb.append("\"sell\":[");
        appendSide(sb, snap.asks);
        sb.append("]");

        sb.append("}");

        return sb.toString();
    }

    private static void appendSide(StringBuilder sb,
                                   List<OrderBookSnapshot.Level> levels) {

        if (levels == null) {
            return;
        }

        boolean first = true;

        for (OrderBookSnapshot.Level level : levels) {

            if (!first) {
                sb.append(",");
            }
            first = false;

            sb.append("{\"price\":")
              .append(level.price != null ? level.price : BigDecimal.ZERO)
              .append(",\"qty\":")
              .append(level.quantity != null ? level.quantity : BigDecimal.ZERO)
              .append("}");
        }
    }
}
