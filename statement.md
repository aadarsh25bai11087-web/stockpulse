# Problem Statement

## The Problem

Retail and student investors who hold a small, mixed portfolio (a few stocks, some crypto, maybe a mutual fund) have no lightweight way to track their holdings, simulate trades, and see live profit/loss without either doing spreadsheet arithmetic by hand or signing up for a full brokerage platform they don't need. There is also no simple, hands-on way for a student to see *why* concurrent programming matters: most course exercises demonstrate threads with a toy counter, not a scenario where getting synchronization wrong has a real, visible consequence (a trade executing against a stale or torn price).

## What StockPulse Does

StockPulse is a console-based Java application that models a single investor's portfolio against a simulated market:

- A background thread continuously moves every asset's price at random, within a volatility band specific to its type (crypto moves the most, mutual funds the least).
- The investor can buy and sell assets at any time; every order is checked against the *live* price and the portfolio's current cash/holdings, atomically, so a trade can never partially apply.
- All state — the asset catalogue, the portfolio, and every executed transaction — is persisted through JDBC to an embedded database, so nothing is lost between runs.
- The investor can request an analytics report (net worth, per-holding P&L, best/worst performer) and export holdings or transaction history to CSV at any time.

## Scope

**In scope:**
- Single-portfolio, single-user simulation (no authentication/multi-tenancy).
- Three asset types: stocks, cryptocurrencies, mutual funds.
- A simulated price feed (not a connection to a real market data provider).
- A text-based console interface.

**Out of scope (see "Future Enhancements" in `README.md`):**
- A graphical user interface.
- Real market data integration.
- Multiple concurrent portfolios/users trading against the same market simultaneously.

## Target Users

- Students learning portfolio/investment concepts who want a safe, zero-risk simulation to experiment with.
- Anyone wanting a minimal, self-hosted way to track a small multi-asset portfolio without an external brokerage account.

## High-Level Features

1. **Live market simulation** via a dedicated background thread.
2. **Trading engine** for buy/sell orders with custom exception handling for invalid symbols, insufficient funds, and insufficient holdings.
3. **JDBC-backed persistence** of the asset catalogue, portfolio, and transaction history (embedded H2 database).
4. **Analytics and reporting**, including unrealized P&L and best/worst performer.
5. **CSV export and a plain-text trade audit log**, via standard Java I/O streams.
