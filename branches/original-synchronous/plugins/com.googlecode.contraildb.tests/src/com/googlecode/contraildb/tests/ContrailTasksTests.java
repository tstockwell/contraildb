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

import java.util.concurrent.atomic.AtomicInteger;

import kilim.Pausable;
import kilim.Task;

import com.googlecode.contraildb.core.Identifier;
import com.googlecode.contraildb.core.async.ContrailAction;
import com.googlecode.contraildb.core.async.ContrailTask;
import com.googlecode.contraildb.core.async.Operation;
import com.googlecode.contraildb.core.async.TaskDomain;


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
			final AtomicInteger count= new AtomicInteger();
			public void execute() throws Pausable, Exception {
				Identifier id= Identifier.create();
				_trackerSession.submit(new ContrailAction(id, Operation.WRITE) {
					protected void action() throws Pausable, Exception {
						ContrailTask.sleep(50);
						count.incrementAndGet();
					}
				});
				_trackerSession.submit(new ContrailAction(id, Operation.FLUSH) {
					protected void action() throws Pausable, Exception {
						assertEquals(1, count);
						ContrailTask.sleep(50);
						count.incrementAndGet();
					}
				});
				_trackerSession.submit(new ContrailAction(id, Operation.READ) {
					protected void action() throws Pausable, Exception {
						assertEquals(2, count);
						ContrailTask.sleep(50);
						count.incrementAndGet();
					}
				});
				_trackerSession.complete().get();
				assertEquals(3, count);
			}
		});
	}

	public void testSleep() throws Throwable {
		runTest(new ContrailAction() {
			@Override
			protected void action() throws Pausable, Exception {
				long start= System.currentTimeMillis();
				ContrailTask.sleep(100);
				assertTrue("ContrailTask.sleep didnt sleep long enough", start+100 < System.currentTimeMillis());
			}
		});
	}
	
}
