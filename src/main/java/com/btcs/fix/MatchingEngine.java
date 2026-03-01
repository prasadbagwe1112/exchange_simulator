package com.btcs.fix;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Deque;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MatchingEngine {

	private final Executions executions;
	private final StopOrderManager stopOrderManager;

	public MatchingEngine(Executions executions, StopOrderManager stopOrderManager) {
		this.executions = executions;
	    this.stopOrderManager = stopOrderManager;
	}

	private static final Logger logger = LoggerFactory.getLogger("MatchingEngine");
	OrderRepository orderRepo = OrderRepository.getInstance();
	private static final String REJ01 = "Market order canceled due to no liquidity";

	public void match(OrderBook book, char aggressorSide) {
		synchronized (book) {

			while (!book.getBuyBook().isEmpty() && !book.getSellBook().isEmpty()) {

				Map.Entry<BigDecimal, Deque<Order>> bestBuyEntry = book.getBuyBook().firstEntry();

				Map.Entry<BigDecimal, Deque<Order>> bestSellEntry = book.getSellBook().firstEntry();

				Order buy = bestBuyEntry.getValue().peekFirst();
				Order sell = bestSellEntry.getValue().peekFirst();

				book.logBook("BEFORE MATCH");
				// price check
				if (!buy.isMarket() && !sell.isMarket()) {
					if (buy.getPrice().compareTo(sell.getPrice()) < 0) {
						break;
					}
				}

				BigDecimal tradeQty = buy.getLeavesQty().min(sell.getLeavesQty());

				logger.info("MATCHING -> buy={} sell={} tradeQty={}", buy.getClOrdId(), sell.getClOrdId(), tradeQty);

				// update leaves qty
				buy.setLeavesQty(buy.getLeavesQty().subtract(tradeQty));
				sell.setLeavesQty(sell.getLeavesQty().subtract(tradeQty));

				BigDecimal tradePrice;
				if (buy.isMarket() && !sell.isMarket()) {
					tradePrice = sell.getPrice(); // passive price
				} else if (!buy.isMarket() && sell.isMarket()) {
					tradePrice = buy.getPrice(); // passive price
				} else {
					// both limit
					tradePrice = (aggressorSide == '1') ? sell.getPrice() : buy.getPrice();
				}
				
				// update AVG price
				updateAvgPrice(buy, tradeQty, tradePrice);
				updateAvgPrice(sell, tradeQty, tradePrice);

				// send fills
				executions.trade(buy, sell, tradeQty, tradePrice);
				
				book.setLastTradedPrice(tradePrice);
				stopOrderManager.onTrade(book.getSymbol(), tradePrice);
				
				book.logBook("AFTER MATCH");
				/*
				 * logger.info( "POST TRADE -> buyCum={} buyAvgPx={} sellCum={} sellAvgPx={}",
				 * buy.getCumQty(), buy.getAvgPrice(), sell.getCumQty(), sell.getAvgPrice() );
				 */

				// update the orders in repository, set avg price
				//OrderRepository orderRepo = OrderRepository.getInstance();
				String buyClOrdID = buy.getClOrdId();
				String sellClOrdID = sell.getClOrdId();
				orderRepo.removeOrder(buyClOrdID);
				orderRepo.removeOrder(sellClOrdID);

				if (buy.getLeavesQty().signum() != 0) {
					// logger.info("partially filled buy order is updated");
					orderRepo.addOrder(buyClOrdID, buy);
				}
				if (sell.getLeavesQty().signum() != 0) {
					// logger.info("partially filled sell order is updated");
					orderRepo.addOrder(sellClOrdID, sell);
				}

				// remove filled BUY
				if (buy.getLeavesQty().signum() == 0) {
					bestBuyEntry.getValue().pollFirst();
					if (bestBuyEntry.getValue().isEmpty()) {
						book.getBuyBook().pollFirstEntry();
					}
				}

				// remove filled SELL
				if (sell.getLeavesQty().signum() == 0) {
					bestSellEntry.getValue().pollFirst();
					if (bestSellEntry.getValue().isEmpty()) {
						book.getSellBook().pollFirstEntry();
					}
				}
			}

			// Handle Market Orders
			while (!book.getBuyBook().isEmpty() && book.getSellBook().isEmpty()) {

				Map.Entry<BigDecimal, Deque<Order>> bestBuyEntry = book.getBuyBook().firstEntry();
				Order buy = bestBuyEntry.getValue().peekFirst();

				if (buy.isMarket()) {
					logger.info("Best Offer not present, cancelling Buy Order");
					executions.sendUnSolCancel(buy.getSessionID(), buy, buy.getClOrdId(), REJ01);
					orderRepo.removeOrder(buy.getClOrdId()); // removing from repo
					book.getBuyBook().pollFirstEntry(); // removing from book
				} else {
			        break; // ✅ EXIT LOOP when condition not met
			    }
			} 
			while (book.getBuyBook().isEmpty() && !book.getSellBook().isEmpty()) {
				Map.Entry<BigDecimal, Deque<Order>> bestSellEntry = book.getSellBook().firstEntry();
				Order sell = bestSellEntry.getValue().peekFirst();
				
				if (sell.isMarket()) {
					logger.info("Best Bid not present, cancelling Sell Order");
					executions.sendUnSolCancel(sell.getSessionID(), sell, sell.getClOrdId(), REJ01);
					orderRepo.removeOrder(sell.getClOrdId()); // removing from repo
					book.getSellBook().pollFirstEntry(); // removing from book
				} else {
			        break; // ✅ EXIT LOOP when condition not met
			    }
			}
		}

	}

	private void updateAvgPrice(Order order, BigDecimal tradeQty, BigDecimal tradePx) {

		BigDecimal oldCumQty = order.getCumQty();
		BigDecimal oldAvgPx = order.getAvgPrice();

		BigDecimal newCumQty = oldCumQty.add(tradeQty);

		BigDecimal newAvgPx;

		if (newCumQty.signum() == 0) {
			newAvgPx = BigDecimal.ZERO;
		} else {
			BigDecimal tradedValue = oldAvgPx.multiply(oldCumQty).add(tradePx.multiply(tradeQty));

			newAvgPx = tradedValue.divide(newCumQty, 8, RoundingMode.HALF_UP);
		}
		// store rounded value
		newAvgPx = newAvgPx.setScale(4, RoundingMode.HALF_UP);
		order.setCumQty(newCumQty);
		order.setAvgPrice(newAvgPx);
	}
}
