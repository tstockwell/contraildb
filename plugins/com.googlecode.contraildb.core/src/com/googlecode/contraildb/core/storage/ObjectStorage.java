package com.googlecode.contraildb.core.storage;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.googlecode.contraildb.core.IResult;
import com.googlecode.contraildb.core.IResultHandler;
import com.googlecode.contraildb.core.Identifier;
import com.googlecode.contraildb.core.storage.provider.IStorageProvider;
import com.googlecode.contraildb.core.utils.ContrailAction;
import com.googlecode.contraildb.core.utils.ContrailTask;
import com.googlecode.contraildb.core.utils.ContrailTaskTracker;
import com.googlecode.contraildb.core.utils.ExternalizationManager;
import com.googlecode.contraildb.core.utils.LRUIdentifierIndexedStorage;
import com.googlecode.contraildb.core.utils.Logging;
import com.googlecode.contraildb.core.utils.Result;
import com.googlecode.contraildb.core.utils.TaskUtils;
import com.googlecode.contraildb.core.utils.ContrailTask.Operation;
import com.googlecode.contraildb.core.utils.tasks.ExternalizationTask;


/**
 * An API for storing Java objects to a raw storage instance.
 * This storage API...
 * 
 * 	...handles the serialization of Java objects to byte streams.
 * 
 * 	...introduces some JPA-like lifecycle management.
 * 		If a stored object implements the Lifecycle interface then 
 * 		the Lifecycle methods will be invoked at appropriate points.
 * 
 * 	...cache objects in order to avoid as much serialization and 
 * 		deserialization as possible (stored objects are cached, but not the lists of children).
 * 
 * 	...can be used by multiple clients in multiple threads, each client should call the
 * 		ObjectStorage.connect method to create its own session.  
 * 		It is safe to use an instance of ObjectStorage from multiple threads. 
 * 		It is also safe to use an instance of ObjectStorage.Session from multiple threads. 
 * 
 * 	...is multithreaded.  This implementation manages the order in which 
 * 		its internal tasks are executed so that operations are performed in a 
 * 		way that allows for as much parallelization as possible in order to 
 * 		maximize performance.  
 * 
 * Like many of Contrail's internal API's all the methods in this class are asynchronous, 
 * they all return an instance of IResult.
 *   
 * @author Ted Stockwell
 */
@SuppressWarnings({"unchecked","rawtypes"})
public class ObjectStorage {
	
	private IStorageProvider _storageProvider;
	private LRUIdentifierIndexedStorage _cache= new LRUIdentifierIndexedStorage();
	private ContrailTaskTracker _tracker= new ContrailTaskTracker();

	/**
	 * @param storageProvider a raw storage provider
	 */
	public ObjectStorage(IStorageProvider storageProvider) {
		this(storageProvider, null);
	}
	public ObjectStorage(IStorageProvider storageProvider, EntityStorage outerStorage) {
		_storageProvider= storageProvider;
	}

	public Session connect() throws IOException {
		return new Session(_tracker.beginSession(), _storageProvider.connect());
	}
	public Session connect(EntityStorage.Session entitySession) throws IOException {
		return new Session(_tracker.beginSession(), _storageProvider.connect(), entitySession);
	}
	public IStorageProvider getStorageProvider() {
		return _storageProvider;
	}
	
	
	public class Session {
		
		private IStorageProvider.Session _storageSession;
		private EntityStorage.Session _outerStorage;
		private ContrailTaskTracker.Session _trackerSession;

		public Session(ContrailTaskTracker.Session tracker, IStorageProvider.Session storageProvider) {
			this(tracker, storageProvider, null);
		}
		public Session(ContrailTaskTracker.Session tracker, IStorageProvider.Session storageProvider, EntityStorage.Session outerStorage) {
			_trackerSession= tracker;
			_storageSession= storageProvider;
			_outerStorage= outerStorage; 
		}
		public <T extends Serializable> void store(final Identifier identifier, final T item) 
		{
			final IResult<byte[]>  serializeTask= new ExternalizationTask(item).submit();

			final ILifecycle lifecycle= (item instanceof ILifecycle) ? (ILifecycle)item : null;
			if (lifecycle != null)
				lifecycle.setStorage(_outerStorage);

			_trackerSession.submit(new ContrailAction(identifier, Operation.WRITE) {
				protected void action() throws IOException {
					_cache.store(identifier, item);
					if (lifecycle != null)
						lifecycle.onInsert(identifier);
					_storageSession.store(identifier, serializeTask);

				}
			});
		}

		public IResult<Void> delete(final Identifier path) {
			final IResult<Serializable> itemRef= fetch(path);
			return new ResultHandler(itemRef) {
				void success() { 
					_cache.delete(path);
					handle(_storageSession.delete(path));
					
					Object item= itemRef.getResult();
					if (item instanceof ILifecycle) {
						((ILifecycle)item).onDelete();
					}
				}
			}.toResult();
		}

