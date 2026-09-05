# System Architecture

StockPulse is a layered console application. The console/service layers operate on **in-memory** portfolio and market state; the DAO/JDBC layer is only touched on startup (load), on each trade (append a transaction), and on shutdown (save a snapshot) — the frequent, per-second price updates from the simulator thread never hit the database.

```mermaid
flowchart TB
    subgraph UI["Presentation"]
        CLI[ConsoleUI]
    end

    subgraph SVC["Service Layer"]
        TE[TradingEngine]
        AS[AnalyticsService]
    end

    subgraph DOMAIN["In-Memory Domain State"]
        PF[(Portfolio<br/>cash + holdings)]
        MKT[(Market<br/>Map&lt;Symbol, Asset&gt;)]
    end

    subgraph CONC["Background Thread"]
        MDS[MarketDataSimulator]
    end

    subgraph IO["File I/O"]
        CSV[CsvExporter]
        LOG[TradeLogger]
    end

    subgraph PERSIST["Persistence Layer (JDBC)"]
        ADao[AssetDao]
        PDao[PortfolioDao]
        TDao[TransactionDao]
    end

    DB[(H2 Database<br/>data/stockpulse.mv.db)]

    CLI --> TE
    CLI --> AS
    CLI --> CSV
    CLI --> LOG

    TE --> PF
    TE --> MKT
    TE --> TDao

    AS --> PF
    AS --> MKT

    MDS -->|mutates prices every N seconds| MKT

    ADao --> DB
    PDao --> DB
    TDao --> DB

    ADao -.load/seed on startup, save on exit.-> MKT
    PDao -.load on startup, save on exit.-> PF
```

**Why the database is on the edges, not the hot path:** the simulator can tick every few seconds indefinitely; persisting every single price change would make the database the bottleneck and serialize what is otherwise a lock-free price update (see `Asset`'s use of `AtomicReference`). Instead, prices are loaded once at startup and snapshotted once at shutdown, while trades — which are comparatively rare and must never be lost — are written to the `transactions` table immediately as they happen.
