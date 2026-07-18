package de.soderer.utilities;

import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;

public class DateUtilitiesForDb {
	public static final String YYYY_MM_DD_HHMMSS = "yyyy-MM-dd HH:mm:ss";

	/**
	 * Format a timestamp (Nanos in .Net format)
	 *
	 * @param ts
	 *            a Timestamp
	 * @return a String in format 'yyyy-mm-dd HH:MM:SS.NNNNNN'
	 */
	public static String formatTimestamp_yyyyMMdd_HHmmssNNNNNN(final Timestamp ts) {
		String returnString = "";

		if (ts != null) {
			final String s = DateTimeFormatter.ofPattern(YYYY_MM_DD_HHMMSS).format(DateUtilities.getLocalDateTimeForDate(ts));
			String nanosString = Integer.toString(ts.getNanos());

			if (nanosString.length() > 6) {
				nanosString = nanosString.substring(0, 6);
			} else {
				while (nanosString.length() < 6) {
					nanosString += "0";
				}
			}

			returnString = s + nanosString;
		}

		return returnString;
	}
}
