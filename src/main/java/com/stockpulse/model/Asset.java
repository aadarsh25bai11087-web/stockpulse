package com.stockpulse.model;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Base class for every tradable instrument in the market.
 *
 * <p>{@code currentPrice} is mutated by the background
 * {@code MarketDataSimulator} thread while it is concurrently read by the
 * {@code TradingEngine} and the console UI on other threads. It is stored in
 * an {@link AtomicReference} rather than a plain {@code double} field so
 * every read/write is atomic without needing a lock just to look at a price.
 */
public abstract class Asset implements Tradable {

    private final String symbol;
    private final String name;
    private final AtomicReference<Double> currentPrice;

    protected Asset(String symbol, String name, double initialPrice) {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("Asset symbol must not be blank");
        }
        if (initialPrice <= 0) {
            throw new IllegalArgumentException("Initial price must be positive");
        }
        this.symbol = symbol.toUpperCase();
        this.name = name;
        this.currentPrice = new AtomicReference<>(initialPrice);
    }

    @Override
    public String getSymbol() {
        return symbol;
    }

    public String getName() {
        return name;
    }

    @Override
    public double getCurrentPrice() {
        return currentPrice.get();
    }

    /**
     * Atomically updates the price and returns the previous value, so the
     * simulator can log a price delta without a separate read-then-write
     * that could race with a trade.
     */
    public double updatePrice(double newPrice) {
        if (newPrice <= 0) {
            throw new IllegalArgumentException("Price must remain positive");
        }
        return currentPrice.getAndSet(newPrice);
    }

    /**
     * Fractional per-tick volatility band (e.g. 0.02 = up to +/-2% per
     * tick). Each concrete asset class defines its own realistic band,
     * which is what gives the market simulation meaningfully different
     * behaviour per asset type instead of one generic random walk.
     */
    public abstract double getVolatilityFactor();

    @Override
    public String toString() {
        return String.format("%-6s %-22s %10.2f  [%s]", symbol, name, getCurrentPrice(), getType());
    }
}
