package de.soderer.utilities;

import java.awt.Desktop;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.program.Program;

/**
 * Allgemeine Utilities
 * 
 * Diese Klasse hat kein Logging via Log4J, da sie meist schon vor dessen Initialisierung genutzt wird
 */
public class Utilities {
	public static final String GERMAN_TEST_STRING = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890 äöüßÄÖÜµ!?§@€$%&/\\<>(){}[]'\"´`^°²³*#.,;:=+-~_|";
	public static final String STANDARD_XML = "<?xml version=\"1.0\" encoding=\"<encoding>\" standalone=\"yes\"?>\n<root>\n</root>\n";
	public static final String STANDARD_HTML = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n<html xmlns=\"http://www.w3.org/1999/xhtml\">\n\t<head>\n\t\t<meta http-equiv=\"Content-Type\" content=\"text/html; charset=<encoding>\" />\n\t\t<title>HtmlTitle</title>\n\t\t<meta name=\"Title\" content=\"HtmlTitle\" />\n\t</head>\n\t<body>\n\t</body>\n</html>\n";
	public static final String STANDARD_BASHSCRIPTSTART = "#!/bin/bash\n";
	
	public static final byte[] UTF_16_LE_BOM = new byte[] { (byte)0xFF, (byte)0xFE };
	public static final byte[] UTF_16_BE_BOM = new byte[] { (byte)0xFE, (byte)0xFF };
	public static final byte[] UTF_8_BOM = new byte[] { (byte)0xEF, (byte)0xBB, (byte)0xBF };

	public static String generateUUID() {
		return UUID.randomUUID().toString().toUpperCase().replaceAll("-", "");
	}

	public static URI getResource(String resourceName) {
		// Find File using the ClassLoader
		try {
			URL url = Utilities.class.getResource("/" + resourceName);
			if (url != null)
				return url.toURI();
			else
				return null;
		} catch (Exception e) {
			System.out.println("Looking for Resource '" + resourceName + "' on the classpath root");
			System.out.println(e.getClass().getName() + ": " + e.getMessage());
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
			return null;
		}
	}

	public static InputStream getResourceAsStream(String resourceName) {
		return Utilities.class.getResourceAsStream("/" + resourceName);
	}

	/**
	 * Decodieren eines URL encodierten Strings.
	 * 
	 * @param decode
	 *            ein URL endcodierter String
	 * @return einen normalen String in Klartext
	 * @throws Exception
	 */
	public static String decodeURL(String decode) throws Exception {
		try {
			return URLDecoder.decode(decode, "ISO-8859-1");
		} catch (UnsupportedEncodingException e) {
			throw new Exception("Fehler in der URL-Decodierung", e);
		}
	}

	public static byte[] encodeToZippedBase64(byte[] clearArrayToEncode) {
		try {
			ByteArrayOutputStream encoded = new ByteArrayOutputStream();

			GZIPOutputStream gzipCompresser = new GZIPOutputStream(encoded);
			gzipCompresser.write(clearArrayToEncode);
			gzipCompresser.close();

			return Base64.encodeBase64(encoded.toByteArray());
		} catch (IOException e) {
			return null;
		}
	}

	public static byte[] decodeFromZippedBase64(byte[] zipedArrayToDecode) throws Exception {
		return unzipByteArray(Base64.decodeBase64(zipedArrayToDecode));
	}

	/**
	 * Unzippen eines ByteArrays mit GZIP-Algorithmus
	 * 
	 * @param zippedData
	 * @return unzippedData
	 * @throws Exception
	 */
	public static byte[] unzipByteArray(byte[] zippedData) throws Exception {
		try {
			ByteArrayOutputStream decoded = new ByteArrayOutputStream();
			ByteArrayInputStream encoded = new ByteArrayInputStream(zippedData);
			IOUtils.copy(new GZIPInputStream(encoded), decoded);
			return decoded.toByteArray();
		} catch (IOException e) {
			throw new Exception("Komprimierte Daten konnten nicht entpackt werden", e);
		}
	}

	public static String getDuration(Calendar startTime, Calendar endTime) {
		int durationInMilliSeconds = (int)(endTime.getTimeInMillis() - startTime.getTimeInMillis());
		int milliSecondsPart = durationInMilliSeconds % 1000;
		int secondsPart = durationInMilliSeconds / 1000 % 60;
		int minutesPart = (durationInMilliSeconds / 1000 / 60) % 60;
		int hoursPart = durationInMilliSeconds / 1000 / 60 / 60 % 24;
		int days = durationInMilliSeconds / 1000 / 60 / 60 % 24;

		String returnString = milliSecondsPart + "ms";
		if (secondsPart > 0)
			returnString = secondsPart + "s " + returnString;
		if (minutesPart > 0)
			returnString = minutesPart + "m " + returnString;
		if (hoursPart > 0)
			returnString = hoursPart + "h " + returnString;
		if (days > 0)
			returnString = days + "d " + returnString;
		return returnString;
	}

	/**
	 * Auslesen eines DateiVerzeichnisses und Ermitteln aller Dateien die mit einem Stammnamen beginnen
	 * 
	 * @param searchPath
	 * @return Array of Files mit den Dateien die gefunden wurden. Im Fehlerfall null
	 */
	public static ArrayList<File> getArrayOfFilesByStammname(String stammNamenListe) {
		return getArrayOfFilesByStammname(Arrays.asList(stammNamenListe.split(";")));
	}

	public static ArrayList<File> getArrayOfFilesByStammname(List<String> stammNamenListe) {
		ArrayList<File> ergebnisListe = new ArrayList<File>();

		for (String stammName : stammNamenListe) {
			// Suchverzeichnis und Dateistammname ermitteln
			// Achtung: stammName kann auch wildcards wie '*' enthalten daher
			// NICHT mit File-Class arbeiten
			int unixLastIndexOfSeparator = stammName.lastIndexOf("/");
			int dosLastIndexOfSeparator = stammName.lastIndexOf("\\");
			int lastIndexOfSeparator = Math.max(unixLastIndexOfSeparator, dosLastIndexOfSeparator);
			String suchVerzeichnis = stammName.substring(0, lastIndexOfSeparator + 1);
			String stammFileName = stammName.substring(lastIndexOfSeparator + 1);

			// Dateiliste erstellen
			File searchPath = new File(suchVerzeichnis);
			if (searchPath.isDirectory()) {
				File[] partList = searchPath.listFiles();
				for (int i = 0; i < partList.length; i++) {
					if (partList[i].isFile()
							&& ((stammFileName.endsWith("*") && partList[i].getName().startsWith(stammFileName.substring(0, stammFileName.length() - 1))) || (!stammFileName.endsWith("*") && partList[i]
									.getName().equals(stammFileName)))) {
						// Datei gefunden: partList[i].getName());
						if (!ergebnisListe.contains(partList[i])) {
							ergebnisListe.add(partList[i]);
						}
					}
				}
			}
		}

		return ergebnisListe;
	}

