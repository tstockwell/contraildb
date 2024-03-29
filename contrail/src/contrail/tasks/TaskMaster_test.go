package tasks

import (
	"contrail/id"
	"testing"
	"time"
)

func TestPanics(t *testing.T) {
	taskMaster:= CreateTaskMaster()
	id:= id.UniqueIdentifier()
	future1:= taskMaster.Submit(DELETE, id, func() interface{} {
		panic ("in Detroit")
		return nil
	})
	future1.Join()
	if (future1.Success()) { t.Errorf("The returned future should indicate that an error occurred") }
	if (future1.Error() != "in Detroit") { t.Errorf("The returned future did not return the associated error") }
}

//Here are the rules for scheduling tasks...
// * A DELETE operation on an object may not proceed until all pending operations 
// * on the associated object or any of its descendants have completed.
// * However, DELETE operations on an object are NOT blocked by CREATE operations 
// * on the same object.    
// * A DELETE operation blocks any subsequent operations on an object and all its 
// * descendants until the delete has completed.
// * 
// * A READ operation on an object may not proceed until all pending WRITE and 
// * CREATE operations on that object have completed.  
// * 
// * A LIST operation on an object may not proceed until all pending WRITE and 
// * CREATE operations of any children have completed.  
// * 
// * A WRITE operation on an object may not proceed until all pending READ, WRITE, 
// * and CREATE operations on that object have completed.  
// * 
// * A CREATE operation on an object may not proceed until all pending READ, 
// * and WRITE operations on that object have completed.  Since the purpose of 
// * the StorageProvider.Create method is to coordinate asynchronous CREATE 
// * requests CREATE requests do not have to wait for other CREATE requests.   
func TestContentionOnIdentifier(t *testing.T) {
	taskMaster:= CreateTaskMaster()
	id:= id.UniqueIdentifier()
	flag:= ""
	start:= time.Now()
	
	if !IsDependentTask(DELETE, DELETE){ t.Errorf("a delete may not proceed until other deletes of the same id are completed"); return }
	
	future1:= taskMaster.Submit(DELETE, id, func() interface{} {
		flag= "1"
		t.Logf("flag = 1\n")
		time.Sleep(time.Second)
		if flag != "1" {
			msg:= "looks like the 2nd func ran before the first func finished"
			t.Errorf(msg)
			panic (msg)
		}		
		flag= "1.1"
		t.Logf("flag = 1.1\n")
		return nil
	})
	
	dependentTasks:= taskMaster.findPendingTasks(DELETE, id)
	if (len(dependentTasks) <= 0) { t.Errorf("Failed to find dependent tasks"); return }
	
	future2:= taskMaster.Submit(DELETE, id, func() interface{} {
		if flag != "1.1" {
			msg:= "looks like the 2nd func ran before the first func finished"
			t.Errorf(msg)
			panic (msg)
		}		
		flag= "2"
		t.Logf("flag = 2\n")
		return nil
	})
	future2.Join()
	if (!future1.Done()) { t.Errorf("the 2nd function somehow completed before the first function"); return }
	if (!future1.Success()) { t.Error(future1.Error()); return}
	if (!future2.Success()) { t.Error(future2.Error()); return}
	if (flag != "2") { t.Errorf("looks like the 2nd function has not finished yet"); return}
	
	if time.Since(start) < time.Second {
		t.Errorf("Since the first func waits for a second it should take at least a second to run this test")
		return 
	}
}


func TestContentionOnDecendant(t *testing.T) {
	taskMaster:= CreateTaskMaster()
	id:= id.UniqueIdentifier()
	decendent:= id.Child("boogity")
	flag:= ""
	start:= time.Now()
	
	future1:= taskMaster.Submit(DELETE, decendent, func() interface{} {
		flag= "1"
		time.Sleep(time.Second)
		if flag != "1" {
			msg:= "looks like the 2nd func ran before the first func finished"
			t.Errorf(msg)
			panic (msg)
		}		
		flag= "1.1"
		return nil
	})
	
	future2:= taskMaster.Submit(DELETE, id, func() interface{} {
		if flag != "1.1" {
			msg:= "looks like the 2nd func ran before the first func finished"
			t.Errorf(msg)
			panic (msg)
		}		
		flag= "2"
		return nil
	})
	future2.Join()
	if (!future1.Done()) { t.Errorf("the 2nd function somehow completed before the first function"); return }
	if (!future1.Success()) { t.Error(future1.Error()); return}
	if (!future2.Success()) { t.Error(future2.Error()); return}
	if (flag != "2") { t.Errorf("looks like the 2nd function has not finished yet"); return}
	
	if time.Since(start) < time.Second {
		t.Errorf("Since the first func waits for a second it should take at least a second to run this test")
		return 
	}
}



// A DELETE operation blocks any subsequent operations on an object and all its 
// descendants until the delete has completed.
func TestContentionOnParent(t *testing.T) {
	taskMaster:= CreateTaskMaster()
	parent:= id.UniqueIdentifier()
	decendent:= parent.Child("boogity")
	op:= DELETE
	
	future1:= taskMaster.Submit(DELETE, parent, func() interface{} {
		time.Sleep(time.Second)
		return nil
	})
	
	future2:= taskMaster.Submit(DELETE, decendent, func() interface{} {
		return nil
	})
	
	pendingTasks:= newTaskSet()
	visitor:= func (nodeId *id.Identifier, content interface{}) {
		t.Logf("visitor invoked\n") 
		tasksInProgress, _:= content.(tTaskSet) 
		if tasksInProgress != nil {
			t.Logf("tasksInProgress != nil\n") 
			for taskInProgress,_:= range tasksInProgress {
				if !taskInProgress.result.Done() && IsDependentTask(op, taskInProgress.op) {
					pendingTasks.add(taskInProgress)
				}
			}
		}
	}
	taskMaster.taskStorage.VisitParents(decendent, visitor)
	if len(pendingTasks) <= 0 { t.Errorf("len(pendingTasks) <= 0"); return } 

	pendingTasks= taskMaster.findPendingTasks(DELETE, decendent)
	if len(pendingTasks) <= 0 { t.Errorf("len(pendingTasks) <= 0"); return } 
	
	future2.Join()
	if !future1.Done() { t.Errorf("the DELETE operation on the parent should have blocked the operation on child "); return }
}

func TestJoin(t *testing.T) {
	taskMaster:= CreateTaskMaster()
	id:= id.UniqueIdentifier()
	decendent:= id.Child("boogity")
	
	future1:= taskMaster.Submit(DELETE, id, func() interface{} {
		time.Sleep(time.Second/2)
		return nil
	})
	
	future2:= taskMaster.Submit(DELETE, decendent, func() interface{} {
		time.Sleep(time.Second/2)
		return nil
	})
	taskMaster.Join()
	if (!future1.Done()) { t.Errorf("parent task not done yet"); return }
	if (!future2.Done()) { t.Errorf("child task not done yet"); return }
}
