package com.stockpulse;

import com.stockpulse.model.CryptoAsset;
import com.stockpulse.model.Portfolio;
import com.stockpulse.model.Stock;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PortfolioTest {

    @Test
    void applyBuyAveragesTheCostBasisAcrossMultiplePurchases() {
        Portfolio portfolio = new Portfolio("Tester", 10_000);

        portfolio.applyBuy("INFY", 10, 100.0);
        portfolio.applyBuy("INFY", 10, 200.0);

        assertEquals(20, portfolio.getHolding("INFY").getQuantity());
        assertEquals(150.0, portfolio.getHolding("INFY").getAverageBuyPrice(), 0.0001);
    }

    @Test
    void applySellRemovesTheHoldingEntirelyOnceQuantityReachesZero() {
        Portfolio portfolio = new Portfolio("Tester", 10_000);
        portfolio.applyBuy("INFY", 5, 100.0);

        portfolio.applySell("INFY", 5);

        assertNull(portfolio.getHolding("INFY"));
    }

    @Test
    void netWorthCombinesCashAndMarkedToMarketHoldings() {
        Portfolio portfolio = new Portfolio("Tester", 1_000);
        portfolio.applyBuy("INFY", 10, 100.0);
        portfolio.debitCash(1_000); // pretend the purchase was paid for

        Stock infy = new Stock("INFY", "Infosys", 120.0, "IT");
        double netWorth = portfolio.getNetWorth(Map.of("INFY", infy));

        assertEquals(1200.0, netWorth, 0.0001); // 0 cash + 10 * 120
    }

    @Test
    void unrealizedPnlIsPositiveWhenPriceRoseAboveAverageCost() {
        Portfolio portfolio = new Portfolio("Tester", 10_000);
        portfolio.applyBuy("BTC", 1, 100.0);

        CryptoAsset btc = new CryptoAsset("BTC", "Bitcoin", 150.0);
        double pnl = portfolio.getHolding("BTC").unrealizedPnl(btc.getCurrentPrice());

        assertTrue(pnl > 0, "expected a positive unrealized P&L, got " + pnl);
        assertEquals(50.0, pnl, 0.0001);
    }
}
