package de.soderer.utilities;

import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;

/**
 * Date utilities for database related formats.
 */
public class DateUtilitiesForDb {
	/** Date time format pattern "yyyy-MM-dd HH:mm:ss". */
	public static final String YYYY_MM_DD_HHMMSS = "yyyy-MM-dd HH:mm:ss";

	/**
	 * Utility class
	 */
	private DateUtilitiesForDb() {
		// Utility class
	}

	/**
	 * Formats a timestamp with microsecond precision (the fraction of a second as used by .Net),
	 * interpreted in the system default time zone.
	 *
	 * @param ts
	 *            the timestamp to format, may be null
	 * @return a String in format "yyyy-MM-dd HH:mm:ss.NNNNNN" (6 digits of the fraction of a second,
	 *         further digits are truncated), or an empty String if the timestamp is null
	 */
	public static String formatTimestamp_yyyyMMdd_HHmmssNNNNNN(final Timestamp ts) {
		if (ts == null) {
			return "";
		} else {
			final String s = DateTimeFormatter.ofPattern(YYYY_MM_DD_HHMMSS).format(DateUtilities.getLocalDateTimeForDate(ts));
			// Nanos must be left padded to 9 digits before truncating to microseconds, e.g. 5000 nanos are "000005"
			final String microsString = String.format("%09d", ts.getNanos()).substring(0, 6);
			return s + "." + microsString;
		}
	}
}
