package com.googlecode.contraildb.core.storage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import com.googlecode.contraildb.core.ConflictingCommitException;
import com.googlecode.contraildb.core.ContrailException;
import com.googlecode.contraildb.core.Identifier;
import com.googlecode.contraildb.core.IContrailService.Mode;
import com.googlecode.contraildb.core.impl.PathUtils;
import com.googlecode.contraildb.core.utils.ContrailTask;
import com.googlecode.contraildb.core.utils.ContrailTaskTracker;
import com.googlecode.contraildb.core.utils.IResult;
import com.googlecode.contraildb.core.utils.Logging;
import com.googlecode.contraildb.core.utils.TaskUtils;


/**
 * A storage session is like an EntityStroage.Session except it implements 
 * versioning on top of the EntityStorage API.
 * 
 * Either the commit method or the close method MUST be called by the client 
 * when done using a session.
 *  
 * @author ted stockwell
 */
public class StorageSession implements IEntityStorage.Session {
	
	private static final String CONTRAIL_FOLDER = ".contrail";
	private final StorageSystem _storageSystem;
	private final String _sessionId;
	private final long _revisionNumber;
	private final long _startingCommitNumber;
	private final Mode _mode;
	private final ObjectStorage.Session _storage;
	private final ContrailTaskTracker.Session _trackerSession;
	
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
	
