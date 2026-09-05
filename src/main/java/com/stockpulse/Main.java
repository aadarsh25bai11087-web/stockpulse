package com.stockpulse;

import com.stockpulse.dao.AssetDao;
import com.stockpulse.dao.PortfolioDao;
import com.stockpulse.dao.TransactionDao;
import com.stockpulse.db.DatabaseManager;
import com.stockpulse.cli.ConsoleUI;
import com.stockpulse.io.TradeLogger;
import com.stockpulse.market.MarketDataSimulator;
import com.stockpulse.model.Asset;
import com.stockpulse.model.CryptoAsset;
import com.stockpulse.model.MutualFund;
import com.stockpulse.model.Portfolio;
import com.stockpulse.model.Stock;
import com.stockpulse.service.TradingEngine;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/** Application entry point: wires up persistence, the market simulator, and the console UI. */
public final class Main {

    private static final double STARTING_CASH = 100_000.00;
    private static final long PRICE_TICK_INTERVAL_MS = 3000;

    private Main() {
    }

    public static void main(String[] args) {
        DatabaseManager databaseManager = new DatabaseManager();
        AssetDao assetDao = new AssetDao(databaseManager.getConnection());
        PortfolioDao portfolioDao = new PortfolioDao(databaseManager.getConnection());
        TransactionDao transactionDao = new TransactionDao(databaseManager.getConnection());

        if (assetDao.isEmpty()) {
            assetDao.seed(defaultAssets());
        }
        Map<String, Asset> market = assetDao.loadAll();

        Portfolio portfolio = portfolioDao.load()
                .orElseGet(() -> portfolioDao.createNew("Aadarsh", STARTING_CASH));

        MarketDataSimulator simulator = new MarketDataSimulator(market, PRICE_TICK_INTERVAL_MS);
        simulator.start();

        TradingEngine tradingEngine = new TradingEngine(
                portfolio, market, transactionDao, PortfolioDao.DEFAULT_PORTFOLIO_ID);
        TradeLogger tradeLogger = new TradeLogger(Path.of("data", "trade_log.txt"));

        // Guards against running the save-and-close sequence twice: once for
        // a normal menu exit (called directly, below) and once more from the
        // JVM shutdown hook that only exists to catch Ctrl+C / kill. The two
        // must never both touch the database - H2 registers its own internal
        // shutdown hook, and the JVM runs all shutdown hooks concurrently on
        // separate threads with no ordering guarantee, so racing our own
        // hook against a normal, already-completed shutdown corrupts the
        // save (this was observed and fixed during testing: see README).
        AtomicBoolean alreadyShutDown = new AtomicBoolean(false);
        Runnable shutdownOnce = () -> {
            if (alreadyShutDown.compareAndSet(false, true)) {
                shutdown(simulator, assetDao, portfolioDao, market, portfolio, databaseManager);
            }
        };
        Runtime.getRuntime().addShutdownHook(new Thread(shutdownOnce, "shutdown-fallback"));

        ConsoleUI consoleUI = new ConsoleUI(
                portfolio, market, tradingEngine, transactionDao, tradeLogger, PortfolioDao.DEFAULT_PORTFOLIO_ID);
        consoleUI.run();

        // Normal exit path: save synchronously, on this thread, before the
        // JVM starts tearing down. The registered hook above only fires as a
        // fallback for abrupt termination (Ctrl+C / kill) and is a no-op here.
        shutdownOnce.run();
    }

    private static void shutdown(MarketDataSimulator simulator, AssetDao assetDao, PortfolioDao portfolioDao,
                                  Map<String, Asset> market, Portfolio portfolio, DatabaseManager databaseManager) {
        simulator.stop();
        portfolioDao.save(portfolio);
        assetDao.savePrices(market);
        databaseManager.close();
    }

    private static List<Asset> defaultAssets() {
        return List.of(
                new Stock("INFY", "Infosys Ltd", 1500.00, "Information Technology"),
                new Stock("TATA", "Tata Motors Ltd", 950.00, "Automobile"),
                new Stock("HDFC", "HDFC Bank Ltd", 1650.00, "Banking"),
                new CryptoAsset("BTC", "Bitcoin", 5_800_000.00),
                new CryptoAsset("ETH", "Ethereum", 310_000.00),
                new MutualFund("NIFB", "Nifty Bluechip Fund", 250.00, 0.85));
    }
}
