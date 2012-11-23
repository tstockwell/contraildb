package com.googlecode.contraildb.core.storage;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import kilim.Pausable;

import com.googlecode.contraildb.core.Identifier;
import com.googlecode.contraildb.core.async.ContrailTask;
import com.googlecode.contraildb.core.async.IResult;
import com.googlecode.contraildb.core.storage.provider.IStorageProvider;


/**
 * An even more convenient API for storing Java objects than ObjectStore.
 * 
 * This API stores instances of IEntity instead of POJOs, thus simplifying the 
 * API somewhat.
 * Other than that, EntityStorage is like ObjectStore, this API handles the 
 * serialization of Java objects to byte streams, object caching, and ILifecycle 
 * lifecycle management.
 * 
 * Implementations should also cache objects in order to avoid as much 
 * serialization and deserialization as possible.
 * 
 * @author Ted Stockwell
 */
@SuppressWarnings("unchecked")
public class EntityStorage implements IEntityStorage {

	ObjectStorage _objectStorage;

	public EntityStorage(IStorageProvider storageProvider) {
		_objectStorage= new ObjectStorage(storageProvider, this);
	}
	
	public IEntityStorage.Session connect() throws Pausable {
		EntityStorage.Session session= new Session();
		ObjectStorage.Session objectSession= _objectStorage.connect(session);
		session._objectSession= objectSession;
		return session;
	}
	
	public IStorageProvider getStorageProvider() {
		return _objectStorage.getStorageProvider();
	}
	

	public class Session implements IEntityStorage.Session {

		ObjectStorage.Session _objectSession;

		Session() {
		}
		public Session(ObjectStorage.Session session) {
			_objectSession= session;
		}
		
		@Override public void close() throws Pausable {
			_objectSession.close();
		}
		
		@Override public void delete(Identifier path) throws Pausable, IOException {
			_objectSession.delete(path);
		}
		
		@Override public <E extends IEntity> void delete(E entity) throws Pausable, IOException {
			_objectSession.delete(entity.getId());
		}

		@Override public void deleteAllChildren(Identifier path) throws Pausable, IOException {
			_objectSession.deleteAllChildren(path);
		}

		@Override public void flush() throws Pausable {
			_objectSession.flush();
		}

		@Override public <E extends IEntity> E fetch(Identifier path) 
		throws Pausable, IOException 
		{
			return _objectSession.fetch(path);
		}

		@Override
		public <E extends IEntity> Collection<E> fetchChildren(final Identifier path) 
		throws Pausable, IOException
		{
			Map<Identifier, Serializable> children= _objectSession.fetchChildren(path);
			ArrayList<E> list= new ArrayList<E>(children.size());
			for (Serializable e:children.values())
				list.add((E)e);
			return list;
		}

		@Override
		public <E extends IEntity> void store(E entity) 
		throws Pausable, IOException 
		{
			_objectSession.store(entity.getId(), entity);
		}

		@Override public Collection<Identifier> listChildren(Identifier path) 
		throws Pausable
		{
			return _objectSession.listChildren(path);
		}

		public <E extends IEntity> boolean create(E entity, long waitMillis) 
		throws Pausable, IOException 
		{
			return _objectSession.create(entity.getId(), entity, waitMillis);
		}
	}
}
