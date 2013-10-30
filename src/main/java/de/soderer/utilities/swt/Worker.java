package de.soderer.utilities.swt;

import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public abstract class Worker<T> implements RunnableFuture<T> {
	private static final long DEFAULT_SHOW_PROGRESS_AFTER_MILLISECONDS = 500;

	protected long todo = -1;
	protected Date lastProgressShow = new Date();
	protected long showProgressAfterMilliseconds;
	protected T result = null;
	
	protected ProgressShowingModalDialog<?> parent;
	
	protected Exception error = null;
	protected boolean cancel = false;
	protected boolean isDone = false;
	
	public Worker(ProgressShowingModalDialog<?> parent) {
		this.parent = parent;
		this.showProgressAfterMilliseconds = DEFAULT_SHOW_PROGRESS_AFTER_MILLISECONDS;
	}
	
	protected void showItemsDone(long itemsDone) {
		if (!cancel && new Date().getTime() - lastProgressShow.getTime() > showProgressAfterMilliseconds) {
			parent.showProgress(todo, itemsDone);
			lastProgressShow = new Date();
		}
	}
	
	protected void showDone() {
		parent.showDone();
	}
	
	@Override
	public boolean cancel(boolean arg0) {
		cancel = true;
		return cancel;
	}

	/**
	 * Check for error and return result value
	 */
	@Override
	public T get() throws InterruptedException, ExecutionException {
		if (error != null) {
			throw new ExecutionException(error);
		} else {
			return result;
		}
	}

	@Override
	public T get(long arg0, TimeUnit arg1) throws InterruptedException, ExecutionException, TimeoutException {
		return get();
	}

	@Override
	public boolean isCancelled() {
		return cancel;
	}

	@Override
	public boolean isDone() {
		return isDone;
	}
	
	@Override
	public abstract void run();
}
