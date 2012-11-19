package com.googlecode.contraildb.core.storage;

import java.io.DataInput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import kilim.Pausable;

import com.googlecode.contraildb.core.Identifier;
import com.googlecode.contraildb.core.async.ContrailAction;
import com.googlecode.contraildb.core.async.IResult;
import com.googlecode.contraildb.core.async.TaskTracker;
import com.googlecode.contraildb.core.utils.ExternalizationManager.Serializer;



/**
 * A revision folder is a folder in a Contrail file system that contains 
 * information about a specific revision.  Information such as its commit number, 
 * the revision journal(list of changes in the revision) etc.
 * 
 * @author Ted Stockwell
 */
public class RevisionFolder extends Entity {
	private static final long serialVersionUID = 1L;
	
	long startCommitNumber;
	long revisionNumber;
	
	transient Entity _sessionsFolder;
	transient LockFolder _lockFolder;
	transient CommitMarker _commitMarker= null;
	transient RevisionJournal _journal= null;
	
	public static Identifier createId(RootFolder root, long revisionNumber) {
		return Identifier.create(root.getRevisionsFolder().id, Long.toString(revisionNumber));
	}
	
	public RevisionFolder(RootFolder root, long revisionNumber) {
		this(root, revisionNumber, -1L);
	}
	public RevisionFolder(RootFolder root, long revisionNumber, long startCommitNumber) {
		super(createId(root, revisionNumber));
		this.startCommitNumber= startCommitNumber;
		this.revisionNumber= revisionNumber;
		_sessionsFolder= new Entity(Identifier.create(getId(), "sessions"));
		_lockFolder= new LockFolder(this);
	}
	
	protected RevisionFolder() { }
	
	@Override
	public String toString() {
		String s= ""+revisionNumber+"{start-commit-number="+startCommitNumber;
		try {
			boolean b= isCommitted();
			s+= ", isCommitted="+b;
		}
		catch (Throwable t) { }
		s+= "}";
		return s;
	}
	
	@Override
	public void onInsert(Identifier identifier) throws IOException {
		super.onInsert(identifier);
		new TaskTracker(
				storage.store(_sessionsFolder),
				storage.store(_lockFolder)
		).awaitb();
	}
	@Override
	public void onLoad(Identifier identifier) throws IOException {
		super.onLoad(identifier);
		IResult<Entity> sf= storage.fetch(Identifier.create(id, "sessions"));
		IResult<LockFolder> lf= storage.fetch(LockFolder.createId(RevisionFolder.this));
		
		_sessionsFolder= sf.getb();
		_lockFolder= lf.getb();
	}

	public IResult<Boolean> lock(String processId, boolean waitForLock) 
	{
		return _lockFolder.lock(processId, waitForLock);
	}
	public IResult<Void> unlock(String processId) {
		return _lockFolder.unlock(processId);
	}

	
	public void addSession(String sessionId) throws IOException {
		storage.store(new Entity(Identifier.create(_sessionsFolder.id, sessionId))).getb();
	}
	public void removeSession(String sessionId) throws IOException {
		storage.delete(Identifier.create(_sessionsFolder.id, sessionId));
	}
	public boolean isActive() throws IOException {
//TODO  This method should check to see if the session claims are expired
//A session can only hold a revision active for a limited amount of time, about a minute.			
		
		Collection<Identifier> identifiers= _sessionsFolder.listChildren().get();
		return !identifiers.isEmpty();
	}
	public static void sortByDescendingCommitNumber(List<RevisionFolder> revisions) 
	throws IOException 
	{
		final HashMap<Long, Long> commitNumbersByRevisionNumber= new HashMap<Long, Long>();
		// pre-populate commit markers so that we can ignore IOExceptions during sort
		for (RevisionFolder revision: revisions)  {
			long commitNumber= -1;
			CommitMarker commitMarker= revision.getCommitMarker();
			if (commitMarker != null)
				commitNumber= commitMarker.finalCommitNumber;
			commitNumbersByRevisionNumber.put(revision.revisionNumber, commitNumber);
		}
		
		// sort in decending order by final commit number;
		Collections.sort(revisions, new Comparator<RevisionFolder>() {
			public int compare(RevisionFolder o1, RevisionFolder o2) {
				long n1= commitNumbersByRevisionNumber.get(o1.revisionNumber);
				long n2= commitNumbersByRevisionNumber.get(o2.revisionNumber);
				if (n1 < n2)
					return 1;
				if (n1 > n2)
					return -1;
				return 0;
			}
		});
	}
	private CommitMarker getCommitMarker() throws IOException {
		CommitMarker marker= (CommitMarker) storage.fetch(CommitMarker.createId(this)).get();
		return marker;
	}
	public boolean isCommitted() throws IOException {
		return getCommitMarker() != null;
	}
	public long getFinalCommitNumber() throws IOException {
		CommitMarker commitMarker= getCommitMarker();
		if (commitMarker == null)
			return -1L;
		return commitMarker.finalCommitNumber;
	}
	public RevisionJournal getRevisionJournal() throws IOException {
		RevisionJournal journal= (RevisionJournal) storage.fetch(RevisionJournal.createId(this)).get();
		return journal;
	}



	public static final Serializer<RevisionFolder> SERIALIZER= new Serializer<RevisionFolder>() {
		private final String typeCode= RevisionFolder.class.getName();
		public RevisionFolder readExternal(java.io.DataInput in) 
		throws IOException {
			RevisionFolder journal= new RevisionFolder();
			readExternal(in, journal);
			return journal;
		};
		public void writeExternal(java.io.DataOutput out, RevisionFolder journal) 
		throws IOException {
			Entity.SERIALIZER.writeExternal(out, journal);
			out.writeLong(journal.startCommitNumber);
			out.writeLong(journal.revisionNumber);
		};
		public void readExternal(DataInput in, RevisionFolder journal)
		throws IOException {
			Entity.SERIALIZER.readExternal(in, journal);
			journal.startCommitNumber= in.readLong();
			journal.revisionNumber= in.readLong();
		}
		public String typeCode() {
			return typeCode;
		}
	};

	
}

