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

import kilim.Pausable;
import kilim.Task;

import com.googlecode.contraildb.core.Identifier;
import com.googlecode.contraildb.core.async.ContrailAction;
import com.googlecode.contraildb.core.async.ContrailTask;
import com.googlecode.contraildb.core.async.Operation;
import com.googlecode.contraildb.core.async.TaskDomain;
import com.googlecode.contraildb.core.storage.provider.IStorageProvider;


/**
 * Basic tests for Contrail Async API.
 * 
 * @author Ted Stockwell
 */
public class ContrailTasksTests extends ContrailTestCase {

	public void testBasicTask() throws Throwable {
		runTest(new Task() {
			final String expected= "boogity";
			public void execute() throws Pausable, Exception {
				String actual= new ContrailTask<String>() {
					protected String run() throws Pausable, Exception {
						return expected;
					}
				}.submit().get();
				assertEquals(expected, actual);
			}
		});
	}
	

	public void testBasicTaskDomain() throws Throwable {
		runTest(new Task() {
			final private TaskDomain _tracker= new TaskDomain();
			final private TaskDomain.Session _trackerSession= _tracker.beginSession();
			public void execute() throws Pausable, Exception {
				Identifier id= Identifier.create();
				_trackerSession.submit(new ContrailAction(id, Operation.WRITE) {
					protected void action() throws Pausable, Exception {
						System.out.println("write");
					}
				});
				_trackerSession.submit(new ContrailAction(id, Operation.FLUSH) {
					protected void action() throws Pausable, Exception {
						System.out.println("flush");
					}
				});
				_trackerSession.submit(new ContrailAction(id, Operation.READ) {
					protected void action() throws Pausable, Exception {
						System.out.println("read");
					}
				});
				_trackerSession.complete().get();
			}
		});
	}
	
}
