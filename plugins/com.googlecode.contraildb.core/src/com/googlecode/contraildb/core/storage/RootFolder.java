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

import com.googlecode.contraildb.core.IResult;
import com.googlecode.contraildb.core.Identifier;
import com.googlecode.contraildb.core.utils.Logging;
import com.googlecode.contraildb.core.utils.Result;
import com.googlecode.contraildb.core.utils.OrderedResults;
import com.googlecode.contraildb.core.utils.TaskUtils;
import com.googlecode.contraildb.core.utils.ExternalizationManager.Serializer;
import com.googlecode.contraildb.core.utils.Handler;


/**
 * The root folder of a Contrail storage system.
 * There are three subfolders...
 * 	...a 'lockfolder' folder that is used to coordinate commits, and 
 * 	...a 'revisions' folder that contains a subfolder for each 
 * 		available revision. 
 * 	...a 'deletions' folder that contains a subfolder for each 
 * 		deleted revision that has not yet been cleaned up. 
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
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

	public IResult<RevisionFolder> getRevisionFolder(long revisionNumber) 
	{
		return storage.fetch(RevisionFolder.createId(this, revisionNumber));
	}

	/**
	 * Returns list of revision folder in descending order by revision number 
	 */
	public IResult<List<RevisionFolder>> getRevisionFolders() 
	{
		final IResult<Collection<Identifier>> deletedResult= _deletionsFolder.listChildren();
		final IResult<Collection<Entity>> childrenResult= _revisionsFolder.getChildren();
		return new Handler(deletedResult, childrenResult) {
			protected IResult onSuccess() throws Exception {
				Collection<Identifier> deleted= deletedResult.getResult();
				Collection<Entity> children= childrenResult.getResult();
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
				return TaskUtils.asResult(revisions);
			};
		}.toResult();
	}
	
	public IResult<Boolean> lock(final String processId, boolean waitForLock) {
		final IResult<Boolean> lock= _lockFolder.lock(processId, waitForLock);
		return new Handler(lock) {
			protected IResult onSuccess() throws Exception {
				if (lock.getResult() && __logger.isLoggable(Level.FINER))
					__logger.finer("root locked by "+processId);
				return lock;
			};
		}.toResult();
	}
	public IResult<Void> unlock(final String processId) {
		return new Handler(_lockFolder.unlock(processId)) {
			protected IResult onSuccess() throws Exception {
				if (__logger.isLoggable(Level.FINER))
					__logger.finer("root unlocked by "+processId);
				return TaskUtils.DONE;
			};
		}.toResult();
	}

	public IResult<RevisionFolder> getLastCommittedRevision() {
		final IResult<List<RevisionFolder>> getRevisionFolders= getRevisionFolders();
		return new Handler(getRevisionFolders) {
			protected IResult onSuccess() throws Exception {
				final RevisionFolder[] folder= new RevisionFolder[] { null };
				final OrderedResults syncResults= new OrderedResults();
				for (final RevisionFolder revision: getRevisionFolders.getResult()) {
					final IResult<Boolean> isCommitted= revision.isCommitted();
					final IResult<Boolean> latch= syncResults.create();
					new Handler(isCommitted, latch) {
						protected void onComplete() throws Exception {
							if (isCommitted.getResult()) {
								if (folder[0] == null) {
									folder[0]= revision;
								}
								else {
									final IResult<Long> folderFinalCommitNumber= folder[0].getFinalCommitNumber();
									final IResult<Long> revisionFinalCommitNumber= revision.getFinalCommitNumber();
									new Handler(folderFinalCommitNumber, revisionFinalCommitNumber) {
										protected void onComplete() throws Exception {
											if (folderFinalCommitNumber.getResult() < revisionFinalCommitNumber.getResult())
												folder[0]= revision;
										};
									};
								}
							}
							else 
								syncResults.next();
						};
					};
				}
				
				return new Handler(syncResults.complete()) {
					protected IResult onSuccess() throws Exception {
						return TaskUtils.asResult(folder[0]);
					};
				}.toResult();
			};
		}.toResult();
	}

	@Override
	public IResult<Void> onInsert(Identifier identifier) {
		return new Handler(super.onInsert(identifier)) {
			protected IResult onSuccess() throws Exception {
				spawnChild(storage.store(_revisionsFolder));
				spawnChild(storage.store(_deletionsFolder));
				spawnChild(storage.store(_lockFolder));
				
				RevisionFolder revision= new RevisionFolder(RootFolder.this, 0L, 0L);  
				spawnChild(storage.store(revision));
				spawnChild(storage.store(new CommitMarker(revision, 0L)));
				spawnChild(storage.store(new RevisionJournal(revision)));
				return TaskUtils.DONE;
			};
		}.toResult();
	}

	@Override
	public IResult<Void> onLoad(final Identifier identifier) {
		return new Handler(super.onLoad(identifier)) {
			protected IResult onSuccess() throws Exception {
				final IResult<Entity> rf= storage.fetch(Identifier.create(identifier, "revisions"));
				final IResult<Entity> df= storage.fetch(Identifier.create(identifier, "deletions"));
				final IResult<LockFolder> lf= storage.fetch(LockFolder.createId(RootFolder.this));
				spawnChild(new Handler(rf, df, lf) {
					protected IResult onSuccess() throws Exception {
						_revisionsFolder= rf.getResult();
						_deletionsFolder= df.getResult();
						_lockFolder= lf.getResult();
						return TaskUtils.DONE;
					};
				}.toResult());
				
				return TaskUtils.DONE;
			};
		}.toResult();
	}

	public IResult<Void> markRevisionForDeletion(long revisionNumber) {
		return storage.store(new Entity(Identifier.create(_deletionsFolder.id, Long.toString(revisionNumber))));
	}

	public IResult<Void> deleteRevision(RevisionFolder revision) {
		ArrayList<IResult> results= new ArrayList<IResult>();
		results.add(storage.delete(revision.getId()));
		results.add(storage.delete(Identifier.create(_deletionsFolder.id, Long.toString(revision.revisionNumber))));
		return TaskUtils.combineResults(results);
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
