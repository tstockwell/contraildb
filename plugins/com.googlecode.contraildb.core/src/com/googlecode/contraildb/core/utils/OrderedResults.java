package com.googlecode.contraildb.core.utils;

import java.util.ArrayList;

import com.googlecode.contraildb.core.IResult;

/**
 * A utility class for coordinating the invocation of ResultHandlers.
 * A OrderedResults produces IResults that complete in the order in which 
 * they are created.  
 * The next method completes the next result.
 * The complete method produces an IResult that will not complete until all 
 * the results produced so far have completed.
 * 
 * @author ted stockwell
 *
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class OrderedResults {
	
	private ArrayList<Result> _resultList= new ArrayList<Result>();

	synchronized public IResult<Boolean> create() {

		Result result= new Result();
		if (_resultList.isEmpty()) 
			result.success(true);
		_resultList.add(result);
		return result;
	}

	synchronized public IResult complete() {
		if (_resultList.isEmpty())
			return TaskUtils.DONE;
		return TaskUtils.combineResults(_resultList);
	}

	synchronized public void next() {
		while (!_resultList.isEmpty()) {
			Result result= _resultList.remove(0);
			if (!result.isDone()) {
				result.success(true);
				break;
			}
		}
	}

}
