package com.stockpulse.io;

import com.stockpulse.model.Asset;
import com.stockpulse.model.Holding;
import com.stockpulse.model.Portfolio;
import com.stockpulse.model.Transaction;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Writes transaction history and the current holdings snapshot to CSV
 * files using a character-oriented {@link FileWriter}/{@link BufferedWriter}
 * stream, as required alongside the JDBC persistence layer.
 */
public class CsvExporter {

    public void exportTransactions(List<Transaction> transactions, Path destination) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(destination.toFile()))) {
            writer.write("id,timestamp,symbol,order_type,quantity,price,total");
            writer.newLine();
            for (Transaction transaction : transactions) {
                writer.write(String.join(",",
                        String.valueOf(transaction.getId()),
                        transaction.getTimestamp().toString(),
                        transaction.getSymbol(),
                        transaction.getOrderType().name(),
                        String.valueOf(transaction.getQuantity()),
                        String.valueOf(transaction.getPrice()),
                        String.valueOf(transaction.getTotalValue())));
                writer.newLine();
            }
        }
    }

    public void exportHoldings(Portfolio portfolio, Map<String, Asset> market, Path destination) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(destination.toFile()))) {
            writer.write("symbol,quantity,average_buy_price,current_price,market_value,unrealized_pnl");
            writer.newLine();
            for (Holding holding : portfolio.getHoldings()) {
                Asset asset = market.get(holding.getSymbol());
                double price = asset == null ? 0 : asset.getCurrentPrice();
                writer.write(String.join(",",
                        holding.getSymbol(),
                        String.valueOf(holding.getQuantity()),
                        String.valueOf(holding.getAverageBuyPrice()),
                        String.valueOf(price),
                        String.valueOf(holding.marketValue(price)),
                        String.valueOf(holding.unrealizedPnl(price))));
                writer.newLine();
            }
        }
    }
}
