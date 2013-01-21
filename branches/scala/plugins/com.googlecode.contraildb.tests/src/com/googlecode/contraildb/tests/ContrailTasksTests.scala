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


import org.junit._
import Assert._


/**
 * Basic tests for Contrail async API.
 * 
 * @author Ted Stockwell
 */
class ContrailTasksTests {

	@Test def testBasicTask() {
	  
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
						assertEquals(1, count.intValue());
						ContrailTask.sleep(50);
						count.incrementAndGet();
					}
				});
				_trackerSession.submit(new ContrailAction(id, Operation.READ) {
					protected void action() throws Pausable, Exception {
						assertEquals(2, count.intValue());
						ContrailTask.sleep(50);
						count.incrementAndGet();
					}
				});
				_trackerSession.complete().get();
				assertEquals(3, count.intValue());
			}
		});
	}

	public void testSleep() throws Throwable {
		runTest(new ContrailAction() {
			@Override
			protected void action() throws Pausable, Exception {
				long start= System.currentTimeMillis();
				ContrailTask.sleep(100);
				assertTrue("ContrailTask.sleep didnt sleep long enough", start+100 <= System.currentTimeMillis());
			}
		});
	}

	/**
	 * At one time there was a bug in the Contrail Async API such that 
	 * if a java.lang.Error was thrown from within a Task, then the associated 
	 * call to IResult.get(), to get the result of the task, did not throw an 
	 * exception.  This test makes sure that throwing a java.lang.Error causes 
	 * an exception to be thrown from the associate Task.get(). 
	 */
	public void testErrorHandling() {
		IResult<Void> result= new ContrailAction() {
			protected void action() throws Pausable, Exception {
				throw new java.lang.Error("catch this");
			}
		}.submit();
		boolean success= true;
		try {
			result.getb();
			success= false;
		}
		catch (Throwable t) {
			// if things are working then we should get here...
		}
		if (!success)
			fail("IResult.getb() should have thrown an exception");
	}
	
}
