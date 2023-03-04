package de.soderer.utilities.worker;

import java.time.Duration;
import java.time.LocalDateTime;

public abstract class WorkerDual<T> extends WorkerSimple<T> {
	protected LocalDateTime startTimeSub = null;
	protected LocalDateTime endTimeSub = null;
	protected String currentItemName = null;
	protected long subItemsToDo = -1;
	protected long subItemsDone = -1;

	public WorkerDual(final WorkerParentDual parent) {
		super(parent);
	}

	protected void signalItemStart(final String itemName) {
		currentItemName = itemName;
		startTimeSub = LocalDateTime.now();
		if (parent != null && !cancel) {
			((WorkerParentDual) parent).receiveItemStartSignal(currentItemName);
		}
	}

	protected void signalItemProgress() {
		signalItemProgress(false);
	}

	protected void signalItemProgress(final boolean overrideRefreshTime) {
		if (parent != null && !cancel) {
			if (Duration.between(lastProgressShow, LocalDateTime.now()).toMillis() > progressDisplayDelayMilliseconds) {
				// Normal progress update
				((WorkerParentDual) parent).receiveProgressSignal(startTimeSub, itemsToDo, itemsDone);
				((WorkerParentDual) parent).receiveItemProgressSignal(startTimeSub, subItemsToDo, subItemsDone);
				lastProgressShow = LocalDateTime.now();
			} else if (overrideRefreshTime) {
				// Important progress update, which may not be left out
				((WorkerParentDual) parent).receiveProgressSignal(startTimeSub, itemsToDo, itemsDone);
				((WorkerParentDual) parent).receiveItemProgressSignal(startTimeSub, subItemsToDo, subItemsDone);
				lastProgressShow = LocalDateTime.now();
			}
		}
	}

	protected void signalItemDone() {
		endTimeSub = LocalDateTime.now();
		if (parent != null) {
			((WorkerParentDual) parent).receiveItemDoneSignal(startTimeSub, endTimeSub, subItemsDone);
			currentItemName = null;
		}
	}

	protected void signalUnlimitedSubProgress() {
		if (parent != null) {
			((WorkerParentDual) parent).receiveUnlimitedSubProgressSignal();
		}
	}
}
