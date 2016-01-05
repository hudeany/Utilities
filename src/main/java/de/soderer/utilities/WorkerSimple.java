package de.soderer.utilities;

import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public abstract class WorkerSimple<T> implements RunnableFuture<T> {
	private static final long DEFAULT_SHOW_PROGRESS_AFTER_MILLISECONDS = 500;
	private static final long DEFAULT_SHOW_PROGRESS_OVERRIDE_REFRESH_MILLISECONDS = 200;

	protected Date startTime;
	protected Date endTime;
	protected long itemsToDo = -1;
	protected long itemsDone = -1;
	protected Date lastProgressShow = new Date();
	protected long showProgressAfterMilliseconds = DEFAULT_SHOW_PROGRESS_AFTER_MILLISECONDS;
	protected long showProgressOverrideRefreshMilliseconds = DEFAULT_SHOW_PROGRESS_OVERRIDE_REFRESH_MILLISECONDS;
	protected T result = null;

	protected WorkerParentSimple parent;

	protected Exception error = null;
	protected boolean cancel = false;
	protected boolean isDone = false;

	public WorkerSimple(WorkerParentSimple parent) {
		this.parent = parent;
	}

	/**
	 * Minimum time between two parent.showProgress calls. (default 500 millis)
	 *
	 * @param value
	 */
	public void setShowProgressAfterMilliseconds(long value) {
		showProgressAfterMilliseconds = value;
	}

	/**
	 * May be set to 0 to speed up actions, when there is no GUI that must handle the progress show requests. (default 150 millis)
	 *
	 * @param value
	 */
	public void setShowProgressOverrideRefreshMilliseconds(long value) {
		showProgressOverrideRefreshMilliseconds = value;
	}

	protected void showProgress() {
		showProgress(false);
	}

	protected void showProgress(boolean overrideRefreshTime) {
		if (parent != null && !cancel) {
			if (new Date().getTime() - lastProgressShow.getTime() > showProgressAfterMilliseconds) {
				// Normal progress update
				parent.showProgress(startTime, itemsToDo, itemsDone);
				lastProgressShow = new Date();
			} else if (overrideRefreshTime) {
				// Important progress update, which may not be left out
				letGuiRefresh();
				parent.showProgress(startTime, itemsToDo, itemsDone);
				lastProgressShow = new Date();
			}
		}
	}

	protected void showDone() {
		endTime = new Date();
		isDone = true;
		if (parent != null) {
			letGuiRefresh();
			parent.showDone(startTime, endTime, itemsDone);
		}
	}

	protected void showUnlimitedProgress() {
		if (parent != null) {
			letGuiRefresh();
			parent.showUnlimitedProgress();
		}
	}

	@Override
	public boolean cancel(boolean arg0) {
		return cancel();
	}

	public boolean cancel() {
		cancel = true;
		return cancel;
	}

	/**
	 * Check for error and return result value
	 */
	@Override
	public T get() throws InterruptedException, ExecutionException {
		if (!isDone) {
			throw new ExecutionException(new Exception("Worker is not done yet"));
		} else if (error != null) {
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

	protected void letGuiRefresh() {
		if (showProgressOverrideRefreshMilliseconds > 0) {
			try {
				// Allow GUI to refresh
				Thread.sleep(showProgressOverrideRefreshMilliseconds);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public abstract void run();
}
