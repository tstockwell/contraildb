package com.googlecode.contraildb.core.storage;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import com.googlecode.contraildb.core.Identifier;
import com.googlecode.contraildb.core.impl.PathUtils;
import com.googlecode.contraildb.core.utils.ContrailAction;
import com.googlecode.contraildb.core.utils.Logging;


/**
 * A task class that cleans up unneeded revisions from versioned storage.
 * 
 * @author Ted Stockwell
 *
 */
public class StorageCleanupAction extends ContrailAction {

	private RootFolder _rootFolder;
	private IEntityStorage.Session _storageSession;
	private StorageSystem _storageSystem;

	public StorageCleanupAction(StorageSystem storageSystem) 
	throws IOException 
	{
		_storageSystem= storageSystem;
		_storageSession= storageSystem._entityStorage.connect();
		_rootFolder= (RootFolder) _storageSession.fetch(storageSystem._root.getId()).get();
	}


	@Override
	protected void action() throws Exception {
		
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

	/**
	 * When removing a revision we also remove revisions of individual 
	 * files that are made obsolete by newer file revisions. 
	 */
	private void cleanupFiles(RevisionFolder revision) 
	throws IOException 
	{
		RevisionJournal journal= revision.getRevisionJournal();
		if (journal == null) 
			return;
		HashSet<Identifier> files= new HashSet<Identifier>();
		files.addAll(journal.deletes);
		files.addAll(journal.inserts);
		files.addAll(journal.updates);
		for (Identifier id: files) {
			Collection<Identifier> children= _storageSession.listChildren(id).get();
			for (Identifier child: children) {
				long rev= PathUtils.getRevisionNumber(child);
				if (rev < 0)
					continue;
				if (rev < revision.revisionNumber)
					_storageSession.delete(child);
			}
		}
	}

}
