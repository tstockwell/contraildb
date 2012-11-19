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
import java.util.Map;

import kilim.Pausable;

import com.googlecode.contraildb.core.ConflictingCommitException;
import com.googlecode.contraildb.core.ContrailException;
import com.googlecode.contraildb.core.ContrailQuery;
import com.googlecode.contraildb.core.IContrailSession;
import com.googlecode.contraildb.core.IPreparedQuery;
import com.googlecode.contraildb.core.Identifier;
import com.googlecode.contraildb.core.Item;
import com.googlecode.contraildb.core.SessionAlreadyClosedException;
import com.googlecode.contraildb.core.IContrailService.Mode;
import com.googlecode.contraildb.core.async.ContrailAction;
import com.googlecode.contraildb.core.async.IResult;
import com.googlecode.contraildb.core.async.TaskTracker;
import com.googlecode.contraildb.core.storage.IEntity;
import com.googlecode.contraildb.core.storage.StorageSession;
import com.googlecode.contraildb.core.storage.StorageUtils;
import com.googlecode.contraildb.core.storage.provider.IStorageProvider;



/**
 * Wraps a StorageSession and adds indexing and search functionality. 
 * IContrailSession interface.
 * 
 * @author Ted Stockwell
 */
public class ContrailSessionImpl
implements IContrailSession 
{
	
	private IndexSearcher _searcher;
	private StorageSession _storageSession;
	private ContrailServiceImpl _service;
	
	ContrailSessionImpl(ContrailServiceImpl service) 
	throws ContrailException, IOException 
	{
		this(service, Mode.READONLY);
	}

	public ContrailSessionImpl(ContrailServiceImpl service, long revisionNumber) 
	throws ContrailException, IOException 
	{
		_service= service;
		_storageSession= service._storageSystem.beginSession(revisionNumber);
		_searcher= new IndexSearcher(_storageSession);
	}
	

	public ContrailSessionImpl(ContrailServiceImpl service, Mode mode) 
	throws ContrailException, IOException 
	{
		_service= service;
		_storageSession= service._storageSystem.beginSession(mode);
		_searcher= new IndexSearcher(_storageSession);
	}

	@Override public long getRevisionNumber() {
		return _storageSession.getRevisionNumber();
	}

	@Override public void commit() 
	throws ConflictingCommitException, IOException 
	{
		try {
			StorageSession session= _storageSession;
			_searcher= null;
			_storageSession= null;
			session.commit();
		}
		finally {
			_service.onClose(this);
		}
	}

	@Override
	public void close() throws IOException  {
		try {
			if (_storageSession != null) {
				StorageSession session= _storageSession;
				_searcher= null;
				_storageSession= null;
				session.close();
			}
		}
		finally {
			_service.onClose(this);
		}
	}

	@Override
	public boolean isActive() {
		if (_storageSession == null)
			return false;
		return _storageSession.isActive();
	}

	@Override
	public <T extends Item> IPreparedQuery<T> prepare(ContrailQuery query) {
		if (_storageSession == null)
			throw new SessionAlreadyClosedException();
		
		return new PreparedQueryImpl<T>(_service, this, query);
	}

	public <T extends Item> Iterable<T> search(ContrailQuery query) throws IOException {
		if (_storageSession == null)
			throw new SessionAlreadyClosedException();
		
		return _searcher.fetchEntities(query);
	}

	public Iterable<Identifier> fetchIdentifiers(ContrailQuery query) throws IOException {
		if (_storageSession == null)
			throw new SessionAlreadyClosedException();
		
		return _searcher.fetchIdentifiers(query);
	}

	@Override
	public void delete(Collection<Identifier> paths) throws IOException {
		if (_storageSession == null)
			throw new SessionAlreadyClosedException();
		
		delete(fetch(paths));
	}

	@Override
	public void deleteAllChildren(Collection<Identifier> paths) throws IOException {
		if (_storageSession == null)
			throw new SessionAlreadyClosedException();
		
		Map<Identifier, Collection<Item>> allChildren= fetchChildren(paths);
		_storageSession.deleteAllChildren(paths);
		ArrayList<Item> all= new ArrayList<Item>();
		for (Collection<Item> children: allChildren.values())
			all.addAll(children);
		_searcher.unindex(all);
	}

	@Override
	public <T extends Item> Collection<T> fetch(Iterable<Identifier> paths) throws IOException {
		if (_storageSession == null)
			throw new SessionAlreadyClosedException();
		return StorageUtils.syncFetch(_storageSession, paths);
	}

	@Override
	public <T extends Item> Map<Identifier, Collection<T>> fetchChildren(final Collection<Identifier> paths) throws IOException {
		
		if (_storageSession == null)
			throw new SessionAlreadyClosedException();
		
		final HashMap<Identifier, Collection<T>> items= new HashMap<Identifier, Collection<T>>();
		new ContrailAction() {
			@Override
			protected void action() throws Pausable, Exception {
				HashMap<Identifier, IResult<Collection<T>>> fetched= new HashMap<Identifier, IResult<Collection<T>>>();
				for (Identifier path:paths) {
					IResult<Collection<T>> result= _storageSession.fetchChildren(path);
					fetched.put(path, result);
				}
				for (Identifier path:paths) {
					Collection<T> results= fetched.get(path).get();
					items.put(path, results);
				}
			}
		}.submit().getb();

		return items;
	}

	public void flush() throws IOException {
		if (_storageSession == null)
			throw new SessionAlreadyClosedException();
		
		_storageSession.flush();
	}

	@Override
	public IStorageProvider getStorageProvider() {
		if (_storageSession == null)
			throw new SessionAlreadyClosedException();
		
		return _service._storageSystem.getStorageProvider();
	}

	@Override
	public Map<Identifier, Collection<Identifier>> listChildren(final Collection<Identifier> paths) throws IOException {
		
		if (_storageSession == null)
			throw new SessionAlreadyClosedException();
		
		final HashMap<Identifier, Collection<Identifier>> items= new HashMap<Identifier, Collection<Identifier>>();
		new ContrailAction() {
			@Override
			protected void action() throws Pausable, Exception {
				HashMap<Identifier, IResult<Collection<Identifier>>> fetched= new HashMap<Identifier, IResult<Collection<Identifier>>>();
				for (Identifier path:paths) {
					IResult<Collection<Identifier>> result= _storageSession.listChildren(path);
					fetched.put(path, result);
				}
				for (Identifier path:paths) {
					Collection<Identifier> results= fetched.get(path).get();
					items.put(path, results);
				}
			}
		}.submit().getb();
		
		return items;
	}

	@Override
	public <T extends Item> void store(Iterable<T> entities) throws IOException {
		if (_storageSession == null)
			throw new SessionAlreadyClosedException();

		for (T t:entities)
			_storageSession.store(t);
		
		_searcher.index(entities);
		
		// dont return until above calls to store have completed
		_storageSession.flush(); 
	}

	@Override
	public void delete(Identifier... paths) throws IOException {
		delete(fetch(paths));
	}

	@Override
	public <T extends Item> void delete(T... entities) throws IOException {
		ArrayList<T> list= new ArrayList<T>();
		for (T entity: entities)
			list.add(entity);
		delete(list);
	}

	@Override
	public void deleteAllChildren(Identifier... paths) throws IOException {
		deleteAllChildren(Arrays.asList(paths));
	}

	@Override
	public <T extends Item> void deleteAllChildren(T... entities) throws IOException {
		ArrayList<Identifier> ids= new ArrayList<Identifier>();
		for (Item entity: entities)
			ids.add(entity.getId());
		deleteAllChildren(ids);
	}

	@Override
	public <T extends Item> T fetch(Identifier path) throws IOException {
		Collection<T> set= fetch(Arrays.asList(new Identifier[] { path }));
		if (set.isEmpty())
			return null;
		return set.iterator().next();
	}

	@Override
	public <T extends Item> Collection<T> fetch(Identifier... paths) throws IOException {
		return fetch(Arrays.asList(paths));
	}

	@Override
	public <T extends Item> Collection<T> fetchChildren(Identifier path) throws IOException {
		Map<Identifier, Collection<T>> set= fetchChildren(Arrays.asList(new Identifier[] { path }));
		return set.get(path);
	}

	@Override
	public <T extends Item> Map<Identifier, Collection<T>> fetchChildren(Identifier... paths)
	throws IOException {
		return fetchChildren(Arrays.asList(paths));
	}

	@Override
	public <T extends Item, C extends Item> Map<Identifier, Collection<C>> fetchChildren(T... entities)
			throws IOException {
		ArrayList<Identifier> ids= new ArrayList<Identifier>();
		for (Item entity: entities)
			ids.add(entity.getId());
		return fetchChildren(ids);
	}

	@Override
	public <T extends Item> void store(T... entities) throws IOException {
		store(Arrays.asList(entities));
	}

	@Override
	public Collection<Identifier> listChildren(Identifier path) throws IOException {
		Map<Identifier, Collection<Identifier>> set= listChildren(Arrays.asList(new Identifier[] { path }));
		return set.get(path);
	}

	@Override
	public Map<Identifier, Collection<Identifier>> listChildren(Identifier... paths)
			throws IOException {
		return listChildren(Arrays.asList(paths));
	}

	@Override
	public <T extends Item> Map<Identifier, Collection<Identifier>> listChildren(T... entities)
			throws IOException {
		ArrayList<Identifier> ids= new ArrayList<Identifier>();
		for (Item entity: entities)
			ids.add(entity.getId());
		return listChildren(ids);
	}
	
	@Override
	public <T extends Item, C extends Item> Collection<C> fetchChildren(T entity) throws IOException {
		Identifier i= entity.getId();
		Map<Identifier, Collection<C>> map= fetchChildren(Arrays.asList(new Identifier[] { i }));
		Collection<C> set= map.get(i);
		if (set == null)
			return Collections.emptySet();
		return set;
	}

	@Override
	public <T extends Item> void delete(Iterable<T> entities)
	throws IOException 
	{
		for (T t:entities)
			_storageSession.delete(t.getId());
		_searcher.unindex(entities);
		
	}

	@Override
	public <T extends Item> void deleteAllChildren(Iterable<T> paths)
	throws IOException 
	{
		ArrayList<Identifier> list= new ArrayList<Identifier>();
		for (T t: paths)
			list.add(t.getId());
		deleteAllChildren(list);
	}

	@Override
	public <T extends Item> Collection<Identifier> listChildren(T path)
	throws IOException 
	{
		Identifier i= path.getId();
		Map<Identifier, Collection<Identifier>> map= listChildren(Arrays.asList(new Identifier[] { i }));
		Collection<Identifier> set= map.get(i);
		if (set == null)
			return Collections.emptySet();
		return set;
	}

	@Override
	public <E extends Item> boolean create(E entity) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public <E extends Item, C extends Item> Map<Identifier, Collection<C>> fetchChildren(Iterable<E> entities) 
	throws IOException 
	{
		ArrayList<Identifier> list= new ArrayList<Identifier>();
		for (IEntity t: entities)
			list.add(t.getId());
		return fetchChildren(list);
	}

	@Override
	public <E extends Item> Map<Identifier, Collection<Identifier>> listChildren(Iterable<E> entities) 
	throws IOException 
	{
		ArrayList<Identifier> list= new ArrayList<Identifier>();
		for (IEntity t: entities)
			list.add(t.getId());
		return listChildren(list);
	}

	@Override
	public <E extends Item> void update(E... entities) throws IOException {
		store(entities);
	}

	@Override
	public <E extends Item> void update(Iterable<E> entities) throws IOException {
		store(entities);
	}

}
