package com.googlecode.contraildb.core.impl;

import java.io.Serializable;
import java.util.List;
import java.util.NoSuchElementException;

import com.googlecode.contraildb.core.IResult;
import com.googlecode.contraildb.core.impl.btree.BPlusTree;
import com.googlecode.contraildb.core.impl.btree.IForwardCursor;
import com.googlecode.contraildb.core.utils.Handler;
import com.googlecode.contraildb.core.utils.Immediate;
import com.googlecode.contraildb.core.utils.InvocationHandler;
import com.googlecode.contraildb.core.utils.TaskUtils;
import com.googlecode.contraildb.core.utils.WhileHandler;


/**
 * Performs logical conjunction on a set of cursors.
 * That is, this cursor returns the intersection of all elements in a set of cursors.
 *  
 * @author Ted Stockwell
 * 
 */
@SuppressWarnings({"unchecked","rawtypes"})
public class ConjunctivePropertyCursor<T extends Comparable<T> & Serializable> 
implements IPropertyCursor<T> 
{
	
	final IForwardCursor<T>[] _cursors;
	
	public ConjunctivePropertyCursor(List<IPropertyCursor> filterCursors) {
		_cursors= new IForwardCursor[filterCursors.size()];
		filterCursors.toArray(_cursors);
	}

	@Override
	@Immediate public T keyValue() {
		if (_cursors.length <= 0)
			throw new NoSuchElementException();
		return _cursors[0].keyValue();
	}

	@Override
	public IResult<Boolean> first() {
		return new InvocationHandler<Boolean>(_cursors[0].first()) {
			protected IResult onSuccess(Boolean first) throws Exception {
				if (!first)
					return TaskUtils.FALSE;
				return to(_cursors[0].keyValue());
			}
		};
	}

	@Override
	public IResult<Boolean> to(T e) {
		return new InvocationHandler<Boolean>(_cursors[0].to(e)) { // move first cursor
			protected IResult onSuccess(Boolean to) throws Exception {
				if (!to)
					return TaskUtils.FALSE; // if failed then done

				final Object[] value= new Object[] { _cursors[0].keyValue() };
				final Boolean[] done= new Boolean[] { null };
				IResult wile= new WhileHandler() {
					protected IResult<Boolean> While() throws Exception {
						return asResult(done[0] == null);
					}
					protected IResult<Void> Do() throws Exception {
						final Object[] ge= new Object[] { value[0] }; // this value is ge to the value from the first cursor

						final int[] cursorIndex= new int[] { 1 };
						IResult moveOtherCursors= new WhileHandler() {
							protected IResult<Boolean> While() throws Exception {
								return asResult(cursorIndex[0] < _cursors.length);
							}
							
							protected IResult<Void> Do() throws Exception {
								final IForwardCursor<T> cursor= _cursors[cursorIndex[0]];
								return new InvocationHandler<Boolean>(cursor.to((T)value[0])) {
									protected IResult onSuccess(Boolean to) {
										if (!to) { // if there no ge value then fail 
											done[0]= false;
											return TaskUtils.DONE;
										}
										
										// if found value is ge than current value then save it
										T t= cursor.keyValue();
										if (BPlusTree.compare((T)ge[0], t) < 0)
											ge[0]= t;
										return TaskUtils.DONE;
									}
								};
							}
						};
						
						return new Handler(moveOtherCursors) {
							protected IResult onSuccess() throws Exception {
								// if all cursors were moved to the first value then return success
								if (BPlusTree.compare((T)value[0], (T)ge[0]) == 0) {
									done[0]= true;
									return TaskUtils.DONE;
								}
								
								// try moving the first cursor to the new value before starting over
								return new InvocationHandler<Boolean>(_cursors[0].to((T)ge[0])) {
									protected IResult onSuccess(Boolean moveFirst) {
										if (!moveFirst) {
											done[0]= false;
											return TaskUtils.DONE;
										}
										value[0]= _cursors[0].keyValue();
										return TaskUtils.DONE;
									}
								};
							}
						};
					}
				};
				return new Handler(wile) {
					protected IResult onSuccess() throws Exception {
						return asResult(done[0]);
					}
				};
			}
		};
	}

	@Override
	public IResult<Boolean> hasNext()  {
		throw new UnsupportedOperationException();
	}

	@Override
	public IResult<Boolean> next() {
		final Boolean[] result= new Boolean[] { null };
		IResult wile= new WhileHandler() {
			protected IResult<Boolean> While() throws Exception {
				if (result[0] != null)
					return TaskUtils.FALSE;
				return _cursors[0].next();			
			}
			protected IResult<Void> Do() throws Exception {
				return new InvocationHandler<Boolean>(to(_cursors[0].keyValue())) {
					protected IResult onSuccess(Boolean to) throws Exception {
						if (to)
							result[0]= Boolean.TRUE; 
						return TaskUtils.DONE;
					}
				};
			}
		};
		return new Handler(wile) {
			protected IResult onSuccess() throws Exception {
				if (result[0] != null)
					return TaskUtils.asResult(result[0]);
				return TaskUtils.FALSE;
			}
		};
	}
	
	@Override
	public Direction getDirection() {
		return Direction.FORWARD;
	}
}
