package com.stockpulse;

import com.stockpulse.dao.PortfolioDao;
import com.stockpulse.dao.TransactionDao;
import com.stockpulse.db.DatabaseManager;
import com.stockpulse.exception.InsufficientFundsException;
import com.stockpulse.exception.InsufficientHoldingsException;
import com.stockpulse.exception.InvalidSymbolException;
import com.stockpulse.model.Asset;
import com.stockpulse.model.CryptoAsset;
import com.stockpulse.model.Portfolio;
import com.stockpulse.model.Stock;
import com.stockpulse.model.Transaction;
import com.stockpulse.service.TradingEngine;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Runs against a throwaway in-memory H2 database (one per test method) so
 * transaction logging is exercised end-to-end without touching the
 * project's real data file.
 */
class TradingEngineTest {

    private DatabaseManager databaseManager;
    private TransactionDao transactionDao;
    private Map<String, Asset> market;
    private Portfolio portfolio;
    private TradingEngine tradingEngine;

    @BeforeEach
    void setUp() {
        String url = "jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
        databaseManager = new DatabaseManager(url);
        transactionDao = new TransactionDao(databaseManager.getConnection());

        market = new LinkedHashMap<>();
        market.put("INFY", new Stock("INFY", "Infosys", 100.0, "IT"));
        market.put("BTC", new CryptoAsset("BTC", "Bitcoin", 1000.0));

        // transactions.portfolio_id is a foreign key, so a real portfolio row
        // must exist before any trade can be logged.
        portfolio = new PortfolioDao(databaseManager.getConnection())
                .createNew("Tester", 1_000.0);
        tradingEngine = new TradingEngine(portfolio, market, transactionDao, PortfolioDao.DEFAULT_PORTFOLIO_ID);
    }

    @AfterEach
    void tearDown() {
        databaseManager.close();
    }

    @Test
    void buySucceedsAndDebitsCashWhenFundsAreSufficient() throws Exception {
        Transaction transaction = tradingEngine.buy("INFY", 5);

        assertEquals(500.0, transaction.getTotalValue(), 0.0001);
        assertEquals(500.0, portfolio.getCashBalance(), 0.0001);
        assertEquals(5, portfolio.getHolding("INFY").getQuantity());
    }

    @Test
    void buyThrowsInsufficientFundsWhenOrderCostsMoreThanAvailableCash() {
        assertThrows(InsufficientFundsException.class, () -> tradingEngine.buy("BTC", 5));
    }

    @Test
    void buyThrowsInvalidSymbolForAnUnknownTicker() {
        assertThrows(InvalidSymbolException.class, () -> tradingEngine.buy("DOGE", 1));
    }

    @Test
    void sellThrowsInsufficientHoldingsWhenSellingMoreThanOwned() {
        assertThrows(InsufficientHoldingsException.class, () -> tradingEngine.sell("INFY", 1));
    }

    @Test
    void sellCreditsCashAndReducesTheHoldingAfterABuy() throws Exception {
        tradingEngine.buy("INFY", 10);

        Transaction sale = tradingEngine.sell("INFY", 4);

        assertEquals(6, portfolio.getHolding("INFY").getQuantity());
        assertEquals(400.0, sale.getTotalValue(), 0.0001);
    }

    @Test
    void everyExecutedTradeIsPersistedToTheTransactionLog() throws Exception {
        tradingEngine.buy("INFY", 2);
        tradingEngine.sell("INFY", 1);

        assertEquals(2, transactionDao.findAll(1).size());
    }
}
