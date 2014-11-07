/* ************************************************************************
#
#  DivConq
#
#  http://divconq.com/
#
#  Copyright:
#    Copyright 2014 eTimeline, LLC. All rights reserved.
#
#  License:
#    See the license.txt file in the project's top-level directory for details.
#
#  Authors:
#    * Andy White
#
************************************************************************ */
package divconq.lang;

import java.util.concurrent.ConcurrentLinkedQueue;

import divconq.lang.op.FuncCallback;

public class CallbackQueue<T> {
	protected ConcurrentLinkedQueue<T> resources = new ConcurrentLinkedQueue<T>();	
	protected ConcurrentLinkedQueue<FuncCallback<T>> callbacks = new ConcurrentLinkedQueue<FuncCallback<T>>();
	protected volatile boolean disposed = false;
	protected QueueWatcher qwatcher = null;

	public void setWatcher(QueueWatcher watcher) {
		this.qwatcher = watcher;
	}
	
	public void pop(FuncCallback<T> callback) {
		if (this.disposed) {
			callback.error(1, "Disposed");		// TODO better code
			callback.complete();
			return;
		}
		
		T resource = this.resources.poll();
		
		if (resource != null) {
			callback.setResult(resource);
			callback.complete();
			return;
		}
		
		this.callbacks.add(callback);
	}
	
	public void add(T resource) {
		if (this.disposed) {
			if (this.qwatcher != null)
				this.qwatcher.disposed(resource);
			
			return;
		}
		
		FuncCallback<T> callback = this.callbacks.poll();
		
		if (callback != null) {
			callback.setResult(resource);
			callback.complete();
			return;
		}
		
		this.resources.add(resource);
	}
	
	// tell the callbacks to go away, nothing more to do
	// and grab all the available resources 
	public void dispose() {
		this.disposed = true;
		
		FuncCallback<T> callback = this.callbacks.poll();
		
		while (callback != null) {
			callback.error(1, "Nothing more to do");		// TODO better code
			callback.complete();
			callback = this.callbacks.poll();
		}		
		
		if (this.qwatcher != null) {
			T res = this.resources.poll();
			
			while (res != null) {
				this.qwatcher.disposed(res);
				res = this.resources.poll();
			}		
		}
	}
	
	abstract public class QueueWatcher {
		abstract public void disposed(T res);
	}
}