	public StorageSession(StorageSystem storageSystem, String sessionId, long revisionNumber, long startingCommitNumber, Mode mode) 
	throws IOException 
	{
		/*
		 * Ok, I guess the next line requires some explanation (and of course if it requires so much explanation then 
		 * it's not good code, but I'm not ready to clean this up)...
		 * I need an ObjectStore session (cause this class needs to save entities under an ID other than the Entitie's ID) 
		 * but I also want all StorageSessions from the same StorageService to share the same cache.
		 * So... here I create an entity session from the StorageService's EntityStore and then use the EnitySession's 
		 * embedded ObjectStore.Session. 
		 */
		_storage= ((EntityStorage.Session)((EntityStorage)storageSystem._entityStorage).connect())._objectSession;
		_sessionId= sessionId;
		_storageSystem= storageSystem;
		_revisionNumber= revisionNumber;
		_mode= mode;
		_startingCommitNumber= startingCommitNumber;
		_trackerSession= storageSystem._tracker.beginSession();
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
	 * @throws ConflictingCommitException when a potentially conficting change has been comitted to the database by another process.
	 * When this exception is throw the caller should open a new session and retry the transaction. 
	 * @throws IOException 
	 */
	public void commit() throws ConflictingCommitException, IOException {
		try {
			_storage.flush();
		}
		finally {
			try {
				_storageSystem.commitRevision(this);
			}
			finally {
				_isActive= false;
				_deletes= _reads= _inserts= _updates= null;
			}
		}
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
	synchronized public void close() throws IOException {
		if (!_isActive)
			return;
		try {
			flush();
		}
		finally {
			try {
				_storage.close(); 
			} 
			finally {
				try {
					_storageSystem.closeStorageSession(this);
				}
				finally {
					try { _trackerSession.close(); } catch (Throwable t) { Logging.warning(t); }
					_isActive= false;
				}
			}
		}
	}


	public <E extends IEntity> void store(E entity) {
		if (_mode == Mode.READONLY)
			throw new ContrailException("Session is read only: "+_revisionNumber);
		
		// we just insert a holder for the item and then insert revisions as children of the CONTRAIL_FOLDER folder
		Identifier originalPath= entity.getId();
		_storage.store(originalPath, new Entity(originalPath));
		
		Identifier contrailFolder= Identifier.create(originalPath, CONTRAIL_FOLDER);
		_storage.store(contrailFolder, new Entity(contrailFolder));
		
		// we then insert revisions as children
		Identifier revisionPath= Identifier.create(contrailFolder, "store-"+_revisionNumber);
		_storage.store(revisionPath, entity);
	}
	
	public void update(Identifier path, IEntity item) throws IOException {
		if (_mode == Mode.READONLY)
			throw new ContrailException("Revision is read only: "+_revisionNumber);

		// we just insert a holder for the item and then insert revisions as children of the CONTRAIL_FOLDER folder
		Identifier originalPath= item.getId();
		Identifier contrailFolder= Identifier.create(originalPath, CONTRAIL_FOLDER);
		Identifier revisionPath= Identifier.create(contrailFolder, "store-"+_revisionNumber);
		_storage.store(revisionPath, item);
	}
	
	public void deleteAllChildren(Collection<Identifier> paths) throws IOException {
		if (_mode == Mode.READONLY)
			throw new ContrailException("Revision is read only: "+_revisionNumber);
		
		ArrayList<IResult<Collection<Identifier>>> childrens= new ArrayList<IResult<Collection<Identifier>>>(); 
		for (Identifier path: paths) 
			childrens.add(listChildren(path));
		for (IResult<Collection<Identifier>> result: childrens) {
			Collection<Identifier> children= result.get();
			for (Identifier child: children)
				delete(child);
		}
	}

	public void delete(Identifier  path) {
		if (_mode == Mode.READONLY)
			throw new ContrailException("Revision is read only: "+_revisionNumber);

		Identifier contrailFolder= Identifier.create(path, CONTRAIL_FOLDER);
		Identifier revisionPath= Identifier.create(contrailFolder, "delete-"+_revisionNumber);
		_storage.store(revisionPath, new Entity(revisionPath));
	}


	public <C extends IEntity> IResult<C> fetch(final Identifier path) {
		return new ContrailTask<C>() {
			@SuppressWarnings("unchecked")
			protected C run() throws IOException {
				Identifier contrailPath= Identifier.create(path, CONTRAIL_FOLDER);
				IResult<Collection<Identifier>> childrenResult= _storage.listChildren(contrailPath);
				Collection<Identifier> children= childrenResult.get();
				Identifier mostRecentRevision= null;
				long maxRevision= -1;
				for (Identifier child: children) {
					long revisionNumber= PathUtils.getRevisionNumber(child);
					if (revisionNumber < 0)
						continue;
					if (_revisionNumber < revisionNumber)
						continue;
					if (revisionNumber != _revisionNumber)
						if (!_storageSystem.isRevisionCommitted(revisionNumber))
							continue;
					if (maxRevision < revisionNumber) {
						maxRevision= revisionNumber;
						mostRecentRevision= child;
					}
				}
				C c= null;
				if (mostRecentRevision != null)
					if (!mostRecentRevision.getName().startsWith("delete-")) 
						c= (C)_storage.fetch(mostRecentRevision).get();
				return c;
			}
		}.submit();
	}


	public IResult<Collection<Identifier>> listChildren(final Identifier path)
	{
		return new ContrailTask<Collection<Identifier>>() {
			protected Collection<Identifier> run() throws IOException {
				
				ArrayList<Identifier> children= _listChildren(path);
				ArrayList<Identifier> results = new ArrayList<Identifier>();
				for (Identifier identifier:children)
					results.add(identifier.getParent());
				
				return results;
			}
		}.submit();
	}

	private ArrayList<Identifier> _listChildren(final Identifier path) {
		ArrayList<Identifier> results= new ArrayList<Identifier>();
		Collection<Identifier> children= _storage.listChildren(path).get();
		
		Map<Identifier, IResult<Collection<Identifier>>> contrailChildrens= new HashMap<Identifier, IResult<Collection<Identifier>>>();
		for (Identifier child:children) {
			if (CONTRAIL_FOLDER.equals(child.getName())) 
				continue;
			Identifier contrailFolder= Identifier.create(child, CONTRAIL_FOLDER);
			contrailChildrens.put(contrailFolder, _storage.listChildren(contrailFolder));
		}
		
		for (Identifier contrailFolder: contrailChildrens.keySet()) {
			Collection<Identifier> contrailChildren= contrailChildrens.get(contrailFolder).get();
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
		}
		return results;
	}


	public <C extends IEntity> IResult<Collection<C>> fetchChildren(final Identifier path)
	{
		return new ContrailTask<Collection<C>>() {
			@SuppressWarnings("unchecked")
			protected Collection<C> run() throws IOException {
				
				ArrayList<Identifier> children= _listChildren(path);
				ArrayList<C> results = new ArrayList<C>();
				for (Identifier identifier:children) {
					results.add((C)_storage.fetch(identifier));
				}
				return results;
			}
		}.submit();
	}

	public void flush() throws IOException {
		TaskUtils.get(_trackerSession.complete(), IOException.class);
		_storage.flush();
	}

	@Override
	public <E extends IEntity> IResult<Boolean> create(E entity, long waitMillis) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void deleteAllChildren(Identifier path) {
		if (_mode == Mode.READONLY)
			throw new ContrailException("Revision is read only: "+_revisionNumber);
		
		IResult<Collection<Identifier>> children= listChildren(path);
		for (Identifier child: children.get())
			delete(child);
	}
}
