package com.googlecode.contraildb.core.storage;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import com.googlecode.contraildb.core.IResult;
import com.googlecode.contraildb.core.Identifier;
import com.googlecode.contraildb.core.impl.PathUtils;
import com.googlecode.contraildb.core.utils.ContrailAction;
import com.googlecode.contraildb.core.utils.Logging;
import com.googlecode.contraildb.core.utils.Handler;
import com.googlecode.contraildb.core.utils.TaskUtils;


/**
 * A task class that cleans up unneeded revisions from versioned storage.
 * 
 * @author Ted Stockwell
 *
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class StorageCleanupAction {

	private RootFolder _rootFolder;
	private IEntityStorage.Session _storageSession;
	private StorageSystem _storageSystem;

	public static IResult<Void> cleanup(StorageSystem storageSystem) {
		return new Handler(StorageCleanupAction.create(storageSystem)) {
			protected IResult onSuccess() throws Exception {
				spawnChild(((StorageCleanupAction)incoming().getResult()).run());
				return TaskUtils.DONE;
			}
		}.toResult();	
	}

	public static final IResult<StorageCleanupAction> create(final StorageSystem storageSystem) 
	{
		final IResult<IEntityStorage.Session> entityStorage= storageSystem._entityStorage.connect();
		return new Handler(entityStorage) {
			protected IResult onSuccess() throws Exception {
				final StorageCleanupAction cleanupAction= new StorageCleanupAction();
				cleanupAction._storageSystem= storageSystem;
				cleanupAction._storageSession= entityStorage.getResult();
				final IResult<RootFolder> rf= cleanupAction._storageSession.fetch(storageSystem._root.getId());
				spawnChild(new Handler(rf) {
					protected IResult onSuccess() throws Exception {
						cleanupAction._rootFolder= rf.get();
						return TaskUtils.DONE;
					};
				}.toResult());
				return TaskUtils.asResult(cleanupAction);
			};
		}.toResult();
	}

	private StorageCleanupAction() 
	{
	}


	public IResult<Void> run() {
		
		final String sessionId= "cleanup."+UUID.randomUUID().toString();

		try {
			/*
			 * Clean up revision starting with oldest.
			 * NOTE: an active revision also keeps revisions with higher commit numbers from being deleted 
			 */
			while (true) {

				if (_rootFolder.lock(sessionId, true)) {
					try {
						List<RevisionFolder> revisions= _rootFolder.getRevisionFolders();
						int i= 0;
						for (RevisionFolder revision: revisions) {
							i++;
							if (revision.isCommitted())
								break;
						}
						if (revisions.size() <= i)
							break; 

						final RevisionFolder revision= revisions.get(revisions.size()-1);

						if (revision.isActive()) 
							break;

						_rootFolder.markRevisionForDeletion(revision.revisionNumber);
						_storageSession.flush();

						new ContrailAction(revision.getId(), Operation.DELETE) {
							protected void action() throws Exception {
								try {
									cleanupFiles(revision);

									String session= Identifier.create().toString();
									_rootFolder.lock(session, true);
										
									try {
										_rootFolder.deleteRevision(revision);
										_storageSession.flush();
									}
									finally {
										_rootFolder.unlock(session);
									}

									_storageSession.flush();
									Logging.fine("revision "+revision.revisionNumber+" cleaned up, session="+sessionId);
									if (_storageSystem._lastKnownDeletedRevision < revision.revisionNumber)
										_storageSystem.updateLastKnownDeletedRevision(revision.revisionNumber);
									//_root.revisionDeletionCompleted(revision.revisionNumber);
								}
								catch (Throwable t) {
									Logging.severe("Error while attempting to clean up revision "+revision.revisionNumber, t);
								}
							}
						}.submit();

					}
					catch (Throwable t) {
						Logging.severe("Error while attempting to clean up storage", t);
						break;
					}
					finally {
						_rootFolder.unlock(sessionId);
					}
				}
				else
					break;
			}
		}
		catch (Throwable t) {
			Logging.warning("Error while cleaning up storage", t);
		}
	}
	
	private IResult<Void> cleanupAndDeleteRevision(final RevisionFolder revision, final String sessionId) {
		return new Handler() {
			protected IResult onSuccess() throws Exception {

					spawnChild(cleanupFiles(revision));

					final String session= Identifier.create().toString();
					IResult deleteRevision= new Handler(_rootFolder.lock(session, true)) {
						protected IResult onSuccess() throws Exception {
							
							IResult doDelete= new Handler(_rootFolder.deleteRevision(revision)) {
								protected void onComplete() throws Exception {
									spawnChild(new Handler(_storageSession.flush()));
								}
							}.toResult();
							
							spawnChild(new Handler(doDelete) {
								protected void onComplete() throws Exception {
										_rootFolder.unlock(session);
								}
							}.toResult());
							
							return TaskUtils.DONE;
						}
					}.toResult();
					
					IResult flush= new Handler(deleteRevision) {
						protected void onComplete() throws Exception {
							_storageSession.flush();
							Logging.fine("revision "+revision.revisionNumber+" cleaned up, session="+sessionId);
							if (_storageSystem._lastKnownDeletedRevision < revision.revisionNumber)
								_storageSystem.updateLastKnownDeletedRevision(revision.revisionNumber);
						}
					}.toResult();
					
					spawnChild(new Handler(flush) {
						protected void onError() {
							Logging.severe("Error while attempting to clean up revision "+revision.revisionNumber, incoming().getError());
						}
					}.toResult());
						

					//_root.revisionDeletionCompleted(revision.revisionNumber);
				return TaskUtils.DONE;
			};
		}.toResult();
		
	}

	/**
	 * When removing a revision we also remove revisions of individual 
	 * files that are made obsolete by newer file revisions. 
	 */
	private IResult<Void> cleanupFiles(final RevisionFolder revision) 
	{
		final IResult<RevisionJournal> getRevisionJournal= revision.getRevisionJournal();
		return new Handler(getRevisionJournal) {
			protected IResult onSuccess() throws Exception {
				RevisionJournal journal= getRevisionJournal.getResult();
				if (journal == null) 
					return TaskUtils.DONE;
				HashSet<Identifier> files= new HashSet<Identifier>();
				files.addAll(journal.deletes);
				files.addAll(journal.inserts);
				files.addAll(journal.updates);
				for (Identifier id: files) {
					final IResult<Collection<Identifier>> listChildren= _storageSession.listChildren(id);
					spawnChild(new Handler(listChildren) {
						protected IResult onSuccess() throws Exception {
							Collection<Identifier> children= listChildren.get();
							for (Identifier child: children) {
								long rev= PathUtils.getRevisionNumber(child);
								if (rev < 0)
									continue;
								if (rev < revision.revisionNumber)
									_storageSession.delete(child);
							}
							return TaskUtils.DONE;
						}
					}.toResult());
				}
				return TaskUtils.DONE;
			}
		}.toResult();
	}

}
