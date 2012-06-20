package storage

import (
	"math/big"
)

/**
 * This class encapsulates behavior associated with the root folder of a 
 * Contrail storage system.
 * There are three subfolders...
 * 	...a 'lockfolder' folder that is used to coordinate commits, and 
 * 	...a 'revisions' folder that contains a subfolder for each 
 * 		available revision. 
 * 	...a 'deletions' folder that contains a subfolder for each 
 * 		deleted revision that has not yet been cleaned up. 
 */
type RootFolder struct {
	storage *ObjectStorageSession
	path *id.Identifier
	revisionsFolder *Folder
	deletionsFolder *Folder
	lockFolder *Folder
} 

func CreateRootFolder(path *id.Identifier) *RootFolder {
	return &RootFolder(
		path: path,
		revisionsFolder: CreateFolder(path.Child("revisions")),
		deletionsFolder: CreateFolder(path.Child("deletions")),
		lockFolder: CreateFolder(path.Child("locks")),
	)
}

func (self *RootFolder) RevisionsFolder() *Folder {
		return self.revisionsFolder;
}

func (self *RootFolder) RevisionFolder(revisionNumber big.Int) *Folder { 
	path:= self.revisionsFolder.Identifier().Child(revisionNumber)
	return &self.storage.Fetch(path).(Folder)
}

/**
 * Returns list of revision folders in descending order by revision number 
 */
func (self *RootFolder)  List<RevisionFolder> getRevisionFolders() []*Revis 
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

func (self *RootFolder) public boolean lock(String processId, boolean waitForLock) 
throws IOException {
	boolean lock= _lockFolder.lock(processId, waitForLock);
	if (lock && __logger.isLoggable(Level.FINER))
		__logger.finer("root locked by "+processId);
	return lock;
}
func (self *RootFolder) public void unlock(String processId) throws IOException {
	_lockFolder.unlock(processId);
	if (__logger.isLoggable(Level.FINER))
		__logger.finer("root unlocked by "+processId);
}

func (self *RootFolder) public RevisionFolder getLastCommittedRevision() throws IOException {
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
func (self *RootFolder) public void onInsert(Identifier identifier) throws IOException {
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
func (self *RootFolder) public void onLoad(Identifier identifier) throws IOException {
	super.onLoad(identifier);
	IResult<Entity> rf= storage.fetch(Identifier.create(identifier, "revisions"));
	IResult<Entity> df= storage.fetch(Identifier.create(identifier, "deletions"));
	IResult<LockFolder> lf= storage.fetch(LockFolder.createId(this));
	
	_revisionsFolder= rf.get();
	_deletionsFolder= df.get();
	_lockFolder= lf.get();
}

func (self *RootFolder) public void markRevisionForDeletion(long revisionNumber) throws IOException {
	storage.store(new Entity(Identifier.create(_deletionsFolder.id, Long.toString(revisionNumber))));
}

func (self *RootFolder) public void deleteRevision(RevisionFolder revision) throws IOException {
	storage.delete(revision.getId());
	storage.delete(Identifier.create(_deletionsFolder.id, Long.toString(revision.revisionNumber)));
}




////////////////////////////
//
// Implementation of Lifecycle interface
//


func (self *RootFolder)	SetStorage(storage *ObjectStorageSession) {
	self.storage= storage
}
func (self *RootFolder)	OnInsert(path *id.Identifier) {
	self.path= path
}
func (self *RootFolder)	OnLoad(path *id.Identifier) {
	self.path= path
}
func (self *RootFolder)	OnDelete() {
	// do nothing
}
func (self *RootFolder)	Identifier() *id.Identifier {
	return self.identifier
}
func (self *RootFolder)	Storage() *ObjectStorageSession {
	return self.storage
}
