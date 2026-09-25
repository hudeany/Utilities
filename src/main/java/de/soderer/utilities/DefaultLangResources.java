package de.soderer.utilities;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Default German and English translations for all message keys used within soderer-utilities.
 *
 * Placeholders use the "{0}", "{1}", ... syntax of {@link LangResources}, which replaces them literally.
 * There is no MessageFormat quoting, so an apostrophe is written as a plain "'".
 */
public class DefaultLangResources {
	private static final Map<String, String> GERMAN = new LinkedHashMap<>();
	private static final Map<String, String> ENGLISH = new LinkedHashMap<>();

	static {
		// Translations taken from the built-in fallbacks (getI18NString) of the library
		add("cancel",                      "Abbrechen", "Cancel");
		add("canceledByUser",              "Abbruch durch Benutzer", "Canceled by user");
		add("cannotOpenMail",              "Email client konnte nicht gefunden werden.", "Email client was not found.");
		add("close",                       "Schließen", "Close");
		add("copyToClipboard",             "In Zwischenablage kopieren", "Copy to clipboard");
		add("dailyUpdateCheck",            "Tägliche Update Prüfung", "Daily update check");
		add("detailsLess",                 "Details <<<", "Details <<<");
		add("detailsMore",                 "Details >>>", "Details >>>");
		add("enterDownloadCredentials",    "Bitte Userdaten für den Download eingeben", "Enter credentials for download");
		// Texts of enterDownloadPassword and enterDownloadUsername are swapped in ApplicationUpdateUtilities, corrected here
		add("enterDownloadPassword",       "Bitte Passwort für Download eingeben", "Enter password for download");
		add("enterDownloadUsername",       "Bitte Usernamen für Download eingeben", "Enter username for download");
		add("enterPassword",               "Bitte Passwort eingeben", "Please enter password");
		add("enterUsername",               "Bitte Usernamen eingeben", "Please enter username");
		add("error",                       "Fehler", "Error");
		add("errorOccurred",               "Es ist ein Fehler aufgetreten", "An error occurred");
		// GUI variant, UpdateableConsoleApplication uses "Update installieren? (jN)" / "Install update? (yN)"
		add("installUpdate",               "Update installieren?", "Install update?");
		add("newApplicationVersion",       "Es ist eine neue Version {0} verfügbar für {1}.\nDie aktuelle lokale Version ist {2}.", "New version {0} is available for {1}.\nThe current local version is {2}.");
		add("no",                          "Nein", "No");
		add("noNewerVersion",              "Es ist keine neuere Version verfügbar für {0}.\nDie aktuelle lokale Version ist {1}.", "There is no newer version available for {0}.\nThe current local version is {1}.");
		add("noPassword",                  "Kein Passwort", "No password");
		add("ok",                          "OK", "OK");
		add("openMail",                    "Mail mit Fehlerdaten an den Entwickler öffnen", "Open mail with errordata to developer");
		add("password",                    "Passwort", "Password");
		add("passwordRequired",            "Fehler:\nPasswort ist notwendig", "Error:\nPassword is required");
		add("restartNow",                  "Jetzt Anwendung neustarten", "Restart application now");
		add("showSwingProperties",         "Swing-Eigenschaften anzeigen", "Show Swing properties");
		add("sureQuestion",                "Sind sie sich sicher?", "Are you sure?");
		add("swingProperties",             "Swing-Eigenschaften", "Swing properties");
		// GUI variant, UpdateableConsoleApplication uses "Update beendet" / "Update done"
		add("updateDone",                  "Upgedatete Anwendung kann neugestartet werden", "Updated application may be restarted");
		add("updateErrorVersionCheck",     "Fehler beim Ermitteln der aktuellsten Version für ein Update der Applikation:\n{0}", "Update error while checking for new version:\n{0}");
		add("username",                    "Username", "Username");
		add("usernameAndPasswordRequired", "Fehler:\nUsername und Passwort sind notwendig", "Error:\nUsername and password are required");
		add("usernameRequired",            "Fehler:\nUsername ist notwendig", "Error:\nUsername is required");
		add("yes",                         "Ja", "Yes");

		// Translations for keys without a built-in fallback in the library
		add("actionSuccessfullyCompleted",               "Aktion erfolgreich abgeschlossen", "Action successfully completed");
		add("base64Format",                              "Base64-Format", "Base64 format");
		add("browse",                                    "Durchsuchen", "Browse");
		add("buildTime",                                 "Build-Zeit", "Build time");
		add("cannotLoadKeystore",                        "Keystore kann nicht geladen werden", "Cannot load keystore");
		add("cannotReadPrivateKey",                      "Privater Schlüssel kann nicht gelesen werden", "Cannot read private key");
		add("cannotSaveKeystore",                        "Keystore kann nicht gespeichert werden", "Cannot save keystore");
		add("checkHostKey",                              "Host-Key prüfen", "Check host key");
		add("checkUpdate",                               "Nach Update suchen", "Check for update");
		add("configuration",                             "Konfiguration", "Configuration");
		add("confirmNewHostKey",                         "Unbekannter Host-Key für {0}:\n{1}\n\nSoll dieser Host-Key akzeptiert werden?", "Unknown host key for {0}:\n{1}\n\nAccept this host key?");
		add("connect",                                   "Verbinden", "Connect");
		add("containsHeadersInFirstRow",                 "Erste Zeile enthält Spaltennamen", "First row contains column headers");
		add("createDataFile",                            "Datendatei erstellen", "Create data file");
		add("createDesktopLink",                         "Desktop-Verknüpfung erstellen", "Create desktop link");
		add("createFolder",                              "Ordner erstellen", "Create folder");
		add("csvDoubleQuote",                            "Doppeltes Anführungszeichen (\")", "Double quote (\")");
		add("csvSeparator",                              "Trennzeichen", "Separator");
		add("csvSeparatorComma",                         "Komma (,)", "Comma (,)");
		add("csvSeparatorSemicolon",                     "Semikolon (;)", "Semicolon (;)");
		add("csvSingleQuote",                            "Einfaches Anführungszeichen (')", "Single quote (')");
		add("csvUnderfilledLines",                       "Unvollständige Zeilen erlauben", "Allow underfilled lines");
		add("csvUseQuote",                               "Textbegrenzer verwenden", "Use quote character");
		add("currentPath",                               "Aktueller Pfad", "Current path");
		add("days",                                      "Tage", "days");
		add("dbName",                                    "Datenbankname", "Database name");
		add("dbType",                                    "Datenbanktyp", "Database type");
		add("deleteFile",                                "Datei löschen", "Delete file");
		add("deleteFolder",                              "Ordner löschen", "Delete folder");
		add("desktopLinkWasCreatedAt",                   "Desktop-Verknüpfung wurde erstellt:\n{0}", "Desktop link was created at:\n{0}");
		add("discardUnsavedConfigurationChanges",        "Ungespeicherte Konfigurationsänderungen verwerfen?", "Discard unsaved configuration changes?");
		add("disconnect",                                "Trennen", "Disconnect");
		add("download",                                  "Herunterladen", "Download");
		add("duration",                                  "Dauer", "Duration");
		add("ellipticCurveName",                         "Name der elliptischen Kurve", "Elliptic curve name");
		add("encoding",                                  "Zeichenkodierung", "Encoding");
		add("enterFolderName",                           "Bitte Ordnernamen eingeben", "Please enter folder name");
		add("enterKeystorePassword",                     "Bitte Keystore-Passwort eingeben", "Please enter keystore password");
		add("error.userNotAuthenticatedOrNotAuthorized", "Benutzer ist nicht angemeldet oder nicht berechtigt", "User is not authenticated or not authorized");
		add("errorCount",                                "Fehleranzahl", "Error count");
		add("errorMessage",                              "Fehler:\n{0}", "Error:\n{0}");
		add("fileWasNotSaved",                           "Datei wurde nicht gespeichert", "File was not saved");
		add("fingerprintMd5",                            "Fingerprint MD5", "Fingerprint MD5");
		add("fingerprintSha1",                           "Fingerprint SHA-1", "Fingerprint SHA-1");
		add("fingerprintSha256",                         "Fingerprint SHA-256", "Fingerprint SHA-256");
		add("fingerprintSha384",                         "Fingerprint SHA-384", "Fingerprint SHA-384");
		add("fingerprintSha512",                         "Fingerprint SHA-512", "Fingerprint SHA-512");
		add("hexColonFormat",                            "Hex-Format mit Doppelpunkten", "Hex format with colons");
		add("hexFormat",                                 "Hex-Format", "Hex format");
		add("hostname",                                  "Hostname", "Hostname");
		add("hostnameOrIp",                              "Hostname oder IP", "Hostname or IP");
		add("hours",                                     "Stunden", "hours");
		add("keepPasswordInMemory",                      "Passwort im Speicher behalten", "Keep password in memory");
		add("keyFormat",                                 "Schlüsselformat", "Key format");
		add("keyStrength",                               "Schlüsselstärke", "Key strength");
		add("keystrengthExceedsAllowedJCE",              "Die Schlüsselstärke übersteigt das von der JCE erlaubte Maximum", "Key strength exceeds the maximum allowed by JCE");
		add("keyType",                                   "Schlüsseltyp", "Key type");
		add("languageChangeRequiresRestart",             "Die Änderung der Sprache wird erst nach einem Neustart der Anwendung wirksam", "Changing the language requires a restart of the application");
		add("latestDuration",                            "Letzte Dauer", "Latest duration");
		add("latestStatus",                              "Letzter Status", "Latest status");
		add("loadServerEntries",                         "Servereinträge laden", "Load server entries");
		add("maxDuration",                               "Max. Dauer", "Max. duration");
		add("millis",                                    "Millisekunden", "milliseconds");
		add("minDuration",                               "Min. Dauer", "Min. duration");
		add("minutes",                                   "Minuten", "minutes");
		add("None",                                      "Keine", "None");
		add("notFound",                                  "Nicht gefunden", "Not found");
		add("numberOfParallelWorkers",                   "Anzahl paralleler Worker", "Number of parallel workers");
		add("numberOfRepetitionsPerWorker",              "Wiederholungen pro Worker", "Repetitions per worker");
		add("open",                                      "Öffnen", "Open");
		add("openDataFile",                              "Datendatei öffnen", "Open data file");
		add("openKeyFile",                               "Schlüsseldatei öffnen", "Open key file");
		add("pathIsDirectory",                           "Der Pfad ist ein Verzeichnis", "The path is a directory");
		add("port",                                      "Port", "Port");
		add("privateKey",                                "Privater Schlüssel", "Private key");
		add("protocol",                                  "Protokoll", "Protocol");
		add("publicKey",                                 "Öffentlicher Schlüssel", "Public key");
		add("reallyDeleteFile",                          "Datei wirklich löschen?\n{0}", "Really delete file?\n{0}");
		add("reallyDeleteFolder",                        "Ordner wirklich löschen?\n{0}", "Really delete folder?\n{0}");
		add("rememberUpdateCredentials",                 "Zugangsdaten für Updates merken", "Remember update credentials");
		add("removeServerEntry",                         "Servereintrag entfernen", "Remove server entry");
		add("runtimeProperties",                         "Laufzeit-Eigenschaften", "Runtime properties");
		add("save",                                      "Speichern", "Save");
		add("saveData",                                  "Daten speichern", "Save data");
		add("saveFile",                                  "Datei speichern", "Save file");
		add("saveResults",                               "Ergebnisse speichern", "Save results");
		add("saveServerEntry",                           "Servereintrag speichern", "Save server entry");
		add("seconds",                                   "Sekunden", "seconds");
		add("serverEntries",                             "Servereinträge", "Server entries");
		add("sharename",                                 "Freigabename", "Share name");
		add("showRuntimeProperties",                     "Laufzeit-Eigenschaften anzeigen", "Show runtime properties");
		add("showSwtProperties",                         "SWT-Eigenschaften anzeigen", "Show SWT properties");
		add("showSystemProperties",                      "System-Eigenschaften anzeigen", "Show system properties");
		add("start",                                     "Start", "Start");
		add("success",                                   "Erfolg", "Success");
		add("successCount",                              "Erfolgsanzahl", "Success count");
		add("swtProperties",                             "SWT-Eigenschaften", "SWT properties");
		add("systemProperties",                          "System-Eigenschaften", "System properties");
		add("Unknown",                                   "Unbekannt", "Unknown");
		add("upload",                                    "Hochladen", "Upload");
		add("uploadFile",                                "Datei hochladen", "Upload file");
		add("uploadFolder",                              "Ordner hochladen", "Upload folder");
		add("useKeyFile",                                "Schlüsseldatei verwenden", "Use key file");
		add("weeks",                                     "Wochen", "weeks");
		add("window_title",                              "Sichere Daten", "Secure data");
		add("workerSleepTime",                           "Pause zwischen Durchläufen", "Worker sleep time");
		add("years",                                     "Jahre", "years");

		// Labels of the library's own configuration keys (used as "conf." + key in ApplicationConfigurationDialog)
		add("conf.Application.Language",               "Sprache", "Language");
		add("conf.Application.ProxyConfigurationType", "Proxy-Konfiguration", "Proxy configuration");
		add("conf.Application.ProxyUrl",               "Proxy-URL", "Proxy URL");
		add("conf.Application.DailyUpdateCheck",       "Tägliche Update Prüfung", "Daily update check");
	}

