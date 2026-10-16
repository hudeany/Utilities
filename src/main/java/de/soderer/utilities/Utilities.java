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
import java.io.Reader;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.net.Proxy;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;


/**
 * Global Utilities
 *
 * <p>This class does no Logging via Log4J, because it is often used before its initialisation</p>
 */
public class Utilities {
	private Utilities() {
		// Utility class
	}

	/**
	 * Return value of stream read methods signaling the end of the stream
	 */
	public static final int EOF = -1;

	/**
	 * Template of a minimal XML document, "&lt;encoding&gt;" is a placeholder for the encoding name
	 */
	public static final String STANDARD_XML = "<?xml version=\"1.0\" encoding=\"<encoding>\" standalone=\"yes\"?>\n<root>\n</root>\n";
	/**
	 * Template of a minimal XHTML document, "&lt;encoding&gt;" is a placeholder for the charset name
	 */
	public static final String STANDARD_HTML = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n<html xmlns=\"http://www.w3.org/1999/xhtml\">\n\t<head>\n\t\t<meta http-equiv=\"Content-Type\" content=\"text/html; charset=<encoding>\" />\n\t\t<title>HtmlTitle</title>\n\t\t<meta name=\"Title\" content=\"HtmlTitle\" />\n\t</head>\n\t<body>\n\t</body>\n</html>\n";
	/**
	 * Shebang line starting a bash script
	 */
	public static final String STANDARD_BASHSCRIPTSTART = "#!/bin/bash\n";
	/**
	 * Sample JSON document with all simple JSON data types
	 */
	public static final String STANDARD_JSON = "{\n\t\"property1\": null,\n\t\"property2\": " + Math.PI + ",\n\t\"property3\": true,\n\t\"property4\": \"Text\",\n\t\"property5\": [\n\t\tnull,\n\t\t" + Math.PI + ",\n\t\ttrue,\n\t\t\"Text\"\n\t]\n}\n";
	/**
	 * Sample YAML document with all simple data types, equivalent to {@link #STANDARD_JSON}
	 */
	public static final String STANDARD_YAML = "property1: null\nproperty2: " + Math.PI + "\nproperty3: true\nproperty4: Text\nproperty5:\n  - null\n  - " + Math.PI + "\n  - true\n  - Text\n";

	/**
	 * Generate a unique ID
	 *
	 * @return random UUID in upper case without hyphens (32 hex characters)
	 */
	public static String generateUUID() {
		return UUID.randomUUID().toString().toUpperCase(Locale.ROOT).replace("-", "");
	}

	/**
	 * Get a UUID from a string
	 *
	 * @param value
	 *            UUID string without hyphens (32 hex characters)
	 * @return UUID
	 */
	public static UUID getUUIDFromString(final String value) {
		final StringBuilder uuidString = new StringBuilder(value);
		uuidString.insert(20, '-');
		uuidString.insert(16, '-');
		uuidString.insert(12, '-');
		uuidString.insert(8, '-');
		return UUID.fromString(uuidString.toString());
	}

	/**
	 * Get the data of a file included in a jar file
	 *
	 * @param resourceName
	 *            path of the resource within the classpath, without leading slash
	 * @return stream of the resource data, or null if the resource does not exist
	 */
	public static InputStream getResourceAsStream(final String resourceName) {
		return Utilities.class.getResourceAsStream("/" + resourceName);
	}

	/**
	 * Zip a byteArray by GZIP-Algorithm
	 *
	 * @param clearData
	 *            data to compress
	 * @return compressed data, or null on error
	 */
	public static byte[] gzipByteArray(final byte[] clearData) {
		try (ByteArrayOutputStream encoded = new ByteArrayOutputStream();
				GZIPOutputStream gzipCompresser = new GZIPOutputStream(encoded)) {
			gzipCompresser.write(clearData);
			// finish() writes the remaining compressed data and the GZIP trailer, closing is done by try-with-resources
			gzipCompresser.finish();
			return encoded.toByteArray();
		} catch (@SuppressWarnings("unused") final IOException e) {
			return null;
		}
	}

	/**
	 * Unzip a byteArray by GZIP-Algorithm
	 *
	 * @param zippedData
	 *            GZIP compressed data
	 * @return uncompressed data, or null if the data cannot be read
	 * @throws Exception
	 *             if decompressing fails with an error other than an IOException
	 */
	public static byte[] gunzipByteArray(final byte[] zippedData) throws Exception {
		try {
			final ByteArrayOutputStream decoded = new ByteArrayOutputStream();
			// Closing the GZIPInputStream releases the native memory of its Inflater immediately
			try (GZIPInputStream gzipInputStream = new GZIPInputStream(new ByteArrayInputStream(zippedData))) {
				IoUtilities.copy(gzipInputStream, decoded);
			}
			return decoded.toByteArray();
		} catch (@SuppressWarnings("unused") final IOException e) {
			return null;
		}
	}

	/**
	 * Encode data as Base64 String
	 *
	 * @param clearData
	 *            data to encode
	 * @return Base64 string
	 */
	public static String encodeBase64(final byte[] clearData) {
		return Base64.getEncoder().encodeToString(clearData);
	}

	/**
	 * Encode data as Base64 String split into lines
	 *
	 * @param clearData
	 *            data to encode
	 * @param maxCharactersPerLine
	 *            maximum number of characters per line
	 * @param splitCharacters
	 *            characters appended after every full line, e.g. a linebreak
	 * @return Base64 string split into lines
	 */
	public static String encodeBase64(final byte[] clearData, final int maxCharactersPerLine, final String splitCharacters) {
		final String dataBase64 = Base64.getEncoder().encodeToString(clearData);
		final StringBuilder returnString = new StringBuilder();
		final int fullLines = (int) Math.floor(dataBase64.length() / maxCharactersPerLine);
		for (int i = 0; i < fullLines; i++) {
			returnString.append(dataBase64.substring(i * maxCharactersPerLine, (i * maxCharactersPerLine) + maxCharactersPerLine));
			returnString.append(splitCharacters);
		}
		returnString.append(dataBase64.substring(fullLines * maxCharactersPerLine, (fullLines * maxCharactersPerLine) + (dataBase64.length() % maxCharactersPerLine)));
		return returnString.toString();
	}

	/**
	 * Decode a Base64 String
	 *
	 * @param base64String
	 *            Base64 string, whitespace and linebreaks are ignored
	 * @return decoded data
	 */
	public static byte[] decodeBase64(final String base64String) {
		return Base64.getDecoder().decode(base64String.replace("\r", "").replace("\n", "").replace("\t", "").replace(" ", "").getBytes(StandardCharsets.UTF_8));
	}

	/**
	 * Check a simple name string (only letters A-Z and a-z, digits, '_' and '-')
	 *
	 * @param value
	 *            name to check
	 * @return true if the name is valid, false for null
	 */
	public static boolean checkForValidUserName(final String value) {
		return value != null && value.matches("[A-Za-z0-9_-]*");
	}

	/**
	 * Convert an ArrayList of Strings to a StringArray
	 *
	 * @param pArrayListOfStrings
	 *            list to convert
	 * @return array with the strings of the list
	 */
	public static String[] convertArrayListOfStringsToStringArray(final ArrayList<String> pArrayListOfStrings) {
		final String[] arrayofStrings = new String[0];
		return pArrayListOfStrings.toArray(arrayofStrings);
	}

