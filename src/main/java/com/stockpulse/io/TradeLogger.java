package com.stockpulse.io;

import com.stockpulse.model.Transaction;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;

/**
 * Appends a line per executed trade to a plain-text log file. Kept
 * separate from {@link CsvExporter} because this is an append-as-you-go
 * audit trail rather than an on-demand snapshot export.
 */
public class TradeLogger {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Path logFile;

    public TradeLogger(Path logFile) {
        this.logFile = logFile;
    }

    public void log(Transaction transaction) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(logFile.toFile(), true))) {
            writer.printf("[%s] %s %d x %s @ %.2f (total %.2f)%n",
                    transaction.getTimestamp().format(TIMESTAMP_FORMAT),
                    transaction.getOrderType(),
                    transaction.getQuantity(),
                    transaction.getSymbol(),
                    transaction.getPrice(),
                    transaction.getTotalValue());
        } catch (IOException e) {
            System.err.println("Warning: could not write to trade log: " + e.getMessage());
        }
    }
}