		public <T extends Serializable> IResult<T> fetch(final Identifier path) 
		{
			ContrailTask<T> task= new ContrailTask<T>(path, Operation.READ) {
				protected T run() throws IOException {
					T storable= (T)_cache.fetch(path);
					if (storable == null) {
						IResult<byte[]> content= _storageSession.fetch(path);
						if (content != null) 
							storable= readStorable(path, content.get());
					}
					return storable;
				}
			};
			return _trackerSession.submit(task);
		}
		
		private <T extends Serializable> T readStorable(Identifier id, byte[] contents)
		throws IOException
		{
			if (contents == null)
				return null;

			T s= ExternalizationManager.readExternal(new DataInputStream(new ByteArrayInputStream(contents)));
			boolean isStorable= s instanceof ILifecycle;
			if (isStorable)
				((ILifecycle)s).setStorage(_outerStorage);
			_cache.store(id, s);
			if (isStorable)
				((ILifecycle)s).onLoad(id);
			return s;
		}

		public <T extends Serializable> IResult<Map<Identifier, T>> fetchChildren(final Identifier path)
		{
			ContrailTask<Map<Identifier, T>> task= new ContrailTask<Map<Identifier, T>>(path, Operation.LIST) {
				protected Map<Identifier, T> run() throws IOException {
					Collection<Identifier> children= _storageSession.listChildren(path).get();
					HashMap<Identifier, IResult<byte[]>> fetched= new HashMap<Identifier, IResult<byte[]>>();
					for (Identifier childId:children) 
						fetched.put(childId, _storageSession.fetch(childId));
					HashMap<Identifier, T> results= new HashMap<Identifier, T>();
					for (Identifier childId:children) {
						IResult<byte[]> result= fetched.get(childId);
						T t= readStorable(childId, result.get());
						results.put(childId, t);
					}
					return results;
				}
			};
			return _trackerSession.submit(task);
		}

		public IResult<Collection<Identifier>> listChildren(final Identifier path)
		{
			return _trackerSession.submit(new ContrailTask<Collection<Identifier>>(path, Operation.LIST) {
				protected Collection<Identifier> run() {
					return _storageSession.listChildren(path).get();
				}
			});
		}
		
		public void flush() throws IOException {
			_trackerSession.onComplete(new IResultHandler<Void>() {
				public void complete(IResult<Void> result) {
					// TODO Auto-generated method stub
					
				}
			});
			try {
				_trackerSession.join();
				_storageSession.flush();
			}
			catch (Throwable t) {
				TaskUtils.throwSomething(t, IOException.class);
			}
		}
		
		public void close() throws IOException {
			flush();
			try { _trackerSession.close().get(); } catch (Throwable t) { Logging.warning(t); }
			try { _storageSession.close().get(); } catch (Throwable t) {  Logging.warning(t); }
			_trackerSession= null;
			_storageSession= null;
			_outerStorage= null;
		}
		
		public <T extends Serializable> IResult<Boolean> create(final Identifier identifier, final T item, final long waitMillis)
		{
			ContrailTask<Boolean> action= new ContrailTask<Boolean>(identifier, Operation.CREATE) {
				protected Boolean run() throws IOException {

					IResult<byte[]> serializeTask= new ExternalizationTask(item).submit();

					boolean created= false;
					IResult<Boolean> result= _storageSession.create(identifier, serializeTask, waitMillis);
					if (result.get()) {
						boolean isStorable= item instanceof ILifecycle;
						if (isStorable)
							((ILifecycle)item).setStorage(_outerStorage);
						_cache.store(identifier, item);
						if (isStorable) 
							((ILifecycle)item).onInsert(identifier);
						created= true;
					}
					return created;
				}
			};
			return _trackerSession.submit(action);
		}
		
		
		public void delete(Identifier... paths) throws IOException {
			for (Identifier identifier:paths)
					delete(identifier);
		}

		public void deleteAllChildren(Identifier... paths) throws IOException {
			deleteAllChildren(Arrays.asList(paths));
		}

		public void deleteAllChildren(Iterable<Identifier> paths) throws IOException {
			for (Identifier identifier:paths)
				deleteAllChildren(identifier);
		}

		public void deleteAllChildren(Identifier path) {
			final IResult<Collection<Identifier>> children= listChildren(path);
			ContrailAction action= new ContrailAction(path, Operation.LIST) {
				protected void action() {
					for (Identifier identifier: children.get()) {
						delete(identifier);
					}
				}
			};
			_trackerSession.submit(action);
		}


	}
}
