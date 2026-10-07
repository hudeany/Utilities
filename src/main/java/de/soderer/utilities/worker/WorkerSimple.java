package de.soderer.utilities.worker;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Base class for a background job with progress reporting to a {@link WorkerParentSimple}.
 * <p>
 * Subclasses implement {@link #work()} and update {@link #itemsToDo} and {@link #itemsDone} while working, calling
 * {@link #signalProgress()} to report the progress. A worker is started by running it in its own thread and can be run only once.
 * <p>
 * Deviating from the {@link java.util.concurrent.Future} contract, {@link #get()} does not block, but throws an
 * ExecutionException while the worker is not done yet, so callers wait for {@link #isDone()} first.
 *
 * @param <T> type of the result
 */
public abstract class WorkerSimple<T> implements RunnableFuture<T> {
	private static final long DEFAULT_PROGRESS_DELAY_MILLISECONDS = 500;

	private volatile LocalDateTime startTime;
	private volatile LocalDateTime endTime;
	/**
	 * Total number of items (e.g. bytes or files) to process, 0 or below if unknown.
	 */
	protected volatile long itemsToDo = -1;
	/**
	 * Unit of the items for display (e.g. "B" for bytes), may be null.
	 */
	protected volatile String itemsUnitSign = null;
	/**
	 * Number of items processed so far.
	 */
	protected volatile long itemsDone = -1;
	/**
	 * Time of the last progress signal sent to the parent.
	 */
	protected LocalDateTime lastProgressShow = LocalDateTime.now();
	/**
	 * Minimum time between two progress signals in milliseconds.
	 */
	protected long progressDisplayDelayMilliseconds = DEFAULT_PROGRESS_DELAY_MILLISECONDS;
	private volatile T result = null;

	/**
	 * Parent receiving the progress signals, may be null.
	 */
	protected WorkerParentSimple parent;

	private volatile Exception error = null;
	/**
	 * True after cancellation was requested. Subclasses check it regularly and stop working.
	 */
	protected volatile boolean cancel = false;
	private volatile boolean isDone = false;

	/**
	 * Creates a worker reporting to the given parent.
	 *
	 * @param parent parent receiving the progress signals, may be null
	 */
	public WorkerSimple(final WorkerParentSimple parent) {
		this.parent = parent;
	}

	/**
	 * Sets the parent receiving the progress signals.
	 *
	 * @param parent parent receiving the progress signals, may be null
	 */
	public void setParent(final WorkerParentSimple parent) {
		this.parent = parent;
	}

	/**
	 * Minimum time between two parent.showProgress calls. (default 500 millis)
	 *
	 * @param value minimum time between two progress signals in milliseconds
	 */
	public void setProgressDisplayDelayMilliseconds(final long value) {
		progressDisplayDelayMilliseconds = value;
	}

	/**
	 * Sends the current progress to the parent, unless the last signal was sent less than the progress display
	 * delay ago or the worker was cancelled.
	 */
	public void signalProgress() {
		signalProgress(false);
	}

	/**
	 * Sends the current progress to the parent, unless the worker was cancelled.
	 *
	 * @param overrideRefreshTime true to send the signal regardless of the progress display delay
	 */
	public void signalProgress(final boolean overrideRefreshTime) {
		if (parent != null && !cancel) {
			if (Duration.between(lastProgressShow, LocalDateTime.now()).toMillis() > progressDisplayDelayMilliseconds
					|| overrideRefreshTime) {
				parent.receiveProgressSignal(startTime, itemsToDo, itemsDone, itemsUnitSign);
				lastProgressShow = LocalDateTime.now();
			}
		}
	}

	/**
	 * Signals the parent, that the progress cannot be measured (e.g. while the number of items is still unknown).
	 */
	public void signalUnlimitedProgress() {
		if (parent != null) {
			parent.receiveUnlimitedProgressSignal();
		}
	}

	/**
	 * Requests cancellation of the worker without waiting for it to stop.
	 *
	 * @return always true
	 */
	public boolean cancel() {
		return cancel(false);
	}

	@Override
	public boolean cancel(final boolean waitForDone) {
		if (!cancel) {
			cancel = true;
			if (parent != null) {
				parent.cancel();
			}
		}
		if (waitForDone) {
			while (!isDone()) {
				try {
					Thread.sleep(500);
				} catch (@SuppressWarnings("unused") final InterruptedException e) {
					// do nothing
				}
			}
		}
		return cancel;
	}

	/**
	 * Check for error and return result value
	 *
	 * @return result of the worker
	 * @throws ExecutionException if the worker is not done yet or failed with an error (as cause)
	 */
	@Override
	public T get() throws ExecutionException {
		if (!isDone) {
			throw new ExecutionException(new Exception("Worker is not done yet"));
		} else if (error != null) {
			throw new ExecutionException(error);
		} else {
			return result;
		}
	}

	/**
	 * Returns the error the worker failed with.
	 *
	 * @return error of the worker, or null if it has not failed (yet)
	 */
	public Exception getError() {
		return error;
	}

	@Override
	public T get(final long arg0, final TimeUnit arg1) throws InterruptedException, ExecutionException, TimeoutException {
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

	/**
	 * Returns the time the worker was started.
	 *
	 * @return start time, or null if the worker was not started yet
	 */
	public LocalDateTime getStartTime() {
		return startTime;
	}

	/**
	 * Sets the end time of the work. If it is not set by the worker itself, it is set when {@link #work()} returns.
	 *
	 * @param endTime end time of the work
	 */
	public void setEndTime(final LocalDateTime endTime) {
		this.endTime = endTime;
	}

	/**
	 * Returns the time the worker has finished.
	 *
	 * @return end time, or null if the worker is not done yet
	 */
	public LocalDateTime getEndTime() {
		return endTime;
	}

	/**
	 * Returns the total number of items to process.
	 *
	 * @return number of items to process, 0 or below if unknown
	 */
	public long getItemsToDo() {
		return itemsToDo;
	}

	/**
	 * Sets the unit of the items for display.
	 *
	 * @param itemsUnitSign unit of the items (e.g. "B" for bytes), may be null
	 */
	public void setitemsUnitSign(final String itemsUnitSign) {
		this.itemsUnitSign = itemsUnitSign;
	}

	/**
	 * Returns the unit of the items for display.
	 *
	 * @return unit of the items, may be null
	 */
	public String getitemsUnitSign() {
		return itemsUnitSign;
	}

	/**
	 * Returns the number of items processed so far.
	 *
	 * @return number of processed items
	 */
	public long getItemsDone() {
		return itemsDone;
	}

	@Override
	public final void run() {
		boolean doIt;
		synchronized (this) {
			if (startTime != null) {
				doIt = false;
			} else {
				startTime = LocalDateTime.now();
				doIt = true;
			}
		}

		if (doIt) {
			result = null;
			error = null;
			// "cancel" is not reset, a cancel requested before the start must stay effective
			isDone = false;
			itemsToDo = 0L;
			itemsDone = 0L;

			try {
				result = work();
			} catch (final Exception e) {
				error = e;
			} catch (final Throwable e) {
				error = new Exception("Fatal error occurred: " + e.getMessage(), e);
			}

			if (endTime == null) {
				endTime = LocalDateTime.now();
			}
			isDone = true;
			if (parent != null) {
				// Ensure to let display show 100%
				try {
					Thread.sleep(progressDisplayDelayMilliseconds);
				} catch (@SuppressWarnings("unused") final InterruptedException e) {
					// Do nothing
				}
				parent.receiveDoneSignal(startTime, endTime, itemsDone, itemsUnitSign, getResultText());
			}
		}
	}

	/**
	 * Returns a text describing the result, which is passed to the parent with the done signal.
	 * Subclasses may override it.
	 *
	 * @return result text, null by default
	 */
	@SuppressWarnings("static-method")
	protected String getResultText() {
		return null;
	}

	/**
	 * Does the actual work of the worker. Called once when the worker is run.
	 *
	 * @return result of the work
	 * @throws Exception if the work fails, the error is available via {@link #get()} and {@link #getError()}
	 */
	public abstract T work() throws Exception;
}
