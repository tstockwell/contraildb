package storage

import (
	"contrail/tasks"
	"math/big"
)



/**
 * A revision folder is a folder in a Contrail file system that contains 
 * information about a specific revision.  Information such as its commit number, 
 * the revision journal(list of changes in the revision) etc.
 * 
 * @author Ted Stockwell
 */
type RevisionFolder struct {
	// # of last committed revision when this revision was created    
	StartCommitNumber *big.Int 
	RevisionNumber *big.Int
	
	storage *ObjectStorageSession
	path *id.Identifier
	
	sessionsFolder *Folder
	lockFolder *Folder
	commitMarker CommitMarker 
	transient RevisionJournal _journal= null;
}

func CreateReadOnlyRevisionFolder(root *RootFolder, revisionNumber *big.Int) *RevisionFolder {
		this(root, revisionNumber, -1L);
}
func CreateReadWriteRevisionFolder(root *RootFolder, revisionNumber *big.Int, startCommitNumber *big.Int) *RevisionFolder {
	path:= root.RevisionsFolder().Identifier().Child("revision-"+revisionNumber.String())
	return &RevisionFolder(
		path: path
		StartCommitNumber: startCommitNumber,
		RevisionNumber: revisionNumber,
		sessionsFolder: CreateFolder(path.Child("sessions")),
		lockFolder: CreateFolder(path.Child("lock")),
	)
}

func (self *RevisionFolder) String() { 	
	return ""+self.revisionNumber.String()+ "{"
		+"start-commit-number="+self.startCommitNumber.String()
		+", isCommitted="+b;self.IsCommitted()
		+= "}"
}

func Lock(


	public boolean lock(String processId, boolean waitForLock) 
	throws IOException 
	{
		return _lockFolder.lock(processId, waitForLock);
	}
	public void unlock(String processId) throws IOException {
		_lockFolder.unlock(processId);
	}

	
	public void addSession(String sessionId) throws IOException {
		storage.store(new Entity(Identifier.create(_sessionsFolder.id, sessionId)));
	}
	public void removeSession(String sessionId) throws IOException {
		storage.delete(Identifier.create(_sessionsFolder.id, sessionId));
	}
	public boolean isActive() throws IOException {
//TODO  This method should check to see if the session claims are expired
//A session can only hold a revision active for a limited amount of time, about a minute.			
		
		return !_sessionsFolder.listChildren().get().isEmpty();
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
		return (CommitMarker) storage.fetch(CommitMarker.createId(this)).get();
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
		return (RevisionJournal) storage.fetch(RevisionJournal.createId(this)).get();
	}





////////////////////////////
//
// Implementation of Lifecycle interface
//


func (self *LockFolder)	SetStorage(storage *ObjectStorageSession) {
	self.storage= storage
}
func (self *LockFolder)	OnInsert(path *id.Identifier) {
	self.path= path
	pool:= tasks.CreateTaskPool() 
	pool.Go(func() { self.storage.Store(self.sessionsFolder) })
	pool.Go(func() { self.storage.Store(self.lockFolder) })
	pool.JoinAll()
}
func (self *LockFolder)	OnLoad(path *id.Identifier) {
	self.path= path
	pool:= tasks.CreateTaskPool() 
	pool.Go(func() { self.sessionsFolder= self.storage.Fetch(path.Child("sessions")) })
	pool.Go(func() { self.lockFolder= self.storage.Fetch(path.Child("lock")) })
	pool.JoinAll()
}
func (self *LockFolder)	OnDelete() {
	pool:= tasks.CreateTaskPool() 
	pool.Go(func() { self.storage.Delete(path.Child("sessions")) })
	pool.Go(func() { self.storage.Delete(path.Child("lock")) })
	pool.JoinAll()
}
func (self *LockFolder)	Identifier() *id.Identifier {
	return self.identifier
}
func (self *LockFolder)	Storage() *ObjectStorageSession {
	return self.storage
}
