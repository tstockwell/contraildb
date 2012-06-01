package testing

import (
 "testing"
)

func AssertEquals(t *testing.T, expected *interface{}, actual *interface{}, message string) {
	if expected != actual {
    	t.Errorf("%s.  Expected %s, was %s", message, expected, actual)
	} 
}