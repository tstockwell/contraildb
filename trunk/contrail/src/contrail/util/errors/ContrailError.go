package errors

import (
	"runtime"
	"runtime/debug"
)

/**
 * A ContrailError is a wrapper for native Go errors that also capture a  
 * stack trace to associate with the error.
 * The idea of a ContrailError is that it maintains the statck associated with 
 * an error in the way similar to Java exceptions
 */  
type ContrailError struct {
	err runtime.Error // the original error
	stack []byte    // stack trace captured with original 
} 

func CreateError(originalError error) ContrailError {
	if ce, ok := originalError.(ContrailError); ok {
		return ce 
	}
	return ContrailError {
		err:  originalError,
		stack:  debug.Stack(), 
	} 
}

func (self ContrailError) Error() string {
	return self.Error() + "\n" + string(stack) 
} 