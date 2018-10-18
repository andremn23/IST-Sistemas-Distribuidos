package org.binas.station.domain.exception;

/** Exception used to signal that no Binas are currently available in a station. */
public class NoBalanceAvailException extends Exception {
    private static final long serialVersionUID = 1L;

    public NoBalanceAvailException() {
    }

    public NoBalanceAvailException(String message) {
        super(message);
    }
}