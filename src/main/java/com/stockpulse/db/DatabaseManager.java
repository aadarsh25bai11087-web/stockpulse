package com.stockpulse.db;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Owns the single JDBC connection to the embedded H2 database used for
 * persistence (assets, the portfolio, its holdings, and transaction
 * history). H2 was chosen over a networked database such as MySQL so the
 * project can be cloned and run with zero external setup - the whole
 * database lives in one file under {@code data/}.
 */
public final class DatabaseManager {

    private static final String DEFAULT_DB_URL = "jdbc:h2:./data/stockpulse;AUTO_SERVER=TRUE";
    private static final String DB_USER = "sa";
    private static final String DB_PASSWORD = "";

    private final Connection connection;

    /** Opens (creating if needed) the project's on-disk database under {@code data/}. */
    public DatabaseManager() {
        this(DEFAULT_DB_URL);
    }

    /** Opens a database at an arbitrary JDBC URL - used by tests to run against an isolated in-memory instance. */
    public DatabaseManager(String jdbcUrl) {
        try {
            this.connection = DriverManager.getConnection(jdbcUrl, DB_USER, DB_PASSWORD);
            runSchema();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not open the StockPulse database", e);
        }
    }

    public Connection getConnection() {
        return connection;
    }

    private void runSchema() throws SQLException {
        String schemaSql = readSchemaResource();
        try (Statement statement = connection.createStatement()) {
            for (String rawStatement : schemaSql.split(";")) {
                String trimmed = rawStatement.trim();
                if (!trimmed.isEmpty()) {
                    statement.execute(trimmed);
                }
            }
        }
    }

    private String readSchemaResource() {
        try (InputStream in = DatabaseManager.class.getResourceAsStream("/schema.sql")) {
            if (in == null) {
                throw new IllegalStateException("schema.sql not found on the classpath");
            }
            StringBuilder builder = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.trim().startsWith("--")) {
                        builder.append(line).append('\n');
                    }
                }
            }
            return builder.toString();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read schema.sql", e);
        }
    }

    public void close() {
        try {
            connection.close();
        } catch (SQLException e) {
            System.err.println("Warning: failed to close the database connection cleanly: " + e.getMessage());
        }
    }
}