	private static void add(final String key, final String germanText, final String englishText) {
		GERMAN.put(key, germanText);
		ENGLISH.put(key, englishText);
	}

	/**
	 * All known message keys in definition order
	 */
	public static Set<String> getKeys() {
		return Collections.unmodifiableSet(ENGLISH.keySet());
	}

	public static boolean containsKey(final String key) {
		return ENGLISH.containsKey(key);
	}

	/**
	 * Default text for a message key.
	 * German for German locales, English for all other locales.
	 *
	 * @return the default text or null if the key is unknown
	 */
	public static String get(final String key, final Locale locale) {
		return getProperties(locale).get(key);
	}

	/**
	 * Default text for a message key in the language of the JVM's default locale
	 *
	 * @return the default text or null if the key is unknown
	 */
	public static String get(final String key) {
		return get(key, Locale.getDefault());
	}

	/**
	 * All default texts of a language.
	 * German for German locales, English for all other locales.
	 */
	public static Map<String, String> getProperties(final Locale locale) {
		if (locale != null && "de".equalsIgnoreCase(locale.getLanguage())) {
			return Collections.unmodifiableMap(GERMAN);
		} else {
			return Collections.unmodifiableMap(ENGLISH);
		}
	}

	/**
	 * Text for a message key from the application's LanguageProperties.
	 * If the key is not defined there, the default text of this class is used.
	 * Both are formatted the same way by {@link LangResources#format(String, Object[])}.
	 */
	public static String getI18NStringOrDefault(final String resourceKey, final Object... arguments) {
		if (LangResources.existsKey(resourceKey)) {
			return LangResources.get(resourceKey, arguments);
		}

		final String pattern = get(resourceKey);
		if (pattern != null) {
			return LangResources.format(pattern, arguments);
		} else if ("de".equalsIgnoreCase(Locale.getDefault().getLanguage())) {
			return "MessageKey unbekannt: " + resourceKey + (arguments != null && arguments.length > 0 ? " Argumente: " + Utilities.join(arguments, ", ") : "");
		} else {
			return "MessageKey unknown: " + resourceKey + (arguments != null && arguments.length > 0 ? " arguments: " + Utilities.join(arguments, ", ") : "");
		}
	}
}
