package engine.model;

import engine.lmsr.LMSRCalculator;
import java.io.Serializable;
import java.util.*;

/**
 * Manages state, accounting, and lifecycle for a single event (LMSR or Order Book).
 */
public class GMEvent implements Serializable {
    private static final long serialVersionUID = 2L;

    private final int id;
    private final String name;
    private final String description;
    private final int commission;
    private final CommissionType commissionType;
    private final String option1;
    private final String option2;
    private final EventType eventType;

    private String marketMakerName;

    // LMSR specific
    private final int b;
    private int q1;
    private int q2;

    // Order Book specific
    private final boolean allowMint;
    private final double initialInvestment;
    private final double d; // Base value denominator
    private final OrderBook orderBookOption1;
    private final OrderBook orderBookOption2;

    private double eventAccountBalance;
    private double totalCommissionCollected;

    private EventStatus status;
    private int winningOptionIndex; // 1 or 2, -1 if not closed

    private final List<Transaction> transactionHistory;

    /**
     * Constructor for LMSR Event
     */
    public GMEvent(int id, String name, String description, int commission,
                   CommissionType commissionType, String option1, String option2, int b) {
        this(id, name, description, commission, commissionType, option1, option2, EventType.LMSR, b, false, 0.0, 1.0);
    }

    /**
     * Master Constructor for both LMSR and Order Book Events
     */
    public GMEvent(int id, String name, String description, int commission,
                   CommissionType commissionType, String option1, String option2,
                   EventType eventType, int b, boolean allowMint, double initialInvestment, double d) {
        if (commission < 0 || commission > 90) {
            throw new IllegalArgumentException("Commission percentage must be between 0 and 90");
        }
        this.id = id;
        this.name = name;
        this.description = description;
        this.commission = commission;
        this.commissionType = commissionType;
        this.option1 = option1;
        this.option2 = option2;
        this.eventType = eventType;
        this.b = b;

        this.allowMint = allowMint;
        this.initialInvestment = initialInvestment;
        this.d = d <= 0 ? 1.0 : d;

        if (eventType == EventType.ORDER_BOOK) {
            this.orderBookOption1 = new OrderBook(option1, this.d);
            this.orderBookOption2 = new OrderBook(option2, this.d);
        } else {
            this.orderBookOption1 = null;
            this.orderBookOption2 = null;
        }

        this.q1 = 0;
        this.q2 = 0;
        this.eventAccountBalance = 0.0;
        this.totalCommissionCollected = 0.0;
        this.status = EventStatus.INACTIVE; // Starts inactive until MM activates it
        this.winningOptionIndex = -1;
        this.marketMakerName = "";
        this.transactionHistory = new ArrayList<>();
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getCommission() { return commission; }
    public CommissionType getCommissionType() { return commissionType; }
    public String getOption1() { return option1; }
    public String getOption2() { return option2; }
    public EventType getEventType() { return eventType; }
    public String getMarketMakerName() { return marketMakerName; }
    public void setMarketMakerName(String marketMakerName) { this.marketMakerName = marketMakerName; }

    public int getB() { return b; }
    public int getQ1() { return q1; }
    public int getQ2() { return q2; }

    public boolean isAllowMint() { return allowMint; }
    public double getInitialInvestment() { return initialInvestment; }
    public double getD() { return d; }

    public OrderBook getOrderBookOption1() { return orderBookOption1; }
    public OrderBook getOrderBookOption2() { return orderBookOption2; }

    public OrderBook getOrderBookForOption(String optionName) {
        if (option1.equalsIgnoreCase(optionName)) return orderBookOption1;
        if (option2.equalsIgnoreCase(optionName)) return orderBookOption2;
        throw new IllegalArgumentException("Unknown option name: " + optionName);
    }

    public double getEventAccountBalance() { return eventAccountBalance; }
    public double getTotalCommissionCollected() { return totalCommissionCollected; }
    public EventStatus getStatus() { return status; }
    public int getWinningOptionIndex() { return winningOptionIndex; }
    public List<Transaction> getTransactionHistory() { return Collections.unmodifiableList(transactionHistory); }

    public double getProbabilityOption1() {
        if (eventType == EventType.LMSR) {
            return LMSRCalculator.calculateProbabilityOption1(q1, q2, b);
        } else {
            return orderBookOption1 != null ? orderBookOption1.getMidPrice() / d : 0.5;
        }
    }

    public double getProbabilityOption2() {
        if (eventType == EventType.LMSR) {
            return LMSRCalculator.calculateProbabilityOption2(q1, q2, b);
        } else {
            return orderBookOption2 != null ? orderBookOption2.getMidPrice() / d : 0.5;
        }
    }

    /**
     * Activates the event by Market Maker.
     * Deducts subsidy (LMSR) or initial stock funding (OB) from MM balance into event account balance.
     */
    public synchronized void activateEvent(User mmUser) {
        if (status != EventStatus.INACTIVE) {
            throw new IllegalStateException("Event is already active or closed.");
        }
        if (mmUser == null || !mmUser.getName().equalsIgnoreCase(marketMakerName)) {
            throw new IllegalArgumentException("Only the assigned Market Maker (" + marketMakerName + ") can activate this event.");
        }

        if (eventType == EventType.LMSR) {
            double initialSubsidy = LMSRCalculator.calculateInitialSubsidy(b);
            mmUser.withdraw(initialSubsidy, "Initial LMSR subsidy for event #" + id + " (" + name + ")");
            this.eventAccountBalance += initialSubsidy;
        } else if (eventType == EventType.ORDER_BOOK) {
            if (initialInvestment > 0) {
                mmUser.withdraw(initialInvestment, "Initial stock investment for OB event #" + id + " (" + name + ")");
                this.eventAccountBalance += initialInvestment;

                // MM receives initial stock pairs (e.g., initialInvestment / d pairs of shares)
                int sharePairsCount = (int) (initialInvestment / d);
                if (sharePairsCount > 0) {
                    mmUser.addShares(id, option1, sharePairsCount, initialInvestment / 2.0);
                    mmUser.addShares(id, option2, sharePairsCount, initialInvestment / 2.0);
                }
            }
        }

        this.status = EventStatus.ACTIVE;
    }

    /**
     * Executes LMSR share purchase.
     */
    public synchronized PurchaseResult buySharesLMSR(User user, int optionIndex, int count) {
        if (status != EventStatus.ACTIVE) {
            throw new IllegalStateException("Event is not active.");
        }
        if (eventType != EventType.LMSR) {
            throw new IllegalStateException("This event does not use LMSR trading mechanism.");
        }
        if (count <= 0) {
            throw new IllegalArgumentException("Share count must be positive.");
        }
        if (optionIndex != 1 && optionIndex != 2) {
            throw new IllegalArgumentException("Invalid option index: must be 1 or 2.");
        }

        boolean isOption1 = (optionIndex == 1);
        double sharePrice = LMSRCalculator.calculatePurchasePrice(q1, q2, count, isOption1, b);
        double commissionAmount = 0.0;

        if (commissionType == CommissionType.ON_PURCHASE && commission > 0) {
            commissionAmount = sharePrice * (commission / 100.0);
            totalCommissionCollected += commissionAmount;
        }

        double totalUserPaid = sharePrice + commissionAmount;
        user.withdraw(totalUserPaid, "Purchase of " + count + " LMSR shares in event #" + id);

        eventAccountBalance += sharePrice;

        if (isOption1) {
            q1 += count;
        } else {
            q2 += count;
        }

        String chosenOptionName = isOption1 ? option1 : option2;
        user.addShares(id, chosenOptionName, count, totalUserPaid);

        Transaction tx = new Transaction(chosenOptionName, count, totalUserPaid);
        transactionHistory.add(0, tx);

        return new PurchaseResult(sharePrice, commissionAmount);
    }

    /**
     * Submits an Order to Order Book event.
     */
    public synchronized List<OrderBookTrade> submitOrder(Order order, Map<String, User> usersMap, User marketMaker) {
        if (status != EventStatus.ACTIVE) {
            throw new IllegalStateException("Event is not active.");
        }
        if (eventType != EventType.ORDER_BOOK) {
            throw new IllegalStateException("This event does not use Order Book trading mechanism.");
        }

        User orderOwner = usersMap.get(order.getUserName().toLowerCase());
        if (orderOwner != null) {
            if (order.getType() == OrderType.SELL) {
                int currentShares = orderOwner.getSharesCount(id, order.getOptionName());
                if (currentShares < order.getSharesCount()) {
                    throw new IllegalArgumentException(String.format("User '%s' only holds %d shares of '%s' (cannot submit SELL order for %d shares).",
                            orderOwner.getName(), currentShares, order.getOptionName(), order.getSharesCount()));
                }
            }
            orderOwner.recordOrderSubmission(id, order.getOptionName(), order.getType().name(), order.getSharesCount(), order.getPricePerShare());
        }

        OrderBook ob = getOrderBookForOption(order.getOptionName());
        boolean isOnPurchaseFee = (commissionType == CommissionType.ON_PURCHASE);

        List<OrderBookTrade> executedTrades = ob.addOrder(order, usersMap, commission, isOnPurchaseFee, marketMaker, id);

        // Auto-Minting check if enabled
        if (allowMint && order.getType() == OrderType.BUY && order.getSharesCount() > 0) {
            executeAutoMinting(order, usersMap, marketMaker, executedTrades);
        }

        // Record executed trades in event history
        for (OrderBookTrade t : executedTrades) {
            Transaction tx = new Transaction(t.getOptionName(), t.getSharesCount(), t.getTotalAmount() + t.getCommissionPaid());
            transactionHistory.add(0, tx);
        }

        return executedTrades;
    }

    private void executeAutoMinting(Order newBuyOrder, Map<String, User> usersMap, User marketMaker, List<OrderBookTrade> executedTrades) {
        if (!allowMint || newBuyOrder.getType() != OrderType.BUY || newBuyOrder.getSharesCount() <= 0) return;

        String otherOptionName = newBuyOrder.getOptionName().equalsIgnoreCase(option1) ? option2 : option1;
        OrderBook obCurrent = getOrderBookForOption(newBuyOrder.getOptionName());
        OrderBook obOther = getOrderBookForOption(otherOptionName);

        List<Order> otherBuyOrders = obOther.getMutableBuyOrders();
        Iterator<Order> iterator = otherBuyOrders.iterator();
        User newBuyer = usersMap.get(newBuyOrder.getUserName().toLowerCase());
        boolean isOnPurchaseFee = (commissionType == CommissionType.ON_PURCHASE);

        while (iterator.hasNext() && newBuyOrder.getSharesCount() > 0) {
            Order existingOtherBuy = iterator.next();
            if (newBuyOrder.getPricePerShare() + existingOtherBuy.getPricePerShare() >= d - 1e-9) {
                User otherBuyer = usersMap.get(existingOtherBuy.getUserName().toLowerCase());

                int mintShares = Math.min(newBuyOrder.getSharesCount(), existingOtherBuy.getSharesCount());

                double existingPrice = existingOtherBuy.getPricePerShare();
                double newPrice = d - existingPrice;
                if (newPrice < 0) newPrice = 0.0;

                double totalNewBase = mintShares * newPrice;
                double totalOtherBase = mintShares * existingPrice;

                double feeNew = (isOnPurchaseFee && commission > 0) ? totalNewBase * (commission / 100.0) : 0.0;
                double feeOther = (isOnPurchaseFee && commission > 0) ? totalOtherBase * (commission / 100.0) : 0.0;

                double totalNewCost = totalNewBase + feeNew;
                double totalOtherCost = totalOtherBase + feeOther;

                if (newBuyer.getBalance() < totalNewCost || otherBuyer.getBalance() < totalOtherCost) {
                    break;
                }

                newBuyer.withdraw(totalNewCost, "Auto-minting purchase of " + mintShares + " shares of " + newBuyOrder.getOptionName() + " in event #" + id);
                otherBuyer.withdraw(totalOtherCost, "Auto-minting purchase of " + mintShares + " shares of " + otherOptionName + " in event #" + id);

                this.eventAccountBalance += (totalNewBase + totalOtherBase);

                if (marketMaker != null && (feeNew + feeOther) > 0) {
                    marketMaker.deposit(feeNew + feeOther, "Commission fee from auto-minting in event #" + id);
                }

                newBuyer.addShares(id, newBuyOrder.getOptionName(), mintShares, totalNewCost);
                otherBuyer.addShares(id, otherOptionName, mintShares, totalOtherCost);

                newBuyOrder.consumeShares(mintShares);
                existingOtherBuy.consumeShares(mintShares);

                OrderBookTrade tradeNew = new OrderBookTrade(newBuyer.getName(), "Event (Mint)", newBuyOrder.getOptionName(), mintShares, newPrice, totalNewBase, feeNew);
                OrderBookTrade tradeOther = new OrderBookTrade(otherBuyer.getName(), "Event (Mint)", otherOptionName, mintShares, existingPrice, totalOtherBase, feeOther);

                obCurrent.addExecutedTrade(tradeNew);
                obOther.addExecutedTrade(tradeOther);

                executedTrades.add(tradeNew);

                if (existingOtherBuy.getSharesCount() == 0) {
                    iterator.remove();
                }
            }
        }
    }

    /**
     * Closes the event, determines the winner, pays out participants and MM.
     */
    public synchronized void closeEvent(User mmUser, int winningOptionIndex, Map<String, User> usersMap) {
        if (status != EventStatus.ACTIVE) {
            throw new IllegalStateException("Event is not active.");
        }
        if (mmUser == null || !mmUser.getName().equalsIgnoreCase(marketMakerName)) {
            throw new IllegalArgumentException("Only the Market Maker (" + marketMakerName + ") can close this event.");
        }
        if (winningOptionIndex != 1 && winningOptionIndex != 2) {
            throw new IllegalArgumentException("Invalid winning option index: must be 1 or 2.");
        }

        this.status = EventStatus.CLOSED;
        this.winningOptionIndex = winningOptionIndex;
        String winningOptionName = (winningOptionIndex == 1) ? option1 : option2;

        double payoutPerShare = (eventType == EventType.ORDER_BOOK) ? d : 1.0;

        // Distribute payouts to all winning share holders
        for (User u : usersMap.values()) {
            int winningShares = u.getSharesCount(id, winningOptionName);
            if (winningShares > 0) {
                double grossPayout = winningShares * payoutPerShare;
                double fee = 0.0;
                if (commissionType == CommissionType.ON_CLOSE && commission > 0) {
                    fee = grossPayout * (commission / 100.0);
                    totalCommissionCollected += fee;
                    if (mmUser != null) {
                        mmUser.deposit(fee, "On-close commission fee from user " + u.getName() + " for event #" + id);
                    }
                }
                double netPayout = grossPayout - fee;
                u.deposit(netPayout, "Winning payout for " + winningShares + " shares in event #" + id);
                if (eventAccountBalance >= netPayout) {
                    eventAccountBalance -= netPayout;
                }
            }
        }

        // Return remaining subsidy to Market Maker if LMSR
        if (eventType == EventType.LMSR && eventAccountBalance > 0 && mmUser != null) {
            mmUser.deposit(eventAccountBalance, "Return of remaining subsidy from event #" + id);
            eventAccountBalance = 0.0;
        }
    }

    public static class PurchaseResult {
        private final double sharePrice;
        private final double commissionPaid;

        public PurchaseResult(double sharePrice, double commissionPaid) {
            this.sharePrice = sharePrice;
            this.commissionPaid = commissionPaid;
        }

        public double getSharePrice() { return sharePrice; }
        public double getCommissionPaid() { return commissionPaid; }
        public double getTotalPaid() { return sharePrice + commissionPaid; }
    }
}
