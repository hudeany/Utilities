package de.soderer.utilities;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.soderer.utilities.collection.CaseInsensitiveLinkedMap;

/**
 * Simple INI-like properties file with sections, read and written in UTF-8:
 *
 * <pre>
 * # comment
 * globalKey = value
 * [sectionName]
 * key = value
 * </pre>
 *
 * <p>
 * Keys before the first section belong to the section null. Keys, values and section names are trimmed, so leading
 * and trailing blanks are not preserved. A key must not be blank, must not contain '=' or line breaks and must not
 * start with '#' or '['. A section name must not contain line breaks. A leading byte order mark is ignored.
 * </p>
 *
 * <p>
 * Line breaks in values are written as "\n". A run of backslashes directly before an 'n' or a line break is doubled
 * when writing, so values containing a literal "\n" (like "C:\new") are read back unchanged. All other backslashes are
 * written as they are, so files written by older versions of this class (which did not escape backslashes) are read the
 * same way as before, except for values containing two or more backslashes directly followed by 'n'.
 * </p>
 */
public class SectionedProperties {
	private static final String COMMENT_PREFIX = "#";
	/**
	 * Blanks around KEY_VALUE_SEPARATOR are ignored.
	 */
	private static final String KEY_VALUE_SEPARATOR = "=";
	private static final String SECTION_PREFIX = "[";
	private static final String SECTION_SUFFIX = "]";

	/** Byte order mark, which is ignored at the beginning of the data */
	private static final char BYTE_ORDER_MARK = '\uFEFF';

	/** A run of backslashes followed by 'n' in the stored form */
	private static final Pattern ESCAPED_N_PATTERN = Pattern.compile("(\\\\+)n");

	/** A run of backslashes followed by 'n' or a line break in the unescaped value */
	private static final Pattern BACKSLASHES_BEFORE_N_PATTERN = Pattern.compile("(\\\\+)(?=[n\\n])");

	private boolean useCaseInsensitiveKeys = true;
	private Map<String, Map<String, String>> entries = null;

	/**
	 * Creates empty properties with case insensitive section names and keys.
	 */
	public SectionedProperties() {
	}

	/**
	 * Creates empty properties.
	 *
	 * @param useCaseInsensitiveKeys true for case insensitive section names and keys
	 */
	public SectionedProperties(final boolean useCaseInsensitiveKeys) {
		this.useCaseInsensitiveKeys = useCaseInsensitiveKeys;
	}

	/**
	 * Loads properties from a UTF-8 stream, replacing all current entries. The stream is not closed.
	 * Sections without keys are kept, so their names are contained in {@link #getSectionNames()}.
	 *
	 * @param inputStream stream to read
	 * @throws Exception if a line is invalid (unterminated section name or empty key) or the stream cannot be read
	 */
	public void load(final InputStream inputStream) throws Exception {
		if (inputStream == null) {
			throw new IllegalArgumentException("InputStream must not be null");
		}

		// Read into a new map first, so the current entries are kept, if the data is invalid
		final Map<String, Map<String, String>> loadedEntries = createMap();

		final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
		String rawLine = null;
		String currentSectionName = null;
		int lineCount = 0;
		while ((rawLine = reader.readLine()) != null) {
			lineCount++;
			if (lineCount == 1 && !rawLine.isEmpty() && rawLine.charAt(0) == BYTE_ORDER_MARK) {
				// Otherwise the BOM would become part of the first key or hide the first section or comment line
				rawLine = rawLine.substring(1);
			}
			final String line = rawLine.trim();
			if (Utilities.isNotBlank(line) && !line.startsWith(COMMENT_PREFIX)) {
				if (line.startsWith(SECTION_PREFIX)) {
					if (line.endsWith(SECTION_SUFFIX)) {
						currentSectionName = line.substring(1, line.length() - 1).trim();
						getOrCreateSection(loadedEntries, currentSectionName);
					} else {
						throw new Exception("Invalid section line (" + lineCount + "): " + rawLine);
					}
				} else {
					String key;
					String value;
					final int posSeparator = line.indexOf(KEY_VALUE_SEPARATOR);
					if (posSeparator < 0) {
						key = line;
						value = "";
					} else {
						key = line.substring(0, posSeparator).trim();
						value = unescapeValue(line.substring(posSeparator + 1).trim());
					}

					if (key.length() <= 0) {
						throw new Exception("Invalid key at line " + lineCount);
					}

					getOrCreateSection(loadedEntries, currentSectionName).put(key, value);
				}
			}
		}

		entries = loadedEntries;
	}

	private <T> Map<String, T> createMap() {
		if (useCaseInsensitiveKeys) {
			return new CaseInsensitiveLinkedMap<>();
		} else {
			return new LinkedHashMap<>();
		}
	}

	private Map<String, String> getOrCreateSection(final Map<String, Map<String, String>> sections, final String sectionName) {
		Map<String, String> section = sections.get(sectionName);
		if (section == null) {
			section = createMap();
			sections.put(sectionName, section);
		}
		return section;
	}

