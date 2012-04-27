package com.googlecode.contraildb.core.storage;

import java.io.DataInput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.googlecode.contraildb.core.Identifier;
import com.googlecode.contraildb.core.utils.IResult;
import com.googlecode.contraildb.core.utils.Logging;
import com.googlecode.contraildb.core.utils.ExternalizationManager.Serializer;


/**
 * The root folder of a Contrail storage system.
 * There are three subfolders...
 * 	...a 'lockfolder' folder that is used to coordinate commits, and 
 * 	...a 'revisions' folder that contains a subfolder for each 
 * 		available revision. 
 * 	...a 'deletions' folder that contains a subfolder for each 
 * 		deleted revision that has not yet been cleaned up. 
 */
public class RootFolder extends Entity  {
	private static final long serialVersionUID = 1L;
	private static Logger __logger= Logging.getLogger();
	
	transient Entity _revisionsFolder;
	transient Entity _deletionsFolder;
	transient LockFolder _lockFolder;
	
	RootFolder(Identifier path) throws IOException {
		super(path);
		_revisionsFolder= new Entity(Identifier.create(id, "revisions"));
		_deletionsFolder= new Entity(Identifier.create(id, "deletions"));
		_lockFolder= new LockFolder(this);
	}
	
	protected RootFolder() { }
	
	Entity getRevisionsFolder() {
		return _revisionsFolder;
	}

	public RevisionFolder getRevisionFolder(long revisionNumber) 
	throws IOException 
	{
		return (RevisionFolder) storage.fetch(RevisionFolder.createId(this, revisionNumber)).get();
	}

	/**
	 * Returns list of revision folder in descending order by revision number 
	 */
	public List<RevisionFolder> getRevisionFolders() 
	throws IOException 
	{
		Collection<Identifier> deleted= _deletionsFolder.listChildren().get();
		Collection<Entity> children= _revisionsFolder.getChildren().get();
		ArrayList<RevisionFolder> revisions= new ArrayList<RevisionFolder>();
		for (Entity child: children) {
			if (child instanceof RevisionFolder) {
				Identifier deleteMarker= Identifier.create(_deletionsFolder.id, child.getId().getName());
				if (!deleted.contains(deleteMarker)) {
					revisions.add((RevisionFolder)child);
				}
			}
		}
		Collections.sort(revisions, new Comparator<RevisionFolder>() {
			public int compare(RevisionFolder o1, RevisionFolder o2) {
				long n1= o1.revisionNumber;
				long n2= o2.revisionNumber;
				if (n1 < n2)
					return 1;
				if (n1 > n2)
					return -1;
				return 0;
			}
		});
		return revisions;
	}
	
	public boolean lock(String processId, boolean waitForLock) 
	throws IOException {
		boolean lock= _lockFolder.lock(processId, waitForLock);
		if (lock && __logger.isLoggable(Level.FINER))
			__logger.finer("root locked by "+processId);
		return lock;
	}
	public void unlock(String processId) throws IOException {
		_lockFolder.unlock(processId);
		if (__logger.isLoggable(Level.FINER))
			__logger.finer("root unlocked by "+processId);
	}

	public RevisionFolder getLastCommittedRevision() throws IOException {
		RevisionFolder folder= null;
		for (RevisionFolder revision: getRevisionFolders()) {
			if (!revision.isCommitted())
				continue;
			if (folder == null || folder.getFinalCommitNumber() < revision.getFinalCommitNumber())
				folder= revision;
		}
		return folder;
	}

	@Override
	public void onInsert(Identifier identifier) throws IOException {
		super.onInsert(identifier);
		storage.store(_revisionsFolder);
		storage.store(_deletionsFolder);
		storage.store(_lockFolder);
		
		RevisionFolder revision= new RevisionFolder(this, 0L, 0L);  
		storage.store(revision);
		storage.store(new CommitMarker(revision, 0L));
		storage.store(new RevisionJournal(revision));
	}

	@Override
	public void onLoad(Identifier identifier) throws IOException {
		super.onLoad(identifier);
		CFuture<Entity> rf= storage.fetch(Identifier.create(identifier, "revisions"));
		CFuture<Entity> df= storage.fetch(Identifier.create(identifier, "deletions"));
		CFuture<LockFolder> lf= storage.fetch(LockFolder.createId(this));
		
		_revisionsFolder= rf.get();
		_deletionsFolder= df.get();
		_lockFolder= lf.get();
	}

	public void markRevisionForDeletion(long revisionNumber) throws IOException {
		storage.store(new Entity(Identifier.create(_deletionsFolder.id, Long.toString(revisionNumber))));
	}

	public void deleteRevision(RevisionFolder revision) throws IOException {
		storage.delete(revision.getId());
		storage.delete(Identifier.create(_deletionsFolder.id, Long.toString(revision.revisionNumber)));
	}
	


	public static final Serializer<RootFolder> SERIALIZER= new Serializer<RootFolder>() {
		private final int typeCode= RootFolder.class.getName().hashCode();
		public RootFolder readExternal(java.io.DataInput in) 
		throws IOException {
			RootFolder journal= new RootFolder();
			readExternal(in, journal);
			return journal;
		};
		public void writeExternal(java.io.DataOutput out, RootFolder journal) 
		throws IOException {
			Entity.SERIALIZER.writeExternal(out, journal);
		};
		public void readExternal(DataInput in, RootFolder journal)
		throws IOException {
			Entity.SERIALIZER.readExternal(in, journal);
		}
		public int typeCode() {
			return typeCode;
		}
	};

	
}
