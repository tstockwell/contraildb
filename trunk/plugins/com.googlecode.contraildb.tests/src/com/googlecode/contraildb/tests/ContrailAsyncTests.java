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

import com.googlecode.contraildb.core.IResult;
import com.googlecode.contraildb.core.async.Block;
import com.googlecode.contraildb.core.async.Series;
import com.googlecode.contraildb.core.async.TaskUtils;

import junit.framework.TestCase;


/**
 * Test the Contrail async utilities.
 * 
 * @author Ted Stockwell
 */
public class ContrailAsyncTests extends TestCase {
	
	/**
	 * A simple test to make sure that tasks get run sequentially by the Series 
	 * class.
	 */
	@SuppressWarnings("rawtypes")
	public void testSeries() {
		final String[] result= new String[] { "" };
		Series series= new Series(
			new Block() {
				protected IResult onSuccess() {
					result[0]+= "1";
					return TaskUtils.DONE;
				}
			},
			new Block() {
				protected IResult onSuccess() {
					result[0]+= "2";
					return TaskUtils.DONE;
				}
			},
			new Block() {
				protected IResult onSuccess() {
					result[0]+= "3";
					return TaskUtils.DONE;
				}
			});
		series.run().get();
		assertEquals("123", result[0]);
	}
}
