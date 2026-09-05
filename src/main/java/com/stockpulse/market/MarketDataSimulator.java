package com.stockpulse.market;

import com.stockpulse.model.Asset;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Runs on its own background thread and periodically nudges every asset's
 * price by a random amount within that asset's volatility band.
 *
 * <p>This is the concurrency centrepiece of the project: this thread
 * mutates prices while the {@code TradingEngine} reads and trades against
 * them from the main/console thread at the same time. Safety does not come
 * from a lock here - each {@link Asset} publishes its price through an
 * {@code AtomicReference} (see {@link Asset#updatePrice}), so a reader
 * always sees a fully-formed, current value with no torn reads and no
 * blocking. The engine still wraps an entire buy/sell in a
 * {@code synchronized} block so a trade's own read-price-then-debit-cash
 * sequence is atomic even though the price itself may move again the
 * instant after it's read.
 */
public class MarketDataSimulator implements Runnable {

    private final Map<String, Asset> market;
    private final long tickIntervalMillis;
    private final Random random = new Random();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread workerThread;

    public MarketDataSimulator(Map<String, Asset> market, long tickIntervalMillis) {
        this.market = market;
        this.tickIntervalMillis = tickIntervalMillis;
    }

    public synchronized void start() {
        if (running.compareAndSet(false, true)) {
            workerThread = new Thread(this, "market-data-simulator");
            workerThread.setDaemon(true);
            workerThread.start();
        }
    }

    public synchronized void stop() {
        running.set(false);
        if (workerThread != null) {
            workerThread.interrupt();
        }
    }

    public boolean isRunning() {
        return running.get();
    }

    @Override
    public void run() {
        while (running.get()) {
            tick();
            try {
                Thread.sleep(tickIntervalMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /** Applies one random price movement to every asset. Package-visible for tests. */
    void tick() {
        for (Asset asset : market.values()) {
            double volatility = asset.getVolatilityFactor();
            double changePercent = (random.nextDouble() * 2 - 1) * volatility;
            double newPrice = asset.getCurrentPrice() * (1 + changePercent);
            asset.updatePrice(round(Math.max(newPrice, 0.01)));
        }
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
