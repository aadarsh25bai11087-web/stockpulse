# Use Case Diagram

```mermaid
flowchart LR
    Investor((Investor))

    subgraph System["StockPulse"]
        UC1(View live market prices)
        UC2(View portfolio)
        UC3(Buy an asset)
        UC4(Sell an asset)
        UC5(View transaction history)
        UC6(View analytics report)
        UC7(Export holdings to CSV)
        UC8(Export transactions to CSV)
    end

    Investor --> UC1
    Investor --> UC2
    Investor --> UC3
    Investor --> UC4
    Investor --> UC5
    Investor --> UC6
    Investor --> UC7
    Investor --> UC8

    UC3 -.includes.-> UC1
    UC4 -.includes.-> UC1
    UC6 -.includes.-> UC2
    UC6 -.includes.-> UC5
```

- **Buy an asset** and **Sell an asset** both include *View live market prices* because every order is priced against the current, live value of the asset at the moment it executes.
- **View analytics report** includes both *View portfolio* and *View transaction history* because the report is derived from the current holdings plus the full trade log.
