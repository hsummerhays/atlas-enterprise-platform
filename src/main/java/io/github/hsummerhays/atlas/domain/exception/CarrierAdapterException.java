package io.github.hsummerhays.atlas.domain.exception;

public class CarrierAdapterException extends RuntimeException {
    public CarrierAdapterException(String message) {
        super(message);
    }

    public CarrierAdapterException(String message, Throwable cause) {
        super(message, cause);
    }
}
