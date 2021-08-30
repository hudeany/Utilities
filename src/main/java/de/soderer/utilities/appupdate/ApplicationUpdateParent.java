package de.soderer.utilities.appupdate;

import java.util.Date;

import de.soderer.utilities.CredentialsParent;
import de.soderer.utilities.Version;
import de.soderer.utilities.worker.WorkerSimple;

public interface ApplicationUpdateParent extends CredentialsParent {
	void showUpdateError(String errorText);

	void showUpdateProgress(Date start, long itemsToDo, long itemsDone);

	void showUpdateDone();

	boolean askForUpdate(Version availableNewVersion) throws Exception;

	void showUpdateDownloadStart(WorkerSimple<Boolean> worker);

	void showUpdateDownloadEnd();
}
