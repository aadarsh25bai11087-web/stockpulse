package com.stockpulse.dao;

import com.stockpulse.model.Asset;
import com.stockpulse.model.AssetType;
import com.stockpulse.model.CryptoAsset;
import com.stockpulse.model.MutualFund;
import com.stockpulse.model.Stock;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Persists the static asset catalogue (symbol, name, type, sector/expense
 * ratio) and the last known price, so the market picks up where it left
 * off across restarts instead of always reseeding from scratch.
 */
public class AssetDao {

    private final Connection connection;

    public AssetDao(Connection connection) {
        this.connection = connection;
    }

    public boolean isEmpty() {
        String sql = "SELECT COUNT(*) FROM assets";
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            return resultSet.getInt(1) == 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to check assets table", e);
        }
    }

    public void seed(List<Asset> assets) {
        String sql = "MERGE INTO assets (symbol, name, asset_type, last_price, sector, extra_ratio) "
                + "KEY (symbol) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (Asset asset : assets) {
                bindAsset(statement, asset);
                statement.addBatch();
            }
            statement.executeBatch();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to seed assets", e);
        }
    }

    /** Snapshots current in-memory prices to disk (called on graceful shutdown). */
    public void savePrices(Map<String, Asset> market) {
        String sql = "UPDATE assets SET last_price = ? WHERE symbol = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (Asset asset : market.values()) {
                statement.setBigDecimal(1, java.math.BigDecimal.valueOf(asset.getCurrentPrice()));
                statement.setString(2, asset.getSymbol());
                statement.addBatch();
            }
            statement.executeBatch();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to persist closing prices", e);
        }
    }

    public Map<String, Asset> loadAll() {
        Map<String, Asset> market = new LinkedHashMap<>();
        String sql = "SELECT symbol, name, asset_type, last_price, sector, extra_ratio FROM assets ORDER BY symbol";
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                Asset asset = toAsset(resultSet);
                market.put(asset.getSymbol(), asset);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to load the asset catalogue", e);
        }
        return market;
    }

    private void bindAsset(PreparedStatement statement, Asset asset) throws SQLException {
        statement.setString(1, asset.getSymbol());
        statement.setString(2, asset.getName());
        statement.setString(3, asset.getType().name());
        statement.setBigDecimal(4, java.math.BigDecimal.valueOf(asset.getCurrentPrice()));
        if (asset instanceof Stock stock) {
            statement.setString(5, stock.getSector());
            statement.setNull(6, java.sql.Types.DECIMAL);
        } else if (asset instanceof MutualFund fund) {
            statement.setNull(5, java.sql.Types.VARCHAR);
            statement.setBigDecimal(6, java.math.BigDecimal.valueOf(fund.getExpenseRatioPercent()));
        } else {
            statement.setNull(5, java.sql.Types.VARCHAR);
            statement.setNull(6, java.sql.Types.DECIMAL);
        }
    }

    private Asset toAsset(ResultSet resultSet) throws SQLException {
        String symbol = resultSet.getString("symbol");
        String name = resultSet.getString("name");
        AssetType type = AssetType.valueOf(resultSet.getString("asset_type"));
        double price = resultSet.getBigDecimal("last_price").doubleValue();

        return switch (type) {
            case STOCK -> new Stock(symbol, name, price, resultSet.getString("sector"));
            case CRYPTO -> new CryptoAsset(symbol, name, price);
            case MUTUAL_FUND -> {
                java.math.BigDecimal ratio = resultSet.getBigDecimal("extra_ratio");
                yield new MutualFund(symbol, name, price, ratio == null ? 0.0 : ratio.doubleValue());
            }
        };
    }
}
