package de.soderer.utilities.worker;

import java.time.LocalDateTime;

/**
 * Receiver of the progress signals of a {@link WorkerDual}, with additional signals for the progress of the current item.
 */
public interface WorkerParentDual extends WorkerParentSimple {
	/**
	 * Signals that the progress of the current item cannot be measured.
	 */
	void receiveUnlimitedSubProgressSignal();

	/**
	 * Signals the start of a new item.
	 *
	 * @param itemName name of the item
	 * @param description description of the item, may be null
	 */
	void receiveItemStartSignal(String itemName, String description);

	/**
	 * Signals the progress of the current item.
	 *
	 * @param itemStart start time of the item
	 * @param subItemToDo size of the item, 0 or below if unknown
	 * @param subItemDone processed part of the item
	 * @param itemsUnitSign unit for display, may be null
	 */
	void receiveItemProgressSignal(LocalDateTime itemStart, long subItemToDo, long subItemDone, String itemsUnitSign);

	/**
	 * Signals that the current item is finished.
	 *
	 * @param itemStart start time of the item
	 * @param itemEnd end time of the item
	 * @param subItemsDone processed part of the item
	 * @param itemsUnitSign unit for display, may be null
	 * @param resultText text describing the result of the item, may be null
	 */
	void receiveItemDoneSignal(LocalDateTime itemStart, LocalDateTime itemEnd, long subItemsDone, String itemsUnitSign, String resultText);
}
