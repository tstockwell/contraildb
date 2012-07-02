package id

import (
	"strings"
	"sync"
	"contrail/util/uuid"
	"contrail/util/lru"
	"contrail/util/errors"
)

/**
 * Identifies an item in a hierarchy of items.
 * 
 * The name of an item may not contain the '/' character.
 * Names are separated by '/' characters to form hierarchies.
 *
 * @author Ted Stockwell
 */
type Identifier struct {
	completePath string;
	ancestors []*Identifier;
	name string;
}

var cache *lru.Cache = lru.New(1000);
var lock *sync.Mutex= new(sync.Mutex)
var empty_ancestors []*Identifier = []*Identifier{};

func getCached(path string) *Identifier {

	lock.Lock()
	defer lock.Unlock()
	id, ok := cache.Get(path)
	if ok {
		return id.(*Identifier)
	}
	return nil
}


func CreateIdentifier(path string) *Identifier {
	path= strings.Trim(path, "/")
	if id, ok := cache.Get(path); ok {
		return id.(*Identifier)
	}
	
	var id *Identifier= new(Identifier)
	id.completePath= path;
	
	i:= strings.LastIndex(path, "/")
	if 0 <= i {
		id.name= path[i+1:]
		var parent *Identifier= CreateIdentifier(path[:i])
		id.ancestors= append(parent.ancestors, parent)
	} else { 
		id.name= path
		id.ancestors= empty_ancestors
	}
	
	cache.Add(path, id)
	
	return id
}


/**
 * Create a unique Identifier with a random UUID for the path 
*/
func UniqueIdentifier() *Identifier {
	uuid, err:= uuid.GenUUID()
	if err == nil {
		return CreateIdentifier(uuid)
	}
	panic(errors.CreateError(err))
}

func (this *Identifier) Path() string {
	return this.completePath;
}

func (this *Identifier) Name() string {
	return this.name;
}

/**
 * Create/Get a child Identifier
 */ 
func (parent *Identifier) Child(name string) *Identifier {
	if parent == nil {
		return CreateIdentifier(name)
	}
	return CreateIdentifier(parent.completePath+"/"+name)
}

func (this *Identifier) Parent() *Identifier {
	if len(this.ancestors) <= 0 {
		return nil
	}
	return this.ancestors[len(this.ancestors)-1]
}

