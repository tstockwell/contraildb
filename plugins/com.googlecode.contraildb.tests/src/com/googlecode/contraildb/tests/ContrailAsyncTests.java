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
import com.googlecode.contraildb.core.async.Handler;
import com.googlecode.contraildb.core.async.Series;
import com.googlecode.contraildb.core.async.TaskUtils;
import com.googlecode.contraildb.core.async.TryFinally;
import com.googlecode.contraildb.core.async.WhileHandler;


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
		Series series= new Series(
			new Handler() {
				protected IResult onSuccess() {
					result[0]+= "1";
					return TaskUtils.DONE;
				}
			},
			new Handler() {
				protected IResult onSuccess() {
					result[0]+= "2";
					return TaskUtils.DONE;
				}
			},
			new Handler() {
				protected IResult onSuccess() {
					result[0]+= "3";
					return TaskUtils.DONE;
				}
			});
		series.get();
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
		final String[] result= new String[] { "" };
		TryFinally handler= new TryFinally() {
			protected IResult doTry() throws Exception {
				result[0]+= "try";
				throw new RuntimeException("some error");
			}
			
			@Override
			protected IResult doFinally() throws Exception {
				result[0]+= "-finally";
				return TaskUtils.DONE;
			}
		};
		handler.get();
		assertEquals("try-finally", result[0]);
		assertFalse(handler.isSuccess());
	}
}
