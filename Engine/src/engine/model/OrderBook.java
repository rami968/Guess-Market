package engine.model;

import java.util.*;

/**
 * Manages the Order Book for a single option in an event.
 * Contains pending buy orders (bids) and sell orders (asks),
 * performs automatic trade matching, and maintains price statistics.
 */
public class OrderBook {
    private final String optionName;
    private final double baseValue; // Denominator 'd'

    // Buy orders (BIDs) sorted by price DESCENDING, then timestamp ASCENDING
    private final List<Order> buyOrders;

    // Sell orders (ASKs) sorted by price ASCENDING, then timestamp ASCENDING
    private final List<Order> sellOrders;

    // Executed trade history
    private final List<OrderBookTrade> tradeHistory;

    private double lastPrice;

    public OrderBook(String optionName, double baseValue) {
        this.optionName = optionName;
        this.baseValue = baseValue;
        this.buyOrders = new ArrayList<>();
        this.sellOrders = new ArrayList<>();
        this.tradeHistory = new ArrayList<>();
        this.lastPrice = 0.0;
    }

    public String getOptionName() { return optionName; }
    public double getBaseValue() { return baseValue; }
    public double getLastPrice() { return lastPrice; }
    public void setLastPrice(double lastPrice) { this.lastPrice = lastPrice; }

    public List<Order> getBuyOrders() {
        return Collections.unmodifiableList(buyOrders);
    }

    public List<Order> getSellOrders() {
        return Collections.unmodifiableList(sellOrders);
    }

    public List<OrderBookTrade> getTradeHistory() {
        return Collections.unmodifiableList(tradeHistory);
    }

    /**
     * Highest bid price currently in the order book.
     */
    public double getHighestBid() {
        if (buyOrders.isEmpty()) return 0.0;
        return buyOrders.stream().mapToDouble(Order::getPricePerShare).max().orElse(0.0);
    }

    /**
     * Lowest ask price currently in the order book.
     */
    public double getLowestAsk() {
        if (sellOrders.isEmpty()) return 0.0;
        return sellOrders.stream().mapToDouble(Order::getPricePerShare).min().orElse(0.0);
    }

    /**
     * Mid price: average of highest bid and lowest ask.
     */
    public double getMidPrice() {
        double bid = getHighestBid();
        double ask = getLowestAsk();
        if (bid > 0 && ask > 0) {
            return (bid + ask) / 2.0;
        } else if (lastPrice > 0) {
            return lastPrice;
        }
        return 0.0;
    }

    /**
     * Spread: difference between lowest ask and highest bid.
     */
    public double getSpread() {
        double bid = getHighestBid();
        double ask = getLowestAsk();
        if (bid > 0 && ask > 0) {
            return ask - bid;
        }
        return 0.0;
    }

    /**
     * Adds an order to the order book and triggers trade matching.
     * @return List of executed trades resulting from this order.
     */
    public synchronized List<OrderBookTrade> addOrder(Order newOrder, Map<String, User> usersMap, double commissionPercent, boolean isOnPurchaseFee, User marketMaker, int eventId) {
        List<OrderBookTrade> executedTrades = new ArrayList<>();

        if (newOrder.getType() == OrderType.BUY) {
            matchBuyOrder(newOrder, usersMap, commissionPercent, isOnPurchaseFee, marketMaker, eventId, executedTrades);
            if (newOrder.getSharesCount() > 0) {
                buyOrders.add(newOrder);
                sortOrders();
            }
        } else {
            matchSellOrder(newOrder, usersMap, commissionPercent, isOnPurchaseFee, marketMaker, eventId, executedTrades);
            if (newOrder.getSharesCount() > 0) {
                sellOrders.add(newOrder);
                sortOrders();
            }
        }

        return executedTrades;
    }

