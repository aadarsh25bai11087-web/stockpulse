package com.stockpulse.model;

import java.time.LocalDateTime;

/** An immutable record of one executed trade, persisted via {@code TransactionDao}. */
public class Transaction {

    private final long id;
    private final String symbol;
    private final OrderType orderType;
    private final int quantity;
    private final double price;
    private final LocalDateTime timestamp;

    public Transaction(long id, String symbol, OrderType orderType, int quantity, double price,
                        LocalDateTime timestamp) {
        this.id = id;
        this.symbol = symbol;
        this.orderType = orderType;
        this.quantity = quantity;
        this.price = price;
        this.timestamp = timestamp;
    }

    public long getId() {
        return id;
    }

    public String getSymbol() {
        return symbol;
    }

    public OrderType getOrderType() {
        return orderType;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getPrice() {
        return price;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public double getTotalValue() {
        return price * quantity;
    }

    @Override
    public String toString() {
        return String.format("#%-5d %-19s %-4s %-6s qty=%-6d price=%10.2f total=%12.2f",
                id, timestamp, orderType, symbol, quantity, price, getTotalValue());
    }
}
