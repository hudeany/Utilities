package de.soderer.utilities;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Language dependent texts (I18N).
 *
 * <p>
 * Texts are looked up in this order:
 * </p>
 * <ol>
 * <li>The application's own language files "LanguageProperties*.properties"
 * in the classpath root, if the application brings any. These always take
 * precedence, so an application may also override any built-in text.</li>
 * <li>The built-in texts (German, English, French, Spanish and Italian):
 * the general texts of this library and the texts other libraries registered
 * via {@link #registerBuiltInText(String, String, String, String, String, String)}.
 * Other languages fall back to English.</li>
 * </ol>
 *
 * <p>
 * So the library's dialogs always work, even for applications without any
 * language files of their own.
 * </p>
 *
 * <p>
 * Libraries bring their own texts by registering them once, e.g. in a static
 * initializer of the class that uses them:
 * </p>
 *
 * <pre>
 * static {
 * 	LangResources.registerBuiltInText("myKey", "English", "Deutsch", "Français", "Español", "Italiano");
 * }
 * </pre>
 *
 * <p>
 * Placeholders use the "{0}", "{1}", ... syntax, which is replaced literally
 * (see {@link #format(String, Object[])}). There is no MessageFormat quoting,
 * so an apostrophe is written as a plain "'".
 * </p>
 */
public class LangResources {
	/** Base name of the language property files in the classpath root (e.g. "LanguageProperties_de.properties") */
	private static final String BUNDLE_NAME = "LanguageProperties";

	/** Matches a placeholder like "{0}" */
	private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{(\\d+)\\}");

	/** Matches the file name of a language file and extracts its language code like "de", "de_CH" or "de_BY" */
	private static final Pattern LANGUAGE_FILE_NAME_PATTERN = Pattern.compile("^" + Pattern.quote(BUNDLE_NAME) + "_([a-z]{2,3}(?:_[A-Za-z0-9]{2,8})*)\\.properties$");

	/**
	 * Built-in texts per language, only modified while holding the class lock.
	 * Readers use the immutable snapshot {@link #builtInTexts} instead.
	 */
	private static final Map<String, String> BUILT_IN_GERMAN = new LinkedHashMap<>();
	private static final Map<String, String> BUILT_IN_ENGLISH = new LinkedHashMap<>();
	private static final Map<String, String> BUILT_IN_FRENCH = new LinkedHashMap<>();
	private static final Map<String, String> BUILT_IN_SPANISH = new LinkedHashMap<>();
	private static final Map<String, String> BUILT_IN_ITALIAN = new LinkedHashMap<>();

	/**
	 * Immutable snapshot of the built-in texts, replaced as a whole on each
	 * registration, so readers need no locking
	 */
	private static volatile BuiltInTexts builtInTexts;

	private static final class BuiltInTexts {
		private final Map<String, String> english;
		private final Map<String, String> german;
		private final Map<String, String> french;
		private final Map<String, String> spanish;
		private final Map<String, String> italian;

		private BuiltInTexts() {
			english = Collections.unmodifiableMap(new LinkedHashMap<>(BUILT_IN_ENGLISH));
			german = Collections.unmodifiableMap(new LinkedHashMap<>(BUILT_IN_GERMAN));
			french = Collections.unmodifiableMap(new LinkedHashMap<>(BUILT_IN_FRENCH));
			spanish = Collections.unmodifiableMap(new LinkedHashMap<>(BUILT_IN_SPANISH));
			italian = Collections.unmodifiableMap(new LinkedHashMap<>(BUILT_IN_ITALIAN));
		}
	}

	/** Language codes of all language files found in the classpath (without the root file) */
	private static List<String> availableLanguages;

	/**
	 * Texts of the current language, loaded on first use. Always replaced as a
	 * whole, so concurrent readers never see a partially initialized state.
	 */
	private static volatile LanguageTexts languageTexts;

	/**
	 * The application's resource bundle (null, if the application has no
	 * language files) and the locale for picking the built-in texts. The
	 * built-in texts are looked up on each access, so texts registered later
	 * are found, too.
	 */
	private static final class LanguageTexts {
		private final ResourceBundle applicationBundle;
		private final Locale locale;

		private LanguageTexts(final ResourceBundle applicationBundle, final Locale locale) {
			this.applicationBundle = applicationBundle;
			this.locale = locale;
		}
	}

	private LangResources() {
		// Utility class
	}

	static {
		// Parameter order of add(): key, English, German, French, Spanish, Italian
		// "yN"/"Yn" contain the localized answer letters (upper case marks the default). Console input has to accept these letters (and should also accept "y")
		addBuiltInText("cancel",                      "Cancel", "Abbrechen", "Annuler", "Cancelar", "Annulla");
		addBuiltInText("canceledByUser",              "Canceled by user", "Abbruch durch Benutzer", "Annulé par l'utilisateur", "Cancelado por el usuario", "Annullato dall'utente");
		addBuiltInText("cannotOpenMail",              "Email client was not found.", "Email client konnte nicht gefunden werden.", "Le client de messagerie est introuvable.", "No se encontró el cliente de correo.", "Il client di posta non è stato trovato.");
		addBuiltInText("close",                       "Close", "Schließen", "Fermer", "Cerrar", "Chiudi");
		addBuiltInText("copyToClipboard",             "Copy to clipboard", "In Zwischenablage kopieren", "Copier dans le presse-papiers", "Copiar al portapapeles", "Copia negli appunti");
		addBuiltInText("dailyUpdateCheck",            "Daily update check", "Tägliche Update Prüfung", "Vérification quotidienne des mises à jour", "Comprobación diaria de actualizaciones", "Controllo giornaliero degli aggiornamenti");
		addBuiltInText("detailsLess",                 "Details <<<", "Details <<<", "Détails <<<", "Detalles <<<", "Dettagli <<<");
		addBuiltInText("detailsMore",                 "Details >>>", "Details >>>", "Détails >>>", "Detalles >>>", "Dettagli >>>");
		addBuiltInText("enterDownloadCredentials",    "Enter credentials for download", "Bitte Userdaten für den Download eingeben", "Saisissez les identifiants pour le téléchargement", "Introduzca las credenciales para la descarga", "Inserire le credenziali per il download");
		addBuiltInText("enterDownloadPassword",       "Enter password for download", "Bitte Passwort für Download eingeben", "Saisissez le mot de passe pour le téléchargement", "Introduzca la contraseña para la descarga", "Inserire la password per il download");
		addBuiltInText("enterDownloadUsername",       "Enter username for download", "Bitte Usernamen für Download eingeben", "Saisissez le nom d'utilisateur pour le téléchargement", "Introduzca el nombre de usuario para la descarga", "Inserire il nome utente per il download");
		addBuiltInText("enterPassword",               "Please enter password", "Bitte Passwort eingeben", "Veuillez saisir le mot de passe", "Introduzca la contraseña", "Inserire la password");
		addBuiltInText("enterUsername",               "Please enter username", "Bitte Usernamen eingeben", "Veuillez saisir le nom d'utilisateur", "Introduzca el nombre de usuario", "Inserire il nome utente");
		addBuiltInText("error",                       "Error", "Fehler", "Erreur", "Error", "Errore");
		addBuiltInText("errorOccurred",               "An error occurred", "Es ist ein Fehler aufgetreten", "Une erreur s'est produite", "Se ha producido un error", "Si è verificato un errore");
		addBuiltInText("installUpdate",               "Install update?", "Update installieren?", "Installer la mise à jour ?", "¿Instalar la actualización?", "Installare l'aggiornamento?");
		addBuiltInText("updateErrorApplicationInUse", "The application file '{0}' is still in use by another running instance of the application (process id {1}), e.g. in another window.\nThe update cannot be installed while the file is in use.\nPlease close all other windows of the application and start the update again.", "Die Anwendungsdatei '{0}' wird noch von einer anderen laufenden Instanz der Anwendung verwendet (Prozess-ID {1}), z.B. in einem anderen Fenster.\nW\u00e4hrend die Datei in Benutzung ist, kann das Update nicht installiert werden.\nBitte schlie\u00dfen Sie alle anderen Fenster der Anwendung und starten Sie das Update erneut.", "Le fichier d'application '{0}' est encore utilis\u00e9 par une autre instance en cours d'ex\u00e9cution de l'application (ID de processus {1}), par ex. dans une autre fen\u00eatre.\nLa mise \u00e0 jour ne peut pas \u00eatre install\u00e9e tant que le fichier est utilis\u00e9.\nVeuillez fermer toutes les autres fen\u00eatres de l'application et relancer la mise \u00e0 jour.", "El archivo de la aplicaci\u00f3n '{0}' todav\u00eda est\u00e1 en uso por otra instancia en ejecuci\u00f3n de la aplicaci\u00f3n (ID de proceso {1}), p. ej. en otra ventana.\nLa actualizaci\u00f3n no se puede instalar mientras el archivo est\u00e9 en uso.\nCierre todas las dem\u00e1s ventanas de la aplicaci\u00f3n y vuelva a iniciar la actualizaci\u00f3n.", "Il file dell'applicazione '{0}' \u00e8 ancora in uso da un'altra istanza in esecuzione dell'applicazione (ID processo {1}), ad es. in un'altra finestra.\nL'aggiornamento non pu\u00f2 essere installato mentre il file \u00e8 in uso.\nChiudere tutte le altre finestre dell'applicazione e avviare di nuovo l'aggiornamento.");
		addBuiltInText("yN",                          "y/N", "j/N", "o/N", "s/N", "s/N");
		addBuiltInText("Yn",                          "Y/n", "J/n", "O/n", "S/n", "S/n");
		addBuiltInText("newApplicationVersion",       "New version {0} is available for {1}.\nThe current local version is {2}.", "Es ist eine neue Version {0} verfügbar für {1}.\nDie aktuelle lokale Version ist {2}.", "Une nouvelle version {0} est disponible pour {1}.\nLa version locale actuelle est {2}.", "Hay una nueva versión {0} disponible para {1}.\nLa versión local actual es {2}.", "È disponibile una nuova versione {0} per {1}.\nLa versione locale attuale è {2}.");
		addBuiltInText("no",                          "No", "Nein", "Non", "No", "No");
		addBuiltInText("noNewerVersion",              "There is no newer version available for {0}.\nThe current local version is {1}.", "Es ist keine neuere Version verfügbar für {0}.\nDie aktuelle lokale Version ist {1}.", "Aucune version plus récente n'est disponible pour {0}.\nLa version locale actuelle est {1}.", "No hay ninguna versión más reciente disponible para {0}.\nLa versión local actual es {1}.", "Non è disponibile una versione più recente per {0}.\nLa versione locale attuale è {1}.");
		addBuiltInText("noPassword",                  "No password", "Kein Passwort", "Pas de mot de passe", "Sin contraseña", "Nessuna password");
		addBuiltInText("ok",                          "OK", "OK", "OK", "Aceptar", "OK");
		addBuiltInText("openMail",                    "Open mail with errordata to developer", "Mail mit Fehlerdaten an den Entwickler öffnen", "Ouvrir un e-mail au développeur avec les données d'erreur", "Abrir un correo al desarrollador con los datos del error", "Apri un'e-mail allo sviluppatore con i dati dell'errore");
		addBuiltInText("password",                    "Password", "Passwort", "Mot de passe", "Contraseña", "Password");
		addBuiltInText("passwordRequired",            "Error:\nPassword is required", "Fehler:\nPasswort ist notwendig", "Erreur :\nLe mot de passe est obligatoire", "Error:\nLa contraseña es obligatoria", "Errore:\nLa password è obbligatoria");
		addBuiltInText("passwordConfirmation",        "Confirm password", "Passwort bestätigen", "Confirmer le mot de passe", "Confirmar contraseña", "Conferma password");
		addBuiltInText("restartNow",                  "Restart application now", "Jetzt Anwendung neustarten", "Redémarrer l'application maintenant", "Reiniciar la aplicación ahora", "Riavvia l'applicazione ora");
		addBuiltInText("showSwingProperties",         "Show Swing properties", "Swing-Eigenschaften anzeigen", "Afficher les propriétés Swing", "Mostrar propiedades de Swing", "Mostra proprietà Swing");
		addBuiltInText("sureQuestion",                "Are you sure?", "Sind sie sich sicher?", "Êtes-vous sûr ?", "¿Está seguro?", "Sei sicuro?");
		addBuiltInText("swingProperties",             "Swing properties", "Swing-Eigenschaften", "Propriétés Swing", "Propiedades de Swing", "Proprietà Swing");
		addBuiltInText("updateDone",                  "Updated application may be restarted", "Upgedatete Anwendung kann neugestartet werden", "L'application mise à jour peut être redémarrée", "La aplicación actualizada puede reiniciarse", "L'applicazione aggiornata può essere riavviata");
		addBuiltInText("updateErrorVersionCheck",     "Update error while checking for new version:\n{0}", "Fehler beim Ermitteln der aktuellsten Version für ein Update der Applikation:\n{0}", "Erreur lors de la recherche d'une nouvelle version pour la mise à jour :\n{0}", "Error al buscar una nueva versión para la actualización:\n{0}", "Errore durante la ricerca di una nuova versione per l'aggiornamento:\n{0}");
		addBuiltInText("username",                    "Username", "Username", "Nom d'utilisateur", "Nombre de usuario", "Nome utente");
		addBuiltInText("usernameAndPasswordRequired", "Error:\nUsername and password are required", "Fehler:\nUsername und Passwort sind notwendig", "Erreur :\nLe nom d'utilisateur et le mot de passe sont obligatoires", "Error:\nEl nombre de usuario y la contraseña son obligatorios", "Errore:\nNome utente e password sono obbligatori");
		addBuiltInText("usernameRequired",            "Error:\nUsername is required", "Fehler:\nUsername ist notwendig", "Erreur :\nLe nom d'utilisateur est obligatoire", "Error:\nEl nombre de usuario es obligatorio", "Errore:\nIl nome utente è obbligatorio");
		addBuiltInText("yes",                         "Yes", "Ja", "Oui", "Sí", "Sì");
		addBuiltInText("askEachTime",                 "Ask each time", "Jedes Mal nachfragen", "", "", "");

		// Translations for keys without a built-in fallback in the library
		addBuiltInText("actionSuccessfullyCompleted",               "Action successfully completed", "Aktion erfolgreich abgeschlossen", "Action terminée avec succès", "Acción completada correctamente", "Azione completata con successo");
		addBuiltInText("base64Format",                              "Base64 format", "Base64-Format", "Format Base64", "Formato Base64", "Formato Base64");
		addBuiltInText("browse",                                    "Browse", "Durchsuchen", "Parcourir", "Examinar", "Sfoglia");
		addBuiltInText("buildTime",                                 "Build time", "Build-Zeit", "Date de build", "Fecha de compilación", "Data di build");
		addBuiltInText("cannotLoadKeystore",                        "Cannot load keystore", "Keystore kann nicht geladen werden", "Impossible de charger le keystore", "No se puede cargar el almacén de claves", "Impossibile caricare il keystore");
		addBuiltInText("cannotReadPrivateKey",                      "Cannot read private key", "Privater Schlüssel kann nicht gelesen werden", "Impossible de lire la clé privée", "No se puede leer la clave privada", "Impossibile leggere la chiave privata");
		addBuiltInText("cannotSaveKeystore",                        "Cannot save keystore", "Keystore kann nicht gespeichert werden", "Impossible d'enregistrer le keystore", "No se puede guardar el almacén de claves", "Impossibile salvare il keystore");
		addBuiltInText("checkHostKey",                              "Check host key", "Host-Key prüfen", "Vérifier la clé d'hôte", "Comprobar la clave del host", "Verifica la chiave host");
		addBuiltInText("checkUpdate",                               "Check for update", "Nach Update suchen", "Rechercher une mise à jour", "Buscar actualizaciones", "Cerca aggiornamenti");
		addBuiltInText("configuration",                             "Configuration", "Konfiguration", "Configuration", "Configuración", "Configurazione");
		addBuiltInText("confirmNewHostKey",                         "Unknown host key for {0}:\n{1}\n\nAccept this host key?", "Unbekannter Host-Key für {0}:\n{1}\n\nSoll dieser Host-Key akzeptiert werden?", "Clé d'hôte inconnue pour {0} :\n{1}\n\nAccepter cette clé d'hôte ?", "Clave de host desconocida para {0}:\n{1}\n\n¿Aceptar esta clave de host?", "Chiave host sconosciuta per {0}:\n{1}\n\nAccettare questa chiave host?");
		addBuiltInText("connect",                                   "Connect", "Verbinden", "Connecter", "Conectar", "Connetti");
		addBuiltInText("containsHeadersInFirstRow",                 "First row contains column headers", "Erste Zeile enthält Spaltennamen", "La première ligne contient les noms de colonnes", "La primera fila contiene los nombres de columna", "La prima riga contiene i nomi delle colonne");
		addBuiltInText("createDataFile",                            "Create data file", "Datendatei erstellen", "Créer un fichier de données", "Crear archivo de datos", "Crea file di dati");
		addBuiltInText("createDesktopLink",                         "Create desktop link", "Desktop-Verknüpfung erstellen", "Créer un raccourci sur le bureau", "Crear acceso directo en el escritorio", "Crea collegamento sul desktop");
		addBuiltInText("createFolder",                              "Create folder", "Ordner erstellen", "Créer un dossier", "Crear carpeta", "Crea cartella");
		addBuiltInText("csvDoubleQuote",                            "Double quote (\")", "Doppeltes Anführungszeichen (\")", "Guillemet double (\")", "Comillas dobles (\")", "Virgolette doppie (\")");
		addBuiltInText("csvSeparator",                              "Separator", "Trennzeichen", "Séparateur", "Separador", "Separatore");
		addBuiltInText("csvSeparatorComma",                         "Comma (,)", "Komma (,)", "Virgule (,)", "Coma (,)", "Virgola (,)");
		addBuiltInText("csvSeparatorSemicolon",                     "Semicolon (;)", "Semikolon (;)", "Point-virgule (;)", "Punto y coma (;)", "Punto e virgola (;)");
		addBuiltInText("csvSingleQuote",                            "Single quote (')", "Einfaches Anführungszeichen (')", "Guillemet simple (')", "Comilla simple (')", "Virgoletta singola (')");
		addBuiltInText("csvUnderfilledLines",                       "Allow underfilled lines", "Unvollständige Zeilen erlauben", "Autoriser les lignes incomplètes", "Permitir líneas incompletas", "Consenti righe incomplete");
		addBuiltInText("csvUseQuote",                               "Use quote character", "Textbegrenzer verwenden", "Utiliser un délimiteur de texte", "Usar delimitador de texto", "Usa delimitatore di testo");
		addBuiltInText("currentPath",                               "Current path", "Aktueller Pfad", "Chemin actuel", "Ruta actual", "Percorso attuale");
		addBuiltInText("days",                                      "days", "Tage", "jours", "días", "giorni");
		addBuiltInText("dbName",                                    "Database name", "Datenbankname", "Nom de la base de données", "Nombre de la base de datos", "Nome del database");
		addBuiltInText("dbType",                                    "Database type", "Datenbanktyp", "Type de base de données", "Tipo de base de datos", "Tipo di database");
		addBuiltInText("deleteFile",                                "Delete file", "Datei löschen", "Supprimer le fichier", "Eliminar archivo", "Elimina file");
		addBuiltInText("deleteFolder",                              "Delete folder", "Ordner löschen", "Supprimer le dossier", "Eliminar carpeta", "Elimina cartella");
		addBuiltInText("desktopLinkWasCreatedAt",                   "Desktop link was created at:\n{0}", "Desktop-Verknüpfung wurde erstellt:\n{0}", "Le raccourci sur le bureau a été créé :\n{0}", "Se ha creado el acceso directo en el escritorio:\n{0}", "Il collegamento sul desktop è stato creato:\n{0}");
		addBuiltInText("discardUnsavedConfigurationChanges",        "Discard unsaved configuration changes?", "Ungespeicherte Konfigurationsänderungen verwerfen?", "Abandonner les modifications de configuration non enregistrées ?", "¿Descartar los cambios de configuración no guardados?", "Scartare le modifiche alla configurazione non salvate?");
		addBuiltInText("disconnect",                                "Disconnect", "Trennen", "Déconnecter", "Desconectar", "Disconnetti");
		addBuiltInText("download",                                  "Download", "Herunterladen", "Télécharger", "Descargar", "Scarica");
		addBuiltInText("duration",                                  "Duration", "Dauer", "Durée", "Duración", "Durata");
		addBuiltInText("ellipticCurveName",                         "Elliptic curve name", "Name der elliptischen Kurve", "Nom de la courbe elliptique", "Nombre de la curva elíptica", "Nome della curva ellittica");
		addBuiltInText("encoding",                                  "Encoding", "Zeichenkodierung", "Encodage", "Codificación", "Codifica");
		addBuiltInText("enterFolderName",                           "Please enter folder name", "Bitte Ordnernamen eingeben", "Veuillez saisir le nom du dossier", "Introduzca el nombre de la carpeta", "Inserire il nome della cartella");
		addBuiltInText("enterKeystorePassword",                     "Please enter keystore password", "Bitte Keystore-Passwort eingeben", "Veuillez saisir le mot de passe du keystore", "Introduzca la contraseña del almacén de claves", "Inserire la password del keystore");
		addBuiltInText("error.userNotAuthenticatedOrNotAuthorized", "User is not authenticated or not authorized", "Benutzer ist nicht angemeldet oder nicht berechtigt", "L'utilisateur n'est pas authentifié ou pas autorisé", "El usuario no está autenticado o no está autorizado", "L'utente non è autenticato o non è autorizzato");
		addBuiltInText("errorCount",                                "Error count", "Fehleranzahl", "Nombre d'erreurs", "Número de errores", "Numero di errori");
		addBuiltInText("errorMessage",                              "Error:\n{0}", "Fehler:\n{0}", "Erreur :\n{0}", "Error:\n{0}", "Errore:\n{0}");
		addBuiltInText("fileWasNotSaved",                           "File was not saved", "Datei wurde nicht gespeichert", "Le fichier n'a pas été enregistré", "El archivo no se ha guardado", "Il file non è stato salvato");
		addBuiltInText("fingerprintMd5",                            "Fingerprint MD5", "Fingerprint MD5", "Empreinte MD5", "Huella digital MD5", "Impronta MD5");
		addBuiltInText("fingerprintSha1",                           "Fingerprint SHA-1", "Fingerprint SHA-1", "Empreinte SHA-1", "Huella digital SHA-1", "Impronta SHA-1");
		addBuiltInText("fingerprintSha256",                         "Fingerprint SHA-256", "Fingerprint SHA-256", "Empreinte SHA-256", "Huella digital SHA-256", "Impronta SHA-256");
		addBuiltInText("fingerprintSha384",                         "Fingerprint SHA-384", "Fingerprint SHA-384", "Empreinte SHA-384", "Huella digital SHA-384", "Impronta SHA-384");
		addBuiltInText("fingerprintSha512",                         "Fingerprint SHA-512", "Fingerprint SHA-512", "Empreinte SHA-512", "Huella digital SHA-512", "Impronta SHA-512");
		addBuiltInText("hexColonFormat",                            "Hex format with colons", "Hex-Format mit Doppelpunkten", "Format hexadécimal avec deux-points", "Formato hexadecimal con dos puntos", "Formato esadecimale con due punti");
		addBuiltInText("hexFormat",                                 "Hex format", "Hex-Format", "Format hexadécimal", "Formato hexadecimal", "Formato esadecimale");
		addBuiltInText("hostname",                                  "Hostname", "Hostname", "Nom d'hôte", "Nombre de host", "Nome host");
		addBuiltInText("hostnameOrIp",                              "Hostname or IP", "Hostname oder IP", "Nom d'hôte ou IP", "Nombre de host o IP", "Nome host o IP");
		addBuiltInText("hours",                                     "hours", "Stunden", "heures", "horas", "ore");
		addBuiltInText("keepPasswordInMemory",                      "Keep password in memory", "Passwort im Speicher behalten", "Conserver le mot de passe en mémoire", "Mantener la contraseña en memoria", "Mantieni la password in memoria");
		addBuiltInText("keyFormat",                                 "Key format", "Schlüsselformat", "Format de clé", "Formato de clave", "Formato della chiave");
		addBuiltInText("keyStrength",                               "Key strength", "Schlüsselstärke", "Force de la clé", "Longitud de clave", "Lunghezza della chiave");
		addBuiltInText("keystrengthExceedsAllowedJCE",              "Key strength exceeds the maximum allowed by JCE", "Die Schlüsselstärke übersteigt das von der JCE erlaubte Maximum", "La force de la clé dépasse le maximum autorisé par la JCE", "La longitud de clave supera el máximo permitido por la JCE", "La lunghezza della chiave supera il massimo consentito dalla JCE");
		addBuiltInText("keyType",                                   "Key type", "Schlüsseltyp", "Type de clé", "Tipo de clave", "Tipo di chiave");
		addBuiltInText("languageChangeRequiresRestart",             "Changing the language requires a restart of the application", "Die Änderung der Sprache wird erst nach einem Neustart der Anwendung wirksam", "Le changement de langue ne prend effet qu'après un redémarrage de l'application", "El cambio de idioma solo se aplicará tras reiniciar la aplicación", "Il cambio di lingua avrà effetto solo dopo il riavvio dell'applicazione");
		addBuiltInText("latestDuration",                            "Latest duration", "Letzte Dauer", "Dernière durée", "Última duración", "Ultima durata");
		addBuiltInText("latestStatus",                              "Latest status", "Letzter Status", "Dernier statut", "Último estado", "Ultimo stato");
		addBuiltInText("loadServerEntries",                         "Load server entries", "Servereinträge laden", "Charger les entrées de serveur", "Cargar entradas de servidor", "Carica voci server");
		addBuiltInText("maxDuration",                               "Max. duration", "Max. Dauer", "Durée max.", "Duración máx.", "Durata max.");
		addBuiltInText("millis",                                    "milliseconds", "Millisekunden", "millisecondes", "milisegundos", "millisecondi");
		addBuiltInText("minDuration",                               "Min. duration", "Min. Dauer", "Durée min.", "Duración mín.", "Durata min.");
		addBuiltInText("minutes",                                   "minutes", "Minuten", "minutes", "minutos", "minuti");
		addBuiltInText("None",                                      "None", "Keine", "Aucun", "Ninguno", "Nessuno");
		addBuiltInText("notFound",                                  "Not found", "Nicht gefunden", "Introuvable", "No encontrado", "Non trovato");
		addBuiltInText("numberOfParallelWorkers",                   "Number of parallel workers", "Anzahl paralleler Worker", "Nombre de workers parallèles", "Número de workers paralelos", "Numero di worker paralleli");
		addBuiltInText("numberOfRepetitionsPerWorker",              "Repetitions per worker", "Wiederholungen pro Worker", "Répétitions par worker", "Repeticiones por worker", "Ripetizioni per worker");
		addBuiltInText("open",                                      "Open", "Öffnen", "Ouvrir", "Abrir", "Apri");
		addBuiltInText("openDataFile",                              "Open data file", "Datendatei öffnen", "Ouvrir un fichier de données", "Abrir archivo de datos", "Apri file di dati");
		addBuiltInText("openKeyFile",                               "Open key file", "Schlüsseldatei öffnen", "Ouvrir un fichier de clé", "Abrir archivo de clave", "Apri file della chiave");
		addBuiltInText("pathIsDirectory",                           "The path is a directory", "Der Pfad ist ein Verzeichnis", "Le chemin est un répertoire", "La ruta es un directorio", "Il percorso è una directory");
		addBuiltInText("port",                                      "Port", "Port", "Port", "Puerto", "Porta");
		addBuiltInText("privateKey",                                "Private key", "Privater Schlüssel", "Clé privée", "Clave privada", "Chiave privata");
		addBuiltInText("protocol",                                  "Protocol", "Protokoll", "Protocole", "Protocolo", "Protocollo");
		addBuiltInText("publicKey",                                 "Public key", "Öffentlicher Schlüssel", "Clé publique", "Clave pública", "Chiave pubblica");
		addBuiltInText("reallyDeleteFile",                          "Really delete file?\n{0}", "Datei wirklich löschen?\n{0}", "Voulez-vous vraiment supprimer le fichier ?\n{0}", "¿Eliminar realmente el archivo?\n{0}", "Eliminare davvero il file?\n{0}");
		addBuiltInText("reallyDeleteFolder",                        "Really delete folder?\n{0}", "Ordner wirklich löschen?\n{0}", "Voulez-vous vraiment supprimer le dossier ?\n{0}", "¿Eliminar realmente la carpeta?\n{0}", "Eliminare davvero la cartella?\n{0}");
		addBuiltInText("rememberUpdateCredentials",                 "Remember update credentials", "Zugangsdaten für Updates merken", "Mémoriser les identifiants de mise à jour", "Recordar las credenciales de actualización", "Memorizza le credenziali di aggiornamento");
		addBuiltInText("removeServerEntry",                         "Remove server entry", "Servereintrag entfernen", "Supprimer l'entrée de serveur", "Eliminar entrada de servidor", "Rimuovi voce server");
		addBuiltInText("runtimeProperties",                         "Runtime properties", "Laufzeit-Eigenschaften", "Propriétés d'exécution", "Propiedades de tiempo de ejecución", "Proprietà di runtime");
		addBuiltInText("save",                                      "Save", "Speichern", "Enregistrer", "Guardar", "Salva");
		addBuiltInText("saveData",                                  "Save data", "Daten speichern", "Enregistrer les données", "Guardar datos", "Salva dati");
		addBuiltInText("saveFile",                                  "Save file", "Datei speichern", "Enregistrer le fichier", "Guardar archivo", "Salva file");
		addBuiltInText("saveResults",                               "Save results", "Ergebnisse speichern", "Enregistrer les résultats", "Guardar resultados", "Salva risultati");
		addBuiltInText("saveServerEntry",                           "Save server entry", "Servereintrag speichern", "Enregistrer l'entrée de serveur", "Guardar entrada de servidor", "Salva voce server");
		addBuiltInText("seconds",                                   "seconds", "Sekunden", "secondes", "segundos", "secondi");
		addBuiltInText("serverEntries",                             "Server entries", "Servereinträge", "Entrées de serveur", "Entradas de servidor", "Voci server");
		addBuiltInText("sharename",                                 "Share name", "Freigabename", "Nom du partage", "Nombre del recurso compartido", "Nome condivisione");
		addBuiltInText("showRuntimeProperties",                     "Show runtime properties", "Laufzeit-Eigenschaften anzeigen", "Afficher les propriétés d'exécution", "Mostrar propiedades de tiempo de ejecución", "Mostra proprietà di runtime");
		addBuiltInText("showSwtProperties",                         "Show SWT properties", "SWT-Eigenschaften anzeigen", "Afficher les propriétés SWT", "Mostrar propiedades de SWT", "Mostra proprietà SWT");
		addBuiltInText("showSystemProperties",                      "Show system properties", "System-Eigenschaften anzeigen", "Afficher les propriétés système", "Mostrar propiedades del sistema", "Mostra proprietà di sistema");
		addBuiltInText("start",                                     "Start", "Start", "Démarrer", "Iniciar", "Avvia");
		addBuiltInText("success",                                   "Success", "Erfolg", "Succès", "Éxito", "Successo");
		addBuiltInText("successCount",                              "Success count", "Erfolgsanzahl", "Nombre de succès", "Número de éxitos", "Numero di successi");
		addBuiltInText("swtProperties",                             "SWT properties", "SWT-Eigenschaften", "Propriétés SWT", "Propiedades de SWT", "Proprietà SWT");
		addBuiltInText("systemProperties",                          "System properties", "System-Eigenschaften", "Propriétés système", "Propiedades del sistema", "Proprietà di sistema");
		addBuiltInText("Unknown",                                   "Unknown", "Unbekannt", "Inconnu", "Desconocido", "Sconosciuto");
		addBuiltInText("upload",                                    "Upload", "Hochladen", "Téléverser", "Subir", "Carica");
		addBuiltInText("uploadFile",                                "Upload file", "Datei hochladen", "Téléverser un fichier", "Subir archivo", "Carica file");
		addBuiltInText("uploadFolder",                              "Upload folder", "Ordner hochladen", "Téléverser un dossier", "Subir carpeta", "Carica cartella");
		addBuiltInText("useKeyFile",                                "Use key file", "Schlüsseldatei verwenden", "Utiliser un fichier de clé", "Usar archivo de clave", "Usa file della chiave");
		addBuiltInText("weeks",                                     "weeks", "Wochen", "semaines", "semanas", "settimane");
		addBuiltInText("window_title",                              "Secure data", "Sichere Daten", "Données sécurisées", "Datos seguros", "Dati protetti");
		addBuiltInText("workerSleepTime",                           "Worker sleep time", "Pause zwischen Durchläufen", "Pause entre les exécutions", "Pausa entre ejecuciones", "Pausa tra le esecuzioni");
		addBuiltInText("years",                                     "years", "Jahre", "ans", "años", "anni");

		// Labels of the library's own configuration keys (used as "conf." + key in ApplicationConfigurationDialog)
		addBuiltInText("conf.Application.Language",               "Language", "Sprache", "Langue", "Idioma", "Lingua");
		addBuiltInText("conf.Application.ProxyConfigurationType", "Proxy configuration", "Proxy-Konfiguration", "Configuration du proxy", "Configuración del proxy", "Configurazione proxy");
		addBuiltInText("conf.Application.ProxyUrl",               "Proxy URL", "Proxy-URL", "URL du proxy", "URL del proxy", "URL del proxy");
		addBuiltInText("conf.Application.DailyUpdateCheck",       "Daily update check", "Tägliche Update Prüfung", "Vérification quotidienne des mises à jour", "Comprobación diaria de actualizaciones", "Controllo giornaliero degli aggiornamenti");

		// Publish all texts of the static initializer at once
		builtInTexts = new BuiltInTexts();
	}

	/**
	 * Registers a built-in text of a library in all supported languages. Meant
	 * to be called once by a library, e.g. in a static initializer of the class
	 * using the text.
	 *
	 * <p>
	 * The application's own language files still take precedence. If the key is
	 * already built in, the existing text is kept, so a library cannot change
	 * the texts of this library or of other libraries registered before.
	 * </p>
	 *
	 * @param key message key, not null
	 * @param englishText English text, also used for all unsupported languages, not null
	 * @param germanText German text, null for the English text
	 * @param frenchText French text, null for the English text
	 * @param spanishText Spanish text, null for the English text
	 * @param italianText Italian text, null for the English text
	 * @return true if the text was registered, false if the key already existed
	 */
	public static synchronized boolean registerBuiltInText(final String key, final String englishText, final String germanText, final String frenchText, final String spanishText, final String italianText) {
		if (key == null || englishText == null) {
			throw new IllegalArgumentException("Key and English text must not be null");
		} else if (BUILT_IN_ENGLISH.containsKey(key)) {
			return false;
		} else {
			addBuiltInText(key, englishText, germanText, frenchText, spanishText, italianText);
			builtInTexts = new BuiltInTexts();
			return true;
		}
	}

	/**
	 * Adds a text to the modifiable maps. Callers must hold the class lock (or
	 * run in the static initializer) and publish a new snapshot afterwards.
	 */
	private static void addBuiltInText(final String key, final String englishText, final String germanText, final String frenchText, final String spanishText, final String italianText) {
		BUILT_IN_ENGLISH.put(key, englishText);
		BUILT_IN_GERMAN.put(key, germanText == null ? englishText : germanText);
		BUILT_IN_FRENCH.put(key, frenchText == null ? englishText : frenchText);
		BUILT_IN_SPANISH.put(key, spanishText == null ? englishText : spanishText);
		BUILT_IN_ITALIAN.put(key, italianText == null ? englishText : italianText);
	}

	/**
	 * Uses the application's root language file "LanguageProperties.properties"
	 * (without language suffix) regardless of the JVM's default locale. The
	 * built-in texts are English in this case.
	 */
	public static synchronized void enforceDefaultLocale() {
		languageTexts = loadLanguageTexts(Locale.ROOT);
	}

	/**
	 * Text for a message key in the current language, with the placeholders
	 * "{0}", "{1}", ... replaced by the arguments. The application's own
	 * language files take precedence over the built-in texts of this library.
	 * Placeholders contained in the arguments themselves are not replaced.
	 *
	 * @param resourceKey message key
	 * @param arguments values for the placeholders (null values are replaced by an empty string)
	 * @return the text, or a "Missing resourceKey" message if the key is
	 *         neither defined by the application nor built in
	 */
	public static String get(final String resourceKey, final Object... arguments) {
		final String pattern = getPattern(resourceKey);
		if (pattern != null) {
			return format(pattern, arguments);
		} else {
			return "Missing resourceKey: " + resourceKey + (arguments != null && arguments.length > 0 ? " " + Arrays.stream(arguments).map(String::valueOf).collect(Collectors.joining(", ")) : "");
		}
	}

	/**
	 * Whether a text exists for the message key, either in the application's
	 * language files or built into this library
	 *
	 * @param resourceKey message key, may be null
	 * @return true if a text exists, false for null
	 */
	public static boolean existsKey(final String resourceKey) {
		return getPattern(resourceKey) != null;
	}

	/**
	 * Replaces the placeholders "{0}", "{1}", ... literally by the arguments (no MessageFormat quoting)
	 */
	static String format(final String pattern, final Object[] arguments) {
		if (arguments == null || arguments.length == 0) {
			return pattern;
		} else {
			// Single pass, so placeholders contained in the arguments themselves are not replaced
			final Matcher matcher = PLACEHOLDER_PATTERN.matcher(pattern);
			final StringBuilder result = new StringBuilder();
			while (matcher.find()) {
				final int index;
				try {
					index = Integer.parseInt(matcher.group(1));
				} catch (@SuppressWarnings("unused") final NumberFormatException e) {
					continue;
				}
				if (index < arguments.length) {
					final Object item = arguments[index];
					matcher.appendReplacement(result, Matcher.quoteReplacement(item == null ? "" : item.toString()));
				}
			}
			matcher.appendTail(result);
			return result.toString();
		}
	}

	/**
	 * All built-in message keys (of this library and registered by other
	 * libraries), in definition order
	 *
	 * @return unmodifiable set of keys
	 */
	public static Set<String> getBuiltInKeys() {
		return builtInTexts.english.keySet();
	}

	/**
	 * All built-in texts of a language (of this library and registered by
	 * other libraries), without the application's own texts.
	 * German, French, Spanish or Italian for locales of those languages
	 * (including regional variants like "de_CH"), English for all other
	 * locales.
	 *
	 * @param locale locale of the language, may be null for English
	 * @return unmodifiable map of keys and texts
	 */
	public static Map<String, String> getBuiltInTexts(final Locale locale) {
		final BuiltInTexts texts = builtInTexts;
		final String language = locale == null ? "" : locale.getLanguage().toLowerCase(Locale.ROOT);
		switch (language) {
			case "de":
				return texts.german;
			case "fr":
				return texts.french;
			case "es":
				return texts.spanish;
			case "it":
				return texts.italian;
			default:
				return texts.english;
		}
	}

	/**
	 * Pattern for a message key: the application's text if defined, otherwise
	 * the built-in text, otherwise null
	 */
	private static String getPattern(final String resourceKey) {
		if (resourceKey == null) {
			// ResourceBundle.containsKey() does not accept null
			return null;
		}
		final LanguageTexts texts = getLanguageTexts();
		if (texts.applicationBundle != null && texts.applicationBundle.containsKey(resourceKey)) {
			return texts.applicationBundle.getString(resourceKey);
		} else {
			return getBuiltInTexts(texts.locale).get(resourceKey);
		}
	}

	private static LanguageTexts getLanguageTexts() {
		LanguageTexts texts = languageTexts;
		if (texts == null) {
			synchronized (LangResources.class) {
				texts = languageTexts;
				if (texts == null) {
					texts = loadLanguageTexts(Locale.getDefault());
					languageTexts = texts;
				}
			}
		}
		return texts;
	}

	/**
	 * Loads the application's language files for the locale, if there are any,
	 * and picks the built-in texts of the same language.
	 */
	private static LanguageTexts loadLanguageTexts(final Locale locale) {
		ResourceBundle applicationBundle = null;
		try {
			applicationBundle = ResourceBundle.getBundle(BUNDLE_NAME, locale);
		} catch (@SuppressWarnings("unused") final MissingResourceException e) {
			// Application without language files of its own: only the built-in texts are used
		} catch (final RuntimeException e) {
			// e.g. a malformed language file: report it, but keep the built-in texts working
			e.printStackTrace();
		}
		return new LanguageTexts(applicationBundle, locale);
	}

	/**
	 * Detects the languages by listing the existing language files in the
	 * classpath (directories as well as jar files), so any code like "de_BY" is
	 * found, even if the JRE does not know it as a locale.
	 *
	 * Also works with Eclipse's jar-in-jar loader ("rsrc:" URLs), whose
	 * locations cannot be listed directly. In that case the entries of
	 * "java.class.path" (the outer jar file) are listed instead.
	 *
	 * @param defaultLanguageCode
	 *            language of the root file without language suffix
	 *            ("LanguageProperties.properties"), added to the result if that
	 *            file exists. May be null.
	 * @return sorted list of language codes
	 */
	public static synchronized List<String> getAvailableLanguages(final String defaultLanguageCode) {
		final ClassLoader classLoader = LangResources.class.getClassLoader();

		if (availableLanguages == null) {
			// TreeSet keeps the result sorted and free of duplicates
			final Set<String> languages = new TreeSet<>();
			boolean unlistableLocationFound = false;
			for (final String location : getLanguageFileLocations(classLoader, defaultLanguageCode)) {
				if (isListableLocation(location)) {
					addLanguageCodesOfLocation(languages, location);
				} else {
					// e.g. "rsrc:" URLs of Eclipse's jar-in-jar loader, which cannot be listed directly
					unlistableLocationFound = true;
				}
			}

			if (unlistableLocationFound) {
				// The classpath entries (e.g. the outer jar file of a jar-in-jar application) contain those files
				for (final String location : getClassPathLocations()) {
					addLanguageCodesOfLocation(languages, location);
				}
			}

			availableLanguages = new ArrayList<>(languages);
		}

		final Set<String> result = new TreeSet<>(availableLanguages);
		if (defaultLanguageCode != null && !defaultLanguageCode.isBlank() && classLoader.getResource(BUNDLE_NAME + ".properties") != null) {
			result.add(defaultLanguageCode);
		}
		return new ArrayList<>(result);
	}

	/**
	 * Only "file" and "jar" locations can be listed. Checked by the URI scheme,
	 * so no URL stream handler is needed for this check.
	 */
	private static boolean isListableLocation(final String location) {
		try {
			final String scheme = URI.create(location).getScheme();
			return "file".equalsIgnoreCase(scheme) || "jar".equalsIgnoreCase(scheme);
		} catch (@SuppressWarnings("unused") final IllegalArgumentException e) {
			return false;
		}
	}

	private static void addLanguageCodesOfLocation(final Set<String> languages, final String location) {
		try {
			languages.addAll(listLanguageCodes(URI.create(location).toURL()));
		} catch (final Exception e) {
			// Location cannot be listed, the other locations may still contain language files
			e.printStackTrace();
		}
	}

	/**
	 * Finds the classpath locations (directories or jar files) containing
	 * language files, by looking up language files that are known to exist.
	 * Falls back to all entries of the classpath, if none of those exist.
	 */
	private static Set<String> getLanguageFileLocations(final ClassLoader classLoader, final String defaultLanguageCode) {
		final List<String> knownFileNames = new ArrayList<>();
		knownFileNames.add(BUNDLE_NAME + ".properties");
		if (defaultLanguageCode != null && !defaultLanguageCode.isBlank()) {
			knownFileNames.add(BUNDLE_NAME + "_" + defaultLanguageCode + ".properties");
		}

		// LinkedHashSet, because the same location is found once per known file
		final Set<String> locations = new LinkedHashSet<>();
		for (final String knownFileName : knownFileNames) {
			try {
				for (final URL url : Collections.list(classLoader.getResources(knownFileName))) {
					locations.add(url.toExternalForm());
				}
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}

		if (locations.isEmpty()) {
			// No known file: check every classpath entry
			locations.addAll(getClassPathLocations());
		}

		return locations;
	}

	/**
	 * Locations of all entries of the system property "java.class.path"
	 */
	private static Set<String> getClassPathLocations() {
		final Set<String> locations = new LinkedHashSet<>();
		for (final String classPathEntry : System.getProperty("java.class.path", "").split(File.pathSeparator)) {
			if (!classPathEntry.isBlank()) {
				try {
					final File file = new File(classPathEntry);
					if (file.isDirectory()) {
						// Dummy file name, only its directory is listed
						locations.add(new File(file, BUNDLE_NAME + ".properties").toURI().toURL().toExternalForm());
					} else if (file.isFile()) {
						// Root of the jar file, which (unlike a missing entry) can always be opened
						locations.add("jar:" + file.toURI().toURL().toExternalForm() + "!/");
					}
				} catch (final IOException e) {
					e.printStackTrace();
				}
			}
		}
		return locations;
	}

	/**
	 * Lists the language codes of all language files next to the given file URL
	 * (a file in a directory or an entry in a jar file).
	 */
	private static List<String> listLanguageCodes(final URL fileUrl) throws IOException, URISyntaxException {
		final List<String> languageCodes = new ArrayList<>();
		if ("file".equalsIgnoreCase(fileUrl.getProtocol())) {
			final Path directory = Paths.get(fileUrl.toURI()).getParent();
			if (directory != null && Files.isDirectory(directory)) {
				try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(directory, BUNDLE_NAME + "_*.properties")) {
					for (final Path path : directoryStream) {
						addLanguageCode(languageCodes, path.getFileName().toString());
					}
				}
			}
		} else if ("jar".equalsIgnoreCase(fileUrl.getProtocol())) {
			final URLConnection connection = fileUrl.openConnection();
			if (connection instanceof JarURLConnection) {
				// No caching, so the jar file may be closed here without affecting other users of that jar
				connection.setUseCaches(false);
				try (JarFile jarFile = ((JarURLConnection) connection).getJarFile()) {
					final Enumeration<JarEntry> entries = jarFile.entries();
					while (entries.hasMoreElements()) {
						final JarEntry entry = entries.nextElement();
						// Only files in the jar's root directory
						if (!entry.isDirectory() && !entry.getName().contains("/")) {
							addLanguageCode(languageCodes, entry.getName());
						}
					}
				}
			}
		}
		return languageCodes;
	}

	private static void addLanguageCode(final List<String> languageCodes, final String fileName) {
		final Matcher matcher = LANGUAGE_FILE_NAME_PATTERN.matcher(fileName);
		if (matcher.matches()) {
			languageCodes.add(matcher.group(1));
		}
	}
}