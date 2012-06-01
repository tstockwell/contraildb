package id

import (
 "testing"
 ct "contrail/testing"
)

func TestBasicIdentifiers(t *testing.T) {
    var id *Identifier= Unique()
    ct.AssertNotNull(id, "Failed to generate a random id")

    var id2 *Identifier= Create(id.Path())
    ct.AssertEquals(id, id3, "Identifier was not cached")
        
    var id3 *Identifier= id.Child("giggity")
    ct.AssertEquals(id3.Parent(), id, "Wrong parent")
        
    ct.AssertEquals(id.Path()+"/giggity", id3.Path(), "Incorrect path")
}
