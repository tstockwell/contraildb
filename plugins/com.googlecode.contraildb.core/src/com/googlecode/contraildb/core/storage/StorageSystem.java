package com.googlecode.contraildb.core.storage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import kilim.Pausable;

import com.googlecode.contraildb.core.ConflictingCommitException;
import com.googlecode.contraildb.core.ContrailException;
import com.googlecode.contraildb.core.Identifier;
import com.googlecode.contraildb.core.IContrailService.Mode;
import com.googlecode.contraildb.core.async.ContrailAction;
import com.googlecode.contraildb.core.async.IResult;
import com.googlecode.contraildb.core.async.Operation;
import com.googlecode.contraildb.core.async.TaskDomain;
import com.googlecode.contraildb.core.async.TaskUtils;
import com.googlecode.contraildb.core.storage.provider.IStorageProvider;
import com.googlecode.contraildb.core.utils.Logging;


/**
 * Implements versioned, transactional, entity storage on top of simple 'raw' storage.
 * Any process that wants to access storage creates an instance of StorageSystem using 
 * a reference to the underlying simple storage system.
 * StorageSystem implements concurrent protocols that enable many processes to 
 * safely access the same underlying simple storage system.  
 * 
 * A StorageSystem system is anchored at a 'root' node in the raw storage system.
 * A StorageSystem creates nodes underneath this root node that contains 
 * metadata about the StorageSystem system, like the available 
 * revisions and so forth.  
 * 
 * A StorageSystem is used by creating a new session, adding and removing objects 
 * from the session and then calling the commit method to commit the changes to 
 * the store.  
 * Other users of a store do not see any changes made by a session to the store 
 * until the session is committed.
 *   
 * StorageSystem uses optimistic concurrency, the StorageSession.commit method may 
 * fail because another process has made potentially conflicting changes.
 * When the commit method throws a ConflictingCommitException then the client should 
 * open a new session and try the transaction again until the transaction succeeds 
 * or throws an exceptions other than ConflictingCommitException.  
 * 
 * When a session instance is opened it may be opened for either reading or writing.
 * When opened for writing a new revision of the store is created.  
 * The new revision is not committed until the close method is called.
 * 
 * When opened for reading the last committed revision of the store is opened for reading.
 * A specific previously committed revision may also be opened as long as the 
 * specified revision has not yet been cleaned up (to keep the performance of the 
 * system from degrading old revisions that are no longer being used must be removed).
 * 
 * Clients *must* call the close method when finished using an instance of ConcurrentStorage.
 * Any uncommitted changes are abandoned when the close method is called.
 * 
 * @see IStorageSPI
 *  
 * @author Ted Stockwell
 */
public class StorageSystem {
	

	public static int LEASE_TIMEOUT= 1000*60*30; // approximately 30 minutes

	EntityStorage _entityStorage;
	IEntityStorage.Session _entitySession;
	RootFolder _root;
	List<StorageSession> _activeSessions= Collections.synchronizedList(new ArrayList<StorageSession>());
	long _lastKnownDeletedRevision= -1;
	List<Long> _knownUncommittedRevisions= Collections.synchronizedList(new ArrayList<Long>());
	TaskDomain _tracker= new TaskDomain();
	TaskDomain.Session _trackerSession= _tracker.beginSession();


	public StorageSystem(final IStorageProvider rawStorage) 
	throws Pausable 
	{
		_entityStorage= new EntityStorage(rawStorage);
		_entitySession= _entityStorage.connect();
		Identifier rootId= Identifier.create("net/sf/contrail/core/storage/rootFolder");
		_root= _entitySession.fetch(rootId);
		if (_root == null) {
			_root= new RootFolder(rootId);
			_entitySession.store(_root);
			_entitySession.flush();
		}
	}
	
	/**
	 * Return the native raw storage used by this storage system.
	 */
	public IStorageProvider getStorageProvider() {
		return _entityStorage.getStorageProvider();
	}


