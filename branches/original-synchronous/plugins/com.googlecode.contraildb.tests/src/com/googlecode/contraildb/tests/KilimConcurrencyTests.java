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

import kilim.Mailbox;
import kilim.Pausable;
import kilim.Task;

import com.googlecode.contraildb.core.utils.TestTask;

/**
 * Basic Kilim tests for verifying Kilim features and runtime characteristics.
 * 
 * @author Ted Stockwell
 */
public class KilimConcurrencyTests extends ContrailTestCase {
	static class BadClass {
		
		public void doSomething() throws Pausable ,Exception {
			new String(getBytes());
		}
		public byte[] getBytes() throws Pausable {
			return "lkj;lkjad;fa".getBytes();
		}
	}
	static class GoodClass {
		
		public void doSomething() throws Pausable, Exception {
			byte[] bytes= getBytes();
			new String(bytes);
		}
		public byte[] getBytes() throws Pausable {
			return "lkj;lkjad;fa".getBytes();
		}
	}
	static class NormalClass {
		
		public void doSomething() throws Exception {
			new String(getBytes());
		}
		public byte[] getBytes() {
			return "lkj;lkjad;fa".getBytes();
		}
	}

	public static class Chain extends Task {
		Mailbox<String> inBox, outBox;

		public Chain(Mailbox<String> in, Mailbox<String> out) {
			inBox = in;
			outBox = out;
		}

		public void execute() throws Pausable {
			String sb = inBox.get();
			outBox.put(sb + "x");
		}
	}
	
	public void testSimpleContinuation() throws Throwable {
		
		runTest(new Task() {
			public void execute() throws Pausable {
				System.out.println("main started");
				int n = 10;
				Mailbox<String> first = new Mailbox<String>();
				Mailbox<String> in = first;
				Mailbox<String> out= null;
				for (int i = 0; i < n; i++) {
					out = new Mailbox<String>();
					new Chain(in, out).start();
					in = out;
				}
				first.put("");
				String result = out.get();
				assertTrue(result.length() == n);
			}
		});
	}

	/**
	 * Once upon a time, in a workspace far far away, this test failed 
	 * because the TestTask class was not exported from the correct folder 
	 * in the com.googlecode.contraildb.core plugin.
	 */
	public void testExportedTask() throws Throwable {
		
		runTest(new TestTask());
	}
	
	
	public void testVerifyError() {
		/*
		 * This line will throw the following error at runtime...
		 * java.lang.VerifyError: (class: com/googlecode/contraildb/tests/BadClass, method: doSomething signature: (Lkilim/Fiber;)V) Mismatched stack types
		 *   at com.googlecode.contraildb.tests.ReproduceVerifyError.testVerifyError(ReproduceVerifyError.java:8)
		 */
		new BadClass();
	}
	public void testGoodClass() throws Exception {
		runTest(new Task() {
			public void execute() throws Pausable, Exception {
				new GoodClass().doSomething();
			}
		});
	}
	
	

	public void testSleep() throws Throwable {
		runTest(new Task() {
			public void execute() throws Pausable, Exception {
				long start= System.currentTimeMillis();
				Task.sleep(1000);
				long time= System.currentTimeMillis() - start;
				assertTrue("ContrailTask.sleep didnt sleep long enough, only slept for "+time+" milliseconds", start+1000 <= System.currentTimeMillis());
			}
		});
	}
	
}
