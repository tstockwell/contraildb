package com.googlecode.contraildb.core.impl;

import com.googlecode.contraildb.core.Identifier;
import com.googlecode.contraildb.core.impl.btree.IForwardCursor;


/** 
 * The type of cursor that is the result of a query
 * @author ted.stockwell
 */
public interface IQueryCursor extends IForwardCursor<IForwardCursor<Identifier>> {

}