	/**
	 * A session must be created in order to read or write.
	 * Begin either a readonly or readwrite session.
	 * A readonly session is only allowed to get Records, readwrite sessions may also insert, update, and delete Records. 
	 * When a readwrite session is started the session will be associated with a new revision of the database.
	 * When a readonly session is started the session will be associated with the last committed database revision. 
	 */
	public StorageSession beginSession(Mode mode) throws IOException, ContrailException {

		String sessionId= "session."+UUID.randomUUID().toString();
		StorageSession storageSession;
		
		_root.lock(sessionId, true);
		try {
			if (Mode.READONLY == mode) {
				RevisionFolder revision= _root.getLastCommittedRevision();
				revision.addSession(sessionId);
				storageSession= new StorageSession(this, sessionId, revision.revisionNumber, -1, mode);
			}
			else {
				RevisionFolder revision= createNewRevision(sessionId);
				storageSession= new StorageSession(this, sessionId, revision.revisionNumber, revision.startCommitNumber, mode);
			}
		}
		finally {
			_root.unlock(sessionId);
			_root.getStorage().flush();
		}

		Logging.info("Created session "+sessionId);

		_activeSessions.add(storageSession);
		return storageSession;
	}

	/**
	 * Begin a new readonly session associated with the given database revision.
	 */
	public StorageSession beginSession(long revisionNumber) 
	throws ContrailException, IOException 
	{
		String sessionId= "session."+UUID.randomUUID().toString();
		StorageSession  storageSession= null;
		// NOTE: it is not possible to lock an uncommitted revision 
		// since the revision remains locked until its committed  
		while (storageSession == null) {
			RevisionFolder revision= (RevisionFolder) 
				_entitySession.fetch(RevisionFolder.createId(_root, revisionNumber));
			if (revision == null)
				throw new IOException("Revision does not exist: "+ revisionNumber);
			revision.lock(sessionId, true);
			try {
				revision.addSession(sessionId);
				storageSession= new StorageSession(this, sessionId, revisionNumber, -1, Mode.READONLY);
			}
			finally {
				revision.unlock(sessionId);
			}
		}
		_entitySession.flush();
		_activeSessions.add(storageSession);
		return storageSession;
	}
	
	/**
	 * Cleans up old file revisions that are no longer needed.
	 * Also cleans up metadata that is no longer needed, like revisions that are no longer active.
	 * @throws IOException
	 */
	public void cleanup() throws IOException {
		_trackerSession.submit(new StorageCleanupAction(this));
	}

	
	private RevisionFolder createNewRevision(String sessionId) throws IOException {

		List<RevisionFolder> revisions= _root.getRevisionFolders();
		RevisionFolder lastRevision= revisions.get(0);
		RevisionFolder startRevision= null;
		for (RevisionFolder revision: revisions) {
			if (revision.isCommitted()) 
				if (startRevision == null || startRevision.getFinalCommitNumber() < revision.getFinalCommitNumber())  
					startRevision= revision;
		}
		RevisionFolder revision= new RevisionFolder(_root, lastRevision.revisionNumber+1, startRevision.revisionNumber);
		_entitySession.store(revision).getb();

		/*
		 * Mark the new revision AND the starting revision as in use by this session.
		 * Adding this session to the starting revision prevents the starting revision 
		 * from being cleaned up until we're done with it.  
		 */ 
		revision.addSession(sessionId);
		startRevision.addSession(sessionId);

		_knownUncommittedRevisions.add(revision.revisionNumber);

		return revision;
	}
	
	void closeStorageSession(StorageSession storageSession) 
	throws IOException 
	{
		try {
			// remove the session reference from the session's starting revision
			long startingCommitNumber= storageSession.getStartingCommitNumber();
			if (0 <= startingCommitNumber) {
				RevisionFolder startRevision= _root.getRevisionFolder(startingCommitNumber);
				assert startRevision != null : "Revision "+startingCommitNumber+" has apparently been deleted when it should have been leased by a session associated with revision "+storageSession.getRevisionNumber();
				startRevision.removeSession(storageSession.getSessionId());
			}
			
			long revisionNumber= storageSession.getRevisionNumber();
			RevisionFolder revisionFolder= _root.getRevisionFolder(revisionNumber);
			revisionFolder.removeSession(storageSession.getSessionId());
		}
		finally {
			Logging.fine("Closed session "+storageSession);
			_activeSessions.remove(storageSession);
		}
// TODO cleanup/release expired sessions that the application did not close 		
	}


