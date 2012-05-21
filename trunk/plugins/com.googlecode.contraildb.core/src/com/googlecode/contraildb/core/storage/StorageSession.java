package com.googlecode.contraildb.core.storage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.googlecode.contraildb.core.ConflictingCommitException;
import com.googlecode.contraildb.core.ContrailException;
import com.googlecode.contraildb.core.IContrailService.Mode;
import com.googlecode.contraildb.core.IResult;
import com.googlecode.contraildb.core.Identifier;
import com.googlecode.contraildb.core.impl.PathUtils;
import com.googlecode.contraildb.core.storage.ObjectStorage.Session;
import com.googlecode.contraildb.core.utils.ForEachHandler;
import com.googlecode.contraildb.core.utils.Handler;
import com.googlecode.contraildb.core.utils.TaskUtils;


/**
 * A storage session is like an EntityStorage.Session except it implements 
 * versioning on top of the EntityStorage API.
 * 
 * Either the commit method or the close method MUST be called by the client 
 * when done using a session.
 *  
 * @author ted stockwell
 */
@SuppressWarnings({"unchecked","rawtypes"})
public class StorageSession implements IEntityStorage.Session {
	
	private static final String CONTRAIL_FOLDER = ".contrail";
	private final StorageSystem _storageSystem;
	private final String _sessionId;
	private final long _revisionNumber;
	private final long _startingCommitNumber;
	private final Mode _mode;
	private final ObjectStorage.Session _storage;
	
	HashSet<Identifier> _reads= new HashSet<Identifier>();
	HashSet<Identifier> _inserts= new HashSet<Identifier>();
	HashSet<Identifier> _updates= new HashSet<Identifier>();
	HashSet<Identifier> _deletes= new HashSet<Identifier>();
	
	/**
	 * Comparator that sorts revision items in descending order
	 */
	private Comparator<? super Identifier> __revisionComparator= new Comparator<Identifier>() {
		public int compare(Identifier o1, Identifier o2) {
			long n1= PathUtils.getRevisionNumber(o1);
			long n2= PathUtils.getRevisionNumber(o2);
			if (n1 < n2)
				return 1;
			if (n1 > n2)
				return -1;
			return 0;
		}
	};
	private boolean _isActive= true;
	
	public static IResult<StorageSession> create(final StorageSystem storageSystem, final String sessionId, final long revisionNumber, final long startingCommitNumber, final Mode mode) 
	{
		/*
		 * Ok, I guess the next line requires some explanation (and of course if it requires so much explanation then 
		 * it's not good code, but I'm not ready to clean this up)...
		 * I need an ObjectStore session (cause this class needs to save entities under an ID other than the Entity's ID) 
		 * but I also want all StorageSessions from the same StorageService to share the same cache.
		 * So... here I create an entity session from the StorageService's EntityStore and then use the EntitySession's 
		 * embedded ObjectStore.Session. 
		 */
		return new Handler(((EntityStorage)storageSystem._entityStorage).connect()) {
			protected IResult onSuccess() {
				ObjectStorage.Session session= (Session) incoming().getResult();
				return asResult(new StorageSession(session, storageSystem, sessionId, revisionNumber, startingCommitNumber, mode));
			}
			
		};
	}
	private StorageSession(ObjectStorage.Session session, StorageSystem storageSystem, String sessionId, long revisionNumber, long startingCommitNumber, Mode mode) 
	{
		/*
		 * Ok, I guess the next line requires some explanation (and of course if it requires so much explanation then 
		 * it's not good code, but I'm not ready to clean this up)...
		 * I need an ObjectStore session (cause this class needs to save entities under an ID other than the Entity's ID) 
		 * but I also want all StorageSessions from the same StorageService to share the same cache.
		 * So... here I create an entity session from the StorageService's EntityStore and then use the EntitySession's 
		 * embedded ObjectStore.Session. 
		 */
		_storage= session;
		_sessionId= sessionId;
		_storageSystem= storageSystem;
		_revisionNumber= revisionNumber;
		_mode= mode;
		_startingCommitNumber= startingCommitNumber;
	}
	
	@Override
	public String toString() {
		return _sessionId+" { mode="+_mode+", revision="+_revisionNumber+", start_revision="+_startingCommitNumber+"}";
	}

	String getSessionId() {
		return _sessionId;
	}
	
	/**
	 * Returns the number of the revision associated with this session.
	 */
	public long getRevisionNumber() {
		return _revisionNumber;
	}
	
	public long getStartingCommitNumber() {
		return _startingCommitNumber;
	}
	
	public boolean isActive() {
		return _isActive;
	}
	

	/**
	 * Commits all changes and closes the session
	 * A client *MUST* call this method or the close method when finished with a session
	 * No other methods may be invoked after invoking this method (except the 
	 * close method, but calling the close method is not necessary after calling this method).
	 * 
	 * @throws ConflictingCommitException when a potentially conflicting change has been committed to the database by another process.
	 * When this exception is throw the caller should open a new session and retry the transaction. 
	 * @throws IOException 
	 */
	public IResult<Void> commit() { 
		return new Handler(_storage.flush()) {
			protected IResult onSuccess() {
				spawn(new Handler(_storageSystem.commitRevision(StorageSession.this)) {
					protected void onComplete() throws Exception {
						_isActive= false;
						_deletes= _reads= _inserts= _updates= null;
					}
				});
				return TaskUtils.DONE;
			};
		};
	}

