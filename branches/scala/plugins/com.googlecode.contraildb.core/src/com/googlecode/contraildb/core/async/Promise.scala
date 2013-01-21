package com.googlecode.contraildb.core.async;


import java.util.concurrent.CancellationException
import com.googlecode.contraildb.core.utils.Logging;
import scala.collection.mutable.ArrayBuffer

/**
 * Implementation of {@link Result}
 * 
 * @author ted.stockwell
 */
class Promise[V] extends Result[V] {
	var _completedHandlers:ArrayBuffer[(Result[V])=>Any]= null;
	
	/**
	 * This method is called when the associated computation has been cancelled.
	 */
	def cancel() {
		complete(false, true, null.asInstanceOf[V], null);
	}
	
	/**
	 * This method is called when the associated computation has successfully completed.
	 */
	def success(value:V) {
		complete(true, false, value, null);
	}
	
	/**
	 * This method is called when the associated computation has encountered an error.
	 */
	def error(t:Throwable) {
		complete(false, false, null.asInstanceOf[V], t);
	}

//	def getb():V {
//	  sync {
//		joinb();
//		
//		if (!_success) {
//			if (_cancelled) {
//				throw new CancellationException();
//			}
//			else
//				TaskUtils.throwSomething(_error);
//		}
//		
//		return _result;
//	  }
//	}

//	@Override public void join() throws Pausable {
//		synchronized (this) {
//			if (_done) 
//				return;
//		}
//		
//		// will not return until this result is completed
//		_completeBox.get(); 
//		
//		// since we cannot synchronize access to _completeBox, it is possible 
//		// that some other thread might also be waiting on _completeBox, so we 
//		// hafta prime the pump for them 
//		_completeBox.putb(true);
//		
//		synchronized (this) {
//			if (!_done)
//				throw new InternalError("Received complete notification but result is not available");
//		}
//	}
	
//	@Override public void joinb() {
//		synchronized (this) {
//			if (_done) 
//				return;
//		}
//		
//		// will not return until this result is completed
//		_completeBox.getb(); 
//		
//		// since we cannot synchronise access to _completeBox, it is possible 
//		// that some other thread might also be waiting on _completeBox, so we 
//		// hafta prime the pump for them 
//		_completeBox.putb(true);
//		
//		synchronized (this) {
//			if (!_done)
//				throw new InternalError("Received complete notification but result is not available");
//		}
//	}

	def complete( result:Result[V]) {
	    result.onDone { 
		  sync {
			complete(result.success, result.cancelled, result.value, result.error);
		  }
	    }
	}

	/**
	 * This method is called when the associated computation has completed.
	 */
	def complete(success:Boolean, cancelled:Boolean, result:V, error:Throwable) {
	  sync {
		if (_done)
			return;
		_done= true;
		_success= success;
		_cancelled= cancelled;
		_error= error;
		_result= result;
		
		if (_completedHandlers != null) {
			for (handler <- _completedHandlers) {
				try {
					handler(this);
				}
				catch {
				case t: Throwable => Logging.warning("Error in result handler", t);
				}
				return;
			}
			_completedHandlers= null;
		}
	  }
	}


}
