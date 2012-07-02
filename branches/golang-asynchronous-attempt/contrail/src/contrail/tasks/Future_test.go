package tasks

import (
	"testing"
	"time"
)

func TestBasicFutures(t *testing.T) {
	future:= CreateFuture()
	go func() {
		time.Sleep(time.Second)
		future.SetSuccess("burgers are ready")
	}()
	future.Join() // wait for result
	if !future.Done() { t.Errorf("future should be done") }
	if !future.Success() { t.Errorf("future should indicate that it succeeded") }
	result:= future.Result()
	if "burgers are ready" != result.(string) { t.Errorf("didn't get the result") }
}

func TestErrors(t *testing.T) {
	future:= CreateFuture()
	go func() {
		time.Sleep(time.Second)
		future.SetError("burgers are burnt")
	}()
	future.Join() // wait for result
	if !future.Done() { t.Errorf("future should be done") }
	if future.Cancelled() { t.Errorf("future indicates that it was cancelled") }
	if future.Success() { t.Errorf("future should indicate that it failed") }
	result:= future.Error()
	if "burgers are burnt" != result.(string) { t.Errorf("didn't get the error") }
	
	var gotPanic interface{}= nil
	func() {
		defer func() { 
			gotPanic= recover() 
		}()		
		future.Get()
	}() 
	if gotPanic == nil { t.Errorf("Future.Get() should panic when future is completed with an error") }
}

func TestCompletionListeners(t *testing.T) {
	future:= CreateFuture()
	complete:= make(chan string, 1) 
	future.OnComplete(func(*Future) {
		complete<-"let's eat"
	})
	go func() {
		time.Sleep(time.Second)
		future.SetError("burgers are ready")
	}()
	future.Join() // wait for result
	msg:= <-complete
	if "let's eat" != msg { t.Errorf("handler failed") }
}
