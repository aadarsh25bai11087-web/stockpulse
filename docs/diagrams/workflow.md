# Process Flow / Application Workflow

End-to-end lifecycle of a single run of the app, from launch to shutdown.

```mermaid
flowchart TD
    Start([Start]) --> OpenDb[Open / create H2 database\nrun schema.sql]
    OpenDb --> CheckSeed{Asset catalogue\nempty?}
    CheckSeed -- yes --> Seed[Seed default assets]
    CheckSeed -- no --> LoadMarket
    Seed --> LoadMarket[Load asset catalogue into memory]
    LoadMarket --> LoadPortfolio{Saved portfolio\nexists?}
    LoadPortfolio -- yes --> RestorePortfolio[Restore cash + holdings from DB]
    LoadPortfolio -- no --> NewPortfolio[Create new portfolio\nwith starting cash]
    RestorePortfolio --> StartSim
    NewPortfolio --> StartSim[Start MarketDataSimulator\non a background thread]
    StartSim --> Menu[[Show console menu]]

    Menu --> Choice{User choice}
    Choice -- View market/portfolio --> Display[Print current state]
    Choice -- Buy/Sell --> Trade[TradingEngine executes order]
    Choice -- History/Report --> Report[Query transactions,\ngenerate report]
    Choice -- Export --> Export[Write CSV via\nCsvExporter]
    Choice -- Exit --> Shutdown

    Trade --> TradeOk{Valid?}
    TradeOk -- yes --> Log[Log transaction to DB + trade log]
    TradeOk -- no --> Reject[Print rejection reason]
    Log --> Menu
    Reject --> Menu
    Display --> Menu
    Report --> Menu
    Export --> Menu

    Shutdown[Stop simulator thread] --> SavePortfolio[Persist portfolio + holdings]
    SavePortfolio --> SavePrices[Persist closing prices]
    SavePrices --> CloseDb[Close database connection]
    CloseDb --> End([End])
```

The background `MarketDataSimulator` thread runs concurrently with the entire menu loop from "Start MarketDataSimulator" until "Stop simulator thread" — it is not shown re-entering the diagram at every step because it operates independently of user input, which is the point of running it on its own thread.
