/*******************************************************************************
 * Copyright (c) 2009 Ted Stockwell
 * 
 * This file is part of the Contrail Database System.
 * 
 * Contrail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License Version 3
 * as published by the Free Software Foundation.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.googlecode.contraildb.core;

import com.googlecode.contraildb.core.impl.ContrailServiceImpl;
import com.googlecode.contraildb.core.storage.provider.IStorageProvider;


public class ContrailServiceFactory {
	
	/**
	 * @param storageProvider 
	 * 		A low-level storage provider that provides access to a Contrail database.  
	 */
	public static IResult<? extends IContrailService> getContrailService(IStorageProvider storageProvider) {
		return ContrailServiceImpl.create(storageProvider);
	}
}
