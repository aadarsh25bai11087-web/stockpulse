package com.stockpulse.market;

import com.stockpulse.model.Asset;
import com.stockpulse.model.CryptoAsset;
import com.stockpulse.model.Stock;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MarketDataSimulatorTest {

    @Test
    void oneTickMovesEveryPriceByNoMoreThanItsVolatilityBand() {
        Map<String, Asset> market = new LinkedHashMap<>();
        market.put("INFY", new Stock("INFY", "Infosys", 100.0, "IT"));
        market.put("BTC", new CryptoAsset("BTC", "Bitcoin", 1000.0));
        MarketDataSimulator simulator = new MarketDataSimulator(market, 100_000);

        simulator.tick();

        Asset infy = market.get("INFY");
        double infyMaxMove = 100.0 * infy.getVolatilityFactor();
        assertTrue(Math.abs(infy.getCurrentPrice() - 100.0) <= infyMaxMove + 0.01,
                "INFY moved further than its volatility band allows: " + infy.getCurrentPrice());

        Asset btc = market.get("BTC");
        double btcMaxMove = 1000.0 * btc.getVolatilityFactor();
        assertTrue(Math.abs(btc.getCurrentPrice() - 1000.0) <= btcMaxMove + 0.01,
                "BTC moved further than its volatility band allows: " + btc.getCurrentPrice());
    }

    @Test
    void startAndStopCleanlyToggleTheRunningFlagWithoutThrowing() throws InterruptedException {
        Map<String, Asset> market = new LinkedHashMap<>();
        market.put("INFY", new Stock("INFY", "Infosys", 100.0, "IT"));
        MarketDataSimulator simulator = new MarketDataSimulator(market, 50);

        simulator.start();
        assertTrue(simulator.isRunning());
        Thread.sleep(120); // let it tick at least once on the background thread
        simulator.stop();

        assertTrue(!simulator.isRunning());
    }
}
