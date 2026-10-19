package de.soderer.utilities;

import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoUnit;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilities for dates and times: formatting, parsing, time zone conversion, human readable durations
 * and the calculation of scheduled job start times.
 */
public class DateUtilities {
	/**
	 * Date time format pattern "dd.MM.yyyy HH:mm:ss"
	 */
	public static final String DD_MM_YYYY_HH_MM_SS = "dd.MM.yyyy HH:mm:ss";
	/**
	 * Date time format pattern with time zone name "dd.MM.yyyy HH:mm:ss z"
	 */
	public static final String DD_MM_YYYY_HH_MM_SS_Z = "dd.MM.yyyy HH:mm:ss z";
	/**
	 * Date time format pattern "dd.MM.yyyy HH:mm"
	 */
	public static final String DD_MM_YYYY_HH_MM = "dd.MM.yyyy HH:mm";
	/**
	 * Date format pattern "dd.MM.yyyy"
	 */
	public static final String DD_MM_YYYY = "dd.MM.yyyy";
	/**
	 * Date format pattern "ddMMyyyy"
	 */
	public static final String DDMMYYYY = "ddMMyyyy";
	/**
	 * Date format pattern "yyyyMMdd"
	 */
	public static final String YYYYMMDD = "yyyyMMdd";
	/**
	 * Time format pattern "HHmmss"
	 */
	public static final String HHMMSS = "HHmmss";
	/**
	 * Date format pattern "yyyy-MM-dd"
	 */
	public static final String YYYY_MM_DD = "yyyy-MM-dd";
	/**
	 * Date time format pattern "yyyy-MM-dd HH:mm"
	 */
	public static final String YYYY_MM_DD_HH_MM = "yyyy-MM-dd HH:mm";
	/**
	 * Date time format pattern usable in file names "dd_MM_yyyy_HH_mm_ss"
	 */
	public static final String DD_MM_YYYY_HH_MM_SS_ForFileName = "dd_MM_yyyy_HH_mm_ss";
	/**
	 * Date time format pattern "yyyyMMddHHmmss"
	 */
	public static final String YYYYMMDDHHMMSS = "yyyyMMddHHmmss";
	/**
	 * Date time format pattern with milliseconds "yyyyMMddHHmmssSSS"
	 */
	public static final String YYYYMMDDHHMMSSSSS = "yyyyMMddHHmmssSSS";
	/**
	 * Date time format pattern "yyyy-MM-dd HH:mm:ss"
	 */
	public static final String YYYY_MM_DD_HHMMSS = "yyyy-MM-dd HH:mm:ss";
	/**
	 * Date time format pattern "yyyyMMdd-HHmmss"
	 */
	public static final String YYYYMMDD_HHMMSS = "yyyyMMdd-HHmmss";
	/**
	 * Time format pattern "HHmm"
	 */
	public static final String HHMM = "HHmm";

	/**
	 * Utility class
	 */
	private DateUtilities() {
		// Utility class
	}

	private static final Pattern MONTH_RULE_PATTERN = Pattern.compile("\\d{0,2}M\\d{2}:\\d{4}");
	private static final Pattern WEEKDAILY_RULE_PATTERN = Pattern.compile("\\d\\D\\D:\\d{4}");
	private static final Pattern TIME_WILDCARD_PATTERN = Pattern.compile("[0-9*]{4}");
	private static final Pattern ISO_8601_OFFSET_PATTERN = Pattern.compile("([+-])(\\d{2})(?::?(\\d{2}))?");

	/** Date format for ISO 8601 */
	public static final String ISO_8601_DATE_FORMAT_NO_TIMEZONE = "yyyy-MM-dd";
	/** Date format for ISO 8601 */
	public static final String ISO_8601_DATE_FORMAT = "yyyy-MM-ddX";
	/** DateTime format for ISO 8601 */
	public static final String ISO_8601_DATETIME_FORMAT_NO_TIMEZONE = "yyyy-MM-dd'T'HH:mm:ss";
	/**
	 * DateTime format for ISO 8601 with nanoseconds (always 9 fraction digits when formatting). For parsing
	 * values with a variable number of fraction digits use {@link #parseIso8601DateTimeString(String)}.
	 */
	public static final String ISO_8601_DATETIME_WITH_NANOS_FORMAT_NO_TIMEZONE = "yyyy-MM-dd'T'HH:mm:ss[.SSSSSSSSS]";
	/** DateTime format for ISO 8601 */
	public static final String ISO_8601_DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ssX";
	/**
	 * DateTime format for ISO 8601 with nanoseconds (always 9 fraction digits when formatting). For parsing
	 * values with a variable number of fraction digits use {@link #parseIso8601DateTimeString(String)}.
	 */
	public static final String ISO_8601_DATETIME_WITH_NANOS_FORMAT = "yyyy-MM-dd'T'HH:mm:ss[.SSSSSSSSS]X";
	/** DateTime format for ISO 8601 */
	public static final String ISO_8601_TIME_FORMAT_NO_TIMEZONE = "HH:mm:ss";
	/** DateTime format for ISO 8601 */
	public static final String ISO_8601_TIME_WITH_NANOS_FORMAT_NO_TIMEZONE = "HH:mm:ss.SSSSSSSSS";
	/** DateTime format for ISO 8601 */
	public static final String ISO_8601_TIME_FORMAT = "HH:mm:ssX";
	/** DateTime format for ISO 8601 */
	public static final String ISO_8601_TIME_WITH_NANOS_FORMAT = "HH:mm:ss.SSSSSSSSSX";

	/** ANSI SQL standard date time format */
	public static final String ANSI_SQL_DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

	/** ANSI SQL standard date format */
	public static final String ANSI_SQL_DATE_FORMAT = "yyyy-MM-dd";

	/**
	 * Returns the first two letters of the weekday name of a date in the default locale.
	 *
	 * @param date
	 *            the date
	 * @return the abbreviated weekday name, like "Mo" or "Mi" for a German default locale
	 */
	public static String getWeekdayNameShort(final GregorianCalendar date) {
		final int dayInt = date.get(Calendar.DAY_OF_WEEK);
		final String dayString = DateFormatSymbols.getInstance().getWeekdays()[dayInt];
		return dayString.substring(0, 2);
	}

	/**
	 * Returns the weekday for the start of an English or German weekday name, ignoring case. English
	 * names are checked first, then two letter abbreviations of German and English names.
	 *
	 * @param weekDayPartString
	 *            the start of the weekday name with at least two letters, like "mo", "Tue" or "Mi"
	 * @return the weekday, or null if the text is blank, shorter than two letters (ambiguous like "s"
	 *         or "t") or no weekday matches
	 */
	public static DayOfWeek getDayOfWeekByNamePart(String weekDayPartString) {
		if (Utilities.isBlank(weekDayPartString)) {
			return null;
		} else {
			weekDayPartString = weekDayPartString.toLowerCase(Locale.ROOT).trim();
			if (weekDayPartString.length() < 2) {
				// A single letter is ambiguous ("s" for Saturday or Sunday, "t" for Tuesday or Thursday)
				return null;
			}
			for (final DayOfWeek dayOfWeek : DayOfWeek.values()) {
				if (dayOfWeek.name().toLowerCase(Locale.ROOT).startsWith(weekDayPartString)) {
					return dayOfWeek;
				}
			}

			if (weekDayPartString.startsWith("so") || weekDayPartString.startsWith("su")) {
				return DayOfWeek.SUNDAY;
			} else if (weekDayPartString.startsWith("mo")) {
				return DayOfWeek.MONDAY;
			} else if (weekDayPartString.startsWith("di") || weekDayPartString.startsWith("tu")) {
				return DayOfWeek.TUESDAY;
			} else if (weekDayPartString.startsWith("mi") || weekDayPartString.startsWith("we")) {
				return DayOfWeek.WEDNESDAY;
			} else if (weekDayPartString.startsWith("do") || weekDayPartString.startsWith("th")) {
				return DayOfWeek.THURSDAY;
			} else if (weekDayPartString.startsWith("fr")) {
				return DayOfWeek.FRIDAY;
			} else if (weekDayPartString.startsWith("sa")) {
				return DayOfWeek.SATURDAY;
			} else {
				return null;
			}
		}
	}

	/**
	 * Format a timestampString from format "dd.MM.yyyy" or "dd-MM-yyyy" to "yyyy-MM-dd"
	 *
	 * @param ddMMyyyyString
	 *            date string in format "dd.MM.yyyy" or "dd-MM-yyyy", may be null
	 * @return date string in format "yyyy-MM-dd", or null if the value is null
	 * @throws IllegalArgumentException
	 *             if the value does not have the expected format
	 */
	public static String convert_ddMMyyyy_to_yyyyMMdd(final String ddMMyyyyString) {
		if (ddMMyyyyString == null) {
			return null;
		} else if (!hasDateLayout(ddMMyyyyString, 2, 5)) {
			throw new IllegalArgumentException("Invalid date, expected format \"dd.MM.yyyy\" or \"dd-MM-yyyy\": " + ddMMyyyyString);
		}
		return ddMMyyyyString.substring(6, 10) + "-" + ddMMyyyyString.substring(3, 5) + "-" + ddMMyyyyString.substring(0, 2);
	}

	/**
	 * Format a timestampString from format "yyyy-MM-dd" or "yyyy.MM.dd" to "dd.MM.yyyy"
	 *
	 * @param yyyyMMddString
	 *            date string in format "yyyy-MM-dd" or "yyyy.MM.dd", may be null
	 * @return date string in format "dd.MM.yyyy", or null if the value is null
	 * @throws IllegalArgumentException
	 *             if the value does not have the expected format
	 */
	public static String convert_yyyyMMdd_to_ddMMyyyy(final String yyyyMMddString) {
		if (yyyyMMddString == null) {
			return null;
		} else if (!hasDateLayout(yyyyMMddString, 4, 7)) {
			throw new IllegalArgumentException("Invalid date, expected format \"yyyy-MM-dd\" or \"yyyy.MM.dd\": " + yyyyMMddString);
		}
		return yyyyMMddString.substring(8, 10) + "." + yyyyMMddString.substring(5, 7) + "." + yyyyMMddString.substring(0, 4);
	}

