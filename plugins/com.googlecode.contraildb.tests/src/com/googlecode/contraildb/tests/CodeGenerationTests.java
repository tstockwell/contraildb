/*******************************************************************************
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

import com.googlecode.contraildb.core.async.IResult;
import com.googlecode.contraildb.core.async.IResultHandler;


/**
 * Basic Kilim tests for verifying Kilim features and runtime characteristics.
 * 
 * @author Ted Stockwell
 */
public class CodeGenerationTests extends ContrailTestCase {
	
	public interface Result<V> {
		

	    /**
	     * Waits if necessary for the computation to complete, and then
	     * retrieves its result.
	     * 
	     * @return the computed result
	     * @throws An unchecked exception if an error occurred while producing the result
	     */
	    public V get();
	    
	    
	}


	static public abstract class ResultHandler<V> {
		public void complete(V result) { }
		abstract void onComplete(V result);
	}
	
	
	static class Task<T> {
		protected T run() throws Exception {
			return null;
		}

		public Result<T> execute() {
			// TODO Auto-generated method stub
			return null;
		};
	}
	
	static class CPSClass {
		
		public void doSomething(final String text, final ResultHandler<String> handler) throws Exception {
			getBytes(text, new ResultHandler<byte[]>() {
				void onComplete(byte[] result) {
					handler.complete(new String(result));
				}
			});
		}
		public void getBytes(final String text, ResultHandler<byte[]> handler) {
			handler.complete(text.getBytes());
		}
	}
	static class NormalClass {
		
		public void doSomething(String text) throws Exception {
			new String(getBytes(text));
		}
		public byte[] getBytes(String text) {
			return text.getBytes();
		}
	}
}
