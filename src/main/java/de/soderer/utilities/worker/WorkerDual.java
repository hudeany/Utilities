package de.soderer.utilities.worker;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Worker that processes several items (e.g. files) and reports the progress of the current item in addition to
 * the overall progress. With a {@link WorkerParentDual} the item signals are sent as separate signals, with a
 * {@link WorkerParentSimple} they are sent as normal progress signals.
 *
 * @param <T> type of the result
 */
public abstract class WorkerDual<T> extends WorkerSimple<T> {
	/**
	 * Start time of the current item.
	 */
	protected LocalDateTime startTimeSub = null;
	/**
	 * End time of the last finished item.
	 */
	protected LocalDateTime endTimeSub = null;
	/**
	 * Name of the current item, null if no item is in progress.
	 */
	protected String currentItemName = null;
	/**
	 * Size of the current item (e.g. bytes of the current file).
	 */
	protected long subItemsToDo = -1;
	/**
	 * Unit of the sub items for display, may be null.
	 */
	protected String subItemsUnitSign = null;
	/**
	 * Processed part of the current item.
	 */
	protected long subItemsDone = -1;
	/**
	 * Time of the last item progress signal sent to the parent. It is separate from the time of the last overall
	 * progress signal, so frequent item signals do not suppress the overall progress signals.
	 */
	protected LocalDateTime lastItemProgressShow = LocalDateTime.now();

	/**
	 * Creates a worker reporting to the given parent.
	 *
	 * @param parent parent receiving the progress signals, may be null
	 */
	public WorkerDual(final WorkerParentDual parent) {
		super(parent);
	}

	/**
	 * Signals the start of a new item.
	 *
	 * @param itemName name of the item
	 * @param description description of the item, may be null
	 */
	protected void signalItemStart(final String itemName, final String description) {
		currentItemName = itemName;
		startTimeSub = LocalDateTime.now();
		if (parent != null && !cancel) {
			if (parent instanceof WorkerParentDual) {
				((WorkerParentDual) parent).receiveItemStartSignal(currentItemName, description);
			} else {
				parent.receiveUnlimitedProgressSignal();
			}
		}
	}

	/**
	 * Sends the progress of the current item, unless the last signal was sent less than the progress display delay ago.
	 */
	protected void signalItemProgress() {
		signalItemProgress(false);
	}

	/**
	 * Sends the progress of the current item.
	 *
	 * @param overrideRefreshTime true to send the signal regardless of the progress display delay
	 */
	protected void signalItemProgress(final boolean overrideRefreshTime) {
		if (parent != null && !cancel) {
			if (Duration.between(lastItemProgressShow, LocalDateTime.now()).toMillis() > progressDisplayDelayMilliseconds
					|| overrideRefreshTime) {
				if (parent instanceof WorkerParentDual) {
					((WorkerParentDual) parent).receiveItemProgressSignal(startTimeSub, subItemsToDo, subItemsDone, subItemsUnitSign);
				} else {
					parent.receiveProgressSignal(startTimeSub, subItemsToDo, subItemsDone, subItemsUnitSign);
				}
				lastItemProgressShow = LocalDateTime.now();
			}
		}
	}

	/**
	 * Signals that the current item is finished.
	 */
	protected void signalItemDone() {
		signalItemDone(null);
	}

	/**
	 * Signals that the current item is finished, with a text describing its result.
	 * A {@link WorkerParentSimple} gets an overall progress signal instead, because its done signal means that the
	 * whole worker is done.
	 *
	 * @param resultText text describing the result of the item, may be null
	 */
	protected void signalItemDone(final String resultText) {
		endTimeSub = LocalDateTime.now();
		currentItemName = null;
		if (parent != null) {
			if (parent instanceof WorkerParentDual) {
				((WorkerParentDual) parent).receiveItemDoneSignal(startTimeSub, endTimeSub, subItemsDone, subItemsUnitSign, resultText);
			} else {
				signalProgress(true);
			}
		}
	}

	/**
	 * Signals that the progress of the current item cannot be measured.
	 */
	protected void signalUnlimitedSubProgress() {
		if (parent != null) {
			if (parent instanceof WorkerParentDual) {
				((WorkerParentDual) parent).receiveUnlimitedSubProgressSignal();
			} else {
				parent.receiveUnlimitedProgressSignal();
			}
		}
	}
}
