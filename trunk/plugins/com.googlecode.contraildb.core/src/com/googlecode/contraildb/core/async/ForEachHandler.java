package com.googlecode.contraildb.core.async;

import java.util.Collection;
import java.util.Iterator;

import com.googlecode.contraildb.core.IResult;

@SuppressWarnings({"rawtypes","unchecked"})
abstract public class ForEachHandler<T> extends Handler {
	
	private Iterator<T> _items;
	private T _item;
	
	public ForEachHandler(Collection<T> items) {
		super();
		_items= items.iterator();
	}
	public ForEachHandler(IResult<Collection<T>> items) {
		super(items);
	}
	
	abstract protected IResult<Void> Do(T item) throws Exception;
	
	private IResult<Boolean> While() {
		if (!_items.hasNext())
			return TaskUtils.FALSE;
		_item= _items.next();
		return TaskUtils.TRUE;
	}
	
	final protected IResult onSuccess() throws Exception {
		if (_items == null) 
			_items= ((Collection)incoming().getResult()).iterator();
		return new Handler(While()) {
			protected IResult onSuccess() throws Exception {
				Boolean x= (Boolean)incoming().getResult();
				if (x == null || !x)
					return TaskUtils.DONE;
				return new Handler(Do(_item)) {
					protected IResult onSuccess() throws Exception {
						return ForEachHandler.this.onSuccess();
					}
				};
			}
		};
	}
}
