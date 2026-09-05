package com.stockpulse.model;

/**
 * Contract for anything that can be quoted and traded on the simulated
 * market. Implemented by {@link Asset} and kept separate from it so that
 * services can depend on the narrow trading contract rather than the full
 * asset hierarchy.
 */
public interface Tradable {
    String getSymbol();

    double getCurrentPrice();

    AssetType getType();
}
