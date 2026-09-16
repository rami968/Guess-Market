package engine.dto;

import java.util.List;

/**
 * DTO containing Order Book statistics (LAST, BID, ASK, MID, SPREAD) and order lists.
 */
public class OrderBookStatsDTO {
    private final String optionName;
    private final double lastPrice;
    private final double highestBid;
    private final double lowestAsk;
    private final double midPrice;
    private final double spread;
    private final List<OrderDTO> buyOrders;
    private final List<OrderDTO> sellOrders;

    public OrderBookStatsDTO(String optionName, double lastPrice, double highestBid, double lowestAsk,
                             double midPrice, double spread, List<OrderDTO> buyOrders, List<OrderDTO> sellOrders) {
        this.optionName = optionName;
        this.lastPrice = lastPrice;
        this.highestBid = highestBid;
        this.lowestAsk = lowestAsk;
        this.midPrice = midPrice;
        this.spread = spread;
        this.buyOrders = buyOrders;
        this.sellOrders = sellOrders;
    }

    public String getOptionName() { return optionName; }
    public double getLastPrice() { return lastPrice; }
    public double getHighestBid() { return highestBid; }
    public double getLowestAsk() { return lowestAsk; }
    public double getMidPrice() { return midPrice; }
    public double getSpread() { return spread; }
    public List<OrderDTO> getBuyOrders() { return buyOrders; }
    public List<OrderDTO> getSellOrders() { return sellOrders; }
}
