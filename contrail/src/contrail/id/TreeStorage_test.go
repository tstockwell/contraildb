package id

import (
 "testing"
)

func TestBasicTreeStorage(t *testing.T) {
    storage:= CreateTreeStorage()
	if storage == nil { t.Errorf("Failed to generate storage") }
    
    id:= CreateIdentifier("group/name")
    storage.Store(id, "one")
    if !storage.Exists(id){ t.Errorf("Store failed") }
    if storage.Size() != 1 { t.Errorf("Store failed") }
    storage.Store(id.Parent(), "two")
    if !storage.Exists(id.Parent()){ t.Errorf("Store failed") }
    if storage.Size() != 2 { t.Errorf("Store failed") }
    
    found:= false
    storage.VisitNode(id, func(identifier *Identifier, content interface{}) {
    	found= true
    })
    if !found { t.Errorf("Visit failed") }
    
    one:= storage.Fetch(id)
	if "one" != one { t.Errorf("Fetch failed") }

    children:= storage.ListChildren(id.Parent())
	if len(children) != 1 { t.Errorf("ListChildren failed") }
	if children[0] != id { t.Errorf("ListChildren failed") }
    
    childStorage:= storage.FetchChildren(id.Parent())
	if childStorage.Size() != 1 { t.Errorf("FetchChildren failed") }
	if "one" != childStorage.Fetch(id) { t.Errorf("FetchChildren failed") }
    
    storage.Delete(id)
    if storage.Size() != 1 { t.Errorf("Delete failed") }
}




func TestVisitParents(t *testing.T) {
    storage:= CreateIdStorage()
	parent:= UniqueIdentifier()
    storage.Store(parent, "parent")
	child:= parent.Child("boogity")
    //storage.Store(child, "child")
	
	var foundParent *Identifier;
	visitor:= func (nodeId *Identifier, content interface{}) {
		foundParent= nodeId
	}

	// look up the tree for deletes
	storage.VisitParents(child, visitor)
	if foundParent != parent { t.Errorf("VisitParents failed") }
}

