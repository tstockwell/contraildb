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
public class EntityStorage implements IEntityStorage {

	ObjectStorage _objectStorage;

	public EntityStorage(IStorageProvider storageProvider) {
		_objectStorage= new ObjectStorage(storageProvider, this);
	}
	
	public IResult<IEntityStorage.Session> connect() throws IOException {
		return new ContrailTask<IEntityStorage.Session>() {
			@Override protected IEntityStorage.Session run() throws Pausable, Exception {
				EntityStorage.Session session= new Session();
				ObjectStorage.Session objectSession= _objectStorage.connect(session).get();
				session._objectSession= objectSession;
				return session;
			}
		}.submit();
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
		
		public IResult<Void> close() {
			return _objectSession.close();
		}
		
		public IResult<Void> delete(Identifier path) {
			return _objectSession.delete(path);
		}
		
		public IResult<Void> delete(Entity entity) {
			return _objectSession.delete(entity.getId());
		}

		public IResult<Void> deleteAllChildren(Identifier path)
		{
			return _objectSession.deleteAllChildren(path);
		}

		public IResult<Void> flush() {
			return _objectSession.flush();
		}

		public <E extends IEntity> IResult<E> fetch(Identifier path) {
			return _objectSession.fetch(path);
		}

		public <E extends IEntity> IResult<Collection<E>> fetchChildren(final Identifier path)
		{
			return new ContrailTask<Collection<E>>() {
				@SuppressWarnings("unchecked")
				protected Collection<E> run() throws Pausable, IOException {
					Map<Identifier, Serializable> children= _objectSession.fetchChildren(path).get();
					ArrayList<E> list= new ArrayList<E>(children.size());
					for (Serializable e:children.values())
						list.add((E)e);
					return list;
				}
			}.submit();
		}

		public <E extends IEntity> IResult<Void> store(E entity) {
			return _objectSession.store(entity.getId(), entity);
		}

		public IResult<Collection<Identifier>> listChildren(Identifier path) 
		{
			return _objectSession.listChildren(path);
		}

		public <E extends IEntity> IResult<Boolean> create(E entity, long waitMillis) {
			return _objectSession.create(entity.getId(), entity, waitMillis);
		}

	}


}
