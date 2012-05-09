package com.googlecode.contraildb.core.storage;

import java.io.DataInput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import com.googlecode.contraildb.core.IResult;
import com.googlecode.contraildb.core.Identifier;
import com.googlecode.contraildb.core.utils.ExternalizationManager.Serializer;
import com.googlecode.contraildb.core.utils.ResultHandler;
import com.googlecode.contraildb.core.utils.TaskUtils;



/**
 * A revision folder is a folder in a Contrail file system that contains 
 * information about a specific revision.  Information such as its commit number, 
 * the revision journal(list of changes in the revision) etc.
 * 
 * @author Ted Stockwell
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
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
			boolean b= isCommitted().get();
			s+= ", isCommitted="+b;
		}
		catch (Throwable t) { }
		s+= "}";
		return s;
	}
	
	@Override
	public IResult<Void> onInsert(Identifier identifier) {
		return new ResultHandler(super.onInsert(identifier)) {
			protected IResult onSuccess() throws Exception {
				spawnChild(storage.store(_sessionsFolder));
				spawnChild(storage.store(_lockFolder));
				return TaskUtils.DONE;
			};
		}.toResult();
	}
	@Override
	public IResult<Void> onLoad(Identifier identifier) {
		return new ResultHandler(super.onLoad(identifier)) {
			protected IResult onSuccess() throws Exception {
				final IResult<Entity> sf= storage.fetch(Identifier.create(id, "sessions"));
				final IResult<LockFolder> lf= storage.fetch(LockFolder.createId(RevisionFolder.this));
				spawnChild(new ResultHandler(sf,lf) {
					protected IResult onSuccess() throws Exception {
						_sessionsFolder= sf.getResult();
						_lockFolder= lf.getResult();
						return TaskUtils.DONE;
					};
				});
				return TaskUtils.DONE;
			};
		}.toResult();
	}

	public IResult<Boolean> lock(String processId, boolean waitForLock) 
	{
		return _lockFolder.lock(processId, waitForLock);
	}
	public IResult<Void> unlock(String processId) {
		return _lockFolder.unlock(processId);
	}

	
	public IResult<Void> addSession(String sessionId) {
		return storage.store(new Entity(Identifier.create(_sessionsFolder.id, sessionId)));
	}
	public IResult<Void> removeSession(String sessionId) {
		return storage.delete(Identifier.create(_sessionsFolder.id, sessionId));
	}
	public IResult<Boolean> isActive()  {
//TODO  This method should check to see if the session claims are expired
//A session can only hold a revision active for a limited amount of time, about a minute.			
		final IResult<Collection<Identifier>> listResult= _sessionsFolder.listChildren();
		return new ResultHandler(listResult) {
			protected IResult onSuccess() throws Exception {
				return TaskUtils.asResult(!listResult.getResult().isEmpty());
			};
		}.toResult();
	}
	public static IResult<Void> sortByDescendingCommitNumber(final List<RevisionFolder> revisions) 
	{
		// pre-populate commit markers so that we can ignore IOExceptions during sort
		final HashMap<Long, Long> commitNumbersByRevisionNumber= new HashMap<Long, Long>();
		final ArrayList<IResult> tasks= new ArrayList<IResult>();
		for (final RevisionFolder revision: revisions)  {
			final IResult<CommitMarker> getCommitMarker= revision.getCommitMarker();
			tasks.add(new ResultHandler(getCommitMarker) {
				protected IResult onSuccess() throws Exception {
					long commitNumber= -1;
					CommitMarker commitMarker= getCommitMarker.getResult();
					if (commitMarker != null)
						commitNumber= commitMarker.finalCommitNumber;
					commitNumbersByRevisionNumber.put(revision.revisionNumber, commitNumber);
					return TaskUtils.DONE;
				}
			}.toResult());
		}
		return new ResultHandler(TaskUtils.combineResults(tasks)) {
			protected IResult onSuccess() throws Exception {
				
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
				
				return TaskUtils.DONE;
			};
		}.toResult();
	}
	private IResult<CommitMarker> getCommitMarker() {
		return storage.fetch(CommitMarker.createId(this));
	}
	public IResult<Boolean> isCommitted() {
		final IResult<CommitMarker> commitMarker= getCommitMarker();
		return new ResultHandler(commitMarker) {
			protected IResult onSuccess() throws Exception {
				return TaskUtils.asResult(commitMarker.getResult() != null);
			};
		}.toResult();
	}
	public IResult<Long> getFinalCommitNumber() {
		final IResult<CommitMarker> getCommitMarker= getCommitMarker();
		return new ResultHandler(getCommitMarker) {
			protected IResult onSuccess() throws Exception {
				CommitMarker commitMarker= getCommitMarker.getResult();
				if (commitMarker == null)
					return TaskUtils.asResult(-1L);
				return TaskUtils.asResult(commitMarker.finalCommitNumber);
			};
		}.toResult();
	}
	public IResult<RevisionJournal> getRevisionJournal() {
		return storage.fetch(RevisionJournal.createId(this));
	}



	public static final Serializer<RevisionFolder> SERIALIZER= new Serializer<RevisionFolder>() {
		private final int typeCode= RevisionFolder.class.getName().hashCode();
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
		public int typeCode() {
			return typeCode;
		}
	};

	
}

