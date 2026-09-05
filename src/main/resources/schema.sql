-- StockPulse schema (H2). Executed once at startup; every statement is
-- idempotent so re-running it against an existing database is harmless.

CREATE TABLE IF NOT EXISTS assets (
    symbol      VARCHAR(10)   PRIMARY KEY,
    name        VARCHAR(100)  NOT NULL,
    asset_type  VARCHAR(20)   NOT NULL,
    last_price  DECIMAL(18,4) NOT NULL,
    sector      VARCHAR(50),
    extra_ratio DECIMAL(6,3)
);

CREATE TABLE IF NOT EXISTS portfolio (
    id            INT           PRIMARY KEY,
    owner_name    VARCHAR(100)  NOT NULL,
    cash_balance  DECIMAL(18,4) NOT NULL
);

CREATE TABLE IF NOT EXISTS holdings (
    portfolio_id      INT           NOT NULL REFERENCES portfolio(id),
    symbol            VARCHAR(10)   NOT NULL REFERENCES assets(symbol),
    quantity          INT           NOT NULL,
    average_buy_price DECIMAL(18,4) NOT NULL,
    PRIMARY KEY (portfolio_id, symbol)
);

CREATE TABLE IF NOT EXISTS transactions (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    portfolio_id INT           NOT NULL REFERENCES portfolio(id),
    symbol       VARCHAR(10)   NOT NULL,
    order_type   VARCHAR(4)    NOT NULL,
    quantity     INT           NOT NULL,
    price        DECIMAL(18,4) NOT NULL,
    executed_at  TIMESTAMP     NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_transactions_portfolio ON transactions(portfolio_id);
