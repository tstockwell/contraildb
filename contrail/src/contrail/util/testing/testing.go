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


func AssertNull(t *testing.T, value interface{}, message string) {
	if value != nil {
    	t.Errorf("Value is not null.  %s", message)
	} 
}

func AssertTrue(t *testing.T, value bool, message string) {
	if !value {
    	t.Errorf("Value is false.  %s", message)
	} 
}
