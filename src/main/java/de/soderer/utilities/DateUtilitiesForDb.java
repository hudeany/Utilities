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
	 * Formatter for "yyyy-MM-dd HH:mm:ss.NNNNNN". Uses the proleptic year "uuuu", because the year of era "yyyy"
	 * would print years before 1 without sign (year 0 as "0001"). The fraction "SSSSSS" is truncated, not rounded.
	 * DateTimeFormatter is immutable and thread safe and always prints ASCII digits.
	 */
	private static final DateTimeFormatter TIMESTAMP_MICROSECONDS_FORMATTER = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss.SSSSSS");

	/**
	 * Utility class
	 */
	private DateUtilitiesForDb() {
		// Utility class
	}

	/**
	 * Formats a timestamp with microsecond precision (the fraction of a second as used by .Net).
	 * <p>
	 * The date and time fields of the timestamp are used as they are (like {@link Timestamp#toString()}), without a
	 * conversion via the instant. A conversion via the instant would shift old dates, because java.sql.Timestamp uses
	 * the Julian calendar before 1582 and the local mean time offsets of the time zone before about 1900, e.g.
	 * "0001-01-01 00:00:00" would become "0001-12-29 23:53:28" in time zone Europe/Berlin.
	 * </p>
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
			return TIMESTAMP_MICROSECONDS_FORMATTER.format(ts.toLocalDateTime());
		}
	}
}
