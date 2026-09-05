package com.stockpulse.exception;

/** Thrown when an order references a ticker symbol the market does not know about. */
public class InvalidSymbolException extends Exception {

    public InvalidSymbolException(String symbol) {
        super("No such asset on the market: " + symbol);
    }
}
