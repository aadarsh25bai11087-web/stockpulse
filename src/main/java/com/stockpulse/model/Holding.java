package com.stockpulse.model;

/**
 * A single line item in a portfolio: how much of one symbol is owned and
 * the average price it was accumulated at. Mutable and only ever touched
 * from inside a synchronized {@code TradingEngine} operation.
 */
public class Holding {

    private final String symbol;
    private int quantity;
    private double averageBuyPrice;

    public Holding(String symbol, int quantity, double averageBuyPrice) {
        this.symbol = symbol;
        this.quantity = quantity;
        this.averageBuyPrice = averageBuyPrice;
    }

    public String getSymbol() {
        return symbol;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getAverageBuyPrice() {
        return averageBuyPrice;
    }

    /** Folds a new buy into the running weighted-average cost basis. */
    public void applyBuy(int addedQuantity, double price) {
        double totalCost = (averageBuyPrice * quantity) + (price * addedQuantity);
        this.quantity += addedQuantity;
        this.averageBuyPrice = totalCost / this.quantity;
    }

    /** Reduces the position; average cost basis is unaffected by a sell. */
    public void applySell(int removedQuantity) {
        this.quantity -= removedQuantity;
    }

    public double unrealizedPnl(double currentPrice) {
        return (currentPrice - averageBuyPrice) * quantity;
    }

    public double marketValue(double currentPrice) {
        return currentPrice * quantity;
    }
}
