package com.googlecode.contraildb.core.utils;

import com.googlecode.contraildb.core.Identifier;

abstract public class ContrailAction extends ContrailTask<Void> {

	public ContrailAction(Identifier id, Operation operation) {
		super(id, operation);
	}
	public ContrailAction() {
		super(Identifier.create(), null);
	}
	
	protected abstract void action() throws Exception;
	
	final protected Void run() throws Exception {
		action();
		return null;
	}

	
}
