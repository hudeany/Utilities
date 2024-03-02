package de.soderer.utilities.worker;

import java.time.Duration;
import java.time.LocalDateTime;

public abstract class WorkerDual<T> extends WorkerSimple<T> {
	protected LocalDateTime startTimeSub = null;
	protected LocalDateTime endTimeSub = null;
	protected String currentItemName = null;
	protected long subItemsToDo = -1;
	protected String subItemsUnitSign = null;
	protected long subItemsDone = -1;

	public WorkerDual(final WorkerParentDual parent) {
		super(parent);
	}

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

	protected void signalItemProgress() {
		signalItemProgress(false);
	}

	protected void signalItemProgress(final boolean overrideRefreshTime) {
		if (parent != null && !cancel) {
			if (Duration.between(lastProgressShow, LocalDateTime.now()).toMillis() > progressDisplayDelayMilliseconds
					|| overrideRefreshTime) {
				if (parent instanceof WorkerParentDual) {
					((WorkerParentDual) parent).receiveItemProgressSignal(startTimeSub, subItemsToDo, subItemsDone, subItemsUnitSign);
				} else {
					parent.receiveProgressSignal(startTimeSub, subItemsToDo, subItemsDone, subItemsUnitSign);
				}
				lastProgressShow = LocalDateTime.now();
			}
		}
	}

	protected void signalItemDone() {
		endTimeSub = LocalDateTime.now();
		if (parent != null) {
			if (parent instanceof WorkerParentDual) {
				((WorkerParentDual) parent).receiveItemDoneSignal(startTimeSub, endTimeSub, subItemsDone, subItemsUnitSign, null);
			} else {
				parent.receiveDoneSignal(startTimeSub, endTimeSub, subItemsDone, subItemsUnitSign, null);
			}
			currentItemName = null;
		}
	}

	protected void signalItemDone(final String resultText) {
		endTimeSub = LocalDateTime.now();
		if (parent != null) {
			if (parent instanceof WorkerParentDual) {
				((WorkerParentDual) parent).receiveItemDoneSignal(startTimeSub, endTimeSub, subItemsDone, subItemsUnitSign, resultText);
			} else {
				parent.receiveDoneSignal(startTimeSub, endTimeSub, subItemsDone, subItemsUnitSign, resultText);
			}
			currentItemName = null;
		}
	}

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
