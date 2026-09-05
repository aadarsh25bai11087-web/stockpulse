package com.stockpulse.service;

import com.stockpulse.model.Asset;
import com.stockpulse.model.Holding;
import com.stockpulse.model.Portfolio;
import com.stockpulse.model.Transaction;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Turns raw portfolio/market/transaction state into human-readable reports. */
public class AnalyticsService {

    public String generateReport(Portfolio portfolio, Map<String, Asset> market, List<Transaction> history) {
        StringBuilder report = new StringBuilder();
        double holdingsValue = portfolio.getTotalHoldingsValue(market);
        double netWorth = portfolio.getNetWorth(market);

        report.append("===== Portfolio Report: ").append(portfolio.getOwnerName()).append(" =====\n");
        report.append(String.format("Cash balance:      %12.2f%n", portfolio.getCashBalance()));
        report.append(String.format("Holdings value:     %12.2f%n", holdingsValue));
        report.append(String.format("Net worth:          %12.2f%n", netWorth));
        report.append(String.format("Total trades:       %12d%n", history.size()));
        report.append('\n');

        if (portfolio.getHoldings().isEmpty()) {
            report.append("No open positions.\n");
        } else {
            report.append(String.format("%-6s %10s %14s %14s %14s%n", "SYM", "QTY", "AVG COST", "PRICE", "P&L"));
            for (Holding holding : portfolio.getHoldings()) {
                Asset asset = market.get(holding.getSymbol());
                double price = asset == null ? 0 : asset.getCurrentPrice();
                report.append(String.format("%-6s %10d %14.2f %14.2f %14.2f%n",
                        holding.getSymbol(), holding.getQuantity(), holding.getAverageBuyPrice(), price,
                        holding.unrealizedPnl(price)));
            }
        }

        bestPerformer(portfolio, market).ifPresent(h ->
                report.append("\nBest performer:  ").append(describePerformance(h, market)).append('\n'));
        worstPerformer(portfolio, market).ifPresent(h ->
                report.append("Worst performer: ").append(describePerformance(h, market)).append('\n'));

        return report.toString();
    }

    public Optional<Holding> bestPerformer(Portfolio portfolio, Map<String, Asset> market) {
        return portfolio.getHoldings().stream()
                .max(Comparator.comparingDouble(h -> pnlPercent(h, market)));
    }

    public Optional<Holding> worstPerformer(Portfolio portfolio, Map<String, Asset> market) {
        return portfolio.getHoldings().stream()
                .min(Comparator.comparingDouble(h -> pnlPercent(h, market)));
    }

    private String describePerformance(Holding holding, Map<String, Asset> market) {
        return String.format("%s (%.2f%%)", holding.getSymbol(), pnlPercent(holding, market));
    }

    private double pnlPercent(Holding holding, Map<String, Asset> market) {
        Asset asset = market.get(holding.getSymbol());
        if (asset == null || holding.getAverageBuyPrice() == 0) {
            return 0.0;
        }
        return ((asset.getCurrentPrice() - holding.getAverageBuyPrice()) / holding.getAverageBuyPrice()) * 100.0;
    }
}