	/**
	 * Abandons any uncommitted changes and closes the session
	 * A client *MUST* call this method or the commit method when finished with a transaction
	 * No other methods may be invoked after invoking this method.
	 * @throws IOException 
	 * 
	 * @throws ConflictingCommitException when a potentially conflicting change has been committed to the database by another process.
	 * When this exception is throw the caller should open a new session and retry the transaction. 
	 */
	synchronized public IResult<Void> close() {
		return new Handler(_isActive ? flush() : TaskUtils.DONE) {
			protected void onComplete() throws Exception {
				spawn(new Handler(_storage.close()) {
					protected void onComplete() throws Exception {
						spawn(_storageSystem.closeStorageSession(StorageSession.this));
						_isActive= false;
					}
				}.toResult());
			}
		}.toResult();
	}


	public <E extends IEntity> IResult<Void> store(final E entity) {
		return new Handler() {
			protected IResult onSuccess() throws Exception {
				if (_mode == Mode.READONLY)
					throw new ContrailException("Session is read only: "+_revisionNumber);
				
				// we just insert a holder for the item and then insert revisions as children of the CONTRAIL_FOLDER folder
				Identifier originalPath= entity.getId();
				spawn(_storage.store(originalPath, new Entity(originalPath)));
				
				Identifier contrailFolder= Identifier.create(originalPath, CONTRAIL_FOLDER);
				spawn(_storage.store(contrailFolder, new Entity(contrailFolder)));
				
				// we then insert revisions as children
				Identifier revisionPath= Identifier.create(contrailFolder, "store-"+_revisionNumber);
				spawn(_storage.store(revisionPath, entity));
				
				return TaskUtils.DONE;
			}
		};
	}
	
	public IResult<Void> update(final Identifier path, final IEntity item) throws IOException {
		return new Handler() {
			protected IResult onSuccess() throws Exception {
				if (_mode == Mode.READONLY)
					throw new ContrailException("Revision is read only: "+_revisionNumber);

				// we just insert a holder for the item and then insert revisions as children of the CONTRAIL_FOLDER folder
				Identifier originalPath= item.getId();
				Identifier contrailFolder= Identifier.create(originalPath, CONTRAIL_FOLDER);
				Identifier revisionPath= Identifier.create(contrailFolder, "store-"+_revisionNumber);
				spawn(_storage.store(revisionPath, item));
				
				return TaskUtils.DONE;
			}
		};
	}
	
	public IResult<Void> deleteAllChildren(final Collection<Identifier> paths) throws IOException {
		return new Handler() {
			protected IResult onSuccess() throws Exception {
				if (_mode == Mode.READONLY)
					throw new ContrailException("Revision is read only: "+_revisionNumber);
				
				final ArrayList<IResult<Collection<Identifier>>> childrens= new ArrayList<IResult<Collection<Identifier>>>(); 
				for (Identifier path: paths) 
					childrens.add(listChildren(path));
				spawn(new Handler(TaskUtils.combineResults(childrens)) {
					protected IResult onSuccess() throws Exception {
						for (IResult<Collection<Identifier>> result: childrens) {
							Collection<Identifier> children= result.getResult();
							for (Identifier child: children)
								spawn(delete(child));
						}
						return TaskUtils.DONE;
					};
				});
				
				return TaskUtils.DONE;
			}
		};
	}

	public IResult<Void> delete(final Identifier  path) {
		return new Handler() {
			protected IResult onSuccess() throws Exception {
				if (_mode == Mode.READONLY)
					throw new ContrailException("Revision is read only: "+_revisionNumber);

				Identifier contrailFolder= Identifier.create(path, CONTRAIL_FOLDER);
				Identifier revisionPath= Identifier.create(contrailFolder, "delete-"+_revisionNumber);
				spawn(_storage.store(revisionPath, new Entity(revisionPath)));
				return TaskUtils.SUCCESS;
			};
		};
	}


