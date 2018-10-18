package org.binas.domain.exception;

/** Exception used to signal that no slots are are currently available in a station. */
public class EmailExistsException extends Exception {
	private static final long serialVersionUID = 1L;

	public EmailExistsException() {
	}

	public EmailExistsException(String message) {
		super(message);
	}
}
