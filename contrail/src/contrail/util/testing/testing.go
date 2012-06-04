package testing
/**
 * Convenience utilities for testing
 */


import (
 "testing"
)

func AssertEquals(t *testing.T, expected interface{}, actual interface{}, message string) {
	if expected != actual {
    	t.Errorf("%s.  Expected %s, was %s", message, expected, actual)
	} 
}


func AssertNotNull(t *testing.T, value interface{}, message string) {
	if value == nil {
    	t.Errorf("Value is null.  %s", message)
	} 
}
