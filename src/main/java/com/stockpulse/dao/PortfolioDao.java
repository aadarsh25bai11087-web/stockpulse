package com.stockpulse.dao;

import com.stockpulse.model.Holding;
import com.stockpulse.model.Portfolio;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Loads and saves the single {@link Portfolio} this project models
 * (portfolio id {@code 1}). A multi-user version would key everything by a
 * real portfolio id instead of the constant used here.
 */
public class PortfolioDao {

    public static final int DEFAULT_PORTFOLIO_ID = 1;

    private final Connection connection;

    public PortfolioDao(Connection connection) {
        this.connection = connection;
    }

    public Optional<Portfolio> load() {
        String sql = "SELECT owner_name, cash_balance FROM portfolio WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, DEFAULT_PORTFOLIO_ID);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                Portfolio portfolio = new Portfolio(
                        resultSet.getString("owner_name"),
                        resultSet.getBigDecimal("cash_balance").doubleValue());
                loadHoldingsInto(portfolio);
                return Optional.of(portfolio);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to load the portfolio", e);
        }
    }

    private void loadHoldingsInto(Portfolio portfolio) throws SQLException {
        String sql = "SELECT symbol, quantity, average_buy_price FROM holdings WHERE portfolio_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, DEFAULT_PORTFOLIO_ID);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    portfolio.restoreHolding(new Holding(
                            resultSet.getString("symbol"),
                            resultSet.getInt("quantity"),
                            resultSet.getBigDecimal("average_buy_price").doubleValue()));
                }
            }
        }
    }

    public Portfolio createNew(String ownerName, double startingCash) {
        String sql = "INSERT INTO portfolio (id, owner_name, cash_balance) VALUES (?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, DEFAULT_PORTFOLIO_ID);
            statement.setString(2, ownerName);
            statement.setBigDecimal(3, java.math.BigDecimal.valueOf(startingCash));
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to create the portfolio", e);
        }
        return new Portfolio(ownerName, startingCash);
    }

    /** Persists cash balance and fully replaces the holdings snapshot. */
    public void save(Portfolio portfolio) {
        try {
            connection.setAutoCommit(false);
            updateCashBalance(portfolio);
            replaceHoldings(portfolio);
            connection.commit();
        } catch (SQLException e) {
            rollbackQuietly();
            throw new IllegalStateException("Failed to save the portfolio", e);
        } finally {
            restoreAutoCommit();
        }
    }

    private void updateCashBalance(Portfolio portfolio) throws SQLException {
        String sql = "UPDATE portfolio SET cash_balance = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBigDecimal(1, java.math.BigDecimal.valueOf(portfolio.getCashBalance()));
            statement.setInt(2, DEFAULT_PORTFOLIO_ID);
            statement.executeUpdate();
        }
    }

    private void replaceHoldings(Portfolio portfolio) throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement("DELETE FROM holdings WHERE portfolio_id = ?")) {
            delete.setInt(1, DEFAULT_PORTFOLIO_ID);
            delete.executeUpdate();
        }
        String insertSql = "INSERT INTO holdings (portfolio_id, symbol, quantity, average_buy_price) VALUES (?, ?, ?, ?)";
        try (PreparedStatement insert = connection.prepareStatement(insertSql)) {
            for (Holding holding : portfolio.getHoldings()) {
                insert.setInt(1, DEFAULT_PORTFOLIO_ID);
                insert.setString(2, holding.getSymbol());
                insert.setInt(3, holding.getQuantity());
                insert.setBigDecimal(4, java.math.BigDecimal.valueOf(holding.getAverageBuyPrice()));
                insert.addBatch();
            }
            insert.executeBatch();
        }
    }

    private void rollbackQuietly() {
        try {
            connection.rollback();
        } catch (SQLException ignored) {
            // Best effort - the outer IllegalStateException already reports the real failure.
        }
    }

    private void restoreAutoCommit() {
        try {
            connection.setAutoCommit(true);
        } catch (SQLException ignored) {
            // Connection is likely being torn down anyway.
        }
    }
}
