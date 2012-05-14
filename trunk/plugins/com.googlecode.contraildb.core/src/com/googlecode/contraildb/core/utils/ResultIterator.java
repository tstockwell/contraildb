package com.googlecode.contraildb.core.utils;

import com.googlecode.contraildb.core.IResult;

/**
 * Like java.util.Iterator only returns aysnchronous results instead of immediate values.
 * @author ted.stockwell
 */
public interface ResultIterator<E> {
    IResult<Boolean> hasNext();
    IResult<E> next();
    IResult<Void> remove();
}