	public static List<File> getFilesByPattern(File startDirectory, String patternString, boolean traverseCompletely) {
		return getFilesByPattern(startDirectory, Pattern.compile(patternString), traverseCompletely);
	}

	public static List<File> getFilesByPattern(File startDirectory, Pattern pattern, boolean traverseCompletely) {
		List<File> files = new ArrayList<File>();
		if (startDirectory.isDirectory()) {
			for (File file : startDirectory.listFiles()) {
				if (file.isDirectory() && traverseCompletely) {
					files.addAll(getFilesByPattern(file, pattern, traverseCompletely));
				} else if (file.isFile() && pattern.matcher(file.getName()).matches()) {
					files.add(file);
				}
			}
		}
		return files;
	}

	public static List<File> getFilesWithWildcards(File startDirectory, String fileFilterWithWildcards, boolean traverseCompletely) {
		List<File> files = new ArrayList<File>();
		if (startDirectory.isDirectory()) {
			FileFilter fileFilter = new WildcardFileFilter(fileFilterWithWildcards);
			for (File file : startDirectory.listFiles(fileFilter)) {
				if (file.isFile()) {
					files.add(file);
				}
			}

			if (traverseCompletely) {
				for (File file : startDirectory.listFiles()) {
					if (file.isDirectory()) {
						files.addAll(getFilesWithWildcards(file, fileFilterWithWildcards, traverseCompletely));
					}
				}
			}
		}
		return files;
	}

	public static boolean checkForValidUserName(String value) {
		return value != null && value.matches("[A-Za-z0-9_-]*");
	}

	public static String[] convertArrayListOfStringsToStringArray(ArrayList<String> pArrayListOfStrings) {
		String[] arrayofStrings = new String[0];
		return pArrayListOfStrings.toArray(arrayofStrings);
	}

	public static int getIndex(int searchInt, int[] intArray) {
		for (int i = 0; i < intArray.length; i++) {
			if (intArray[i] == searchInt)
				return i;
		}
		return -1;
	}

	/**
	 * Auslesen des Streams in einen String
	 */
	public static String inputStreamToString(InputStream in) throws IOException {
		BufferedReader bufferedReader = null;
		try {
			bufferedReader = new BufferedReader(new InputStreamReader(in));
			StringBuffer stringBuilder = new StringBuffer();
			String line = null;

			while ((line = bufferedReader.readLine()) != null) {
				stringBuilder.append(line + "\n");
			}

			return stringBuilder.toString();
		} finally {
			if (bufferedReader != null) {
				bufferedReader.close();
				bufferedReader = null;
			}
		}
	}

	/**
	 * Einen String als InputStream zur Verfügung stellen
	 * 
	 * @throws UnsupportedEncodingException
	 */
	public static InputStream stringToInputStream(String valueString) throws UnsupportedEncodingException {
		return new ByteArrayInputStream(valueString.getBytes("UTF-8"));
	}

	public static String formatInt(int value, int len) {
		NumberFormat nf = NumberFormat.getInstance();
		nf.setMinimumIntegerDigits(len);
		nf.setMaximumIntegerDigits(len);
		nf.setGroupingUsed(false);

		return nf.format((long)value);
	}

	public static byte[] readStreamInArray(InputStream inputStream) throws Exception {
		// Wenn keine Daten aus dem Stream gelesen werden konnten
		// Wird mehrfach diese Thread-Wartezeit eingeräumt
		final int WAIT_TIME = 50;

		// Maximale Anzahl von Wartezyklen mit WAIT_TIME
		// Danach wird eine Exception geworfen
		final int MAX_WAIT_COUNT = 2000;

		ByteArrayOutputStream returnArrayBuffer = null;
		byte[] Buff = new byte[4096];
		int readBlockCount = 0;
		int waitCycleCount = 0;

		try {
			// Anlegen des Writers mit der Groesse des Streams
			returnArrayBuffer = new ByteArrayOutputStream();

			// In pContentLen sollte eigentlich immer die max. Laenge des
			// Streams stehen
			// Ende des Lesens ist, wenn genau diese Anzahl Bytes aus dem Stream
			// ausgelesen werden konnte
			// Das Lesen erfolgt in Bloecken zu max. 4096 Byte Laenge
			while (readBlockCount > -1) {
				try {
					readBlockCount = inputStream.read(Buff);

					if (readBlockCount > -1) {
						// Übergabe der Daten an den Writer
						for (int i = 0; i < readBlockCount; i++) {
							returnArrayBuffer.write((char)Buff[i]);
						}
						waitCycleCount = 0;
					} else if (readBlockCount < 0) {
						// Es wurden alle Daten gelesen
						continue;
					}
				} catch (SocketTimeoutException ex) {
					Thread.sleep(WAIT_TIME);
					waitCycleCount++;
				}

				// Prüfen ob max. Wartzeit ueberschritten?
				if (waitCycleCount > MAX_WAIT_COUNT) {
					throw new Exception("Fehler in readStreamInArray WaitExceed");
				}
			}
		} catch (InterruptedException ex) {
			// Wird von der Methode "Sleep" geworfen
			throw new Exception("Interrupted-Fehler in readStreamInArray: " + ex.getMessage(), ex);
		} catch (IOException ex) {
			throw new Exception("IO-Fehler in readStreamInArray: " + ex.getMessage(), ex);
		} catch (Exception ex) {
			throw new Exception("Fehler in readStreamInArray: " + ex.getMessage(), ex);
		} finally {
			IOUtils.closeQuietly(returnArrayBuffer);
		}

		return returnArrayBuffer.toByteArray();
	}
	
	public static byte[] readAvailableDataFromStreamInArray(InputStream inputStream) throws Exception {
		byte[] returnData = new byte[inputStream.available()];
		@SuppressWarnings("unused")
		int bytesRead = inputStream.read(returnData);
		return returnData;
	}
	
	public static byte[] readStreamUntilEndOrLinefeed(InputStream inStream) throws IOException {
		ByteArrayOutputStream returnData = new ByteArrayOutputStream();
		int nextByte;
		while (true) {
			nextByte = inStream.read();
			if (nextByte < 0) {
				break;
			} else if (nextByte == '\n') {
				returnData.write(nextByte);
				break;
			} else {
				returnData.write(nextByte);
			}
		}
		return returnData.toByteArray();
	}

