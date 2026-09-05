package com.stockpulse.model;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A single investor's cash balance and holdings. Holdings are stored in a
 * {@link ConcurrentHashMap} because the analytics/report path may iterate
 * them from the console thread while a trade could in principle be applied
 * from another thread; the actual mutation of an individual holding is
 * still serialized through {@code TradingEngine}'s synchronized methods.
 */
public class Portfolio {

    private final String ownerName;
    private double cashBalance;
    private final Map<String, Holding> holdings = new ConcurrentHashMap<>();

    public Portfolio(String ownerName, double startingCash) {
        this.ownerName = ownerName;
        this.cashBalance = startingCash;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public double getCashBalance() {
        return cashBalance;
    }

    public void debitCash(double amount) {
        this.cashBalance -= amount;
    }

    public void creditCash(double amount) {
        this.cashBalance += amount;
    }

    public Holding getHolding(String symbol) {
        return holdings.get(symbol);
    }

    public Collection<Holding> getHoldings() {
        return holdings.values();
    }

    public void restoreHolding(Holding holding) {
        holdings.put(holding.getSymbol(), holding);
    }

    public void applyBuy(String symbol, int quantity, double price) {
        holdings.computeIfAbsent(symbol, s -> new Holding(s, 0, 0.0)).applyBuy(quantity, price);
    }

    /** Removes the holding entirely once its quantity drops to zero. */
    public void applySell(String symbol, int quantity) {
        Holding holding = holdings.get(symbol);
        holding.applySell(quantity);
        if (holding.getQuantity() == 0) {
            holdings.remove(symbol);
        }
    }

    public double getTotalHoldingsValue(Map<String, ? extends Tradable> market) {
        double total = 0.0;
        for (Holding holding : holdings.values()) {
            Tradable asset = market.get(holding.getSymbol());
            if (asset != null) {
                total += holding.marketValue(asset.getCurrentPrice());
            }
        }
        return total;
    }

    public double getNetWorth(Map<String, ? extends Tradable> market) {
        return cashBalance + getTotalHoldingsValue(market);
    }
}
