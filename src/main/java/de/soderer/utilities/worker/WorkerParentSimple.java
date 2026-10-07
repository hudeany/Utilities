package de.soderer.utilities.worker;

import java.time.LocalDateTime;

/**
 * Receiver of the progress signals of a {@link WorkerSimple}, e.g. a progress dialog.
 * The signals are sent from the worker's thread.
 */
public interface WorkerParentSimple {
	/**
	 * Signals that the progress cannot be measured.
	 */
	void receiveUnlimitedProgressSignal();

	/**
	 * Signals the current progress.
	 *
	 * @param start start time of the worker
	 * @param itemsToDo total number of items, 0 or below if unknown
	 * @param itemsDone number of items processed so far
	 * @param itemsUnitSign unit of the items for display, may be null
	 */
	void receiveProgressSignal(LocalDateTime start, long itemsToDo, long itemsDone, String itemsUnitSign);

	/**
	 * Signals that the worker is done (successfully, with error or cancelled).
	 *
	 * @param start start time of the worker
	 * @param end end time of the worker
	 * @param itemsDone number of items processed
	 * @param itemsUnitSign unit of the items for display, may be null
	 * @param resultText text describing the result, may be null
	 */
	void receiveDoneSignal(LocalDateTime start, LocalDateTime end, long itemsDone, String itemsUnitSign, String resultText);

	/**
	 * Changes the title of the progress display.
	 *
	 * @param text new title
	 */
	void changeTitle(String text);

	/**
	 * Called when the worker is cancelled, so the parent can update its display.
	 *
	 * @return true if the parent accepts the cancellation
	 */
	boolean cancel();
}
