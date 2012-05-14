package com.googlecode.contraildb.core.impl;

import java.io.IOException;
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
public class ConjunctiveCursor<T extends Comparable<T>> implements IForwardCursor<T> {
	
	final IForwardCursor<T>[] _cursors;
	
	public ConjunctiveCursor(List<IForwardCursor<T>> filterCursors) {
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
		return new InvocationHandler<Boolean>(_cursors[0].to(e)) {
			protected IResult onSuccess(Boolean to) throws Exception {
				if (!to)
					return TaskUtils.FALSE;

				final T value= _cursors[0].keyValue();
				final Boolean[] done= new Boolean[] { null };
				IResult wile= new WhileHandler() {
					protected IResult<Boolean> While() throws Exception {
						return asResult(done[0] == null);
					}
					protected IResult<Void> Do() throws Exception {
						T ge= value;
						for (int i= 1; i < _cursors.length; i++) {
							IForwardCursor<T> cursor= _cursors[i];
							if (!cursor.to(value))
								return false;
							T t= cursor.keyValue();
							if (BPlusTree.compare(value, t) < 0)
								ge= t;
						}
						if (BPlusTree.compare(value, ge) == 0) 
							return true;

						if (!_cursors[0].to(ge))
							return false;
						value= _cursors[0].keyValue();
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
