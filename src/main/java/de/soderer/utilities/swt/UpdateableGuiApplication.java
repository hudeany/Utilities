package de.soderer.utilities.swt;

import java.io.File;
import java.text.MessageFormat;
import java.util.Date;
import java.util.Locale;

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

	public UpdateableGuiApplication(final Display display, final String applicationName, final Version applicationVersion, final File keystoreFile, final byte[] keystoreSalt) throws Exception {
		super(display, keystoreFile, keystoreSalt);

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
						updateProgressDialog.showDone(null, null, 0);
					}
				}
			});

			updateProgressDialog = null;
		}

		new QuestionDialog(getShell(), "Update " + applicationName + " Error", errorText).setBackgroundColor(SwtColor.LightRed).open();
	}

	@Override
	public void showUpdateProgress(final Date itemStart, final long itemsToDo, final long itemsDone) {
		if (updateProgressDialog != null && !updateProgressDialog.isDisposed()) {
			getDisplay().asyncExec(new Runnable() {
				@Override
				public void run() {
					if (updateProgressDialog != null && !updateProgressDialog.isDisposed()) {
						updateProgressDialog.showProgress(itemStart, itemsToDo, itemsDone);
					}
				}
			});
		}
	}

	@Override
	public void showUpdateDone(final Date startTime, final Date endTime, final long itemsDone) {
		if (updateProgressDialog != null) {
			updateProgressDialog.showDone(startTime, endTime, itemsDone);
			updateProgressDialog = null;
		}

		new QuestionDialog(this, "Update " + applicationName, getI18NString("updateDone"), getI18NString("restartNow")).open();
	}

	@Override
	public boolean askForUpdate(final Version availableNewVersion) throws Exception {
		if (availableNewVersion == null) {
			final QuestionDialog questionDialog = new QuestionDialog(getShell(), "Update " + applicationName, getI18NString("noNewerVersion", applicationName, applicationVersion.toString()));
			if (isDailyUpdateCheckActivated() != null) {
				questionDialog.setCheckboxText(getI18NString("dailyUpdateCheck"));
				questionDialog.setCheckboxStatus(isDailyUpdateCheckActivated());
			}
			questionDialog.open();
			setDailyUpdateCheckStatus(questionDialog.getCheckboxStatus());
			return false;
		} else {
			final QuestionDialog questionDialog = new QuestionDialog(getShell(), "Update " + applicationName, getI18NString("newApplicationVersion", availableNewVersion.toString(), applicationName, applicationVersion.toString()) + "\n\n" + getI18NString("installUpdate"), getI18NString("yes"), getI18NString("no"));
			if (isDailyUpdateCheckActivated() != null) {
				questionDialog.setCheckboxText(getI18NString("dailyUpdateCheck"));
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
	public void showUpdateDownloadEnd(final Date startTime, final Date endTime, final long itemsDone) {
		if (updateProgressDialog != null) {
			updateProgressDialog.showDone(startTime, endTime, itemsDone);
			updateProgressDialog = null;
		}
	}

	private static String getI18NString(final String resourceKey, final Object... arguments) {
		if (LangResources.existsKey(resourceKey)) {
			return LangResources.get(resourceKey, arguments);
		} else if ("de".equalsIgnoreCase(Locale.getDefault().getLanguage())) {
			String pattern;
			switch(resourceKey) {
				case "noNewerVersion": pattern = "Es ist keine neuere Version verfügbar für {0}.\nDie aktuelle lokale Version ist {1}."; break;
				case "newApplicationVersion": pattern = "Es ist eine neue Version {0} verfügbar für {1}.\nDie aktuelle lokale Version ist {2}."; break;
				case "dailyUpdateCheck": pattern = "Tägliche Update Prüfung"; break;
				case "installUpdate": pattern = "Update installieren?"; break;
				case "yes": pattern = "Ja"; break;
				case "no": pattern = "Nein"; break;
				case "close": pattern = "Schließen"; break;
				case "updateDone": pattern = "Upgedatete Anwendung kann neugestartet werden"; break;
				case "restartNow": pattern = "Jetzt neustarten"; break;
				default: pattern = "MessageKey unbekannt: " + resourceKey + (arguments != null && arguments.length > 0 ? " Argumente: " + Utilities.join(arguments, ", ") : "");
			}
			return MessageFormat.format(pattern, arguments);
		} else {
			String pattern;
			switch(resourceKey) {
				case "noNewerVersion": pattern = "There is no newer version available for {0}.\nThe current local version is {1}."; break;
				case "newApplicationVersion": pattern = "New version {0} is available for {1}.\nThe current local version is {2}."; break;
				case "dailyUpdateCheck": pattern = "Daily update check"; break;
				case "installUpdate": pattern = "Install update?"; break;
				case "yes": pattern = "Yes"; break;
				case "no": pattern = "No"; break;
				case "close": pattern = "Close"; break;
				case "updateDone": pattern = "Updated application may be restarted"; break;
				case "restartNow": pattern = "Restart now"; break;
				default: pattern = "MessageKey unknown: " + resourceKey + (arguments != null && arguments.length > 0 ? " arguments: " + Utilities.join(arguments, ", ") : "");
			}
			return MessageFormat.format(pattern, arguments);
		}
	}

	protected void setDailyUpdateCheckStatus(@SuppressWarnings("unused") final boolean checkboxStatus) {
		// Do nothing by standard
	}

	protected Boolean isDailyUpdateCheckActivated() {
		return null;
	}
}
