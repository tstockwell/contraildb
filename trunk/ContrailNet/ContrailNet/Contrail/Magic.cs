using System;

namespace Contrail {

	public interface Magic {
		/**
		 * The maximum number of milliseconds that a session may be active 
		 */
		public static readonly long SESSION_MAX_MILLIS= 1000*31;
	}

}