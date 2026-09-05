# Sequence Diagram — Placing a Buy Order

Shows the full path of a buy order, including the rejection branch, and highlights where the trading engine reads a price that the background simulator thread could change at any moment.

```mermaid
sequenceDiagram
    actor Investor
    participant UI as ConsoleUI
    participant TE as TradingEngine
    participant MKT as Market (Map<Symbol,Asset>)
    participant PF as Portfolio
    participant TDao as TransactionDao
    participant DB as H2 Database
    participant MDS as MarketDataSimulator (bg thread)

    par Concurrently, in the background
        loop every tickIntervalMillis
            MDS ->> MKT: updatePrice(symbol, newPrice)
        end
    end

    Investor ->> UI: choose "Buy", enter symbol + quantity
    UI ->> TE: buy(symbol, quantity)
    activate TE
    Note over TE: synchronized(this) - the whole\ncheck-then-act sequence below\nis one atomic step
    TE ->> MKT: get(symbol)
    alt symbol not found
        TE -->> UI: throw InvalidSymbolException
        UI -->> Investor: "Order rejected: no such asset"
    else symbol found
        MKT -->> TE: asset (current price)
        TE ->> PF: getCashBalance()
        PF -->> TE: cashBalance
        alt price * quantity > cashBalance
            TE -->> UI: throw InsufficientFundsException
            UI -->> Investor: "Order rejected: insufficient funds"
        else funds sufficient
            TE ->> PF: debitCash(cost)
            TE ->> PF: applyBuy(symbol, quantity, price)
            TE ->> TDao: insert(portfolioId, symbol, BUY, quantity, price, now)
            TDao ->> DB: INSERT INTO transactions ...
            DB -->> TDao: generated id
            TDao -->> TE: Transaction
            TE -->> UI: Transaction
            UI -->> Investor: "Executed: #id BUY ..."
        end
    end
    deactivate TE
```

The `par` block at the top is the reason `Asset.currentPrice` is an `AtomicReference` rather than a plain field: the simulator thread and the trading engine genuinely execute at the same time, and the sequence diagram makes explicit that `TE ->> MKT: get(symbol)` can race with `MDS ->> MKT: updatePrice(...)` on any given order — the engine must always see one fully-formed price, never a torn or half-written one, which `AtomicReference` guarantees without a lock.
