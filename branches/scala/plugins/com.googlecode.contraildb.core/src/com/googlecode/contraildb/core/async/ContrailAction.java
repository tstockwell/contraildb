package com.googlecode.contraildb.core.async;

import kilim.Pausable;

import com.googlecode.contraildb.core.Identifier;

abstract public class ContrailAction extends ContrailTask<Void> {

	public ContrailAction(Identifier id, Operation operation) {
		super(id, operation,null);
	}
	public ContrailAction() {
		super(Identifier.create(), null, null);
	}
	public ContrailAction(String name) {
		super(Identifier.create(), null, name);
	}
	
	protected abstract void action() throws Pausable, Exception;
	
	final protected Void run() throws Pausable, Exception {
		action();
		return null;
	}

	
}
