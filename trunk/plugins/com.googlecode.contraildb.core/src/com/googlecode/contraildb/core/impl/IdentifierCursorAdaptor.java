package com.googlecode.contraildb.core.impl;

import com.googlecode.contraildb.core.IResult;
import com.googlecode.contraildb.core.Identifier;
import com.googlecode.contraildb.core.impl.btree.IForwardCursor;
import com.googlecode.contraildb.core.utils.Handler;
import com.googlecode.contraildb.core.utils.InvocationHandler;
import com.googlecode.contraildb.core.utils.TaskUtils;


/**
 * Provides an IForwardCursor<Identifier> interface to an IPropertyCursor interface.
 * This implementation iterates through the given IPropertyCursor instance as this 
 * cursor is iterated through. 
 *  
 * @author Ted Stockwell
 * 
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class IdentifierCursorAdaptor implements IForwardCursor<Identifier> {
	
	IForwardCursor<Identifier> _nextElement;
	final IPropertyCursor _cursor;
	Object _startingValue;
	
	public static IResult<IdentifierCursorAdaptor> create(IPropertyCursor propertyCursor) {
		final IdentifierCursorAdaptor adaptor= new IdentifierCursorAdaptor(propertyCursor);
		return new Handler(propertyCursor.keyValue()) {
			protected IResult onSuccess() throws Exception {
				adaptor._startingValue= incoming().getResult();
				return asResult(adaptor);
			}
		};
	}

	private IdentifierCursorAdaptor(IPropertyCursor propertyCursor) {
		_cursor= propertyCursor;
	}

	@Override
	public IResult<Identifier> keyValue() {
		if (_nextElement == null)
			return new InvocationHandler<IForwardCursor<Identifier>>(_cursor.elementValue()) {
				protected IResult onSuccess(IForwardCursor<Identifier> results) {
					_nextElement= results;
					return _nextElement.keyValue();
				}
			};
		return _nextElement.keyValue();
	}

	@Override
	public IResult<Boolean> first() {
		return new InvocationHandler<Boolean>(_cursor.to(_startingValue)) {
			protected IResult onSuccess(Boolean moved) throws Exception {
				if (!moved)
					return TaskUtils.FALSE;
				return new InvocationHandler<IForwardCursor<Identifier>>(_cursor.elementValue()) {
					protected IResult onSuccess(IForwardCursor<Identifier> element) {
						_nextElement= element;
						return TaskUtils.TRUE;
					}
				};
			}
		};
	}

	@Override
	public IResult<Boolean> to(final Identifier e) {
		return new Handler() {
			protected IResult onSuccess() throws Exception {
				throw new UnsupportedOperationException("This operation is not supported");
			}
		};
	}

	@Override
	public IResult<Boolean> hasNext() {
		
		IResult<Boolean> nextFound= TaskUtils.FALSE;
		if (_nextElement != null)
			nextFound= _nextElement.hasNext();
		return new InvocationHandler<Boolean>(nextFound) {
			protected IResult onSuccess(Boolean hasNext) throws Exception {
				if (hasNext)
					return TaskUtils.TRUE;
				_nextElement= null;
				return _cursor.hasNext();
			}
		};
	}


	@Override
	public IResult<Boolean> next() {
		IResult<Boolean> nextFound= TaskUtils.FALSE;
		if (_nextElement != null)
			nextFound= _nextElement.next();
		return new InvocationHandler<Boolean>(nextFound) {
			protected IResult onSuccess(Boolean hasNext) throws Exception {
				if (hasNext)
					return TaskUtils.TRUE;
				_nextElement= null;
				return _cursor.next();
			}
		};
	}

	@Override
	public Direction getDirection() {
		return Direction.FORWARD;
	}


	@Override
	public IResult<Boolean> to(IResult<Identifier> e) {
		return new InvocationHandler<Identifier>(e) {
			protected IResult onSuccess(Identifier id) {
				return to(id);
			}
		};
	}

}
