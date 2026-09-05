package com.stockpulse.exception;

/** Thrown when a sell order asks for more units than the portfolio currently holds. */
public class InsufficientHoldingsException extends Exception {

    public InsufficientHoldingsException(String symbol, int requested, int owned) {
        super(String.format("Cannot sell %d units of %s, only %d are held", requested, symbol, owned));
    }
}
