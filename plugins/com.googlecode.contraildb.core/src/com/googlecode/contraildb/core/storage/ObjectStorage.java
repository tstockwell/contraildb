package com.googlecode.contraildb.core.storage;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import kilim.Pausable;

import com.googlecode.contraildb.core.Identifier;
import com.googlecode.contraildb.core.async.ContrailAction;
import com.googlecode.contraildb.core.async.ContrailTask;
import com.googlecode.contraildb.core.async.IResult;
import com.googlecode.contraildb.core.async.Operation;
import com.googlecode.contraildb.core.async.TaskDomain;
import com.googlecode.contraildb.core.async.TaskTracker;
import com.googlecode.contraildb.core.async.TaskUtils;
import com.googlecode.contraildb.core.storage.provider.IStorageProvider;
import com.googlecode.contraildb.core.utils.ExternalizationManager;
import com.googlecode.contraildb.core.utils.LRUIdentifierIndexedStorage;
import com.googlecode.contraildb.core.utils.Logging;
import com.googlecode.contraildb.core.utils.tasks.ExternalizationTask;

/*
 * Implementation Note:
 * I've realized that the caching done by this class is gonna have to change.
 * Caching is problematic since there can be multiple processes writing to storage. 
 * Based on the defined semantics for IStorageProvider,  we cannot caching 
 * lists of children.  Also, when fetching a stored object we should check to make 
 * sure that it has not been overwritten since the last time we cached it. 
 */

