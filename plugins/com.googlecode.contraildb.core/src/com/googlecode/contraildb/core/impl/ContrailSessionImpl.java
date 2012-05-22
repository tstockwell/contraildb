/*******************************************************************************
 * Copyright (c) 2009 Ted Stockwell
 * 
 * This file is part of the Contrail Database System.
 * 
 * Contrail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License Version 3
 * as published by the Free Software Foundation.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.googlecode.contraildb.core.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.googlecode.contraildb.core.ContrailException;
import com.googlecode.contraildb.core.ContrailQuery;
import com.googlecode.contraildb.core.IContrailService.Mode;
import com.googlecode.contraildb.core.IContrailSession;
import com.googlecode.contraildb.core.IPreparedQuery;
import com.googlecode.contraildb.core.IResult;
import com.googlecode.contraildb.core.Identifier;
import com.googlecode.contraildb.core.Item;
import com.googlecode.contraildb.core.SessionAlreadyClosedException;
import com.googlecode.contraildb.core.async.Handler;
import com.googlecode.contraildb.core.async.IAsyncerator;
import com.googlecode.contraildb.core.async.ResultHandler;
import com.googlecode.contraildb.core.async.TaskUtils;
import com.googlecode.contraildb.core.storage.IEntity;
import com.googlecode.contraildb.core.storage.StorageSession;
import com.googlecode.contraildb.core.storage.StorageUtils;
import com.googlecode.contraildb.core.storage.provider.IStorageProvider;



/**
 * Wraps a StorageSession and adds indexing and search functionality. 
 * 
 * @author Ted Stockwell
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class ContrailSessionImpl
implements IContrailSession 
{
	
	private IndexSearcher _searcher;
	private StorageSession _storageSession;
	private ContrailServiceImpl _service;
	
	public static IResult<ContrailSessionImpl> create(final ContrailServiceImpl service) 
	{
		return create(service, Mode.READONLY);
	}
	public static IResult<ContrailSessionImpl> create(final ContrailServiceImpl service, final long revisionNumber) 
	{
		return new ResultHandler<StorageSession>(service._storageSystem.beginSession(revisionNumber)) {
			protected IResult onSuccess(StorageSession session) throws Exception {
				ContrailSessionImpl impl= new ContrailSessionImpl(service, revisionNumber, session);
				return asResult(impl);
			}
		};
	}
	public static IResult<ContrailSessionImpl> create(final ContrailServiceImpl service, final Mode mode) 
	{
		return new ResultHandler<StorageSession>(service._storageSystem.beginSession(mode)) {
			protected IResult onSuccess(StorageSession session) throws Exception {
				ContrailSessionImpl impl= new ContrailSessionImpl(service, mode, session);
				return asResult(impl);
			}
		};
	}

	private ContrailSessionImpl(ContrailServiceImpl service, long revisionNumber, StorageSession storageSession) 
	throws ContrailException, IOException 
	{
		_service= service;
		_storageSession= storageSession;
		_searcher= new IndexSearcher(_storageSession);
	}
	

	private ContrailSessionImpl(ContrailServiceImpl service, Mode mode, StorageSession storageSession) 
	throws ContrailException, IOException 
	{
		_service= service;
		_storageSession= storageSession;
		_searcher= new IndexSearcher(_storageSession);
	}

	@Override public long getRevisionNumber() {
		return _storageSession.getRevisionNumber();
	}

	@Override public IResult<Void> commit() 
	{
		final StorageSession session= _storageSession;
		_searcher= null;
		_storageSession= null;
		return new Handler(session.commit()) {
			protected void onComplete() throws Exception {
				spawn(_service.onClose(ContrailSessionImpl.this));
			}
		};
	}

	@Override
	public IResult<Void> close() {
		StorageSession session= _storageSession;
		_searcher= null;
		_storageSession= null;
		return new Handler(_storageSession != null ? session.close() : TaskUtils.DONE) {
			protected void onComplete() throws Exception {
				spawn(_service.onClose(ContrailSessionImpl.this));
			}
		};
	}

	@Override
	public boolean isActive() {
		if (_storageSession == null)
			return false;
		return _storageSession.isActive();
	}

	@Override
	public <T extends Item> IResult<IPreparedQuery<T>> prepare(final ContrailQuery query) {
		return new Handler() {
			protected IResult onSuccess() throws Exception {
				if (_storageSession == null)
					throw new SessionAlreadyClosedException();
				
				return asResult(new PreparedQueryImpl<T>(_service, ContrailSessionImpl.this, query));
			}
		}.run();
	}

//	public <T extends Item> Iterable<T> search(ContrailQuery query) throws IOException {
//		if (_storageSession == null)
//			throw new SessionAlreadyClosedException();
//		
//		return _searcher.fetchEntities(query);
//	}

	@Override
	public IResult<Void> delete(final Collection<Identifier> paths) {
		return new Handler() {
			protected IResult onSuccess() throws Exception {
				if (_storageSession == null)
					throw new SessionAlreadyClosedException();
				return new ResultHandler<Collection<Item>>(fetch(paths)) {
					protected IResult onSuccess(Collection<Item> items) throws Exception {
						return delete(items);
					}
				};
			}
		}.run();
	}

	@Override
	public IResult<Void> deleteAllChildren(final Collection<Identifier> paths) {
		return new ResultHandler<Map<Identifier, Collection<Item>>>(fetchChildren(paths)) {
			protected IResult onSuccess(final Map<Identifier, Collection<Item>> allChildren) throws Exception {
				if (_storageSession == null)
					throw new SessionAlreadyClosedException();
				
				return new Handler(_storageSession.deleteAllChildren(paths)) {
					protected IResult onSuccess() throws Exception {
						
						ArrayList<Item> all= new ArrayList<Item>();
						for (Collection<Item> children: allChildren.values())
							all.addAll(children);
								
						return _searcher.unindex(all);
					}
				};
			}
		};
	}

	@Override
	public <T extends Item> IResult<Collection<T>> fetch(final Iterable<Identifier> paths) {
		return new Handler() {
			protected IResult onSuccess() throws Exception {
				if (_storageSession == null)
					throw new SessionAlreadyClosedException();
				return StorageUtils.fetchAll(_storageSession, paths);
			}
		}.run();
	}

	@Override
	public <T extends Item> IResult<Map<Identifier, Collection<T>>> fetchChildren(final Collection<Identifier> paths) {
		return new Handler() {
			protected IResult onSuccess() throws Exception {
				if (_storageSession == null)
					throw new SessionAlreadyClosedException();

				final HashMap<Identifier, IResult<Collection<T>>> fetched= new HashMap<Identifier, IResult<Collection<T>>>();
				for (Identifier path:paths) {
					IResult<Collection<T>> result= _storageSession.fetchChildren(path);
					fetched.put(path, result);
				}
				return new Handler(combineResults(fetched.values())) {
					protected IResult onSuccess() throws Exception {
						HashMap<Identifier, Collection<T>> items= new HashMap<Identifier, Collection<T>>();
						for (Identifier path:paths) {
							IResult<Collection<T>> result= fetched.get(path);
							items.put(path, result.getResult());
						}
						
						return asResult(items);
					}
				};
			}
		}.run();
	}

	public IResult<Void> flush() {
		return new Handler() {
			protected IResult onSuccess() {
				if (_storageSession == null)
					throw new SessionAlreadyClosedException();
				
				return _storageSession.flush();
			}
		}.run();
	}

	@Override
	public IStorageProvider getStorageProvider() {
		if (_storageSession == null)
			throw new SessionAlreadyClosedException();
		
		return _service._storageSystem.getStorageProvider();
	}

	@Override
	public IResult<Map<Identifier, Collection<Identifier>>> listChildren(final Collection<Identifier> paths) {
		return new Handler() {
			protected IResult onSuccess() throws Exception {
				if (_storageSession == null)
					throw new SessionAlreadyClosedException();

				final HashMap<Identifier, IResult<Collection<Identifier>>> fetched= new HashMap<Identifier, IResult<Collection<Identifier>>>();
				for (Identifier path:paths) {
					IResult<Collection<Identifier>> result= _storageSession.listChildren(path);
					fetched.put(path, result);
				}
				
				return new Handler(combineResults(fetched.values())) {
					protected IResult onSuccess() throws Exception {
						HashMap<Identifier, Collection<Identifier>> items= new HashMap<Identifier, Collection<Identifier>>();
						for (Identifier path:paths) {
							IResult<Collection<Identifier>> result= fetched.get(path);
							items.put(path, result.get());
						}
						
						return asResult(items);
					}
				};
			}
		}.run();
	}

	@Override
	public <T extends Item> IResult<Void> store(final Iterable<T> entities) {
		return new Action() {
			protected IResult onSuccess() throws Exception {
				if (_storageSession == null)
					throw new SessionAlreadyClosedException();

				ArrayList<IResult> tasks= new ArrayList<IResult>();
				for (T t:entities)
					tasks.add(_storageSession.store(t));
				return new Handler(combineResults(tasks)) {
					protected IResult onSuccess() throws Exception {
						return _searcher.index(entities);
					}
				};
			}
		};
	}

	@Override
	public IResult<Void> delete(Identifier... paths) {
		return new ResultHandler<Collection<Item>>(fetch(paths)) {
			protected IResult onSuccess(Collection<Item> items) throws Exception {
				return delete(items);
			}	
		};
	}

	@Override
	public <T extends Item> IResult<Void> delete(T... entities) {
		ArrayList<T> list= new ArrayList<T>();
		for (T entity: entities)
			list.add(entity);
		return delete(list);
	}

	@Override
	public IResult<Void> deleteAllChildren(Identifier... paths) {
		return deleteAllChildren(Arrays.asList(paths));
	}

	@Override
	public <T extends Item> IResult<Void> deleteAllChildren(T... entities) {
		ArrayList<Identifier> ids= new ArrayList<Identifier>();
		for (Item entity: entities)
			ids.add(entity.getId());
		return deleteAllChildren(ids);
	}

	@Override
	public <T extends Item> IResult<T> fetch(Identifier path) {
		final IResult<Collection<T>> fetch= fetch(Arrays.asList(new Identifier[] { path }));
		return new Handler(fetch) {
			protected IResult onSuccess() throws Exception {
				Collection<T> set= fetch.getResult();
				if (set.isEmpty())
					return TaskUtils.NULL;
				return asResult(set.iterator().next());
			}
		};
	}

	@Override
	public <T extends Item> IResult<Collection<T>> fetch(Identifier... paths) {
		return fetch(Arrays.asList(paths));
	}

	@Override
	public <T extends Item> IResult<Collection<T>> fetchChildren(final Identifier path) {
		final IResult<Map<Identifier, Collection<T>>> fetch= fetchChildren(Arrays.asList(new Identifier[] { path }));
		return new Handler(fetch) {
			protected IResult onSuccess() throws Exception {
				Map<Identifier, Collection<T>> set= fetch.getResult();
				return asResult(set.get(path));
			}
		};
	}

	@Override
	public <T extends Item> IResult<Map<Identifier, Collection<T>>> fetchChildren(Identifier... paths) {
		return fetchChildren(Arrays.asList(paths));
	}

	@Override
	public <T extends Item, C extends Item> IResult<Map<Identifier, Collection<C>>> fetchChildren(T... entities) {
		ArrayList<Identifier> ids= new ArrayList<Identifier>();
		for (Item entity: entities)
			ids.add(entity.getId());
		return fetchChildren(ids);
	}

	@Override
	public <T extends Item> IResult<Void> store(T... entities) {
		return store(Arrays.asList(entities));
	}

	@Override
	public IResult<Collection<Identifier>> listChildren(final Identifier path) {
		List<Identifier> ids= Arrays.asList(new Identifier[] { path });
		return new ResultHandler<Map<Identifier, Collection<Identifier>>>(listChildren(ids)) {
			protected IResult onSuccess(Map<Identifier, Collection<Identifier>> children) throws Exception {
				return asResult(children.get(path));
			}
		};
	}

	@Override
	public IResult<Map<Identifier, Collection<Identifier>>> listChildren(Identifier... paths) {
		return listChildren(Arrays.asList(paths));
	}

	@Override
	public <T extends Item> IResult<Map<Identifier, Collection<Identifier>>> listChildren(T... entities) {
		ArrayList<Identifier> ids= new ArrayList<Identifier>();
		for (Item entity: entities)
			ids.add(entity.getId());
		return listChildren(ids);
	}
	
	@Override
	public <T extends Item, C extends Item> IResult<Collection<C>> fetchChildren(T entity) {
		final Identifier i= entity.getId();
		final List<Identifier> ids= Arrays.asList(new Identifier[] { i });
		return new ResultHandler<Map<Identifier, Collection<Item>>>(fetchChildren(ids)) {
			protected IResult onSuccess(Map<Identifier, Collection<Item>> map) throws Exception {
				Collection<Item> set= map.get(i);
				if (set == null)
					return asResult(Collections.emptySet());
				return asResult(set);
			}
		};
	}

	@Override
	public <T extends Item> IResult<Void> delete(final Iterable<T> entities)
	{
		ArrayList<IResult> deletes= new ArrayList<IResult>();
		for (T t:entities)
			deletes.add(_storageSession.delete(t.getId()));
		return new Handler(TaskUtils.combineResults(deletes)) {
			protected IResult onSuccess() throws Exception {
				return _searcher.unindex(entities); 
			}
		};
	}

	@Override
	public <T extends Item> IResult<Void> deleteAllChildren(Iterable<T> paths) {
		ArrayList<Identifier> list= new ArrayList<Identifier>();
		for (T t: paths)
			list.add(t.getId());
		return deleteAllChildren(list);
	}

	@Override
	public <T extends Item> IResult<Collection<Identifier>> listChildren(T path)
	{
		final Identifier i= path.getId();
		final IResult<Map<Identifier, Collection<Identifier>>> fetch= listChildren(Arrays.asList(new Identifier[] { i }));
		return new Handler(fetch) {
			protected IResult onSuccess() throws Exception {
				Map<Identifier, Collection<Identifier>> map= fetch.getResult();
				Collection<Identifier> set= map.get(i);
				if (set == null)
					return asResult(Collections.emptySet());
				return asResult(set);
			}
		};
	}

	@Override
	public <E extends Item> IResult<Boolean> create(E entity) {
		return new Handler() {
			protected IResult onSuccess() throws Exception {
				throw new UnsupportedOperationException();
			}
		}.run();
	}

	@Override
	public <E extends Item, C extends Item> IResult<Map<Identifier, Collection<C>>> fetchChildren(Iterable<E> entities) 
	{
		ArrayList<Identifier> list= new ArrayList<Identifier>();
		for (IEntity t: entities)
			list.add(t.getId());
		return fetchChildren(list);
	}

	@Override
	public <E extends Item> IResult<Map<Identifier, Collection<Identifier>>> listChildren(Iterable<E> entities) 
	{
		ArrayList<Identifier> list= new ArrayList<Identifier>();
		for (IEntity t: entities)
			list.add(t.getId());
		return listChildren(list);
	}

	@Override
	public <E extends Item> IResult<Void> update(E... entities) {
		return store(entities);
	}

	@Override
	public <E extends Item> IResult<Void> update(Iterable<E> entities) {
		return store(entities);
	}

	public IResult<IAsyncerator<Identifier>> iterate(final ContrailQuery query) {
		return new Action() {
			protected IResult onSuccess() throws Exception {
				if (_storageSession == null)
					throw new SessionAlreadyClosedException();
				return _searcher.fetchIdentifiers(query);
			}
		};
	}

	public <E extends Item> IResult<E> fetch(IResult<Identifier> result) {
		return new ResultHandler<Identifier>(result) {
			protected IResult onSuccess(Identifier path) {
				return fetch(path);
			}
		};
	}

}
