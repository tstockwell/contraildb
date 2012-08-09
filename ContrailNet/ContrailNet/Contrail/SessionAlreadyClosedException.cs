using System;

namespace Contrail
{
	public class SessionAlreadyClosedException : ContrailException
	{
	
		public SessionAlreadyClosedException () : base("Session is already closed")
		{
		}
	}

}