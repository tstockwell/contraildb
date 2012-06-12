package storage

import (
	"testing"
	"time"
	"os"
	"path/filepath"
)

var dirName string= filepath.Join(os.TempDir, "contrail_test")
var provider FileStorageProvider=CreateFileStorageProvider(dirName, true/*clean*/)
var tester StorageProviderTester= CreateStorageProviderTester(provider)

func TestSimpleStorage(t *testing.T) {
	tester.TestSimpleStorage(t);
}
	
func TestCreate(t *testing.T) {
	tester.TestCreate(t);
}
	
func (self *StorageProviderTester) TestConcurrentStoreAPIListChildren(t *testing.T) {
	tester.TestConcurrentStoreAPIListChildren(t);
}