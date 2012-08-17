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

import java.io.IOException;
import java.util.Collection;
import java.util.List;


/**
 * High level API to a Contrail database.
 * 
 * @author Ted Stockwell
 *
 */
public interface IContrailService {

	public static enum Mode {
		READONLY,
		READWRITE
	}

	/**
	 * A session must be created in order to read or write to the database.
	 * Begin either a readonly or readwrite session.
	 * A readonly session is only allowed to get Records and run queries, readwrite 
	 * sessions may also insert, update, and delete Records. 
	 * When a readwrite session is started the session will be associated with a new revision of the database.
	 * When a readonly session is started the session will be associated with the last committed database revision. 
	 */
	public IContrailSession beginSession(Mode mode) 
		throws IOException, ContrailException;

	/**
	 * Begin a new readonly session associated with the given database revision.
	 */
	public IContrailSession beginSession(long revisionNumber) 
		throws IOException, ContrailException;

	public Collection<IContrailSession> getActiveSessions()
		throws IOException, ContrailException;
	
	/**
	 * Returns the numbers of all available revisions.
	 * Contrail tracks sessions that are reading from past revisions and automatically 
	 * deletes old revisions when they are no longer in use (except for the most recently 
	 * committed revision).
	 * This method returns a list of the revision numbers of revisions that are still being used or have not yet been cleaned up.
	 * 
	 * @return A list of revision numbers in descending order
	 */
	public List<Long> getAvailableRevisions()
		throws IOException, ContrailException;

	public void close() 
		throws IOException, ContrailException;
	
}
