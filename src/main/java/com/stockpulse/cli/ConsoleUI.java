package com.stockpulse.cli;

import com.stockpulse.dao.TransactionDao;
import com.stockpulse.exception.InsufficientFundsException;
import com.stockpulse.exception.InsufficientHoldingsException;
import com.stockpulse.exception.InvalidSymbolException;
import com.stockpulse.io.CsvExporter;
import com.stockpulse.io.TradeLogger;
import com.stockpulse.model.Asset;
import com.stockpulse.model.Portfolio;
import com.stockpulse.model.Transaction;
import com.stockpulse.service.AnalyticsService;
import com.stockpulse.service.TradingEngine;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/** A small text menu that drives the trading engine and reports interactively. */
public class ConsoleUI {

    private final Scanner scanner = new Scanner(System.in);
    private final Portfolio portfolio;
    private final Map<String, Asset> market;
    private final TradingEngine tradingEngine;
    private final AnalyticsService analyticsService = new AnalyticsService();
    private final TransactionDao transactionDao;
    private final CsvExporter csvExporter = new CsvExporter();
    private final TradeLogger tradeLogger;
    private final int portfolioId;

    public ConsoleUI(Portfolio portfolio, Map<String, Asset> market, TradingEngine tradingEngine,
                      TransactionDao transactionDao, TradeLogger tradeLogger, int portfolioId) {
        this.portfolio = portfolio;
        this.market = market;
        this.tradingEngine = tradingEngine;
        this.transactionDao = transactionDao;
        this.tradeLogger = tradeLogger;
        this.portfolioId = portfolioId;
    }

    public void run() {
        boolean running = true;
        printWelcome();
        while (running) {
            printMenu();
            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1" -> viewMarket();
                case "2" -> viewPortfolio();
                case "3" -> placeOrder(com.stockpulse.model.OrderType.BUY);
                case "4" -> placeOrder(com.stockpulse.model.OrderType.SELL);
                case "5" -> viewHistory();
                case "6" -> viewAnalytics();
                case "7" -> exportHoldings();
                case "8" -> exportTransactions();
                case "0" -> running = false;
                default -> System.out.println("Not a valid option, try again.");
            }
        }
        System.out.println("Goodbye!");
    }

    private void printWelcome() {
        System.out.println("=================================================");
        System.out.println(" StockPulse - Multi-threaded Portfolio Simulator");
        System.out.println(" Prices update live in the background every few");
        System.out.println(" seconds while you trade.");
        System.out.println("=================================================");
    }

    private void printMenu() {
        System.out.println();
        System.out.println("1) View market   2) View portfolio   3) Buy   4) Sell");
        System.out.println("5) Transaction history   6) Analytics report");
        System.out.println("7) Export holdings CSV   8) Export transactions CSV   0) Exit");
        System.out.print("> ");
    }

    private void viewMarket() {
        System.out.println();
        for (Asset asset : market.values()) {
            System.out.println(asset);
        }
    }

    private void viewPortfolio() {
        System.out.println();
        System.out.printf("Owner: %s   Cash: %.2f%n", portfolio.getOwnerName(), portfolio.getCashBalance());
        if (portfolio.getHoldings().isEmpty()) {
            System.out.println("No open positions.");
            return;
        }
        portfolio.getHoldings().forEach(h -> System.out.printf(
                "  %-6s qty=%-6d avgCost=%.2f%n", h.getSymbol(), h.getQuantity(), h.getAverageBuyPrice()));
    }

    private void placeOrder(com.stockpulse.model.OrderType orderType) {
        System.out.print("Symbol: ");
        String symbol = scanner.nextLine().trim();
        System.out.print("Quantity: ");
        String quantityInput = scanner.nextLine().trim();

        try {
            int quantity = Integer.parseInt(quantityInput);
            Transaction transaction = orderType == com.stockpulse.model.OrderType.BUY
                    ? tradingEngine.buy(symbol, quantity)
                    : tradingEngine.sell(symbol, quantity);
            tradeLogger.log(transaction);
            System.out.println("Executed: " + transaction);
        } catch (NumberFormatException e) {
            System.out.println("Quantity must be a whole number: " + quantityInput);
        } catch (InvalidSymbolException | InsufficientFundsException | InsufficientHoldingsException e) {
            System.out.println("Order rejected: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid order: " + e.getMessage());
        }
    }

    private void viewHistory() {
        System.out.println();
        List<Transaction> history = transactionDao.findAll(portfolioId);
        if (history.isEmpty()) {
            System.out.println("No transactions yet.");
            return;
        }
        history.forEach(System.out::println);
    }

    private void viewAnalytics() {
        System.out.println();
        List<Transaction> history = transactionDao.findAll(portfolioId);
        System.out.println(analyticsService.generateReport(portfolio, market, history));
    }

    private void exportHoldings() {
        Path destination = Path.of("data", "holdings_export.csv");
        try {
            csvExporter.exportHoldings(portfolio, market, destination);
            System.out.println("Holdings exported to " + destination);
        } catch (IOException e) {
            System.out.println("Export failed: " + e.getMessage());
        }
    }

    private void exportTransactions() {
        Path destination = Path.of("data", "transactions_export.csv");
        try {
            csvExporter.exportTransactions(transactionDao.findAll(portfolioId), destination);
            System.out.println("Transactions exported to " + destination);
        } catch (IOException e) {
            System.out.println("Export failed: " + e.getMessage());
        }
    }
}