	/**
	 * Ermitteln aller Dateien die anhand einer Zeile mit Stammnamen (durch ; getrennt) referenziert werden
	 * 
	 * @param pStammnamenListe
	 * @return
	 */
	protected List<File> evaluateStammnamenFileList(String pStammnamenListe) {
		// Ermitteln aller stammnamen aus der StammnamenListe
		ArrayList<String> alleStammnamen = new ArrayList<String>();
		String parameterZeile = pStammnamenListe; // ;-getrennter String mit den
		// Stammnamen
		while (parameterZeile.indexOf(";") > -1) {
			alleStammnamen.add(parameterZeile.substring(0, parameterZeile.indexOf(";")).trim());
			parameterZeile = parameterZeile.substring(parameterZeile.indexOf(";") + 1);
		}
		alleStammnamen.add(parameterZeile.trim());

		ArrayList<File> importFiles = new ArrayList<File>();

		// Suchen der Importfilenames
		for (int i = 0; i < alleStammnamen.size(); i++) {
			// Zusammenführen aller FileArrays zu Einem
			importFiles.addAll(Utilities.getArrayOfFilesByStammname((String)alleStammnamen.get(i)));
		}

		return importFiles;
	}

	/**
	 * Vergleich von 2 VersionsStrings (z.B.: 4.1.3)
	 * 
	 * @param version1
	 * @param version2
	 * @return -1 wenn Version1 < Version2<br>
	 *         0 wenn Version1 = Version2<br>
	 *         +1 wenn Version1 > Version2<br>
	 */
	public static int compareVersion(String version1, String version2) {
		String[] versionparts1 = version1.split("\\.");
		String[] versionparts2 = version2.split("\\.");
		for (int i = 0; i < Math.min(versionparts1.length, versionparts1.length); i++) {
			int value1 = 0;
			if (versionparts1.length > i)
				value1 = Integer.parseInt(versionparts1[i]);
			int value2 = 0;
			if (versionparts2.length > i)
				value2 = Integer.parseInt(versionparts2[i]);
			if (value1 < value2)
				return -1;
			else if (value1 > value2)
				return 1;
		}
		return 0;
	}

	public static String getEmailFromCertificate(X509Certificate cert) {
		String[] nameParts = cert.getSubjectX500Principal().toString().split(",");
		for (String namePart : nameParts) {
			if (namePart.matches("^[ \\t]*EMAILADDRESS=.*"))
				return namePart.substring(namePart.indexOf("=") + 1).trim();
		}

		return null;
	}

	public static String getCnFromCertificate(X509Certificate cert) {
		String[] nameParts = cert.getSubjectX500Principal().toString().split(",");
		for (String namePart : nameParts) {
			if (namePart.matches("^[ \\t]*CN=.*"))
				return namePart.substring(namePart.indexOf("=") + 1).trim();
		}

		return null;
	}

	public static boolean dayListIncludes(List<GregorianCalendar> listOfDays, GregorianCalendar day) {
		for (GregorianCalendar listDay : listOfDays) {
			if (listDay.get(Calendar.DAY_OF_YEAR) == day.get(Calendar.DAY_OF_YEAR))
				return true;
		}
		return false;
	}

