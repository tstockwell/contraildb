package com.googlecode.contraildb.core.storage;

public class StorageException extends RuntimeException {

	public StorageException(String message, Throwable cause) {
		super(message, cause);
	}

	public StorageException(String message) {
		super(message);
	}

	public StorageException(Throwable cause) {
		super(cause);
	}

	private static final long serialVersionUID = 1L;

}