	/**
	 * Checks for a date text of 10 characters with separators '.' or '-' at the given positions and digits elsewhere.
	 */
	private static boolean hasDateLayout(final String value, final int firstSeparatorIndex, final int secondSeparatorIndex) {
		if (value.length() != 10) {
			return false;
		}
		for (int i = 0; i < value.length(); i++) {
			final char c = value.charAt(i);
			if (i == firstSeparatorIndex || i == secondSeparatorIndex) {
				if (c != '.' && c != '-') {
					return false;
				}
			} else if (c < '0' || c > '9') {
				return false;
			}
		}
		return true;
	}

	/**
	 * Replaces date placeholders in a text by the values of a date: [yyyy] or [YYYY] (year), [MM]
	 * (month), [dd] or [DD] (day), [HH] or [hh] (hour, 24h), [mm] (minute), [SS] or [ss] (second).
	 * Escaped brackets "\[" and "\]" are replaced by plain brackets afterwards.
	 *
	 * @param stringWithPattern
	 *            the text with placeholders, may be null
	 * @param localDateTime
	 *            the date to insert
	 * @return the text with replaced placeholders, or null if the text is null
	 */
	public static String replaceDatePatternInString(final String stringWithPattern, final LocalDateTime localDateTime) {
		if (stringWithPattern == null) {
			return null;
		} else {
			String returnString = stringWithPattern;
			returnString = returnString.replace("[yyyy]", String.format("%04d", localDateTime.getYear()));
			returnString = returnString.replace("[YYYY]", String.format("%04d", localDateTime.getYear()));
			returnString = returnString.replace("[MM]", String.format("%02d", localDateTime.getMonthValue()));
			returnString = returnString.replace("[dd]", String.format("%02d", localDateTime.getDayOfMonth()));
			returnString = returnString.replace("[DD]", String.format("%02d", localDateTime.getDayOfMonth()));
			returnString = returnString.replace("[HH]", String.format("%02d", localDateTime.getHour()));
			returnString = returnString.replace("[hh]", String.format("%02d", localDateTime.getHour()));
			returnString = returnString.replace("[mm]", String.format("%02d", localDateTime.getMinute()));
			returnString = returnString.replace("[SS]", String.format("%02d", localDateTime.getSecond()));
			returnString = returnString.replace("[ss]", String.format("%02d", localDateTime.getSecond()));
			returnString = returnString.replace("\\[", "[");
			returnString = returnString.replace("\\]", "]");
			return returnString;
		}
	}

	/**
	 * Estimates the end time of a running task by linear extrapolation of the progress so far.
	 *
	 * @param start
	 *            the start time of the task
	 * @param itemsToDo
	 *            the total number of items to process
	 * @param itemsDone
	 *            the number of items processed so far
	 * @return the estimated end time, the current time if all items are done, or null if the values
	 *         do not allow an estimation (no progress yet, invalid counts or start in the future)
	 */
	public static LocalDateTime calculateETA(final LocalDateTime start, final long itemsToDo, final long itemsDone) {
		if (start == null || itemsToDo <= 0 || itemsDone <= 0 || itemsToDo < itemsDone) {
			return null;
		} else {
			final LocalDateTime now = LocalDateTime.now();
			if (start.isAfter(now)) {
				return null;
			} else if (itemsDone >= itemsToDo) {
				return now;
			} else {
				final Duration durationSinceStartToNow = Duration.between(start, now);
				final Duration durationFromStartToEnd = Duration.ofNanos((long) ((double) itemsToDo / itemsDone * durationSinceStartToNow.toNanos()));
				final LocalDateTime estimatedEnd = start.plus(durationFromStartToEnd);
				return estimatedEnd;
			}
		}
	}

	/**
	 * Formats a duration in short form like "1h 02m 03s". Only the most significant parts are shown:
	 * seconds only below one hour, minutes only below one day, hours only below one week and days only
	 * below one year. A year is counted as 52 weeks.
	 *
	 * @param duration
	 *            the duration
	 * @param showMillis
	 *            true to show milliseconds for durations below one minute
	 * @param showLeadingZeros
	 *            true to pad the values with leading zeros
	 * @return the formatted duration, "0s" or "00s" for a zero duration
	 */
	public static String getShortHumanReadableTimespan(final Duration duration, final boolean showMillis, final boolean showLeadingZeros) {
		if (duration.isNegative()) {
			// The parts of a negative duration are negative, so the absolute value is formatted with a leading sign
			return "-" + getShortHumanReadableTimespan(duration.negated(), showMillis, showLeadingZeros);
		}

		final StringBuilder returnValue = new StringBuilder();

		final long millis = duration.toMillisPart();
		final long seconds = duration.toSecondsPart();
		final long minutes = duration.toMinutesPart();
		final long hours = duration.toHoursPart();
		final long days = duration.toDays() % 7;
		final long weeks = duration.toDays() / 7 % 52;
		final long years = duration.toDays() / 7 / 52;

		if (showMillis && millis != 0 && minutes == 0 && hours == 0 && days == 0 && weeks == 0 && years == 0) {
			returnValue.insert(0, "ms");
			if (showLeadingZeros) {
				returnValue.insert(0, String.format("%03d", millis));
			} else {
				returnValue.insert(0, millis);
			}
		}

		if (seconds != 0 && hours == 0 && days == 0 && weeks == 0 && years == 0) {
			if (returnValue.length() > 0) {
				returnValue.insert(0, " ");
			}
			returnValue.insert(0, "s");

			if (showLeadingZeros) {
				returnValue.insert(0, String.format("%02d", seconds));
			} else {
				returnValue.insert(0, seconds);
			}
		}

		if (minutes != 0 && days == 0 && weeks == 0 && years == 0) {
			if (returnValue.length() > 0) {
				returnValue.insert(0, " ");
			}
			returnValue.insert(0, "m");
			if (showLeadingZeros) {
				returnValue.insert(0, String.format("%02d", minutes));
			} else {
				returnValue.insert(0, minutes);
			}
		}

		if (hours != 0 && weeks == 0 && years == 0) {
			if (returnValue.length() > 0) {
				returnValue.insert(0, " ");
			}
			returnValue.insert(0, "h");
			if (showLeadingZeros) {
				returnValue.insert(0, String.format("%02d", hours));
			} else {
				returnValue.insert(0, hours);
			}
		}

		if (days != 0 && years == 0) {
			if (returnValue.length() > 0) {
				returnValue.insert(0, " ");
			}
			returnValue.insert(0, "d");
			if (showLeadingZeros) {
				returnValue.insert(0, String.format("%02d", days));
			} else {
				returnValue.insert(0, days);
			}
		}

		if (weeks != 0) {
			if (returnValue.length() > 0) {
				returnValue.insert(0, " ");
			}
			returnValue.insert(0, "w");
			if (showLeadingZeros) {
				returnValue.insert(0, String.format("%02d", weeks));
			} else {
				returnValue.insert(0, weeks);
			}
		}

		if (years != 0) {
			if (returnValue.length() > 0) {
				returnValue.insert(0, " ");
			}
			returnValue.insert(0, "y");
			returnValue.insert(0, years);
		}

		if (returnValue.length() > 0) {
			return returnValue.toString();
		} else {
			if (showLeadingZeros) {
				return "00s";
			} else {
				return "0s";
			}
		}
	}

	/**
	 * Formats a duration with localized unit names (see {@link LangResources}), like "1 hours 2 minutes".
	 * Parts with value 0 are omitted. A year is counted as 52 weeks.
	 *
	 * @param duration
	 *            the duration
	 * @param showMillis
	 *            true to show milliseconds
	 * @return the formatted duration
	 */
	public static String getHumanReadableTimespan(final Duration duration, final boolean showMillis) {
		if (duration.isNegative()) {
			// The parts of a negative duration are negative, so the absolute value is formatted with a leading sign
			return "-" + getHumanReadableTimespan(duration.negated(), showMillis);
		}

		final StringBuilder returnValue = new StringBuilder();

		final long millis = duration.toMillisPart();
		final long seconds = duration.toSecondsPart();
		final long minutes = duration.toMinutesPart();
		final long hours = duration.toHoursPart();
		final long days = duration.toDays() % 7;
		final long weeks = duration.toDays() / 7 % 52;
		final long years = duration.toDays() / 7 / 52;

		if (millis != 0 && showMillis) {
			returnValue.insert(0, " " + LangResources.get("millis"));
			returnValue.insert(0, millis);
		}

		if (seconds != 0) {
			if (returnValue.length() > 0) {
				returnValue.insert(0, " ");
			}
			returnValue.insert(0, " " + LangResources.get("seconds"));
			returnValue.insert(0, seconds);
		}

		if (minutes != 0) {
			if (returnValue.length() > 0) {
				returnValue.insert(0, " ");
			}
			returnValue.insert(0, " " + LangResources.get("minutes"));
			returnValue.insert(0, minutes);
		}

		if (hours != 0) {
			if (returnValue.length() > 0) {
				returnValue.insert(0, " ");
			}
			returnValue.insert(0, " " + LangResources.get("hours"));
			returnValue.insert(0, hours);
		}

		if (days != 0) {
			if (returnValue.length() > 0) {
				returnValue.insert(0, " ");
			}
			returnValue.insert(0, " " + LangResources.get("days"));
			returnValue.insert(0, days);
		}

		if (weeks != 0) {
			if (returnValue.length() > 0) {
				returnValue.insert(0, " ");
			}
			returnValue.insert(0, " " + LangResources.get("weeks"));
			returnValue.insert(0, weeks);
		}

		if (years != 0) {
			if (returnValue.length() > 0) {
				returnValue.insert(0, " ");
			}
			returnValue.insert(0, " " + LangResources.get("years"));
			returnValue.insert(0, years);
		}

		if (returnValue.length() > 0) {
			return returnValue.toString();
		} else if (!showMillis) {
			return "0 " + LangResources.get("seconds");
		} else {
			return "0 " + LangResources.get("millis");
		}
	}