    private void matchBuyOrder(Order buyOrder, Map<String, User> usersMap, double commissionPercent, boolean isOnPurchaseFee, User marketMaker, int eventId, List<OrderBookTrade> executedTrades) {
        Iterator<Order> iterator = sellOrders.iterator();
        User buyer = usersMap.get(buyOrder.getUserName().toLowerCase());

        while (iterator.hasNext() && buyOrder.getSharesCount() > 0) {
            Order sellOrder = iterator.next();
            // Matching condition: Buy price >= Sell price
            if (buyOrder.getPricePerShare() >= sellOrder.getPricePerShare()) {
                User seller = usersMap.get(sellOrder.getUserName().toLowerCase());
                int tradeShares = Math.min(buyOrder.getSharesCount(), sellOrder.getSharesCount());
                double executionPrice = sellOrder.getPricePerShare(); // Execution price is the existing order's price
                double tradeTotal = tradeShares * executionPrice;

                // Commission calculation (if on-purchase fee)
                double commissionFee = 0.0;
                if (isOnPurchaseFee && commissionPercent > 0) {
                    commissionFee = tradeTotal * (commissionPercent / 100.0);
                }

                double buyerTotalCost = tradeTotal + commissionFee;

                // Check buyer balance
                if (buyer.getBalance() < buyerTotalCost) {
                    break; // Buyer cannot afford remaining shares
                }

                // Execute trade: transfer funds and shares
                buyer.withdraw(buyerTotalCost, "Purchase of " + tradeShares + " shares of " + optionName + " in event #" + eventId);
                seller.deposit(tradeTotal, "Sale of " + tradeShares + " shares of " + optionName + " in event #" + eventId);
                
                if (commissionFee > 0 && marketMaker != null) {
                    marketMaker.deposit(commissionFee, "Commission fee from trade in option " + optionName + " in event #" + eventId);
                }

                // Update holdings with real eventId
                buyer.addShares(eventId, optionName, tradeShares, buyerTotalCost);
                seller.removeShares(eventId, optionName, tradeShares);

                // Update orders
                buyOrder.consumeShares(tradeShares);
                sellOrder.consumeShares(tradeShares);
                this.lastPrice = executionPrice;

                OrderBookTrade trade = new OrderBookTrade(buyer.getName(), seller.getName(), optionName, tradeShares, executionPrice, tradeTotal, commissionFee);
                executedTrades.add(trade);
                tradeHistory.add(trade);

                if (sellOrder.getSharesCount() == 0) {
                    iterator.remove();
                }
            }
        }
    }

    private void matchSellOrder(Order sellOrder, Map<String, User> usersMap, double commissionPercent, boolean isOnPurchaseFee, User marketMaker, int eventId, List<OrderBookTrade> executedTrades) {
        Iterator<Order> iterator = buyOrders.iterator();
        User seller = usersMap.get(sellOrder.getUserName().toLowerCase());

        while (iterator.hasNext() && sellOrder.getSharesCount() > 0) {
            Order buyOrder = iterator.next();
            // Matching condition: Sell price <= Buy price
            if (sellOrder.getPricePerShare() <= buyOrder.getPricePerShare()) {
                User buyer = usersMap.get(buyOrder.getUserName().toLowerCase());
                int tradeShares = Math.min(sellOrder.getSharesCount(), buyOrder.getSharesCount());
                double executionPrice = buyOrder.getPricePerShare();
                double tradeTotal = tradeShares * executionPrice;

                double commissionFee = 0.0;
                if (isOnPurchaseFee && commissionPercent > 0) {
                    commissionFee = tradeTotal * (commissionPercent / 100.0);
                }

                double buyerTotalCost = tradeTotal + commissionFee;

                if (buyer.getBalance() < buyerTotalCost) {
                    iterator.remove();
                    continue;
                }

                buyer.withdraw(buyerTotalCost, "Purchase of " + tradeShares + " shares of " + optionName + " in event #" + eventId);
                seller.deposit(tradeTotal, "Sale of " + tradeShares + " shares of " + optionName + " in event #" + eventId);

                if (commissionFee > 0 && marketMaker != null) {
                    marketMaker.deposit(commissionFee, "Commission fee from trade in option " + optionName + " in event #" + eventId);
                }

                buyer.addShares(eventId, optionName, tradeShares, buyerTotalCost);
                seller.removeShares(eventId, optionName, tradeShares);

                sellOrder.consumeShares(tradeShares);
                buyOrder.consumeShares(tradeShares);
                this.lastPrice = executionPrice;

                OrderBookTrade trade = new OrderBookTrade(buyer.getName(), seller.getName(), optionName, tradeShares, executionPrice, tradeTotal, commissionFee);
                executedTrades.add(trade);
                tradeHistory.add(trade);

                if (buyOrder.getSharesCount() == 0) {
                    iterator.remove();
                }
            }
        }
    }

    private void sortOrders() {
        // Buy orders: price DESCENDING, then timestamp ASCENDING
        buyOrders.sort((a, b) -> {
            int priceCmp = Double.compare(b.getPricePerShare(), a.getPricePerShare());
            if (priceCmp != 0) return priceCmp;
            return Long.compare(a.getTimestamp(), b.getTimestamp());
        });

        // Sell orders: price ASCENDING, then timestamp ASCENDING
        sellOrders.sort((a, b) -> {
            int priceCmp = Double.compare(a.getPricePerShare(), b.getPricePerShare());
            if (priceCmp != 0) return priceCmp;
            return Long.compare(a.getTimestamp(), b.getTimestamp());
        });
    }
}
