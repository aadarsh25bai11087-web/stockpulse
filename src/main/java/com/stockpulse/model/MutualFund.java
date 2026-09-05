package com.stockpulse.model;

/** A mutual fund unit. Tracks an expense ratio and moves the least of any asset type. */
public class MutualFund extends Asset {

    private static final double VOLATILITY = 0.005;

    private final double expenseRatioPercent;

    public MutualFund(String symbol, String name, double initialPrice, double expenseRatioPercent) {
        super(symbol, name, initialPrice);
        this.expenseRatioPercent = expenseRatioPercent;
    }

    public double getExpenseRatioPercent() {
        return expenseRatioPercent;
    }

    @Override
    public double getVolatilityFactor() {
        return VOLATILITY;
    }

    @Override
    public AssetType getType() {
        return AssetType.MUTUAL_FUND;
    }
}