	/**
	 * Formats a duration with English unit names, like "1 hours 2 minutes". Parts with value 0 are
	 * omitted. A year is counted as 52 weeks.
	 *
	 * @param duration
	 *            the duration
	 * @param showMillis
	 *            true to show milliseconds
	 * @return the formatted duration
	 */
	public static String getHumanReadableTimespanEnglish(final Duration duration, final boolean showMillis) {
		if (duration.isNegative()) {
			// The parts of a negative duration are negative, so the absolute value is formatted with a leading sign
			return "-" + getHumanReadableTimespanEnglish(duration.negated(), showMillis);
		}

		final StringBuilder returnValue = new StringBuilder();

		final long millis = duration.toMillisPart();
		final long seconds = duration.toSecondsPart();
		final long minutes = duration.toMinutesPart();
		final long hours = duration.toHoursPart();
		final long days = duration.toDays() % 7;
		final long weeks = duration.toDays() / 7 % 52;
		final long years = duration.toDays() / 7 / 52;

		if (millis != 0 && showMillis) {
			returnValue.insert(0, " " + "millis");
			returnValue.insert(0, millis);
		}

		if (seconds != 0) {
			if (returnValue.length() > 0) {
				returnValue.insert(0, " ");
			}
			returnValue.insert(0, " " + "seconds");
			returnValue.insert(0, seconds);
		}

		if (minutes != 0) {
			if (returnValue.length() > 0) {
				returnValue.insert(0, " ");
			}
			returnValue.insert(0, " " + "minutes");
			returnValue.insert(0, minutes);
		}

		if (hours != 0) {
			if (returnValue.length() > 0) {
				returnValue.insert(0, " ");
			}
			returnValue.insert(0, " " + "hours");
			returnValue.insert(0, hours);
		}

		if (days != 0) {
			if (returnValue.length() > 0) {
				returnValue.insert(0, " ");
			}
			returnValue.insert(0, " " + "days");
			returnValue.insert(0, days);
		}

		if (weeks != 0) {
			if (returnValue.length() > 0) {
				returnValue.insert(0, " ");
			}
			returnValue.insert(0, " " + "weeks");
			returnValue.insert(0, weeks);
		}

		if (years != 0) {
			if (returnValue.length() > 0) {
				returnValue.insert(0, " ");
			}
			returnValue.insert(0, " " + "years");
			returnValue.insert(0, years);
		}

		if (returnValue.length() > 0) {
			return returnValue.toString();
		} else if (!showMillis) {
			return "0 " + "seconds";
		} else {
			return "0 " + "millis";
		}
	}

	/**
	 * Get the duration between two timestamps as a string
	 *
	 * @param startTime
	 *            start of the duration
	 * @param endTime
	 *            end of the duration
	 * @return duration like "1d 2h 3m 4s 5ms", leading parts with value 0 are omitted, with a leading
	 *         "-" if the end is before the start
	 */
	public static String getDuration(final Calendar startTime, final Calendar endTime) {
		final long signedDurationInMilliSeconds = endTime.getTimeInMillis() - startTime.getTimeInMillis();
		final boolean negative = signedDurationInMilliSeconds < 0;
		final long durationInMilliSeconds = Math.abs(signedDurationInMilliSeconds);
		final long milliSecondsPart = durationInMilliSeconds % 1000;
		final long secondsPart = durationInMilliSeconds / 1000 % 60;
		final long minutesPart = durationInMilliSeconds / 1000 / 60 % 60;
		final long hoursPart = durationInMilliSeconds / 1000 / 60 / 60 % 24;
		final long days = durationInMilliSeconds / 1000 / 60 / 60 / 24;

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
		return negative ? "-" + returnString : returnString;
	}

	/**
	 * Calculates the next start of a scheduled job from now in the system default time zone, see
	 * {@link #calculateNextJobStart(ZonedDateTime, String, ZoneId)}.
	 *
	 * @param timingString
	 *            the timing definition
	 * @return the next job start, or null for "ONCE" or an empty timing definition
	 * @throws Exception
	 *             if the timing definition is invalid
	 */
	public static ZonedDateTime calculateNextJobStart(final String timingString) throws Exception {
		return calculateNextJobStart(null, timingString, null);
	}

	/**
	 * Calculates the next start of a scheduled job from now, see
	 * {@link #calculateNextJobStart(ZonedDateTime, String, ZoneId)}.
	 *
	 * @param timingString
	 *            the timing definition
	 * @param zoneId
	 *            the time zone for the calculation, null for the system default
	 * @return the next job start, or null for "ONCE" or an empty timing definition
	 * @throws Exception
	 *             if the timing definition is invalid
	 */
	public static ZonedDateTime calculateNextJobStart(final String timingString, final ZoneId zoneId) throws Exception {
		return calculateNextJobStart(null, timingString, zoneId);
	}

