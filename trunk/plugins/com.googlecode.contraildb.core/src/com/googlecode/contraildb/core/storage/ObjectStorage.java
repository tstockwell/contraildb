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

import com.googlecode.contraildb.core.IResult;
import com.googlecode.contraildb.core.Identifier;
import com.googlecode.contraildb.core.storage.provider.IStorageProvider;
import com.googlecode.contraildb.core.utils.ContrailTaskTracker;
import com.googlecode.contraildb.core.utils.ExternalizationManager;
import com.googlecode.contraildb.core.utils.LRUIdentifierIndexedStorage;
import com.googlecode.contraildb.core.utils.ResultHandler;
import com.googlecode.contraildb.core.utils.TaskUtils;
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

	public IResult<Session> connect() throws IOException {
		return new ResultHandler(_storageProvider.connect()) {
			protected IResult onSuccess() throws Exception {
				IStorageProvider.Session session= (IStorageProvider.Session) incoming().getResult();
				return TaskUtils.asResult(
						new Session(_tracker.beginSession(), session));
			};
		}.toResult();
	}
	
	public IResult<Session> connect(final EntityStorage.Session entitySession) {
		return new ResultHandler(_storageProvider.connect()) {
			protected IResult onSuccess() throws Exception {
				IStorageProvider.Session session= (IStorageProvider.Session) incoming().getResult();
				return TaskUtils.asResult(
						new Session(_tracker.beginSession(), session, entitySession));
			};
		}.toResult();
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
		public <T extends Serializable> IResult<Void> store(final Identifier identifier, final T item) 
		{
			final IResult<byte[]>  serializeTask= new ExternalizationTask(item).submit();

			final ILifecycle lifecycle= (item instanceof ILifecycle) ? (ILifecycle)item : null;
			if (lifecycle != null)
				lifecycle.setStorage(_outerStorage);
			
			_cache.store(identifier, item);
			
			return new ResultHandler() {
				protected IResult onSuccess() throws Exception {
					if (lifecycle != null)
						spawnChild(lifecycle.onInsert(identifier));
					spawnChild(_storageSession.store(identifier, serializeTask));
					return TaskUtils.SUCCESS;
				}
			}.toResult();
		}

		public IResult<Void> delete(final Identifier path) {
			return new ResultHandler(fetch(path)) {
				protected IResult onSuccess() throws IOException {

					spawnChild(new ResultHandler(_storageSession.delete(path)) {
						protected IResult onSuccess() throws IOException {
							_cache.delete(path);
							return null;
						}
					});

					Object item= incoming().getResult();
					if (item instanceof ILifecycle) 
						spawnChild(((ILifecycle)item).onDelete());
					
					return null;
				}
			}.toResult();
		}

		public <T extends Serializable> IResult<T> fetch(final Identifier path) 
		{
			return new ResultHandler(_cache.fetch(path)) {
				protected IResult onSuccess() throws IOException {
					Object storable= incoming().getResult();
					if (storable != null)
						return TaskUtils.asResult(storable);

					IResult content= _storageSession.fetch(path);
					return readStorable(path, content);
				}
			}.toResult();
		}
		
		private <T extends Serializable> IResult<T> readStorable(final Identifier id, IResult<byte[]> contents)
		throws IOException
		{
			return new ResultHandler(contents) {
				protected IResult onSuccess() throws Exception {
					byte[] bytes= (byte[]) incoming().getResult();
					T s= ExternalizationManager.readExternal(new DataInputStream(new ByteArrayInputStream(bytes)));
					boolean isStorable= s instanceof ILifecycle;
					if (isStorable)
						((ILifecycle)s).setStorage(_outerStorage);
					_cache.store(id, s);
					if (isStorable)
						spawnChild(((ILifecycle)s).onLoad(id));
					return TaskUtils.asResult(s);
				};
			}.toResult();
		}

		public <T extends Serializable> IResult<Map<Identifier, T>> fetchChildren(final Identifier path)
		{
			return new ResultHandler<Collection<Identifier>,Map<Identifier, T>>(_storageSession.listChildren(path)) {
				protected IResult<Map<Identifier, T>> onSuccess() throws Exception {
					Collection<Identifier> children= incoming().getResult();
					
					HashMap<Identifier, IResult<byte[]>> fetched= new HashMap<Identifier, IResult<byte[]>>();
					for (Identifier childId:children) 
						fetched.put(childId, _storageSession.fetch(childId));
					
					final HashMap results= new HashMap<Identifier, T>();
					for (final Identifier childId:children) {
						spawnChild(new ResultHandler(readStorable(childId, fetched.get(childId))) {
							protected IResult onSuccess() throws Exception {
								results.put(childId, incoming().getResult());
								return TaskUtils.SUCCESS;
							}
						});
					}
					return TaskUtils.asResult(results);
				};
			}.toResult();
		}

		public IResult<Collection<Identifier>> listChildren(final Identifier path)
		{
			return _storageSession.listChildren(path);
		}
		
		public IResult<Void> flush() {
			return new ResultHandler(_trackerSession.complete()) {
				protected IResult onSuccess() throws Exception {
					spawnChild(_storageSession.flush());
					return TaskUtils.SUCCESS;
				}
			}.toResult();
		}
		
		public IResult<Void> close() {
			return new ResultHandler(flush()) {
				protected IResult onSuccess() throws Exception {
					spawnChild(new ResultHandler(_trackerSession.close()) {
						protected IResult onSuccess() throws Exception {
							spawnChild(new ResultHandler(_storageSession.close()) {
								protected IResult onSuccess() throws Exception {
									_trackerSession= null;
									_storageSession= null;
									_outerStorage= null;
									return TaskUtils.SUCCESS;
								};
							});
							return TaskUtils.SUCCESS;
						};
					});
					return TaskUtils.SUCCESS;
				};
			}.toResult();
		}
		
		public <T extends Serializable> IResult<Boolean> create(final Identifier identifier, final T item, final long waitMillis)
		{
			final IResult<byte[]> serializeTask= new ExternalizationTask(item).submit();
			return new ResultHandler(_storageSession.create(identifier, serializeTask, waitMillis)) {
				protected IResult onSuccess() throws Exception {
					boolean isStorable= item instanceof ILifecycle;
					if (isStorable)
						((ILifecycle)item).setStorage(_outerStorage);
					_cache.store(identifier, item);
					if (isStorable) 
						spawnChild(((ILifecycle)item).onInsert(identifier));
					return TaskUtils.SUCCESS;
				};
			}.toResult();
		}
		
		
		public IResult<Void> delete(Identifier... paths) {
			ArrayList<IResult> tasks= new ArrayList<IResult>();
			for (Identifier identifier:paths)
					tasks.add(delete(identifier));
			return TaskUtils.combineResults(tasks);
		}

		public IResult<Void> deleteAllChildren(Identifier... paths) {
			return deleteAllChildren(Arrays.asList(paths));
		}

		public IResult<Void> deleteAllChildren(Iterable<Identifier> paths) {
			ArrayList<IResult> tasks= new ArrayList<IResult>();
			for (Identifier identifier:paths)
				deleteAllChildren(identifier);
			return TaskUtils.combineResults(tasks);
		}

		public IResult<Void> deleteAllChildren(Identifier path) {
			final IResult<Collection<Identifier>> children= listChildren(path);
			return new ResultHandler(children) {
				protected IResult onSuccess() throws Exception {
					ArrayList<IResult> tasks= new ArrayList<IResult>();
					for (Identifier identifier: children.get()) {
						tasks.add(delete(identifier));
					}
					return TaskUtils.combineResults(tasks);
				};
			}.toResult();
		}


	}
}
