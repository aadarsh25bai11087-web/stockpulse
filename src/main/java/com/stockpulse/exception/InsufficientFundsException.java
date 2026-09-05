package com.stockpulse.exception;

/** Thrown when a buy order costs more than the portfolio's available cash. */
public class InsufficientFundsException extends Exception {

    public InsufficientFundsException(double required, double available) {
        super(String.format("Order requires %.2f but only %.2f is available", required, available));
    }
}
