package com.gdtahara.gdtaharabackend.exception;

public class PlanParseException extends RuntimeException {

    public PlanParseException(String message) {
        super(message);
    }

    public PlanParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
