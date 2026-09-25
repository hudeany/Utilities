package de.soderer.utilities;

import java.awt.HeadlessException;
import java.io.Console;
import java.time.LocalDateTime;

import de.soderer.utilities.appupdate.ApplicationUpdateParent;
import de.soderer.utilities.console.ConsoleType;
import de.soderer.utilities.console.ConsoleUtilities;
import de.soderer.utilities.console.PasswordConsoleInput;
import de.soderer.utilities.console.SimpleConsoleInput;
import de.soderer.utilities.worker.WorkerSimple;

public class UpdateableConsoleApplication implements ApplicationUpdateParent {
	private final String applicationName;
	private final Version applicationVersion;

	public UpdateableConsoleApplication(final String applicationName, final Version applicationVersion) throws HeadlessException {
		this.applicationName = applicationName;
		this.applicationVersion = applicationVersion;
	}

	@Override
	public boolean askForUpdate(final Version availableNewVersion) throws Exception {
		final Console console = System.console();
		if (console == null) {
			throw new Exception("Couldn't get Console instance");
		}
		ConsoleUtilities.clearScreen();
		if (availableNewVersion == null) {
			System.out.println(DefaultLangResources.getI18NStringOrDefault("noNewerVersion", applicationName, applicationVersion.toString()));
			System.out.println();
			return false;
		} else {
			System.out.println(DefaultLangResources.getI18NStringOrDefault("newApplicationVersion", availableNewVersion.toString(), applicationName, applicationVersion.toString()));
			final String input = new SimpleConsoleInput().withPrompt(DefaultLangResources.getI18NStringOrDefault("installUpdate") + " (" + DefaultLangResources.getI18NStringOrDefault("yN") + "): ").readInput();
			System.out.println();
			return input != null && (input.toLowerCase().startsWith("y") || input.toLowerCase().startsWith("j"));
		}
	}

	@Override
	public Credentials aquireCredentials(final String text, final boolean aquireUsername, final boolean aquirePassword, final boolean firstRequest) throws Exception {
		final Console console = System.console();
		if (console == null) {
			throw new Exception("Couldn't get Console instance");
		}

		String userName = null;
		char[] password = null;

		if (aquireUsername && aquirePassword) {
			userName = new SimpleConsoleInput().withPrompt(DefaultLangResources.getI18NStringOrDefault("enterUsername") + ": ").readInput();
			password = new PasswordConsoleInput().withPrompt(DefaultLangResources.getI18NStringOrDefault("enterPassword") + ": ").readInput();
		} else if (aquireUsername) {
			userName = new SimpleConsoleInput().withPrompt(DefaultLangResources.getI18NStringOrDefault("enterUsername") + ": ").readInput();
		} else if (aquirePassword) {
			password = new PasswordConsoleInput().withPrompt(DefaultLangResources.getI18NStringOrDefault("enterPassword") + ": ").readInput();
		}

		if (Utilities.isBlank(userName) && Utilities.isBlank(password)) {
			return null;
		} else {
			return new Credentials(userName, password);
		}
	}

	@Override
	public void showUpdateError(final String errorText) {
		System.err.println(errorText);
		final Console console = System.console();
		if (console != null) {
			System.out.println();
			System.out.println("Press enter to continue");
			System.console().readLine();
		}
	}

	@Override
	public void showUpdateProgress(final LocalDateTime itemStart, final long itemsToDo, final long itemsDone, final String itemsUnitSign) {
		if (ConsoleUtilities.getConsoleType() == ConsoleType.ANSI) {
			ConsoleUtilities.saveCurrentCursorPosition();

			System.out.print(ConsoleUtilities.getConsoleProgressString(80 - 1, itemStart, itemsToDo, itemsDone, itemsUnitSign));

			ConsoleUtilities.moveCursorToSavedPosition();
		} else if (ConsoleUtilities.getConsoleType() == ConsoleType.ANSI) {
			System.out.print(ConsoleUtilities.getConsoleProgressString(80 - 1, itemStart, itemsToDo, itemsDone, itemsUnitSign) + "\n");
		} else {
			System.out.print("\r" + ConsoleUtilities.getConsoleProgressString(80 - 1, itemStart, itemsToDo, itemsDone, itemsUnitSign) + "\r");
		}
	}

	@Override
	public void showUpdateDone(final LocalDateTime startTime, final LocalDateTime endTime, final long itemsDone, final String itemsUnitSign) {
		System.out.println();
		System.out.println(DefaultLangResources.getI18NStringOrDefault("updateDone"));
		System.out.println();
	}

	@Override
	public void showUpdateDownloadStart(final WorkerSimple<Boolean> worker) {
		// Do nothing
	}

	@Override
	public void showUpdateDownloadEnd(final LocalDateTime startTime, final LocalDateTime endTime, final long itemsDone, final String itemsUnitSign) {
		// Do nothing
	}
}
