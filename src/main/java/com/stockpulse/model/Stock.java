package com.stockpulse.model;

/** An equity share. Moderate volatility, tagged with a market sector. */
public class Stock extends Asset {

    private static final double VOLATILITY = 0.02;

    private final String sector;

    public Stock(String symbol, String name, double initialPrice, String sector) {
        super(symbol, name, initialPrice);
        this.sector = sector;
    }

    public String getSector() {
        return sector;
    }

    @Override
    public double getVolatilityFactor() {
        return VOLATILITY;
    }

    @Override
    public AssetType getType() {
        return AssetType.STOCK;
    }
}
