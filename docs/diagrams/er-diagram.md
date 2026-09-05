# ER Diagram

Matches `src/main/resources/schema.sql` exactly.

```mermaid
erDiagram
    ASSETS {
        varchar symbol PK
        varchar name
        varchar asset_type
        decimal last_price
        varchar sector
        decimal extra_ratio
    }

    PORTFOLIO {
        int id PK
        varchar owner_name
        decimal cash_balance
    }

    HOLDINGS {
        int portfolio_id PK, FK
        varchar symbol PK, FK
        int quantity
        decimal average_buy_price
    }

    TRANSACTIONS {
        bigint id PK
        int portfolio_id FK
        varchar symbol
        varchar order_type
        int quantity
        decimal price
        timestamp executed_at
    }

    PORTFOLIO ||--o{ HOLDINGS : "owns"
    ASSETS ||--o{ HOLDINGS : "held as"
    PORTFOLIO ||--o{ TRANSACTIONS : "executed by"
```

**Notes on the schema:**
- `HOLDINGS` uses a composite primary key `(portfolio_id, symbol)` since exactly one holding row can exist per asset per portfolio.
- `sector` and `extra_ratio` are nullable on `ASSETS` because they only apply to `Stock` and `MutualFund` respectively (a `CryptoAsset` row leaves both null) — a deliberate denormalization to keep a single-table catalogue instead of one table per asset type, which was not worth the extra join complexity for three asset types.
- `TRANSACTIONS` is append-only: rows are never updated or deleted, so it doubles as a full audit trail.
