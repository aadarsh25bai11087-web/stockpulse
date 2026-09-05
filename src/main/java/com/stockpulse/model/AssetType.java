package com.stockpulse.model;

/**
 * The category of a tradable asset. Each type is associated with a
 * default volatility band used by the market simulator.
 */
public enum AssetType {
    STOCK,
    CRYPTO,
    MUTUAL_FUND
}