	/**
	 * Calculation of next scheduled job start.
	 * Timingparameter may contain weekdays, clocktimes, months, quarters and holidays.
	 *
	 * Allowed parameters:
	 * <pre>
	 * "ONCE"                      =&gt; only once (returns null)
	 * "0600;0800"                 =&gt; daily at 06:00 and 08:00
	 * "MoMi:1700"                 =&gt; every monday and wednesday at 17:00
	 * "M05:1600"                  =&gt; every 05th day of month at 16:00 (a day beyond the month's end means its last day)
	 * "Q:1600"                    =&gt; every first day of quarter at 16:00
	 * "QW:1600"                   =&gt; every first working day of quarter at 16:00
	 * "MoDiMiDoFr:1700;!23012011" =&gt; mondays to fridays at 17:00 except for 23.01.2011 (holidays marked by '!')
	 * "24122030"                  =&gt; only once on 24.12.2030 at 00:00
	 * "*4*5"                      =&gt; daily at all times matching the pattern, '*' matches any digit
	 * </pre>
	 *
	 * All values may be combined separated by semicolons.
	 *
	 * @param calulationStartDateTime
	 *            point in time to calculate the next start from, null for now
	 * @param timingString
	 *            timing definition as described above
	 * @param zoneId
	 *            time zone for the calculation, null for the system default
	 * @return next job start, or null for "ONCE", an empty timing definition or if all rules are fixed
	 *         dates in the past or on excluded days
	 * @throws Exception
	 *             if the timing definition is invalid
	 */
	public static ZonedDateTime calculateNextJobStart(ZonedDateTime calulationStartDateTime, final String timingString, ZoneId zoneId) throws Exception {
		if (Utilities.isBlank(timingString) || "once".equalsIgnoreCase(timingString.trim())) {
			return null;
		}

		if (zoneId == null) {
			zoneId = ZoneId.systemDefault();
		}

		if (calulationStartDateTime == null) {
			calulationStartDateTime = ZonedDateTime.now(zoneId);
		}

		ZonedDateTime returnStart = null;

		// Holidays to exclude
		final List<LocalDate> excludedDays = new ArrayList<>();

		// Empty parts, e.g. from "0600; 0800" (separator followed by a blank) or a leading separator like ";0600", are ignored
		final String[] timingParameterList = timingString.trim().split("[;, ]+");
		for (final String timingParameter : timingParameterList) {
			if (timingParameter.startsWith("!")) {
				excludedDays.add(parseTimingDate(timingParameter.substring(1), timingParameter));
			}
		}

		// Number of rules (no exclusions) and number of fixed dates, that will never be executed (past or excluded)
		int ruleCount = 0;
		int expiredFixedDateCount = 0;

		for (final String timingParameter : timingParameterList) {
			if (timingParameter.isEmpty() || timingParameter.startsWith("!")) {
				// Exclusions are done previously
				continue;
			}

			ruleCount++;
			ZonedDateTime nextStartByThisParameter;
			if (!timingParameter.contains(":")) {
				if (isAsciiDigits(timingParameter)) {
					if (timingParameter.length() == 4) {
						// daily execution on given time
						final LocalTime startTime = parseRuleTime(timingParameter, timingParameter);

						// Move next start into future (+1 day) until rule is matched
						// Move also when meeting holiday rule
						// The days are counted as local dates, so a daylight saving time gap (e.g. 02:30 on the day of the change) does not shift the time of the following days
						LocalDate day = calulationStartDateTime.toLocalDate();
						nextStartByThisParameter = ZonedDateTime.of(day, startTime, zoneId);
						while (!nextStartByThisParameter.isAfter(calulationStartDateTime) && (returnStart == null || nextStartByThisParameter.isBefore(returnStart))
								|| dayListIncludes(excludedDays, day)) {
							day = day.plusDays(1);
							nextStartByThisParameter = ZonedDateTime.of(day, startTime, zoneId);
						}
					} else if (timingParameter.length() == 8) {
						// execution on given day
						nextStartByThisParameter = parseTimingDate(timingParameter, timingParameter).atStartOfDay(zoneId);

						if (!nextStartByThisParameter.isAfter(calulationStartDateTime) || dayListIncludes(excludedDays, nextStartByThisParameter.toLocalDate())) {
							// A fixed date in the past or on a holiday will never be executed
							expiredFixedDateCount++;
							continue;
						}
					} else {
						throw new Exception("Invalid interval description: " + timingParameter);
					}
				} else if (timingParameter.contains("*")) {
					// daily execution on given time with wildcards '*' like '*4*5'
					if (!TIME_WILDCARD_PATTERN.matcher(timingParameter).matches()) {
						throw new Exception("Invalid interval description, time pattern must consist of 4 digits or '*': " + timingParameter);
					}
					nextStartByThisParameter = calulationStartDateTime.truncatedTo(ChronoUnit.MINUTES);

					// Move next start into future (+1 minute) until rule is matched
					// Move also when meeting holiday rule
					// A pattern like "**99" never matches any time, so the search is limited to avoid an endless loop
					final ZonedDateTime searchLimit = nextStartByThisParameter.plusYears(1);
					while (!nextStartByThisParameter.isAfter(calulationStartDateTime) && (returnStart == null || nextStartByThisParameter.isBefore(returnStart))
							|| dayListIncludes(excludedDays, nextStartByThisParameter.toLocalDate())
							|| !checkTimeMatchesPattern(timingParameter, nextStartByThisParameter.toLocalTime())) {
						nextStartByThisParameter = nextStartByThisParameter.plusMinutes(1);
						if (nextStartByThisParameter.isAfter(searchLimit)) {
							throw new Exception("Invalid interval description, time pattern never matches: " + timingParameter);
						}
					}
				} else {
					// Fr: weekly execution on Friday at 00:00 Uhr
					final WeekdaySelection weekdaySelection = parseWeekdaySelection(timingParameter, timingString);
					nextStartByThisParameter = getNextWeekdayStart(calulationStartDateTime, LocalTime.MIDNIGHT, zoneId, weekdaySelection, excludedDays, returnStart);
				}
			} else if (MONTH_RULE_PATTERN.matcher(timingParameter).matches()) {
				// month rule "M99:1700" (every month at ultimo)
				// month rule "06M01:1700" (every half a year at months first day)
				final String xMonth = timingParameter.substring(0, timingParameter.indexOf("M"));
				final int monthInterval = xMonth.length() == 0 ? 1 : Integer.parseInt(xMonth);
				if (monthInterval < 1) {
					throw new Exception("Invalid month interval in timing data: " + timingParameter);
				}
				final String day = timingParameter.substring(timingParameter.indexOf("M") + 1, timingParameter.indexOf(":"));
				final int dayOfMonth = Integer.parseInt(day);
				if (dayOfMonth != 99 && (dayOfMonth < 1 || dayOfMonth > 31)) {
					throw new Exception("Invalid day of month in timing data: " + timingParameter);
				}
				final LocalTime startTime = parseRuleTime(timingParameter.substring(timingParameter.indexOf(":") + 1), timingParameter);

				// Day 99 (ultimo) and a day beyond the end of a month (e.g. 31 in November) mean the last day of that month.
				// The calculation is done with local dates, so a daylight saving time gap does not shift the time.
				LocalDate startDate = withDayOfMonthOrLastDay(calulationStartDateTime.toLocalDate(), dayOfMonth);
				// ensure that the first estimated "next time" is in the past, before making forward steps
				if (ZonedDateTime.of(startDate, startTime, zoneId).isAfter(calulationStartDateTime)) {
					startDate = withDayOfMonthOrLastDay(startDate.minusMonths(1), dayOfMonth);
				}

				// Make forward step
				// plusMonths shortens e.g. January 31 to February 28, so the wanted day is set again
				startDate = withDayOfMonthOrLastDay(startDate.plusMonths(monthInterval), dayOfMonth);

				// Move also when meeting holiday rule
				while (dayListIncludes(excludedDays, startDate)) {
					startDate = startDate.plusDays(1);
				}
				nextStartByThisParameter = ZonedDateTime.of(startDate, startTime, zoneId);
			} else if (timingParameter.startsWith("Q:") || timingParameter.startsWith("QW:")) {
				// quarterly execution (Q:1200) at first day of quarter, or (QW:1200) at first working day of quarter
				final boolean onlyWorkingDays = timingParameter.startsWith("QW:");
				final LocalTime startTime = parseRuleTime(timingParameter.substring(timingParameter.indexOf(":") + 1), timingParameter);

				// The current quarter is checked first, because its start may still be ahead (e.g. on January 1st before the start time)
				final LocalDate currentDate = calulationStartDateTime.toLocalDate();
				LocalDate quarterStart = LocalDate.of(currentDate.getYear(), (currentDate.get(IsoFields.QUARTER_OF_YEAR) - 1) * 3 + 1, 1);
				nextStartByThisParameter = getQuarterStart(quarterStart, startTime, zoneId, onlyWorkingDays, excludedDays);
				if (!nextStartByThisParameter.isAfter(calulationStartDateTime)) {
					quarterStart = quarterStart.plusMonths(3);
					nextStartByThisParameter = getQuarterStart(quarterStart, startTime, zoneId, onlyWorkingDays, excludedDays);
				}
			} else if (WEEKDAILY_RULE_PATTERN.matcher(timingParameter).matches()) {
				// every xth of a weekday in a month
				final int weekDayOrder = Integer.parseInt(timingParameter.substring(0, 1));
				if (weekDayOrder < 1 || 5 < weekDayOrder) {
					throw new Exception("Invalid interval description");
				}
				final String weekDayPartString = timingParameter.substring(1, 3);
				final DayOfWeek weekdayIndex = getDayOfWeekByNamePart(weekDayPartString);
				if (weekdayIndex == null) {
					throw new Exception("Invalid weekday in timing data: " + timingString);
				}
				final LocalTime startTime = parseRuleTime(timingParameter.substring(timingParameter.indexOf(":") + 1), timingParameter);

				// Move next start into future (+1 day) until rule is matched
				// Move also when meeting holiday rule
				// The days are counted as local dates, so a daylight saving time gap does not shift the time of the following days
				LocalDate day = calulationStartDateTime.toLocalDate();
				nextStartByThisParameter = ZonedDateTime.of(day, startTime, zoneId);
				while ((!nextStartByThisParameter.isAfter(calulationStartDateTime)
						|| weekdayIndex != day.getDayOfWeek()
						|| weekDayOrder != getNumberOfWeekdayInMonth(day.getDayOfMonth())) && (returnStart == null || nextStartByThisParameter.isBefore(returnStart))
						|| dayListIncludes(excludedDays, day)) {
					day = day.plusDays(1);
					nextStartByThisParameter = ZonedDateTime.of(day, startTime, zoneId);
				}
			} else {
				// weekday execution (also allows workingday execution, german: "Werktagssteuerung" by "MoTuWeThFr:0000")
				final WeekdaySelection weekdaySelection = parseWeekdaySelection(timingParameter.substring(0, timingParameter.indexOf(":")), timingString);
				final LocalTime startTime = parseRuleTime(timingParameter.substring(timingParameter.indexOf(":") + 1), timingParameter);
				nextStartByThisParameter = getNextWeekdayStart(calulationStartDateTime, startTime, zoneId, weekdaySelection, excludedDays, returnStart);
			}

			if (returnStart == null || nextStartByThisParameter.isBefore(returnStart)) {
				returnStart = nextStartByThisParameter;
			}
		}

		if (returnStart == null) {
			if (ruleCount > 0 && ruleCount == expiredFixedDateCount) {
				// Only fixed dates, which are all in the past or excluded: there is no next start anymore
				return null;
			} else {
				throw new Exception("Invalid interval description");
			}
		}

		return returnStart;
	}

	/**
	 * Selection of weekdays, optionally restricted to odd or even ISO weeks.
	 */
	private static class WeekdaySelection {
		private final List<DayOfWeek> weekdays = new ArrayList<>();
		private boolean onlyWithinOddWeeks = false;
		private boolean onlyWithinEvenWeeks = false;

