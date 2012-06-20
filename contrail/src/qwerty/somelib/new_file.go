package somelib

import (

)

type someType struct {
}

func CreateSomeType() *someType {
	return new(someType)
}

