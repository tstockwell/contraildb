package com.googlecode.contraildb.tests;

import junit.framework.TestCase;
import kilim.Pausable;

public class ReproduceVerifyError extends TestCase {
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
	
	public void testVerifyError() {
		/*
		 * This line will throw the following error at runtime...
		 * java.lang.VerifyError: (class: com/googlecode/contraildb/tests/BadClass, method: doSomething signature: (Lkilim/Fiber;)V) Mismatched stack types
		 *   at com.googlecode.contraildb.tests.ReproduceVerifyError.testVerifyError(ReproduceVerifyError.java:8)
		 */
		new BadClass();
	}
}
