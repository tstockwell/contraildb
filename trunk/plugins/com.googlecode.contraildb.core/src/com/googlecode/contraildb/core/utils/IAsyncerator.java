package com.googlecode.contraildb.core.utils;

import com.googlecode.contraildb.core.IResult;

/**
 * Like java.util.Iterator only this interface returns results 
 * asynchronously instead of immediately.
 * 
 * @author ted.stockwell
 */
public interface IAsyncerator<E> {
    IResult<Boolean> hasNext();
    IResult<E> next();
    IResult<Void> remove();
}
