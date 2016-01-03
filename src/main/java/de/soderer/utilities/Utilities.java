package de.soderer.utilities;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Global Utilities
 * 
 * This class does no Logging via Log4J, because it is often used before its initialisation
 */
public class Utilities {
	public static final String STANDARD_XML = "<?xml version=\"1.0\" encoding=\"<encoding>\" standalone=\"yes\"?>\n<root>\n</root>\n";
	public static final String STANDARD_HTML = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n<html xmlns=\"http://www.w3.org/1999/xhtml\">\n\t<head>\n\t\t<meta http-equiv=\"Content-Type\" content=\"text/html; charset=<encoding>\" />\n\t\t<title>HtmlTitle</title>\n\t\t<meta name=\"Title\" content=\"HtmlTitle\" />\n\t</head>\n\t<body>\n\t</body>\n</html>\n";
	public static final String STANDARD_BASHSCRIPTSTART = "#!/bin/bash\n";

	public static final byte[] UTF_16_LE_BOM = new byte[] { (byte)0xFF, (byte)0xFE };
	public static final byte[] UTF_16_BE_BOM = new byte[] { (byte)0xFE, (byte)0xFF };
	public static final byte[] UTF_8_BOM = new byte[] { (byte)0xEF, (byte)0xBB, (byte)0xBF };

	/**
	 * Generate a unique ID
	 * 
	 * @return
	 */
	public static String generateUUID() {
		return UUID.randomUUID().toString().toUpperCase().replaceAll("-", "");
	}

	/**
	 * Get a UUID from a string
	 * 
	 * @param value
	 * @return
	 */
	public static UUID getUUIDFromString(String value) {
		StringBuilder uuidString = new StringBuilder(value);
		uuidString.insert(20, '-');
		uuidString.insert(16, '-');
		uuidString.insert(12, '-');
		uuidString.insert(8, '-');
		return UUID.fromString(uuidString.toString());
	}

	/**
	 * Find a file using the ClassLoader
	 * 
	 * @param resourceName
	 * @return
	 */
	public static URI getResource(String resourceName) {
		try {
			URL url = Utilities.class.getResource("/" + resourceName);
			if (url != null) {
				return url.toURI();
			} else {
				return null;
			}
		} catch (Exception e) {
			System.out.println("Looking for Resource '" + resourceName + "' on the classpath root");
			System.out.println(e.getClass().getName() + ": " + e.getMessage());
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
			return null;
		}
	}

	/**
	 * Get the data of a file included in a jar file
	 * 
	 * @param resourceName
	 * @return
	 */
	public static InputStream getResourceAsStream(String resourceName) {
		return Utilities.class.getResourceAsStream("/" + resourceName);
	}

	/**
	 * Decode a URL encoded string
	 * 
	 * @param decode
	 * @return
	 * @throws Exception
	 */
	public static String decodeURL(String decode) throws Exception {
		try {
			return URLDecoder.decode(decode, "ISO-8859-1");
		} catch (UnsupportedEncodingException e) {
			throw new Exception("Fehler in der URL-Decodierung", e);
		}
	}

	/**
	 * Encode data in a zipped base64 string data
	 * 
	 * @param clearArrayToEncode
	 * @return
	 */
	public static byte[] encodeToZippedBase64(byte[] clearArrayToEncode) {
		try {
			ByteArrayOutputStream encoded = new ByteArrayOutputStream();

			GZIPOutputStream gzipCompresser = new GZIPOutputStream(encoded);
			gzipCompresser.write(clearArrayToEncode);
			gzipCompresser.close();

			return Base64.getEncoder().encode(encoded.toByteArray());
		} catch (IOException e) {
			return null;
		}
	}

	/**
	 * Decode data from a zipped base64 string data
	 * 
	 * @param zipedArrayToDecode
	 * @return
	 * @throws Exception
	 */
	public static byte[] decodeFromZippedBase64(byte[] zipedArrayToDecode) throws Exception {
		return unzipByteArray(Base64.getDecoder().decode(zipedArrayToDecode));
	}

	/**
	 * Unzip byteArray by GZIP-Algorithm
	 * 
	 * @param zippedData
	 * @return unzippedData
	 * @throws Exception
	 */
	public static byte[] unzipByteArray(byte[] zippedData) throws Exception {
		try {
			ByteArrayOutputStream decoded = new ByteArrayOutputStream();
			ByteArrayInputStream encoded = new ByteArrayInputStream(zippedData);
			copy(new GZIPInputStream(encoded), decoded);
			return decoded.toByteArray();
		} catch (IOException e) {
			throw new Exception("Komprimierte Daten konnten nicht entpackt werden", e);
		}
	}

