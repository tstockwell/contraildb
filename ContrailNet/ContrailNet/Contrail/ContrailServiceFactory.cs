using System;

namespace Contrail
{
	// Factory for getting a Contrail instance
	public static class ContrailServiceFactory
	{
	
		/**
	 	 * @param storageProvider - A low-level storage provider.  
	 	 */
		public static IContrailService getContrailService (IStorageProvider storageProvider)
		{
			try {
				return new ContrailServiceImpl (storageProvider);
			} catch (Exception e) {
				throw new ContrailException ("Error creating contrail service:" + e.getMessage (), e);
			}
		}
	}

}