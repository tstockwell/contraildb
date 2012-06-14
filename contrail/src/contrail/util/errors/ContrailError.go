package errors

import (
	"fmt"
	"runtime/debug"
)

/**
 * A ContrailError is a wrapper for native Go errors that also capture a  
 * stack trace to associate with the error.
 * The idea of a ContrailError is that it maintains the statck associated with 
 * an error in the way similar to Java exceptions
 */  
type ContrailError struct {
	err interface{} // the original error
	stack []byte    // stack trace captured with original 
} 

func CreateError(originalError interface{}) ContrailError {
	if ce, ok := originalError.(ContrailError); ok {
		return ce 
	}
	return ContrailError {
		err:  originalError,
		stack:  debug.Stack(), 
	} 
}

func (self ContrailError) String() string {
	return self.Error()
}

func (self ContrailError) Error() string {
	msg:= ""	
	if ce, ok := self.err.(error); ok {
		msg= ce.Error() 
	}
	if stringer, ok := self.err.(fmt.Stringer); ok {
		msg= stringer.String()
	}
	// this is typical of Go - string does not implementer Stringer - WTF
	if s, ok := self.err.(string); ok {
		msg= s 
	}
	msg+= "\n" + string(self.stack)
	return msg 
} 