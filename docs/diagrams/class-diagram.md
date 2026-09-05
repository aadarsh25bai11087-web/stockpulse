# Class Diagram

Focused on the domain model and the services that operate on it (DAO plumbing and exact getters are omitted for readability — see the Javadoc-style comments in source for those).

```mermaid
classDiagram
    class Tradable {
        <<interface>>
        +getSymbol() String
        +getCurrentPrice() double
        +getType() AssetType
    }

    class Asset {
        <<abstract>>
        -symbol String
        -name String
        -currentPrice AtomicReference~Double~
        +getCurrentPrice() double
        +updatePrice(double) double
        +getVolatilityFactor()* double
    }

    class Stock {
        -sector String
        +getVolatilityFactor() double
    }
    class CryptoAsset {
        +getVolatilityFactor() double
    }
    class MutualFund {
        -expenseRatioPercent double
        +getVolatilityFactor() double
    }

    Tradable <|.. Asset
    Asset <|-- Stock
    Asset <|-- CryptoAsset
    Asset <|-- MutualFund

    class AssetType {
        <<enumeration>>
        STOCK
        CRYPTO
        MUTUAL_FUND
    }
    class OrderType {
        <<enumeration>>
        BUY
        SELL
    }

    class Holding {
        -symbol String
        -quantity int
        -averageBuyPrice double
        +applyBuy(int, double)
        +applySell(int)
        +unrealizedPnl(double) double
    }

    class Portfolio {
        -ownerName String
        -cashBalance double
        -holdings Map~String, Holding~
        +applyBuy(String, int, double)
        +applySell(String, int)
        +getNetWorth(Map) double
    }
    Portfolio "1" o-- "0..*" Holding

    class Transaction {
        -id long
        -symbol String
        -orderType OrderType
        -quantity int
        -price double
        -timestamp LocalDateTime
    }

    class TradingEngine {
        -portfolio Portfolio
        -market Map~String, Asset~
        -transactionDao TransactionDao
        +buy(String, int) Transaction
        +sell(String, int) Transaction
    }
    TradingEngine --> Portfolio
    TradingEngine ..> Asset : reads price from
    TradingEngine ..> Transaction : creates
    TradingEngine ..> InvalidSymbolException
    TradingEngine ..> InsufficientFundsException
    TradingEngine ..> InsufficientHoldingsException

    class MarketDataSimulator {
        -market Map~String, Asset~
        -tickIntervalMillis long
        -running AtomicBoolean
        +start()
        +stop()
        +run()
    }
    MarketDataSimulator ..> Asset : mutates price of
    MarketDataSimulator ..|> Runnable

    class AnalyticsService {
        +generateReport(Portfolio, Map, List) String
        +bestPerformer(Portfolio, Map) Optional~Holding~
        +worstPerformer(Portfolio, Map) Optional~Holding~
    }
    AnalyticsService --> Portfolio

    class InvalidSymbolException {
        <<exception>>
    }
    class InsufficientFundsException {
        <<exception>>
    }
    class InsufficientHoldingsException {
        <<exception>>
    }
```

**Key design decisions reflected here:**
- `Asset` is abstract and delegates `getVolatilityFactor()` to each subtype, so the market simulator can treat every asset polymorphically while still giving crypto meaningfully different behaviour from a mutual fund.
- `TradingEngine` depends only on the narrow domain types (`Portfolio`, the `Asset` map, `TransactionDao`) and never touches the console or persistence details beyond logging a transaction — it can be unit-tested (see `TradingEngineTest`) without any UI.
- The three custom exceptions are checked exceptions, forcing every call site to explicitly handle a rejected order rather than letting it fail silently.
