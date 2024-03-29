package id

import (
 "testing"
 "strconv"
 ct "contrail/util/testing"
)

func TestBasicIdentifiers(t *testing.T) {
    var id *Identifier= UniqueIdentifier()
    ct.AssertNotNull(t, id, "Failed to generate a random id")

    var id2 *Identifier= CreateIdentifier(id.Path())
    ct.AssertEquals(t, id, id2, "Identifier was not cached")
        
    var id3 *Identifier= id.Child("giggity")
    ct.AssertEquals(t, id3.Parent(), id, "Wrong parent")
        
    ct.AssertEquals(t, id.Path()+"/giggity", id3.Path(), "Incorrect path")
  
}

func TestMultiIdentifiers(t *testing.T) {

	for i:= 0; i< 10000; i++ {
    	CreateIdentifier("id-"+strconv.Itoa(i))
	}
}

