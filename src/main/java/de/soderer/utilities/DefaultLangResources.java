package de.soderer.utilities;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Default German, English, French, Spanish and Italian translations for all message keys used within soderer-utilities.
 *
 * Placeholders use the "{0}", "{1}", ... syntax of {@link LangResources}, which replaces them literally.
 * There is no MessageFormat quoting, so an apostrophe is written as a plain "'".
 */
public class DefaultLangResources {
	private static final Map<String, String> GERMAN = new LinkedHashMap<>();
	private static final Map<String, String> ENGLISH = new LinkedHashMap<>();
	private static final Map<String, String> FRENCH = new LinkedHashMap<>();
	private static final Map<String, String> SPANISH = new LinkedHashMap<>();
	private static final Map<String, String> ITALIAN = new LinkedHashMap<>();

	static {
		// Parameter order of add(): key, English, German, French, Spanish, Italian
		// "yN"/"Yn" contain the localized answer letters (upper case marks the default). Console input has to accept these letters (and should also accept "y")
		add("cancel",                      "Cancel", "Abbrechen", "Annuler", "Cancelar", "Annulla");
		add("canceledByUser",              "Canceled by user", "Abbruch durch Benutzer", "Annulé par l'utilisateur", "Cancelado por el usuario", "Annullato dall'utente");
		add("cannotOpenMail",              "Email client was not found.", "Email client konnte nicht gefunden werden.", "Le client de messagerie est introuvable.", "No se encontró el cliente de correo.", "Il client di posta non è stato trovato.");
		add("close",                       "Close", "Schließen", "Fermer", "Cerrar", "Chiudi");
		add("copyToClipboard",             "Copy to clipboard", "In Zwischenablage kopieren", "Copier dans le presse-papiers", "Copiar al portapapeles", "Copia negli appunti");
		add("dailyUpdateCheck",            "Daily update check", "Tägliche Update Prüfung", "Vérification quotidienne des mises à jour", "Comprobación diaria de actualizaciones", "Controllo giornaliero degli aggiornamenti");
		add("detailsLess",                 "Details <<<", "Details <<<", "Détails <<<", "Detalles <<<", "Dettagli <<<");
		add("detailsMore",                 "Details >>>", "Details >>>", "Détails >>>", "Detalles >>>", "Dettagli >>>");
		add("enterDownloadCredentials",    "Enter credentials for download", "Bitte Userdaten für den Download eingeben", "Saisissez les identifiants pour le téléchargement", "Introduzca las credenciales para la descarga", "Inserire le credenziali per il download");
		add("enterDownloadPassword",       "Enter password for download", "Bitte Passwort für Download eingeben", "Saisissez le mot de passe pour le téléchargement", "Introduzca la contraseña para la descarga", "Inserire la password per il download");
		add("enterDownloadUsername",       "Enter username for download", "Bitte Usernamen für Download eingeben", "Saisissez le nom d'utilisateur pour le téléchargement", "Introduzca el nombre de usuario para la descarga", "Inserire il nome utente per il download");
		add("enterPassword",               "Please enter password", "Bitte Passwort eingeben", "Veuillez saisir le mot de passe", "Introduzca la contraseña", "Inserire la password");
		add("enterUsername",               "Please enter username", "Bitte Usernamen eingeben", "Veuillez saisir le nom d'utilisateur", "Introduzca el nombre de usuario", "Inserire il nome utente");
		add("error",                       "Error", "Fehler", "Erreur", "Error", "Errore");
		add("errorOccurred",               "An error occurred", "Es ist ein Fehler aufgetreten", "Une erreur s'est produite", "Se ha producido un error", "Si è verificato un errore");
		add("installUpdate",               "Install update?", "Update installieren?", "Installer la mise à jour ?", "¿Instalar la actualización?", "Installare l'aggiornamento?");
		add("yN",                          "y/N", "j/N", "o/N", "s/N", "s/N");
		add("Yn",                          "Y/n", "J/n", "O/n", "S/n", "S/n");
		add("newApplicationVersion",       "New version {0} is available for {1}.\nThe current local version is {2}.", "Es ist eine neue Version {0} verfügbar für {1}.\nDie aktuelle lokale Version ist {2}.", "Une nouvelle version {0} est disponible pour {1}.\nLa version locale actuelle est {2}.", "Hay una nueva versión {0} disponible para {1}.\nLa versión local actual es {2}.", "È disponibile una nuova versione {0} per {1}.\nLa versione locale attuale è {2}.");
		add("no",                          "No", "Nein", "Non", "No", "No");
		add("noNewerVersion",              "There is no newer version available for {0}.\nThe current local version is {1}.", "Es ist keine neuere Version verfügbar für {0}.\nDie aktuelle lokale Version ist {1}.", "Aucune version plus récente n'est disponible pour {0}.\nLa version locale actuelle est {1}.", "No hay ninguna versión más reciente disponible para {0}.\nLa versión local actual es {1}.", "Non è disponibile una versione più recente per {0}.\nLa versione locale attuale è {1}.");
		add("noPassword",                  "No password", "Kein Passwort", "Pas de mot de passe", "Sin contraseña", "Nessuna password");
		add("ok",                          "OK", "OK", "OK", "Aceptar", "OK");
		add("openMail",                    "Open mail with errordata to developer", "Mail mit Fehlerdaten an den Entwickler öffnen", "Ouvrir un e-mail au développeur avec les données d'erreur", "Abrir un correo al desarrollador con los datos del error", "Apri un'e-mail allo sviluppatore con i dati dell'errore");
		add("password",                    "Password", "Passwort", "Mot de passe", "Contraseña", "Password");
		add("passwordRequired",            "Error:\nPassword is required", "Fehler:\nPasswort ist notwendig", "Erreur :\nLe mot de passe est obligatoire", "Error:\nLa contraseña es obligatoria", "Errore:\nLa password è obbligatoria");
		add("restartNow",                  "Restart application now", "Jetzt Anwendung neustarten", "Redémarrer l'application maintenant", "Reiniciar la aplicación ahora", "Riavvia l'applicazione ora");
		add("showSwingProperties",         "Show Swing properties", "Swing-Eigenschaften anzeigen", "Afficher les propriétés Swing", "Mostrar propiedades de Swing", "Mostra proprietà Swing");
		add("sureQuestion",                "Are you sure?", "Sind sie sich sicher?", "Êtes-vous sûr ?", "¿Está seguro?", "Sei sicuro?");
		add("swingProperties",             "Swing properties", "Swing-Eigenschaften", "Propriétés Swing", "Propiedades de Swing", "Proprietà Swing");
		add("updateDone",                  "Updated application may be restarted", "Upgedatete Anwendung kann neugestartet werden", "L'application mise à jour peut être redémarrée", "La aplicación actualizada puede reiniciarse", "L'applicazione aggiornata può essere riavviata");
		add("updateErrorVersionCheck",     "Update error while checking for new version:\n{0}", "Fehler beim Ermitteln der aktuellsten Version für ein Update der Applikation:\n{0}", "Erreur lors de la recherche d'une nouvelle version pour la mise à jour :\n{0}", "Error al buscar una nueva versión para la actualización:\n{0}", "Errore durante la ricerca di una nuova versione per l'aggiornamento:\n{0}");
		add("username",                    "Username", "Username", "Nom d'utilisateur", "Nombre de usuario", "Nome utente");
		add("usernameAndPasswordRequired", "Error:\nUsername and password are required", "Fehler:\nUsername und Passwort sind notwendig", "Erreur :\nLe nom d'utilisateur et le mot de passe sont obligatoires", "Error:\nEl nombre de usuario y la contraseña son obligatorios", "Errore:\nNome utente e password sono obbligatori");
		add("usernameRequired",            "Error:\nUsername is required", "Fehler:\nUsername ist notwendig", "Erreur :\nLe nom d'utilisateur est obligatoire", "Error:\nEl nombre de usuario es obligatorio", "Errore:\nIl nome utente è obbligatorio");
		add("yes",                         "Yes", "Ja", "Oui", "Sí", "Sì");

		// Translations for keys without a built-in fallback in the library
		add("actionSuccessfullyCompleted",               "Action successfully completed", "Aktion erfolgreich abgeschlossen", "Action terminée avec succès", "Acción completada correctamente", "Azione completata con successo");
		add("base64Format",                              "Base64 format", "Base64-Format", "Format Base64", "Formato Base64", "Formato Base64");
		add("browse",                                    "Browse", "Durchsuchen", "Parcourir", "Examinar", "Sfoglia");
		add("buildTime",                                 "Build time", "Build-Zeit", "Date de build", "Fecha de compilación", "Data di build");
		add("cannotLoadKeystore",                        "Cannot load keystore", "Keystore kann nicht geladen werden", "Impossible de charger le keystore", "No se puede cargar el almacén de claves", "Impossibile caricare il keystore");
		add("cannotReadPrivateKey",                      "Cannot read private key", "Privater Schlüssel kann nicht gelesen werden", "Impossible de lire la clé privée", "No se puede leer la clave privada", "Impossibile leggere la chiave privata");
		add("cannotSaveKeystore",                        "Cannot save keystore", "Keystore kann nicht gespeichert werden", "Impossible d'enregistrer le keystore", "No se puede guardar el almacén de claves", "Impossibile salvare il keystore");
		add("checkHostKey",                              "Check host key", "Host-Key prüfen", "Vérifier la clé d'hôte", "Comprobar la clave del host", "Verifica la chiave host");
		add("checkUpdate",                               "Check for update", "Nach Update suchen", "Rechercher une mise à jour", "Buscar actualizaciones", "Cerca aggiornamenti");
		add("configuration",                             "Configuration", "Konfiguration", "Configuration", "Configuración", "Configurazione");
		add("confirmNewHostKey",                         "Unknown host key for {0}:\n{1}\n\nAccept this host key?", "Unbekannter Host-Key für {0}:\n{1}\n\nSoll dieser Host-Key akzeptiert werden?", "Clé d'hôte inconnue pour {0} :\n{1}\n\nAccepter cette clé d'hôte ?", "Clave de host desconocida para {0}:\n{1}\n\n¿Aceptar esta clave de host?", "Chiave host sconosciuta per {0}:\n{1}\n\nAccettare questa chiave host?");
		add("connect",                                   "Connect", "Verbinden", "Connecter", "Conectar", "Connetti");
		add("containsHeadersInFirstRow",                 "First row contains column headers", "Erste Zeile enthält Spaltennamen", "La première ligne contient les noms de colonnes", "La primera fila contiene los nombres de columna", "La prima riga contiene i nomi delle colonne");
		add("createDataFile",                            "Create data file", "Datendatei erstellen", "Créer un fichier de données", "Crear archivo de datos", "Crea file di dati");
		add("createDesktopLink",                         "Create desktop link", "Desktop-Verknüpfung erstellen", "Créer un raccourci sur le bureau", "Crear acceso directo en el escritorio", "Crea collegamento sul desktop");
		add("createFolder",                              "Create folder", "Ordner erstellen", "Créer un dossier", "Crear carpeta", "Crea cartella");
		add("csvDoubleQuote",                            "Double quote (\")", "Doppeltes Anführungszeichen (\")", "Guillemet double (\")", "Comillas dobles (\")", "Virgolette doppie (\")");
		add("csvSeparator",                              "Separator", "Trennzeichen", "Séparateur", "Separador", "Separatore");
		add("csvSeparatorComma",                         "Comma (,)", "Komma (,)", "Virgule (,)", "Coma (,)", "Virgola (,)");
		add("csvSeparatorSemicolon",                     "Semicolon (;)", "Semikolon (;)", "Point-virgule (;)", "Punto y coma (;)", "Punto e virgola (;)");
		add("csvSingleQuote",                            "Single quote (')", "Einfaches Anführungszeichen (')", "Guillemet simple (')", "Comilla simple (')", "Virgoletta singola (')");
		add("csvUnderfilledLines",                       "Allow underfilled lines", "Unvollständige Zeilen erlauben", "Autoriser les lignes incomplètes", "Permitir líneas incompletas", "Consenti righe incomplete");
		add("csvUseQuote",                               "Use quote character", "Textbegrenzer verwenden", "Utiliser un délimiteur de texte", "Usar delimitador de texto", "Usa delimitatore di testo");
		add("currentPath",                               "Current path", "Aktueller Pfad", "Chemin actuel", "Ruta actual", "Percorso attuale");
		add("days",                                      "days", "Tage", "jours", "días", "giorni");
		add("dbName",                                    "Database name", "Datenbankname", "Nom de la base de données", "Nombre de la base de datos", "Nome del database");
		add("dbType",                                    "Database type", "Datenbanktyp", "Type de base de données", "Tipo de base de datos", "Tipo di database");
		add("deleteFile",                                "Delete file", "Datei löschen", "Supprimer le fichier", "Eliminar archivo", "Elimina file");
		add("deleteFolder",                              "Delete folder", "Ordner löschen", "Supprimer le dossier", "Eliminar carpeta", "Elimina cartella");
		add("desktopLinkWasCreatedAt",                   "Desktop link was created at:\n{0}", "Desktop-Verknüpfung wurde erstellt:\n{0}", "Le raccourci sur le bureau a été créé :\n{0}", "Se ha creado el acceso directo en el escritorio:\n{0}", "Il collegamento sul desktop è stato creato:\n{0}");
		add("discardUnsavedConfigurationChanges",        "Discard unsaved configuration changes?", "Ungespeicherte Konfigurationsänderungen verwerfen?", "Abandonner les modifications de configuration non enregistrées ?", "¿Descartar los cambios de configuración no guardados?", "Scartare le modifiche alla configurazione non salvate?");
		add("disconnect",                                "Disconnect", "Trennen", "Déconnecter", "Desconectar", "Disconnetti");
		add("download",                                  "Download", "Herunterladen", "Télécharger", "Descargar", "Scarica");
		add("duration",                                  "Duration", "Dauer", "Durée", "Duración", "Durata");
		add("ellipticCurveName",                         "Elliptic curve name", "Name der elliptischen Kurve", "Nom de la courbe elliptique", "Nombre de la curva elíptica", "Nome della curva ellittica");
		add("encoding",                                  "Encoding", "Zeichenkodierung", "Encodage", "Codificación", "Codifica");
		add("enterFolderName",                           "Please enter folder name", "Bitte Ordnernamen eingeben", "Veuillez saisir le nom du dossier", "Introduzca el nombre de la carpeta", "Inserire il nome della cartella");
		add("enterKeystorePassword",                     "Please enter keystore password", "Bitte Keystore-Passwort eingeben", "Veuillez saisir le mot de passe du keystore", "Introduzca la contraseña del almacén de claves", "Inserire la password del keystore");
		add("error.userNotAuthenticatedOrNotAuthorized", "User is not authenticated or not authorized", "Benutzer ist nicht angemeldet oder nicht berechtigt", "L'utilisateur n'est pas authentifié ou pas autorisé", "El usuario no está autenticado o no está autorizado", "L'utente non è autenticato o non è autorizzato");
		add("errorCount",                                "Error count", "Fehleranzahl", "Nombre d'erreurs", "Número de errores", "Numero di errori");
		add("errorMessage",                              "Error:\n{0}", "Fehler:\n{0}", "Erreur :\n{0}", "Error:\n{0}", "Errore:\n{0}");
		add("fileWasNotSaved",                           "File was not saved", "Datei wurde nicht gespeichert", "Le fichier n'a pas été enregistré", "El archivo no se ha guardado", "Il file non è stato salvato");
		add("fingerprintMd5",                            "Fingerprint MD5", "Fingerprint MD5", "Empreinte MD5", "Huella digital MD5", "Impronta MD5");
		add("fingerprintSha1",                           "Fingerprint SHA-1", "Fingerprint SHA-1", "Empreinte SHA-1", "Huella digital SHA-1", "Impronta SHA-1");
		add("fingerprintSha256",                         "Fingerprint SHA-256", "Fingerprint SHA-256", "Empreinte SHA-256", "Huella digital SHA-256", "Impronta SHA-256");
		add("fingerprintSha384",                         "Fingerprint SHA-384", "Fingerprint SHA-384", "Empreinte SHA-384", "Huella digital SHA-384", "Impronta SHA-384");
		add("fingerprintSha512",                         "Fingerprint SHA-512", "Fingerprint SHA-512", "Empreinte SHA-512", "Huella digital SHA-512", "Impronta SHA-512");
		add("hexColonFormat",                            "Hex format with colons", "Hex-Format mit Doppelpunkten", "Format hexadécimal avec deux-points", "Formato hexadecimal con dos puntos", "Formato esadecimale con due punti");
		add("hexFormat",                                 "Hex format", "Hex-Format", "Format hexadécimal", "Formato hexadecimal", "Formato esadecimale");
		add("hostname",                                  "Hostname", "Hostname", "Nom d'hôte", "Nombre de host", "Nome host");
		add("hostnameOrIp",                              "Hostname or IP", "Hostname oder IP", "Nom d'hôte ou IP", "Nombre de host o IP", "Nome host o IP");
		add("hours",                                     "hours", "Stunden", "heures", "horas", "ore");
		add("keepPasswordInMemory",                      "Keep password in memory", "Passwort im Speicher behalten", "Conserver le mot de passe en mémoire", "Mantener la contraseña en memoria", "Mantieni la password in memoria");
		add("keyFormat",                                 "Key format", "Schlüsselformat", "Format de clé", "Formato de clave", "Formato della chiave");
		add("keyStrength",                               "Key strength", "Schlüsselstärke", "Force de la clé", "Longitud de clave", "Lunghezza della chiave");
		add("keystrengthExceedsAllowedJCE",              "Key strength exceeds the maximum allowed by JCE", "Die Schlüsselstärke übersteigt das von der JCE erlaubte Maximum", "La force de la clé dépasse le maximum autorisé par la JCE", "La longitud de clave supera el máximo permitido por la JCE", "La lunghezza della chiave supera il massimo consentito dalla JCE");
		add("keyType",                                   "Key type", "Schlüsseltyp", "Type de clé", "Tipo de clave", "Tipo di chiave");
		add("languageChangeRequiresRestart",             "Changing the language requires a restart of the application", "Die Änderung der Sprache wird erst nach einem Neustart der Anwendung wirksam", "Le changement de langue ne prend effet qu'après un redémarrage de l'application", "El cambio de idioma solo se aplicará tras reiniciar la aplicación", "Il cambio di lingua avrà effetto solo dopo il riavvio dell'applicazione");
		add("latestDuration",                            "Latest duration", "Letzte Dauer", "Dernière durée", "Última duración", "Ultima durata");
		add("latestStatus",                              "Latest status", "Letzter Status", "Dernier statut", "Último estado", "Ultimo stato");
		add("loadServerEntries",                         "Load server entries", "Servereinträge laden", "Charger les entrées de serveur", "Cargar entradas de servidor", "Carica voci server");
		add("maxDuration",                               "Max. duration", "Max. Dauer", "Durée max.", "Duración máx.", "Durata max.");
		add("millis",                                    "milliseconds", "Millisekunden", "millisecondes", "milisegundos", "millisecondi");
		add("minDuration",                               "Min. duration", "Min. Dauer", "Durée min.", "Duración mín.", "Durata min.");
		add("minutes",                                   "minutes", "Minuten", "minutes", "minutos", "minuti");
		add("None",                                      "None", "Keine", "Aucun", "Ninguno", "Nessuno");
		add("notFound",                                  "Not found", "Nicht gefunden", "Introuvable", "No encontrado", "Non trovato");
		add("numberOfParallelWorkers",                   "Number of parallel workers", "Anzahl paralleler Worker", "Nombre de workers parallèles", "Número de workers paralelos", "Numero di worker paralleli");
		add("numberOfRepetitionsPerWorker",              "Repetitions per worker", "Wiederholungen pro Worker", "Répétitions par worker", "Repeticiones por worker", "Ripetizioni per worker");
		add("open",                                      "Open", "Öffnen", "Ouvrir", "Abrir", "Apri");
		add("openDataFile",                              "Open data file", "Datendatei öffnen", "Ouvrir un fichier de données", "Abrir archivo de datos", "Apri file di dati");
		add("openKeyFile",                               "Open key file", "Schlüsseldatei öffnen", "Ouvrir un fichier de clé", "Abrir archivo de clave", "Apri file della chiave");
		add("pathIsDirectory",                           "The path is a directory", "Der Pfad ist ein Verzeichnis", "Le chemin est un répertoire", "La ruta es un directorio", "Il percorso è una directory");
		add("port",                                      "Port", "Port", "Port", "Puerto", "Porta");
		add("privateKey",                                "Private key", "Privater Schlüssel", "Clé privée", "Clave privada", "Chiave privata");
		add("protocol",                                  "Protocol", "Protokoll", "Protocole", "Protocolo", "Protocollo");
		add("publicKey",                                 "Public key", "Öffentlicher Schlüssel", "Clé publique", "Clave pública", "Chiave pubblica");
		add("reallyDeleteFile",                          "Really delete file?\n{0}", "Datei wirklich löschen?\n{0}", "Voulez-vous vraiment supprimer le fichier ?\n{0}", "¿Eliminar realmente el archivo?\n{0}", "Eliminare davvero il file?\n{0}");
		add("reallyDeleteFolder",                        "Really delete folder?\n{0}", "Ordner wirklich löschen?\n{0}", "Voulez-vous vraiment supprimer le dossier ?\n{0}", "¿Eliminar realmente la carpeta?\n{0}", "Eliminare davvero la cartella?\n{0}");
		add("rememberUpdateCredentials",                 "Remember update credentials", "Zugangsdaten für Updates merken", "Mémoriser les identifiants de mise à jour", "Recordar las credenciales de actualización", "Memorizza le credenziali di aggiornamento");
		add("removeServerEntry",                         "Remove server entry", "Servereintrag entfernen", "Supprimer l'entrée de serveur", "Eliminar entrada de servidor", "Rimuovi voce server");
		add("runtimeProperties",                         "Runtime properties", "Laufzeit-Eigenschaften", "Propriétés d'exécution", "Propiedades de tiempo de ejecución", "Proprietà di runtime");
		add("save",                                      "Save", "Speichern", "Enregistrer", "Guardar", "Salva");
		add("saveData",                                  "Save data", "Daten speichern", "Enregistrer les données", "Guardar datos", "Salva dati");
		add("saveFile",                                  "Save file", "Datei speichern", "Enregistrer le fichier", "Guardar archivo", "Salva file");
		add("saveResults",                               "Save results", "Ergebnisse speichern", "Enregistrer les résultats", "Guardar resultados", "Salva risultati");
		add("saveServerEntry",                           "Save server entry", "Servereintrag speichern", "Enregistrer l'entrée de serveur", "Guardar entrada de servidor", "Salva voce server");
		add("seconds",                                   "seconds", "Sekunden", "secondes", "segundos", "secondi");
		add("serverEntries",                             "Server entries", "Servereinträge", "Entrées de serveur", "Entradas de servidor", "Voci server");
		add("sharename",                                 "Share name", "Freigabename", "Nom du partage", "Nombre del recurso compartido", "Nome condivisione");
		add("showRuntimeProperties",                     "Show runtime properties", "Laufzeit-Eigenschaften anzeigen", "Afficher les propriétés d'exécution", "Mostrar propiedades de tiempo de ejecución", "Mostra proprietà di runtime");
		add("showSwtProperties",                         "Show SWT properties", "SWT-Eigenschaften anzeigen", "Afficher les propriétés SWT", "Mostrar propiedades de SWT", "Mostra proprietà SWT");
		add("showSystemProperties",                      "Show system properties", "System-Eigenschaften anzeigen", "Afficher les propriétés système", "Mostrar propiedades del sistema", "Mostra proprietà di sistema");
		add("start",                                     "Start", "Start", "Démarrer", "Iniciar", "Avvia");
		add("success",                                   "Success", "Erfolg", "Succès", "Éxito", "Successo");
		add("successCount",                              "Success count", "Erfolgsanzahl", "Nombre de succès", "Número de éxitos", "Numero di successi");
		add("swtProperties",                             "SWT properties", "SWT-Eigenschaften", "Propriétés SWT", "Propiedades de SWT", "Proprietà SWT");
		add("systemProperties",                          "System properties", "System-Eigenschaften", "Propriétés système", "Propiedades del sistema", "Proprietà di sistema");
		add("Unknown",                                   "Unknown", "Unbekannt", "Inconnu", "Desconocido", "Sconosciuto");
		add("upload",                                    "Upload", "Hochladen", "Téléverser", "Subir", "Carica");
		add("uploadFile",                                "Upload file", "Datei hochladen", "Téléverser un fichier", "Subir archivo", "Carica file");
		add("uploadFolder",                              "Upload folder", "Ordner hochladen", "Téléverser un dossier", "Subir carpeta", "Carica cartella");
		add("useKeyFile",                                "Use key file", "Schlüsseldatei verwenden", "Utiliser un fichier de clé", "Usar archivo de clave", "Usa file della chiave");
		add("weeks",                                     "weeks", "Wochen", "semaines", "semanas", "settimane");
		add("window_title",                              "Secure data", "Sichere Daten", "Données sécurisées", "Datos seguros", "Dati protetti");
		add("workerSleepTime",                           "Worker sleep time", "Pause zwischen Durchläufen", "Pause entre les exécutions", "Pausa entre ejecuciones", "Pausa tra le esecuzioni");
		add("years",                                     "years", "Jahre", "ans", "años", "anni");

		// Labels of the library's own configuration keys (used as "conf." + key in ApplicationConfigurationDialog)
		add("conf.Application.Language",               "Language", "Sprache", "Langue", "Idioma", "Lingua");
		add("conf.Application.ProxyConfigurationType", "Proxy configuration", "Proxy-Konfiguration", "Configuration du proxy", "Configuración del proxy", "Configurazione proxy");
		add("conf.Application.ProxyUrl",               "Proxy URL", "Proxy-URL", "URL du proxy", "URL del proxy", "URL del proxy");
		add("conf.Application.DailyUpdateCheck",       "Daily update check", "Tägliche Update Prüfung", "Vérification quotidienne des mises à jour", "Comprobación diaria de actualizaciones", "Controllo giornaliero degli aggiornamenti");
	}

	private static void add(final String key, final String englishText, final String germanText, final String frenchText, final String spanishText, final String italianText) {
		ENGLISH.put(key, englishText);
		GERMAN.put(key, germanText);
		FRENCH.put(key, frenchText);
		SPANISH.put(key, spanishText);
		ITALIAN.put(key, italianText);
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
	 * German, French, Spanish or Italian for locales of those languages, English for all other locales.
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
	 * German, French, Spanish or Italian for locales of those languages (including regional variants like "de_CH"), English for all other locales.
	 */
	public static Map<String, String> getProperties(final Locale locale) {
		final String language = locale == null ? "" : locale.getLanguage().toLowerCase();
		switch (language) {
			case "de":
				return Collections.unmodifiableMap(GERMAN);
			case "fr":
				return Collections.unmodifiableMap(FRENCH);
			case "es":
				return Collections.unmodifiableMap(SPANISH);
			case "it":
				return Collections.unmodifiableMap(ITALIAN);
			default:
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
