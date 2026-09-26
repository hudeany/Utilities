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
		// Parameter order of add(): key, German, English, French, Spanish, Italian
		// "yN"/"Yn" keep the English answer letters in French, Spanish and Italian, since they have to match the letters accepted by console input
		add("cancel",                      "Abbrechen", "Cancel", "Annuler", "Cancelar", "Annulla");
		add("canceledByUser",              "Abbruch durch Benutzer", "Canceled by user", "Annulé par l'utilisateur", "Cancelado por el usuario", "Annullato dall'utente");
		add("cannotOpenMail",              "Email client konnte nicht gefunden werden.", "Email client was not found.", "Le client de messagerie est introuvable.", "No se encontró el cliente de correo.", "Il client di posta non è stato trovato.");
		add("close",                       "Schließen", "Close", "Fermer", "Cerrar", "Chiudi");
		add("copyToClipboard",             "In Zwischenablage kopieren", "Copy to clipboard", "Copier dans le presse-papiers", "Copiar al portapapeles", "Copia negli appunti");
		add("dailyUpdateCheck",            "Tägliche Update Prüfung", "Daily update check", "Vérification quotidienne des mises à jour", "Comprobación diaria de actualizaciones", "Controllo giornaliero degli aggiornamenti");
		add("detailsLess",                 "Details <<<", "Details <<<", "Détails <<<", "Detalles <<<", "Dettagli <<<");
		add("detailsMore",                 "Details >>>", "Details >>>", "Détails >>>", "Detalles >>>", "Dettagli >>>");
		add("enterDownloadCredentials",    "Bitte Userdaten für den Download eingeben", "Enter credentials for download", "Saisissez les identifiants pour le téléchargement", "Introduzca las credenciales para la descarga", "Inserire le credenziali per il download");
		add("enterDownloadPassword",       "Bitte Passwort für Download eingeben", "Enter password for download", "Saisissez le mot de passe pour le téléchargement", "Introduzca la contraseña para la descarga", "Inserire la password per il download");
		add("enterDownloadUsername",       "Bitte Usernamen für Download eingeben", "Enter username for download", "Saisissez le nom d'utilisateur pour le téléchargement", "Introduzca el nombre de usuario para la descarga", "Inserire il nome utente per il download");
		add("enterPassword",               "Bitte Passwort eingeben", "Please enter password", "Veuillez saisir le mot de passe", "Introduzca la contraseña", "Inserire la password");
		add("enterUsername",               "Bitte Usernamen eingeben", "Please enter username", "Veuillez saisir le nom d'utilisateur", "Introduzca el nombre de usuario", "Inserire il nome utente");
		add("error",                       "Fehler", "Error", "Erreur", "Error", "Errore");
		add("errorOccurred",               "Es ist ein Fehler aufgetreten", "An error occurred", "Une erreur s'est produite", "Se ha producido un error", "Si è verificato un errore");
		add("installUpdate",               "Update installieren?", "Install update?", "Installer la mise à jour ?", "¿Instalar la actualización?", "Installare l'aggiornamento?");
		add("yN",                          "jN", "yN", "oN", "sN", "sN");
		add("Yn",                          "Jn", "Yn", "On", "Sn", "Sn");
		add("newApplicationVersion",       "Es ist eine neue Version {0} verfügbar für {1}.\nDie aktuelle lokale Version ist {2}.", "New version {0} is available for {1}.\nThe current local version is {2}.", "Une nouvelle version {0} est disponible pour {1}.\nLa version locale actuelle est {2}.", "Hay una nueva versión {0} disponible para {1}.\nLa versión local actual es {2}.", "È disponibile una nuova versione {0} per {1}.\nLa versione locale attuale è {2}.");
		add("no",                          "Nein", "No", "Non", "No", "No");
		add("noNewerVersion",              "Es ist keine neuere Version verfügbar für {0}.\nDie aktuelle lokale Version ist {1}.", "There is no newer version available for {0}.\nThe current local version is {1}.", "Aucune version plus récente n'est disponible pour {0}.\nLa version locale actuelle est {1}.", "No hay ninguna versión más reciente disponible para {0}.\nLa versión local actual es {1}.", "Non è disponibile una versione più recente per {0}.\nLa versione locale attuale è {1}.");
		add("noPassword",                  "Kein Passwort", "No password", "Pas de mot de passe", "Sin contraseña", "Nessuna password");
		add("ok",                          "OK", "OK", "OK", "Aceptar", "OK");
		add("openMail",                    "Mail mit Fehlerdaten an den Entwickler öffnen", "Open mail with errordata to developer", "Ouvrir un e-mail au développeur avec les données d'erreur", "Abrir un correo al desarrollador con los datos del error", "Apri un'e-mail allo sviluppatore con i dati dell'errore");
		add("password",                    "Passwort", "Password", "Mot de passe", "Contraseña", "Password");
		add("passwordRequired",            "Fehler:\nPasswort ist notwendig", "Error:\nPassword is required", "Erreur :\nLe mot de passe est obligatoire", "Error:\nLa contraseña es obligatoria", "Errore:\nLa password è obbligatoria");
		add("restartNow",                  "Jetzt Anwendung neustarten", "Restart application now", "Redémarrer l'application maintenant", "Reiniciar la aplicación ahora", "Riavvia l'applicazione ora");
		add("showSwingProperties",         "Swing-Eigenschaften anzeigen", "Show Swing properties", "Afficher les propriétés Swing", "Mostrar propiedades de Swing", "Mostra proprietà Swing");
		add("sureQuestion",                "Sind sie sich sicher?", "Are you sure?", "Êtes-vous sûr ?", "¿Está seguro?", "Sei sicuro?");
		add("swingProperties",             "Swing-Eigenschaften", "Swing properties", "Propriétés Swing", "Propiedades de Swing", "Proprietà Swing");
		add("updateDone",                  "Upgedatete Anwendung kann neugestartet werden", "Updated application may be restarted", "L'application mise à jour peut être redémarrée", "La aplicación actualizada puede reiniciarse", "L'applicazione aggiornata può essere riavviata");
		add("updateErrorVersionCheck",     "Fehler beim Ermitteln der aktuellsten Version für ein Update der Applikation:\n{0}", "Update error while checking for new version:\n{0}", "Erreur lors de la recherche d'une nouvelle version pour la mise à jour :\n{0}", "Error al buscar una nueva versión para la actualización:\n{0}", "Errore durante la ricerca di una nuova versione per l'aggiornamento:\n{0}");
		add("username",                    "Username", "Username", "Nom d'utilisateur", "Nombre de usuario", "Nome utente");
		add("usernameAndPasswordRequired", "Fehler:\nUsername und Passwort sind notwendig", "Error:\nUsername and password are required", "Erreur :\nLe nom d'utilisateur et le mot de passe sont obligatoires", "Error:\nEl nombre de usuario y la contraseña son obligatorios", "Errore:\nNome utente e password sono obbligatori");
		add("usernameRequired",            "Fehler:\nUsername ist notwendig", "Error:\nUsername is required", "Erreur :\nLe nom d'utilisateur est obligatoire", "Error:\nEl nombre de usuario es obligatorio", "Errore:\nIl nome utente è obbligatorio");
		add("yes",                         "Ja", "Yes", "Oui", "Sí", "Sì");

		// Translations for keys without a built-in fallback in the library
		add("actionSuccessfullyCompleted",               "Aktion erfolgreich abgeschlossen", "Action successfully completed", "Action terminée avec succès", "Acción completada correctamente", "Azione completata con successo");
		add("base64Format",                              "Base64-Format", "Base64 format", "Format Base64", "Formato Base64", "Formato Base64");
		add("browse",                                    "Durchsuchen", "Browse", "Parcourir", "Examinar", "Sfoglia");
		add("buildTime",                                 "Build-Zeit", "Build time", "Date de build", "Fecha de compilación", "Data di build");
		add("cannotLoadKeystore",                        "Keystore kann nicht geladen werden", "Cannot load keystore", "Impossible de charger le keystore", "No se puede cargar el almacén de claves", "Impossibile caricare il keystore");
		add("cannotReadPrivateKey",                      "Privater Schlüssel kann nicht gelesen werden", "Cannot read private key", "Impossible de lire la clé privée", "No se puede leer la clave privada", "Impossibile leggere la chiave privata");
		add("cannotSaveKeystore",                        "Keystore kann nicht gespeichert werden", "Cannot save keystore", "Impossible d'enregistrer le keystore", "No se puede guardar el almacén de claves", "Impossibile salvare il keystore");
		add("checkHostKey",                              "Host-Key prüfen", "Check host key", "Vérifier la clé d'hôte", "Comprobar la clave del host", "Verifica la chiave host");
		add("checkUpdate",                               "Nach Update suchen", "Check for update", "Rechercher une mise à jour", "Buscar actualizaciones", "Cerca aggiornamenti");
		add("configuration",                             "Konfiguration", "Configuration", "Configuration", "Configuración", "Configurazione");
		add("confirmNewHostKey",                         "Unbekannter Host-Key für {0}:\n{1}\n\nSoll dieser Host-Key akzeptiert werden?", "Unknown host key for {0}:\n{1}\n\nAccept this host key?", "Clé d'hôte inconnue pour {0} :\n{1}\n\nAccepter cette clé d'hôte ?", "Clave de host desconocida para {0}:\n{1}\n\n¿Aceptar esta clave de host?", "Chiave host sconosciuta per {0}:\n{1}\n\nAccettare questa chiave host?");
		add("connect",                                   "Verbinden", "Connect", "Connecter", "Conectar", "Connetti");
		add("containsHeadersInFirstRow",                 "Erste Zeile enthält Spaltennamen", "First row contains column headers", "La première ligne contient les noms de colonnes", "La primera fila contiene los nombres de columna", "La prima riga contiene i nomi delle colonne");
		add("createDataFile",                            "Datendatei erstellen", "Create data file", "Créer un fichier de données", "Crear archivo de datos", "Crea file di dati");
		add("createDesktopLink",                         "Desktop-Verknüpfung erstellen", "Create desktop link", "Créer un raccourci sur le bureau", "Crear acceso directo en el escritorio", "Crea collegamento sul desktop");
		add("createFolder",                              "Ordner erstellen", "Create folder", "Créer un dossier", "Crear carpeta", "Crea cartella");
		add("csvDoubleQuote",                            "Doppeltes Anführungszeichen (\")", "Double quote (\")", "Guillemet double (\")", "Comillas dobles (\")", "Virgolette doppie (\")");
		add("csvSeparator",                              "Trennzeichen", "Separator", "Séparateur", "Separador", "Separatore");
		add("csvSeparatorComma",                         "Komma (,)", "Comma (,)", "Virgule (,)", "Coma (,)", "Virgola (,)");
		add("csvSeparatorSemicolon",                     "Semikolon (;)", "Semicolon (;)", "Point-virgule (;)", "Punto y coma (;)", "Punto e virgola (;)");
		add("csvSingleQuote",                            "Einfaches Anführungszeichen (')", "Single quote (')", "Guillemet simple (')", "Comilla simple (')", "Virgoletta singola (')");
		add("csvUnderfilledLines",                       "Unvollständige Zeilen erlauben", "Allow underfilled lines", "Autoriser les lignes incomplètes", "Permitir líneas incompletas", "Consenti righe incomplete");
		add("csvUseQuote",                               "Textbegrenzer verwenden", "Use quote character", "Utiliser un délimiteur de texte", "Usar delimitador de texto", "Usa delimitatore di testo");
		add("currentPath",                               "Aktueller Pfad", "Current path", "Chemin actuel", "Ruta actual", "Percorso attuale");
		add("days",                                      "Tage", "days", "jours", "días", "giorni");
		add("dbName",                                    "Datenbankname", "Database name", "Nom de la base de données", "Nombre de la base de datos", "Nome del database");
		add("dbType",                                    "Datenbanktyp", "Database type", "Type de base de données", "Tipo de base de datos", "Tipo di database");
		add("deleteFile",                                "Datei löschen", "Delete file", "Supprimer le fichier", "Eliminar archivo", "Elimina file");
		add("deleteFolder",                              "Ordner löschen", "Delete folder", "Supprimer le dossier", "Eliminar carpeta", "Elimina cartella");
		add("desktopLinkWasCreatedAt",                   "Desktop-Verknüpfung wurde erstellt:\n{0}", "Desktop link was created at:\n{0}", "Le raccourci sur le bureau a été créé :\n{0}", "Se ha creado el acceso directo en el escritorio:\n{0}", "Il collegamento sul desktop è stato creato:\n{0}");
		add("discardUnsavedConfigurationChanges",        "Ungespeicherte Konfigurationsänderungen verwerfen?", "Discard unsaved configuration changes?", "Abandonner les modifications de configuration non enregistrées ?", "¿Descartar los cambios de configuración no guardados?", "Scartare le modifiche alla configurazione non salvate?");
		add("disconnect",                                "Trennen", "Disconnect", "Déconnecter", "Desconectar", "Disconnetti");
		add("download",                                  "Herunterladen", "Download", "Télécharger", "Descargar", "Scarica");
		add("duration",                                  "Dauer", "Duration", "Durée", "Duración", "Durata");
		add("ellipticCurveName",                         "Name der elliptischen Kurve", "Elliptic curve name", "Nom de la courbe elliptique", "Nombre de la curva elíptica", "Nome della curva ellittica");
		add("encoding",                                  "Zeichenkodierung", "Encoding", "Encodage", "Codificación", "Codifica");
		add("enterFolderName",                           "Bitte Ordnernamen eingeben", "Please enter folder name", "Veuillez saisir le nom du dossier", "Introduzca el nombre de la carpeta", "Inserire il nome della cartella");
		add("enterKeystorePassword",                     "Bitte Keystore-Passwort eingeben", "Please enter keystore password", "Veuillez saisir le mot de passe du keystore", "Introduzca la contraseña del almacén de claves", "Inserire la password del keystore");
		add("error.userNotAuthenticatedOrNotAuthorized", "Benutzer ist nicht angemeldet oder nicht berechtigt", "User is not authenticated or not authorized", "L'utilisateur n'est pas authentifié ou pas autorisé", "El usuario no está autenticado o no está autorizado", "L'utente non è autenticato o non è autorizzato");
		add("errorCount",                                "Fehleranzahl", "Error count", "Nombre d'erreurs", "Número de errores", "Numero di errori");
		add("errorMessage",                              "Fehler:\n{0}", "Error:\n{0}", "Erreur :\n{0}", "Error:\n{0}", "Errore:\n{0}");
		add("fileWasNotSaved",                           "Datei wurde nicht gespeichert", "File was not saved", "Le fichier n'a pas été enregistré", "El archivo no se ha guardado", "Il file non è stato salvato");
		add("fingerprintMd5",                            "Fingerprint MD5", "Fingerprint MD5", "Empreinte MD5", "Huella digital MD5", "Impronta MD5");
		add("fingerprintSha1",                           "Fingerprint SHA-1", "Fingerprint SHA-1", "Empreinte SHA-1", "Huella digital SHA-1", "Impronta SHA-1");
		add("fingerprintSha256",                         "Fingerprint SHA-256", "Fingerprint SHA-256", "Empreinte SHA-256", "Huella digital SHA-256", "Impronta SHA-256");
		add("fingerprintSha384",                         "Fingerprint SHA-384", "Fingerprint SHA-384", "Empreinte SHA-384", "Huella digital SHA-384", "Impronta SHA-384");
		add("fingerprintSha512",                         "Fingerprint SHA-512", "Fingerprint SHA-512", "Empreinte SHA-512", "Huella digital SHA-512", "Impronta SHA-512");
		add("hexColonFormat",                            "Hex-Format mit Doppelpunkten", "Hex format with colons", "Format hexadécimal avec deux-points", "Formato hexadecimal con dos puntos", "Formato esadecimale con due punti");
		add("hexFormat",                                 "Hex-Format", "Hex format", "Format hexadécimal", "Formato hexadecimal", "Formato esadecimale");
		add("hostname",                                  "Hostname", "Hostname", "Nom d'hôte", "Nombre de host", "Nome host");
		add("hostnameOrIp",                              "Hostname oder IP", "Hostname or IP", "Nom d'hôte ou IP", "Nombre de host o IP", "Nome host o IP");
		add("hours",                                     "Stunden", "hours", "heures", "horas", "ore");
		add("keepPasswordInMemory",                      "Passwort im Speicher behalten", "Keep password in memory", "Conserver le mot de passe en mémoire", "Mantener la contraseña en memoria", "Mantieni la password in memoria");
		add("keyFormat",                                 "Schlüsselformat", "Key format", "Format de clé", "Formato de clave", "Formato della chiave");
		add("keyStrength",                               "Schlüsselstärke", "Key strength", "Force de la clé", "Longitud de clave", "Lunghezza della chiave");
		add("keystrengthExceedsAllowedJCE",              "Die Schlüsselstärke übersteigt das von der JCE erlaubte Maximum", "Key strength exceeds the maximum allowed by JCE", "La force de la clé dépasse le maximum autorisé par la JCE", "La longitud de clave supera el máximo permitido por la JCE", "La lunghezza della chiave supera il massimo consentito dalla JCE");
		add("keyType",                                   "Schlüsseltyp", "Key type", "Type de clé", "Tipo de clave", "Tipo di chiave");
		add("languageChangeRequiresRestart",             "Die Änderung der Sprache wird erst nach einem Neustart der Anwendung wirksam", "Changing the language requires a restart of the application", "Le changement de langue ne prend effet qu'après un redémarrage de l'application", "El cambio de idioma solo se aplicará tras reiniciar la aplicación", "Il cambio di lingua avrà effetto solo dopo il riavvio dell'applicazione");
		add("latestDuration",                            "Letzte Dauer", "Latest duration", "Dernière durée", "Última duración", "Ultima durata");
		add("latestStatus",                              "Letzter Status", "Latest status", "Dernier statut", "Último estado", "Ultimo stato");
		add("loadServerEntries",                         "Servereinträge laden", "Load server entries", "Charger les entrées de serveur", "Cargar entradas de servidor", "Carica voci server");
		add("maxDuration",                               "Max. Dauer", "Max. duration", "Durée max.", "Duración máx.", "Durata max.");
		add("millis",                                    "Millisekunden", "milliseconds", "millisecondes", "milisegundos", "millisecondi");
		add("minDuration",                               "Min. Dauer", "Min. duration", "Durée min.", "Duración mín.", "Durata min.");
		add("minutes",                                   "Minuten", "minutes", "minutes", "minutos", "minuti");
		add("None",                                      "Keine", "None", "Aucun", "Ninguno", "Nessuno");
		add("notFound",                                  "Nicht gefunden", "Not found", "Introuvable", "No encontrado", "Non trovato");
		add("numberOfParallelWorkers",                   "Anzahl paralleler Worker", "Number of parallel workers", "Nombre de workers parallèles", "Número de workers paralelos", "Numero di worker paralleli");
		add("numberOfRepetitionsPerWorker",              "Wiederholungen pro Worker", "Repetitions per worker", "Répétitions par worker", "Repeticiones por worker", "Ripetizioni per worker");
		add("open",                                      "Öffnen", "Open", "Ouvrir", "Abrir", "Apri");
		add("openDataFile",                              "Datendatei öffnen", "Open data file", "Ouvrir un fichier de données", "Abrir archivo de datos", "Apri file di dati");
		add("openKeyFile",                               "Schlüsseldatei öffnen", "Open key file", "Ouvrir un fichier de clé", "Abrir archivo de clave", "Apri file della chiave");
		add("pathIsDirectory",                           "Der Pfad ist ein Verzeichnis", "The path is a directory", "Le chemin est un répertoire", "La ruta es un directorio", "Il percorso è una directory");
		add("port",                                      "Port", "Port", "Port", "Puerto", "Porta");
		add("privateKey",                                "Privater Schlüssel", "Private key", "Clé privée", "Clave privada", "Chiave privata");
		add("protocol",                                  "Protokoll", "Protocol", "Protocole", "Protocolo", "Protocollo");
		add("publicKey",                                 "Öffentlicher Schlüssel", "Public key", "Clé publique", "Clave pública", "Chiave pubblica");
		add("reallyDeleteFile",                          "Datei wirklich löschen?\n{0}", "Really delete file?\n{0}", "Voulez-vous vraiment supprimer le fichier ?\n{0}", "¿Eliminar realmente el archivo?\n{0}", "Eliminare davvero il file?\n{0}");
		add("reallyDeleteFolder",                        "Ordner wirklich löschen?\n{0}", "Really delete folder?\n{0}", "Voulez-vous vraiment supprimer le dossier ?\n{0}", "¿Eliminar realmente la carpeta?\n{0}", "Eliminare davvero la cartella?\n{0}");
		add("rememberUpdateCredentials",                 "Zugangsdaten für Updates merken", "Remember update credentials", "Mémoriser les identifiants de mise à jour", "Recordar las credenciales de actualización", "Memorizza le credenziali di aggiornamento");
		add("removeServerEntry",                         "Servereintrag entfernen", "Remove server entry", "Supprimer l'entrée de serveur", "Eliminar entrada de servidor", "Rimuovi voce server");
		add("runtimeProperties",                         "Laufzeit-Eigenschaften", "Runtime properties", "Propriétés d'exécution", "Propiedades de tiempo de ejecución", "Proprietà di runtime");
		add("save",                                      "Speichern", "Save", "Enregistrer", "Guardar", "Salva");
		add("saveData",                                  "Daten speichern", "Save data", "Enregistrer les données", "Guardar datos", "Salva dati");
		add("saveFile",                                  "Datei speichern", "Save file", "Enregistrer le fichier", "Guardar archivo", "Salva file");
		add("saveResults",                               "Ergebnisse speichern", "Save results", "Enregistrer les résultats", "Guardar resultados", "Salva risultati");
		add("saveServerEntry",                           "Servereintrag speichern", "Save server entry", "Enregistrer l'entrée de serveur", "Guardar entrada de servidor", "Salva voce server");
		add("seconds",                                   "Sekunden", "seconds", "secondes", "segundos", "secondi");
		add("serverEntries",                             "Servereinträge", "Server entries", "Entrées de serveur", "Entradas de servidor", "Voci server");
		add("sharename",                                 "Freigabename", "Share name", "Nom du partage", "Nombre del recurso compartido", "Nome condivisione");
		add("showRuntimeProperties",                     "Laufzeit-Eigenschaften anzeigen", "Show runtime properties", "Afficher les propriétés d'exécution", "Mostrar propiedades de tiempo de ejecución", "Mostra proprietà di runtime");
		add("showSwtProperties",                         "SWT-Eigenschaften anzeigen", "Show SWT properties", "Afficher les propriétés SWT", "Mostrar propiedades de SWT", "Mostra proprietà SWT");
		add("showSystemProperties",                      "System-Eigenschaften anzeigen", "Show system properties", "Afficher les propriétés système", "Mostrar propiedades del sistema", "Mostra proprietà di sistema");
		add("start",                                     "Start", "Start", "Démarrer", "Iniciar", "Avvia");
		add("success",                                   "Erfolg", "Success", "Succès", "Éxito", "Successo");
		add("successCount",                              "Erfolgsanzahl", "Success count", "Nombre de succès", "Número de éxitos", "Numero di successi");
		add("swtProperties",                             "SWT-Eigenschaften", "SWT properties", "Propriétés SWT", "Propiedades de SWT", "Proprietà SWT");
		add("systemProperties",                          "System-Eigenschaften", "System properties", "Propriétés système", "Propiedades del sistema", "Proprietà di sistema");
		add("Unknown",                                   "Unbekannt", "Unknown", "Inconnu", "Desconocido", "Sconosciuto");
		add("upload",                                    "Hochladen", "Upload", "Téléverser", "Subir", "Carica");
		add("uploadFile",                                "Datei hochladen", "Upload file", "Téléverser un fichier", "Subir archivo", "Carica file");
		add("uploadFolder",                              "Ordner hochladen", "Upload folder", "Téléverser un dossier", "Subir carpeta", "Carica cartella");
		add("useKeyFile",                                "Schlüsseldatei verwenden", "Use key file", "Utiliser un fichier de clé", "Usar archivo de clave", "Usa file della chiave");
		add("weeks",                                     "Wochen", "weeks", "semaines", "semanas", "settimane");
		add("window_title",                              "Sichere Daten", "Secure data", "Données sécurisées", "Datos seguros", "Dati protetti");
		add("workerSleepTime",                           "Pause zwischen Durchläufen", "Worker sleep time", "Pause entre les exécutions", "Pausa entre ejecuciones", "Pausa tra le esecuzioni");
		add("years",                                     "Jahre", "years", "ans", "años", "anni");

		// Labels of the library's own configuration keys (used as "conf." + key in ApplicationConfigurationDialog)
		add("conf.Application.Language",               "Sprache", "Language", "Langue", "Idioma", "Lingua");
		add("conf.Application.ProxyConfigurationType", "Proxy-Konfiguration", "Proxy configuration", "Configuration du proxy", "Configuración del proxy", "Configurazione proxy");
		add("conf.Application.ProxyUrl",               "Proxy-URL", "Proxy URL", "URL du proxy", "URL del proxy", "URL del proxy");
		add("conf.Application.DailyUpdateCheck",       "Tägliche Update Prüfung", "Daily update check", "Vérification quotidienne des mises à jour", "Comprobación diaria de actualizaciones", "Controllo giornaliero degli aggiornamenti");
	}

	private static void add(final String key, final String germanText, final String englishText, final String frenchText, final String spanishText, final String italianText) {
		GERMAN.put(key, germanText);
		ENGLISH.put(key, englishText);
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