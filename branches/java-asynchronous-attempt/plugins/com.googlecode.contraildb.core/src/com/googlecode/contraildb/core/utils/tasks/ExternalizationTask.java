package com.googlecode.contraildb.core.utils.tasks;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.List;

import com.googlecode.contraildb.core.IResult;
import com.googlecode.contraildb.core.utils.ClosableByteArrayOutputStream;
import com.googlecode.contraildb.core.utils.ContrailTask;
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
	public IResult<byte[]> submit() {
		return super.submit();
	}
	
	@Override
	public IResult<byte[]> submit(List<ContrailTask<?>> dependentTasks) {
		return super.submit(dependentTasks);
	}
	
	protected byte[] run() throws IOException {
		ObjectOutputStream outputStream= new ObjectOutputStream(_byteStream);
		outputStream.writeObject(_item);
		outputStream.flush();
		return _byteStream.toByteArray();
	}
	
	public byte[] get() {
		return super.get();
	}
	
	protected void stop() {
		/*
		 * If/when this task is canceled we close the byte stream which causes 
		 * the above call to ExternalizationManager.writeExternal to fail.
		 */
		try { _byteStream.close(); } catch (Throwable t) { Logging.warning(t); }
	}
}