	/**
	 * Get index of an Integer within an Array of Integers
	 *
	 * @param searchInt
	 *            value to search for
	 * @param intArray
	 *            array to search in
	 * @return index of the first occurrence, or -1 if not found
	 */
	public static int getIndex(final int searchInt, final int[] intArray) {
		for (int i = 0; i < intArray.length; i++) {
			if (intArray[i] == searchInt) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Read stream data in byteArray until next linefeed or stream end
	 *
	 * @param inStream
	 *            stream to read from
	 * @return read data, including the linefeed if one was found
	 * @throws IOException
	 *             if reading fails
	 */
	public static byte[] readStreamUntilEndOrLinefeed(final InputStream inStream) throws IOException {
		final ByteArrayOutputStream returnData = new ByteArrayOutputStream();
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
	 *            certificate
	 * @return email address of the certificate subject, or null if not available
	 */
	public static String getEmailFromCertificate(final X509Certificate cert) {
		return getSubjectAttribute(cert, "EMAILADDRESS");
	}

	/**
	 * Get cn from X509Certificate
	 *
	 * @param cert
	 *            certificate
	 * @return common name (CN) of the certificate subject, or null if not available
	 */
	public static String getCnFromCertificate(final X509Certificate cert) {
		return getSubjectAttribute(cert, "CN");
	}

	/**
	 * Returns an attribute of the certificate subject. The distinguished name is parsed properly, so values containing
	 * commas (quoted like CN="Doe, John" or escaped like CN=Doe\, John) are returned completely and unquoted.
	 */
	private static String getSubjectAttribute(final X509Certificate cert, final String attributeType) {
		try {
			for (final Rdn rdn : new LdapName(cert.getSubjectX500Principal().toString()).getRdns()) {
				if (attributeType.equalsIgnoreCase(rdn.getType())) {
					return rdn.getValue().toString().trim();
				}
			}
			return null;
		} catch (@SuppressWarnings("unused") final InvalidNameException e) {
			return null;
		}
	}

	/**
	 * Split a list into smaller lists to a maximum chunkSize
	 *
	 * @param <E> type of the list items
	 * @param originalList list to split
	 * @param chunkSize maximum size of each chunk
	 * @return list of sublists (views on the original list), or null for an empty list or a chunk size less than 1
	 */
	public static <E> List<List<E>> chopListToChunks(final List<E> originalList, final int chunkSize) {
		if (originalList == null || originalList.size() <= 0 || chunkSize <= 0) {
			return null;
		}

		final List<List<E>> returnList = new ArrayList<>();
		int endIndex = 0;

		while (endIndex < originalList.size()) {
			final int startIndex = endIndex;
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
	 *            upper bound (exclusive)
	 * @return random number from 0 to excludedMaximum - 1
	 */
	public static int getRandomNumber(final int excludedMaximum) {
		return random.nextInt(excludedMaximum);
	}

	/**
	 * Generate a random string of given size
	 *
	 * @param length
	 *            length of the string
	 * @return random string of letters (including german umlauts)
	 */
	public static String getRandomString(final int length) {
		final StringBuilder sb = new StringBuilder(length);
		for (int i = 0; i < length; i++) {
			sb.append(randomCharacters[random.nextInt(randomCharacters.length)]);
		}
		return sb.toString();
	}

	/**
	 * Generate a random string of numbers and characters of given size
	 *
	 * @param length
	 *            length of the string
	 * @return random string of letters (including german umlauts) and digits
	 */
	public static String getRandomAlphanumericString(final int length) {
		final StringBuilder sb = new StringBuilder(length);
		for (int i = 0; i < length; i++) {
			sb.append(randomAlphaNumericCharacters[random.nextInt(randomAlphaNumericCharacters.length)]);
		}
		return sb.toString();
	}

	/**
	 * Generate a random string of digits of given size
	 *
	 * @param length
	 *            number of digits
	 * @return random string of digits
	 */
	public static String getRandomNumberString(final int length) {
		final StringBuilder sb = new StringBuilder(length);
		for (int i = 0; i < length; i++) {
			sb.append(random.nextInt(10));
		}
		return sb.toString();
	}

	/**
	 * Generate a random byte
	 *
	 * @return random byte
	 */
	public static byte getRandomByte() {
		final byte[] result = new byte[1];
		random.nextBytes(result);
		return result[0];
	}

	/**
	 * Generate a random byteArray
	 *
	 * @param arrayToFill
	 *            array to fill with random bytes
	 * @return the given array, filled with random bytes
	 */
	public static byte[] getRandomByteArray(final byte[] arrayToFill) {
		random.nextBytes(arrayToFill);
		return arrayToFill;
	}

	/**
	 * Check if an Integer is contained by an interval definition like {@code -1;2-5;8+}
	 *
	 * @param intervals
	 *            interval definitions separated by ';': a single value, a range "a-b", "-b" for up to b or "a+" for a and above
	 * @param item
	 *            value to check
	 * @return true if the value is contained in at least one interval
	 */
	public static boolean checkForIntervalContainment(final String intervals, final int item) {
		if (intervals != null && intervals.length() > 0) {
			final String[] blockStrings = intervals.split(";");
			for (final String blockString : blockStrings) {
				if (blockString.endsWith("+")) {
					if (Integer.parseInt(blockString.substring(0, blockString.length() - 1)) <= item) {
						return true;
					}
				} else if (blockString.matches("\\d+-\\d+")) {
					final int plusIndex = blockString.indexOf("-");
					final int startVersion = Integer.parseInt(blockString.substring(0, plusIndex));
					final int endeVersion = Integer.parseInt(blockString.substring(plusIndex + 1));
					if (startVersion <= item && endeVersion >= item) {
						return true;
					}
				} else if (blockString.matches("-\\d+")) {
					final int endeVersion = Integer.parseInt(blockString.substring(1));
					if (endeVersion >= item) {
						return true;
					}
				} else {
					if (Integer.parseInt(blockString) == item) {
						return true;
					}
				}
			}
		}

		return false;
	}

	/**
	 * Get the minimum of a value list down to a valid minimum
	 *
	 * @param allowedValueMinimum
	 *            values below this minimum are ignored
	 * @param values
	 *            values to check
	 * @return smallest allowed value, or Integer.MAX_VALUE if there is none
	 */
	public static int getMinimumOfAllowedValues(final int allowedValueMinimum, final int... values) {
		int returnValue = Integer.MAX_VALUE;
		if (values != null) {
			for (final int value : values) {
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
	 *            string to interpret
	 * @return true for "true", "+", "yes", "y", "ja", "j", "ok", "on" or "an" (case insensitive), otherwise false
	 */
	public static boolean interpretAsBool(String value) {
		if (isNotEmpty(value)) {
			value = value.trim();
			return "true".equalsIgnoreCase(value)
					|| "+".equalsIgnoreCase(value)
					|| "yes".equalsIgnoreCase(value)
					|| "y".equalsIgnoreCase(value)
					|| "ja".equalsIgnoreCase(value)
					|| "j".equalsIgnoreCase(value)
					|| "ok".equalsIgnoreCase(value)
					|| "on".equalsIgnoreCase(value)
					|| "an".equalsIgnoreCase(value);
		} else {
			return false;
		}
	}

	/**
	 * Check if any characters in a list are equal
	 *
	 * @param values
	 *            characters to check
	 * @return true if at least two characters are equal
	 */
	public static boolean anyCharsAreEqual(final char... values) {
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
	 * Check whether a character array contains a character.
	 *
	 * @param characterArray characters to search in, may be null
	 * @param searchCharacter character to search for, may be null
	 * @return true if the character is contained, false if any parameter is null
	 */
	public static boolean contains(final char[] characterArray, final Character searchCharacter) {
		if (characterArray == null || searchCharacter == null) {
			return false;
		}

		for (final char character : characterArray) {
			if (character == searchCharacter) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Math.square
	 *
	 * @param value
	 *            value to square
	 * @return value * value
	 */
	public static int square(final int value) {
		return value * value;
	}

	/**
	 * Math power
	 *
	 * @param base
	 *            base
	 * @param exp
	 *            exponent, must not be negative
	 * @return base to the power of exp
	 * @throws IllegalArgumentException
	 *             if the exponent is negative
	 */
	public static int pow(final int base, final int exp) {
		if (exp < 0) {
			throw new IllegalArgumentException("Invalid negative exponent");
		} else if (exp == 0) {
			return 1;
		} else {
			return square(pow(base, exp / 2)) * (exp % 2 == 1 ? base : 1);
		}
	}

	/**
	 * Get a collection like a set as a ordered list
	 *
	 * @param <T> type of the items
	 * @param c collection to sort, must not contain null items
	 * @return new list with the items in natural order
	 */
	public static <T extends Comparable<? super T>> List<T> asSortedList(final Collection<T> c) {
		final List<T> list = new ArrayList<>(c);
		Collections.sort(list);
		return list;
	}

	/**
	 * Sort a collection in natural order, but put the given items first in their given order
	 *
	 * @param <T> type of the items
	 * @param collection items to sort, must not contain null items
	 * @param firstItems items to put first, in this order
	 * @return new sorted list
	 */
	@SafeVarargs
	public static <T extends Comparable<? super T>> List<T> sortButPutItemsFirst(final Collection<T> collection, final T... firstItems) {
		final List<T> firstItemsList = new ArrayList<>(Arrays.asList(firstItems));
		final List<T> list = new ArrayList<>(collection);
		Collections.sort(list, new Comparator<T>() {
			@Override
			public int compare(final T o1, final T o2) {
				if (o1.equals(o2)) {
					return 0;
				} else if (firstItemsList.contains(o1)) {
					if (firstItemsList.contains(o2)) {
						return firstItemsList.indexOf(o1) < firstItemsList.indexOf(o2) ? -1 : 1;
					} else {
						return -1;
					}
				} else if (firstItemsList.contains(o2)) {
					return 1;
				} else {
					return o1.compareTo(o2);
				}
			}
		});
		return list;
	}

	/**
	 * Sort a map by a Comparator for the keytype
	 *
	 * @param <Key> type of the keys
	 * @param <Value> type of the values
	 * @param mapToSort map to sort
	 * @param comparator comparator for the keys
	 * @return new map with the entries in sorted key order
	 */
	public static <Key, Value> Map<Key, Value> sortMap(final Map<Key, Value> mapToSort, final Comparator<Key> comparator) {
		final List<Key> keys = new ArrayList<>(mapToSort.keySet());
		Collections.sort(keys, comparator);
		final LinkedHashMap<Key, Value> sortedContent = new LinkedHashMap<>();
		for (final Key key : keys) {
			sortedContent.put(key, mapToSort.get(key));
		}
		return sortedContent;
	}

	/**
	 * Sort a map by the String keytype
	 *
	 * @param <Value> type of the values
	 * @param mapToSort map to sort, must not contain a null key
	 * @return new map with the entries in natural key order
	 */
	public static <Value> Map<String, Value> sortMap(final Map<String, Value> mapToSort) {
		final List<String> keys = new ArrayList<>(mapToSort.keySet());
		Collections.sort(keys);
		final LinkedHashMap<String, Value> sortedContent = new LinkedHashMap<>();
		for (final String key : keys) {
			sortedContent.put(key, mapToSort.get(key));
		}
		return sortedContent;
	}

	/**
	 * Get files of classpath
	 *
	 * @return classpath entries separated by the platform path separator (":" on Linux, ";" on Windows)
	 */
	public static String getClassPath() {
		return System.getProperty("java.class.path");
	}

	/**
	 * Check array equality. Items are compared by equals(), null items are allowed.
	 *
	 * @param <T> type of the items
	 * @param array1 first array
	 * @param array2 second array
	 * @return true if both arrays are null or have equal items in the same order
	 */
	public static <T> boolean compare(final T[] array1, final T[] array2) {
		return Arrays.equals(array1, array2);
	}

	/**
	 * Get all system properties
	 *
	 * @return map of all system properties, on Linux extended by "os.distribution.name" and "os.distribution.version"
	 */
	public static Map<String, String> getSystemPropertiesMap() {
		final Map<String, String> propertiesMap = new HashMap<>();
		// stringPropertyNames() skips non-String keys, which caused a ClassCastException before
		final Properties systemProperties = System.getProperties();
		for (final String key : systemProperties.stringPropertyNames()) {
			propertiesMap.put(key, systemProperties.getProperty(key));
		}
		if (SystemUtilities.isLinuxSystem()) {
			final File distributionInfoFile = new File("/etc/os-release");
			if (distributionInfoFile.exists()) {
				try (FileInputStream inputStream = new FileInputStream(distributionInfoFile)) {
					final Properties distributionInfoProperties = new Properties();
					distributionInfoProperties.load(inputStream);
					final String distributionName = distributionInfoProperties.getProperty("NAME");
					if (distributionName != null && !"".equals(distributionName.trim())) {
						propertiesMap.put("os.distribution.name",  Utilities.trimSimultaneously(distributionName, "\""));
					} else {
						propertiesMap.put("os.distribution.name", "Unknown");
					}
					final String distributionVersion = distributionInfoProperties.getProperty("VERSION");
					if (distributionVersion != null && !"".equals(distributionVersion.trim())) {
						propertiesMap.put("os.distribution.version",  Utilities.trimSimultaneously(distributionVersion, "\""));
					} else {
						propertiesMap.put("os.distribution.version", "Unknown");
					}
				} catch (@SuppressWarnings("unused") final Exception e) {
					propertiesMap.put("os.distribution.name", "Unknown");
					propertiesMap.put("os.distribution.version", "Unknown");
				}
			} else {
				propertiesMap.put("os.distribution.name", "Unknown");
				propertiesMap.put("os.distribution.version", "Unknown");
			}
		}
		return propertiesMap;
	}

	/**
	 * Convert Map to String
	 *
	 * @param map
	 *            map to convert
	 * @param entrySeparator
	 *            separator between the entries
	 * @param keySeparator
	 *            separator between key and value
	 * @param sort
	 *            sort the entries by key
	 * @return string with all entries of the map
	 */
	public static String getStringFromMap(final Map<String, ? extends Object> map, final String entrySeparator, final String keySeparator, final boolean sort) {
		final List<String> keyList = new ArrayList<>(map.keySet());
		if (sort) {
			Collections.sort(keyList, new Comparator<String>() {
				@Override
				public int compare(final String o1, final String o2) {
					return Comparator.nullsFirst(String::compareTo).compare(o1, o2);
				}
			});
		}

		final StringBuilder builder = new StringBuilder();
		for (final String key : keyList) {
			if (builder.length() > 0) {
				builder.append(entrySeparator);
			}
			builder.append(key == null ? "" : key);
			builder.append(keySeparator);
			final Object value = map.get(key);
			builder.append(value == null ? "" : value.toString());
		}
		return builder.toString();
	}

	/**
	 * Make a number with unitsign human readable
	 *
	 * @param value
	 *            number to format
	 * @param unitTypeSign
	 *            unit sign appended to the unit prefix, e.g. "B" for bytes
	 * @param siUnits
	 *            true for SI units (factor 1000), false for binary units (factor 1024)
	 * @param amountOfSignifiantDigits
	 *            number of significant digits
	 * @param keepTrailingZeros
	 *            keep trailing zeros of the decimals
	 * @param locale
	 *            locale for the decimal separator
	 * @return formatted number with unit, e.g. "1.5 KiB"
	 */
	public static String getHumanReadableNumber(final Number value, final String unitTypeSign, final boolean siUnits, final int amountOfSignifiantDigits, final boolean keepTrailingZeros, final Locale locale) {
		final int unit = siUnits ? 1000 : 1024;
		final double doubleValue = value.doubleValue();

		if (doubleValue < unit && (value instanceof Integer || value instanceof Long)) {
			return value + getHumanReadableUnitExtension(0, unitTypeSign, siUnits);
		}

		int exponent = 0;
		if (doubleValue >= unit && !Double.isInfinite(doubleValue)) {
			// "E" is the largest available unit prefix
			exponent = Math.min((int) (Math.log(doubleValue) / Math.log(unit)), HUMAN_READABLE_MAX_EXPONENT);
		}
		double interimValue = doubleValue / Math.pow(unit, exponent);

		final DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols(locale);
		DecimalFormat numberFormat = createHumanReadableNumberFormat(interimValue, amountOfSignifiantDigits, keepTrailingZeros, decimalFormatSymbols);

		if (!Double.isNaN(interimValue) && !Double.isInfinite(interimValue)) {
			// Rounding may carry over into the next decade or even the next unit prefix (e.g. 999.96 -> "1000.0"),
			// so the format is chosen by the rounded value instead of the raw value
			double roundedValue = new BigDecimal(interimValue).setScale(numberFormat.getMaximumFractionDigits(), numberFormat.getRoundingMode()).doubleValue();
			if (roundedValue >= unit && exponent < HUMAN_READABLE_MAX_EXPONENT) {
				exponent++;
				interimValue = doubleValue / Math.pow(unit, exponent);
				roundedValue = roundedValue / unit;
			}
			numberFormat = createHumanReadableNumberFormat(roundedValue, amountOfSignifiantDigits, keepTrailingZeros, decimalFormatSymbols);
		}

		return numberFormat.format(interimValue) + getHumanReadableUnitExtension(exponent, unitTypeSign, siUnits);
	}

	/** Index of the largest unit prefix "E" in "kMGTPE" (1-based exponent) */
	private static final int HUMAN_READABLE_MAX_EXPONENT = 6;

	private static String getHumanReadableUnitExtension(final int exponent, final String unitTypeSign, final boolean siUnits) {
		if (exponent == 0) {
			return isNotBlank(unitTypeSign) ? " " + unitTypeSign : "";
		} else {
			return " " + (siUnits ? "kMGTPE" : "KMGTPE").charAt(exponent - 1) + (siUnits ? "" : "i") + (isNotBlank(unitTypeSign) ? unitTypeSign : "");
		}
	}

	private static DecimalFormat createHumanReadableNumberFormat(final double magnitudeValue, final int amountOfSignifiantDigits, final boolean keepTrailingZeros, final DecimalFormatSymbols decimalFormatSymbols) {
		final int integerDigits;
		if (magnitudeValue >= 1000) {
			integerDigits = 4;
		} else if (magnitudeValue >= 100) {
			integerDigits = 3;
		} else if (magnitudeValue >= 10) {
			integerDigits = 2;
		} else if (magnitudeValue >= 1) {
			integerDigits = 1;
		} else {
			integerDigits = 0;
		}

		if (keepTrailingZeros) {
			final int fractionDigits = amountOfSignifiantDigits - integerDigits;
			// Pattern "#0." would always show the decimal separator, e.g. "1000,"
			return new DecimalFormat(fractionDigits > 0 ? "#0." + repeat("0", fractionDigits) : "#0", decimalFormatSymbols);
		} else {
			// At least one decimal is always shown, further ones only up to the amount of significant digits
			return new DecimalFormat("#0.0" + repeat("#", amountOfSignifiantDigits - integerDigits - 1), decimalFormatSymbols);
		}
	}

	/**
	 * Parse a human readable number with optional unit prefix like "1.5 KiB", "2 MB" or "300 k".
	 * Both SI prefixes (k, M, G, T, P, E) and binary prefixes (Ki, Mi, ...) are accepted, the factor is defined by siUnits.
	 *
	 * @param value text to parse
	 * @param unitTypeSign unit sign at the end of the text to remove, e.g. "B" for bytes, may be null
	 * @param siUnits true for factor 1000 per prefix step, false for factor 1024
	 * @param locale locale defining the decimal and grouping separator
	 * @return parsed value as float (may be rounded)
	 * @throws RuntimeException if the value is blank
	 * @throws NumberFormatException if the value is no number
	 */
	public static float parseHumanReadableNumber(String value, final String unitTypeSign, final boolean siUnits, final Locale locale) {
		if (isBlank(value)) {
			throw new RuntimeException("Not a numeric value");
		} else if (unitTypeSign != null && endsWithIgnoreCase(value, unitTypeSign)) {
			value = value.substring(0, value.length() - unitTypeSign.length()).trim();
		}

		final DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols(locale);

		value = value.replace(Character.toString(decimalFormatSymbols.getGroupingSeparator()), "").replace(Character.toString(decimalFormatSymbols.getDecimalSeparator()), ".").trim();

		// Binary unit prefixes like "Ki" or "Mi", as created by getHumanReadableNumber (before, they caused a NumberFormatException)
		if (value.length() > 1 && endsWithIgnoreCase(value, "i") && "kmgtpe".indexOf(Character.toLowerCase(value.charAt(value.length() - 2))) >= 0) {
			value = value.substring(0, value.length() - 1);
		}

		long dimension = 1;
		if (endsWithIgnoreCase(value, "k")) {
			if (siUnits) {
				dimension = 1000;
			} else {
				dimension = 1024;
			}
			value = value.substring(0, value.length() - 1).trim();
		} else if (endsWithIgnoreCase(value, "M")) {
			if (siUnits) {
				dimension = (long) Math.pow(1000, 2);
			} else {
				dimension = (long) Math.pow(1024, 2);
			}
			value = value.substring(0, value.length() - 1).trim();
		} else if (endsWithIgnoreCase(value, "G")) {
			if (siUnits) {
				dimension = (long) Math.pow(1000, 3);
			} else {
				dimension = (long) Math.pow(1024, 3);
			}
			value = value.substring(0, value.length() - 1).trim();
		} else if (endsWithIgnoreCase(value, "T")) {
			if (siUnits) {
				dimension = (long) Math.pow(1000, 4);
			} else {
				dimension = (long) Math.pow(1024, 4);
			}
			value = value.substring(0, value.length() - 1).trim();
		} else if (endsWithIgnoreCase(value, "P")) {
			if (siUnits) {
				dimension = (long) Math.pow(1000, 5);
			} else {
				dimension = (long) Math.pow(1024, 5);
			}
			value = value.substring(0, value.length() - 1).trim();
		} else if (endsWithIgnoreCase(value, "E")) {
			if (siUnits) {
				dimension = (long) Math.pow(1000, 6);
			} else {
				dimension = (long) Math.pow(1024, 6);
			}
			value = value.substring(0, value.length() - 1).trim();
		}

		final float numericValue = Float.parseFloat(value) * dimension;
		return numericValue;
	}

	/**
	 * Get a human readable speed like "1.5 MiB/s" for the number of items processed in a time span.
	 *
	 * @param startTime start of the processing
	 * @param endTime end of the processing
	 * @param itemsDone number of items processed
	 * @param unitTypeSign unit sign of the items, e.g. "B" for bytes, may be null
	 * @param siUnits true for SI unit prefixes (factor 1000), false for binary unit prefixes (factor 1024)
	 * @param locale locale for the decimal separator
	 * @return speed per second with unit, or "Done in &lt;1 ms" if the time span is shorter than one millisecond
	 */
	public static String getHumanReadableSpeed(final LocalDateTime startTime, final LocalDateTime endTime, final long itemsDone, final String unitTypeSign, final boolean siUnits, final Locale locale) {
		final long milliseconds = Duration.between(startTime, endTime).toMillis();
		if (milliseconds > 0) {
			// Floating point division, so fractions of items per second are kept
			final double itemsPerSecond = itemsDone * 1000.0 / milliseconds;

			final int unit = siUnits ? 1000 : 1024;
			double interimValue = itemsPerSecond;
			String unitExtension = "";
			int maxTrailingDigits = 0;
			if (interimValue > unit) {
				// "E" is the largest available unit prefix
				final int exponent = Math.min((int) (Math.log(interimValue) / Math.log(unit)), HUMAN_READABLE_MAX_EXPONENT);
				unitExtension = " " + (siUnits ? "kMGTPE" : "KMGTPE").charAt(exponent - 1) + (siUnits ? "" : "i");
				interimValue = interimValue / Math.pow(unit, exponent);
				maxTrailingDigits = exponent * 3;
			}

			if (isNotBlank(unitTypeSign)) {
				unitExtension = unitExtension + unitTypeSign + "/s";
			} else {
				unitExtension = unitExtension + "/s";
			}

			final DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols(locale);
			final DecimalFormat numberFormat;
			final int amountOfSignificantDigits = 5;

			int trailingDigits;
			if (interimValue >= 1000) {
				trailingDigits = amountOfSignificantDigits - 4;
			} else if (interimValue >= 100) {
				trailingDigits = amountOfSignificantDigits - 3;
			} else if (interimValue >= 10) {
				trailingDigits = amountOfSignificantDigits - 2;
			} else if (interimValue >= 1) {
				trailingDigits = amountOfSignificantDigits - 1;
			} else {
				trailingDigits = amountOfSignificantDigits;
			}

			// Without unit prefix maxTrailingDigits is 0, so the repeat count must not get negative
			numberFormat = new DecimalFormat("#0.0" + repeat("#", Math.max(0, Math.min(trailingDigits, maxTrailingDigits - 1))), decimalFormatSymbols);

			return numberFormat.format(interimValue) + unitExtension;
		} else {
			return "Done in <1 ms";
		}
	}

	/**
	 * Make an integer with unitsign human readable and keep all digits
	 *
	 * @param value
	 *            number to format
	 * @param unitTypeSign
	 *            unit sign appended after a blank, may be null
	 * @param locale
	 *            locale for the grouping separator
	 * @return formatted number with grouping separators and unit, e.g. "1,234,567 B"
	 */
	public static String getHumanReadableInteger(final Long value, final String unitTypeSign, final Locale locale) {
		// Format the long value itself, a double loses precision for values above 2^53
		final long interimValue = value.longValue();
		String unitExtension = "";
		if (isNotBlank(unitTypeSign)) {
			unitExtension = " " + unitTypeSign;
		}

		final DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols(locale);
		final DecimalFormat numberFormat = new DecimalFormat("###,##0", decimalFormatSymbols);

		return numberFormat.format(interimValue) + unitExtension;
	}

	/**
	 * Generate MD5 from string data
	 *
	 * @param data
	 *            text to hash, encoded as UTF-8
	 * @return MD5 hash
	 * @throws Exception
	 *             if the hash algorithm is not available
	 */
	public static byte[] getMD5Hash(final String data) throws Exception {
		try {
			return MessageDigest.getInstance("MD5").digest(data.getBytes(StandardCharsets.UTF_8));
		} catch (final Exception e) {
			throw new Exception("Error while MD5 hashing", e);
		}
	}

	/**
	 * Generate SHA-1 from string data
	 *
	 * @param data
	 *            text to hash, encoded as UTF-8
	 * @return SHA-1 hash
	 * @throws Exception
	 *             if the hash algorithm is not available
	 */
	public static byte[] getSHA1Hash(final String data) throws Exception {
		try {
			return MessageDigest.getInstance("SHA-1").digest(data.getBytes(StandardCharsets.UTF_8));
		} catch (final Exception e) {
			throw new Exception("Error while SHA-1 hashing", e);
		}
	}

	/**
	 * Generate SHA-256 from string data
	 *
	 * @param data
	 *            text to hash, encoded as UTF-8
	 * @return SHA-256 hash
	 * @throws Exception
	 *             if the hash algorithm is not available
	 */
	public static byte[] getSHA256Hash(final String data) throws Exception {
		try {
			return MessageDigest.getInstance("SHA-256").digest(data.getBytes(StandardCharsets.UTF_8));
		} catch (final Exception e) {
			throw new Exception("Error while SHA-256 hashing", e);
		}
	}

	/**
	 * Generate SHA-384 from string data
	 *
	 * @param data
	 *            text to hash, encoded as UTF-8
	 * @return SHA-384 hash
	 * @throws Exception
	 *             if the hash algorithm is not available
	 */
	public static byte[] getSHA384Hash(final String data) throws Exception {
		try {
			return MessageDigest.getInstance("SHA-384").digest(data.getBytes(StandardCharsets.UTF_8));
		} catch (final Exception e) {
			throw new Exception("Error while SHA-384 hashing", e);
		}
	}

	/**
	 * Generate SHA-512 from string data
	 *
	 * @param data
	 *            text to hash, encoded as UTF-8
	 * @return SHA-512 hash
	 * @throws Exception
	 *             if the hash algorithm is not available
	 */
	public static byte[] getSHA512Hash(final String data) throws Exception {
		try {
			return MessageDigest.getInstance("SHA-512").digest(data.getBytes(StandardCharsets.UTF_8));
		} catch (final Exception e) {
			throw new Exception("Error while SHA-512 hashing", e);
		}
	}

	/**
	 * Get bytearray for list of bytes
	 *
	 * @param data
	 *            list of bytes
	 * @return array with the bytes of the list
	 */
	public static byte[] getByteArray(final List<Byte> data) {
		final byte[] returnArray = new byte[data.size()];
		for (int i = 0; i < data.size(); i++) {
			returnArray[i] = data.get(i);
		}
		return returnArray;
	}

	/**
	 * Get stacktrace as string
	 *
	 * @param stackTrace
	 *            stacktrace elements, may be null
	 * @return stacktrace with one element per line, empty for null
	 */
	public static String stacktraceToString(final StackTraceElement[] stackTrace) {
		final StringBuilder returnBuilder = new StringBuilder();
		if (stackTrace != null) {
			for (final StackTraceElement stackTraceElement : stackTrace) {
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
	 *            URL of the file
	 * @param localeDestionationPath
	 *            local file path to write to
	 * @param proxy
	 *            proxy to use, null for a direct connection
	 * @throws Exception
	 *             if the download or writing the file fails
	 */
	public static void downloadFile(final String url, final String localeDestionationPath, final Proxy proxy) throws Exception {
		final URLConnection urlConnection = URI.create(url).toURL().openConnection(proxy == null ? Proxy.NO_PROXY : proxy);
		// Without timeouts an unresponsive server would block forever
		urlConnection.setConnectTimeout(DOWNLOAD_TIMEOUT_MILLIS);
		urlConnection.setReadTimeout(DOWNLOAD_TIMEOUT_MILLIS);
		try (BufferedInputStream bufferedInputStream = new BufferedInputStream(urlConnection.getInputStream());
				FileOutputStream fileOutputStream = new FileOutputStream(localeDestionationPath)) {
			IoUtilities.copy(bufferedInputStream, fileOutputStream);
		} catch (final Exception e) {
			// Do not leave an incomplete file behind
			new File(localeDestionationPath).delete();
			throw new Exception("Cannot download file", e);
		}
	}

	/** Timeout in milliseconds for connecting and for each read of a download */
	private static final int DOWNLOAD_TIMEOUT_MILLIS = 60_000;

	/**
	 * Download a file from url
	 *
	 * @param url
	 *            URL of the file
	 * @param localeDestionationPath
	 *            local file path to write to
	 * @throws Exception
	 *             if the download or writing the file fails
	 */
	public static void downloadFile(final String url, final String localeDestionationPath) throws Exception {
		downloadFile(url, localeDestionationPath, null);
	}

	/**
	 * Check array for duplicate strings
	 *
	 * @param inputArray
	 *            array to check
	 * @param ignoreNullValues
	 *            do not count multiple null values as duplicates
	 * @return true if the array contains a duplicate
	 */
	public static boolean checkForDuplicates(final String[] inputArray, final boolean ignoreNullValues) {
		final Set<String> tempSet = new HashSet<>();
		for (final String stringItem : inputArray) {
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
	 * @param <T> type of the items to select
	 * @param collection items to filter
	 * @param classToSelect class of the items to select
	 * @return list of all items that are instances of the given class
	 */
	@SuppressWarnings("unchecked")
	public static <T> List<T> selectItems(final Collection<?> collection, final Class<T> classToSelect) {
		final List<T> list = new ArrayList<>();
		for (final Object item : collection) {
			if (classToSelect.isInstance(item)) {
				list.add((T) item);
			}
		}
		return list;
	}

	/**
	 * Filter all Objects of given class
	 *
	 * @param <T> type of the items to select
	 * @param array items to filter
	 * @param classToSelect class of the items to select
	 * @return list of all items that are instances of the given class
	 */
	@SuppressWarnings("unchecked")
	public static <T> List<T> selectItems(final Object[] array, final Class<T> classToSelect) {
		final List<T> list = new ArrayList<>();
		for (final Object item : array) {
			if (classToSelect.isInstance(item)) {
				list.add((T) item);
			}
		}
		return list;
	}

	/**
	 * Create a reversed copy of an array. The given array is not changed (see {@link #reverseArray(Object[])} for in-place reversal).
	 *
	 * @param <T> type of the items
	 * @param array array to reverse, must not be null
	 * @return new array of the same component type with the items in reverse order
	 */
	public static <T> T[] revertArray(final T[] array) {
		// Array of the same component type (before, an Object[] was returned, causing a ClassCastException for e.g. String[] callers)
		@SuppressWarnings("unchecked")
		final T[] returnValue = (T[]) Array.newInstance(array.getClass().getComponentType(), array.length);
		for (int i = 0; i < array.length; i++) {
			returnValue[i] = array[array.length - 1 - i];
		}
		return returnValue;
	}

	/**
	 * Get the host name of a URL without a leading "www.".
	 *
	 * @param url URL like "https://www.example.com/path"
	 * @return host name like "example.com", or null if the URL has no host part
	 * @throws Exception if the URL is invalid
	 */
	public static String getDomainFromUrl(final String url) throws Exception {
		final URI uri = new URI(url);
		final String domain = uri.getHost();
		if (domain == null) {
			// URLs without host part like "mailto:..." or relative URLs
			return null;
		}
		return domain.startsWith("www.") ? domain.substring(4) : domain;
	}

	/**
	 * Check whether a string is null or empty.
	 *
	 * @param value string to check
	 * @return true for null or ""
	 */
	public static boolean isEmpty(final String value) {
		return value == null || value.length() == 0;
	}

	/**
	 * Check whether a string has at least one character.
	 *
	 * @param value string to check
	 * @return false for null or "", otherwise true
	 */
	public static boolean isNotEmpty(final String value) {
		return !isEmpty(value);
	}

	/**
	 * Check whether a collection is null or empty.
	 *
	 * @param collection collection to check
	 * @return true for null or an empty collection
	 */
	public static boolean isEmpty(final Collection<?> collection) {
		return collection == null || collection.isEmpty();
	}

	/**
	 * Check whether a collection has at least one item.
	 *
	 * @param collection collection to check
	 * @return false for null or an empty collection, otherwise true
	 */
	public static boolean isNotEmpty(final Collection<?> collection) {
		return !isEmpty(collection);
	}

	/**
	 * Check whether a string is null, empty or contains only whitespace (characters up to U+0020, see {@link String#trim()}).
	 *
	 * @param value string to check
	 * @return true for null, empty or whitespace only
	 */
	public static boolean isBlank(final String value) {
		return value == null || value.length() == 0 || value.trim().length() == 0;
	}

	/**
	 * Check whether a string contains at least one non-whitespace character (see {@link #isBlank(String)}).
	 *
	 * @param value string to check
	 * @return true if the string is not blank
	 */
	public static boolean isNotBlank(final String value) {
		return !isBlank(value);
	}

	/**
	 * Check whether a character array is null or empty.
	 *
	 * @param value array to check
	 * @return true for null or an empty array
	 */
	public static boolean isEmpty(final char[] value) {
		return value == null || value.length == 0;
	}

	/**
	 * Overwrite all characters of an array with zero, e.g. to remove a password from memory.
	 *
	 * @param array array to clear, may be null
	 */
	public static void clear(final char[] array) {
		if (array != null) {
			for (int i = 0; i < array.length; i++) {
				array[i] = 0;
			}
		}
	}

	/**
	 * Overwrite all bytes of an array with zero, e.g. to remove key data from memory.
	 *
	 * @param array array to clear, may be null
	 */
	public static void clear(final byte[] array) {
		if (array != null) {
			Arrays.fill(array, (byte) 0);
		}
	}

	/**
	 * Check whether a character array has at least one character.
	 *
	 * @param value array to check
	 * @return false for null or an empty array, otherwise true
	 */
	public static boolean isNotEmpty(final char[] value) {
		return !isEmpty(value);
	}

	/**
	 * Check whether a character array is null, empty or contains only whitespace (see {@link Character#isWhitespace(char)}).
	 *
	 * @param value array to check
	 * @return true for null, empty or whitespace only
	 */
	public static boolean isBlank(final char[] value) {
		if (value == null || value.length == 0) {
			return true;
		} else {
			for (final char character : value) {
				if (!Character.isWhitespace(character)) {
					return false;
				}
			}
			return true;
		}
	}

	/**
	 * Check whether a character array contains at least one non-whitespace character.
	 *
	 * @param value array to check
	 * @return true if the array is not blank
	 */
	public static boolean isNotBlank(final char[] value) {
		return !isBlank(value);
	}

	/**
	 * Close a resource and ignore any IOException.
	 *
	 * @param closeable resource to close, may be null
	 */
	public static void closeQuietly(final Closeable closeable) {
		if (closeable != null) {
			try {
				closeable.close();
			} catch (@SuppressWarnings("unused") final IOException e) {
				// Do nothing
			}
		}
	}

	/**
	 * XMLStreamReader.close() doesn't close the underlying stream.
	 * So it must be closed separately.
	 *
	 * @param closeable
	 *            XMLStreamReader to close, may be null
	 * @param inputStream
	 *            underlying stream to close, may be null
	 */
	public static void closeQuietly(final XMLStreamReader closeable, final InputStream inputStream) {
		if (closeable != null) {
			try {
				closeable.close();
			} catch (@SuppressWarnings("unused") final Exception e) {
				// Do nothing
			}
		}
		if (inputStream != null) {
			try {
				inputStream.close();
			} catch (@SuppressWarnings("unused") final Exception e) {
				// Do nothing
			}
		}
	}

	/**
	 * Close an XMLStreamWriter, errors are only printed to stderr.
	 * XMLStreamWriter.close() does not close the underlying stream.
	 *
	 * @param xmlWriter writer to close, may be null
	 */
	public static void closeQuietly(XMLStreamWriter xmlWriter) {
		if (xmlWriter != null) {
			try {
				xmlWriter.close();
			} catch (final Exception e) {
				e.printStackTrace();
			}
			xmlWriter = null;
		}
	}

	/**
	 * Build a string by repeating a character.
	 *
	 * @param valueChar character to repeat
	 * @param count number of repetitions, 0 or less for an empty string
	 * @return repeated characters
	 */
	public static String repeat(final char valueChar, final int count) {
		return repeat(Character.toString(valueChar), count, null);
	}

	/**
	 * Build a string by repeating a text.
	 *
	 * @param value text to repeat, may be null
	 * @param count number of repetitions, 0 or less for an empty string
	 * @return repeated text, or null for null input
	 */
	public static String repeat(final String value, final int count) {
		return repeat(value, count, null);
	}

	/**
	 * Build a string by repeating a text with a separator between the repetitions.
	 *
	 * @param value text to repeat, may be null
	 * @param count number of repetitions, 0 or less for an empty string
	 * @param separatorString separator between the repetitions, null for none
	 * @return repeated text, or null for null input
	 */
	public static String repeat(final String value, final int count, final String separatorString) {
		if (value == null) {
			return null;
		} else if (value.length() == 0 || count == 0) {
			return "";
		} else {
			final StringBuilder returnValue = new StringBuilder();
			for (int i = 0; i < count; i++) {
				if (separatorString != null && returnValue.length() > 0) {
					returnValue.append(separatorString);
				}
				returnValue.append(value);
			}
			return returnValue.toString();
		}
	}

	/**
	 * Join the string representations of array items. Null items are shown as empty strings.
	 *
	 * @param array items to join, may be null
	 * @param glue separator between the items, null for none
	 * @return joined text, or null for a null array
	 */
	public static String join(final Object[] array, String glue) {
		if (array == null) {
			return null;
		} else if (array.length == 0) {
			return "";
		} else {
			if (glue == null) {
				glue = "";
			}

			final StringBuilder returnValue = new StringBuilder();
			boolean isFirst = true;
			for (Object object : array) {
				if (!isFirst) {
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

	/**
	 * Join characters with a separator.
	 *
	 * @param array characters to join, may be null
	 * @param glue separator between the characters, null for none
	 * @return joined text, or null for a null array
	 */
	public static String join(final char[] array, String glue) {
		if (array == null) {
			return null;
		} else if (array.length == 0) {
			return "";
		} else {
			if (glue == null) {
				glue = "";
			}

			final StringBuilder returnValue = new StringBuilder();
			boolean isFirst = true;
			for (final char nextChar : array) {
				if (!isFirst) {
					returnValue.append(glue);
				}
				returnValue.append(nextChar);
				isFirst = false;
			}
			return returnValue.toString();
		}
	}

	/**
	 * Join the string representations of items. Null items are shown as empty strings.
	 *
	 * @param iterableObject items to join, may be null
	 * @param glue separator between the items, null for none
	 * @return joined text, or null for null items
	 */
	public static String join(final Iterable<?> iterableObject, String glue) {
		if (iterableObject == null) {
			return null;
		} else {
			if (glue == null) {
				glue = "";
			}

			final StringBuilder returnValue = new StringBuilder();
			boolean isFirst = true;
			for (Object object : iterableObject) {
				if (!isFirst) {
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

	/**
	 * Create a copy of an array without the first occurrence of an item (compared by equals).
	 *
	 * @param <T> type of the items
	 * @param array array to copy, may be null
	 * @param itemToRemove item to remove, may be null
	 * @return new array without the item (an unchanged copy if the item is not contained), or null for a null array
	 */
	public static <T> T[] remove(final T[] array, final T itemToRemove) {
		int indexToRemove = -1;
		if (array == null) {
			return null;
		} else if (itemToRemove == null) {
			for (int i = 0; i < array.length; i++) {
				if (array[i] == null) {
					indexToRemove = i;
					break;
				}
			}
		} else {
			for (int i = 0; i < array.length; i++) {
				if (itemToRemove.equals(array[i])) {
					indexToRemove = i;
					break;
				}
			}
		}

		if (indexToRemove >= 0) {
			@SuppressWarnings("unchecked")
			final T[] result = (T[]) Array.newInstance(array.getClass().getComponentType(), array.length - 1);
			if (indexToRemove > 0) {
				System.arraycopy(array, 0, result, 0, indexToRemove);
			}
			if (indexToRemove < array.length - 1) {
				System.arraycopy(array, indexToRemove + 1, result, indexToRemove, array.length - indexToRemove - 1);
			}
			return result;
		} else {
			@SuppressWarnings("unchecked")
			final T[] result = (T[]) Array.newInstance(array.getClass().getComponentType(), array.length);
			System.arraycopy(array, 0, result, 0, array.length);
			return result;
		}
	}

	/**
	 * Create a copy of an array without the item at the given index.
	 *
	 * @param <T> type of the items
	 * @param array array to copy, must not be null
	 * @param itemIndexToRemove index of the item to remove, must be a valid index of the array
	 * @return new array with one item less
	 */
	public static <T> T[] removeItemAtIndex(final T[] array, final int itemIndexToRemove) {
		@SuppressWarnings("unchecked")
		final T[] result = (T[]) Array.newInstance(array.getClass().getComponentType(), array.length - 1);
		for (int i = 0; i < array.length; i++) {
			if (i < itemIndexToRemove) {
				result[i] = array[i];
			} else if (i > itemIndexToRemove) {
				result[i - 1] = array[i];
			}
		}
		return result;
	}

	/**
	 * Read the complete content of a file.
	 *
	 * @param file file to read
	 * @return file content
	 * @throws FileNotFoundException if the file does not exist or cannot be opened
	 * @throws IOException if reading fails
	 */
	public static byte[] readFileToByteArray(final File file) throws FileNotFoundException, IOException {
		try (FileInputStream in = new FileInputStream(file)) {
			// A single read() call may return less data than requested, so read until the end of the stream
			return in.readAllBytes();
		}
	}

	/**
	 * Prepend blanks at the left of a string to make it fit the given minimum length
	 *
	 * @param value
	 *            string to pad
	 * @param minimumLength
	 *            minimum length of the result
	 * @return padded string, unchanged if it is already long enough
	 */
	public static String leftPad(final String value, final int minimumLength) {
		try {
			return String.format("%1$" + minimumLength + "s", value);
		} catch (@SuppressWarnings("unused") final Exception e) {
			return value;
		}
	}

	/**
	 * Prepend a pad character at the left of a string to make it fit the given minimum length.
	 *
	 * @param value string to pad, may be null
	 * @param size minimum length of the result
	 * @param padChar character to prepend
	 * @return padded string, unchanged if it is already long enough, or null for null input
	 */
	public static String leftPad(final String value, final int size, final char padChar) {
		if (value == null) {
			return null;
		} else {
			final int padsize = size - value.length();
			if (padsize <= 0) {
				return value;
			} else {
				return repeat(padChar, padsize).concat(value);
			}
		}
	}

	/**
	 * Append blanks at the right of a string to make it fit the given minimum length
	 *
	 * @param value
	 *            string to pad
	 * @param minimumLength
	 *            minimum length of the result
	 * @return padded string, unchanged if it is already long enough
	 */
	public static String rightPad(final String value, final int minimumLength) {
		try {
			return String.format("%1$-" + minimumLength + "s", value);
		} catch (@SuppressWarnings("unused") final Exception e) {
			return value;
		}
	}

	/**
	 * Only trim the value when the surrounding occurs on both ends
	 *
	 * @param value
	 *            string to trim
	 * @param surrounding
	 *            text to remove at the start and the end
	 * @return trimmed string, or the unchanged value if it does not start and end with the surrounding
	 */
	public static String trimSimultaneously(final String value, final String surrounding) {
		if (value == null) {
			return null;
		} else if (isEmpty(surrounding)) {
			return value;
		} else if (value.length() >= surrounding.length() * 2 && value.startsWith(surrounding) && value.endsWith(surrounding)) {
			// Length check: a single '"' starts and ends with '"', but must not be trimmed (substring would fail)
			return value.substring(surrounding.length(), value.length() - surrounding.length());
		} else {
			return value;
		}
	}

	/**
	 * Null-safe {@link String#trim()}.
	 *
	 * @param value string to trim, may be null
	 * @return trimmed string, or null for null input
	 */
	public static String trim(final String value) {
		if (value == null) {
			return null;
		} else {
			return value.trim();
		}
	}

	/**
	 * Remove all occurrences of a character at the start and the end of a string.
	 *
	 * @param value string to trim, may be null
	 * @param trimChar character to remove
	 * @return trimmed string, or null for null input
	 */
	public static String trim(String value, final char trimChar) {
		while (value != null && value.startsWith(Character.toString(trimChar))) {
			value = value.substring(1);
		}

		while (value != null && value.endsWith(Character.toString(trimChar))) {
			value = value.substring(0, value.length() - 1);
		}

		return value;
	}

	/**
	 * Read the remaining data of a stream as text. The stream is not closed.
	 *
	 * @param inputStream stream to read
	 * @param encoding charset of the data
	 * @return text of the stream
	 * @throws IOException if reading fails
	 */
	public static String toString(final InputStream inputStream, final Charset encoding) throws IOException {
		return new String(IoUtilities.toByteArray(inputStream), encoding);
	}

	/**
	 * Read the remaining characters of a reader. The reader is not closed.
	 *
	 * @param characterStream reader to read
	 * @return text of the reader (always a String)
	 * @throws IOException if reading fails
	 */
	public static Object toString(final Reader characterStream) throws IOException {
		final StringBuilder returnValue = new StringBuilder();
		int characterInt;
		while ((characterInt = characterStream.read()) > -1) {
			returnValue.append((char) characterInt);
		}
		return returnValue.toString();
	}

	/**
	 * Read all text lines of a stream. Linebreaks (LF, CR or CRLF) are not included. The stream is closed afterwards.
	 *
	 * @param inStream stream to read
	 * @param encoding charset of the data
	 * @return lines of the stream
	 * @throws IOException if reading fails
	 */
	public static List<String> readLines(final InputStream inStream, final Charset encoding) throws IOException {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(inStream, encoding))) {
			final List<String> lines = new ArrayList<>();
			String nextLine;
			while ((nextLine = reader.readLine()) != null) {
				lines.add(nextLine);
			}
			return lines;
		}
	}

	/**
	 * Remove leading whitespace (regex \s) of a string.
	 *
	 * @param value string to trim, must not be null
	 * @return string without leading whitespace
	 */
	public static String trimLeft(final String value) {
		return value.replaceAll("^\\s+", "");
	}

	/**
	 * Remove trailing whitespace (regex \s) of a string.
	 *
	 * @param value string to trim, must not be null
	 * @return string without trailing whitespace
	 */
	public static String trimRight(final String value) {
		return value.replaceAll("\\s+$", "");
	}

	/**
	 * Add a jar file or directory to the system classloader by reflection.
	 * This only works up to Java 8, because since Java 9 the system classloader is no URLClassLoader anymore.
	 *
	 * @param filePath path of the jar file or directory
	 * @throws IOException if the file cannot be added
	 */
	public static void addFileToClasspath(final String filePath) throws IOException {
		try {
			final Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
			method.setAccessible(true);
			method.invoke(ClassLoader.getSystemClassLoader(), new Object[] { URI.create(new File(filePath).toURI().toString()).toURL() });
		} catch (final Throwable t) {
			throw new IOException("Error, could not add file to system classloader: " + t.getMessage(), t);
		}
	}

	/**
	 * Shorten a string to a maximum length by cutting off its end and appending a cut sign.
	 *
	 * @param value string to shorten, may be null
	 * @param maxLength maximum length of the result including the cut sign
	 * @param cutSign text appended if the string was shortened, null for none
	 * @return shortened string, unchanged if it is not longer than maxLength
	 */
	public static String shortenStringToMaxLengthCutRight(final String value, final int maxLength, final String cutSign) {
		if (value != null && value.length() > maxLength) {
			// Use the real length of the cut sign (before, always 4 characters were reserved, which only fits the default " ...")
			final String cutSignToUse = cutSign == null ? "" : cutSign;
			return value.substring(0, Math.max(0, maxLength - cutSignToUse.length())) + cutSignToUse;
		} else {
			return value;
		}
	}

	/**
	 * Shorten a string to a maximum length by cutting off its end and appending " ...".
	 *
	 * @param value string to shorten, may be null
	 * @param maxLength maximum length of the result including the cut sign
	 * @return shortened string, unchanged if it is not longer than maxLength
	 */
	public static String shortenStringToMaxLengthCutRight(final String value, final int maxLength) {
		return shortenStringToMaxLengthCutRight(value, maxLength, " ...");
	}

	/**
	 * Shorten a string to a maximum length by replacing its middle part by " ... ".
	 *
	 * @param value string to shorten, may be null
	 * @param maxLength maximum length of the result, should be at least 5
	 * @return shortened string, unchanged if it is not longer than maxLength
	 */
	public static String shortenStringToMaxLengthCutMiddle(final String value, final int maxLength) {
		if (value != null && value.length() > maxLength) {
			final int leftLength = (maxLength - 5) / 2;
			return value.substring(0, leftLength) + " ... " + value.substring(value.length() - ((maxLength - leftLength) - 5));
		} else {
			return value;
		}
	}

	/**
	 * Shorten a string to a maximum length by cutting off its start and prepending "... ".
	 *
	 * @param value string to shorten, may be null
	 * @param maxLength maximum length of the result, should be at least 4
	 * @return shortened string, unchanged if it is not longer than maxLength
	 */
	public static String shortenStringToMaxLengthCutLeft(final String value, final int maxLength) {
		if (value != null && value.length() > maxLength) {
			return "... " + value.substring((value.length() - maxLength) + 4);
		} else {
			return value;
		}
	}

	/**
	 * Split a text at ',', ';', '|', blanks, tabs and linebreaks into trimmed, non-empty items.
	 *
	 * @param stringList text to split, may be null
	 * @return list of items, or null for null input
	 */
	public static List<String> splitAndTrimList(final String stringList) {
		if (stringList == null) {
			return null;
		} else {
			final List<String> returnList = new ArrayList<>();
			final String[] parts = stringList.split(",|;|\\|| |\\n|\\r|\\t");
			for (final String part : parts) {
				if (isNotEmpty(part)) {
					returnList.add(part.trim());
				}
			}
			return returnList;
		}
	}

	/**
	 * Split a text at the given separator characters into trimmed, non-empty items.
	 *
	 * @param stringList text to split, may be null
	 * @param separatorChars characters separating the items
	 * @return list of items, or null for null input
	 */
	public static List<String> splitAndTrimList(final String stringList, final Character... separatorChars) {
		if (stringList == null) {
			return null;
		} else {
			final List<String> returnList = new ArrayList<>();
			// Each separator is quoted, so regex meta characters like '.' or '|' are used literally
			final StringBuilder separatorRegex = new StringBuilder();
			for (final Character separatorChar : separatorChars) {
				if (separatorChar != null) {
					if (separatorRegex.length() > 0) {
						separatorRegex.append('|');
					}
					separatorRegex.append(Pattern.quote(separatorChar.toString()));
				}
			}
			if (separatorRegex.length() == 0) {
				final List<String> singleItemList = new ArrayList<>();
				if (isNotEmpty(stringList)) {
					singleItemList.add(stringList.trim());
				}
				return singleItemList;
			}
			final String[] parts = stringList.split(separatorRegex.toString());
			for (final String part : parts) {
				if (isNotEmpty(part)) {
					returnList.add(part.trim());
				}
			}
			return returnList;
		}
	}

	/**
	 * Check case-insensitively whether a string ends with a suffix, independent of the default locale.
	 *
	 * @param data string to check, may be null
	 * @param suffix suffix to look for, may be null
	 * @return true if data ends with suffix; true if both are null or only suffix is null; false if only data is null
	 */
	public static boolean endsWithIgnoreCase(final String data, final String suffix) {
		if (data == suffix) {
			// both null or same object
			return true;
		} else if (data == null) {
			// data is null but suffix is not
			return false;
		} else if (suffix == null) {
			// suffix is null but data is not
			return true;
		} else if (data.length() >= suffix.length() && data.regionMatches(true, data.length() - suffix.length(), suffix, 0, suffix.length())) {
			// regionMatches is independent of the default locale (toLowerCase() fails for 'I' in Turkish locale)
			// both are set, so ignore the case for standard endsWith-method
			return true;
		} else {
			// anything else
			return false;
		}
	}

	/**
	 * Find the first case-insensitive occurrence of a text, independent of the default locale.
	 *
	 * @param data string to search in, may be null
	 * @param part text to search for, may be null
	 * @return index of the first occurrence in data, 0 if both are the same object (or null), -1 if not found or only one is null
	 */
	public static int indexOfIgnoreCase(final String data, final String part) {
		if (data == part) {
			// both null or same object
			return 0;
		} else if (data == null || part == null) {
			// suffix is null but data is not or vice versa
			return -1;
		} else {
			// regionMatches is independent of the default locale and keeps the indexes of the original text
			// (toLowerCase() may change the text length, e.g. for 'İ', and depends on the default locale)
			for (int i = 0; i <= data.length() - part.length(); i++) {
				if (data.regionMatches(true, i, part, 0, part.length())) {
					return i;
				}
			}
			return -1;
		}
	}

	/**
	 * Split a text at the given separator characters into trimmed, non-empty items.
	 * Separators within single or double quotes are ignored, the quotes are kept in the items.
	 *
	 * @param stringList text to split, must not be null
	 * @param separatorChars characters separating the items
	 * @return list of items
	 */
	public static List<String> splitAndTrimListQuoted(final String stringList, final char... separatorChars) {
		final List<String> returnList = new ArrayList<>();
		StringBuilder nextLine = new StringBuilder();
		boolean quotedBySingleQoute = false;
		boolean quotedByDoubleQoute = false;
		for (final char nextChar : stringList.toCharArray()) {
			if ('\'' == nextChar) {
				if (!quotedBySingleQoute && !quotedByDoubleQoute) {
					quotedBySingleQoute = true;
				} else if (quotedBySingleQoute) {
					quotedBySingleQoute = false;
				}
			} else if ('"' == nextChar) {
				if (!quotedBySingleQoute && !quotedByDoubleQoute) {
					quotedByDoubleQoute = true;
				} else if (quotedByDoubleQoute) {
					quotedByDoubleQoute = false;
				}
			}

			boolean splitFound = false;
			for (final char separatorChar : separatorChars) {
				if (separatorChar == nextChar && !quotedBySingleQoute && !quotedByDoubleQoute) {
					final String line = nextLine.toString().trim();
					if (line.length() > 0) {
						returnList.add(line);
					}
					// The separator is consumed also after an empty item (before, it was added to the next item)
					splitFound = true;
					nextLine = new StringBuilder();
					break;
				}
			}

			if (!splitFound) {
				nextLine.append(nextChar);
			}
		}
		final String line = nextLine.toString().trim();
		if (line.length() > 0) {
			returnList.add(line);
		}
		return returnList;
	}

	/**
	 * Check whether a collection contains a string, ignoring case.
	 *
	 * @param list strings to search in, may be null
	 * @param item string to search for, may be null
	 * @return true if an item equals the string ignoring case (or both are null)
	 */
	public static boolean containsIgnoreCase(final Collection<String> list, final String item) {
		if (list == null) {
			return false;
		} else {
			for (final String listItem : list) {
				if (listItem == item || (listItem != null && listItem.equalsIgnoreCase(item))) {
					return true;
				}
			}
			return false;
		}
	}

	/**
	 * Delete a file or a directory recursively. Symbolic links are deleted themselves, the link targets are never followed.
	 *
	 * @param file file or directory to delete
	 * @return true if everything was deleted, false if any file could not be deleted (deletion stops at the first failure)
	 */
	public static boolean delete(final File file) {
		// A symbolic link to a directory must not be followed, otherwise content outside of the deleted tree is deleted
		if (file.isDirectory() && !Files.isSymbolicLink(file.toPath())) {
			// listFiles() returns null if the directory cannot be read, so its content cannot be deleted either
			final File[] subFiles = file.listFiles();
			if (subFiles == null) {
				return false;
			}
			for (final File subFile : subFiles) {
				if (!delete(subFile)) {
					return false;
				}
			}
		}
		return file.delete();
	}

	/**
	 * Limit a value to a range.
	 *
	 * @param minimum lower limit
	 * @param value value to limit
	 * @param maximum upper limit
	 * @return minimum if value is smaller, maximum if value is greater, otherwise value
	 */
	public static int limitValue(final int minimum, final int value, final int maximum) {
		if (value < minimum) {
			return minimum;
		} else if (maximum < value) {
			return maximum;
		} else {
			return value;
		}
	}

	/**
	 * Check case-insensitively whether a string starts with a prefix, independent of the default locale.
	 *
	 * @param data string to check, may be null
	 * @param prefix prefix to look for, may be null
	 * @return true if data starts with prefix, false if any parameter is null
	 */
	public static boolean startsWithCaseinsensitive(final String data, final String prefix) {
		if (data == null || prefix == null) {
			return false;
		} else {
			// regionMatches is independent of the default locale (toLowerCase() fails for 'I' in Turkish locale)
			return data.regionMatches(true, 0, prefix, 0, prefix.length());
		}
	}

	/**
	 * Create a map from alternating keys and values.
	 *
	 * @param data key1, value1, key2, value2, ...
	 * @return new map with the given entries
	 * @throws Exception if an odd number of strings is given
	 */
	public static Map<String, String> createMap(final String... data) throws Exception {
		final Map<String, String> returnMap = new HashMap<>();
		if (data != null && data.length > 0) {
			if (data.length % 2 != 0) {
				throw new Exception("Invalid map data: odd number of parameters, must be even");
			} else {
				for (int i = 0; i < data.length / 2; i++) {
					final String key = data[i * 2];
					final String value = data[i * 2 + 1];
					returnMap.put(key, value);
				}
			}
		}
		return returnMap;
	}

	/**
	 * Check whether an iterable collection contains a special object. Items are compared by identity (==), not by equals().
	 *
	 * @param hayshack
	 *            items to search in
	 * @param needle
	 *            object to search for
	 * @return true if the object is contained
	 */
	public static boolean containsObject(final Iterable<?> hayshack, final Object needle) {
		for (final Object item : hayshack) {
			if (item == needle) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Replace a leading "~" as well as "$HOME" and "${HOME}" in a file path by the user's home directory.
	 *
	 * @param filePath file path, may be null
	 * @return file path with the home directory, or null for null input
	 */
	public static String replaceUsersHome(String filePath) {
		if (filePath == null) {
			return filePath;
		}
		final String homePath = System.getProperty("user.home");

		// "~" only stands for the home directory at the start of the path, otherwise it is a normal file name character
		if ("~".equals(filePath)) {
			filePath = homePath;
		} else if (filePath.startsWith("~/") || filePath.startsWith("~" + File.separator)) {
			filePath = homePath + filePath.substring(1);
		}

		return filePath
				.replace("${HOME}", homePath)
				.replace("$HOME", homePath);
	}

	/**
	 * Replace a leading user's home directory in a file path by "~".
	 *
	 * @param filePath file path, may be null
	 * @return file path starting with "~" if it is within the home directory, otherwise unchanged
	 */
	public static String replaceUsersHomeByTilde(final String filePath) {
		if (filePath == null) {
			return filePath;
		}
		final String homePath = System.getProperty("user.home");

		// Only a complete leading home path is replaced, so "/home/user2" is not changed to "~2" for home "/home/user"
		if (filePath.equals(homePath)) {
			return "~";
		} else if (filePath.startsWith(homePath + "/") || filePath.startsWith(homePath + File.separator)) {
			return "~" + filePath.substring(homePath.length());
		} else {
			return filePath;
		}
	}

	/**
	 * Get the user's download directory, read from XDG_DOWNLOAD_DIR in "~/.config/user-dirs.dirs" on Linux, or "~/Downloads" otherwise.
	 *
	 * @return absolute path of the download directory
	 */
	public static String getUsersDefaultDownloadDirectory() {
		final String userHome = System.getProperty("user.home");

		final File userDirsFile = new File(userHome, ".config/user-dirs.dirs");
		if (userDirsFile.isFile()) {
			try {
				for (final String line : Files.readAllLines(userDirsFile.toPath())) {
					final String trimmedLine = line.trim();
					if (trimmedLine.startsWith("XDG_DOWNLOAD_DIR=")) {
						String value = trimmedLine.substring("XDG_DOWNLOAD_DIR=".length()).trim();
						if (value.startsWith("\"") && value.endsWith("\"")) {
							value = value.substring(1, value.length() - 1);
						}
						value = value.replace("$HOME", userHome);
						if (Utilities.isNotBlank(value)) {
							return value;
						}
					}
				}
			} catch (@SuppressWarnings("unused") final Exception e) {
				// Use fall back
			}
		}

		return new File(userHome, "Downloads").getAbsolutePath();
	}

	/**
	 * Null-safe substring that tolerates a start index behind the end of the text.
	 *
	 * @param text text, may be null
	 * @param startIndex start index, must not be negative
	 * @return substring, "" if startIndex is greater than the text length, or null for null text
	 */
	public static String substring(final String text, final int startIndex) {
		if (text == null) {
			return null;
		} else {
			if (text.length() < startIndex) {
				return "";
			} else {
				return text.substring(startIndex);
			}
		}
	}

	/**
	 * Null-safe substring that tolerates indexes behind the end of the text.
	 *
	 * @param text text, may be null
	 * @param startIndex start index (inclusive), must not be negative
	 * @param endIndex end index (exclusive), cut to the text length
	 * @return substring, "" if startIndex is greater than the text length, or null for null text
	 */
	public static String substring(final String text, final int startIndex, final int endIndex) {
		if (text == null) {
			return null;
		} else {
			if (text.length() < startIndex) {
				return "";
			} else if (text.length() < endIndex) {
				return text.substring(startIndex);
			} else {
				return text.substring(startIndex, endIndex);
			}
		}
	}

	/**
	 * Null-safe check whether a text contains another text.
	 *
	 * @param text text to search in, may be null
	 * @param searchText text to search for, must not be null
	 * @return true if contained, false for null text
	 */
	public static boolean contains(final String text, final String searchText) {
		if (text == null) {
			return false;
		} else {
			return text.contains(searchText);
		}
	}

	/**
	 * Skip an exact number of bytes of a stream.
	 *
	 * @param inputStream stream
	 * @param bytesToSkip number of bytes to skip
	 * @return number of bytes skipped
	 * @throws Exception if the stream cannot skip further (e.g. at its end)
	 */
	public static long skipInputStreamData(final InputStream inputStream, final long bytesToSkip) throws Exception {
		long bytesSkipped = 0;
		while (bytesSkipped < bytesToSkip) {
			final long newBytesSkipped = inputStream.skip(bytesToSkip - bytesSkipped);
			if (newBytesSkipped == 0) {
				throw new Exception("Cannot skip data while reading stream");
			}
			bytesSkipped += newBytesSkipped;
		}
		return bytesSkipped;
	}

	/**
	 * Convert an int into 4 bytes in big endian order.
	 *
	 * @param value int value
	 * @return 4 bytes, most significant first
	 */
	public static byte[] convertIntToByteArray(final int value) {
		return new byte[] { (byte) (value >> 24), (byte) (value >> 16), (byte) (value >> 8), (byte) value };
	}

	/**
	 * Convert up to 4 bytes in big endian order into an int.
	 *
	 * @param bytes 0 to 4 bytes, most significant first
	 * @return int value (unsigned interpretation for less than 4 bytes)
	 * @throws Exception if more than 4 bytes are given
	 */
	public static int convertByteArrayToInt(final byte[] bytes) throws Exception {
		if (bytes.length > 4) {
			throw new Exception("ByteArray is too big for int value");
		} else {
			// Big endian, also for less than 4 bytes (before, ByteBuffer.getInt() threw a BufferUnderflowException for them)
			int value = 0;
			for (final byte nextByte : bytes) {
				value = (value << 8) | (nextByte & 0xFF);
			}
			return value;
		}
	}

	/**
	 * Split a text at a delimiter character, which can be escaped by an escape character.
	 * Escaped delimiters are not split at and are unescaped in the result.
	 *
	 * @param text text to split, must not be null
	 * @param delimiterChar delimiter character
	 * @param escapeChar escape character preceding a delimiter that is part of the data
	 * @param limit limit like in {@link String#split(String, int)}
	 * @return parts of the text
	 */
	public static String[] split(final String text, final char delimiterChar, final char escapeChar, final int limit) {
		final String escapeCharString = Character.toString(escapeChar);
		final String delimiterCharString = Character.toString(delimiterChar);
		final String splitRegex = "(?<!" + Pattern.quote(escapeCharString) + ")" + Pattern.quote(delimiterCharString);
		final String[] returnParts = text.split(splitRegex, limit);
		for (int i = 0; i < returnParts.length; i++) {
			// An escaped delimiter becomes the plain delimiter (before, the delimiter was dropped and the escape character kept)
			returnParts[i] = returnParts[i].replace(escapeCharString + delimiterCharString, delimiterCharString);
		}
		return returnParts;
	}

	/**
	 * Split a text at a delimiter character, which can be escaped by an escape character.
	 * Escaped delimiters are not split at and are unescaped in the result. Trailing empty parts are removed.
	 *
	 * @param text text to split, must not be null
	 * @param delimiterChar delimiter character
	 * @param escapeChar escape character preceding a delimiter that is part of the data
	 * @return parts of the text
	 */
	public static String[] split(final String text, final char delimiterChar, final char escapeChar) {
		final String escapeCharString = Character.toString(escapeChar);
		final String delimiterCharString = Character.toString(delimiterChar);
		final String splitRegex = "(?<!" + Pattern.quote(escapeCharString) + ")" + Pattern.quote(delimiterCharString);
		final String[] returnParts = text.split(splitRegex);
		for (int i = 0; i < returnParts.length; i++) {
			// An escaped delimiter becomes the plain delimiter (before, the delimiter was dropped and the escape character kept)
			returnParts[i] = returnParts[i].replace(escapeCharString + delimiterCharString, delimiterCharString);
		}
		return returnParts;
	}

	/**
	 * Split a command line into arguments at blanks, tabs and linebreaks.
	 * Like in a shell, quoted parts (single or double quotes) may contain blanks and are part of the surrounding argument,
	 * e.g. {@code --name="a b"} becomes {@code --name=a b}. Within quotes a backslash escapes the quote character.
	 *
	 * @param argumentLine command line, must not be null
	 * @return list of arguments
	 * @throws Exception if a quote is not closed
	 */
	public static List<String> parseArguments(final String argumentLine) throws Exception {
		final List<String> returnList = new ArrayList<>();
		StringBuilder currentArgument = new StringBuilder();
		Character enclosingChar = null;
		boolean escapedNextChar = false;
		boolean argumentStarted = false;
		for (final char nextChar : argumentLine.replace("\r\n", "\n").replace("\r", "\n").toCharArray()) {
			if (enclosingChar != null) {
				if (escapedNextChar) {
					if (nextChar != enclosingChar) {
						currentArgument.append("\\");
					}
					currentArgument.append(nextChar);
					escapedNextChar = false;
				} else if (nextChar == '\\') {
					escapedNextChar = true;
				} else if (nextChar == enclosingChar) {
					// Like in a shell the quoted part continues the current argument, e.g. --name="a b" becomes --name=a b
					enclosingChar = null;
				} else {
					currentArgument.append(nextChar);
				}
			} else {
				if (nextChar == '\'' || nextChar == '"') {
					enclosingChar = nextChar;
					// Also an empty quoted text like "" is an argument
					argumentStarted = true;
				} else if (nextChar == ' ' || nextChar == '\n' || nextChar == '\t') {
					if (argumentStarted) {
						returnList.add(currentArgument.toString());
						currentArgument = new StringBuilder();
						argumentStarted = false;
					}
				} else {
					currentArgument.append(nextChar);
					argumentStarted = true;
				}
			}
		}
		if (enclosingChar != null) {
			throw new Exception("Invalid quotation. Missing closing " + enclosingChar);
		} else if (argumentStarted) {
			returnList.add(currentArgument.toString());
		}
		return returnList;
	}

	/**
	 * Split a text into tokens at the given delimiter characters.
	 * Like in a shell, quoted parts (single or double quotes) may contain delimiters and are part of the surrounding
	 * token, e.g. {@code key="a;b"} becomes {@code key=a;b}. Within quotes a backslash escapes the quote character.
	 *
	 * @param line text to split, must not be null
	 * @param delimiters characters separating the tokens
	 * @return list of tokens
	 * @throws Exception if a quote is not closed
	 */
	public static List<String> parseTokens(final String line, final char... delimiters) throws Exception {
		final List<String> returnList = new ArrayList<>();
		StringBuilder currentArgument = new StringBuilder();
		Character enclosingChar = null;
		boolean escapedNextChar = false;
		boolean argumentStarted = false;
		for (final char nextChar : line.replace("\r\n", "\n").replace("\r", "\n").toCharArray()) {
			if (enclosingChar != null) {
				if (escapedNextChar) {
					if (nextChar != enclosingChar) {
						currentArgument.append("\\");
					}
					currentArgument.append(nextChar);
					escapedNextChar = false;
				} else if (nextChar == '\\') {
					escapedNextChar = true;
				} else if (nextChar == enclosingChar) {
					// Like in a shell the quoted part continues the current argument, e.g. --name="a b" becomes --name=a b
					enclosingChar = null;
				} else {
					currentArgument.append(nextChar);
				}
			} else {
				if (nextChar == '\'' || nextChar == '"') {
					enclosingChar = nextChar;
					// Also an empty quoted text like "" is an argument
					argumentStarted = true;
				} else if (equalsAnyChar(nextChar, delimiters)) {
					if (argumentStarted) {
						returnList.add(currentArgument.toString());
						currentArgument = new StringBuilder();
						argumentStarted = false;
					}
				} else {
					currentArgument.append(nextChar);
					argumentStarted = true;
				}
			}
		}
		if (enclosingChar != null) {
			throw new Exception("Invalid quotation. Missing closing " + enclosingChar);
		} else if (argumentStarted) {
			returnList.add(currentArgument.toString());
		}
		return returnList;
	}

	/**
	 * Check whether a character equals any of the given characters.
	 *
	 * @param checkChar character to check
	 * @param compareChars characters to compare with
	 * @return true if the character is one of compareChars
	 */
	public static boolean equalsAnyChar(final char checkChar, final char... compareChars) {
		for (final char compareChar : compareChars) {
			if (checkChar == compareChar) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Create a modifiable list of strings.
	 *
	 * @param items strings for the list
	 * @return new ArrayList with the given strings
	 */
	public static List<String> getList(final String... items) {
		final List<String> returnList = new ArrayList<>();
		for (final String item : items) {
			returnList.add(item);
		}
		return returnList;
	}

	/**
	 * Get the system temp directory (system property "java.io.tmpdir").
	 *
	 * @return path of the temp directory
	 */
	public static String getSystemTempDir() {
		return System.getProperty("java.io.tmpdir");
	}

	/**
	 * Reverse the order of the items of an array in place.
	 *
	 * @param <T> type of the items
	 * @param arrayData array to reverse, must not be null
	 * @return the given array, now reversed
	 */
	public static <T> T[] reverseArray(final T[] arrayData) {
		for (int i = 0; i < arrayData.length / 2; i++) {
			final int j = arrayData.length - 1 - i;
			final T tmp = arrayData[i];
			arrayData[i] = arrayData[j];
			arrayData[j] = tmp;
		}
		return arrayData;
	}

	/**
	 * Reverse the order of the values of an array in place.
	 *
	 * @param arrayData array to reverse, must not be null
	 * @return the given array, now reversed
	 */
	public static int[] reverseArray(final int[] arrayData) {
		for (int i = 0; i < arrayData.length / 2; i++) {
			final int j = arrayData.length - 1 - i;
			final int tmp = arrayData[i];
			arrayData[i] = arrayData[j];
			arrayData[j] = tmp;
		}
		return arrayData;
	}

	/**
	 * Reverse the order of the bytes of an array in place.
	 *
	 * @param arrayData array to reverse, must not be null
	 * @return the given array, now reversed
	 */
	public static byte[] reverseArray(final byte[] arrayData) {
		for (int i = 0; i < arrayData.length / 2; i++) {
			final int j = arrayData.length - 1 - i;
			final byte tmp = arrayData[i];
			arrayData[i] = arrayData[j];
			arrayData[j] = tmp;
		}
		return arrayData;
	}

	/**
	 * KeyStores saved with blank password should also contain certificates when opened with a null password.
	 * Special cacerts keystore of JDKs behave this way, because they are JKS keystores, but PKCS12 keystores do not behave this way.
	 * Those are fixed by this keystore copy job to also behave in the intended way to show their certificates when openend with null password.
	 *
	 * @param keyStoreFileWithNullPassword
	 *            PKCS12 keystore file with blank password, replaced by a JKS keystore; files with another password stay unchanged
	 * @throws Exception
	 *             if the keystore cannot be converted or written
	 */
	public static void convertPkcs12TrustStoreToJKS(final File keyStoreFileWithNullPassword) throws Exception {
		final KeyStore readKeyStore = KeyStore.getInstance("PKCS12");
		try (FileInputStream inputStream = new FileInputStream(keyStoreFileWithNullPassword)) {
			readKeyStore.load(inputStream, "".toCharArray());
		} catch (@SuppressWarnings("unused") final Exception e) {
			// KeyStore password is not blank, so do not fix anything
			return;
		}

		final KeyStore writeKeyStore = KeyStore.getInstance("JKS");
		writeKeyStore.load(null, null);

		// KeyStore password is null
		for (final String alias : Collections.list(readKeyStore.aliases())) {
			writeKeyStore.setCertificateEntry(alias, readKeyStore.getCertificate(alias));
		}
		// Create the new keystore data completely before overwriting the file, so the original file is not lost if storing fails
		// (before, the file was deleted first)
		final ByteArrayOutputStream keyStoreData = new ByteArrayOutputStream();
		writeKeyStore.store(keyStoreData, "".toCharArray());
		// Write into a temp file first and replace the original afterwards, so an interrupted write cannot damage it
		final File tempFile = File.createTempFile(keyStoreFileWithNullPassword.getName() + ".", ".tmp", keyStoreFileWithNullPassword.getAbsoluteFile().getParentFile());
		try {
			Files.write(tempFile.toPath(), keyStoreData.toByteArray());
			Files.move(tempFile.toPath(), keyStoreFileWithNullPassword.toPath(), StandardCopyOption.REPLACE_EXISTING);
		} finally {
			Files.deleteIfExists(tempFile.toPath());
		}
	}

	/**
	 * Fix the encoding of a String if it was stored in UTF-8 encoding but decoded with ISO-8859-1 encoding
	 *
	 * <p>Examples of byte data of wrongly encoded Umlauts and other special characters:</p>
	 * <pre>
	 * Ä: [-61, -124]
	 * ä: [-61, -92]
	 * ß: [-61, -97]
	 * è: [-61, -88]
	 * </pre>
	 *
	 * @param comment
	 *            text to fix
	 * @return text decoded as UTF-8 if a wrong encoding was detected, otherwise the unchanged text
	 */
	public static String fixStringEncodingIfNeeded(final String comment) {
		boolean wrongEncodingDetected = false;
		for (final char nextChar : comment.toCharArray()) {
			if (nextChar > 0xFF) {
				// Characters outside of ISO-8859-1 cannot result from wrongly decoded bytes, so the text is correct
				return comment;
			} else if (nextChar == 0xC3) {
				wrongEncodingDetected = true;
			}
		}
		if (wrongEncodingDetected) {
			// Only convert if the bytes are valid UTF-8, otherwise the text was correct (e.g. a real "Ã")
			try {
				return StandardCharsets.UTF_8.newDecoder()
						.onMalformedInput(CodingErrorAction.REPORT)
						.onUnmappableCharacter(CodingErrorAction.REPORT)
						.decode(ByteBuffer.wrap(comment.getBytes(StandardCharsets.ISO_8859_1)))
						.toString();
			} catch (@SuppressWarnings("unused") final CharacterCodingException e) {
				return comment;
			}
		} else {
			return comment;
		}
	}

	/**
	 * Join all non-blank strings of a list.
	 *
	 * @param listOfStrings strings to join, must not be null
	 * @param glue separator between the strings, null for none
	 * @return joined text
	 */
	public static String joinNotBlank(final List<String> listOfStrings, final String glue) {
		return join(listOfStrings.stream().filter(x -> isNotBlank(x)).collect(Collectors.toList()), glue);
	}

	/**
	 * Null-safe conversion to upper case, independent of the default locale.
	 * Locale.ROOT is used, because the default locale changes the result, e.g. "i" becomes the dotted "\u0130" in
	 * Turkish, which breaks comparisons of keywords, identifiers and file names.
	 *
	 * @param value text, may be null
	 * @return upper case text, or null for null input
	 */
	public static String upperCase(final String value) {
		if (value == null) {
			return null;
		} else {
			return value.toUpperCase(Locale.ROOT);
		}
	}

	/**
	 * Check whether an array contains a sequence of items (compared by equals).
	 *
	 * @param <T> type of the items
	 * @param dataArray array to search in, may be null
	 * @param searchArray sequence to search for, may be null
	 * @return true if the sequence is contained (always for an empty sequence), false if any array is null
	 */
	public static <T> boolean arrayContains(final T[] dataArray, final T[] searchArray) {
		if (dataArray == null || searchArray == null || dataArray.length < searchArray.length) {
			return false;
		} else if (searchArray.length == 0) {
			return true;
		} else {
			// Only start positions where the whole searchArray still fits into dataArray
			for (int i = 0; i <= dataArray.length - searchArray.length; i++) {
				int matchingItems = 0;
				while (matchingItems < searchArray.length && Objects.equals(dataArray[i + matchingItems], searchArray[matchingItems])) {
					matchingItems++;
				}
				if (matchingItems == searchArray.length) {
					return true;
				}
			}
			return false;
		}
	}

	/**
	 * Check whether a byte array contains a sequence of bytes.
	 *
	 * @param dataArray array to search in, may be null
	 * @param searchArray sequence to search for, may be null
	 * @return true if the sequence is contained (always for an empty sequence), false if any array is null
	 */
	public static boolean arrayContains(final byte[] dataArray, final byte[] searchArray) {
		if (dataArray == null || searchArray == null || dataArray.length < searchArray.length) {
			return false;
		} else if (searchArray.length == 0) {
			return true;
		} else {
			// Only start positions where the whole searchArray still fits into dataArray
			for (int i = 0; i <= dataArray.length - searchArray.length; i++) {
				int matchingItems = 0;
				while (matchingItems < searchArray.length && dataArray[i + matchingItems] == searchArray[matchingItems]) {
					matchingItems++;
				}
				if (matchingItems == searchArray.length) {
					return true;
				}
			}
			return false;
		}
	}

	/**
	 * Inserts line breaks, so no line of the text is longer than the maximum line length.
	 * Existing line breaks are kept and start a new line. Words are not respected, lines are broken at any character.
	 *
	 * @param text
	 *            text to break
	 * @param maximumLinelength
	 *            maximum number of characters per line, values less than 1 leave the text unchanged
	 * @param linebreak
	 *            line break to insert, for example "\n" or "\r\n" (null means "\n")
	 * @return text with inserted line breaks, or null for null text
	 */
	public static String breakTextToMaximumLinelength(final String text, final int maximumLinelength, String linebreak) {
		if (linebreak == null) {
			linebreak = "\n";
		}

		if (text == null || maximumLinelength < 1) {
			return text;
		} else {
			final StringBuilder returnValue = new StringBuilder(text.length() + text.length() / maximumLinelength * linebreak.length());
			int currentLineLength = 0;
			for (final char nextChar : text.toCharArray()) {
				if (nextChar == '\r' || nextChar == '\n') {
					// Existing line break starts a new line
					currentLineLength = 0;
				} else {
					if (currentLineLength >= maximumLinelength) {
						returnValue.append(linebreak);
						currentLineLength = 0;
					}
					currentLineLength++;
				}
				returnValue.append(nextChar);
			}

			return returnValue.toString();
		}
	}

	/**
	 * Escape a text for use in a Java string literal.
	 * Backslash, double quote and control characters are escaped, all characters outside printable ASCII become unicode escapes.
	 *
	 * @param text text to escape, must not be null
	 * @return escaped text
	 */
	public static String escapeJavaString(final String text) {
		final StringBuilder escapedTextBuilder = new StringBuilder();

		for (final char nextChar : text.toCharArray()) {
			switch (nextChar) {
				case '\\':
					escapedTextBuilder.append("\\\\");
					break;
				case '\"':
					escapedTextBuilder.append("\\\"");
					break;
				case '\n':
					escapedTextBuilder.append("\\n");
					break;
				case '\r':
					escapedTextBuilder.append("\\r");
					break;
				case '\t':
					escapedTextBuilder.append("\\t");
					break;
				case '\b':
					escapedTextBuilder.append("\\b");
					break;
				case '\f':
					escapedTextBuilder.append("\\f");
					break;
				default:
					if (nextChar < 32 || nextChar > 126) {
						escapedTextBuilder.append(String.format("\\u%04X", (int) nextChar));
					} else {
						escapedTextBuilder.append(nextChar);
					}
			}
		}

		return escapedTextBuilder.toString();
	}

	/**
	 * Unescape a text escaped like a Java string literal.
	 * Supported escapes: \n \r \t \b \f \\ \' \", \xHH (8 bit), &#92;uHHHH (16 bit) and \UHHHHHHHH (unicode code point).
	 *
	 * @param javaEscapedText escaped text, must not be null
	 * @return unescaped text
	 * @throws Exception if an escape sequence is invalid
	 */
	public static String unescapeJavaString(final String javaEscapedText) throws Exception {
		final StringBuilder unescapedTextBuilder = new StringBuilder();
		final int length = javaEscapedText.length();

		for (int i = 0; i < length; i++) {
			final char nextChar = javaEscapedText.charAt(i);

			if (nextChar == '\\' && i + 1 < length) {
				final char oneMoreChar = javaEscapedText.charAt(i + 1);
				switch (oneMoreChar) {
					case 'n':
						unescapedTextBuilder.append('\n');
						i++;
						break;
					case 'r':
						unescapedTextBuilder.append('\r');
						i++;
						break;
					case 't':
						unescapedTextBuilder.append('\t');
						i++;
						break;
					case 'b':
						unescapedTextBuilder.append('\b');
						i++;
						break;
					case 'f':
						unescapedTextBuilder.append('\f');
						i++;
						break;
					case '\\':
						unescapedTextBuilder.append('\\');
						i++;
						break;
					case '\'':
						unescapedTextBuilder.append('\'');
						i++;
						break;
					case '\"':
						unescapedTextBuilder.append('\"');
						i++;
						break;
					case 'x': // hexadecimal escapes: 8-bit size
						if (i + 3 < length) {
							final String hex = javaEscapedText.substring(i + 2, i + 4);
							try {
								final int code = Integer.parseInt(hex, 16);
								unescapedTextBuilder.append((char) code);
								i += 3;
							} catch (final NumberFormatException e) {
								throw new Exception("Invalid hex sequence at character index " + i + " ('" + "\\x" + hex + "')", e);
							}
						} else {
							final String invalidHex = javaEscapedText.substring(i + 2);
							throw new Exception("Invalid unicode sequence at character index " + i + " ('" + "\\x" + invalidHex + "')");
						}
						break;
					case 'u': // Java escapes: 16-bit size
						if (i + 5 < length) {
							final String hex = javaEscapedText.substring(i + 2, i + 6);
							try {
								final int code = Integer.parseInt(hex, 16);
								unescapedTextBuilder.append((char) code);
								i += 5;
							} catch (final NumberFormatException e) {
								throw new Exception("Invalid unicode sequence at character index " + i + " ('" + "\\u" + hex + "')", e);
							}
						} else {
							final String invalidHex = javaEscapedText.substring(i + 2);
							throw new Exception("Invalid unicode sequence at character index " + i + " ('" + "\\u" + invalidHex + "')");
						}
						break;
					case 'U': // Unicode escapes: 32-bit size
						if (i + 9 < length) {
							final String hex = javaEscapedText.substring(i + 2, i + 10);
							try {
								// Radix 16 and full code point (before, radix 32 was used and the value was truncated to a char)
								final int codePoint = Integer.parseInt(hex, 16);
								if (!Character.isValidCodePoint(codePoint)) {
									throw new Exception("Invalid unicode code point at character index " + i + " ('" + "\\U" + hex + "')");
								}
								unescapedTextBuilder.appendCodePoint(codePoint);
								i += 9;
							} catch (final NumberFormatException e) {
								throw new Exception("Invalid unicode sequence at character index " + i + " ('" + "\\U" + hex + "')", e);
							}
						} else {
							final String invalidHex = javaEscapedText.substring(i + 2);
							throw new Exception("Invalid unicode sequence at character index " + i + " ('" + "\\U" + invalidHex + "')");
						}
						break;
					default:
						throw new Exception("Invalid escape sequence at character index " + i + " ('" + "\\" + oneMoreChar + "')");
				}
			} else {
				unescapedTextBuilder.append(nextChar);
			}
		}

		return unescapedTextBuilder.toString();
	}

	/**
	 * Escape a text for use as a value in a .properties file.
	 * Only escapes that are valid for java.util.Properties are used.
	 * Double quotes have no special meaning in properties files and are not escaped.
	 *
	 * @param text
	 *            text to escape
	 * @return escaped text, non-ASCII and control characters as unicode escapes
	 */
	public static String escapePropertiesString(final String text) {
		final StringBuilder escapedTextBuilder = new StringBuilder();

		boolean isLeadingPosition = true;
		for (final char nextChar : text.toCharArray()) {
			if (isLeadingPosition && nextChar == ' ') {
				// java.util.Properties removes leading blanks of a value, unless they are escaped
				escapedTextBuilder.append("\\ ");
				continue;
			}
			isLeadingPosition = false;
			switch (nextChar) {
				case '\\':
					escapedTextBuilder.append("\\\\");
					break;
				case '\n':
					escapedTextBuilder.append("\\n");
					break;
				case '\r':
					escapedTextBuilder.append("\\r");
					break;
				case '\t':
					escapedTextBuilder.append("\\t");
					break;
				case '\f':
					escapedTextBuilder.append("\\f");
					break;
				default:
					// "\b" is not a valid properties escape, so backspace and other control chars become unicode escapes
					if (nextChar < 32 || nextChar > 126) {
						escapedTextBuilder.append(String.format("\\u%04X", (int) nextChar));
					} else {
						escapedTextBuilder.append(nextChar);
					}
			}
		}

		return escapedTextBuilder.toString();
	}

	/**
	 * Unescape a text escaped like a value in a .properties file.
	 * Supported escapes: \n \r \t \b \f, escaped blank, \\ \' \", \xHH (8 bit), &#92;uHHHH (16 bit) and \UHHHHHHHH (unicode code point).
	 * A backslash before any other character is removed.
	 *
	 * @param propertiesEscapedText escaped text, must not be null
	 * @return unescaped text
	 * @throws Exception if a hex or unicode escape sequence is invalid
	 */
	public static String unescapePropertiesString(final String propertiesEscapedText) throws Exception {
		final StringBuilder unescapedTextBuilder = new StringBuilder();
		final int length = propertiesEscapedText.length();

		for (int i = 0; i < length; i++) {
			final char nextChar = propertiesEscapedText.charAt(i);

			if (nextChar == '\\' && i + 1 < length) {
				final char oneMoreChar = propertiesEscapedText.charAt(i + 1);
				switch (oneMoreChar) {
					case 'n':
						unescapedTextBuilder.append('\n');
						i++;
						break;
					case 'r':
						unescapedTextBuilder.append('\r');
						i++;
						break;
					case 't':
						unescapedTextBuilder.append('\t');
						i++;
						break;
					case 'b':
						unescapedTextBuilder.append('\b');
						i++;
						break;
					case 'f':
						unescapedTextBuilder.append('\f');
						i++;
						break;
					case ' ':
						unescapedTextBuilder.append(' ');
						i++;
						break;
					case '\\':
						unescapedTextBuilder.append('\\');
						i++;
						break;
					case '\'':
						unescapedTextBuilder.append('\'');
						i++;
						break;
					case '\"':
						unescapedTextBuilder.append('\"');
						i++;
						break;
					case 'x': // hexadecimal escapes: 8-bit size
						if (i + 3 < length) {
							final String hex = propertiesEscapedText.substring(i + 2, i + 4);
							try {
								final int code = Integer.parseInt(hex, 16);
								unescapedTextBuilder.append((char) code);
								i += 3;
							} catch (final NumberFormatException e) {
								throw new Exception("Invalid hex sequence at character index " + i + " ('" + "\\x" + hex + "')", e);
							}
						} else {
							final String invalidHex = propertiesEscapedText.substring(i + 2);
							throw new Exception("Invalid unicode sequence at character index " + i + " ('" + "\\x" + invalidHex + "')");
						}
						break;
					case 'u': // Java escapes: 16-bit size
						if (i + 5 < length) {
							final String hex = propertiesEscapedText.substring(i + 2, i + 6);
							try {
								final int code = Integer.parseInt(hex, 16);
								unescapedTextBuilder.append((char) code);
								i += 5;
							} catch (final NumberFormatException e) {
								throw new Exception("Invalid unicode sequence at character index " + i + " ('" + "\\u" + hex + "')", e);
							}
						} else {
							final String invalidHex = propertiesEscapedText.substring(i + 2);
							throw new Exception("Invalid unicode sequence at character index " + i + " ('" + "\\u" + invalidHex + "')");
						}
						break;
					case 'U': // Unicode escapes: 32-bit size
						if (i + 9 < length) {
							final String hex = propertiesEscapedText.substring(i + 2, i + 10);
							try {
								final int codePoint = Integer.parseInt(hex, 16);
								if (!Character.isValidCodePoint(codePoint)) {
									throw new Exception("Invalid unicode code point at character index " + i + " ('" + "\\U" + hex + "')");
								}
								unescapedTextBuilder.appendCodePoint(codePoint);
								i += 9;
							} catch (final NumberFormatException e) {
								throw new Exception("Invalid unicode sequence at character index " + i + " ('" + "\\U" + hex + "')", e);
							}
						} else {
							final String invalidHex = propertiesEscapedText.substring(i + 2);
							throw new Exception("Invalid unicode sequence at character index " + i + " ('" + "\\U" + invalidHex + "')");
						}
						break;
					default:
						unescapedTextBuilder.append(oneMoreChar);
						i++;
						break;
				}
			} else {
				unescapedTextBuilder.append(nextChar);
			}
		}

		return unescapedTextBuilder.toString();
	}

	/**
	 * Surround a single-line text by a box of characters, e.g. for console output.
	 *
	 * @param text single-line text, must not be null
	 * @param boxCharacter character of the box border
	 * @return three lines of text: border, text line, border
	 */
	public static String getBoxedString(final String text, final char boxCharacter) {
		String returnString = "";
		final String boxCharacterString = Character.toString(boxCharacter);
		returnString += boxCharacterString.repeat(2) + boxCharacterString.repeat(text.length()) + boxCharacterString.repeat(2) + "\n";
		returnString += boxCharacterString + " " + text + " " + boxCharacterString + "\n";
		returnString += boxCharacterString.repeat(2) + boxCharacterString.repeat(text.length()) + boxCharacterString.repeat(2);
		return returnString;
	}

	/**
	 * Collect the remaining items of an iterator into a list.
	 *
	 * @param <T> type of the items
	 * @param iterator iterator to read
	 * @return new list with the remaining items
	 */
	public static <T> List<T> getListForIterator(final Iterator<T> iterator) {
		final List<T> list = new ArrayList<>();
		iterator.forEachRemaining(list::add);
		return list;
	}

	/**
	 * Collect the remaining items of an enumeration into a list.
	 *
	 * @param <T> type of the items
	 * @param enumeration enumeration to read
	 * @return new list with the remaining items
	 */
	public static <T> List<T> getListForEnumeration(final Enumeration<T> enumeration) {
		return Collections.list(enumeration);
	}
}