	/**
	 * Writes all properties in UTF-8 to a stream. Entries of the section null (keys without section) are written first,
	 * without section line. Null values are written as empty values. The stream is not closed.
	 *
	 * @param outputStream stream to write to
	 * @throws Exception if the stream cannot be written
	 */
	public void save(final OutputStream outputStream) throws Exception {
		if (outputStream == null) {
			throw new IllegalArgumentException("OutputStream must not be null");
		} else if (entries == null) {
			return;
		}

		// Buffered, but not closed, because the stream belongs to the caller
		final Writer writer = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));

		// Keys without section must be written before the first section line, otherwise they would be read into that section
		final Map<String, String> entriesWithoutSection = entries.get(null);
		if (entriesWithoutSection != null) {
			writeSectionEntries(writer, entriesWithoutSection);
			writer.write("\n");
		}

		for (final Entry<String, Map<String, String>> sectionEntry : entries.entrySet()) {
			if (sectionEntry.getKey() != null) {
				writer.write(SECTION_PREFIX + sectionEntry.getKey().trim() + SECTION_SUFFIX + "\n");
				writeSectionEntries(writer, sectionEntry.getValue());
				writer.write("\n");
			}
		}

		writer.flush();
	}

	private static void writeSectionEntries(final Writer writer, final Map<String, String> sectionEntries) throws Exception {
		for (final Entry<String, String> keyValueEntry : sectionEntries.entrySet()) {
			// Trimmed after escaping, so line breaks at the start or end of a value are kept as "\n"
			final String value = keyValueEntry.getValue() == null ? "" : escapeValue(keyValueEntry.getValue()).trim();
			writer.write(keyValueEntry.getKey().trim() + KEY_VALUE_SEPARATOR + value + "\n");
		}
	}

	/**
	 * Names of all sections (including null for keys without section).
	 *
	 * @return live set of section names, or null if nothing was loaded or set yet
	 */
	public Set<String> getSectionNames() {
		if (entries != null) {
			return entries.keySet();
		} else {
			return null;
		}
	}

	/**
	 * Keys of a section.
	 *
	 * @param sectionName name of the section, or null for keys without section
	 * @return live set of keys, or null if the section does not exist
	 */
	public Set<String> getSectionKeys(final String sectionName) {
		if (entries != null) {
			final Map<String, String> section = entries.get(normalizeSectionName(sectionName));
			if (section != null) {
				return section.keySet();
			} else {
				return null;
			}
		} else {
			return null;
		}
	}

	/**
	 * Value of a key in a section.
	 *
	 * @param sectionName name of the section, or null for keys without section
	 * @param key the key
	 * @return the value, or null if the section or key does not exist
	 */
	public String getValue(final String sectionName, final String key) {
		if (entries != null && key != null) {
			final Map<String, String> section = entries.get(normalizeSectionName(sectionName));
			if (section != null) {
				return section.get(key.trim());
			} else {
				return null;
			}
		} else {
			return null;
		}
	}

	/**
	 * Sets the value of a key in a section, creating the section if needed. Section name and key are trimmed, like
	 * they are when saving and loading.
	 *
	 * @param sectionName name of the section, or null for keys without section. Must not contain line breaks.
	 * @param key the key, must not be blank, contain '=' or line breaks or start with '#' or '['
	 * @param value the value (null is saved as empty value)
	 * @throws IllegalArgumentException if the section name or key cannot be saved and loaded again
	 */
	public void setValue(final String sectionName, final String key, final String value) {
		final String normalizedSectionName = normalizeSectionName(sectionName);
		if (normalizedSectionName != null && (normalizedSectionName.contains("\n") || normalizedSectionName.contains("\r"))) {
			throw new IllegalArgumentException("Section name must not contain line breaks: " + sectionName);
		}
		final String normalizedKey = key == null ? null : key.trim();
		if (normalizedKey == null || normalizedKey.isEmpty()) {
			throw new IllegalArgumentException("Key must not be blank");
		} else if (normalizedKey.contains(KEY_VALUE_SEPARATOR) || normalizedKey.contains("\n") || normalizedKey.contains("\r")
				|| normalizedKey.startsWith(COMMENT_PREFIX) || normalizedKey.startsWith(SECTION_PREFIX)) {
			// Such a key would be read as different key, as comment or as section line, or would make the file unreadable
			throw new IllegalArgumentException("Invalid key: " + key);
		}

		if (entries == null) {
			entries = createMap();
		}
		getOrCreateSection(entries, normalizedSectionName).put(normalizedKey, value);
	}

	private static String normalizeSectionName(final String sectionName) {
		return sectionName == null ? null : sectionName.trim();
	}

	/**
	 * Unescapes a stored value: a run of k backslashes followed by 'n' becomes k/2 backslashes followed by a line break
	 * (odd k) or by 'n' (even k). All other characters are kept.
	 */
	private static String unescapeValue(final String value) {
		final Matcher matcher = ESCAPED_N_PATTERN.matcher(value);
		final StringBuilder result = new StringBuilder();
		while (matcher.find()) {
			final int backslashCount = matcher.group(1).length();
			final String replacement = "\\".repeat(backslashCount / 2) + (backslashCount % 2 == 1 ? "\n" : "n");
			matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
		}
		matcher.appendTail(result);
		return result.toString();
	}

	/**
	 * Escapes a value for storing: line breaks become "\n" and runs of backslashes directly before an 'n' or a line
	 * break are doubled (counterpart of {@link #unescapeValue(String)}).
	 */
	private static String escapeValue(final String value) {
		final String normalizedValue = value.replace("\r\n", "\n").replace("\r", "\n");
		final Matcher matcher = BACKSLASHES_BEFORE_N_PATTERN.matcher(normalizedValue);
		final StringBuilder result = new StringBuilder();
		while (matcher.find()) {
			matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group(1) + matcher.group(1)));
		}
		matcher.appendTail(result);
		return result.toString().replace("\n", "\\n");
	}
}
