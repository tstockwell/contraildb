package com.googlecode.contraildb.core.storage;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import kilim.Pausable;

import com.googlecode.contraildb.core.Identifier;
import com.googlecode.contraildb.core.async.ContrailAction;
import com.googlecode.contraildb.core.async.ContrailTask;
import com.googlecode.contraildb.core.async.IResult;
import com.googlecode.contraildb.core.async.Operation;
import com.googlecode.contraildb.core.async.TaskDomain;
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

	public IResult<Session> connect(final EntityStorage.Session entitySession) {
		return new ContrailTask<Session>() {
			@Override
			protected Session run() throws Pausable, Exception {
				IStorageProvider.Session storageSession = _storageProvider.connect().get();
				return new Session(_tracker.beginSession(), storageSession, entitySession);
			}
		}.submit();
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

		public <T extends Serializable> IResult<Void> store(final Identifier identifier, final T item) {
			
			return new ContrailAction() {
				@Override
				protected void action() throws Pausable, Exception {
					_trackerSession.submit(new ContrailAction(identifier, Operation.WRITE) {
						protected void action() throws Pausable, Exception {
							IResult<byte[]> serializeTask = new ExternalizationTask(item).submit();
							_storageSession.store(identifier, serializeTask).get();
						}
					}).get();
					
					final Object originalCached= _cache.fetch(identifier);
					try {
						_cache.store(identifier, item);
						
						if (item instanceof ILifecycle) {
							ILifecycle lifecycle = (ILifecycle) item;
							lifecycle.setStorage(_outerStorage).get();
							lifecycle.onInsert(identifier).get();
						}
					} 
					catch (Exception e) {
						if (originalCached == null) {
							_cache.delete(identifier);
						}
						else
							_cache.store(identifier, originalCached);
						throw e;
					}
				}
			}.submit();
		}

		public IResult<Void> delete(final Identifier path) {
			return new ContrailAction() {
				@Override
				protected void action() throws Pausable, Exception {
					final Object item = fetch(path).get();
					_trackerSession.submit(new ContrailAction(path, Operation.DELETE) {
						protected void action() throws IOException, Pausable {
							_cache.delete(path);
							_storageSession.delete(path);
						}
					}).get();

					if (item instanceof ILifecycle)
						((ILifecycle) item).onDelete().get();
				}
			}.submit();
		}

		public <T extends Serializable> IResult<T> fetch(final Identifier path) {
			return _trackerSession.submit(new ContrailTask<T>(path, Operation.READ) {
				protected T run() throws IOException, Pausable {
					T storable = (T) _cache.fetch(path);
					if (storable == null) {
						byte[] content = _storageSession.fetch(path).get();
						if (content != null) {
							storable = (T) readStorable(path, content).get();
						}
					}
					return storable;
				}
			});
		}

		private <T extends Serializable> IResult<T> readStorable(final Identifier id, final byte[] contents) 
		throws IOException 
		{
			if (contents == null)
				return TaskUtils.NULL;

			return new ContrailTask<T>() {
				@Override
				protected T run() throws Pausable, Exception {
					T s = ExternalizationManager.readExternal(new DataInputStream(new ByteArrayInputStream(contents)));
					boolean isStorable = s instanceof ILifecycle;
					if (isStorable)
						((ILifecycle) s).setStorage(_outerStorage).get();

					_cache.store(id, s);

					if (isStorable) {
						((ILifecycle) s).onLoad(id).get();
					}
					return s;
				}
			}.submit();
		}

		public <T extends Serializable> IResult<Map<Identifier, T>> fetchChildren(final Identifier path) {
			return _trackerSession.submit(new ContrailTask<Map<Identifier, T>>(path, Operation.LIST) {
				protected Map<Identifier, T> run() throws IOException, Pausable {
					Collection<Identifier> children = _storageSession.listChildren(path).get();
					HashMap<Identifier, IResult<byte[]>> fetched = new HashMap<Identifier, IResult<byte[]>>();
					for (Identifier childId : children)
						fetched.put(childId, _storageSession.fetch(childId));
					HashMap<Identifier, T> results = new HashMap<Identifier, T>();
					for (Identifier childId : children) {
						byte[] content = fetched.get(childId).get();
						T t = (T) readStorable(childId, content).get();
						results.put(childId, t);
					}
					return results;
				}
			});
		}

		public IResult<Collection<Identifier>> listChildren(final Identifier path) {
			return _storageSession.listChildren(path);
		}

		public IResult<Void> flush() {
			return _trackerSession.submit(new ContrailAction(null, Operation.FLUSH) {
				@Override
				protected void action() throws Pausable, Exception {
					_storageSession.flush();
				}
			});
		}

		public IResult<Void> close() {
			return _trackerSession.submit(new ContrailAction(null, Operation.FLUSH) {
				@Override
				protected void action() throws Pausable, Exception {
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
			});
		}

		public <T extends Serializable> IResult<Boolean> create(final Identifier identifier, final T item, final long waitMillis) {
			return new ContrailTask<Boolean>() {
				@Override
				protected Boolean run() throws Pausable, Exception {

					// first do the create
					boolean created= _trackerSession.submit(
						new ContrailTask<Boolean>(identifier, Operation.CREATE) {
							protected Boolean run() throws IOException, Pausable {
								IResult<byte[]> bytes = new ExternalizationTask(item).submit();
								boolean created = _storageSession.create(identifier, bytes, waitMillis).get();
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
							iLifecycle.setStorage(_outerStorage).get();
							iLifecycle.onInsert(identifier).get();
						} 
						catch (Exception e) {
							_cache.delete(identifier);
							throw e;
						}
					}
					
					return true;
				}
			}.submit();
		}

		public IResult<Void> delete(Identifier... paths) {
			ArrayList<IResult> all = new ArrayList<IResult>();
			for (Identifier identifier : paths)
				all.add(delete(identifier));
			return TaskUtils.combineResults(all);
		}

		public IResult<Void> deleteAllChildren(Identifier... paths) {
			return deleteAllChildren(Arrays.asList(paths));
		}

		public IResult<Void> deleteAllChildren(Iterable<Identifier> paths) {
			ArrayList<IResult> all = new ArrayList<IResult>();
			for (Identifier identifier : paths)
				all.add(deleteAllChildren(identifier));
			return TaskUtils.combineResults(all);
		}

		public IResult<Void> deleteAllChildren(final Identifier path) {
			return new ContrailAction() {
				@Override
				protected void action() throws Pausable, Exception {
					ArrayList<IResult> all = new ArrayList<IResult>();
					IResult<Collection<Identifier>> children = listChildren(path);
					Collection<Identifier> identifiers = children.get();
					for (Identifier identifier : identifiers) {
						all.add(delete(identifier));
					}
					TaskUtils.combineResults(all).join();
				}
			}.submit();
		}

	}
}
