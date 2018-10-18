package org.binas.domain.exception;

/** Exception used to signal that no Binas are currently available in a station. */
public class InvalidStationException extends Exception {
	private static final long serialVersionUID = 1L;

	public InvalidStationException() {
	}

	public InvalidStationException(String message) {
		super(message);
	}
}
