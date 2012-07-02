/**
 * 
 */
package com.googlecode.contraildb.core.impl.btree;

import com.googlecode.contraildb.core.IResult;
import com.googlecode.contraildb.core.async.Handler;
import com.googlecode.contraildb.core.async.Immediate;
import com.googlecode.contraildb.core.async.ResultAction;
import com.googlecode.contraildb.core.async.ResultHandler;
import com.googlecode.contraildb.core.async.TaskUtils;
import com.googlecode.contraildb.core.async.WhileHandler;
import com.googlecode.contraildb.core.storage.IEntity;


@SuppressWarnings({ "unchecked", "rawtypes" })
public class CursorImpl<T extends Comparable, V> 
implements IKeyValueCursor<T,V> {
	KeyValueSet _tree;
	Direction _direction;
	
	public CursorImpl(KeyValueSet index, Direction direction) {
		_direction= direction;
		_tree= index;
	}
	Node<T> _page;
	int _index;

	@Override
	public IResult<V> elementValue() {
		return TaskUtils.asResult((V) _page._values[_index]);
	}
	
	private IResult<Void> initFirst()  {
		_index= -1;
		if ((_page= _tree.getRoot()) != null) {
			return new WhileHandler() {
				protected IResult<Boolean> While() throws Exception {
					return asResult(!_page.isLeaf());
				}
				protected IResult<Void> Do() throws Exception {
					return new ResultHandler<Node<T>>(_page.getChildNode(0)) {
						protected IResult onSuccess(Node<T> child) throws Exception {
							_page= child;
							return TaskUtils.DONE;
						}
					}; 
				}
			};
		}
		return TaskUtils.DONE;
	}
	
	protected IResult<Void> initLast() {
		if ((_page= _tree.getRoot()) != null) {
			IResult dowhile= new WhileHandler() {
				protected IResult<Boolean> While() throws Exception {
					return asResult(!_page.isLeaf());
				}
				protected IResult<Void> Do() throws Exception {
					return new ResultHandler<Node<T>>(_page.getChildNode(_page._size-1)) {
						protected IResult onSuccess(Node<T> child) throws Exception {
							_page= child;
							return TaskUtils.DONE;
						}
					}; 
				}
			};
			return new ResultAction(dowhile) {
				protected void onSuccess(Object results) throws Exception {
					_index= _page._size;
				}
			};
		}
		return TaskUtils.DONE;
	}

	@Override 
	@Immediate public IResult<T> keyValue() {
		return TaskUtils.asResult(_page._keys[_index]);
	}
	protected IResult<Boolean> previous() {
		IResult<Void> init= TaskUtils.DONE;
		if (_page == null)
			init= initLast();
		return new Handler(init) {
			protected IResult onSuccess() throws Exception {
				if (_page == null)
					return TaskUtils.FALSE;
				
				IResult<Void > fetch= TaskUtils.DONE;
				if (_index <= 0) {
					if (_page._previous == null) 
						return TaskUtils.FALSE;
					fetch= new ResultHandler<IEntity>(_page.getStorage().fetch(_page._previous)) {
						protected IResult onSuccess(IEntity previousPage) throws Exception {
							_page = (Node<T>) previousPage;
							_index = _page._size;
							return TaskUtils.DONE;
						}
					};
				}
				
				return new Handler(fetch) {
					protected IResult onSuccess() throws Exception {
						_index--;
						return TaskUtils.TRUE;
					}
				};
			}
		};
	}
	
	protected IResult<Boolean> ge(final T e) {
		if ((_page= _tree.getRoot()) == null)
			return TaskUtils.asResult(false);
		IResult whileNotLeaf= new WhileHandler() {
			protected IResult<Boolean> While() throws Exception {
				return asResult(!_page.isLeaf());
			}
			protected IResult<Void> Do() throws Exception {
				_index= _page.indexOf(e);
				return new ResultHandler<Node<T>>(_page.getChildNode(_index)) {
					protected IResult onSuccess(Node<T> childNode) throws Exception {
						_page = childNode;
						return TaskUtils.DONE;
					}
				};
			}
		};
		
		return new Handler(whileNotLeaf) {
			protected IResult onSuccess() throws Exception {
				_index= _page.indexOf(e);
				return asResult(0 <= _index && _index < _page._size && (KeyValueSet.compare(e, _page._keys[_index]) == 0)); 
			}
		};
	}
	
	protected IResult<Boolean> le(final T e) {
		return new ResultHandler<Boolean>(ge(e)) {
			protected IResult onSuccess(Boolean ge) throws Exception {
				if (ge) {
					return new ResultHandler<T>(keyValue()) {
						protected IResult onSuccess(T keyValue) throws Exception {
							if (KeyValueSet.compare(keyValue, e) == 0)
								return TaskUtils.TRUE;
							return previous();
						}
					};
				}
				return last();
			}
		};
	}
	
	@Override
	public IResult<Boolean> next() {
		if (Direction.REVERSE == _direction)
			return previous();
		
		if (_page == null) 
			return first();
	
		IResult next= TaskUtils.DONE;
		if (_page._size-1 <= _index) {
			if (_page._next == null)  
				return TaskUtils.FALSE;
			next= new ResultAction<IEntity>(_page.getStorage().fetch(_page._next)) {
				protected void onSuccess(IEntity next) throws Exception {
					_page = (Node<T>) next;
					_index = -1;
				}
			};
		}
		return new Handler(next) {
			protected IResult onSuccess() throws Exception {
				_index++;
				return TaskUtils.TRUE;	
			}
		};
		
	}

	@Override
	public IResult<Boolean> hasNext() {
		if (Direction.REVERSE == _direction)
			return hasPrevious();
		
		if (_page == null) 
			initFirst();
		if (_page == null)
			return TaskUtils.FALSE;
		
		IResult next= TaskUtils.DONE;
		if (_page._size-1 <= _index) {
			if (_page._next == null)  
				return TaskUtils.FALSE;
			next= new ResultAction<IEntity>(_page.getStorage().fetch(_page._next)) {
				protected void onSuccess(IEntity next) throws Exception {
					_page = (Node<T>) next;
					_index = -1;
				}
			};
		}
		return new Handler(next) {
			protected IResult onSuccess() throws Exception {
				return TaskUtils.TRUE;	
			}
		};
	}

	public IResult<Boolean> hasPrevious() {
		IResult<Void> init= TaskUtils.DONE;
		if (_page == null)
			init= initLast();
		return new Handler(init) {
			protected IResult onSuccess() throws Exception {
				if (_page == null)
					return TaskUtils.FALSE;
				
				IResult<Void > fetch= TaskUtils.DONE;
				if (_index <= 0) {
					if (_page._previous == null) 
						return TaskUtils.FALSE;
					fetch= new ResultHandler<IEntity>(_page.getStorage().fetch(_page._previous)) {
						protected IResult onSuccess(IEntity previousPage) throws Exception {
							_page = (Node<T>) previousPage;
							_index = _page._size;
							return TaskUtils.DONE;
						}
					};
				}
				
				return new Handler(fetch) {
					protected IResult onSuccess() throws Exception {
						return TaskUtils.TRUE;
					}
				};
			}
		};
	}

	@Override
	public IResult<Boolean> first() {
		if (Direction.REVERSE == _direction)
			return last();
		
		return new Handler(initFirst()) {
			protected IResult onSuccess() throws Exception {
				if (_page == null)
					return TaskUtils.FALSE;
				return next();
			}
		};
	}

	protected IResult<Boolean> last() {
		if (Direction.REVERSE == _direction)
			return first();
		
		return new Handler(initLast()) {
			protected IResult onSuccess() throws Exception {
				if (_page == null)
					return TaskUtils.FALSE;
				return previous();
			}
		};
	}
	@Override
	public IKeyValueCursor.Direction getDirection() {
		return _direction;
	}
	@Override
	public IResult<Boolean> to(T e) {
		if (Direction.REVERSE == _direction)
			return le(e);
		return ge(e);
	}
	
	/**
	 * Find the value associated with the given key.
	 * Always starts the search from the beginning 
	 */
	@Override
	public synchronized IResult<V> find(final T key) {
		IResult init;
		if (Direction.REVERSE == _direction) {
			init= initLast();
		}
		else
			init= initFirst();
		return new Handler(init) {
			protected IResult onSuccess() throws Exception {
				return new ResultHandler<Boolean>(to(key)) {
					protected IResult onSuccess(Boolean to) throws Exception {
						if (!to)
							return TaskUtils.NULL;
						return new ResultHandler<T>(keyValue()) {
							protected IResult onSuccess(T keyValue) throws Exception {
								if (KeyValueSet.compare(key, keyValue) != 0)
									return TaskUtils.NULL;
								return asResult(elementValue());
							}
						};
					}
				};
			}
		};
	}

	@Override
	public IResult<Boolean> to(IResult<T> e) {
		return new ResultHandler<T>(e) {
			protected IResult onSuccess(T t) {
				return to(t);
			}
		};
	}
	
}