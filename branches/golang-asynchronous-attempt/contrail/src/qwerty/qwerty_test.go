package qwerty

import (
	"fmt"
	"testing"
	"qwerty/somelib"
)

func TestSomething(t *testing.T) {
	somelib.someType= CreateSomeType()

	f:= 112999
	fmt.Printf("f=%d\n",f) 
	fmt.Printf("f="+string(f)+"\n") 
}
