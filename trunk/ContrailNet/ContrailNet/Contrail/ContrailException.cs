using System;

namespace Contrail
{
	public class ContrailException : Exception
	{
		public ContrailException (String message, Exception cause) : base(message, cause)
		{
		}

		public ContrailException (String message) : base(message)
		{
		}
	}
}