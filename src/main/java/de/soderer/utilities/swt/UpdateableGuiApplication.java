package de.soderer.utilities.swt;

import java.io.File;
import java.time.LocalDateTime;

import org.eclipse.swt.widgets.Display;

import de.soderer.utilities.Credentials;
import de.soderer.utilities.LangResources;
import de.soderer.utilities.Utilities;
import de.soderer.utilities.Version;
import de.soderer.utilities.appupdate.ApplicationUpdateParent;
import de.soderer.utilities.worker.WorkerSimple;

public class UpdateableGuiApplication extends SecureDataGuiApplication implements ApplicationUpdateParent {
	private final String applicationName;
	private final Version applicationVersion;

	private ProgressDialog<?> updateProgressDialog;

	public UpdateableGuiApplication(final Display display, final String applicationName, final Version applicationVersion, final File keystoreFile) throws Exception {
		super(display, keystoreFile);

		if (Utilities.isBlank(applicationName)) {
			throw new Exception("Invalid empty applicationName");
		} else if (applicationVersion == null) {
			throw new Exception("Invalid empty applicationVersion");
		}

		this.applicationName = applicationName;
		this.applicationVersion = applicationVersion;
	}

	@Override
	public Credentials aquireCredentials(final String text, final boolean aquireUsername, final boolean aquirePassword, final boolean firstRequest) throws Exception {
		Credentials downloadCredentials = null;

		if (firstRequest && keystoreExists()) {
			downloadCredentials = getKeystoreValue(Credentials.class, "Update_Credentials");
		}

		if (downloadCredentials == null) {
			final CredentialsDialog downloadCredentialsDialog = new CredentialsDialog(getShell(), "Update " + applicationName, text, aquireUsername, aquirePassword);
			downloadCredentialsDialog.setRememberCredentialsText(LangResources.get("rememberUpdateCredentials"));
			downloadCredentials = downloadCredentialsDialog.open();

			if (downloadCredentials != null && downloadCredentialsDialog.isRememberCredentials()) {
				saveKeyStoreValue("Update_Credentials", downloadCredentials);
			}
		}

		return downloadCredentials;
	}

	@Override
	public void showUpdateError(final String errorText) {
		if (updateProgressDialog != null && !updateProgressDialog.isDisposed()) {
			getDisplay().asyncExec(new Runnable() {
				@Override
				public void run() {
					if (updateProgressDialog != null && !updateProgressDialog.isDisposed()) {
						updateProgressDialog.receiveDoneSignal(null, null, 0, null, null);
					}
				}
			});

			updateProgressDialog = null;
		}

		new QuestionDialog(getShell(), "Update " + applicationName + " Error", errorText).setBackgroundColor(SwtColor.LightRed).open();
	}

	@Override
	public void showUpdateProgress(final LocalDateTime itemStart, final long itemsToDo, final long itemsDone, final String itemsUnitSign) {
		if (updateProgressDialog != null && !updateProgressDialog.isDisposed()) {
			getDisplay().asyncExec(new Runnable() {
				@Override
				public void run() {
					if (updateProgressDialog != null && !updateProgressDialog.isDisposed()) {
						updateProgressDialog.receiveProgressSignal(itemStart, itemsToDo, itemsDone, itemsUnitSign);
					}
				}
			});
		}
	}

	@Override
	public void showUpdateDone(final LocalDateTime startTime, final LocalDateTime endTime, final long itemsDone, final String itemsUnitSign) {
		if (updateProgressDialog != null) {
			updateProgressDialog.receiveDoneSignal(startTime, endTime, itemsDone, itemsUnitSign, null);
			updateProgressDialog = null;
		}

		new QuestionDialog(this, "Update " + applicationName, LangResources.get("updateDone"), LangResources.get("restartNow")).open();
	}

	@Override
	public boolean askForUpdate(final Version availableNewVersion) throws Exception {
		if (availableNewVersion == null) {
			final QuestionDialog questionDialog = new QuestionDialog(getShell(), "Update " + applicationName, LangResources.get("noNewerVersion", applicationName, applicationVersion.toString()));
			if (isDailyUpdateCheckActivated() != null) {
				questionDialog.setCheckboxText(LangResources.get("dailyUpdateCheck"));
				questionDialog.setCheckboxStatus(isDailyUpdateCheckActivated());
			}
			questionDialog.open();
			setDailyUpdateCheckStatus(questionDialog.getCheckboxStatus());
			return false;
		} else {
			final QuestionDialog questionDialog = new QuestionDialog(getShell(), "Update " + applicationName, LangResources.get("newApplicationVersion", availableNewVersion.toString(), applicationName, applicationVersion.toString()) + "\n\n" + LangResources.get("installUpdate"), LangResources.get("yes"), LangResources.get("no"));
			if (isDailyUpdateCheckActivated() != null) {
				questionDialog.setCheckboxText(LangResources.get("dailyUpdateCheck"));
				questionDialog.setCheckboxStatus(isDailyUpdateCheckActivated());
			}
			final Integer buttonIndex = questionDialog.open();
			setDailyUpdateCheckStatus(questionDialog.getCheckboxStatus());
			return buttonIndex != null && buttonIndex == 0;
		}
	}

	@Override
	public void showUpdateDownloadStart(final WorkerSimple<Boolean> worker) {
		updateProgressDialog = new ProgressDialog<>(getShell(), "Update " + applicationName, null, worker);
		updateProgressDialog.open();
	}

	@Override
	public void showUpdateDownloadEnd(final LocalDateTime startTime, final LocalDateTime endTime, final long itemsDone, final String itemsUnitSign) {
		if (updateProgressDialog != null) {
			updateProgressDialog.receiveDoneSignal(startTime, endTime, itemsDone, itemsUnitSign, null);
			updateProgressDialog = null;
		}
	}

	protected void setDailyUpdateCheckStatus(@SuppressWarnings("unused") final boolean checkboxStatus) {
		// Do nothing by standard
	}

	@SuppressWarnings("static-method")
	protected Boolean isDailyUpdateCheckActivated() {
		return null;
	}
}
