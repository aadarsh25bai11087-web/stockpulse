package com.stockpulse.dao;

import com.stockpulse.model.OrderType;
import com.stockpulse.model.Transaction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Append-only log of executed trades for one portfolio. */
public class TransactionDao {

    private final Connection connection;

    public TransactionDao(Connection connection) {
        this.connection = connection;
    }

    public Transaction insert(int portfolioId, String symbol, OrderType orderType, int quantity, double price,
                               LocalDateTime executedAt) {
        String sql = "INSERT INTO transactions (portfolio_id, symbol, order_type, quantity, price, executed_at) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, portfolioId);
            statement.setString(2, symbol);
            statement.setString(3, orderType.name());
            statement.setInt(4, quantity);
            statement.setBigDecimal(5, java.math.BigDecimal.valueOf(price));
            statement.setTimestamp(6, Timestamp.valueOf(executedAt));
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                long id = keys.next() ? keys.getLong(1) : -1;
                return new Transaction(id, symbol, orderType, quantity, price, executedAt);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to record transaction", e);
        }
    }

    public List<Transaction> findAll(int portfolioId) {
        List<Transaction> transactions = new ArrayList<>();
        String sql = "SELECT id, symbol, order_type, quantity, price, executed_at FROM transactions "
                + "WHERE portfolio_id = ? ORDER BY executed_at ASC";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, portfolioId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    transactions.add(new Transaction(
                            resultSet.getLong("id"),
                            resultSet.getString("symbol"),
                            OrderType.valueOf(resultSet.getString("order_type")),
                            resultSet.getInt("quantity"),
                            resultSet.getBigDecimal("price").doubleValue(),
                            resultSet.getTimestamp("executed_at").toLocalDateTime()));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to load transaction history", e);
        }
        return transactions;
    }
}
