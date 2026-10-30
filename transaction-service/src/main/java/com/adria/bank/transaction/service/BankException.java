package com.adria.bank.transaction.service;

public class BankException extends RuntimeException {
    public enum Reason { NOT_FOUND, REJECTED, UNAVAILABLE }

    private final Reason reason;

    public BankException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
