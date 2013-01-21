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
package com.googlecode.contraildb.tests;

import junit.framework.TestCase;
import kilim.ExitMsg;
import kilim.Task;

import com.googlecode.contraildb.core.async.ContrailTask;

/**
 * Base class for Contrail tests.
 * 
 * @author Ted Stockwell
 */
@SuppressWarnings("rawtypes")
public class ContrailTestCase extends TestCase {

	protected Object runTest(Task task) throws Exception {
		task.start();
		ExitMsg exitMsg= task.joinb();
		
		Object result= null;
        if (exitMsg == null) {
            fail("Timed Out");
        } else {
            Object res = exitMsg.result;
            if (res instanceof Exception) {
            	throw (Exception)res;
            }
            if (res instanceof Error)
            	throw (Error)res;
            if (res instanceof Throwable)
            	throw new RuntimeException("test failed with some wierd kind of exception type", (Throwable)res);
            result= res;
        }
        return result;
	}
	
	protected void runTest(ContrailTask task) {
		task.submit().getb();
	}

}