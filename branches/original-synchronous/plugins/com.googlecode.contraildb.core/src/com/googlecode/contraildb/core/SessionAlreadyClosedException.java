package com.googlecode.contraildb.core;

public class SessionAlreadyClosedException extends ContrailException {
	private static final long serialVersionUID = 1L;
	
	public SessionAlreadyClosedException() {
		super("Session is already closed");
	}
	
}
