package com.googlecode.contraildb.core.storage;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import com.googlecode.contraildb.core.Identifier;
import com.googlecode.contraildb.core.storage.provider.IStorageProvider;
import com.googlecode.contraildb.core.utils.ContrailTask;
import com.googlecode.contraildb.core.utils.IResult;


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
	
	public IEntityStorage.Session connect() throws IOException {
		return new Session();
	}
	
	public IStorageProvider getStorageProvider() {
		return _objectStorage.getStorageProvider();
	}
	

	public class Session implements IEntityStorage.Session {

		ObjectStorage.Session _objectSession;

		public Session() throws IOException {
			_objectSession= _objectStorage.connect(this);
		}
		
		public void close() throws IOException {
			_objectSession.close();
		}
		
		public void delete(Identifier path) {
			_objectSession.delete(path);
		}
		
		public void delete(Entity entity) {
			_objectSession.delete(entity.getId());
		}

		public void deleteAllChildren(Identifier path)
		{
			_objectSession.deleteAllChildren(path);
		}

		public void flush() throws IOException {
			_objectSession.flush();
		}

		public <E extends IEntity> IResult<E> fetch(Identifier path) {
			return _objectSession.fetch(path);
		}

		public <E extends IEntity> IResult<Collection<E>> fetchChildren(final Identifier path)
		{
			return new ContrailTask<Collection<E>>() {
				@SuppressWarnings("unchecked")
				protected void run() throws IOException {
					Map<Identifier, Serializable> children= _objectSession.fetchChildren(path).get();
					ArrayList<E> list= new ArrayList<E>(children.size());
					for (Serializable e:children.values())
						list.add((E)e);
					setResult(list);
				}
			}.submit();
		}

		public <E extends IEntity> void store(E entity) {
			_objectSession.store(entity.getId(), entity);
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
