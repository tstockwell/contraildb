package com.googlecode.contraildb.core.utils.tasks;

import java.io.IOException;
import java.io.ObjectOutputStream;

import kilim.Pausable;

import com.googlecode.contraildb.core.async.ContrailTask;
import com.googlecode.contraildb.core.async.IResult;
import com.googlecode.contraildb.core.async.TaskUtils;
import com.googlecode.contraildb.core.utils.ClosableByteArrayOutputStream;
import com.googlecode.contraildb.core.utils.Logging;


/**
 * Persists the given object to a byte array using the Contrail externalization 
 * scheme.
 */
public class ExternalizationTask extends ContrailTask<byte[]>  {
	ClosableByteArrayOutputStream _byteStream= new ClosableByteArrayOutputStream();
	Object _item;
	public ExternalizationTask(Object item) {
		_item= item;
	}
	
	@Override
	public IResult<byte[]>  submit() {
		return super.submit();
	}
	
	protected byte[] run() throws Pausable, IOException {
		try {
			ObjectOutputStream outputStream= new ObjectOutputStream(_byteStream);
			outputStream.writeObject(_item);
			outputStream.flush();
			return _byteStream.toByteArray();
		}
		catch (Throwable x) {
			if (!getResult().isCancelled()) { // if task was canceled then we can ignore the error.
				x.printStackTrace();
				TaskUtils.throwSomething(x, IOException.class);
			}
			return null;
		}
	}
	
	protected void stop() {
		/*
		 * If/when this task is canceled we close the byte stream which causes 
		 * the above call to ExternalizationManager.writeExternal to fail.
		 */
		try { _byteStream.close(); } catch (Throwable t) { Logging.warning(t); }
	}
}
