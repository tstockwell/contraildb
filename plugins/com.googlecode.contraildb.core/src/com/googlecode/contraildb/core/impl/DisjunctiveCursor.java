package com.googlecode.contraildb.core.impl;

import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

import com.googlecode.contraildb.core.IResult;
import com.googlecode.contraildb.core.Identifier;
import com.googlecode.contraildb.core.impl.btree.IForwardCursor;
import com.googlecode.contraildb.core.impl.btree.KeyValueSet;
import com.googlecode.contraildb.core.utils.Handler;
import com.googlecode.contraildb.core.utils.InvocationHandler;
import com.googlecode.contraildb.core.utils.TaskUtils;
import com.googlecode.contraildb.core.utils.WhileHandler;


/**
 * Performs logical disjunction on a set of cursors.
 * That is, this cursor returns the union of all elements in a set of cursors.
 *  
 * @author Ted Stockwell
 * 
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class DisjunctiveCursor<T extends Comparable> implements IForwardCursor<T> {
	
	private static Comparator<Comparable> __comparator= new Comparator<Comparable>() {
		@Override public int compare(Comparable o1,Comparable o2) {
			return KeyValueSet.compare(o1, o2);
		}
	};
	
	final IForwardCursor<T>[] _cursors;
	T _value;
	TreeSet<T> _queue= new TreeSet<T>(__comparator);

	public DisjunctiveCursor(List<IForwardCursor<Identifier>> cursors) {
		_cursors= new IForwardCursor[cursors.size()];
		cursors.toArray(_cursors);
	}

	@Override
	public IResult<T> keyValue() {
		if (_queue.isEmpty())
			return TaskUtils.NULL;
		return TaskUtils.asResult(_queue.first());
	}

	@Override
	public IResult<Boolean> first() {
		_queue.clear();

		final int[] index= new int[] { 0 }; 
		IResult first= new WhileHandler() {
			protected IResult<Boolean> While() {
				return asResult(index[0] < _cursors.length);
			}
			protected IResult<Void> Do() {
				final IForwardCursor<T> cursor= _cursors[index[0]++];
				return new InvocationHandler<Boolean>(cursor.first()) {
					protected IResult onSuccess(Boolean first) {
						if (first) 
							return new InvocationHandler<T>(cursor.keyValue()) {
								protected IResult onSuccess(T keyValue) {
									_queue.add(keyValue);
									return TaskUtils.DONE;
								}
							};
						return TaskUtils.DONE;
					}
				};
			}
		};
		
		return new Handler(first) {
			protected IResult onSuccess() throws Exception {
				return asResult(!_queue.isEmpty());
			}
		};
	}

	@Override
	public IResult<Boolean> to(final T e) {
		final int[] index= new int[] { 0 }; 
		IResult first= new WhileHandler() {
			protected IResult<Boolean> While() {
				return asResult(index[0] < _cursors.length);
			}
			protected IResult<Void> Do() {
				final IForwardCursor<T> cursor= _cursors[index[0]++];
				return new InvocationHandler<Boolean>(cursor.to(e)) {
					protected IResult onSuccess(Boolean to) {
						if (to) 
							return new InvocationHandler<T>(cursor.keyValue()) {
								protected IResult onSuccess(T keyValue) {
									_queue.add(keyValue);
									return TaskUtils.DONE;
								}
							};
						return TaskUtils.DONE;
					}
				};
			}
		};
		
		return new Handler(first) {
			protected IResult onSuccess() {
				final T t= _queue.ceiling(e);
				if (KeyValueSet.compare(e, t) <= 0) {
					T f;
					while (!_queue.isEmpty() && KeyValueSet.compare(f= _queue.first(), t) < 0)
						_queue.remove(f);
					return TaskUtils.TRUE;
				}
				return TaskUtils.FALSE;
			}
		};
	}

	@Override
	public IResult<Boolean> hasNext() {
		if (!_queue.isEmpty())
			return TaskUtils.TRUE;
		
		final int[] index= new int[] { 0 }; 
		IResult first= new WhileHandler() {
			protected IResult<Boolean> While() {
				return asResult(index[0] < _cursors.length);
			}
			protected IResult<Void> Do() {
				final IForwardCursor<T> cursor= _cursors[index[0]++];
				return new InvocationHandler<Boolean>(cursor.next()) {
					protected IResult onSuccess(Boolean next) {
						if (next) 
							return new InvocationHandler<T>(cursor.keyValue()) {
								protected IResult onSuccess(T keyValue) {
									_queue.add(keyValue);
									return TaskUtils.DONE;
								}
							};
						return TaskUtils.DONE;
					}
				};
			}
		};
		
		return new Handler(first) {
			protected IResult onSuccess() throws Exception {
				return asResult(!_queue.isEmpty());
			}
		};
	}


	@Override
	public IResult<Boolean> next() {
		if (!_queue.isEmpty()) {
			_queue.remove(_queue.first());
			if (!_queue.isEmpty()) 
				return TaskUtils.TRUE;
		}
		
		final int[] index= new int[] { 0 }; 
		IResult first= new WhileHandler() {
			protected IResult<Boolean> While() {
				return asResult(index[0] < _cursors.length);
			}
			protected IResult<Void> Do() {
				final IForwardCursor<T> cursor= _cursors[index[0]++];
				return new InvocationHandler<Boolean>(cursor.next()) {
					protected IResult onSuccess(Boolean next) {
						if (next) 
							return new InvocationHandler<T>(cursor.keyValue()) {
								protected IResult onSuccess(T keyValue) {
									_queue.add(keyValue);
									return TaskUtils.DONE;
								}
							};
						return TaskUtils.DONE;
					}
				};
			}
		};
		
		return new Handler(first) {
			protected IResult onSuccess() throws Exception {
				return asResult(!_queue.isEmpty());
			}
		};
	}

	@Override
	public Direction getDirection() {
		return Direction.FORWARD;
	}


	@Override
	public IResult<Boolean> to(IResult<T> e) {
		return new InvocationHandler<T>(e) {
			protected IResult onSuccess(T results) {
				return to(results);
			}
		};
	}

}