	void commitRevision(StorageSession storageSession) 
	throws IOException, ConflictingCommitException 
	{
		String sessionId= storageSession.getSessionId();
		long revisionNumber= storageSession.getRevisionNumber();
		final long startingCommit= storageSession.getStartingCommitNumber();
		RevisionFolder revision= _root.getRevisionFolder(revisionNumber);
		RevisionFolder startRevision= _root.getRevisionFolder(startingCommit);
		
		// write journal
		final RevisionJournal journal= new RevisionJournal(revision, storageSession);
		_entitySession.store(journal).getb();
		
		// lock root, no other revisions may be committed while root is locked.
		_root.lock(sessionId, true);
		try {
			
			List<RevisionFolder> revisions= _root.getRevisionFolders();
			RevisionFolder.sortByDescendingCommitNumber(revisions);
			long lastCommitNumber= revisions.get(0).getFinalCommitNumber();
			if (lastCommitNumber < 0) {
				String msg= "Corrupted Storage: There do not appear to be any committed revisions.";
				msg+= "\nAvailable revisions: {";
				for (RevisionFolder folder: revisions)
					msg+= "\n"+folder;
				msg+= "\n}";
				Logging.severe(msg);
				throw new ContrailException(msg);
			}
			
			// validate transaction
			ArrayList<IResult<Void>> validateActions= new ArrayList<IResult<Void>>(revisions.size());
			final List<RevisionFolder> conflictingRevisions= Collections.synchronizedList(new ArrayList<RevisionFolder>(1));  
			for (final RevisionFolder r: revisions) {
				validateActions.add(new ContrailAction() {
					protected void action() throws IOException {
						if (r.getFinalCommitNumber() <= startingCommit)
							return; // we're done
						if (!conflictingRevisions.isEmpty())
							return;
						
						if (r.getRevisionJournal().confictsWith(journal))
							conflictingRevisions.add(r);
					}
				}.submit());
			}
			TaskUtils.getAll(validateActions, IOException.class);
			if (!conflictingRevisions.isEmpty())
				throw new ConflictingCommitException();
			
			// write commit marker
			_entitySession.store(new CommitMarker(revision, lastCommitNumber+1)).getb();

			// unlock revision and remove session id from start revision
			revision.removeSession(sessionId);
			startRevision.removeSession(sessionId);
			
			_entitySession.flush();
			
			_knownUncommittedRevisions.remove(revisionNumber);
			
		}
		finally {
			
			_activeSessions.remove(storageSession);
			
			try { _root.unlock(sessionId); } catch (Throwable t) { }
			
			try { Logging.info("Committed session "+storageSession); } catch (Throwable t) { }
			
			try { _trackerSession.submit(new StorageCleanupAction(this)); } catch (Throwable t) { }
		}
	}

	public boolean isRevisionCommitted(long revisionNumber) 
	throws IOException 
	{
		if (revisionNumber <= _lastKnownDeletedRevision)
			return true;
		if (_knownUncommittedRevisions.contains(revisionNumber))
			return false;
		RevisionFolder folder= _root.getRevisionFolder(revisionNumber);
		if (folder == null) { 
			/*
			 * Revision was committed and revision info has already been cleaned up.
			 * Since a revision cannot be cleaned until all previous revisions were 
			 * cleaned we can ignore any revisions prior to this one (which saves us a 
			 * boatload of file lookups).  
			 */
			updateLastKnownDeletedRevision(revisionNumber);
			return true;
		}
		return folder.isCommitted();
	}

	void updateLastKnownDeletedRevision(long revisionNumber) {
		_lastKnownDeletedRevision= revisionNumber;
		for (Iterator<Long>i= _knownUncommittedRevisions.iterator(); i.hasNext();) {
			if (i.next() <= revisionNumber)
				i.remove();
		}
	}

	public List<Long> getAvailableRevisions() throws IOException {
		List<RevisionFolder> folders= _root.getRevisionFolders();
		ArrayList<Long> revisions= new ArrayList<Long>();
		for (RevisionFolder folder: folders)
			revisions.add(folder.revisionNumber);
		return revisions;
	}
	
	public void close() throws IOException {
		
		// close all open sessions
		while (!_activeSessions.isEmpty()) {
			final StorageSession session= _activeSessions.remove(0);
			_trackerSession.submit(new ContrailAction(Identifier.create(session.getSessionId()), Operation.DELETE) {
				protected void action() throws IOException {
					session.close();
				}
			});
		}
		
		// wait for all sessions to close and for any cleanup tasks in progress
		_trackerSession.complete().join();
	}

}
