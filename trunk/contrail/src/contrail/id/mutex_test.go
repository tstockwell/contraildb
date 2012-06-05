package id

import (
	"sync"
 	"testing"
)

type Lockable struct {
	lock *sync.Mutex
}

func CreateLockable() *Lockable {
	return &Lockable { lock:&sync.Mutex{} }
}

func (this *Lockable) DoSomething() {
	this.lock.Lock()
	defer this.lock.Unlock()
}


func TestMutex(t *testing.T) {
    lockable:= CreateLockable()
    lockable.DoSomething()
}
