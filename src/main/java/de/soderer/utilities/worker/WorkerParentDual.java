package de.soderer.utilities.worker;

import java.time.LocalDateTime;

public interface WorkerParentDual extends WorkerParentSimple {
	void receiveUnlimitedSubProgressSignal();

	void receiveItemStartSignal(String itemName);

	void receiveItemProgressSignal(LocalDateTime itemStart, long subItemToDo, long subItemDone);

	void receiveItemDoneSignal(LocalDateTime itemStart, LocalDateTime itemEnd, long subItemsDone);
}
