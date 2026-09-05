# StockPulse

A multi-threaded portfolio and market simulation system built in core Java for **CSE2006 – Programming in Java** (VIT Bhopal), submitted as the "Build Your Own Project" assignment.

StockPulse lets an investor manage a multi-asset portfolio (stocks, crypto, mutual funds) against a market whose prices move on their own in the background, live, while trades are placed — a genuine concurrent, thread-safe trading engine rather than a static CRUD app.

## Overview

The console app runs a background **market data simulator** thread that nudges every asset's price at a fixed interval, while the main thread accepts buy/sell orders through a **trading engine** that reads the shared, concurrently-changing price data safely. All portfolio state, the asset catalogue, and every executed trade are persisted to an embedded H2 database via JDBC, so the app resumes exactly where it left off on the next run.

See [`statement.md`](statement.md) for the full problem statement, scope, and target users, and [`docs/`](docs/) for the design diagrams and the full project report.

## Features

- **Live market simulation** — a dedicated background thread mutates every asset's price on an interval, within a volatility band specific to its asset type (crypto moves far more than a mutual fund).
- **Thread-safe trading engine** — buy/sell orders are executed atomically against the live, concurrently-updated price feed; each order is validated and either fully applied or rejected with no partial state.
- **Multi-asset OOP model** — an abstract `Asset` hierarchy (`Stock`, `CryptoAsset`, `MutualFund`) implementing a common `Tradable` interface.
- **Custom exception handling** — `InvalidSymbolException`, `InsufficientFundsException`, and `InsufficientHoldingsException` guard every trade, with dedicated multi-catch handling in the console layer.
- **JDBC persistence** — the asset catalogue, portfolio (cash + holdings), and full transaction history are stored in an embedded H2 database; nothing is lost between runs.
- **Analytics & reporting** — net worth, per-holding unrealized P&L, and best/worst performer, computed on demand.
- **File I/O** — transaction history and holdings can be exported to CSV, and every trade is appended to a plain-text audit log, both using standard Java I/O streams (`BufferedWriter`/`FileWriter`) independent of the database.

## Technologies / Tools Used

| Concern | Technology |
|---|---|
| Language / runtime | Java 17 |
| Build | Apache Maven |
| Persistence | JDBC, embedded H2 database (file-based, zero external setup) |
| Concurrency | `java.util.concurrent` (`AtomicReference`, `AtomicBoolean`), `synchronized` |
| Testing | JUnit 5 |
| Packaging | `maven-shade-plugin` (single runnable fat jar) |

## Architecture

```
Console UI  →  TradingEngine  →  Portfolio (in-memory)
     │              │                    │
     │              ▼                    ▼
     │        Market (Map<Symbol, Asset)  Holdings
     │              ▲
     │              │ mutates prices on a timer
     │        MarketDataSimulator (background thread)
     │
     ▼
AnalyticsService / CsvExporter / TradeLogger
     │
     ▼
JDBC (AssetDao / PortfolioDao / TransactionDao)  →  H2 database (data/stockpulse.mv.db)
```

Full UML (use case, class, sequence) and ER diagrams are in [`docs/diagrams/`](docs/diagrams/).

## Project Structure

```
.
├── pom.xml
├── README.md
├── statement.md
├── schema.sql (packaged from src/main/resources/schema.sql)
├── data/                       # embedded database + exports live here at runtime
├── docs/
│   ├── diagrams/                # UML + ER + architecture diagrams (Mermaid)
│   └── report/                  # full project report (PDF + source)
└── src/
    ├── main/java/com/stockpulse/
    │   ├── Main.java
    │   ├── model/                # Asset, Stock, CryptoAsset, MutualFund, Portfolio, Holding, Transaction, enums
    │   ├── exception/             # custom checked exceptions
    │   ├── db/                    # DatabaseManager (JDBC connection + schema bootstrap)
    │   ├── dao/                   # AssetDao, PortfolioDao, TransactionDao
    │   ├── market/                 # MarketDataSimulator (background thread)
    │   ├── service/                # TradingEngine, AnalyticsService
    │   ├── io/                     # CsvExporter, TradeLogger
    │   └── cli/                    # ConsoleUI
    └── test/java/com/stockpulse/   # JUnit 5 tests
```

## Steps to Install & Run

This is a pure command-line application — no GUI, no IDE, and no external database server are required. Everything below works in a plain terminal.

### 1. Prerequisites

You need **JDK 17 or newer** and **Apache Maven 3.6+**. Check what you already have:

```bash
java -version
mvn -version
```

If either command is missing or reports a version older than the above, install them:

| OS | Install |
|---|---|
| macOS | `brew install openjdk@17 maven` (requires [Homebrew](https://brew.sh)) |
| Ubuntu/Debian | `sudo apt update && sudo apt install openjdk-17-jdk maven` |
| Windows | Install [Eclipse Temurin JDK 17](https://adoptium.net/) and [Maven](https://maven.apache.org/download.cgi), then add both to your `PATH` |

No IDE, application server, or manual JDK/Maven configuration beyond having them on your `PATH` is needed — Maven resolves every dependency (including the embedded H2 database driver and JUnit) automatically from Maven Central on first build.

### 2. Clone the repository

```bash
git clone https://github.com/aadarsh25bai11087-web/stockpulse.git
cd stockpulse
```

### 3. Configuration

**None required.** The app uses an embedded, file-based H2 database that is created automatically on first run at `data/stockpulse.mv.db` — there is no connection string, credentials, port, or `.env` file to set up.

### 4. Build

```bash
mvn clean package
```

This compiles the project, runs the full test suite, and produces a single self-contained runnable jar at `target/stockpulse.jar` (all dependencies bundled in — nothing else needs to be on the classpath).

### 5. Run

```bash
java -jar target/stockpulse.jar
```

You'll see a numbered menu (reproduced in full under [Sample Session](#sample-session) below). On first run the app seeds a default catalogue of six assets (three stocks, two cryptocurrencies, one mutual fund) and a portfolio with ₹100,000 starting cash. On every subsequent run it loads your saved portfolio and last known prices back from `data/stockpulse.mv.db`, so progress persists across runs.

## Instructions for Testing

```bash
mvn test
```

This runs the full JUnit 5 suite (12 tests) covering:
- `PortfolioTest` — cost-basis averaging, position closing, net worth, and unrealized P&L math.
- `TradingEngineTest` — successful buy/sell execution, rejection paths for insufficient funds, insufficient holdings, and unknown symbols, and that every executed trade is persisted.
- `MarketDataSimulatorTest` — that a price tick never moves a price beyond its asset's volatility band, and that the background thread starts and stops cleanly.

Tests run against an isolated in-memory H2 database (a fresh one per test) and never touch the real `data/stockpulse.mv.db` file.

## Sample Session

```
1) View market   2) View portfolio   3) Buy   4) Sell
5) Transaction history   6) Analytics report
7) Export holdings CSV   8) Export transactions CSV   0) Exit
> 3
Symbol: INFY
Quantity: 10
Executed: #1  2026-09-05T23:41:49  BUY  INFY  qty=10  price=1488.27  total=14882.70
```

## Future Enhancements

- Swap the console front-end for a JavaFX/Swing GUI with a live-updating price ticker.
- Replace the simulated feed with a real market data API.
- Support multiple named portfolios / multiple users.

## License

Submitted as coursework for CSE2006 – Programming in Java, VIT Bhopal.