/**
 * An API for storing Java objects to a raw storage instance. This storage
 * API...
 * 
 * ...handles the serialization of Java objects to byte streams.
 * 
 * ...introduces some JPA-like lifecycle management. If a stored object
 * implements the ILifecycle interface then the ILifecycle methods will be invoked
 * at appropriate points.
 * 
 * ...caches objects in order to avoid as much serialization and deserialization
 * as possible.
 * 
 * ...can be used by multiple clients in multiple threads, each client should
 * call the ObjectStorage.connect method to create its own session. It is safe
 * to use an instance of ObjectStorage from multiple threads. It is also safe to
 * use an instance of ObjectStorage.Session from multiple threads.
 * 
 * ...is concurrent. This implementation manages the order in which its
 * internal tasks are executed so that operations are performed in a way that 
 * preserves sequential serialization (in other words, the operations appear to 
 * have all been executed sequentially, in the order that they arrived) while 
 * also allowing for as much parallelization as possible in order to maximize 
 * performance.  
 * 
 * @author Ted Stockwell
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class ObjectStorage {

	private IStorageProvider _storageProvider;
	private LRUIdentifierIndexedStorage _cache = new LRUIdentifierIndexedStorage();
	private TaskDomain _tracker = new TaskDomain();


	ObjectStorage(IStorageProvider storageProvider, EntityStorage outerStorage) {
		_storageProvider = storageProvider;
	}

	public Session connect(final EntityStorage.Session entitySession) throws Pausable {
		IStorageProvider.Session storageSession = _storageProvider.connect();
		TaskDomain.Session domainSession=_tracker.beginSession();
		Session session= new Session(domainSession, storageSession, entitySession);
		return session;
	}

	public IStorageProvider getStorageProvider() {
		return _storageProvider;
	}

	public class Session {

		private IStorageProvider.Session _storageSession;
		private EntityStorage.Session _outerStorage;
		private TaskDomain.Session _trackerSession;

		public Session(TaskDomain.Session tracker, IStorageProvider.Session storageProvider, EntityStorage.Session outerStorage) {
			_trackerSession = tracker;
			_storageSession = storageProvider;
			_outerStorage = outerStorage;
		}

		public <T extends Serializable> void store(final Identifier identifier, final T item)
		throws Pausable, IOException
		{

			final Object originalCached= _cache.fetch(identifier);
			_cache.store(identifier, item);

			// fire off task to store item
			IResult<Void> storeTask= _trackerSession.submit(new ContrailAction(identifier, Operation.WRITE) {
				protected void action() throws Pausable, Exception {
					IResult<byte[]> serializeTask = new ExternalizationTask(item).submit();
					_storageSession.store(identifier, serializeTask);
				}
			});
			
			try {

				if (item instanceof ILifecycle) {
					ILifecycle lifecycle = (ILifecycle) item;
					lifecycle.setStorage(_outerStorage);
					lifecycle.onInsert(identifier);
				}
				
				storeTask.get(); // wait for store task tocomplete
			} 
			catch (Exception e) {
				if (originalCached == null) {
					_cache.delete(identifier);
				}
				else
					_cache.store(identifier, originalCached);
				TaskUtils.throwSomething(e, IOException.class);
			}
		}

		public void delete(final Identifier path) throws IOException, Pausable {
			Object item = fetch(path);
			_cache.delete(path);
			_storageSession.delete(path);
			if (item instanceof ILifecycle)
				((ILifecycle) item).onDelete();
		}

		public <T extends Serializable> T fetch(final Identifier path) 
		throws Pausable, IOException 
		{
			T storable = (T) _cache.fetch(path);
			if (storable == null) {
				byte[] content = _storageSession.fetch(path);
				if (content != null) {
					storable = (T) readStorable(path, content);
				}
			}
			return storable;
		}

		private <T extends Serializable> T readStorable(final Identifier id, final byte[] contents) 
		throws Pausable, IOException 
		{
			if (contents == null)
				return null;

			T s = ExternalizationManager.readExternal(new DataInputStream(new ByteArrayInputStream(contents)));
			boolean isStorable = s instanceof ILifecycle;
			if (isStorable)
				((ILifecycle) s).setStorage(_outerStorage);

			_cache.store(id, s);

			if (isStorable) {
				((ILifecycle) s).onLoad(id);
			}
			return s;
		}

		public <T extends Serializable> Map<Identifier, T> fetchChildren(final Identifier path) 
		throws Pausable, IOException 
		{
			final Map<Identifier, T> results = Collections.synchronizedMap(new HashMap<Identifier, T>());
			try {
				Collection<Identifier> children = _storageSession.listChildren(path);
				final Map<Identifier, IResult<byte[]>> fetched = Collections.synchronizedMap(new HashMap<Identifier, IResult<byte[]>>());
				for (Identifier childId : children) {
					final Identifier id= childId;
					fetched.put(id, new ContrailTask<byte[]>() {
						@Override protected byte[] run() throws Pausable, Exception {
							return _storageSession.fetch(id);
						}
					}.submit());
				}
				TaskTracker tracker= new TaskTracker();
				for (Identifier childId : children) {
					final Identifier id= childId;
					tracker.submit(new ContrailAction() {
						@Override protected void action() throws Pausable, Exception {
							byte[] content = fetched.get(id).get();
							T t = (T) readStorable(id, content);
							results.put(id, t);
						}
					});
				}
				tracker.await();
			}
			catch (Throwable t) {
				TaskUtils.throwSomething(t, IOException.class);
			}
			return results;
		}

		public Collection<Identifier> listChildren(final Identifier path) throws Pausable {
			return _storageSession.listChildren(path);
		}

		public void flush() throws Pausable {
			_storageSession.flush();
		}

		public void close() throws Pausable {
			try {
				_storageSession.flush();
			} catch (Throwable t) {
				Logging.warning(t);
			}
			try {
				_trackerSession.close();
			} catch (Throwable t) {
				Logging.warning(t);
			}
			try {
				_storageSession.close();
			} catch (Throwable t) {
				Logging.warning(t);
			}
			_trackerSession = null;
			_storageSession = null;
			_outerStorage = null;
		}

		public <T extends Serializable> boolean create(final Identifier identifier, final T item, final long waitMillis)
		throws Pausable, IOException
		{
			// first do the create
			boolean created= _trackerSession.submit(
				new ContrailTask<Boolean>(identifier, Operation.CREATE) {
					protected Boolean run() throws IOException, Pausable {
						IResult<byte[]> bytes = new ExternalizationTask(item).submit();
						boolean created = _storageSession.create(identifier, bytes, waitMillis);
						_cache.store(identifier, item);
						return created;
					}
				}).get();
			
			if (!created)
				return false;

			// if the create succeeds and the item implements ILifecycle then 
			// also execute the ILifecycle.onInsert method.
			// The ILifecycle.onInsert should not be executed from within the 
			// create task since objects cannot create sub-objects until the 
			// create task is completed.
			if (item instanceof ILifecycle) { 
				try {
					ILifecycle iLifecycle= (ILifecycle)item;
					iLifecycle.setStorage(_outerStorage);
					iLifecycle.onInsert(identifier);
				} 
				catch (Exception e) {
					_cache.delete(identifier);
					TaskUtils.throwSomething(e, IOException.class);
				}
			}
			
			return true;
		}

		public void delete(Identifier... paths) throws IOException, Pausable {
			TaskTracker tracker= new TaskTracker();
			for (Identifier identifier : paths) {
				final Identifier id= identifier; 
				tracker.submit(new ContrailAction() {
					@Override protected void action() throws Pausable, IOException {
						delete(id);
					}
				});
			}
			tracker.await(IOException.class);
		}

		public void deleteAllChildren(Identifier... paths) 
		throws IOException, Pausable 
		{
			deleteAllChildren(Arrays.asList(paths));
		}

		public void deleteAllChildren(Iterable<Identifier> paths) 
		throws IOException, Pausable 
		{
			TaskTracker tracker= new TaskTracker();
			for (Identifier identifier : paths) {
				final Identifier id= identifier; 
				tracker.submit(new ContrailAction() {
					@Override protected void action() throws Pausable, IOException {
						deleteAllChildren(id);
					}
				});
			}
			tracker.await(IOException.class);
		}

		public void deleteAllChildren(final Identifier path) throws Pausable, IOException {
			TaskTracker tracker= new TaskTracker();
			Collection<Identifier> identifiers = listChildren(path);
			for (Identifier identifier : identifiers) {
				final Identifier id= identifier; 
				tracker.submit(new ContrailAction() {
					@Override protected void action() throws Pausable, IOException {
						deleteAllChildren(id);
					}
				});
			}
			tracker.await(IOException.class);
		}

	}
}
