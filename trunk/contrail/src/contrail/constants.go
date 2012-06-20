package contrail

import "time"
	
/**
 * A reserved property name used to refer to the ID of an item. 
 */
const KEY_ID string = "__ID__";

/**
 * A reserved property name used to refer to the type of an item. 
 */
const KEY_KIND string = "__KIND__";

/**
 * The maximum number of milliseconds that a session may be active 
 */
const SESSION_MAX_ACTIVE= time.Second*31

