package com.googlecode.contraildb.core.utils;

import java.util.HashMap;
import java.util.HashSet;

import com.googlecode.contraildb.core.Identifier;


/**
 *	This singleton implements the 'Completion Dispatcher' role in the
 *	'Proactor' pattern.
 *	@see http://www.cse.wustl.edu/~schmidt/PDF/proactor.pdf
 *  
 *	This implementation is a little different than the orthodox implementation 
 *	in that it is designed to decouple asynchronous tasks from their completion 
 *	handlers.  That is, instead of tasks calling the Completion Dispatcher and 
 *	passing the dispatcher a reference to the completion handler to call, in 
 *	this implementation handlers register themselves with the using a 'signal' 
 *	object.  When a task completes it calls the dispatcher and passes the signal, 
 *	the dispatcher then calls all handlers registered under that signal.  
 */
public class Signals {
	
	static private HashMap<Identifier, HashSet<SignalHandler>> __handlers= 
		new HashMap<Identifier, HashSet<SignalHandler>>();
	
	@SuppressWarnings("unchecked")
	final public synchronized static void signal(final Identifier path) {
		HashSet<SignalHandler> set= __handlers.get(path);
		if (set != null) {
			final HashSet<SignalHandler> actions= (HashSet<SignalHandler>) set.clone();
			new ContrailAction() {
				protected void run() throws Exception {
					for (final SignalHandler action:actions) {
						try {
							action.signal(path);
						}
						catch (Throwable t) {
							Logging.warning("Error throw from signal handler", t);
						}
					}
				}
			}.submit();
		}
	}
	final public synchronized static void register(Identifier path, SignalHandler handler) {
		HashSet<SignalHandler> actions= __handlers.get(path);
		if (actions == null) {
			actions= new HashSet<SignalHandler>();
			__handlers.put(path, actions);
		}
		actions.add(handler);
	}
	final public synchronized static void unregister(Identifier path, SignalHandler handler) {
		HashSet<SignalHandler> actions= __handlers.get(path);
		if (actions != null) {
			actions.remove(handler);
			if (actions.isEmpty())
				__handlers.remove(path);
		}
	}
	
}
