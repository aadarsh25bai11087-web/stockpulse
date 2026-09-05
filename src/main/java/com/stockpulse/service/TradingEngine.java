package com.stockpulse.service;

import com.stockpulse.dao.TransactionDao;
import com.stockpulse.exception.InsufficientFundsException;
import com.stockpulse.exception.InsufficientHoldingsException;
import com.stockpulse.exception.InvalidSymbolException;
import com.stockpulse.model.Asset;
import com.stockpulse.model.Holding;
import com.stockpulse.model.OrderType;
import com.stockpulse.model.Portfolio;
import com.stockpulse.model.Transaction;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Executes buy/sell orders against a {@link Portfolio} using live prices
 * from the shared market map that {@code MarketDataSimulator} mutates
 * concurrently.
 *
 * <p>Each order method is {@code synchronized} on this engine instance so
 * that "read the current price, check funds/holdings, then mutate the
 * portfolio and log the transaction" happens as one atomic step per
 * engine. Without this, two trades issued back-to-back (or, in a future
 * multi-client version, from two different threads) could both read the
 * same starting cash balance and both be approved when only one should
 * have been.
 */
public class TradingEngine {

    private final Portfolio portfolio;
    private final Map<String, Asset> market;
    private final TransactionDao transactionDao;
    private final int portfolioId;

    public TradingEngine(Portfolio portfolio, Map<String, Asset> market, TransactionDao transactionDao,
                          int portfolioId) {
        this.portfolio = portfolio;
        this.market = market;
        this.transactionDao = transactionDao;
        this.portfolioId = portfolioId;
    }

    public synchronized Transaction buy(String symbol, int quantity)
            throws InvalidSymbolException, InsufficientFundsException {
        requirePositiveQuantity(quantity);
        Asset asset = requireAsset(symbol);

        double price = asset.getCurrentPrice();
        double cost = price * quantity;
        if (cost > portfolio.getCashBalance()) {
            throw new InsufficientFundsException(cost, portfolio.getCashBalance());
        }

        portfolio.debitCash(cost);
        portfolio.applyBuy(asset.getSymbol(), quantity, price);
        return logTransaction(asset.getSymbol(), OrderType.BUY, quantity, price);
    }

    public synchronized Transaction sell(String symbol, int quantity)
            throws InvalidSymbolException, InsufficientHoldingsException {
        requirePositiveQuantity(quantity);
        Asset asset = requireAsset(symbol);

        Holding holding = portfolio.getHolding(asset.getSymbol());
        int owned = holding == null ? 0 : holding.getQuantity();
        if (quantity > owned) {
            throw new InsufficientHoldingsException(asset.getSymbol(), quantity, owned);
        }

        double price = asset.getCurrentPrice();
        double proceeds = price * quantity;
        portfolio.applySell(asset.getSymbol(), quantity);
        portfolio.creditCash(proceeds);
        return logTransaction(asset.getSymbol(), OrderType.SELL, quantity, price);
    }

    private Transaction logTransaction(String symbol, OrderType orderType, int quantity, double price) {
        return transactionDao.insert(portfolioId, symbol, orderType, quantity, price, LocalDateTime.now());
    }

    private Asset requireAsset(String symbol) throws InvalidSymbolException {
        Asset asset = market.get(symbol.toUpperCase());
        if (asset == null) {
            throw new InvalidSymbolException(symbol);
        }
        return asset;
    }

    private void requirePositiveQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive, got " + quantity);
        }
    }
}
