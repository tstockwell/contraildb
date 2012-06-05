package id

import (
 "testing"
 ct "contrail/util/testing"
)

func TestBasicIdStorage(t *testing.T) {
    storage:= CreateIdStorage()
	if err := quick.Check(f, nil); err != nil {
		t.Error(err)
	}
	    ct.AssertNotNull(t, storage, "Failed to generate storage")
    
    id:= CreateIdentifier("group/name")
    storage.Store(id, "one")
    storage.Store(id.Parent(), "two")
    
    one:= storage.Fetch(id)
    ct.AssertEquals(t, "one", one, "Fetch failed")

    ct.AssertTrue(t, storage.Exists(id), "Exists failed")
    
    children:= storage.ListChildren(id.Parent())
    ct.AssertEquals(t, 1, len(children), "ListChildren failed")
    
    childStorage:= storage.FetchChildren(id.Parent())
    ct.AssertEquals(t, "one", childStorage.Fetch(id), "FetchChildren failed")
    
    storage.Delete(id)
    ct.AssertNull(t, childStorage.Fetch(id), "FetchChildren failed")
    
    storage.Store(id, "one")
    one= storage.Fetch(id)
    ct.AssertEquals(t, "one", one, "Fetch failed")
}
