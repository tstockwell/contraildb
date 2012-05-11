package com.googlecode.contraildb.core.storage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import com.googlecode.contraildb.core.ConflictingCommitException;
import com.googlecode.contraildb.core.ContrailException;
import com.googlecode.contraildb.core.IResult;
import com.googlecode.contraildb.core.Identifier;
import com.googlecode.contraildb.core.IContrailService.Mode;
import com.googlecode.contraildb.core.storage.provider.IStorageProvider;
import com.googlecode.contraildb.core.utils.ContrailAction;
import com.googlecode.contraildb.core.utils.ContrailTaskTracker;
import com.googlecode.contraildb.core.utils.InvocationAction;
import com.googlecode.contraildb.core.utils.Logging;
import com.googlecode.contraildb.core.utils.Handler;
import com.googlecode.contraildb.core.utils.TaskUtils;
import com.googlecode.contraildb.core.utils.ContrailTask.Operation;


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
@SuppressWarnings({"rawtypes","unchecked"})
public class StorageSystem {
	

	public static int LEASE_TIMEOUT= 1000*60*30; // approximately 30 minutes

	EntityStorage _entityStorage;
	IEntityStorage.Session _entitySession;
	RootFolder _root;
	List<StorageSession> _activeSessions= Collections.synchronizedList(new ArrayList<StorageSession>());
	long _lastKnownDeletedRevision= -1;
	List<Long> _knownUncommittedRevisions= Collections.synchronizedList(new ArrayList<Long>());
	ContrailTaskTracker _tracker= new ContrailTaskTracker();
	ContrailTaskTracker.Session _trackerSession= _tracker.beginSession();

	public static IResult<StorageSystem> create(IStorageProvider rawStorage) 
	{
		final StorageSystem storageSystem= new StorageSystem();
		storageSystem._entityStorage= new EntityStorage(rawStorage);
		final IResult<IEntityStorage.Session> entityStorageConnect= storageSystem._entityStorage.connect();
		return new Handler(entityStorageConnect) {
			protected IResult onSuccess() throws Exception {
				storageSystem._entitySession= entityStorageConnect.getResult();
				final Identifier rootId= Identifier.create("net/sf/contrail/core/storage/rootFolder");
				final IResult<RootFolder> fetchRoot= storageSystem._entitySession.fetch(rootId);
				spawn(new Handler(fetchRoot) {
					protected IResult onSuccess() throws Exception {
						storageSystem._root= fetchRoot.getResult();
						if (storageSystem._root == null) {
							spawn(storageSystem._entitySession.store(storageSystem._root= new RootFolder(rootId)));
							spawn(storageSystem._entitySession.flush());
						}
						return TaskUtils.DONE;
					};
				});
				
				return asResult(storageSystem);
			};
		};
	}
	
