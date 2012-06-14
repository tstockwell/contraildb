package storage



/**
 * An API for storing objects to a raw storage instance.
 * This storage API...
 * 
 * 	...handles the serialization of objects to byte streams.
 * 
 * 	...introduces some JPA-like lifecycle management.
 * 		If a stored object implements the Lifecycle interface then 
 * 		the Lifecycle methods will be invoked at appropriate points.
 * 
 * 	...caches objects in order to avoid as much serialization and 
 * 		deserialization as possible.
 * 
 * 	...can be used by multiple clients in multiple threads.
 *		Each client should call the ObjectStorage.Connect method to create its 
 *		own session.  
 * 		It is safe to use an instance of ObjectStorage from multiple threads. 
 * 		It is also safe to use an instance of ObjectStorage.Session from multiple threads. 
 * 
 * 	...is parallel.  This implementation manages the order in which 
 * 		its internal tasks are executed so that operations are performed in a 
 * 		way that allows for as much parallelization as possible in order to 
 * 		maximize performance.  
 *   
 * @author Ted Stockwell
 */
type ObjectStorage struct {
	storageProvider *StorageProvider
	cache *LRUIdentifierIndexedStorage
	tracker *TaskMaster
}

func CreateObjectStorage (storageProvider *StorageProvider) *ObjectStorage {
	return &ObjectStorage {
		storageProvider: storageProvider,
		cache: 
	}
}
public ObjectStorage(IStorageProvider storageProvider, EntityStorage outerStorage) {
	_storageProvider= storageProvider;
}

public class ObjectStorage {
	


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
			final ExternalizationTask  serializeTask= new ExternalizationTask(item).submit();

			final ILifecycle lifecycle= (item instanceof ILifecycle) ? (ILifecycle)item : null;
			if (lifecycle != null)
				lifecycle.setStorage(_outerStorage);

			_trackerSession.submit(new ContrailAction(identifier, Operation.WRITE) {
				protected void run() throws IOException {
					_cache.store(identifier, item);
					if (lifecycle != null)
						lifecycle.onInsert(identifier);
					_storageSession.store(identifier, serializeTask);

				}
			});
		}

		public void delete(final Identifier path) {
			final Object item= fetch(path).get();
			_trackerSession.submit(new ContrailAction(path, Operation.DELETE) {
				protected void run() throws IOException {
					_cache.delete(path);
					_storageSession.delete(path);
					if (item instanceof ILifecycle) 
						((ILifecycle)item).onDelete();
				}
			});
		}

		public <T extends Serializable> IResult<T> fetch(final Identifier path) 
		{
			ContrailTask<T> task= new ContrailTask<T>(path, Operation.READ) {
				protected void run() throws IOException {
					T storable= (T)_cache.fetch(path);
					if (storable == null) {
						IResult<byte[]> content= _storageSession.fetch(path);
						if (content != null) 
							storable= readStorable(path, content.get());
					}
					setResult(storable);
				}
			};
			_trackerSession.submit(task);
			return task;
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
				protected void run() throws IOException {
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
					setResult(results);
				}
			};
			_trackerSession.submit(task);
			return task;
		}


/*
 * Implementation Note:
 * Caching is not problematic, even though there can be multiple processes writing 
 * to storage, since the higer level Contrail APIs never changes a stored object, 
 * it only writes new versions.  
 * However, while caching stored objects is not a problem, caching lists of children is,
 * since obviously other processes are adding children. 
 */
		public IResult<Collection<Identifier>> listChildren(final Identifier path)
		{
			ContrailTask<Collection<Identifier>> task= new ContrailTask<Collection<Identifier>>(path, Operation.LIST) {
				protected void run() {
					Collection<Identifier> list= _storageSession.listChildren(path).get();
					setResult(list);
				}
			};
			_trackerSession.submit(task);
			return task;
		}
		
		public void flush() throws IOException {
			_trackerSession.awaitCompletion(IOException.class);
			_storageSession.flush();
		}
		
		public void close() throws IOException {
			flush();
			try { _trackerSession.close(); } catch (Throwable t) { Logging.warning(t); }
			try { _storageSession.close(); } catch (Throwable t) {  Logging.warning(t); }
			_trackerSession= null;
			_storageSession= null;
			_outerStorage= null;
		}
		
		public <T extends Serializable> IResult<Boolean> create(final Identifier identifier, final T item, final long waitMillis)
		{
			ContrailTask<Boolean> action= new ContrailTask<Boolean>(identifier, Operation.CREATE) {
				protected void run() throws IOException {

					ExternalizationTask  serializeTask= new ExternalizationTask(item);
					serializeTask.submit();

					boolean created= false;
					if (_storageSession.create(identifier, serializeTask, waitMillis).get()) {
						boolean isStorable= item instanceof ILifecycle;
						if (isStorable)
							((ILifecycle)item).setStorage(_outerStorage);
						_cache.store(identifier, item);
						if (isStorable) 
							((ILifecycle)item).onInsert(identifier);
						created= true;
					}
					setResult(created);
				}
			};
			_trackerSession.submit(action);
			return action;
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
				protected void run() {
					for (Identifier identifier: children.get()) {
						delete(identifier);
					}
				}
			};
			_trackerSession.submit(action);
		}


	}
}