	/**
	 * Get the duration between two timestamps as a string
	 * 
	 * @param startTime
	 * @param endTime
	 * @return
	 */
	public static String getDuration(Calendar startTime, Calendar endTime) {
		int durationInMilliSeconds = (int)(endTime.getTimeInMillis() - startTime.getTimeInMillis());
		int milliSecondsPart = durationInMilliSeconds % 1000;
		int secondsPart = durationInMilliSeconds / 1000 % 60;
		int minutesPart = durationInMilliSeconds / 1000 / 60 % 60;
		int hoursPart = durationInMilliSeconds / 1000 / 60 / 60 % 24;
		int days = durationInMilliSeconds / 1000 / 60 / 60 % 24;

		String returnString = milliSecondsPart + "ms";
		if (secondsPart > 0) {
			returnString = secondsPart + "s " + returnString;
		}
		if (minutesPart > 0) {
			returnString = minutesPart + "m " + returnString;
		}
		if (hoursPart > 0) {
			returnString = hoursPart + "h " + returnString;
		}
		if (days > 0) {
			returnString = days + "d " + returnString;
		}
		return returnString;
	}

	/**
	 * Read a directory and return all files starting with a basename form a list of basenames
	 * 
	 * @param basenameList
	 * @return
	 */
	public static ArrayList<File> getArrayOfFilesByStammname(String basenameList) {
		return getArrayOfFilesByStammname(Arrays.asList(basenameList.split(";")));
	}