	/**
	 * Ermitteln des nächsten geplanten Startzeitpunktes Der Timingparameter kann Wochentage, Zeiten, Monats-, Quartalsplanungen und Feiertage enthalten
	 * 
	 * Mögliche Ausprägungen: "ONCE" => nur einmalig (returns null) "0600;0800" => täglich um 06:00 und um 08:00 "MoMi:1700" => Montags und Mittwochs um 17:00 "M05:1600" => jeden 05.ten im Monat um
	 * 16:00 "Q:1600" => Jeden ersten Tag im Quartal um 16:00 "QW:1600" => Jeden ersten Werktag im Quartal um 16:00 "MoDiMiDoFr:1700;!23012011" => Mo bis Fr um 17:00 aber nicht am 23.01.2011
	 * (Feiertagsregelung durch '!'-Zeichen)
	 * 
	 * Alle Ausprägungen können beliebig kombiniert werden, werden dann durch Semikolons getrennt aufgelistet
	 * 
	 * @param timingString
	 * @return
	 * @throws Exception
	 */
	public static Timestamp calculateNextJobStart(String timingString) {
		GregorianCalendar now = new GregorianCalendar();
		GregorianCalendar returnStart = new GregorianCalendar();
		returnStart.add(GregorianCalendar.YEAR, 1);

		// Feiertage die explizit ausgeschlossen werden
		List<GregorianCalendar> excludedDays = new ArrayList<GregorianCalendar>();

		if (timingString.equalsIgnoreCase("once"))
			return null;

		String[] timingParameterList = timingString.split(";");
		for (String timingParameter : timingParameterList) {
			if (timingParameter.startsWith("!")) {
				try {
					GregorianCalendar exclusionDate = new GregorianCalendar();
					exclusionDate.setTime(DateUtils.DDMMYYYY.parse(timingParameter.substring(1)));
					excludedDays.add(exclusionDate);
				} catch (ParseException e) {
					e.printStackTrace();
				}
			}
		}

		for (String timingParameter : timingParameterList) {
			GregorianCalendar nextStartByThisParameter = new GregorianCalendar();
			nextStartByThisParameter.setTime(now.getTime());

			if (timingParameter.startsWith("!")) {
				// Exclusionen wurden bereits verarbeitet
				continue;
			} else if (!timingParameter.contains(":")) {
				if (isNumber(timingParameter)) {
					// tägliche Verarbeitung zur gegebenen Uhrzeit
					nextStartByThisParameter.set(GregorianCalendar.HOUR_OF_DAY, Integer.parseInt(timingParameter.substring(0, 2)));
					nextStartByThisParameter.set(GregorianCalendar.MINUTE, Integer.parseInt(timingParameter.substring(2)));
					nextStartByThisParameter.set(GregorianCalendar.SECOND, 0);
					nextStartByThisParameter.set(GregorianCalendar.MILLISECOND, 0);

					// nächsten Start solange in die Zukunft verschieben (+1Tag)
					// bis
					// Parameter-Bedingung erfüllt ist
					// Bei treffender Feiertagseintragung auch um einen Tag
					// weiterschieben
					while (nextStartByThisParameter.before(now) && nextStartByThisParameter.before(returnStart) || dayListIncludes(excludedDays, nextStartByThisParameter))
						nextStartByThisParameter.add(GregorianCalendar.DAY_OF_MONTH, 1);
				} else {
					// wochentägliche Verarbeitung um 00:00 Uhr
					List<Integer> weekdayIndexes = new ArrayList<Integer>();
					for (String weekDay : TextUtilities.chopToChunks(timingParameter, 2)) {
						weekdayIndexes.add(DateUtils.getWeekdayIndex(weekDay));
					}
					nextStartByThisParameter.set(GregorianCalendar.HOUR_OF_DAY, 0);
					nextStartByThisParameter.set(GregorianCalendar.MINUTE, 0);
					nextStartByThisParameter.set(GregorianCalendar.SECOND, 0);
					nextStartByThisParameter.set(GregorianCalendar.MILLISECOND, 0);

					// nächsten Start solange in die Zukunft verschieben (+1Tag)
					// bis
					// Parameter-Bedingung erfüllt ist
					// Bei treffender Feiertagseintragung auch um einen Tag
					// weiterschieben
					while ((nextStartByThisParameter.before(now) || !weekdayIndexes.contains(nextStartByThisParameter.get(Calendar.DAY_OF_WEEK))) && nextStartByThisParameter.before(returnStart)
							|| dayListIncludes(excludedDays, nextStartByThisParameter))
						nextStartByThisParameter.add(GregorianCalendar.DAY_OF_MONTH, 1);
				}
			} else if (timingParameter.length() == 8) {
				// Monatssteuerung "M01:1700"
				String tag = timingParameter.substring(1, timingParameter.indexOf(":"));
				String zeit = timingParameter.substring(timingParameter.indexOf(":") + 1);
				nextStartByThisParameter.set(GregorianCalendar.DAY_OF_MONTH, Integer.parseInt(tag));
				nextStartByThisParameter.set(GregorianCalendar.HOUR_OF_DAY, Integer.parseInt(zeit.substring(0, 2)));
				nextStartByThisParameter.set(GregorianCalendar.MINUTE, Integer.parseInt(zeit.substring(2)));
				nextStartByThisParameter.set(GregorianCalendar.SECOND, 0);
				nextStartByThisParameter.set(GregorianCalendar.MILLISECOND, 0);

				// Den nächsten passenden Monat finden
				while (nextStartByThisParameter.before(now) && nextStartByThisParameter.before(returnStart))
					nextStartByThisParameter.add(GregorianCalendar.MONTH, 1);

				// Bei treffender Feiertagseintragung um einen Tag
				// weiterschieben
				while (dayListIncludes(excludedDays, nextStartByThisParameter))
					nextStartByThisParameter.add(GregorianCalendar.DAY_OF_YEAR, 1);
			} else if (timingParameter.startsWith("Q:")) {
				// Quartalsweise Planung (Q:1200): Start am ersten Kalendertag
				// im Quartal
				if (nextStartByThisParameter.get(GregorianCalendar.MONTH) < GregorianCalendar.APRIL)
					nextStartByThisParameter.set(GregorianCalendar.MONTH, GregorianCalendar.APRIL);
				else if (nextStartByThisParameter.get(GregorianCalendar.MONTH) < GregorianCalendar.JULY)
					nextStartByThisParameter.set(GregorianCalendar.MONTH, GregorianCalendar.JULY);
				else if (nextStartByThisParameter.get(GregorianCalendar.MONTH) < GregorianCalendar.OCTOBER)
					nextStartByThisParameter.set(GregorianCalendar.MONTH, GregorianCalendar.OCTOBER);
				else {
					nextStartByThisParameter.set(GregorianCalendar.MONTH, GregorianCalendar.JANUARY);
					nextStartByThisParameter.add(GregorianCalendar.YEAR, 1);
				}

				nextStartByThisParameter.set(GregorianCalendar.DAY_OF_MONTH, 1);
				String zeit = timingParameter.substring(timingParameter.indexOf(":") + 1);
				nextStartByThisParameter.set(GregorianCalendar.HOUR_OF_DAY, Integer.parseInt(zeit.substring(0, 2)));
				nextStartByThisParameter.set(GregorianCalendar.MINUTE, Integer.parseInt(zeit.substring(2)));
				nextStartByThisParameter.set(GregorianCalendar.SECOND, 0);
				nextStartByThisParameter.set(GregorianCalendar.MILLISECOND, 0);

				// Bei treffender Feiertagseintragung um einen Tag
				// weiterschieben
				while (dayListIncludes(excludedDays, nextStartByThisParameter))
					nextStartByThisParameter.add(GregorianCalendar.DAY_OF_YEAR, 1);
			} else if (timingParameter.startsWith("QW:")) {
				// Quartalsweise Planung (QW:1200): Start am ersten Werktag im
				// Quartal
				if (nextStartByThisParameter.get(GregorianCalendar.MONTH) < GregorianCalendar.APRIL)
					nextStartByThisParameter.set(GregorianCalendar.MONTH, GregorianCalendar.APRIL);
				else if (nextStartByThisParameter.get(GregorianCalendar.MONTH) < GregorianCalendar.JULY)
					nextStartByThisParameter.set(GregorianCalendar.MONTH, GregorianCalendar.JULY);
				else if (nextStartByThisParameter.get(GregorianCalendar.MONTH) < GregorianCalendar.OCTOBER)
					nextStartByThisParameter.set(GregorianCalendar.MONTH, GregorianCalendar.OCTOBER);
				else {
					nextStartByThisParameter.set(GregorianCalendar.MONTH, GregorianCalendar.JANUARY);
					nextStartByThisParameter.add(GregorianCalendar.YEAR, 1);
				}

				nextStartByThisParameter.set(GregorianCalendar.DAY_OF_MONTH, 1);

				// Bei treffender Feiertagseintragung oder Wochentagsregelung um
				// einen Tag weiterschieben
				while (nextStartByThisParameter.get(GregorianCalendar.DAY_OF_WEEK) == java.util.Calendar.SATURDAY
						|| nextStartByThisParameter.get(GregorianCalendar.DAY_OF_WEEK) == java.util.Calendar.SUNDAY || dayListIncludes(excludedDays, nextStartByThisParameter))
					nextStartByThisParameter.add(GregorianCalendar.DAY_OF_MONTH, 1);

				String zeit = timingParameter.substring(timingParameter.indexOf(":") + 1);
				nextStartByThisParameter.set(GregorianCalendar.HOUR_OF_DAY, Integer.parseInt(zeit.substring(0, 2)));
				nextStartByThisParameter.set(GregorianCalendar.MINUTE, Integer.parseInt(zeit.substring(2)));
				nextStartByThisParameter.set(GregorianCalendar.SECOND, 0);
				nextStartByThisParameter.set(GregorianCalendar.MILLISECOND, 0);
			} else {
				// WochenTagessteuerung (ermöglicht auch Werktagssteuerung)
				String wochenTage = timingParameter.substring(0, timingParameter.indexOf(":"));
				String zeit = timingParameter.substring(timingParameter.indexOf(":") + 1);
				List<Integer> weekdayIndexes = new ArrayList<Integer>();
				for (String weekDay : TextUtilities.chopToChunks(wochenTage, 2)) {
					weekdayIndexes.add(DateUtils.getWeekdayIndex(weekDay));
				}
				nextStartByThisParameter.set(GregorianCalendar.HOUR_OF_DAY, Integer.parseInt(zeit.substring(0, 2)));
				nextStartByThisParameter.set(GregorianCalendar.MINUTE, Integer.parseInt(zeit.substring(2)));
				nextStartByThisParameter.set(GregorianCalendar.SECOND, 0);
				nextStartByThisParameter.set(GregorianCalendar.MILLISECOND, 0);

				// nächsten Start solange in die Zukunft verschieben (+1Tag) bis
				// Parameter-Bedingung erfüllt ist
				// Bei treffender Feiertagseintragung auch um einen Tag
				// weiterschieben
				while ((nextStartByThisParameter.before(now) || !weekdayIndexes.contains(nextStartByThisParameter.get(Calendar.DAY_OF_WEEK))) && nextStartByThisParameter.before(returnStart)
						|| dayListIncludes(excludedDays, nextStartByThisParameter))
					nextStartByThisParameter.add(GregorianCalendar.DAY_OF_MONTH, 1);
			}

			if (nextStartByThisParameter.before(returnStart))
				returnStart = nextStartByThisParameter;
		}

		return new Timestamp(returnStart.getTimeInMillis());
	}