	public <C extends IEntity> IResult<C> fetch(final Identifier path) {
		final Identifier contrailPath= Identifier.create(path, CONTRAIL_FOLDER);
		return new Handler(_storage.listChildren(contrailPath)) {
			protected IResult onSuccess() {
				Collection<Identifier> children= (Collection<Identifier>) incoming().getResult();
				final Identifier[] mostRecentRevision= new Identifier[] { null };
				final long[] maxRevision= new long[] { -1 };
				IResult findMostRecentRevision= new ForEachHandler<Identifier>(children) {
					protected IResult<Void> Do(final Identifier child) throws Exception {
						final long revisionNumber= PathUtils.getRevisionNumber(child);
						if (revisionNumber < 0)
							return TaskUtils.DONE;
						if (_revisionNumber < revisionNumber)
							return TaskUtils.DONE;
						
						IResult<Boolean> committed= TaskUtils.TRUE;
						if (revisionNumber != _revisionNumber)
							committed= _storageSystem.isRevisionCommitted(revisionNumber);
						
						return new Handler(committed) {
							protected IResult onSuccess() throws Exception {
								boolean revisionIsCommitted= (Boolean) incoming().getResult();
								if (revisionIsCommitted) {
									if (maxRevision[0] < revisionNumber) {
										maxRevision[0]= revisionNumber;
										mostRecentRevision[0]= child;
									}
								}
								return TaskUtils.DONE;
							}
						};
					}
				};
				return new Handler(findMostRecentRevision) {
					protected IResult onSuccess() throws Exception {
						if (mostRecentRevision[0] == null)
							return TaskUtils.NULL;
						if (mostRecentRevision[0].getName().startsWith("delete-")) 
							return TaskUtils.NULL;
						return _storage.fetch(mostRecentRevision[0]);
					}
				};
			}
		};
	}


	public IResult<Collection<Identifier>> listChildren(final Identifier path)
	{
		return new Handler(_listChildren(path)) {
			protected IResult onSuccess() throws Exception {
				ArrayList<Identifier> children= (ArrayList<Identifier>) incoming().getResult();
				ArrayList<Identifier> results = new ArrayList<Identifier>();
				for (Identifier identifier:children)
					results.add(identifier.getParent());
				return asResult(results);
			}
		};
	}

	private IResult<ArrayList<Identifier>> _listChildren(final Identifier path) {
		return new Handler(_storage.listChildren(path)) {
			protected IResult onSuccess() throws Exception {
				Collection<Identifier> children= (Collection<Identifier>) incoming().getResult();
				final List<Identifier> results= Collections.synchronizedList(new ArrayList<Identifier>());
				Map<Identifier, IResult<Collection<Identifier>>> contrailChildrens= new HashMap<Identifier, IResult<Collection<Identifier>>>();
				for (Identifier child:children) {
					if (CONTRAIL_FOLDER.equals(child.getName())) 
						continue;
					Identifier contrailFolder= Identifier.create(child, CONTRAIL_FOLDER);
					contrailChildrens.put(contrailFolder, _storage.listChildren(contrailFolder));
				}
				
				ArrayList<IResult> tasks= new ArrayList<IResult>();
				for (final Identifier contrailFolder: contrailChildrens.keySet()) {
					tasks.add(new Handler(contrailChildrens.get(contrailFolder)) {
						protected IResult onSuccess() throws Exception {
							Collection<Identifier> contrailChildren= (Collection<Identifier>) incoming().getResult();
							ArrayList<Identifier> revisions= new ArrayList<Identifier>(contrailChildren);
							Collections.sort(revisions, __revisionComparator);
							Identifier revision= null;
							for (Identifier child: revisions) {
								long number= PathUtils.getRevisionNumber(child);
								if (number < 0)
									continue;
								if (child.getName().startsWith("deleted-"))
									break;
								if (number <= _revisionNumber) {
									revision= child;
									break;
								}
							}
							if (revision != null) 
								results.add(contrailFolder);
							return TaskUtils.DONE;
						}
					});
				}
				
				return new Handler(tasks) {
					protected IResult onSuccess() throws Exception {
						return asResult(results);
					}
				};
			}
		};
	}


	public <C extends IEntity> IResult<Collection<C>> fetchChildren(final Identifier path)
	{
		return new Handler(_listChildren(path)) {
			protected IResult onSuccess() throws Exception {
				ArrayList<Identifier> children= (ArrayList<Identifier>) incoming().getResult();
				final List<C> results = Collections.synchronizedList(new ArrayList<C>());
				for (final Identifier identifier:children) {
					spawn(new Handler(_storage.fetch(identifier)) {
						protected IResult onSuccess() throws Exception {
							results.add((C)_storage.fetch(identifier));
							return TaskUtils.DONE;
						}
					});
				}
				return asResult(results);
			}
		};
	}

	public IResult<Void> flush()  {
		return _storage.flush();
	}

	@Override
	public <E extends IEntity> IResult<Boolean> create(E entity, long waitMillis) {
		return new Handler() {
			protected IResult onSuccess() {
				throw new UnsupportedOperationException();
			}
		};
	}

	@Override
	public IResult<Void> deleteAllChildren(final Identifier path) {
		return new Handler() {
			protected IResult onSuccess() throws Exception {
				if (_mode == Mode.READONLY)
					throw new ContrailException("Revision is read only: "+_revisionNumber);
				
				final IResult<Collection<Identifier>> children= listChildren(path);
				spawn(new Handler(children) {
					protected IResult onSuccess() throws Exception {
						for (Identifier child: children.getResult())
							spawn(delete(child));
						return TaskUtils.SUCCESS;
					}
				});
				return TaskUtils.SUCCESS;
			};
		};
	}
}