	/**
	 * Read a directory and return all files starting with a basename form a list of basenames
	 * 
	 * @param stammNamenListe
	 * @return
	 */
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
							&& (stammFileName.endsWith("*") && partList[i].getName().startsWith(stammFileName.substring(0, stammFileName.length() - 1)) || !stammFileName.endsWith("*")
									&& partList[i].getName().equals(stammFileName))) {
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

	/**
	 * Read a directory and return all files fitting to a regex pattern
	 * 
	 * @param startDirectory
	 * @param patternString
	 * @param traverseCompletely
	 * @return
	 */
	public static List<File> getFilesByPattern(File startDirectory, String patternString, boolean traverseCompletely) {
		return getFilesByPattern(startDirectory, Pattern.compile(patternString), traverseCompletely);
	}

	/**
	 * Read a directory and return all files fitting to a regex pattern
	 * 
	 * @param startDirectory
	 * @param pattern
	 * @param traverseCompletely
	 * @return
	 */
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

	/**
	 * Check a simple name string
	 * 
	 * @param value
	 * @return
	 */
	public static boolean checkForValidUserName(String value) {
		return value != null && value.matches("[A-Za-z0-9_-]*");
	}

	/**
	 * Convert an ArrayList of Strings to a StringArray
	 * 
	 * @param pArrayListOfStrings
	 * @return
	 */
	public static String[] convertArrayListOfStringsToStringArray(ArrayList<String> pArrayListOfStrings) {
		String[] arrayofStrings = new String[0];
		return pArrayListOfStrings.toArray(arrayofStrings);
	}

	/**
	 * Get index of an Integer within an Array of Integers
	 * 
	 * @param searchInt
	 * @param intArray
	 * @return
	 */
	public static int getIndex(int searchInt, int[] intArray) {
		for (int i = 0; i < intArray.length; i++) {
			if (intArray[i] == searchInt) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Read a stream into a string
	 */
	public static String inputStreamToString(InputStream in, String encoding) throws IOException {
		BufferedReader bufferedReader = null;
		try {
			bufferedReader = new BufferedReader(new InputStreamReader(in, encoding));
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
	 * Use a string as inputStream
	 * 
	 * @throws UnsupportedEncodingException
	 */
	public static InputStream stringToInputStream(String valueString) throws UnsupportedEncodingException {
		return new ByteArrayInputStream(valueString.getBytes("UTF-8"));
	}

	/**
	 * Read all stream data in byteArray
	 * 
	 * @param inputStream
	 * @return
	 * @throws Exception
	 */
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
			closeQuietly(returnArrayBuffer);
		}

		return returnArrayBuffer.toByteArray();
	}

	/**
	 * Read available stream data in byteArray
	 * 
	 * @param inputStream
	 * @return
	 * @throws Exception
	 */
	public static byte[] readAvailableDataFromStreamInArray(InputStream inputStream) throws Exception {
		byte[] returnData = new byte[inputStream.available()];
		@SuppressWarnings("unused")
		int bytesRead = inputStream.read(returnData);
		return returnData;
	}

	/**
	 * Read stream data in byteArray until next linefeed or stream end
	 * 
	 * @param inStream
	 * @return
	 * @throws IOException
	 */
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
	 * Get email from X509Certificate
	 * 
	 * @param cert
	 * @return
	 */
	public static String getEmailFromCertificate(X509Certificate cert) {
		String[] nameParts = cert.getSubjectX500Principal().toString().split(",");
		for (String namePart : nameParts) {
			if (namePart.matches("^[ \\t]*EMAILADDRESS=.*")) {
				return namePart.substring(namePart.indexOf("=") + 1).trim();
			}
		}

		return null;
	}

	/**
	 * Get cn from X509Certificate
	 * 
	 * @param cert
	 * @return
	 */
	public static String getCnFromCertificate(X509Certificate cert) {
		String[] nameParts = cert.getSubjectX500Principal().toString().split(",");
		for (String namePart : nameParts) {
			if (namePart.matches("^[ \\t]*CN=.*")) {
				return namePart.substring(namePart.indexOf("=") + 1).trim();
			}
		}

		return null;
	}

	/**
	 * Check if a day is included in a list of days
	 * 
	 * @param listOfDays
	 * @param day
	 * @return
	 */
	public static boolean dayListIncludes(List<GregorianCalendar> listOfDays, GregorianCalendar day) {
		for (GregorianCalendar listDay : listOfDays) {
			if (listDay.get(Calendar.DAY_OF_YEAR) == day.get(Calendar.DAY_OF_YEAR)) {
				return true;
			}
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

		if (timingString.equalsIgnoreCase("once")) {
			return null;
		}

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
					while (nextStartByThisParameter.before(now) && nextStartByThisParameter.before(returnStart) || dayListIncludes(excludedDays, nextStartByThisParameter)) {
						nextStartByThisParameter.add(GregorianCalendar.DAY_OF_MONTH, 1);
					}
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
							|| dayListIncludes(excludedDays, nextStartByThisParameter)) {
						nextStartByThisParameter.add(GregorianCalendar.DAY_OF_MONTH, 1);
					}
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
				while (nextStartByThisParameter.before(now) && nextStartByThisParameter.before(returnStart)) {
					nextStartByThisParameter.add(GregorianCalendar.MONTH, 1);
				}

				// Bei treffender Feiertagseintragung um einen Tag
				// weiterschieben
				while (dayListIncludes(excludedDays, nextStartByThisParameter)) {
					nextStartByThisParameter.add(GregorianCalendar.DAY_OF_YEAR, 1);
				}
			} else if (timingParameter.startsWith("Q:")) {
				// Quartalsweise Planung (Q:1200): Start am ersten Kalendertag
				// im Quartal
				if (nextStartByThisParameter.get(GregorianCalendar.MONTH) < GregorianCalendar.APRIL) {
					nextStartByThisParameter.set(GregorianCalendar.MONTH, GregorianCalendar.APRIL);
				} else if (nextStartByThisParameter.get(GregorianCalendar.MONTH) < GregorianCalendar.JULY) {
					nextStartByThisParameter.set(GregorianCalendar.MONTH, GregorianCalendar.JULY);
				} else if (nextStartByThisParameter.get(GregorianCalendar.MONTH) < GregorianCalendar.OCTOBER) {
					nextStartByThisParameter.set(GregorianCalendar.MONTH, GregorianCalendar.OCTOBER);
				} else {
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
				while (dayListIncludes(excludedDays, nextStartByThisParameter)) {
					nextStartByThisParameter.add(GregorianCalendar.DAY_OF_YEAR, 1);
				}
			} else if (timingParameter.startsWith("QW:")) {
				// Quartalsweise Planung (QW:1200): Start am ersten Werktag im
				// Quartal
				if (nextStartByThisParameter.get(GregorianCalendar.MONTH) < GregorianCalendar.APRIL) {
					nextStartByThisParameter.set(GregorianCalendar.MONTH, GregorianCalendar.APRIL);
				} else if (nextStartByThisParameter.get(GregorianCalendar.MONTH) < GregorianCalendar.JULY) {
					nextStartByThisParameter.set(GregorianCalendar.MONTH, GregorianCalendar.JULY);
				} else if (nextStartByThisParameter.get(GregorianCalendar.MONTH) < GregorianCalendar.OCTOBER) {
					nextStartByThisParameter.set(GregorianCalendar.MONTH, GregorianCalendar.OCTOBER);
				} else {
					nextStartByThisParameter.set(GregorianCalendar.MONTH, GregorianCalendar.JANUARY);
					nextStartByThisParameter.add(GregorianCalendar.YEAR, 1);
				}

				nextStartByThisParameter.set(GregorianCalendar.DAY_OF_MONTH, 1);

				// Bei treffender Feiertagseintragung oder Wochentagsregelung um
				// einen Tag weiterschieben
				while (nextStartByThisParameter.get(GregorianCalendar.DAY_OF_WEEK) == java.util.Calendar.SATURDAY
						|| nextStartByThisParameter.get(GregorianCalendar.DAY_OF_WEEK) == java.util.Calendar.SUNDAY || dayListIncludes(excludedDays, nextStartByThisParameter)) {
					nextStartByThisParameter.add(GregorianCalendar.DAY_OF_MONTH, 1);
				}

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
						|| dayListIncludes(excludedDays, nextStartByThisParameter)) {
					nextStartByThisParameter.add(GregorianCalendar.DAY_OF_MONTH, 1);
				}
			}

			if (nextStartByThisParameter.before(returnStart)) {
				returnStart = nextStartByThisParameter;
			}
		}

		return new Timestamp(returnStart.getTimeInMillis());
	}

	/**
	 * Parse a version string like 4.3.2
	 * 
	 * @param versionString
	 * @return
	 */
	public static int[] parseVersionNumber(String versionString) {
		try {
			Pattern versionPattern = Pattern.compile("^(\\d+)(?:\\.(\\d+)){0,1}(?:\\.(\\d+)){0,1}$");
			Matcher matcher = versionPattern.matcher(versionString);
			if (!matcher.find() || matcher.groupCount() < 1 || matcher.groupCount() > 3) {
				throw new IllegalArgumentException("Version must be in form <major>[.<minor>[.<micro>]]");
			}

			int major = new Integer(matcher.group(1));
			int minor = 0;
			if (matcher.groupCount() >= 2 && matcher.group(2) != null) {
				minor = new Integer(matcher.group(2));
			}
			int micro = 0;
			if (matcher.groupCount() == 3 && matcher.group(3) != null) {
				micro = new Integer(matcher.group(3));
			}

			return new int[] { major, minor, micro };
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Version must contain integervalues only");
		}
	}

	/**
	 * Check for a number
	 * 
	 * @param value
	 * @return
	 */
	public static boolean isNumber(String value) {
		try {
			Integer.parseInt(value);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	/**
	 * Remove the time part of a GregorianCalendar
	 * 
	 * @param value
	 * @return
	 */
	public static GregorianCalendar getDayWithoutTime(GregorianCalendar value) {
		return new GregorianCalendar(value.get(GregorianCalendar.YEAR), value.get(GregorianCalendar.MONTH), value.get(GregorianCalendar.DAY_OF_MONTH));
	}

	/***
	 * Split a list into smaller lists to a maximum chunkSize
	 * 
	 * @param originalList
	 * @param chunkSize
	 * @return
	 */
	public static <E> List<List<E>> chopListToChunks(List<E> originalList, int chunkSize) {
		if (originalList == null || originalList.size() <= 0 || chunkSize <= 0) {
			return null;
		}

		List<List<E>> returnList = new ArrayList<List<E>>();
		int endIndex = 0;

		while (endIndex < originalList.size()) {
			int startIndex = endIndex;
			if (chunkSize < originalList.size() - endIndex) {
				endIndex += chunkSize;
			} else {
				endIndex = originalList.size();
			}

			returnList.add(originalList.subList(startIndex, endIndex));
		}

		return returnList;
	}

	/**
	 * List of characters for randomization
	 */
	private static final char[] randomCharacters = "ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÜabcdefghijklmnopqrstuvwxyzäöüß".toCharArray();

	/**
	 * List of numbers and strings for randomization
	 */
	private static final char[] randomAlphaNumericCharacters = (new String(randomCharacters) + "0123456789").toCharArray();

	/**
	 * Random generator
	 */
	private static final Random random = new SecureRandom();

	/**
	 * Generate a random number up to maximum value
	 * 
	 * @param excludedMaximum
	 * @return
	 */
	public static int getRandomNumber(int excludedMaximum) {
		return random.nextInt(excludedMaximum);
	}

	/**
	 * Generate a random string of given size
	 * 
	 * @param length
	 * @return
	 */
	public static String getRandomString(int length) {
		StringBuilder sb = new StringBuilder(length);
		for (int i = 0; i < length; i++) {
			sb.append(randomCharacters[random.nextInt(randomCharacters.length)]);
		}
		return sb.toString();
	}

	/**
	 * Generate a random string of numbers and characters of given size
	 * 
	 * @param length
	 * @return
	 */
	public static String getRandomAlphanumericString(int length) {
		StringBuilder sb = new StringBuilder(length);
		for (int i = 0; i < length; i++) {
			sb.append(randomAlphaNumericCharacters[random.nextInt(randomAlphaNumericCharacters.length)]);
		}
		return sb.toString();
	}

	/**
	 * Generate a random number of given size
	 * 
	 * @param length
	 * @return
	 */
	public static String getRandomNumberString(int length) {
		StringBuilder sb = new StringBuilder(length);
		for (int i = 0; i < length; i++) {
			sb.append(random.nextInt(10));
		}
		return sb.toString();
	}

	/**
	 * Generate a random byte
	 * 
	 * @return
	 */
	public static byte getRandomByte() {
		byte[] result = new byte[1];
		random.nextBytes(result);
		return result[0];
	}

	/**
	 * Generate a random byteArray
	 * 
	 * @param arrayToFill
	 * @return
	 */
	public static byte[] getRandomByteArray(byte[] arrayToFill) {
		random.nextBytes(arrayToFill);
		return arrayToFill;
	}

	/**
	 * Convert a collection to a string like Utilities.join, but keep null values as "<null>" string
	 * 
	 * @param collection
	 * @param separator
	 * @return
	 */
	public static String convertCollectionToString(Collection<?> collection, String separator) {
		StringBuilder builder = new StringBuilder();

		for (Object object : collection) {
			if (builder.length() > 0) {
				builder.append(separator);
			}
			if (object == null) {
				builder.append("<null>");
			} else {
				builder.append(object.toString());
			}
		}

		return builder.toString();
	}

	/**
	 * Check if a Integer is contained by an interval definition
	 * Interval definitions like -1;2-5;8+
	 * 
	 * @param intervals
	 * @param item
	 * @return
	 */
	public static boolean checkForIntervalContainment(String intervals, int item) {
		if (intervals != null && intervals.length() > 0) {
			String[] blockStrings = intervals.split(";");
			for (int i = 0; i < blockStrings.length; i++) {
				if (blockStrings[i].endsWith("+")) {
					if (Integer.parseInt(blockStrings[i].substring(0, blockStrings[i].length() - 1)) <= item) {
						return true;
					}
				} else if (blockStrings[i].matches("\\d+-\\d+")) {
					int plusIndex = blockStrings[i].indexOf("-");
					int startVersion = Integer.parseInt(blockStrings[i].substring(0, plusIndex));
					int endeVersion = Integer.parseInt(blockStrings[i].substring(plusIndex + 1));
					if (startVersion <= item && endeVersion >= item) {
						return true;
					}
				} else if (blockStrings[i].matches("-\\d+")) {
					int endeVersion = Integer.parseInt(blockStrings[i].substring(1));
					if (endeVersion >= item) {
						return true;
					}
				} else {
					if (Integer.parseInt(blockStrings[i]) == item) {
						return true;
					}
				}
			}
		}

		return false;
	}

	/**
	 * Get all recursive subfiles of a directory
	 * 
	 * @param directory
	 * @return
	 * @throws Exception
	 */
	public static List<File> getAllSubfiles(File directory) throws Exception {
		try {
			List<File> files = new LinkedList<File>();
			File[] subFiles = directory.listFiles();
			if (subFiles != null) {
				for (File file : subFiles) {
					if (file.isDirectory()) {
						files.addAll(getAllSubfiles(file));
					} else {
						files.add(file);
					}
				}
			}
			return files;
		} catch (DoNotWrapException e) {
			throw e;
		} catch (Exception e) {
			throw new DoNotWrapException("Error reading subfiles of: " + directory.getAbsolutePath(), e);
		}
	}

	/**
	 * Get number of all recursive subfiles of a directory
	 * 
	 * @param directory
	 * @return
	 * @throws Exception
	 */
	public static int getAllSubfilesNumber(File directory) throws Exception {
		try {
			int fileCount = 0;
			File[] subFiles = directory.listFiles();
			if (subFiles != null) {
				for (File file : subFiles) {
					if (file.isDirectory()) {
						fileCount += getAllSubfilesNumber(file);
					} else {
						fileCount++;
					}
				}
			}
			return fileCount;
		} catch (DoNotWrapException e) {
			throw e;
		} catch (Exception e) {
			throw new DoNotWrapException("Error reading subfilenumber of: " + directory.getAbsolutePath(), e);
		}
	}

	/**
	 * Get size in bytes of all recursive subfiles of a directory
	 * 
	 * @param directory
	 * @return
	 * @throws Exception
	 */
	public static long getAllSubfilesSize(File directory) throws Exception {
		try {
			long fileSizeSum = 0;
			File[] subFiles = directory.listFiles();
			if (subFiles != null) {
				for (File file : subFiles) {
					if (file.isDirectory()) {
						fileSizeSum += getAllSubfilesSize(file);
					} else {
						fileSizeSum += file.length();
					}
				}
			}
			return fileSizeSum;
		} catch (DoNotWrapException e) {
			throw e;
		} catch (Exception e) {
			throw new DoNotWrapException("Error reading subfilesize of: " + directory.getAbsolutePath(), e);
		}
	}

	/**
	 * Get the minimum of a value list down to a valid minimum
	 * 
	 * @param allowedValueMinimum
	 * @param values
	 * @return
	 */
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

	/**
	 * Convert a string to boolean
	 * 
	 * @param value
	 * @return
	 */
	public static boolean interpretAsBool(String value) {
		if (isNotEmpty(value)) {
			value = value.trim();
			return value.equalsIgnoreCase("true") || value.equalsIgnoreCase("+") || value.equalsIgnoreCase("yes") || value.equalsIgnoreCase("ja") || value.equalsIgnoreCase("ok") || value.equalsIgnoreCase("on") || value.equalsIgnoreCase("an");
		} else {
			return false;
		}
	}

	/**
	 * Check if any characters in a list are equal
	 * 
	 * @param values
	 * @return
	 */
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

	/**
	 * Math.square
	 * 
	 * @param value
	 * @return
	 */
	public static int square(int value) {
		return value * value;
	}

	/**
	 * Math power
	 * 
	 * @param base
	 * @param exp
	 * @return
	 */
	public static int pow(int base, int exp) {
		if (exp < 0) {
			throw new IllegalArgumentException("Invalid negative exponent");
		} else if (exp == 0) {
			return 1;
		} else {
			return square(pow(base, exp / 2)) * (exp % 2 == 1 ? base : 1);
		}
	}
	
	/**
	 * Get number of lines of a textfile
	 * 
	 * @param file
	 * @return
	 * @throws IOException
	 */
	public static int getLineCount(File file) throws IOException {
		LineNumberReader lineNumberReader = null;
		try {
			lineNumberReader = new LineNumberReader(new InputStreamReader(new FileInputStream(file)));
			while (lineNumberReader.readLine() != null) {
				// do nothing
			}

			return lineNumberReader.getLineNumber();
		} finally {
			closeQuietly(lineNumberReader);
		}
	}

	/**
	 * Get a collection like a set as a ordered list
	 * @param c
	 * @return
	 */
	public static <T extends Comparable<? super T>> List<T> asSortedList(Collection<T> c) {
		List<T> list = new ArrayList<T>(c);
		java.util.Collections.sort(list);
		return list;
	}

	/**
	 * Get files of classpath
	 * 
	 * @return
	 */
	public static String getClassPath() {
		ClassLoader sysClassLoader = ClassLoader.getSystemClassLoader();
		URL[] urls = ((URLClassLoader)sysClassLoader).getURLs();

		StringBuilder classpath = new StringBuilder();
		for (int i = 0; i < urls.length; i++) {
			classpath.append(urls[i].getFile() + "\n");
		}
		return classpath.toString();
	}

	/**
	 * Check bytearrays equality
	 * 
	 * @param byteArray1
	 * @param byteArray2
	 * @return
	 */
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

	/**
	 * Get all system properties
	 * 
	 * @return
	 */
	public static Map<String, String> getSystemPropertiesMap() {
		Map<String, String> propertiesMap = new HashMap<String, String>();
		for (Object key : System.getProperties().keySet()) {
			propertiesMap.put((String)key, System.getProperties().getProperty((String)key));
		}
		return propertiesMap;
	}

	/**
	 * Convert Map to String
	 * 
	 * @param map
	 * @param entrySeparator
	 * @param keySeparator
	 * @param sort
	 * @return
	 */
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

	/**
	 * Make a number human readable
	 * 
	 * @param value
	 * @return
	 */
	public static String getHumanReadableNumber(Number value) {
		return getHumanReadableNumber(value, null, true);
	}

	/**
	 * Make a number with unitsign human readable
	 * 
	 * @param value
	 * @param unitTypeSign
	 * @return
	 */
	public static String getHumanReadableNumber(Number value, String unitTypeSign) {
		return getHumanReadableNumber(value, unitTypeSign, true);
	}

	/**
	 * Make a number with unitsign human readable
	 * 
	 * @param value
	 * @param unitTypeSign
	 * @param siUnits
	 * @return
	 */
	public static String getHumanReadableNumber(Number value, String unitTypeSign, boolean siUnits) {
		int unit = siUnits ? 1000 : 1024;
		double interimValue = value.doubleValue();
		String unitExtension = "";
		if (interimValue < unit) {
			if (isNotBlank(unitTypeSign)) {
				unitExtension = " " + unitTypeSign;
			}

			if (value instanceof Integer || value instanceof Long) {
				return value + unitExtension;
			}
		} else {
			int exp = (int)(Math.log(interimValue) / Math.log(unit));
			unitExtension = " " + (siUnits ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (siUnits ? "" : "i");
			if (isNotBlank(unitTypeSign)) {
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

	/**
	 * Generate MD5 from string data
	 * 
	 * @param data
	 * @return
	 * @throws Exception
	 */
	public static byte[] getMD5Hash(String data) throws Exception {
		try {
			return MessageDigest.getInstance("MD5").digest(data.getBytes("UTF-8"));
		} catch (Exception e) {
			throw new Exception("Error while MD5 hashing", e);
		}
	}

	/**
	 * Generate SHA-1 from string data
	 * 
	 * @param data
	 * @return
	 * @throws Exception
	 */
	public static byte[] getSHA1Hash(String data) throws Exception {
		try {
			return MessageDigest.getInstance("SHA-1").digest(data.getBytes("UTF-8"));
		} catch (Exception e) {
			throw new Exception("Error while SHA-1 hashing", e);
		}
	}

	/**
	 * Generate SHA-512 from string data
	 * 
	 * @param data
	 * @return
	 * @throws Exception
	 */
	public static byte[] getSHA512Hash(String data) throws Exception {
		try {
			return MessageDigest.getInstance("SHA-512").digest(data.getBytes("UTF-8"));
		} catch (Exception e) {
			throw new Exception("Error while SHA-512 hashing", e);
		}
	}

	/**
	 * Get bytearray for list of bytes
	 * 
	 * @param data
	 * @return
	 */
	public static byte[] getByteArray(List<Byte> data) {
		byte[] returnArray = new byte[data.size()];
		for (int i = 0; i < data.size(); i++) {
			returnArray[i] = data.get(i);
		}
		return returnArray;
	}

	/**
	 * Get stacktrace as string
	 * 
	 * @param stackTrace
	 * @return
	 */
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

	/**
	 * Download a file from url
	 * 
	 * @param url
	 * @param localeDestionationPath
	 * @throws Exception
	 */
	public static void downloadFile(String url, String localeDestionationPath) throws Exception {
		BufferedInputStream bufferedInputStream = null;
		FileOutputStream fileOutputStream = null;
		try {
			bufferedInputStream = new BufferedInputStream(new URL(url).openStream());
			fileOutputStream = new FileOutputStream(localeDestionationPath);
			Utilities.copy(bufferedInputStream, fileOutputStream);
		} catch (Exception e) {
			throw new Exception("Cannot download file", e);
		} finally {
			closeQuietly(fileOutputStream);
			closeQuietly(bufferedInputStream);
		}
	}

	/**
	 * Check array for duplicate strings
	 * 
	 * @param inputArray
	 * @param ignoreNullValues
	 * @return
	 */
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

	/**
	 * Filter all Objects of given class
	 * 
	 * @param collection
	 * @param classToSelect
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> List<T> selectItems(Collection<?> collection, Class<T> classToSelect) {
		List<T> list = new ArrayList<T>();
		for (Object item : collection) {
			if (classToSelect.isInstance(item)) {
				list.add((T)item);
			}
		}
		return list;
	}

	/**
	 * Filter all Objects of given class
	 * 
	 * @param array
	 * @param classToSelect
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> List<T> selectItems(Object[] array, Class<T> classToSelect) {
		List<T> list = new ArrayList<T>();
		for (Object item : array) {
			if (classToSelect.isInstance(item)) {
				list.add((T)item);
			}
		}
		return list;
	}
	
	public static int[] revertArray(int[] array) {
		int[] returnValue = new int[array.length];
		for (int i = 0; i < array.length; i++) {
			returnValue[i] = array[array.length - 1 - i];
		}
		return returnValue;
	}

	public static String getDomainFromUrl(String url) throws Exception {
		URI uri = new URI(url);
	    String domain = uri.getHost();
	    return domain.startsWith("www.") ? domain.substring(4) : domain;
	}
	
	public static boolean isEmpty(String value) {
		return value == null || value.length() == 0;
	}
	
	public static boolean isNotEmpty(String value) {
		return !isEmpty(value);
	}
	
	public static boolean isBlank(String value) {
		return value == null || value.length() == 0 || value.trim().length() == 0;
	}
	
	public static boolean isNotBlank(String value) {
		return !isBlank(value);
	}
	
	public static void closeQuietly(Closeable closeable) {
		if (closeable != null) {
			try {
				closeable.close();
			} catch (IOException e) {
				// Do nothing
			}
		}
	}

	public static String repeat(String value, int count) {
		if (value == null) {
			return null;
		} else if (value.length() == 0 || count == 0) {
			return "";
		} else {
			StringBuilder returnValue = new StringBuilder();
			for (int i = 0; i < count; i++) {
				returnValue.append(value);
			}
			return returnValue.toString();
		}
	}

	public static String join(Object[] array, String glue) {
		if (array == null) {
			return null;
		} else if (array.length == 0) {
			return "";
		} else {
			if (glue == null) {
				glue = "";
			}
			
			StringBuilder returnValue = new StringBuilder();
			boolean isFirst = true;
			for (Object object : array) {
				if (isFirst) {
					returnValue.append(glue);
				}
				if (object == null) {
					object = "";
				}
				returnValue.append(object.toString());
				isFirst = false;
			}
			return returnValue.toString();
		}
	}

	@SuppressWarnings("unchecked")
	public static <T> T[] remove(T[] items, T item) {
		int foundAtIndex = -1;
		for (int i = 0; i < items.length && foundAtIndex == -1; i++) {
			if (items[i] == item) {
				foundAtIndex = i;
			}
		}
		
		if (foundAtIndex > -1) {
			Object[] returnArray = new Object[items.length - 1];
			for (int i = 0; i < foundAtIndex; i++) {
				if (items[i] == item) {
					foundAtIndex = i;
				}
			}
			for (int i = foundAtIndex + 1; i < items.length; i++) {
				if (items[i] == item) {
					foundAtIndex = i;
				}
			}
			return (T[]) returnArray;
		} else {
			return items;
		}
	}

	public static byte[] readFileToByteArray(File file) throws FileNotFoundException, IOException {
		try (FileInputStream in = new FileInputStream(file)) {
			byte[] returnArray = new byte[(int) file.length()];
			in.read(returnArray);
			return returnArray;
		}
	}

	private static void copy(InputStream inputStream, OutputStream outputStream) throws IOException {
		byte[] buffer = new byte[4096];
		int lengthRead = -1;
		while ((lengthRead = inputStream.read(buffer)) > -1) {
			outputStream.write(buffer, 0, lengthRead);
		}
		outputStream.flush();
	}

	public static byte[] toByteArray(InputStream inputStream) throws IOException {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		copy(inputStream, byteArrayOutputStream);
		byteArrayOutputStream.close();
		return byteArrayOutputStream.toByteArray();
	}
}