	public static int[] parseVersionNumber(String versionString) {
		try {
			Pattern versionPattern = Pattern.compile("^(\\d+)(?:\\.(\\d+)){0,1}(?:\\.(\\d+)){0,1}$");
			Matcher matcher = versionPattern.matcher(versionString);
			if (!matcher.find() || matcher.groupCount() < 1 || matcher.groupCount() > 3) {
				throw new IllegalArgumentException("Version must be in form <major>[.<minor>[.<micro>]]");
			}

			int major = new Integer(matcher.group(1));
			int minor = 0;
			if (matcher.groupCount() >= 2 && matcher.group(2) != null)
				minor = new Integer(matcher.group(2));
			int micro = 0;
			if (matcher.groupCount() == 3 && matcher.group(3) != null)
				micro = new Integer(matcher.group(3));

			return new int[] { major, minor, micro };
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Version must contain integervalues only");
		}
	}

	public static boolean isNumber(String value) {
		try {
			Integer.parseInt(value);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	public static boolean testConnection(String hostname, int port) throws Exception {
		Socket socket = null;
		try {
			socket = new Socket();
			InetSocketAddress endPoint = new InetSocketAddress(hostname, port);
			int timeout = 2000; // 2 Sekunden
			if (endPoint.isUnresolved()) {
				throw new Exception("Hostname '" + hostname + "' konnte nicht aufgelöst werden");
			} else {
				try {
					socket.connect(endPoint, timeout);
					return true;
				} catch (IOException ioe) {
					throw new Exception("Verbindung zum Host '" + hostname + "' auf Port " + port + " konnte nicht aufgebaut werden: " + ioe.getClass().getSimpleName() + ": " + ioe.getMessage());
				}
			}
		} finally {
			IOUtils.closeQuietly(socket);
		}
	}

	public static GregorianCalendar getDayWithoutTime(GregorianCalendar value) {
		return new GregorianCalendar(value.get(GregorianCalendar.YEAR), value.get(GregorianCalendar.MONTH), value.get(GregorianCalendar.DAY_OF_MONTH));
	}

	public static UUID getUUIDFromString(String value) {
		StringBuilder uuidString = new StringBuilder(value);
		uuidString.insert(20, '-');
		uuidString.insert(16, '-');
		uuidString.insert(12, '-');
		uuidString.insert(8, '-');
		return UUID.fromString(uuidString.toString());
	}

	/***
	 * Aufteilen der Originalliste in kleinere Listen zu je maximal chunkSize-großen Listen
	 * 
	 * @param <E>
	 * @param originalList
	 * @param chunkSize
	 * @return
	 */
	public static <E> List<List<E>> chopListToChunks(List<E> originalList, int chunkSize) {
		if (originalList == null || originalList.size() <= 0 || chunkSize <= 0)
			return null;

		List<List<E>> returnList = new ArrayList<List<E>>();
		int endIndex = 0;

		while (endIndex < originalList.size()) {
			int startIndex = endIndex;
			if (chunkSize < originalList.size() - endIndex)
				endIndex += chunkSize;
			else
				endIndex = originalList.size();

			returnList.add(originalList.subList(startIndex, endIndex));
		}

		return returnList;
	}

	private static final char[] randomCharacters = "ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÜabcdefghijklmnopqrstuvwxyzäöüß".toCharArray();

	private static final char[] randomAlphaNumericCharacters = (new String(randomCharacters) + "0123456789").toCharArray();

	private static final Random random = new Random();

	public static String getRandomString(int length) {
		StringBuilder sb = new StringBuilder(length);
		for (int i = 0; i < length; i++) {
			sb.append(randomCharacters[random.nextInt(randomCharacters.length)]);
		}
		return sb.toString();
	}

	public static String getRandomAlphanumericString(int length) {
		StringBuilder sb = new StringBuilder(length);
		for (int i = 0; i < length; i++) {
			sb.append(randomAlphaNumericCharacters[random.nextInt(randomAlphaNumericCharacters.length)]);
		}
		return sb.toString();
	}

	public static String getRandomNumberString(int length) {
		StringBuilder sb = new StringBuilder(length);
		for (int i = 0; i < length; i++) {
			sb.append(random.nextInt(10));
		}
		return sb.toString();
	}

	public static byte getRandomByte() {
		byte[] resutlt = new byte[1];
		random.nextBytes(resutlt);
		return resutlt[0];
	}

	public static String getHostName() {
		try {
			return InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			return "unbekannter Rechnername";
		}
	}

	public static String convertCollectionToString(Collection<?> collection, String separator) {
		StringBuilder builder = new StringBuilder();

		for (Object object : collection) {
			if (builder.length() > 0)
				builder.append(separator);
			if (object == null)
				builder.append("<null>");
			else
				builder.append(object.toString());
		}

		return builder.toString();
	}

	public static boolean checkForIntervalContainment(String intervals, int item) {
		if (intervals != null && intervals.length() > 0) {
			String[] blockStrings = intervals.split(";");
			for (int i = 0; i < blockStrings.length; i++) {
				if (blockStrings[i].endsWith("+")) {
					if (Integer.parseInt(blockStrings[i].substring(0, blockStrings[i].length() - 1)) <= item)
						return true;
				} else if (blockStrings[i].matches("\\d+-\\d+")) {
					int plusIndex = blockStrings[i].indexOf("-");
					int startVersion = Integer.parseInt(blockStrings[i].substring(0, plusIndex));
					int endeVersion = Integer.parseInt(blockStrings[i].substring(plusIndex + 1));
					if (startVersion <= item && endeVersion >= item)
						return true;
				} else if (blockStrings[i].matches("-\\d+")) {
					int endeVersion = Integer.parseInt(blockStrings[i].substring(1));
					if (endeVersion >= item)
						return true;
				} else {
					if (Integer.parseInt(blockStrings[i]) == item)
						return true;
				}
			}
		}

		return false;
	}

	public static String getSameFilenameInParentDirectory(String file) {
		return getSameFilenameInParentDirectory(new File(file)).getAbsolutePath();
	}

	public static File getSameFilenameInParentDirectory(File file) {
		return new File(file.getParentFile().getParent() + File.separator + file.getName());
	}

	public static SCPFile getSameFilenameInParentDirectory(SCPFile file) throws Exception {
		return new SCPFile(file.getHostname(), file.getDirectory().getParent().getPath(), file.getFilename());
	}

	public static File[] getSubfilesByNameAndExtension(File directory, String nameStart, String extension) {
		List<File> files = new LinkedList<File>();
		for (File file : directory.listFiles()) {
			if (file.getName().startsWith(nameStart) && file.getName().endsWith("." + extension))
				files.add(file);
		}
		return files.toArray(new File[0]);
	}

	public static void deleteSubfilesByNameAndExtension(File directory, String nameStart, String extension) {
		for (File file : getSubfilesByNameAndExtension(directory, nameStart, extension)) {
			FileUtils.deleteQuietly(file);
		}
	}

	public static int getMinimumOfAllowedValues(int allowedValueMinimum, int... values) {
		int returnValue = Integer.MAX_VALUE;
		if (values != null) {
			for (int value : values) {
				if (value >= allowedValueMinimum) {
					returnValue = Math.min(returnValue, value);
				}
			}
		}
		return returnValue;
	}

	public static boolean interpretAsBool(String value) {
		if (StringUtils.isNotEmpty(value)) {
			value = value.trim();
			return value.equalsIgnoreCase("true") || value.equalsIgnoreCase("+") || value.equalsIgnoreCase("yes") || value.equalsIgnoreCase("ja") || value.equalsIgnoreCase("ok");
		} else {
			return false;
		}
	}

	public static boolean anyCharsAreEqual(char... values) {
		for (int i = 0; i < values.length; i++) {
			for (int j = i + 1; j < values.length; j++) {
				if (values[i] == values[j]) {
					return true;
				}
			}
		}
		return false;
	}

	public static byte[] xor(byte[]... byteArrays) {
		return xor(Arrays.asList(byteArrays));
	}

	public static byte[] xor(List<byte[]> byteArrays) {
		byte[] returnArray = new byte[byteArrays.get(0).length];

		for (int i = 0; i < byteArrays.size(); i++) {
			for (int j = 0; j < returnArray.length; j++) {
				returnArray[j] = xor(returnArray[j], byteArrays.get(i)[j]);
			}
		}

		return returnArray;
	}

	public static byte xor(byte... keyData) {
		byte resultItem = 0;

		for (byte key : keyData) {
			resultItem = (byte)(resultItem ^ key);
		}

		return resultItem;
	}

	public static int square(int value) {
		return value * value;
	}

	public static int pow(int base, int exp) {
		if (exp < 0) {
			throw new IllegalArgumentException("Invalid negative exponent");
		} else if (exp == 0) {
			return 1;
		} else {
			return square(pow(base, exp / 2)) * (exp % 2 == 1 ? base : 1);
		}
	}

	public static int getLineCount(File file) throws IOException {
		LineNumberReader lineNumberReader = null;
		try {
			lineNumberReader = new LineNumberReader(new InputStreamReader(new FileInputStream(file)));
			while (lineNumberReader.readLine() != null) {
				// do nothing
			}

			return lineNumberReader.getLineNumber();
		} finally {
			IOUtils.closeQuietly(lineNumberReader);
		}
	}

	public static <T extends Comparable<? super T>> List<T> asSortedList(Collection<T> c) {
		List<T> list = new ArrayList<T>(c);
		java.util.Collections.sort(list);
		return list;
	}

	public static boolean fileSearchText(File searchFile, String searchText, boolean searchCaseSensitive, boolean searchTextIsRegularExpression, String defaultEncoding) throws Exception {
		List<String> searchTexts = new ArrayList<String>();
		searchTexts.add(searchText);
		return fileSearchText(searchFile, searchTexts, searchCaseSensitive, searchTextIsRegularExpression, defaultEncoding);
	}

	public static boolean fileSearchText(File searchFile, List<String> searchTexts, boolean searchCaseSensitive, boolean searchTextIsRegularExpression, String defaultEncoding) throws Exception {
		FileInputStream fileInputStream = null;
		try {
			if (searchFile.length() > 0 && searchTexts != null && searchTexts.size() > 0) {
				byte[] encodingBuffer = Utilities.readFirstFileData(searchFile, 100);
				Tuple<String, Boolean> encodingData = TextUtilities.detectEncoding(encodingBuffer);
				if (encodingData == null) {
					encodingData = TextFilePropertiesReader.getEncodingData(defaultEncoding);
				}

				fileInputStream = new FileInputStream(searchFile);
				String dataString = IOUtils.toString(fileInputStream, encodingData.getFirst());

				for (int i = 0; i < searchTexts.size(); i++) {
					Pattern searchPattern;
					if (searchTextIsRegularExpression) {
						if (searchCaseSensitive) {
							searchPattern = Pattern.compile(searchTexts.get(i));
						} else {
							searchPattern = Pattern.compile(searchTexts.get(i), Pattern.CASE_INSENSITIVE);
						}
					} else {
						if (searchCaseSensitive) {
							searchPattern = Pattern.compile(searchTexts.get(i), Pattern.LITERAL);
						} else {
							searchPattern = Pattern.compile(searchTexts.get(i), Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
						}
					}
					Matcher matcher = searchPattern.matcher(dataString);
					if (matcher.find()) {
						return true;
					}
				}
			}
			return false;
		} finally {
			IOUtils.closeQuietly(fileInputStream);
		}
	}

	public static boolean fileReplaceText(File searchFile, String searchText, String replacementText, boolean searchCaseSensitive, boolean searchTextIsRegularExpression, String defaultEncoding)
			throws Exception {
		List<String> searchTexts = new ArrayList<String>();
		searchTexts.add(searchText);
		List<String> replacementTexts = new ArrayList<String>();
		replacementTexts.add(replacementText);
		return fileReplaceText(searchFile, searchTexts, replacementTexts, searchCaseSensitive, searchTextIsRegularExpression, defaultEncoding);
	}

	public static boolean fileReplaceText(File searchFile, List<String> searchTexts, List<String> replacementTexts, boolean searchCaseSensitive, boolean searchTextIsRegularExpression, String defaultEncoding) throws Exception {
		FileInputStream fileInputStream = null;
		try {
			boolean foundAndReplaced = false;
			if (searchFile.length() > 0 && searchTexts != null && searchTexts.size() > 0 && replacementTexts != null && replacementTexts.size() > 0) {
				byte[] encodingBuffer = Utilities.readFirstFileData(searchFile, 100);
				Tuple<String, Boolean> encodingData = TextUtilities.detectEncoding(encodingBuffer);
				if (encodingData == null) {
					encodingData = TextFilePropertiesReader.getEncodingData(defaultEncoding);
				}
	
				fileInputStream = new FileInputStream(searchFile);
				String dataString = IOUtils.toString(fileInputStream, encodingData.getFirst());
	
				for (int i = 0; i < searchTexts.size(); i++) {
					Pattern searchPattern;
					if (searchTextIsRegularExpression) {
						if (searchCaseSensitive) {
							searchPattern = Pattern.compile(searchTexts.get(i), Pattern.MULTILINE);
						} else {
							searchPattern = Pattern.compile(searchTexts.get(i), Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
						}
					} else {
						if (searchCaseSensitive) {
							searchPattern = Pattern.compile(searchTexts.get(i), Pattern.MULTILINE | Pattern.LITERAL);
						} else {
							searchPattern = Pattern.compile(searchTexts.get(i), Pattern.MULTILINE | Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
						}
					}
					Matcher matcher = searchPattern.matcher(dataString);
					if (matcher.find()) {
						dataString = matcher.replaceAll(replacementTexts.get(i));
						foundAndReplaced = true;
					}
				}
	
				if (foundAndReplaced) {
					FileUtils.writeStringToFile(searchFile, dataString, encodingData.getFirst());
				}
			}
			return foundAndReplaced;
		} finally {
			IOUtils.closeQuietly(fileInputStream);
		}
	}

	public static String getClassPath() {
		ClassLoader sysClassLoader = ClassLoader.getSystemClassLoader();
		URL[] urls = ((URLClassLoader)sysClassLoader).getURLs();

		StringBuilder classpath = new StringBuilder();
		for (int i = 0; i < urls.length; i++) {
			classpath.append(urls[i].getFile() + "\n");
		}
		return classpath.toString();
	}

	public static boolean compare(byte[] byteArray1, byte[] byteArray2) {
		if (byteArray1 == byteArray2) {
			return true;
		} else if (byteArray1 == null || byteArray2 == null || byteArray1.length != byteArray2.length) {
			return false;
		} else {
			for (int i = 0; i < byteArray1.length; i++) {
				if (byteArray1[i] != byteArray2[i]) {
					return false;
				}
			}
			return true;
		}
	}

	public static byte[] readFirstFileData(File dataFile, int maxAmountToRead) throws Exception {
		FileInputStream input = null;
		try {
			input = new FileInputStream(dataFile);
			byte[] encodingBufferInterim = new byte[maxAmountToRead];
			int bytesRead = input.read(encodingBufferInterim);
			byte[] encodingBuffer = ArrayUtils.subarray(encodingBufferInterim, 0, bytesRead);
			return encodingBuffer;
		} catch (Exception e) {
			throw e;
		} finally {
			IOUtils.closeQuietly(input);
		}
	}
	
	public static Map<String, String> getSystemPropertiesMap() {
		Map<String, String> propertiesMap = new HashMap<String, String>();
		for (Object key : System.getProperties().keySet()) {
			propertiesMap.put((String) key, System.getProperties().getProperty((String) key));
		}
		return propertiesMap;
	}
	
	public static String getStringFromMap(Map<String, ? extends Object> map, String entrySeparator, String keySeparator, boolean sort) {
		List<String> keyList = new ArrayList<String>(map.keySet());
		if (sort) {
			Collections.sort(keyList);
		}

		StringBuilder builder = new StringBuilder();
		for (Object key : keyList) {
			if (builder.length() > 0) {
				builder.append(entrySeparator);
			}
			builder.append(key == null ? "" : key);
			builder.append(keySeparator);
			Object value = map.get(key);
			builder.append(value == null ? "" : value.toString());
		}
		return builder.toString();
	}

	public static String getHumanReadableNumber(Number value) {
		return getHumanReadableNumber(value, null, true);
	}
	
	public static String getHumanReadableNumber(Number value, String unitTypeSign) {
		return getHumanReadableNumber(value, unitTypeSign, true);
	}
	
	public static String getHumanReadableNumber(Number value, String unitTypeSign, boolean siUnits) {
	    int unit = siUnits ? 1000 : 1024;
	    double interimValue = value.doubleValue();
	    String unitExtension = "";
	    if (interimValue < unit) {
	    	if (StringUtils.isNotBlank(unitTypeSign)) {
	    		unitExtension = " " + unitTypeSign;
	    	}
	    	
	    	if (value instanceof Integer || value instanceof Long) {
	    		return value + unitExtension;
	    	}
	    } else {
		    int exp = (int) (Math.log(interimValue) / Math.log(unit));
		    unitExtension = " " + (siUnits ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (siUnits ? "" : "i");
	    	if (StringUtils.isNotBlank(unitTypeSign)) {
	    		unitExtension += unitTypeSign;
	    	}
		    interimValue = interimValue / Math.pow(unit, exp);
	    }
	    
	    String valueString;
	    if (interimValue >= 1000) {
	    	valueString = String.format("%.1f", interimValue);
	    } else if (interimValue >= 100) {
	    	valueString = String.format("%.2f", interimValue);
	    } else if (interimValue >= 10) {
	    	valueString = String.format("%.3f", interimValue);
	    } else if (interimValue >= 1) {
	    	valueString = String.format("%.4f", interimValue);
	    } else {
	    	valueString = String.format("%.5f", interimValue);
	    }
	    
	    return valueString + unitExtension;
	}
	
	public static byte[] getMD5Hash(String data) throws Exception {
		try {
			return MessageDigest.getInstance("MD5").digest(data.getBytes("UTF-8"));
		} catch (Exception e) {
			throw new Exception("Error while MD5 hashing", e);
		}
	}
	
	public static byte[] getSHA1Hash(String data) throws Exception {
		try {
			return MessageDigest.getInstance("SHA-1").digest(data.getBytes("UTF-8"));
		} catch (Exception e) {
			throw new Exception("Error while SHA-1 hashing", e);
		}
	}
	
	public static byte[] getSHA512Hash(String data) throws Exception {
		try {
			return MessageDigest.getInstance("SHA-512").digest(data.getBytes("UTF-8"));
		} catch (Exception e) {
			throw new Exception("Error while SHA-512 hashing", e);
		}
	}
	
	public static byte[] getByteArray(List<Byte> data) {
		byte[] returnArray = new byte[data.size()];
		for (int i = 0; i < data.size(); i++) {
			returnArray[i] = data.get(i);
		}
		return returnArray;
	}
	
	public static String stacktraceToString(StackTraceElement[] stackTrace) {
		StringBuilder returnBuilder = new StringBuilder();
		if (stackTrace != null) {
			for (StackTraceElement stackTraceElement : stackTrace) {
				returnBuilder.append(stackTraceElement.toString());
				returnBuilder.append("\n");
			}
		}
		return returnBuilder.toString();
	}
	
	public static String throwableToString(Throwable throwable) {
		return throwableToString(throwable, 5);
	}
	
	public static String throwableToString(Throwable throwable, int maxLevel) {
		StringBuilder returnBuilder = new StringBuilder(throwable.getMessage() + "\n" + Utilities.stacktraceToString(throwable.getStackTrace()));
		int level = 0;
		Throwable subException = throwable.getCause();
		while (level < maxLevel && subException != null) {
			returnBuilder.append("\n\n" + subException.getMessage() + "\n" + Utilities.stacktraceToString(subException.getStackTrace()));
			level++;
			subException = subException.getCause();
		}
		if (level == maxLevel) {
			returnBuilder.append("\n\n... cut after level " + maxLevel + " ...");
		}
		return returnBuilder.toString();
	}
	
	public static boolean sendEmail(String senderAdress, String toAdressList, String ccAdressList, String subject, String bodyText, String bodyHtml, int mailtype, String charset) {
		try {
			// create some properties and get the default Session
			Properties props = new Properties();
			props.put("system.mail.host", "localhost");
			Session session = Session.getDefaultInstance(props, null);
			// session.setDebug(debug);

			// create a message
			MimeMessage msg = new MimeMessage(session);
			msg.setFrom(new InternetAddress(senderAdress));
			msg.setSubject(subject, charset);
			msg.setSentDate(new Date());

			// Set to-recipient email addresses
			InternetAddress[] toAddresses = getEmailAddressesFromList(toAdressList);
			if (toAddresses != null && toAddresses.length > 0) {
				msg.setRecipients(Message.RecipientType.TO, toAddresses);
			}

			// Set cc-recipient email addresses
			InternetAddress[] ccAddresses = getEmailAddressesFromList(ccAdressList);
			if (ccAddresses != null && ccAddresses.length > 0) {
				msg.setRecipients(Message.RecipientType.CC, ccAddresses);
			}

			switch (mailtype) {
				case 0:
					msg.setText(bodyText, charset);
					break;
				case 1:
					Multipart mp = new MimeMultipart("alternative");
					MimeBodyPart mbp = new MimeBodyPart();
					mbp.setText(bodyText, charset);
					mp.addBodyPart(mbp);
					mbp = new MimeBodyPart();
					mbp.setContent(bodyHtml, "text/html; charset=" + charset);
					mp.addBodyPart(mbp);
					msg.setContent(mp);
					break;
				default:
					msg.setText(bodyText, charset);
					break;
			}

			Transport.send(msg);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	public static InternetAddress[] getEmailAddressesFromList(String listString) {
		if (StringUtils.isNotBlank(listString)) {
			List<InternetAddress> emailAddresses = new ArrayList<InternetAddress>();
			for (String singleAdr : listString.split(";|,| ")) {
				singleAdr = singleAdr.trim();
				if (StringUtils.isNotBlank(singleAdr)) {
					try {
						InternetAddress nextAddress = new InternetAddress(singleAdr.trim());
						nextAddress.validate();
						emailAddresses.add(nextAddress);
					} catch (AddressException e) {
						e.printStackTrace();
					}
				}
			}

			return emailAddresses.toArray(new InternetAddress[emailAddresses.size()]);
		} else {
			return new InternetAddress[0];
		}
	}

	public static boolean openMailInStandardClient(String toAdress, String subject, String body) {
		return openMailInStandardClient(toAdress, null, subject, body);
	}
	
	public static boolean openMailInStandardClient(String toAdress, String ccAdress, String subject, String body) {
		try {
			File attachmentFile = null;
			String mailtoString = "mailto:" + URLEncoder.encode(toAdress, "UTF-8")
					+ "?" + (StringUtils.isNotBlank(ccAdress) ? "cc=" + URLEncoder.encode(ccAdress, "UTF-8") + "&" : "")
					+ "subject=" + URLEncoder.encode(subject, "UTF-8").replace("+", "%20")
					+ "&body=" + URLEncoder.encode(body, "UTF-8").replace("+", "%20")
					+ (attachmentFile != null ? "&attachment=" + attachmentFile.getAbsolutePath() : ""); // maybe it is "&attach="
			boolean success;
			try {
				success = Program.launch(mailtoString);
			} catch (Exception e) {
				throw new Exception("Open mail by swt.Program failed");
			}

			if (!success) {
				if (SystemUtilities.isWindowsSystem()) {
					if (Desktop.isDesktopSupported()) {
						Desktop desktop = Desktop.getDesktop();
						if (desktop.isSupported(Desktop.Action.MAIL)) {
							desktop.mail(new URI(mailtoString));
							return true;
						} else {
							return false;
						}
					} else {
						return false;
					}
				} else {
					try {
						// For using the parameters check the following description
						// http://kb.mozillazine.org/Command_line_arguments_%28Thunderbird%29
						Runtime.getRuntime().exec("thunderbird -compose");
					} catch (Exception e) {
						throw new Exception("Open mail by thunderbird failed");
					}
				}
				return true;
			} else {
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public static void downloadFile(String url, String localeDestionationPath) throws Exception {
		BufferedInputStream bufferedInputStream = null;
		FileOutputStream fileOutputStream = null;
		try {
			bufferedInputStream = new BufferedInputStream(new URL(url).openStream());
			fileOutputStream = new FileOutputStream(localeDestionationPath);
			IOUtils.copy(bufferedInputStream, fileOutputStream);
		} catch (Exception e) {
			throw new Exception("Cannot download file", e);
		} finally {
			IOUtils.closeQuietly(fileOutputStream);
			IOUtils.closeQuietly(bufferedInputStream);
		}
	}
	
	public static boolean checkForDuplicates(String[] inputArray, boolean ignoreNullValues) {
		Set<String> tempSet = new HashSet<String>();
        for (String stringItem : inputArray) {
        	if (!ignoreNullValues || stringItem != null) {
	            if (!tempSet.add(stringItem)) {
	                return true;
	            }
        	}
        }
        return false;
	}
}