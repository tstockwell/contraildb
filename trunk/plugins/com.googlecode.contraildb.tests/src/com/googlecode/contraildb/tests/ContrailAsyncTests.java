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

import com.googlecode.contraildb.core.IResult;
import com.googlecode.contraildb.core.async.If;
import com.googlecode.contraildb.core.async.Series;
import com.googlecode.contraildb.core.async.TaskUtils;
import com.googlecode.contraildb.core.async.TryFinally;
import com.googlecode.contraildb.core.async.WhileHandler;
import com.googlecode.contraildb.core.async.init;
import com.googlecode.contraildb.core.async.seq;


/**
 * Test the Contrail async utilities.
 * 
 * @author Ted Stockwell
 */
@SuppressWarnings("rawtypes")
public class ContrailAsyncTests extends TestCase {
	
	/**
	 * A simple test to make sure that tasks get run sequentially by the Series 
	 * class.
	 */
	public void testSeries() {
		final String[] result= new String[] { "" };
		
		new Series() {
			@init("addTwo") String initTest;
			
			void start() {
				result[0]+= "1";
			}
			@seq("start") IResult addTwo() {
				result[0]+= "2";
				return TaskUtils.asResult("init-test");
			}
			@seq("addTwo") void addThree() {
				assertEquals(initTest, "init-test");
				result[0]+= "3";
			}
		}.get();
		assertEquals("123", result[0]);
	}
	
	public void testWhile() {
		final String[] result= new String[] { "" };
		WhileHandler wile= new WhileHandler() {
			int i= 10;
			protected IResult<Boolean> While() throws Exception {
				return TaskUtils.asResult(0 < i--);
			}
			
			protected IResult<Void> Do() throws Exception {
				result[0]+= i;
				return TaskUtils.DONE;
			}
		};
		wile.get();
		assertEquals("9876543210", result[0]);
	}
	
	public void testTryFinally() {
		TryFinally handler= new TryFinally() {
			String result= "";
			void doTry() {
				result+= "try";
				throw new RuntimeException("some error");
			}
			String doFinally() {
				return result+= "-finally";
			}
		};
		handler.join();
		assertEquals("try-finally", handler.getResult());
		
		// the doTry method above throws an exception, therefore the result should no be successful
		assertFalse(handler.isSuccess());
		
		// the result's error should be the error thrown in the doTry method
		assertEquals("some error", handler.getError().getMessage());
	}
	
	public void testIf() {
		final String[] result= new String[] { "" };
		If handler= new If(true) {
			protected IResult doTrue() throws Exception {
				result[0]+= "true";
				return TaskUtils.DONE;
			}
			protected IResult doFalse() throws Exception {
				result[0]+= "false";
				return TaskUtils.DONE;
			}
		};
		handler.join();
		assertEquals("true", result[0]);
		
		handler= new If(false) {
			protected IResult doTrue() throws Exception {
				result[0]+= "true";
				return TaskUtils.DONE;
			}
			protected IResult doFalse() throws Exception {
				result[0]+= "false";
				return TaskUtils.DONE;
			}
		};
		
		handler.join();
		assertEquals("truefalse", result[0]);
	}
}
