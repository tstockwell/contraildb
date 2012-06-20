package tasks

import (

)

/**
 * A simple utility class for executing functions and waiting for the results.
 */ 
type TaskPool struct  {
	futures []*Future		
}

func CreateTaskPool() *TaskPool {
	return &TaskPool { futures: make([]*Future,0) }
}

func (self *TaskPool) Go(task func()) { 
	futures= append(futures, Go(task))
}


func (self *TaskPool) JoinAll(task func()) { 
	JoinAll(futures)
}