	private StorageSystem() {
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
	public IResult<StorageSession> beginSession(final Mode mode) {

		final String sessionId= "session."+UUID.randomUUID().toString();
		final StorageSession[] storageSession= new StorageSession[] { null };
		
		IResult beginSession= new Handler(_root.lock(sessionId, true)) {
			protected IResult onSuccess() throws Exception {
				if (Mode.READONLY == mode) {
					final IResult<RevisionFolder> getLastCommittedRevision= _root.getLastCommittedRevision();
					spawn(new Handler(getLastCommittedRevision) {
						protected IResult onSuccess() throws Exception {
							final RevisionFolder revision= getLastCommittedRevision.getResult();
							final IResult<StorageSession> createStorageSession= StorageSession.create(this, sessionId, revision.revisionNumber, -1, mode);
							spawn(new Handler(createStorageSession) {
								protected IResult onSuccess() throws Exception {
									return TaskUtils.asResult(storageSession[0]= createStorageSession.getResult());
								}
							});
						}
					});
				}
				else {
					final IResult<RevisionFolder> createNewRevision= createNewRevision(sessionId);;
					spawn(new Handler(createNewRevision) {
						protected IResult onSuccess() throws Exception {
							final RevisionFolder revision= createNewRevision.getResult();
							final IResult<StorageSession> createStorageSession= StorageSession.create(this, sessionId, revision.revisionNumber, revision.startCommitNumber, mode);
							spawn(new Handler(createStorageSession) {
								protected IResult onSuccess() throws Exception {
									return TaskUtils.asResult(storageSession[0]= createStorageSession.getResult());
								}
							});
						}
					});
				}
				return TaskUtils.asResult(storageSession[0]);
			}
		}.toResult();
		
		return new Handler(beginSession) {
			protected void onComplete() throws Exception {
				spawn(_root.unlock(sessionId));
				spawn(_root.getStorage().flush());
			}
			protected IResult onSuccess() throws Exception {
				Logging.info("Created session "+sessionId);

				_activeSessions.add(storageSession[0]);
				return TaskUtils.asResult(storageSession[0]);
			}
		}.toResult();
	}

	/**
	 * Begin a new readonly session associated with the given database revision.
	 */
	public IResult<StorageSession> beginSession(final long revisionNumber) 
	{
		final String sessionId= "session."+UUID.randomUUID().toString();
		class state { 
			RevisionFolder revision;
			StorageSession storageSession;
		}
		final state local= new state();
		final IResult<RevisionFolder> revisionFolder= _entitySession.fetch(RevisionFolder.createId(_root, revisionNumber));
		IResult<Void> lockRevision= new Handler(revisionFolder) {
			protected IResult onSuccess() throws Exception {
				local.revision= revisionFolder.getResult();
				if (local.revision == null)
					throw new IOException("Revision does not exist: "+ revisionNumber);
				spawn(local.revision.lock(sessionId, true/*waitForLock*/));
				return TaskUtils.DONE;
			}
		}.toResult();
		IResult<Void> addSession= new Handler(lockRevision) {
			protected IResult onSuccess() throws Exception {
				spawn(new Handler(local.revision.addSession(sessionId)) {
					protected IResult onSuccess() throws Exception {
						spawn(new Handler(StorageSession.create(this, sessionId, revisionNumber, -1, Mode.READONLY)) {
							protected IResult onSuccess() throws Exception {
								local.storageSession= (StorageSession) incoming().getResult();
							}
						});
					}
				});
				return TaskUtils.DONE;
			}
		};
		return new Handler(addSession) {
			protected void onComplete() throws Exception {
				RevisionFolder revision= revisionFolder.getResult();
				spawn(new Handler(revision.unlock(sessionId)) {
					protected void onComplete() throws Exception {
						_entitySession.flush();
						spawn(new Handler(_entitySession.flush()) {
							protected void onComplete() throws Exception {
								_activeSessions.add(local.storageSession);
							}
						});
					}
				});
			}
			protected IResult onSuccess() throws Exception {
				return asResult(local.storageSession);
			}
		};
	}
	
	/**
	 * Cleans up old file revisions that are no longer needed.
	 * Also cleans up metadata that is no longer needed, like revisions that are no longer active.
	 * @throws IOException
	 */
	public IResult<Void> cleanup() throws IOException {
		return StorageCleanupAction.cleanup(this);
	}

	
	private IResult<RevisionFolder> createNewRevision(final String sessionId) throws IOException {

		class state {
			List<RevisionFolder> revisions;
			RevisionFolder lastRevision;
			RevisionFolder startRevision;
		}
		final state local= new state();
		IResult getRevisionFolders= new Handler(_root.getRevisionFolders()) {
			protected IResult onSuccess() throws Exception {
				local.revisions= (List<RevisionFolder>) incoming().getResult();
				local.lastRevision= local.revisions.get(0);
				local.startRevision= null;
				for (final RevisionFolder revision: local.revisions) {
					spawn(new Handler(revision.isCommitted()) {
						protected IResult onSuccess() throws Exception {
							if ((Boolean) incoming().get()) {
								synchronized (local) {
									if (local.startRevision == null || local.startRevision.getFinalCommitNumber() < revision.getFinalCommitNumber())  
										startRevision= revision;
								}
							}
						}
					});
					if (revision.isCommitted()) 
						if (startRevision == null || startRevision.getFinalCommitNumber() < revision.getFinalCommitNumber())  
							startRevision= revision;
				}
			}
		};
		
		
		List<RevisionFolder> revisions= _root.getRevisionFolders();
		RevisionFolder lastRevision= revisions.get(0);
		RevisionFolder startRevision= null;
		for (RevisionFolder revision: revisions) {
			if (revision.isCommitted()) 
				if (startRevision == null || startRevision.getFinalCommitNumber() < revision.getFinalCommitNumber())  
					startRevision= revision;
		}
		RevisionFolder revision= new RevisionFolder(_root, lastRevision.revisionNumber+1, startRevision.revisionNumber);
		_entitySession.store(revision);

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
	
	IResult<Void> closeStorageSession(final StorageSession storageSession) 
	{
		// remove the session reference from the session's starting revision
		final long startingCommitNumber= storageSession.getStartingCommitNumber();
		final long revisionNumber= storageSession.getRevisionNumber();
		IResult removeStartSession= TaskUtils.DONE;
		if (0 <= startingCommitNumber) {
			final IResult<RevisionFolder> getRevisionFolder= _root.getRevisionFolder(startingCommitNumber);
			removeStartSession= new Handler(getRevisionFolder) {
				protected IResult onSuccess() throws Exception {
					RevisionFolder startRevision= getRevisionFolder.getResult();
					assert startRevision != null : "Revision "+startingCommitNumber+" has apparently been deleted when it should have been leased by a session associated with revision "+storageSession.getRevisionNumber();
					startRevision.removeSession(storageSession.getSessionId());
				}
			};
		}
		
		return new Handler(removeStartSession) {
			protected IResult onSuccess() throws Exception {
				final IResult<RevisionFolder> getRevisionFolder= _root.getRevisionFolder(revisionNumber);
				return new Handler(getRevisionFolder) {
					protected IResult onSuccess() throws Exception {
						RevisionFolder revisionFolder= getRevisionFolder.getResult();
						return revisionFolder.removeSession(storageSession.getSessionId());
					}
				};
			}
		};
		

		---------------
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
		_entitySession.store(journal);
		
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
			TaskUtils.combineResults(validateActions).get();
			if (!conflictingRevisions.isEmpty())
				throw new ConflictingCommitException();
			
			// write commit marker
			_entitySession.store(new CommitMarker(revision, lastCommitNumber+1));

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

	public IResult<Boolean> isRevisionCommitted(final long revisionNumber) 
	{
		if (revisionNumber <= _lastKnownDeletedRevision)
			return TaskUtils.TRUE;
		if (_knownUncommittedRevisions.contains(revisionNumber))
			return TaskUtils.FALSE;
		final IResult<RevisionFolder> getRevisionFolder= _root.getRevisionFolder(revisionNumber);
		return new Handler(getRevisionFolder) {
			protected IResult onSuccess() throws Exception {
				RevisionFolder folder= getRevisionFolder.getResult();
				if (folder == null) { 
					/*
					 * Revision was committed and revision info has already been cleaned up.
					 * Since a revision cannot be cleaned until all previous revisions were 
					 * cleaned we can ignore any revisions prior to this one (which saves us a 
					 * boatload of file lookups).  
					 */
					updateLastKnownDeletedRevision(revisionNumber);
					return TaskUtils.TRUE;
				}
				return folder.isCommitted();
			}
		}.toResult();
	}

	void updateLastKnownDeletedRevision(long revisionNumber) {
		_lastKnownDeletedRevision= revisionNumber;
		for (Iterator<Long>i= _knownUncommittedRevisions.iterator(); i.hasNext();) {
			if (i.next() <= revisionNumber)
				i.remove();
		}
	}

	public IResult<List<Long>> getAvailableRevisions() {
		final IResult<List<RevisionFolder>> getRevisionFolders= _root.getRevisionFolders();
		return new Handler(getRevisionFolders) {
			protected IResult onSuccess() throws Exception {
				List<RevisionFolder> folders= getRevisionFolders.getResult();
				ArrayList<Long> revisions= new ArrayList<Long>();
				for (RevisionFolder folder: folders)
					revisions.add(folder.revisionNumber);
				return TaskUtils.asResult(revisions);
			}
		}.toResult();
	}
	
	public IResult<Void> close() {
		// close all open sessions
		ArrayList<IResult> allResults= new ArrayList<IResult>();
		while (!_activeSessions.isEmpty()) {
			StorageSession session= _activeSessions.remove(0);
			allResults.add(session.close());
		}
		allResults.add(_trackerSession.complete());
		
		return TaskUtils.combineResults(allResults);
	}

}
