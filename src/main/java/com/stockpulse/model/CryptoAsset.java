package com.stockpulse.model;

/** A cryptocurrency. Deliberately the most volatile asset type modelled. */
public class CryptoAsset extends Asset {

    private static final double VOLATILITY = 0.08;

    public CryptoAsset(String symbol, String name, double initialPrice) {
        super(symbol, name, initialPrice);
    }

    @Override
    public double getVolatilityFactor() {
        return VOLATILITY;
    }

    @Override
    public AssetType getType() {
        return AssetType.CRYPTO;
    }
}
