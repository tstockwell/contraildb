package storage

import (
	"testing"
	"os"
	"path/filepath"
)

var dirName string= filepath.Join(os.TempDir(), "contrail_test")
var provider *FileStorageProvider=CreateFileStorageProvider(dirName, true/*clean*/)
var providerTester *StorageProviderTester= CreateStorageProviderTester(provider)
var objectStorageTester *StorageProviderTester= CreateStorageProviderTester(provider)

func TestProviderSimpleStorage(t *testing.T) {
	providerTester.TestSimpleStorage(t);
}
func TestObjectStoreSimpleStorage(t *testing.T) {
	objectStorageTester.TestSimpleStorage(t);
}
	
func TestProviderCreateMulti(t *testing.T) {
	providerTester.TestCreateMulti(t);
}
func TestObjectStoreCreateMulti(t *testing.T) {
	objectStorageTester.TestCreateMulti(t);
}
	
func TestProviderCreateHardcore(t *testing.T) {
	providerTester.TestCreateHardcore(t);
}
func TestObjectStoreCreateHardcore(t *testing.T) {
	objectStorageTester.TestCreateHardcore(t);
}
	
func TestProviderConcurrentListChildren(t *testing.T) {
	providerTester.TestConcurrentStoreAPIListChildren(t);
}
func TestObjectStoreConcurrentListChildren(t *testing.T) {
	objectStorageTester.TestConcurrentStoreAPIListChildren(t);
}