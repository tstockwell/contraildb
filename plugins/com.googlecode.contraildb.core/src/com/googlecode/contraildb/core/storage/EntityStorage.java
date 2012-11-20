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
	
	public IResult<IEntityStorage.Session> connectA() {
		return new ContrailTask<IEntityStorage.Session>() {
			@Override protected IEntityStorage.Session run() throws Pausable, Exception {
				EntityStorage.Session session= new Session();
				ObjectStorage.Session objectSession= _objectStorage.connect(session).get();
				session._objectSession= objectSession;
				return session;
			}
		}.submit();
	}
	final public IEntityStorage.Session connect() throws Pausable {
		return connectA().get();
	}
	
	public IStorageProvider getStorageProvider() {
		return _objectStorage.getStorageProvider();
	}
	

	public class Session implements IEntityStorage.Session {

		ObjectStorage.Session _objectSession;

		Session() throws IOException {
		}
		public Session(ObjectStorage.Session session) throws IOException {
			_objectSession= session;
		}
		
		public IResult<Void> closeA() {
			return _objectSession.close();
		}
		@Override
		final public void close() throws Pausable {
			closeA().get();
		}
		
		@Override
		public IResult<Void> deleteA(Identifier path) {
			return _objectSession.delete(path);
		}
		@Override
		final public void delete(Identifier path) throws Pausable {
			deleteA(path).get();
		}
		
		@Override
		public <E extends IEntity> IResult<Void> deleteA(E entity) {
			return _objectSession.delete(entity.getId());
		}
		@Override
		final public <E extends IEntity> void delete(E entity) throws Pausable {
			deleteA(entity).get();
		}

		@Override
		public IResult<Void> deleteAllChildrenA(Identifier path) {
			return _objectSession.deleteAllChildren(path);
		}
		@Override
		final public void deleteAllChildren(Identifier path) throws Pausable {
			deleteAllChildrenA(path).get();
		}

		@Override
		public IResult<Void> flushA() {
			return _objectSession.flush();
		}
		@Override
		final public void flush() throws Pausable {
			flushA().get();
		}

		@Override
		public <E extends IEntity> IResult<E> fetchA(Identifier path) {
			return _objectSession.fetch(path);
		}
		@Override
		final public <E extends IEntity> E fetch(Identifier path) throws Pausable {
			return (E) fetchA(path).get();
		}

		@Override
		public <E extends IEntity> IResult<Collection<E>> fetchChildrenA(final Identifier path)
		{
			return new ContrailTask<Collection<E>>() {
				protected Collection<E> run() throws Pausable, IOException {
					Map<Identifier, Serializable> children= _objectSession.fetchChildren(path).get();
					ArrayList<E> list= new ArrayList<E>(children.size());
					for (Serializable e:children.values())
						list.add((E)e);
					return list;
				}
			}.submit();
		}
		@Override
		final public <E extends IEntity> Collection<E> fetchChildren(Identifier path) throws Pausable {
			return (Collection<E>) fetchChildrenA(path).get();
		}

		@Override
		public <E extends IEntity> IResult<Void> storeA(E entity) {
			return _objectSession.store(entity.getId(), entity);
		}
		@Override
		final public <E extends IEntity> void store(E entity) throws Pausable {
			storeA(entity).get();
		}

		@Override
		public IResult<Collection<Identifier>> listChildrenA(Identifier path) 
		{
			return _objectSession.listChildren(path);
		}
		@Override
		final public Collection<Identifier> listChildren(Identifier path) throws Pausable {
			return listChildrenA(path).get();
		}

		public <E extends IEntity> IResult<Boolean> createA(E entity, long waitMillis) {
			return _objectSession.create(entity.getId(), entity, waitMillis);
		}
		final public <E extends IEntity> boolean create(E entity, long waitMillis) throws Pausable {
			return createA(entity, waitMillis).get();
		}

	}


}