		private boolean matches(final LocalDate day) {
			if (!weekdays.contains(day.getDayOfWeek())) {
				return false;
			} else if (onlyWithinOddWeeks && day.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR) % 2 == 0) {
				return false;
			} else if (onlyWithinEvenWeeks && day.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR) % 2 != 0) {
				return false;
			} else {
				return true;
			}
		}
	}

	/**
	 * Parses weekday definitions of two letters each, like "MoMiFr", optionally with "Ev" (even weeks only) or "Od" (odd weeks only).
	 */
	private static WeekdaySelection parseWeekdaySelection(final String weekDays, final String timingString) throws Exception {
		final WeekdaySelection weekdaySelection = new WeekdaySelection();
		for (final String weekDayPartString : TextUtilities.chopToChunks(weekDays, 2)) {
			if ("ev".equalsIgnoreCase(weekDayPartString)) {
				weekdaySelection.onlyWithinEvenWeeks = true;
			} else if ("od".equalsIgnoreCase(weekDayPartString)) {
				weekdaySelection.onlyWithinOddWeeks = true;
			} else {
				final DayOfWeek weekday = getDayOfWeekByNamePart(weekDayPartString);
				if (weekday == null) {
					throw new Exception("Invalid weekday in timing data: " + timingString);
				}
				weekdaySelection.weekdays.add(weekday);
			}
		}
		if (weekdaySelection.weekdays.isEmpty()) {
			// Only "Ev" or "Od" without weekday would never match and loop endlessly
			throw new Exception("Invalid timing data: " + timingString);
		}
		if (weekdaySelection.onlyWithinEvenWeeks && weekdaySelection.onlyWithinOddWeeks) {
			// "Ev" and "Od" together would never match and loop endlessly
			throw new Exception("Invalid timing data, even and odd weeks exclude each other: " + timingString);
		}
		return weekdaySelection;
	}

	/**
	 * Returns the next start on one of the selected weekdays after the calculation start, skipping excluded days.
	 * The days are counted as local dates, so a daylight saving time gap does not shift the time of the following days.
	 */
	private static ZonedDateTime getNextWeekdayStart(final ZonedDateTime calulationStartDateTime, final LocalTime startTime, final ZoneId zoneId, final WeekdaySelection weekdaySelection, final List<LocalDate> excludedDays, final ZonedDateTime returnStart) {
		LocalDate day = calulationStartDateTime.toLocalDate();
		ZonedDateTime nextStart = ZonedDateTime.of(day, startTime, zoneId);
		while ((!nextStart.isAfter(calulationStartDateTime) || !weekdaySelection.matches(day)) && (returnStart == null || nextStart.isBefore(returnStart))
				|| dayListIncludes(excludedDays, day)) {
			day = day.plusDays(1);
			nextStart = ZonedDateTime.of(day, startTime, zoneId);
		}
		return nextStart;
	}

	/**
	 * Parses a time "HHmm" of a timing rule strictly.
	 */
	private static LocalTime parseRuleTime(final String time, final String timingParameter) throws Exception {
		if (time.length() != 4 || !isAsciiDigits(time)) {
			throw new Exception("Invalid time in timing data: " + timingParameter);
		}
		try {
			return LocalTime.of(Integer.parseInt(time.substring(0, 2)), Integer.parseInt(time.substring(2)));
		} catch (final DateTimeException e) {
			throw new Exception("Invalid time in timing data: " + timingParameter, e);
		}
	}

	/**
	 * Parses a date "ddMMyyyy" of a timing rule strictly, so invalid dates like "31022026" are rejected instead of being moved to another day.
	 */
	private static LocalDate parseTimingDate(final String dateString, final String timingParameter) throws Exception {
		try {
			return parseStrictLocalDate(DDMMYYYY, dateString);
		} catch (final DateTimeParseException e) {
			throw new Exception("Invalid date in timing data: " + timingParameter, e);
		}
	}

	/**
	 * Checks whether a text is not empty and consists of the ASCII digits '0' to '9' only.
	 */
	private static boolean isAsciiDigits(final String value) {
		if (value.isEmpty()) {
			return false;
		}
		for (int i = 0; i < value.length(); i++) {
			final char c = value.charAt(i);
			if (c < '0' || c > '9') {
				return false;
			}
		}
		return true;
	}

	/**
	 * Sets the day of month, or the last day of the month if it is shorter (e.g. day 31 in November or the special day 99 for ultimo).
	 */
	private static LocalDate withDayOfMonthOrLastDay(final LocalDate date, final int dayOfMonth) {
		return date.withDayOfMonth(Math.min(dayOfMonth, date.lengthOfMonth()));
	}

	/**
	 * Returns the start of a quarter: its first day (or first working day) at the given time, moved behind excluded days.
	 */
	private static ZonedDateTime getQuarterStart(final LocalDate firstDayOfQuarter, final LocalTime startTime, final ZoneId zoneId, final boolean onlyWorkingDays, final List<LocalDate> excludedDays) {
		LocalDate startDay = firstDayOfQuarter;
		while ((onlyWorkingDays && (startDay.getDayOfWeek() == DayOfWeek.SATURDAY || startDay.getDayOfWeek() == DayOfWeek.SUNDAY))
				|| dayListIncludes(excludedDays, startDay)) {
			startDay = startDay.plusDays(1);
		}
		return ZonedDateTime.of(startDay, startTime, zoneId);
	}

	/**
	 * Checks whether a time matches a pattern of four characters for "HHmm", where "*" matches any
	 * digit, like "*4*5".
	 *
	 * @param pattern
	 *            the time pattern, consisting of exactly four digits or '*'
	 * @param time
	 *            the time to check
	 * @return true, if the time matches the pattern
	 * @throws IllegalArgumentException
	 *             if the pattern is null or does not consist of exactly four digits or '*'
	 */
	public static boolean checkTimeMatchesPattern(final String pattern, final LocalTime time) {
		if (pattern == null || !TIME_WILDCARD_PATTERN.matcher(pattern).matches()) {
			throw new IllegalArgumentException("Invalid time pattern, must consist of 4 digits or '*': " + pattern);
		}
		// Compared character by character without regex, because this is called for every minute while searching the next job start
		final int[] timeDigits = { time.getHour() / 10, time.getHour() % 10, time.getMinute() / 10, time.getMinute() % 10 };
		for (int i = 0; i < 4; i++) {
			final char patternChar = pattern.charAt(i);
			if (patternChar != '*' && patternChar - '0' != timeDigits[i]) {
				return false;
			}
		}
		return true;
	}
	/**
	 * Remove the time part of a GregorianCalendar
	 *
	 * @param value
	 *            calendar with date and time
	 * @return new calendar with the same day at 00:00:00
	 */
	public static GregorianCalendar getDayWithoutTime(final GregorianCalendar value) {
		return new GregorianCalendar(value.get(Calendar.YEAR), value.get(Calendar.MONTH), value.get(Calendar.DAY_OF_MONTH));
	}

	/**
	 * Check if a day is included in a list of days
	 *
	 * @param listOfDays
	 *            days to search in
	 * @param day
	 *            day to search for
	 * @return true if the list contains the day
	 */
	public static boolean dayListIncludes(final List<LocalDate> listOfDays, final LocalDate day) {
		for (final LocalDate listDay : listOfDays) {
			if (listDay.isEqual(day)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Parses a date in one of several common formats, interpreting values without time zone in the
	 * system default time zone, see {@link #parseUnknownDateFormat(String, ZoneId)}.
	 *
	 * @param value
	 *            the date text, may be null
	 * @return the parsed date, or null if the value is null
	 * @throws Exception
	 *             if the format of the value is unknown
	 */
	public static ZonedDateTime parseUnknownDateFormat(final String value) throws Exception {
		return parseUnknownDateFormat(value, ZoneId.systemDefault());
	}

	/**
	 * Parses a date in one of several common formats: ISO 8601 and "yyyy-MM-dd HH:mm" (values containing
	 * "-"), "dd.MM.yyyy HH:mm:ss", "dd.MM.yyyy HH:mm" and "dd.MM.yyyy" (values containing "."), or
	 * "yyyyMMddHHmmss", "ddMMyyyy" and "yyyyMMdd".
	 *
	 * @param value
	 *            the date text, may be null
	 * @param timeZone
	 *            the time zone for values without time zone information
	 * @return the parsed date, or null if the value is null
	 * @throws Exception
	 *             if the format of the value is unknown
	 */
	public static ZonedDateTime parseUnknownDateFormat(final String value, final ZoneId timeZone) throws Exception {
		if (value == null) {
			return null;
		} else if (value.contains("-")) {
			try {
				return DateUtilities.parseIso8601DateTimeString(value);
			} catch (@SuppressWarnings("unused") final Exception e1) {
				try {
					return parseLocalDateTime(YYYY_MM_DD_HH_MM, value).atZone(timeZone);
				} catch (@SuppressWarnings("unused") final DateTimeParseException e2) {
					throw new Exception("Unknown date format");
				}
			}
		} else if (value.contains(".")) {
			try {
				return parseLocalDateTime(DD_MM_YYYY_HH_MM_SS, value).atZone(timeZone);
			} catch (@SuppressWarnings("unused") final DateTimeParseException e1) {
				try {
					return parseLocalDateTime(DD_MM_YYYY_HH_MM, value).atZone(timeZone);
				} catch (@SuppressWarnings("unused") final DateTimeParseException e2) {
					try {
						// A date-only pattern can only be parsed as LocalDate, LocalDateTime.parse always fails for it
						return parseLocalDate(DD_MM_YYYY, value).atStartOfDay(timeZone);
					} catch (@SuppressWarnings("unused") final DateTimeParseException e3) {
						throw new Exception("Unknown date format");
					}
				}
			}
		} else {
			try {
				// The offset of the value must be kept, LocalDateTime.parse would silently drop it
				return OffsetDateTime.parse(value, DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmssX")).atZoneSameInstant(timeZone);
			} catch (@SuppressWarnings("unused") final DateTimeParseException e1) {
				try {
					return parseLocalDateTime(YYYYMMDDHHMMSS, value).atZone(timeZone);
				} catch (@SuppressWarnings("unused") final DateTimeParseException e2) {
					try {
						// Strict, so "20251224" is no valid "ddMMyyyy" (month 25) and is parsed as "yyyyMMdd" below
						return parseStrictLocalDate(DDMMYYYY, value).atStartOfDay(timeZone);
					} catch (@SuppressWarnings("unused") final DateTimeParseException e3) {
						try {
							return parseStrictLocalDate(YYYYMMDD, value).atStartOfDay(timeZone);
						} catch (@SuppressWarnings("unused") final DateTimeParseException e4) {
							throw new Exception("Unknown date format");
						}
					}
				}
			}
		}
	}

	/**
	 * Converts a date (also java.sql.Date and java.sql.Timestamp) to a local date time in the system
	 * default time zone. Sub-millisecond parts of a timestamp are lost.
	 *
	 * @param date
	 *            the date, may be null
	 * @return the local date time, or null if the date is null
	 */
	public static LocalDateTime getLocalDateTimeForDate(final Date date) {
		if (date == null) {
			return null;
		} else {
			// getTime() also works for java.sql.Date, whose toInstant() is not supported.
			// Instant handles negative milliseconds (dates before 1970) correctly.
			return Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toLocalDateTime();
		}
	}

	/**
	 * Converts milliseconds since 1970-01-01T00:00:00Z to a local date time in the system default time
	 * zone.
	 *
	 * @param millis
	 *            the milliseconds since the epoch, may be null
	 * @return the local date time, or null if the value is null
	 */
	public static LocalDateTime getLocalDateTime(final Long millis) {
		if (millis == null) {
			return null;
		} else {
			return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDateTime();
		}
	}

	/**
	 * Converts a date (also java.sql.Date) to a local date in the system default time zone.
	 *
	 * @param date
	 *            the date
	 * @return the local date
	 */
	public static LocalDate getLocalDateForDate(final Date date) {
		// new Date(date.getTime()) to convert value of java.sql.Date
		return (new Date(date.getTime())).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
	}

	/**
	 * Converts a local date time, interpreted in the system default time zone, to a date.
	 *
	 * @param localDateTime
	 *            the local date time
	 * @return the date
	 */
	public static Date getDateForLocalDateTime(final LocalDateTime localDateTime) {
		return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
	}

	/**
	 * Converts a local date time, interpreted in the system default time zone, to an SQL timestamp with
	 * millisecond precision.
	 *
	 * @param localDateTime
	 *            the local date time
	 * @return the SQL timestamp
	 */
	public static java.sql.Timestamp getSqlTimestampForLocalDateTime(final LocalDateTime localDateTime) {
		return new java.sql.Timestamp(Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant()).getTime());
	}

	/**
	 * Converts a local date to a date at the start of the day in the system default time zone.
	 *
	 * @param localDate
	 *            the local date
	 * @return the date
	 */
	public static Date getDateForLocalDate(final LocalDate localDate) {
		return Date.from(localDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
	}

	/**
	 * Converts a local date to an SQL date at the start of the day in the system default time zone.
	 *
	 * @param localDate
	 *            the local date
	 * @return the SQL date
	 */
	public static java.sql.Date getSqlDateForLocalDate(final LocalDate localDate) {
		return new java.sql.Date(Date.from(localDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()).getTime());
	}

	/**
	 * Converts a zoned date time to a date representing the same point in time.
	 *
	 * @param zonedDateTime
	 *            the zoned date time
	 * @return the date
	 */
	public static Date getDateForZonedDateTime(final ZonedDateTime zonedDateTime) {
		return Date.from(zonedDateTime.withZoneSameInstant(ZoneId.systemDefault()).toInstant());
	}

	/**
	 * Converts a date to a zoned date time in the system default time zone.
	 *
	 * @param date
	 *            the date (also java.sql.Date)
	 * @return the zoned date time
	 */
	public static ZonedDateTime getZonedDateTimeForDate(final Date date) {
		// getTime() also works for java.sql.Date, whose toInstant() throws an UnsupportedOperationException
		return Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault());
	}

	/**
	 * Parse DateTime strings for ISO 8601.
	 * Values without time zone are interpreted in the system default time zone.
	 *
	 * @param dateValue
	 *            date or datetime string in ISO 8601 format
	 * @return parsed datetime, or null for an empty value
	 */
	public static ZonedDateTime parseIso8601DateTimeString(final String dateValue) {
		return parseIso8601DateTimeString(dateValue, ZoneId.systemDefault());
	}

	/**
	 * Parse DateTime strings for ISO 8601. The time zone offset may be given as "Z", "+hh", "+hhmm" or
	 * "+hh:mm" (or with "-"), also for values without time.
	 *
	 * @param dateValueString
	 *            date or datetime string in ISO 8601 format
	 * @param defaultZoneId
	 *            time zone for values without time zone information
	 * @return parsed datetime, or null for an empty value
	 * @throws DateTimeParseException
	 *             if the value cannot be parsed
	 */
	public static ZonedDateTime parseIso8601DateTimeString(final String dateValueString, final ZoneId defaultZoneId) {
		if (Utilities.isBlank(dateValueString)) {
			return null;
		}

		final String upperCaseValue = dateValueString.trim().toUpperCase(Locale.ROOT);

		// Split into date time part and optional offset part
		String dateTimePart;
		String offsetPart = null;
		final int timeSeparatorIndex = upperCaseValue.indexOf('T');
		if (upperCaseValue.endsWith("Z")) {
			dateTimePart = upperCaseValue.substring(0, upperCaseValue.length() - 1);
			offsetPart = "+00:00";
		} else if (timeSeparatorIndex >= 0) {
			// Within the time part a '+' or '-' can only start the offset
			final int offsetIndex = Math.max(upperCaseValue.lastIndexOf('+'), upperCaseValue.lastIndexOf('-'));
			if (offsetIndex > timeSeparatorIndex) {
				dateTimePart = upperCaseValue.substring(0, offsetIndex);
				offsetPart = upperCaseValue.substring(offsetIndex);
			} else {
				dateTimePart = upperCaseValue;
			}
		} else if (upperCaseValue.length() > 10) {
			// Date only "yyyy-MM-dd" followed by an offset
			dateTimePart = upperCaseValue.substring(0, 10);
			offsetPart = upperCaseValue.substring(10);
		} else {
			dateTimePart = upperCaseValue;
		}

		if (timeSeparatorIndex < 0) {
			dateTimePart = dateTimePart + "T00:00:00";
		}

		// Date with time, optionally with fractions of a second.
		// The ISO formatters read ".5" as half a second, while the pattern "[.n]" read it as 5 nanoseconds.
		if (offsetPart != null) {
			// Normalize the offset to "+hh:mm", ISO_OFFSET_DATE_TIME does not accept "+hhmm"
			final Matcher offsetMatcher = ISO_8601_OFFSET_PATTERN.matcher(offsetPart);
			if (!offsetMatcher.matches()) {
				throw new DateTimeParseException("Invalid time zone offset in ISO 8601 value", dateValueString, 0);
			}
			final String offsetMinutes = offsetMatcher.group(3) == null ? "00" : offsetMatcher.group(3);
			final String normalizedValue = dateTimePart + offsetMatcher.group(1) + offsetMatcher.group(2) + ":" + offsetMinutes;
			final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withResolverStyle(ResolverStyle.STRICT);
			return ZonedDateTime.parse(normalizedValue, dateTimeFormatter);
		} else {
			final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME.withResolverStyle(ResolverStyle.STRICT);
			return LocalDateTime.parse(dateTimePart, dateTimeFormatter).atZone(defaultZoneId);
		}
	}

	/**
	 * Get the ordinal of occurrence of the weekday of the given day in its month
	 *
	 * @param dayOfMonth
	 *            day of month (1 - 31)
	 * @return ordinal of the weekday in its month (1 - 5), e.g. 2 for the second monday
	 */
	public static int getNumberOfWeekdayInMonth(final int dayOfMonth) {
		final float ordinalFloat = dayOfMonth / 7.0f;
		final int ordinalInt = (int) Math.round(Math.ceil(ordinalFloat));
		return ordinalInt;
	}

	/**
	 * Shifts a date, whose wall clock time is meant in the source time zone, so that it shows the same
	 * point in time as wall clock time in the destination time zone. Daylight saving time is determined
	 * for the given date in both zones.
	 *
	 * @param date
	 *            the date, may be null
	 * @param sourceTimeZone
	 *            the source time zone, null for the system default
	 * @param destinationTimeZone
	 *            the destination time zone, null for the system default
	 * @return the shifted date, or null if the date is null
	 */
	public static Date changeDateTimeZone(final Date date, TimeZone sourceTimeZone, TimeZone destinationTimeZone) {
		if (date == null) {
			return null;
		} else {
			if (sourceTimeZone == null) {
				sourceTimeZone = TimeZone.getDefault();
			}
			if (destinationTimeZone == null) {
				destinationTimeZone = TimeZone.getDefault();
			}
			if (sourceTimeZone.equals(destinationTimeZone)) {
				return date;
			} else {
				long fromTZDst = 0;
				if (sourceTimeZone.inDaylightTime(date)) {
					fromTZDst = sourceTimeZone.getDSTSavings();
				}

				final long fromTZOffset = sourceTimeZone.getRawOffset() + fromTZDst;

				long toTZDst = 0;
				if (destinationTimeZone.inDaylightTime(date)) {
					toTZDst = destinationTimeZone.getDSTSavings();
				}
				final long toTZOffset = destinationTimeZone.getRawOffset() + toTZDst;

				return new Date(date.getTime() + (toTZOffset - fromTZOffset));
			}
		}
	}

	/**
	 * Shifts a date, whose wall clock time (in the system default time zone) is meant in the source time
	 * zone, so that its wall clock time shows the same point in time in the destination time zone.
	 *
	 * @param date
	 *            the date, may be null
	 * @param sourceZoneId
	 *            the source time zone, null for the system default
	 * @param destinationZoneId
	 *            the destination time zone, null for the system default
	 * @return the shifted date, or null if the date is null
	 */
	public static Date changeDateTimeZone(final Date date, ZoneId sourceZoneId, ZoneId destinationZoneId) {
		if (date == null) {
			return null;
		} else {
			if (sourceZoneId == null) {
				sourceZoneId = ZoneId.systemDefault();
			}
			if (destinationZoneId == null) {
				destinationZoneId = ZoneId.systemDefault();
			}
			if (sourceZoneId.equals(destinationZoneId)) {
				return date;
			} else {
				final LocalDateTime localDateTime = getLocalDateTimeForDate(date);
				final ZonedDateTime sourceZonedDateTime = localDateTime.atZone(sourceZoneId);
				final ZonedDateTime destinationZonedDateTime = sourceZonedDateTime.withZoneSameInstant(destinationZoneId);
				final Date rezonedDate = new Date(destinationZonedDateTime.withZoneSameLocal(ZoneId.systemDefault()).toInstant().toEpochMilli());
				return rezonedDate;
			}
		}
	}

	/**
	 * Converts a local date time from a source time zone to the local date time of the same point in
	 * time in a destination time zone.
	 *
	 * @param localDateTime
	 *            the local date time in the source time zone, may be null
	 * @param sourceZoneId
	 *            the source time zone, null for the system default
	 * @param destinationZoneId
	 *            the destination time zone, null for the system default
	 * @return the local date time in the destination time zone, or null if the value is null
	 */
	public static LocalDateTime changeDateTimeZone(final LocalDateTime localDateTime, ZoneId sourceZoneId, ZoneId destinationZoneId) {
		if (localDateTime == null) {
			return null;
		} else {
			if (sourceZoneId == null) {
				sourceZoneId = ZoneId.systemDefault();
			}
			if (destinationZoneId == null) {
				destinationZoneId = ZoneId.systemDefault();
			}
			if (sourceZoneId.equals(destinationZoneId)) {
				return localDateTime;
			} else {
				final ZonedDateTime sourceZonedDateTime = localDateTime.atZone(sourceZoneId);
				return sourceZonedDateTime.withZoneSameInstant(destinationZoneId).toLocalDateTime();
			}
		}
	}

	/**
	 * Converts a zoned date time to the same point in time in another time zone.
	 *
	 * @param zonedDateTime
	 *            the zoned date time, may be null
	 * @param destinationZoneId
	 *            the destination time zone, null for the system default
	 * @return the converted date time, or null if the value is null
	 */
	public static ZonedDateTime changeDateTimeZone(final ZonedDateTime zonedDateTime, ZoneId destinationZoneId) {
		if (zonedDateTime == null) {
			return null;
		} else {
			if (destinationZoneId == null) {
				destinationZoneId = ZoneId.systemDefault();
			}
			if (zonedDateTime.getZone().equals(destinationZoneId)) {
				return zonedDateTime;
			} else {
				return zonedDateTime.withZoneSameInstant(destinationZoneId);
			}
		}
	}

	/**
	 * Returns a strict date formatter with the short date format of a locale, using four digit years.
	 *
	 * @param locale
	 *            the locale
	 * @return the date formatter
	 */
	public static DateTimeFormatter getDateFormatter(final Locale locale) {
		DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(getDateFormatPattern(locale).replace("yyyy", "uuuu"));
		dateTimeFormatter = dateTimeFormatter.withResolverStyle(ResolverStyle.STRICT);
		return dateTimeFormatter;
	}

	/**
	 * Returns a strict date formatter with the short date format of a locale, using four digit years and
	 * the given time zone.
	 *
	 * @param locale
	 *            the locale
	 * @param zoneId
	 *            the time zone of the formatter
	 * @return the date formatter
	 */
	public static DateTimeFormatter getDateFormatter(final Locale locale, final ZoneId zoneId) {
		DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(getDateFormatPattern(locale).replace("yyyy", "uuuu"));
		dateTimeFormatter = dateTimeFormatter.withResolverStyle(ResolverStyle.STRICT);
		dateTimeFormatter = dateTimeFormatter.withZone(zoneId);
		return dateTimeFormatter;
	}

	/**
	 * Returns the short date format pattern of a locale, using four digit years.
	 *
	 * @param locale
	 *            the locale
	 * @return the date format pattern, like "dd.MM.yyyy" for German
	 */
	public static String getDateFormatPattern(final Locale locale) {
		final SimpleDateFormat dateTimeFormat = (SimpleDateFormat) DateFormat.getDateInstance(DateFormat.SHORT, locale);
		return dateTimeFormat.toPattern().replaceFirst("y+", "yyyy");
	}

	/**
	 * Returns the short date format pattern of a locale, using four digit years, followed by "HH:mm".
	 *
	 * @param locale
	 *            the locale
	 * @return the date time format pattern
	 */
	public static String getDateTimeFormatPattern(final Locale locale) {
		return getDateFormatPattern(locale) + " HH:mm";
	}

	/**
	 * Returns the short date format pattern of a locale, using four digit years, followed by
	 * "HH:mm:ss".
	 *
	 * @param locale
	 *            the locale
	 * @return the date time format pattern
	 */
	public static String getDateTimeFormatWithSecondsPattern(final Locale locale) {
		return getDateFormatPattern(locale) + " HH:mm:ss";
	}

	/**
	 * Returns a strict date time formatter for {@link #getDateTimeFormatPattern(Locale)}.
	 *
	 * @param locale
	 *            the locale
	 * @return the date time formatter
	 */
	public static DateTimeFormatter getDateTimeFormatter(final Locale locale) {
		DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(getDateTimeFormatPattern(locale).replace("yyyy", "uuuu"));
		dateTimeFormatter = dateTimeFormatter.withResolverStyle(ResolverStyle.STRICT);
		return dateTimeFormatter;
	}

	/**
	 * Returns a strict date time formatter for {@link #getDateTimeFormatPattern(Locale)} with the given
	 * time zone.
	 *
	 * @param locale
	 *            the locale
	 * @param zoneId
	 *            the time zone of the formatter
	 * @return the date time formatter
	 */
	public static DateTimeFormatter getDateTimeFormatter(final Locale locale, final ZoneId zoneId) {
		DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(getDateTimeFormatPattern(locale).replace("yyyy", "uuuu"));
		dateTimeFormatter = dateTimeFormatter.withResolverStyle(ResolverStyle.STRICT);
		dateTimeFormatter = dateTimeFormatter.withZone(zoneId);
		return dateTimeFormatter;
	}

	/**
	 * Returns a strict date time formatter for {@link #getDateTimeFormatWithSecondsPattern(Locale)}.
	 *
	 * @param locale
	 *            the locale
	 * @return the date time formatter
	 */
	public static DateTimeFormatter getDateTimeFormatterWithSeconds(final Locale locale) {
		DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(getDateTimeFormatWithSecondsPattern(locale).replace("yyyy", "uuuu"));
		dateTimeFormatter = dateTimeFormatter.withResolverStyle(ResolverStyle.STRICT);
		return dateTimeFormatter;
	}

	/**
	 * Returns a strict date time formatter for {@link #getDateTimeFormatWithSecondsPattern(Locale)} with
	 * the given time zone.
	 *
	 * @param locale
	 *            the locale
	 * @param zoneId
	 *            the time zone of the formatter
	 * @return the date time formatter
	 */
	public static DateTimeFormatter getDateTimeFormatterWithSeconds(final Locale locale, final ZoneId zoneId) {
		DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(getDateTimeFormatWithSecondsPattern(locale).replace("yyyy", "uuuu"));
		dateTimeFormatter = dateTimeFormatter.withResolverStyle(ResolverStyle.STRICT);
		dateTimeFormatter = dateTimeFormatter.withZone(zoneId);
		return dateTimeFormatter;
	}

	/**
	 * Formats a zoned date time.
	 *
	 * @param format
	 *            the format pattern, see {@link DateTimeFormatter}
	 * @param date
	 *            the value to format, may be null
	 * @return the formatted value, or null if the value is null
	 */
	public static String formatDate(final String format, final ZonedDateTime date) {
		if (date == null) {
			return null;
		} else {
			return DateTimeFormatter.ofPattern(format).format(date);
		}
	}

	/**
	 * Formats a date in the system default time zone.
	 *
	 * @param format
	 *            the format pattern, see {@link DateTimeFormatter}
	 * @param date
	 *            the value to format, may be null
	 * @return the formatted value, or null if the value is null
	 */
	public static String formatDate(final String format, final Date date) {
		if (date == null) {
			return null;
		} else {
			// Formatted from the instant, a LocalDateTime round trip is ambiguous within the daylight saving time overlap
			return DateTimeFormatter.ofPattern(format).withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(date.getTime()));
		}
	}

	/**
	 * Formats a local date time. The pattern must not contain time zone fields.
	 *
	 * @param format
	 *            the format pattern, see {@link DateTimeFormatter}
	 * @param date
	 *            the value to format, may be null
	 * @return the formatted value, or null if the value is null
	 */
	public static String formatDate(final String format, final LocalDateTime date) {
		if (date == null) {
			return null;
		} else {
			return DateTimeFormatter.ofPattern(format).format(date);
		}
	}

	/**
	 * Formats a local date. The pattern must not contain time or time zone fields.
	 *
	 * @param format
	 *            the format pattern, see {@link DateTimeFormatter}
	 * @param date
	 *            the value to format, may be null
	 * @return the formatted value, or null if the value is null
	 */
	public static String formatDate(final String format, final LocalDate date) {
		if (date == null) {
			return null;
		} else {
			return DateTimeFormatter.ofPattern(format).format(date);
		}
	}

	/**
	 * Formats a local time. The pattern must not contain date or time zone fields.
	 *
	 * @param format
	 *            the format pattern, see {@link DateTimeFormatter}
	 * @param time
	 *            the value to format, may be null
	 * @return the formatted value, or null if the value is null
	 */
	public static String formatDate(final String format, final LocalTime time) {
		if (time == null) {
			return null;
		} else {
			return DateTimeFormatter.ofPattern(format).format(time);
		}
	}

	/**
	 * Formats a date in the given time zone with localized texts (like month names).
	 *
	 * @param format
	 *            the format pattern, see {@link DateTimeFormatter}
	 * @param date
	 *            the value to format, may be null
	 * @param locale
	 *            the locale for localized texts
	 * @param zoneId
	 *            the time zone to show the date in
	 * @return the formatted value, or null if the value is null
	 */
	public static String formatDate(final String format, final Date date, final Locale locale, final ZoneId zoneId) {
		if (date == null) {
			return null;
		} else {
			// The date is a point in time, which is converted to the given time zone.
			// Formatted from the instant, a LocalDateTime round trip is ambiguous within the daylight saving time overlap.
			return DateTimeFormatter.ofPattern(format).localizedBy(locale).withZone(zoneId).format(Instant.ofEpochMilli(date.getTime()));
		}
	}

	/**
	 * Formats a local date time with localized texts (like month names). The local date time is not
	 * converted, the time zone is only used for time zone fields of the pattern.
	 *
	 * @param format
	 *            the format pattern, see {@link DateTimeFormatter}
	 * @param date
	 *            the value to format, may be null
	 * @param locale
	 *            the locale for localized texts
	 * @param zoneId
	 *            the time zone for time zone fields
	 * @return the formatted value, or null if the value is null
	 */
	public static String formatDate(final String format, final LocalDateTime date, final Locale locale, final ZoneId zoneId) {
		if (date == null) {
			return null;
		} else {
			return DateTimeFormatter.ofPattern(format).localizedBy(locale).withZone(zoneId).format(date);
		}
	}

	/**
	 * Formats a local date with localized texts (like month names).
	 *
	 * @param format
	 *            the format pattern, see {@link DateTimeFormatter}
	 * @param date
	 *            the value to format, may be null
	 * @param locale
	 *            the locale for localized texts
	 * @param zoneId
	 *            the time zone of the formatter
	 * @return the formatted value, or null if the value is null
	 */
	public static String formatDate(final String format, final LocalDate date, final Locale locale, final ZoneId zoneId) {
		if (date == null) {
			return null;
		} else {
			return DateTimeFormatter.ofPattern(format).localizedBy(locale).withZone(zoneId).format(date);
		}
	}

	/**
	 * Formats a date in the given time zone.
	 *
	 * @param format
	 *            the format pattern, see {@link DateTimeFormatter}
	 * @param date
	 *            the value to format, may be null
	 * @param zoneId
	 *            the time zone to show the date in
	 * @return the formatted value, or null if the value is null
	 */
	public static String formatDate(final String format, final Date date, final ZoneId zoneId) {
		if (date == null) {
			return null;
		} else {
			// Formatted from the instant, a LocalDateTime round trip is ambiguous within the daylight saving time overlap
			return DateTimeFormatter.ofPattern(format).withZone(zoneId).format(Instant.ofEpochMilli(date.getTime()));
		}
	}

	/**
	 * Formats a local date time. The local date time is not converted, the time zone is only used for
	 * time zone fields of the pattern.
	 *
	 * @param format
	 *            the format pattern, see {@link DateTimeFormatter}
	 * @param date
	 *            the value to format, may be null
	 * @param zoneId
	 *            the time zone for time zone fields
	 * @return the formatted value, or null if the value is null
	 */
	public static String formatDate(final String format, final LocalDateTime date, final ZoneId zoneId) {
		if (date == null) {
			return null;
		} else {
			return DateTimeFormatter.ofPattern(format).withZone(zoneId).format(date);
		}
	}

	/**
	 * Formats a zoned date time in the given time zone.
	 *
	 * @param format
	 *            the format pattern, see {@link DateTimeFormatter}
	 * @param date
	 *            the value to format, may be null
	 * @param zoneId
	 *            the time zone to show the date in
	 * @return the formatted value, or null if the value is null
	 */
	public static String formatDate(final String format, final ZonedDateTime date, final ZoneId zoneId) {
		if (date == null) {
			return null;
		} else {
			return DateTimeFormatter.ofPattern(format).withZone(zoneId).format(date);
		}
	}

	/**
	 * Parses a local date with smart resolving (e.g. day 31 of a 30 day month becomes the last day).
	 *
	 * @param dateFormatPattern
	 *            the format pattern, see {@link DateTimeFormatter}
	 * @param dateString
	 *            the text to parse
	 * @return the parsed date
	 * @throws java.time.format.DateTimeParseException
	 *             if the text cannot be parsed
	 */
	public static LocalDate parseLocalDate(final String dateFormatPattern, final String dateString) {
		final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(dateFormatPattern);
		final LocalDate localDate = LocalDate.parse(dateString, dateTimeFormatter);
		return localDate;
	}

	/**
	 * Parses a local date strictly, invalid dates like February 30 are rejected. "yyyy" in the pattern
	 * is treated as proleptic year "uuuu".
	 *
	 * @param dateFormatPattern
	 *            the format pattern, see {@link DateTimeFormatter}
	 * @param dateString
	 *            the text to parse
	 * @return the parsed date
	 * @throws java.time.format.DateTimeParseException
	 *             if the text cannot be parsed or is no valid date
	 */
	public static LocalDate parseStrictLocalDate(final String dateFormatPattern, final String dateString) {
		final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(dateFormatPattern.replace("yyyy", "uuuu")).withResolverStyle(ResolverStyle.STRICT);
		final LocalDate localDate = LocalDate.parse(dateString, dateTimeFormatter);
		return localDate;
	}

	/**
	 * Parses a local date time with smart resolving.
	 *
	 * @param dateTimeFormatPattern
	 *            the format pattern, see {@link DateTimeFormatter}
	 * @param dateTimeString
	 *            the text to parse
	 * @return the parsed date time
	 * @throws java.time.format.DateTimeParseException
	 *             if the text cannot be parsed
	 */
	public static LocalDateTime parseLocalDateTime(final String dateTimeFormatPattern, final String dateTimeString) {
		final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(dateTimeFormatPattern);
		final LocalDateTime localDateTime = LocalDateTime.parse(dateTimeString, dateTimeFormatter);
		return localDateTime;
	}

	/**
	 * Parses a local time strictly.
	 *
	 * @param timeFormatPattern
	 *            the format pattern, see {@link DateTimeFormatter}
	 * @param timeString
	 *            the text to parse
	 * @return the parsed time
	 * @throws java.time.format.DateTimeParseException
	 *             if the text cannot be parsed or is no valid time
	 */
	public static LocalTime parseLocalTime(final String timeFormatPattern, final String timeString) {
		final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern(timeFormatPattern).withResolverStyle(ResolverStyle.STRICT);
		final LocalTime localTime = LocalTime.parse(timeString, timeFormatter);
		return localTime;
	}

	/**
	 * Parses a date time in the system default time zone.
	 *
	 * @param format
	 *            the format pattern, see {@link DateTimeFormatter}, must contain date and time fields
	 * @param dateTimeString
	 *            the text to parse
	 * @return the parsed date
	 * @throws java.time.format.DateTimeParseException
	 *             if the text cannot be parsed
	 */
	public static Date parseDateTime(final String format, final String dateTimeString) {
		final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(format);
		final LocalDateTime localDateTime = LocalDateTime.parse(dateTimeString, dateTimeFormatter);
		return getDateForLocalDateTime(localDateTime);
	}

	/**
	 * Parses a date time in the given time zone. A time zone or offset contained in the text takes
	 * precedence.
	 *
	 * @param format
	 *            the format pattern, see {@link DateTimeFormatter}, must contain date and time fields
	 * @param dateTimeString
	 *            the text to parse
	 * @param timeZone
	 *            the time zone of the value
	 * @return the parsed date
	 * @throws java.time.format.DateTimeParseException
	 *             if the text cannot be parsed
	 */
	public static Date parseDateTime(final String format, final String dateTimeString, final TimeZone timeZone) {
		return parseDateTime(format, dateTimeString, timeZone.toZoneId());
	}

	/**
	 * Parses a date time in the given time zone. A time zone or offset contained in the text takes
	 * precedence.
	 *
	 * @param format
	 *            the format pattern, see {@link DateTimeFormatter}, must contain date and time fields
	 * @param dateTimeString
	 *            the text to parse
	 * @param zoneId
	 *            the time zone of the value
	 * @return the parsed date
	 * @throws java.time.format.DateTimeParseException
	 *             if the text cannot be parsed
	 */
	public static Date parseDateTime(final String format, final String dateTimeString, final ZoneId zoneId) {
		DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(format);
		dateTimeFormatter = dateTimeFormatter.withZone(zoneId);
		// LocalDateTime.parse would ignore the zone, so the value would always be interpreted in the system default time zone
		final ZonedDateTime zonedDateTime = ZonedDateTime.parse(dateTimeString, dateTimeFormatter);
		return Date.from(zonedDateTime.toInstant());
	}

	/**
	 * Parses a zoned date time. A time zone or offset contained in the text takes precedence over the
	 * given time zone.
	 *
	 * @param format
	 *            the format pattern, see {@link DateTimeFormatter}, must contain date and time fields
	 * @param dateTimeString
	 *            the text to parse
	 * @param zoneId
	 *            the time zone for values without time zone information
	 * @return the parsed date time
	 * @throws java.time.format.DateTimeParseException
	 *             if the text cannot be parsed
	 */
	public static ZonedDateTime parseZonedDateTime(final String format, final String dateTimeString, final ZoneId zoneId) {
		DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(format);
		dateTimeFormatter = dateTimeFormatter.withZone(zoneId);
		final ZonedDateTime zonedDateTime = ZonedDateTime.parse(dateTimeString, dateTimeFormatter);
		return zonedDateTime;
	}

	/**
	 * OpenJDK 15+ doesn't recognize german three letter months by "MMM" in SimpleDateFormat anymore.
	 * So here is a helper to cope with that problem.
	 *
	 * @param threeLetterMonth
	 *            english or german three letter month name, case insensitive (e.g. "Mar" or "MÄR")
	 * @return month number (1 - 12)
	 * @throws Exception
	 *             if the month name is unknown
	 */
	public static int parseThreeLetterMonth(final String threeLetterMonth) throws Exception {
		switch(threeLetterMonth.toUpperCase(Locale.ROOT)) {
			case "JAN":
				return 1;
			case "FEB":
				return 2;
			case "MAR":
			case "M\u00C4R":
				return 3;
			case "APR":
				return 4;
			case "MAY":
			case "MAI":
				return 5;
			case "JUN":
				return 6;
			case "JUL":
				return 7;
			case "AUG":
				return 8;
			case "SEP":
				return 9;
			case "OCT":
			case "OKT":
				return 10;
			case "NOV":
				return 11;
			case "DEC":
			case "DEZ":
				return 12;
			default:
				throw new Exception("Unknown three letter month: " + threeLetterMonth);
		}
	}

	/**
	 * Converts milliseconds since 1970-01-01T00:00:00Z to a local date in the system default time zone.
	 *
	 * @param date1970Millis
	 *            the milliseconds since the epoch
	 * @return the local date
	 */
	public static LocalDate getLocalDateFor1970Millis(final long date1970Millis) {
		return getLocalDateForDate(new Date(date1970Millis));
	}

	/**
	 * Formats a date time in ISO 8601 format in UTC, like "2024-01-31T12:00:00Z".
	 *
	 * @param zonedDateTime
	 *            the date time to format
	 * @return the formatted date time
	 */
	public static String formatZonedDateTimeWithZuluTimezone(final ZonedDateTime zonedDateTime) {
		return DateUtilities.formatDate(DateUtilities.ISO_8601_DATETIME_FORMAT, zonedDateTime.withZoneSameInstant(ZoneId.of("UTC")));
	}
}
