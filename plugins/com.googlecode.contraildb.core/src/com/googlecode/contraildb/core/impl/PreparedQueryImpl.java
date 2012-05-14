package com.googlecode.contraildb.core.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.googlecode.contraildb.core.ContrailQuery;
import com.googlecode.contraildb.core.FetchOptions;
import com.googlecode.contraildb.core.IContrailSession;
import com.googlecode.contraildb.core.IPreparedQuery;
import com.googlecode.contraildb.core.IProcessor;
import com.googlecode.contraildb.core.Identifier;
import com.googlecode.contraildb.core.Item;
import com.googlecode.contraildb.core.utils.ContrailAction;
import com.googlecode.contraildb.core.utils.ContrailTaskTracker;
import com.googlecode.contraildb.core.utils.ConversionUtils;
import com.googlecode.contraildb.core.utils.Handler;
import com.googlecode.contraildb.core.utils.TaskUtils;



@SuppressWarnings({"unchecked","rawtypes"})
public class PreparedQueryImpl<T extends Item> 
implements IPreparedQuery<T> 
{
	ContrailServiceImpl _service;
	ContrailSessionImpl _session;
	ContrailQuery _query;

	PreparedQueryImpl(ContrailServiceImpl service, ContrailSessionImpl transaction, ContrailQuery query) {
		_service= service;
		_session= transaction;
		_query= query;
	}


	@Override
	public List<T> list() throws IOException {
		return list(FetchOptions.withPrefetchSize(0));
	}

	@Override
	public List<T> list(FetchOptions fetchOptions) throws IOException {
		final ContrailTaskTracker.Session tracker= new ContrailTaskTracker().beginSession();
		try {
			final boolean[] complete= new boolean[] { false };
			final Throwable[] error= new Throwable[] { null }; 
			final List<T> results= Collections.synchronizedList(new ArrayList<T>());
			final IProcessor processor= new IProcessor() {
				public boolean result(final Identifier identifier) {
					// loads objects concurrently
					tracker.submit(new ContrailAction() {
						protected void action() throws Exception {
							try {
								results.add((T) _session.fetch(identifier).get());
							}
							catch (Exception x) {
								if (error[0] == null)
									error[0]= x;
							}
						}
					});
					return error[0] == null; // if an error occurred then cancel query
				}
				public void complete(Throwable t) {
					if (t != null) 
						error[0]= t;
					
					// wait until all items have been fetched 
					new Handler(tracker.complete()) {
						public void onComplete() {
							synchronized (tracker) {
								complete[0]= true;
								tracker.notify();
							}
						}
					};
				}
			};
			_session.process(_query, processor);
			
			// wait for completion
			while(true) {
				synchronized (tracker) {
					if (complete[0])
						break;
					try {
						tracker.wait();
					}
					catch (InterruptedException x) {
					}
				}
			}
			
			if (error[0] != null)
				TaskUtils.throwSomething(error[0], IOException.class);
			
			return results;
		}
		finally {
			tracker.close();
		}
	}


	@Override
	public T item() throws IOException {
		List<T> list= list(FetchOptions.withPrefetchSize(1));
		if (list.isEmpty())
			return null;
		return list.get(0);
	}


	@Override
	public int count() throws IOException {
		return ConversionUtils.toList(identifiers()).size();
	}


	@Override
	public IContrailSession getSession() {
		return _session;
	}

	@Override
	public Iterable<Identifier> identifiers() throws IOException {
		final boolean[] complete= new boolean[] { false };
		final Throwable[] error= new Throwable[] { null }; 
		final ArrayList<Identifier> results= new ArrayList<Identifier>();
		IProcessor processor= new IProcessor() {
			synchronized public boolean result(final Identifier identifier) {
				results.add(identifier);
				return true;
			}
			synchronized public void complete(Throwable t) {
				error[0]= t;
				complete[0]= true;
				this.notify();
			}
		};
		_session.process(_query, processor);

		// wait for completion
		while(true) {
			synchronized (processor) {
				if (complete[0])
					break;
				try {
					processor.wait();
				}
				catch (InterruptedException x) {
				}
			}
		}

		if (error[0] != null)
			TaskUtils.throwSomething(error[0], IOException.class);

		return results;
	}

	@Override
	public void process(IProcessor processor) throws IOException {
		_session.process(_query, processor);
	}
	
}