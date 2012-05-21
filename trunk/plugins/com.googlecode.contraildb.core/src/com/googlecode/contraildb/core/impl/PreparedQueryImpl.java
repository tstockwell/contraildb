package com.googlecode.contraildb.core.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.googlecode.contraildb.core.ContrailQuery;
import com.googlecode.contraildb.core.FetchOptions;
import com.googlecode.contraildb.core.IContrailSession;
import com.googlecode.contraildb.core.IPreparedQuery;
import com.googlecode.contraildb.core.IResult;
import com.googlecode.contraildb.core.Identifier;
import com.googlecode.contraildb.core.Item;
import com.googlecode.contraildb.core.utils.ConversionUtils;
import com.googlecode.contraildb.core.utils.Handler;
import com.googlecode.contraildb.core.utils.IAsyncerator;
import com.googlecode.contraildb.core.utils.InvocationHandler;
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
	public IResult<List<T>> list() {

		final List<T> results= Collections.synchronizedList(new ArrayList<T>());
		final ArrayList<IResult> tasks= new ArrayList<IResult>();
		tasks.add(new InvocationHandler<IAsyncerator<Identifier>>(_session.iterate(_query)) {
			protected IResult onSuccess(final IAsyncerator<Identifier> iterator) {
				class DoNext extends Handler {
					DoNext() {
						super(iterator.hasNext());
						tasks.add(this);
					}
					protected IResult onSuccess() {
						if (!(Boolean)incoming().getResult())
							return TaskUtils.DONE;
						return new InvocationHandler<Item>(_session.fetch(iterator.next())) {
							protected IResult onSuccess(Item item) {
								results.add((T)item);
								return new DoNext();
							}
						}; 
					}
				}
				return new DoNext();
			}
		});
		
		return new Handler(tasks) {
			protected IResult onSuccess() {
				return asResult(results);
			}
		};
	}


	@Override
	public IResult<T> item() {
		return new InvocationHandler<IAsyncerator<T>>(iterate(FetchOptions.withPrefetchSize(1))) {
			protected IResult onSuccess(final IAsyncerator<T> iterator) {
				return new InvocationHandler<Boolean>(iterator.hasNext()) {
					protected IResult onSuccess(Boolean hasNext) {
						if (!hasNext)
							return TaskUtils.NULL;
						return iterator.next();
					}
				};
			}
		};
	}


	@Override
	public IResult<Integer> count() {
		return new InvocationHandler<Iterable<Identifier>>(identifiers()) {
			protected IResult onSuccess(Iterable<Identifier> identifiers) {
				return asResult(ConversionUtils.toList(identifiers).size());
			}
		};
	}


	@Override
	public IContrailSession getSession() {
		return _session;
	}

	@Override
	public IResult<Iterable<Identifier>> identifiers() {
		

		final List<T> results= Collections.synchronizedList(new ArrayList<T>());
		final ArrayList<IResult> tasks= new ArrayList<IResult>();
		tasks.add(new InvocationHandler<IAsyncerator<Identifier>>(_session.iterate(_query)) {
			protected IResult onSuccess(final IAsyncerator<Identifier> iterator) {
				class DoNext extends Handler {
					DoNext() {
						super(iterator.hasNext());
						tasks.add(this);
					}
					protected IResult onSuccess() {
						if (!(Boolean)incoming().getResult())
							return TaskUtils.DONE;
						return new InvocationHandler<Item>(_session.fetch(iterator.next())) {
							protected IResult onSuccess(Item item) {
								results.add((T)item);
								return new DoNext();
							}
						}; 
					}
				}
				return new DoNext();
			}
		});
		
		return new Handler(tasks) {
			protected IResult onSuccess() {
				return asResult(results);
			}
		};		
	}


	@Override
	public IResult<IAsyncerator<T>> iterate(FetchOptions fetchOptions) {
		return new InvocationHandler<IAsyncerator<Identifier>>(identifiers(fetchOptions)) {
			protected IResult onSuccess(final IAsyncerator<Identifier> identifiers) {
				IAsyncerator<T> items= new IAsyncerator<T>() {
					public IResult<Boolean> hasNext() {
						return identifiers.hasNext();
					}
					public IResult<T> next() {
						return _session.fetch(identifiers.next());
					}
					public IResult<Void> remove() {
						throw new UnsupportedOperationException();
					}
				};
				return asResult(items);
			}
		};
	}


	@Override
	public IResult<IAsyncerator<Identifier>> identifiers(FetchOptions fetchOptions) {
		return _session.iterate(_query);
	}
	
}