package de.soderer.utilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link DateUtilities}. All tests use explicit time zones and locales. Methods depending on the
 * system default time zone or locale are tested with temporarily changed defaults.
 */
@SuppressWarnings("static-method")
public class DateUtilitiesTest {
	private static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");
	private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");
	private static final ZoneId UTC = ZoneId.of("UTC");

	/** Wednesday of ISO week 2 (even week) */
	private static final ZonedDateTime START = ZonedDateTime.of(2024, 1, 10, 12, 0, 0, 0, BERLIN);

	@FunctionalInterface
	private interface ThrowingRunnable {
		void run() throws Exception;
	}

	/**
	 * Runs code with a temporarily changed default time zone.
	 */
	private static void withDefaultTimeZone(final ZoneId zoneId, final ThrowingRunnable code) throws Exception {
		final TimeZone defaultTimeZone = TimeZone.getDefault();
		try {
			TimeZone.setDefault(TimeZone.getTimeZone(zoneId));
			code.run();
		} finally {
			TimeZone.setDefault(defaultTimeZone);
		}
	}

	/**
	 * Runs code with a temporarily changed default locale (all categories).
	 */
	private static void withDefaultLocale(final Locale locale, final ThrowingRunnable code) throws Exception {
		final Locale defaultLocale = Locale.getDefault();
		final Locale defaultFormatLocale = Locale.getDefault(Locale.Category.FORMAT);
		final Locale defaultDisplayLocale = Locale.getDefault(Locale.Category.DISPLAY);
		try {
			Locale.setDefault(locale);
			code.run();
		} finally {
			Locale.setDefault(defaultLocale);
			Locale.setDefault(Locale.Category.FORMAT, defaultFormatLocale);
			Locale.setDefault(Locale.Category.DISPLAY, defaultDisplayLocale);
		}
	}

	private static ZonedDateTime berlin(final int year, final int month, final int day, final int hour, final int minute) {
		return ZonedDateTime.of(year, month, day, hour, minute, 0, 0, BERLIN);
	}

	private static ZonedDateTime next(final ZonedDateTime start, final String timing) throws Exception {
		return DateUtilities.calculateNextJobStart(start, timing, start.getZone());
	}

	private static Date utcDate(final int year, final int month, final int day, final int hour, final int minute) {
		return Date.from(ZonedDateTime.of(year, month, day, hour, minute, 0, 0, UTC).toInstant());
	}

	// ------------------------------------------------------------------
	// Constants
	// ------------------------------------------------------------------

	@Test
	public void testFormatConstants() {
		final LocalDateTime value = LocalDateTime.of(2024, 1, 5, 7, 8, 9, 123_000_000);
		assertEquals("05.01.2024 07:08:09", DateUtilities.formatDate(DateUtilities.DD_MM_YYYY_HH_MM_SS, value));
		assertEquals("05.01.2024 07:08", DateUtilities.formatDate(DateUtilities.DD_MM_YYYY_HH_MM, value));
		assertEquals("05.01.2024", DateUtilities.formatDate(DateUtilities.DD_MM_YYYY, value));
		assertEquals("05012024", DateUtilities.formatDate(DateUtilities.DDMMYYYY, value));
		assertEquals("20240105", DateUtilities.formatDate(DateUtilities.YYYYMMDD, value));
		assertEquals("070809", DateUtilities.formatDate(DateUtilities.HHMMSS, value));
		assertEquals("0708", DateUtilities.formatDate(DateUtilities.HHMM, value));
		assertEquals("2024-01-05", DateUtilities.formatDate(DateUtilities.YYYY_MM_DD, value));
		assertEquals("2024-01-05 07:08", DateUtilities.formatDate(DateUtilities.YYYY_MM_DD_HH_MM, value));
		assertEquals("05_01_2024_07_08_09", DateUtilities.formatDate(DateUtilities.DD_MM_YYYY_HH_MM_SS_ForFileName, value));
		assertEquals("20240105070809", DateUtilities.formatDate(DateUtilities.YYYYMMDDHHMMSS, value));
		assertEquals("20240105070809123", DateUtilities.formatDate(DateUtilities.YYYYMMDDHHMMSSSSS, value));
		assertEquals("2024-01-05 07:08:09", DateUtilities.formatDate(DateUtilities.YYYY_MM_DD_HHMMSS, value));
		assertEquals("20240105-070809", DateUtilities.formatDate(DateUtilities.YYYYMMDD_HHMMSS, value));
		assertEquals("2024-01-05T07:08:09", DateUtilities.formatDate(DateUtilities.ISO_8601_DATETIME_FORMAT_NO_TIMEZONE, value));
		assertEquals("2024-01-05 07:08:09", DateUtilities.formatDate(DateUtilities.ANSI_SQL_DATETIME_FORMAT, value));
		assertEquals("2024-01-05", DateUtilities.formatDate(DateUtilities.ANSI_SQL_DATE_FORMAT, value));

		final ZonedDateTime zoned = ZonedDateTime.of(2024, 1, 5, 7, 8, 9, 0, ZoneOffset.ofHours(2));
		assertEquals("2024-01-05T07:08:09+02", DateUtilities.formatDate(DateUtilities.ISO_8601_DATETIME_FORMAT, zoned));
		assertEquals("2024-01-05+02", DateUtilities.formatDate(DateUtilities.ISO_8601_DATE_FORMAT, zoned));
		assertEquals("07:08:09+02", DateUtilities.formatDate(DateUtilities.ISO_8601_TIME_FORMAT, zoned));
		assertEquals("07:08:09.000000000+02", DateUtilities.formatDate(DateUtilities.ISO_8601_TIME_WITH_NANOS_FORMAT, zoned));
		assertEquals("2024-01-05T07:08:09Z", DateUtilities.formatDate(DateUtilities.ISO_8601_DATETIME_FORMAT, zoned.withZoneSameInstant(UTC).plusHours(2)));
	}

	@Test
	public void testIso8601WithNanosFormatParsesOptionalFraction() {
		final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DateUtilities.ISO_8601_DATETIME_WITH_NANOS_FORMAT_NO_TIMEZONE);
		assertEquals(LocalDateTime.of(2024, 1, 5, 7, 8, 9), LocalDateTime.parse("2024-01-05T07:08:09", formatter));
		assertEquals(LocalDateTime.of(2024, 1, 5, 7, 8, 9, 123_456_789), LocalDateTime.parse("2024-01-05T07:08:09.123456789", formatter));
	}

	// ------------------------------------------------------------------
	// Weekday names
	// ------------------------------------------------------------------

	@Test
	public void testGetWeekdayNameShort() throws Exception {
		final GregorianCalendar wednesday = new GregorianCalendar(2024, Calendar.JANUARY, 10);
		withDefaultLocale(Locale.GERMANY, () -> assertEquals("Mi", DateUtilities.getWeekdayNameShort(wednesday)));
		withDefaultLocale(Locale.US, () -> assertEquals("We", DateUtilities.getWeekdayNameShort(wednesday)));
		withDefaultLocale(Locale.GERMANY, () -> assertEquals("So", DateUtilities.getWeekdayNameShort(new GregorianCalendar(2024, Calendar.JANUARY, 14))));
	}

	@Test
	public void testGetDayOfWeekByNamePartEnglish() {
		assertEquals(DayOfWeek.MONDAY, DateUtilities.getDayOfWeekByNamePart("mo"));
		assertEquals(DayOfWeek.MONDAY, DateUtilities.getDayOfWeekByNamePart("Monday"));
		assertEquals(DayOfWeek.TUESDAY, DateUtilities.getDayOfWeekByNamePart("Tue"));
		assertEquals(DayOfWeek.TUESDAY, DateUtilities.getDayOfWeekByNamePart("tu"));
		assertEquals(DayOfWeek.WEDNESDAY, DateUtilities.getDayOfWeekByNamePart("WE"));
		assertEquals(DayOfWeek.THURSDAY, DateUtilities.getDayOfWeekByNamePart("th"));
		assertEquals(DayOfWeek.FRIDAY, DateUtilities.getDayOfWeekByNamePart("FRIDAY"));
		assertEquals(DayOfWeek.SATURDAY, DateUtilities.getDayOfWeekByNamePart("sat"));
		assertEquals(DayOfWeek.SUNDAY, DateUtilities.getDayOfWeekByNamePart("su"));
		assertEquals(DayOfWeek.FRIDAY, DateUtilities.getDayOfWeekByNamePart(" fr "));
	}

	@Test
	public void testGetDayOfWeekByNamePartGerman() {
		assertEquals(DayOfWeek.MONDAY, DateUtilities.getDayOfWeekByNamePart("Montag"));
		assertEquals(DayOfWeek.TUESDAY, DateUtilities.getDayOfWeekByNamePart("Di"));
		assertEquals(DayOfWeek.TUESDAY, DateUtilities.getDayOfWeekByNamePart("Dienstag"));
		assertEquals(DayOfWeek.WEDNESDAY, DateUtilities.getDayOfWeekByNamePart("Mi"));
		assertEquals(DayOfWeek.WEDNESDAY, DateUtilities.getDayOfWeekByNamePart("Mittwoch"));
		assertEquals(DayOfWeek.THURSDAY, DateUtilities.getDayOfWeekByNamePart("Do"));
		assertEquals(DayOfWeek.THURSDAY, DateUtilities.getDayOfWeekByNamePart("Donnerstag"));
		assertEquals(DayOfWeek.FRIDAY, DateUtilities.getDayOfWeekByNamePart("Freitag"));
		assertEquals(DayOfWeek.SATURDAY, DateUtilities.getDayOfWeekByNamePart("Samstag"));
		assertEquals(DayOfWeek.SUNDAY, DateUtilities.getDayOfWeekByNamePart("So"));
		assertEquals(DayOfWeek.SUNDAY, DateUtilities.getDayOfWeekByNamePart("Sonntag"));
	}

	@Test
	public void testGetDayOfWeekByNamePartInvalid() {
		assertNull(DateUtilities.getDayOfWeekByNamePart(null));
		assertNull(DateUtilities.getDayOfWeekByNamePart(""));
		assertNull(DateUtilities.getDayOfWeekByNamePart("   "));
		// Single letters are ambiguous
		assertNull(DateUtilities.getDayOfWeekByNamePart("s"));
		assertNull(DateUtilities.getDayOfWeekByNamePart("t"));
		assertNull(DateUtilities.getDayOfWeekByNamePart("m"));
		assertNull(DateUtilities.getDayOfWeekByNamePart("xx"));
		assertNull(DateUtilities.getDayOfWeekByNamePart("Ev"));
		assertNull(DateUtilities.getDayOfWeekByNamePart("Od"));
	}

	// ------------------------------------------------------------------
	// Date text conversions
	// ------------------------------------------------------------------

	@Test
	public void testConvertDdMMyyyyToYyyyMMdd() {
		assertEquals("2024-12-31", DateUtilities.convert_ddMMyyyy_to_yyyyMMdd("31.12.2024"));
		assertEquals("2024-12-31", DateUtilities.convert_ddMMyyyy_to_yyyyMMdd("31-12-2024"));
		assertNull(DateUtilities.convert_ddMMyyyy_to_yyyyMMdd(null));
		assertThrows(IllegalArgumentException.class, () -> DateUtilities.convert_ddMMyyyy_to_yyyyMMdd("2024-12-31"));
		assertThrows(IllegalArgumentException.class, () -> DateUtilities.convert_ddMMyyyy_to_yyyyMMdd("1.12.2024"));
		assertThrows(IllegalArgumentException.class, () -> DateUtilities.convert_ddMMyyyy_to_yyyyMMdd("31/12/2024"));
		assertThrows(IllegalArgumentException.class, () -> DateUtilities.convert_ddMMyyyy_to_yyyyMMdd("31.12.2024 "));
		assertThrows(IllegalArgumentException.class, () -> DateUtilities.convert_ddMMyyyy_to_yyyyMMdd("3a.12.2024"));
		assertThrows(IllegalArgumentException.class, () -> DateUtilities.convert_ddMMyyyy_to_yyyyMMdd(""));
	}

	@Test
	public void testConvertYyyyMMddToDdMMyyyy() {
		assertEquals("31.12.2024", DateUtilities.convert_yyyyMMdd_to_ddMMyyyy("2024-12-31"));
		assertEquals("31.12.2024", DateUtilities.convert_yyyyMMdd_to_ddMMyyyy("2024.12.31"));
		assertNull(DateUtilities.convert_yyyyMMdd_to_ddMMyyyy(null));
		assertThrows(IllegalArgumentException.class, () -> DateUtilities.convert_yyyyMMdd_to_ddMMyyyy("31.12.2024"));
		assertThrows(IllegalArgumentException.class, () -> DateUtilities.convert_yyyyMMdd_to_ddMMyyyy("2024-12-3"));
		assertThrows(IllegalArgumentException.class, () -> DateUtilities.convert_yyyyMMdd_to_ddMMyyyy("2024/12/31"));
		assertThrows(IllegalArgumentException.class, () -> DateUtilities.convert_yyyyMMdd_to_ddMMyyyy("20241231"));
	}

	@Test
	public void testConvertRoundTrip() {
		assertEquals("05.01.2024", DateUtilities.convert_yyyyMMdd_to_ddMMyyyy(DateUtilities.convert_ddMMyyyy_to_yyyyMMdd("05.01.2024")));
	}

	@Test
	public void testReplaceDatePatternInString() {
		final LocalDateTime value = LocalDateTime.of(2024, 1, 5, 7, 8, 9);
		assertEquals("backup_20240105_070809.zip", DateUtilities.replaceDatePatternInString("backup_[yyyy][MM][dd]_[HH][mm][ss].zip", value));
		assertEquals("2024-01-05 07:08:09", DateUtilities.replaceDatePatternInString("[YYYY]-[MM]-[DD] [hh]:[mm]:[SS]", value));
		assertEquals("0999", DateUtilities.replaceDatePatternInString("[yyyy]", LocalDateTime.of(999, 1, 1, 0, 0)));
		assertEquals("23", DateUtilities.replaceDatePatternInString("[HH]", LocalDateTime.of(2024, 1, 1, 23, 0)));
		// Repeated placeholders
		assertEquals("05/05", DateUtilities.replaceDatePatternInString("[dd]/[dd]", value));
		// Escaped brackets are kept as plain brackets without replacement
		assertEquals("[yyyy] 2024", DateUtilities.replaceDatePatternInString("\\[yyyy\\] [yyyy]", value));
		assertEquals("no placeholders", DateUtilities.replaceDatePatternInString("no placeholders", value));
		assertEquals("[xx]", DateUtilities.replaceDatePatternInString("[xx]", value));
		assertNull(DateUtilities.replaceDatePatternInString(null, value));
	}

	@Test
	public void testReplaceDatePatternInStringUsesAsciiDigits() throws Exception {
		// String.format with the default locale would write Arabic-Indic digits into file names
		withDefaultLocale(Locale.forLanguageTag("ar-EG"), () -> assertEquals("backup_20240105_070809.zip",
				DateUtilities.replaceDatePatternInString("backup_[yyyy][MM][dd]_[HH][mm][ss].zip", LocalDateTime.of(2024, 1, 5, 7, 8, 9))));
	}

	// ------------------------------------------------------------------
	// ETA and timespans
	// ------------------------------------------------------------------

	@Test
	public void testCalculateETAInvalidValues() {
		final LocalDateTime past = LocalDateTime.now().minusSeconds(10);
		assertNull(DateUtilities.calculateETA(null, 10, 5));
		assertNull(DateUtilities.calculateETA(past, 0, 0));
		assertNull(DateUtilities.calculateETA(past, 10, 0));
		assertNull(DateUtilities.calculateETA(past, -10, 5));
		assertNull(DateUtilities.calculateETA(past, 10, -1));
		assertNull(DateUtilities.calculateETA(past, 5, 10));
		assertNull(DateUtilities.calculateETA(LocalDateTime.now().plusHours(1), 10, 5));
	}

	@Test
	public void testCalculateETA() {
		final LocalDateTime start = LocalDateTime.now().minusSeconds(10);
		final LocalDateTime eta = DateUtilities.calculateETA(start, 100, 50);
		assertNotNull(eta);
		// Half of the items took at least 10 seconds, so all items take at least 20 seconds
		assertFalse(eta.isBefore(start.plusSeconds(20)), "ETA " + eta);
		assertTrue(eta.isBefore(start.plusSeconds(30)), "ETA " + eta);

		final LocalDateTime quarterEta = DateUtilities.calculateETA(start, 4, 1);
		assertFalse(quarterEta.isBefore(start.plusSeconds(40)), "ETA " + quarterEta);
		assertTrue(quarterEta.isBefore(start.plusSeconds(60)), "ETA " + quarterEta);
	}

	@Test
	public void testCalculateETAAllDone() {
		final LocalDateTime before = LocalDateTime.now();
		final LocalDateTime eta = DateUtilities.calculateETA(before.minusMinutes(1), 10, 10);
		assertFalse(eta.isBefore(before));
		assertFalse(eta.isAfter(LocalDateTime.now()));
	}

	@Test
	public void testGetShortHumanReadableTimespanZero() {
		assertEquals("0s", DateUtilities.getShortHumanReadableTimespan(Duration.ZERO, false, false));
		assertEquals("00s", DateUtilities.getShortHumanReadableTimespan(Duration.ZERO, false, true));
		assertEquals("0s", DateUtilities.getShortHumanReadableTimespan(Duration.ZERO, true, false));
		// Milliseconds are hidden without showMillis
		assertEquals("0s", DateUtilities.getShortHumanReadableTimespan(Duration.ofMillis(5), false, false));
	}

	@Test
	public void testGetShortHumanReadableTimespanSecondsAndMillis() {
		assertEquals("5ms", DateUtilities.getShortHumanReadableTimespan(Duration.ofMillis(5), true, false));
		assertEquals("005ms", DateUtilities.getShortHumanReadableTimespan(Duration.ofMillis(5), true, true));
		assertEquals("1s 500ms", DateUtilities.getShortHumanReadableTimespan(Duration.ofMillis(1500), true, false));
		assertEquals("01s 500ms", DateUtilities.getShortHumanReadableTimespan(Duration.ofMillis(1500), true, true));
		assertEquals("1s", DateUtilities.getShortHumanReadableTimespan(Duration.ofMillis(1500), false, false));
		assertEquals("59s", DateUtilities.getShortHumanReadableTimespan(Duration.ofSeconds(59), false, false));
	}

	@Test
	public void testGetShortHumanReadableTimespanShowsOnlySignificantParts() {
		// Milliseconds only below one minute
		assertEquals("1m 1s", DateUtilities.getShortHumanReadableTimespan(Duration.ofMillis(61_500), true, false));
		assertEquals("1m 3s", DateUtilities.getShortHumanReadableTimespan(Duration.ofSeconds(63), false, false));
		assertEquals("01m 03s", DateUtilities.getShortHumanReadableTimespan(Duration.ofSeconds(63), false, true));
		// Seconds only below one hour
		assertEquals("1h 2m", DateUtilities.getShortHumanReadableTimespan(Duration.ofSeconds(3723), false, false));
		assertEquals("01h 02m", DateUtilities.getShortHumanReadableTimespan(Duration.ofSeconds(3723), false, true));
		assertEquals("1h", DateUtilities.getShortHumanReadableTimespan(Duration.ofHours(1), false, false));
		// Minutes only below one day
		assertEquals("1d 2h", DateUtilities.getShortHumanReadableTimespan(Duration.ofDays(1).plusHours(2).plusMinutes(3), false, false));
		// Hours only below one week
		assertEquals("1w 1d", DateUtilities.getShortHumanReadableTimespan(Duration.ofDays(8).plusHours(5), false, false));
		assertEquals("02w 03d", DateUtilities.getShortHumanReadableTimespan(Duration.ofDays(17), false, true));
	}

	@Test
	public void testGetShortHumanReadableTimespanYears() {
		// A year is counted as 52 weeks (364 days), days only below one year
		assertEquals("1y", DateUtilities.getShortHumanReadableTimespan(Duration.ofDays(364), false, false));
		assertEquals("1y", DateUtilities.getShortHumanReadableTimespan(Duration.ofDays(365), false, false));
		assertEquals("51w 6d", DateUtilities.getShortHumanReadableTimespan(Duration.ofDays(363), false, false));
		assertEquals("1y 1w", DateUtilities.getShortHumanReadableTimespan(Duration.ofDays(372), false, false));
		assertEquals("1y 01w", DateUtilities.getShortHumanReadableTimespan(Duration.ofDays(372), false, true));
		assertEquals("10y", DateUtilities.getShortHumanReadableTimespan(Duration.ofDays(3640), false, true));
	}

	@Test
	public void testGetShortHumanReadableTimespanNegative() {
		assertEquals("-1m 3s", DateUtilities.getShortHumanReadableTimespan(Duration.ofSeconds(-63), false, false));
		assertEquals("-500ms", DateUtilities.getShortHumanReadableTimespan(Duration.ofMillis(-500), true, false));
		assertEquals("-1d 2h", DateUtilities.getShortHumanReadableTimespan(Duration.ofHours(-26), false, false));
	}

	@Test
	public void testGetShortHumanReadableTimespanUsesAsciiDigits() throws Exception {
		// Values with and without leading zeros must not mix digit systems
		withDefaultLocale(Locale.forLanguageTag("ar-EG"), () -> {
			assertEquals("1y 01w", DateUtilities.getShortHumanReadableTimespan(Duration.ofDays(372), false, true));
			assertEquals("01h 02m", DateUtilities.getShortHumanReadableTimespan(Duration.ofSeconds(3723), false, true));
			assertEquals("01s 005ms", DateUtilities.getShortHumanReadableTimespan(Duration.ofMillis(1005), true, true));
			assertEquals("02w 03d", DateUtilities.getShortHumanReadableTimespan(Duration.ofDays(17), false, true));
		});
	}

	@Test
	public void testGetHumanReadableTimespanEnglish() {
		assertEquals("0 seconds", DateUtilities.getHumanReadableTimespanEnglish(Duration.ZERO, false));
		assertEquals("0 millis", DateUtilities.getHumanReadableTimespanEnglish(Duration.ZERO, true));
		assertEquals("0 seconds", DateUtilities.getHumanReadableTimespanEnglish(Duration.ofMillis(5), false));
		assertEquals("5 millis", DateUtilities.getHumanReadableTimespanEnglish(Duration.ofMillis(5), true));
		assertEquals("1 seconds 500 millis", DateUtilities.getHumanReadableTimespanEnglish(Duration.ofMillis(1500), true));
		assertEquals("1 seconds", DateUtilities.getHumanReadableTimespanEnglish(Duration.ofMillis(1500), false));
		// All non-zero parts are shown
		assertEquals("1 hours 2 minutes 3 seconds", DateUtilities.getHumanReadableTimespanEnglish(Duration.ofSeconds(3723), false));
		assertEquals("1 hours 3 seconds", DateUtilities.getHumanReadableTimespanEnglish(Duration.ofSeconds(3603), false));
		assertEquals("1 weeks 1 days 5 hours", DateUtilities.getHumanReadableTimespanEnglish(Duration.ofDays(8).plusHours(5), false));
		assertEquals("1 years 1 days", DateUtilities.getHumanReadableTimespanEnglish(Duration.ofDays(365), false));
		assertEquals("-1 minutes 3 seconds", DateUtilities.getHumanReadableTimespanEnglish(Duration.ofSeconds(-63), false));
	}

	@Test
	public void testGetHumanReadableTimespanUsesLangResources() {
		final String hours = LangResources.get("hours");
		final String minutes = LangResources.get("minutes");
		final String seconds = LangResources.get("seconds");
		final String millis = LangResources.get("millis");
		final String days = LangResources.get("days");
		final String weeks = LangResources.get("weeks");
		final String years = LangResources.get("years");
		assertEquals("0 " + seconds, DateUtilities.getHumanReadableTimespan(Duration.ZERO, false));
		assertEquals("0 " + millis, DateUtilities.getHumanReadableTimespan(Duration.ZERO, true));
		assertEquals("1 " + hours + " 2 " + minutes + " 3 " + seconds, DateUtilities.getHumanReadableTimespan(Duration.ofSeconds(3723), false));
		assertEquals("1 " + seconds + " 500 " + millis, DateUtilities.getHumanReadableTimespan(Duration.ofMillis(1500), true));
		assertEquals("1 " + years + " 1 " + weeks + " 1 " + days, DateUtilities.getHumanReadableTimespan(Duration.ofDays(372), false));
		assertEquals("-1 " + minutes, DateUtilities.getHumanReadableTimespan(Duration.ofMinutes(-1), false));
	}

	@Test
	public void testGetDuration() {
		final GregorianCalendar start = new GregorianCalendar();
		start.setTimeInMillis(1_700_000_000_000L);
		final GregorianCalendar end = new GregorianCalendar();

		end.setTimeInMillis(start.getTimeInMillis());
		assertEquals("0ms", DateUtilities.getDuration(start, end));

		end.setTimeInMillis(start.getTimeInMillis() + Duration.ofDays(1).plusHours(2).plusMinutes(3).plusSeconds(4).plusMillis(5).toMillis());
		assertEquals("1d 2h 3m 4s 5ms", DateUtilities.getDuration(start, end));
		assertEquals("-1d 2h 3m 4s 5ms", DateUtilities.getDuration(end, start));

		// Zero parts are omitted, also between other parts
		end.setTimeInMillis(start.getTimeInMillis() + Duration.ofHours(2).plusMillis(5).toMillis());
		assertEquals("2h 5ms", DateUtilities.getDuration(start, end));

		end.setTimeInMillis(start.getTimeInMillis() + Duration.ofDays(400).toMillis());
		assertEquals("400d 0ms", DateUtilities.getDuration(start, end));
	}

	// ------------------------------------------------------------------
	// calculateNextJobStart
	// ------------------------------------------------------------------

	@Test
	public void testNextJobStartOnceOrEmpty() throws Exception {
		assertNull(next(START, "ONCE"));
		assertNull(next(START, "once"));
		assertNull(next(START, " Once "));
		assertNull(next(START, ""));
		assertNull(next(START, "   "));
		assertNull(DateUtilities.calculateNextJobStart(START, null, BERLIN));
		assertNull(DateUtilities.calculateNextJobStart("ONCE"));
		assertNull(DateUtilities.calculateNextJobStart("ONCE", BERLIN));
	}

	@Test
	public void testNextJobStartDailyTime() throws Exception {
		assertEquals(berlin(2024, 1, 10, 13, 0), next(START, "1300"));
		assertEquals(berlin(2024, 1, 11, 6, 0), next(START, "0600"));
		// The calculation start itself is not a next start
		assertEquals(berlin(2024, 1, 11, 12, 0), next(START, "1200"));
		assertEquals(berlin(2024, 1, 10, 12, 1), next(START, "1201"));
		assertEquals(berlin(2024, 1, 11, 0, 0), next(START, "0000"));
		assertEquals(berlin(2024, 1, 10, 23, 59), next(START, "2359"));
	}

	@Test
	public void testNextJobStartMultipleTimesUsesEarliest() throws Exception {
		assertEquals(berlin(2024, 1, 10, 13, 0), next(START, "0600;1300"));
		assertEquals(berlin(2024, 1, 10, 13, 0), next(START, "1300;0600"));
		assertEquals(berlin(2024, 1, 10, 13, 0), next(START, "1800;1300;0600"));
		// Other separators and empty parts
		assertEquals(berlin(2024, 1, 10, 13, 0), next(START, "0600, 1300"));
		assertEquals(berlin(2024, 1, 10, 13, 0), next(START, "0600 1300"));
		assertEquals(berlin(2024, 1, 10, 13, 0), next(START, "0600; 1300"));
		assertEquals(berlin(2024, 1, 10, 13, 0), next(START, ";0600;;1300;"));
		assertEquals(berlin(2024, 1, 10, 13, 0), next(START, " 1300 "));
	}

	@Test
	public void testNextJobStartWithoutStartUsesNow() throws Exception {
		final ZonedDateTime before = ZonedDateTime.now(BERLIN);
		final ZonedDateTime nextStart = DateUtilities.calculateNextJobStart("0000;0600;1200;1800", BERLIN);
		assertTrue(nextStart.isAfter(before));
		assertFalse(nextStart.isAfter(before.plusHours(6).plusMinutes(1)));
		assertEquals(BERLIN, nextStart.getZone());

		final ZonedDateTime withNullZone = DateUtilities.calculateNextJobStart(null, "0000;0600;1200;1800", null);
		assertEquals(ZoneId.systemDefault(), withNullZone.getZone());
	}

	@Test
	public void testNextJobStartWeekdays() throws Exception {
		// START is a Wednesday
		assertEquals(berlin(2024, 1, 10, 17, 0), next(START, "MoMi:1700"));
		assertEquals(berlin(2024, 1, 15, 17, 0), next(START, "Mo:1700"));
		assertEquals(berlin(2024, 1, 17, 11, 0), next(START, "Mi:1100"));
		assertEquals(berlin(2024, 1, 11, 8, 0), next(START, "Th:0800"));
		assertEquals(berlin(2024, 1, 11, 8, 0), next(START, "MoTuWeThFr:0800"));
		assertEquals(berlin(2024, 1, 11, 8, 0), next(START, "MoDiMiDoFr:0800"));
		assertEquals(berlin(2024, 1, 13, 10, 0), next(START, "SaSo:1000"));
		// Weekday without time means midnight
		assertEquals(berlin(2024, 1, 12, 0, 0), next(START, "Fr"));
		assertEquals(berlin(2024, 1, 12, 0, 0), next(START, "SaFr"));
	}

	@Test
	public void testNextJobStartEvenAndOddWeeks() throws Exception {
		// January 15th 2024 is in ISO week 3, January 22nd in week 4
		assertEquals(berlin(2024, 1, 22, 8, 0), next(START, "MoEv:0800"));
		assertEquals(berlin(2024, 1, 15, 8, 0), next(START, "MoOd:0800"));
		assertEquals(berlin(2024, 1, 15, 8, 0), next(START, "OdMo:0800"));
		// START is in the even week 2
		assertEquals(berlin(2024, 1, 10, 17, 0), next(START, "MiEv:1700"));
		assertEquals(berlin(2024, 1, 17, 17, 0), next(START, "MiOd:1700"));
	}

	@Test
	public void testNextJobStartInvalidWeekdays() {
		assertThrows(Exception.class, () -> next(START, "Xx:1700"));
		assertThrows(Exception.class, () -> next(START, "Mo:17"));
		assertThrows(Exception.class, () -> next(START, "Mo:2400"));
		assertThrows(Exception.class, () -> next(START, "Mo:1760"));
		assertThrows(Exception.class, () -> next(START, "Mo:17a0"));
		assertThrows(Exception.class, () -> next(START, "Mon:1700"));
		assertThrows(Exception.class, () -> next(START, "abc"));
		// Even or odd weeks without weekday, or both together, never match
		assertThrows(Exception.class, () -> next(START, "Ev:1700"));
		assertThrows(Exception.class, () -> next(START, "Od"));
		assertThrows(Exception.class, () -> next(START, "MoEvOd:1700"));
	}

	@Test
	public void testNextJobStartMonthDay() throws Exception {
		assertEquals(berlin(2024, 2, 5, 16, 0), next(START, "M05:1600"));
		assertEquals(berlin(2024, 1, 15, 16, 0), next(START, "M15:1600"));
		assertEquals(berlin(2024, 1, 10, 13, 0), next(START, "M10:1300"));
		assertEquals(berlin(2024, 2, 10, 12, 0), next(START, "M10:1200"));
	}

	@Test
	public void testNextJobStartMonthUltimo() throws Exception {
		assertEquals(berlin(2024, 1, 31, 17, 0), next(START, "M99:1700"));
		assertEquals(berlin(2024, 2, 29, 17, 0), next(berlin(2024, 2, 1, 0, 0), "M99:1700"));
		assertEquals(berlin(2023, 2, 28, 17, 0), next(berlin(2023, 2, 1, 0, 0), "M99:1700"));
		assertEquals(berlin(2024, 3, 31, 17, 0), next(berlin(2024, 2, 29, 18, 0), "M99:1700"));
	}

	@Test
	public void testNextJobStartMonthDayBeyondEndOfMonth() throws Exception {
		// Day 31 means the last day of shorter months
		assertEquals(berlin(2024, 2, 29, 17, 0), next(berlin(2024, 2, 1, 0, 0), "M31:1700"));
		assertEquals(berlin(2024, 4, 30, 17, 0), next(berlin(2024, 4, 1, 0, 0), "M31:1700"));
		// After a short month the day is 31 again
		assertEquals(berlin(2024, 3, 31, 17, 0), next(berlin(2024, 2, 29, 18, 0), "M31:1700"));
		assertEquals(berlin(2024, 3, 30, 17, 0), next(berlin(2024, 2, 29, 18, 0), "M30:1700"));
	}

	@Test
	public void testNextJobStartMonthInterval() throws Exception {
		assertEquals(berlin(2024, 4, 1, 0, 0), next(START, "03M01:0000"));
		assertEquals(berlin(2024, 7, 1, 17, 0), next(START, "06M01:1700"));
		assertEquals(berlin(2024, 2, 1, 17, 0), next(START, "01M01:1700"));
		assertEquals(berlin(2024, 12, 31, 17, 0), next(START, "12M99:1700"));
	}

	@Test
	public void testNextJobStartInvalidMonthRules() {
		assertThrows(Exception.class, () -> next(START, "00M01:1000"));
		assertThrows(Exception.class, () -> next(START, "M00:1000"));
		assertThrows(Exception.class, () -> next(START, "M32:1000"));
		assertThrows(Exception.class, () -> next(START, "M98:1000"));
		assertThrows(Exception.class, () -> next(START, "M05:2400"));
		assertThrows(Exception.class, () -> next(START, "M5:1600"));
		assertThrows(Exception.class, () -> next(START, "M05:160"));
	}

	@Test
	public void testNextJobStartQuarter() throws Exception {
		assertEquals(berlin(2024, 4, 1, 16, 0), next(START, "Q:1600"));
		assertEquals(berlin(2024, 7, 1, 16, 0), next(berlin(2024, 4, 1, 16, 0), "Q:1600"));
		assertEquals(berlin(2025, 1, 1, 16, 0), next(berlin(2024, 12, 31, 23, 0), "Q:1600"));
		// The start of the current quarter may still be ahead
		assertEquals(berlin(2024, 1, 1, 16, 0), next(berlin(2024, 1, 1, 8, 0), "Q:1600"));
	}

	@Test
	public void testNextJobStartQuarterWorkingDay() throws Exception {
		// July 1st 2023 is a Saturday
		assertEquals(berlin(2023, 7, 1, 16, 0), next(berlin(2023, 5, 10, 0, 0), "Q:1600"));
		assertEquals(berlin(2023, 7, 3, 16, 0), next(berlin(2023, 5, 10, 0, 0), "QW:1600"));
		// The first working day is still ahead, though the first day of the quarter has passed
		assertEquals(berlin(2023, 7, 3, 16, 0), next(berlin(2023, 7, 2, 12, 0), "QW:1600"));
		// April 1st 2024 is a Monday
		assertEquals(berlin(2024, 4, 1, 16, 0), next(START, "QW:1600"));
		assertThrows(Exception.class, () -> next(START, "Q:16"));
		assertThrows(Exception.class, () -> next(START, "QW:2500"));
	}

	@Test
	public void testNextJobStartNthWeekdayOfMonth() throws Exception {
		// Second Monday: January 8th has passed, so February 12th
		assertEquals(berlin(2024, 2, 12, 17, 0), next(START, "2Mo:1700"));
		// First Friday of February 2024
		assertEquals(berlin(2024, 2, 2, 9, 0), next(START, "1Fr:0900"));
		// Second Wednesday of January 2024 is START's day
		assertEquals(berlin(2024, 1, 10, 13, 0), next(START, "2Mi:1300"));
		assertEquals(berlin(2024, 2, 14, 12, 0), next(START, "2We:1200"));
		// Fifth Thursday: February 29th 2024
		assertEquals(berlin(2024, 2, 29, 10, 0), next(START, "5Th:1000"));

		assertThrows(Exception.class, () -> next(START, "0Mo:1700"));
		assertThrows(Exception.class, () -> next(START, "6Mo:1700"));
		assertThrows(Exception.class, () -> next(START, "2Xx:1700"));
		assertThrows(Exception.class, () -> next(START, "2Mo:9999"));
	}

	@Test
	public void testNextJobStartFixedDate() throws Exception {
		assertEquals(berlin(2030, 12, 24, 0, 0), next(START, "24122030"));
		// Fixed dates in the past have no next start
		assertNull(next(START, "01012020"));
		assertNull(next(START, "01012020;10012024"));
		// Other rules are still used
		assertEquals(berlin(2024, 1, 11, 6, 0), next(START, "01012020;0600"));
		assertEquals(berlin(2024, 1, 11, 0, 0), next(START, "11012024;0600"));
		// Invalid dates are rejected instead of being moved to another day
		assertThrows(Exception.class, () -> next(START, "31022026"));
		assertThrows(Exception.class, () -> next(START, "00012026"));
		assertThrows(Exception.class, () -> next(START, "01132026"));
	}

	@Test
	public void testNextJobStartInvalidLengths() {
		assertThrows(Exception.class, () -> next(START, "123"));
		assertThrows(Exception.class, () -> next(START, "12345"));
		assertThrows(Exception.class, () -> next(START, "2500"));
		assertThrows(Exception.class, () -> next(START, "1260"));
		assertThrows(Exception.class, () -> next(START, "0600;xyz"));
	}

	@Test
	public void testNextJobStartExclusions() throws Exception {
		// START's day is excluded
		assertEquals(berlin(2024, 1, 11, 13, 0), next(START, "1300;!10012024"));
		assertEquals(berlin(2024, 1, 11, 17, 0), next(START, "MoDiMiDoFr:1700;!10012024"));
		assertEquals(berlin(2024, 1, 15, 17, 0), next(START, "!10012024;MoDiMiDoFr:1700;!11012024;!12012024"));
		// Excluded fixed date
		assertNull(next(START, "24122030;!24122030"));
		// Excluded month day is moved to the following day
		assertEquals(berlin(2024, 2, 6, 16, 0), next(START, "M05:1600;!05022024"));
		// Excluded first day of quarter
		assertEquals(berlin(2024, 4, 2, 16, 0), next(START, "Q:1600;!01042024"));
		// Excluded nth weekday is skipped to the next match
		assertEquals(berlin(2024, 3, 11, 17, 0), next(START, "2Mo:1700;!12022024"));
		// An invalid exclusion date is rejected
		assertThrows(Exception.class, () -> next(START, "1300;!32012024"));
		assertThrows(Exception.class, () -> next(START, "1300;!1001"));
	}

	@Test
	public void testNextJobStartTimeWildcards() throws Exception {
		assertEquals(berlin(2024, 1, 10, 14, 5), next(START, "*4*5"));
		assertEquals(berlin(2024, 1, 10, 13, 0), next(START, "**00"));
		assertEquals(berlin(2024, 1, 10, 12, 1), next(START, "****"));
		assertEquals(berlin(2024, 1, 11, 4, 5), next(berlin(2024, 1, 10, 23, 0), "*4*5"));
		assertEquals(berlin(2024, 1, 11, 4, 5), next(START, "*4*5;!10012024"));
		// Seconds of the start are ignored, but the start itself is never returned
		assertEquals(berlin(2024, 1, 10, 13, 0), next(START.withHour(13).minusSeconds(1), "**00"));
	}

	@Test
	public void testNextJobStartInvalidTimeWildcards() {
		assertThrows(Exception.class, () -> next(START, "*4*"));
		assertThrows(Exception.class, () -> next(START, "*4*55"));
		assertThrows(Exception.class, () -> next(START, "*a*5"));
		// Never matching patterns do not loop endlessly
		assertThrows(Exception.class, () -> next(START, "**99"));
		assertThrows(Exception.class, () -> next(START, "3***"));
	}

	@Test
	public void testNextJobStartCombinedRules() throws Exception {
		assertEquals(berlin(2024, 1, 10, 20, 0), next(START, "Fr:0800;Mi:2000"));
		assertEquals(berlin(2024, 1, 10, 13, 0), next(START, "M15:1600;1300;Q:0000"));
		assertEquals(berlin(2024, 1, 12, 8, 0), next(START, "Fr:0800;M15:1600"));
	}

	@Test
	public void testNextJobStartDaylightSavingTime() throws Exception {
		// March 31st 2024 02:30 does not exist in Berlin, the time is moved to 03:30
		assertEquals(ZonedDateTime.of(2024, 3, 31, 3, 30, 0, 0, BERLIN), next(berlin(2024, 3, 30, 12, 0), "0230"));
		// The following day is at 02:30 again
		final ZonedDateTime afterGap = next(berlin(2024, 3, 31, 12, 0), "0230");
		assertEquals(LocalDateTime.of(2024, 4, 1, 2, 30), afterGap.toLocalDateTime());
		assertEquals(ZoneOffset.ofHours(2), afterGap.getOffset());

		// Weekday and month rules keep the wall clock time across the change
		assertEquals(LocalDateTime.of(2024, 4, 1, 8, 0), next(berlin(2024, 3, 25, 12, 0), "Mo:0800").toLocalDateTime());
		assertEquals(LocalDateTime.of(2024, 4, 5, 8, 0), next(berlin(2024, 3, 25, 12, 0), "M05:0800").toLocalDateTime());
		assertEquals(LocalDateTime.of(2024, 10, 28, 8, 0), next(berlin(2024, 10, 21, 12, 0), "Mo:0800").toLocalDateTime());
	}

	@Test
	public void testNextJobStartInOtherZoneThanStart() throws Exception {
		// 12:00 UTC is 13:00 in Berlin
		final ZonedDateTime utcStart = ZonedDateTime.of(2024, 1, 10, 12, 0, 0, 0, UTC);
		assertEquals(berlin(2024, 1, 10, 13, 30), DateUtilities.calculateNextJobStart(utcStart, "1330", BERLIN));
		assertEquals(berlin(2024, 1, 11, 12, 30), DateUtilities.calculateNextJobStart(utcStart, "1230", BERLIN));
	}

	@Test
	public void testCheckTimeMatchesPattern() {
		assertTrue(DateUtilities.checkTimeMatchesPattern("1405", LocalTime.of(14, 5)));
		assertFalse(DateUtilities.checkTimeMatchesPattern("1405", LocalTime.of(14, 6)));
		assertTrue(DateUtilities.checkTimeMatchesPattern("*4*5", LocalTime.of(4, 15)));
		assertTrue(DateUtilities.checkTimeMatchesPattern("*4*5", LocalTime.of(14, 55)));
		assertFalse(DateUtilities.checkTimeMatchesPattern("*4*5", LocalTime.of(14, 56)));
		assertFalse(DateUtilities.checkTimeMatchesPattern("*4*5", LocalTime.of(13, 5)));
		assertTrue(DateUtilities.checkTimeMatchesPattern("****", LocalTime.MIDNIGHT));
		assertTrue(DateUtilities.checkTimeMatchesPattern("0000", LocalTime.MIDNIGHT));
		assertTrue(DateUtilities.checkTimeMatchesPattern("2359", LocalTime.of(23, 59, 59)));

		assertThrows(IllegalArgumentException.class, () -> DateUtilities.checkTimeMatchesPattern(null, LocalTime.NOON));
		assertThrows(IllegalArgumentException.class, () -> DateUtilities.checkTimeMatchesPattern("", LocalTime.NOON));
		assertThrows(IllegalArgumentException.class, () -> DateUtilities.checkTimeMatchesPattern("14:05", LocalTime.NOON));
		assertThrows(IllegalArgumentException.class, () -> DateUtilities.checkTimeMatchesPattern("140", LocalTime.NOON));
		assertThrows(IllegalArgumentException.class, () -> DateUtilities.checkTimeMatchesPattern("1a05", LocalTime.NOON));
		assertThrows(IllegalArgumentException.class, () -> DateUtilities.checkTimeMatchesPattern("?405", LocalTime.NOON));
	}

	@Test
	public void testGetDayWithoutTime() {
		final GregorianCalendar value = new GregorianCalendar(2024, Calendar.JANUARY, 10, 13, 14, 15);
		value.set(Calendar.MILLISECOND, 678);
		final GregorianCalendar day = DateUtilities.getDayWithoutTime(value);
		assertEquals(new GregorianCalendar(2024, Calendar.JANUARY, 10), day);
		assertEquals(0, day.get(Calendar.HOUR_OF_DAY));
		assertEquals(0, day.get(Calendar.MILLISECOND));
		// The original is unchanged
		assertEquals(13, value.get(Calendar.HOUR_OF_DAY));
	}

	@Test
	public void testDayListIncludes() {
		assertTrue(DateUtilities.dayListIncludes(Arrays.asList(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 10)), LocalDate.of(2024, 1, 10)));
		assertFalse(DateUtilities.dayListIncludes(Arrays.asList(LocalDate.of(2024, 1, 1)), LocalDate.of(2024, 1, 10)));
		assertFalse(DateUtilities.dayListIncludes(Collections.<LocalDate>emptyList(), LocalDate.of(2024, 1, 10)));
	}

	@Test
	public void testGetNumberOfWeekdayInMonth() {
		assertEquals(1, DateUtilities.getNumberOfWeekdayInMonth(1));
		assertEquals(1, DateUtilities.getNumberOfWeekdayInMonth(7));
		assertEquals(2, DateUtilities.getNumberOfWeekdayInMonth(8));
		assertEquals(2, DateUtilities.getNumberOfWeekdayInMonth(14));
		assertEquals(3, DateUtilities.getNumberOfWeekdayInMonth(15));
		assertEquals(4, DateUtilities.getNumberOfWeekdayInMonth(28));
		assertEquals(5, DateUtilities.getNumberOfWeekdayInMonth(29));
		assertEquals(5, DateUtilities.getNumberOfWeekdayInMonth(31));
	}

	// ------------------------------------------------------------------
	// parseUnknownDateFormat
	// ------------------------------------------------------------------

	@Test
	public void testParseUnknownDateFormatNull() throws Exception {
		assertNull(DateUtilities.parseUnknownDateFormat(null, BERLIN));
		assertNull(DateUtilities.parseUnknownDateFormat(null));
	}

	@Test
	public void testParseUnknownDateFormatIso() throws Exception {
		withDefaultTimeZone(UTC, () -> {
			// Values without offset are interpreted in the given time zone, not in the system default
			assertEquals(berlin(2024, 1, 31, 12, 0), DateUtilities.parseUnknownDateFormat("2024-01-31T12:00:00", BERLIN));
			assertEquals(berlin(2024, 1, 31, 0, 0), DateUtilities.parseUnknownDateFormat("2024-01-31", BERLIN));
			// Values with offset keep their point in time
			assertEquals(Instant.parse("2024-01-31T11:00:00Z"), DateUtilities.parseUnknownDateFormat("2024-01-31T12:00:00+01:00", NEW_YORK).toInstant());
			assertEquals(Instant.parse("2024-01-31T12:00:00Z"), DateUtilities.parseUnknownDateFormat("2024-01-31T12:00:00Z", NEW_YORK).toInstant());
		});
	}

	@Test
	public void testParseUnknownDateFormatWithDash() throws Exception {
		assertEquals(berlin(2024, 1, 31, 12, 30), DateUtilities.parseUnknownDateFormat("2024-01-31 12:30", BERLIN));
		assertThrows(Exception.class, () -> DateUtilities.parseUnknownDateFormat("2024-13-31 12:30", BERLIN));
		assertThrows(Exception.class, () -> DateUtilities.parseUnknownDateFormat("31-01-2024", BERLIN));
		assertThrows(Exception.class, () -> DateUtilities.parseUnknownDateFormat("-", BERLIN));
	}

	@Test
	public void testParseUnknownDateFormatWithDots() throws Exception {
		assertEquals(ZonedDateTime.of(2024, 1, 31, 12, 30, 45, 0, BERLIN), DateUtilities.parseUnknownDateFormat("31.01.2024 12:30:45", BERLIN));
		assertEquals(berlin(2024, 1, 31, 12, 30), DateUtilities.parseUnknownDateFormat("31.01.2024 12:30", BERLIN));
		assertEquals(berlin(2024, 1, 31, 0, 0), DateUtilities.parseUnknownDateFormat("31.01.2024", BERLIN));
		assertEquals(ZonedDateTime.of(2024, 1, 31, 0, 0, 0, 0, NEW_YORK), DateUtilities.parseUnknownDateFormat("31.01.2024", NEW_YORK));
		assertThrows(Exception.class, () -> DateUtilities.parseUnknownDateFormat("31.01.24", BERLIN));
		assertThrows(Exception.class, () -> DateUtilities.parseUnknownDateFormat("1.1.2024", BERLIN));
		assertThrows(Exception.class, () -> DateUtilities.parseUnknownDateFormat("31.01.2024 12", BERLIN));
		assertThrows(Exception.class, () -> DateUtilities.parseUnknownDateFormat("a.b", BERLIN));
	}

	@Test
	public void testParseUnknownDateFormatCompact() throws Exception {
		assertEquals(ZonedDateTime.of(2024, 1, 31, 12, 30, 45, 0, BERLIN), DateUtilities.parseUnknownDateFormat("20240131123045", BERLIN));
		assertEquals(berlin(2024, 1, 31, 0, 0), DateUtilities.parseUnknownDateFormat("31012024", BERLIN));
		// No valid "ddMMyyyy" (month 24), so parsed as "yyyyMMdd"
		assertEquals(berlin(2024, 1, 31, 0, 0), DateUtilities.parseUnknownDateFormat("20240131", BERLIN));
		// Valid as both, "ddMMyyyy" wins
		assertEquals(berlin(2012, 1, 20, 0, 0), DateUtilities.parseUnknownDateFormat("20012012", BERLIN));
		assertThrows(Exception.class, () -> DateUtilities.parseUnknownDateFormat("31022024", BERLIN));
		assertThrows(Exception.class, () -> DateUtilities.parseUnknownDateFormat("2024013", BERLIN));
		assertThrows(Exception.class, () -> DateUtilities.parseUnknownDateFormat("abc", BERLIN));
		assertThrows(Exception.class, () -> DateUtilities.parseUnknownDateFormat("", BERLIN));
	}

	@Test
	public void testParseUnknownDateFormatCompactWithOffset() throws Exception {
		// The offset of the value is kept and converted to the given time zone
		final ZonedDateTime parsed = DateUtilities.parseUnknownDateFormat("20240131T120000Z", BERLIN);
		assertEquals(berlin(2024, 1, 31, 13, 0), parsed);
		assertEquals(BERLIN, parsed.getZone());
	}

	@Test
	public void testParseUnknownDateFormatUsesDefaultTimeZone() throws Exception {
		withDefaultTimeZone(NEW_YORK, () -> {
			assertEquals(ZonedDateTime.of(2024, 1, 31, 12, 30, 0, 0, NEW_YORK), DateUtilities.parseUnknownDateFormat("31.01.2024 12:30"));
			assertEquals(ZonedDateTime.of(2024, 1, 31, 12, 30, 0, 0, NEW_YORK), DateUtilities.parseUnknownDateFormat("2024-01-31T12:30"));
		});
	}

	// ------------------------------------------------------------------
	// Conversions between the date types
	// ------------------------------------------------------------------

	@Test
	public void testGetLocalDateTimeForDate() throws Exception {
		withDefaultTimeZone(BERLIN, () -> {
			assertNull(DateUtilities.getLocalDateTimeForDate(null));
			assertEquals(LocalDateTime.of(2024, 1, 31, 12, 0), DateUtilities.getLocalDateTimeForDate(utcDate(2024, 1, 31, 11, 0)));
			assertEquals(LocalDateTime.of(2024, 7, 31, 13, 0), DateUtilities.getLocalDateTimeForDate(utcDate(2024, 7, 31, 11, 0)));
			// java.sql.Date.toInstant() is not supported, but works here
			assertEquals(LocalDateTime.of(2024, 1, 31, 0, 0), DateUtilities.getLocalDateTimeForDate(java.sql.Date.valueOf("2024-01-31")));
			final java.sql.Timestamp timestamp = java.sql.Timestamp.valueOf("2024-01-31 12:34:56.789");
			assertEquals(LocalDateTime.of(2024, 1, 31, 12, 34, 56, 789_000_000), DateUtilities.getLocalDateTimeForDate(timestamp));
			// Dates before 1970
			assertEquals(LocalDateTime.of(1970, 1, 1, 0, 59, 59, 999_000_000), DateUtilities.getLocalDateTimeForDate(new Date(-1)));
			assertEquals(LocalDateTime.of(1900, 6, 15, 12, 0), DateUtilities.getLocalDateTimeForDate(DateUtilities.getDateForLocalDateTime(LocalDateTime.of(1900, 6, 15, 12, 0))));
		});
	}

	@Test
	public void testGetLocalDateTimeForMillis() throws Exception {
		withDefaultTimeZone(BERLIN, () -> {
			assertNull(DateUtilities.getLocalDateTime(null));
			assertEquals(LocalDateTime.of(1970, 1, 1, 1, 0), DateUtilities.getLocalDateTime(0L));
			assertEquals(LocalDateTime.of(1970, 1, 1, 0, 59, 59, 999_000_000), DateUtilities.getLocalDateTime(-1L));
			assertEquals(LocalDateTime.of(2024, 1, 31, 12, 0), DateUtilities.getLocalDateTime(utcDate(2024, 1, 31, 11, 0).getTime()));
		});
	}

	@Test
	public void testGetLocalDateForDate() throws Exception {
		withDefaultTimeZone(BERLIN, () -> {
			assertEquals(LocalDate.of(2024, 1, 31), DateUtilities.getLocalDateForDate(java.sql.Date.valueOf("2024-01-31")));
			// 23:30 UTC is already the next day in Berlin
			assertEquals(LocalDate.of(2024, 2, 1), DateUtilities.getLocalDateForDate(utcDate(2024, 1, 31, 23, 30)));
			assertEquals(LocalDate.of(2024, 1, 31), DateUtilities.getLocalDateForDate(new java.sql.Timestamp(utcDate(2024, 1, 31, 12, 0).getTime())));
		});
	}

	@Test
	public void testGetLocalDateFor1970Millis() throws Exception {
		withDefaultTimeZone(BERLIN, () -> assertEquals(LocalDate.of(1970, 1, 1), DateUtilities.getLocalDateFor1970Millis(0)));
		withDefaultTimeZone(NEW_YORK, () -> assertEquals(LocalDate.of(1969, 12, 31), DateUtilities.getLocalDateFor1970Millis(0)));
	}

	@Test
	public void testGetDateForLocalDateTime() throws Exception {
		withDefaultTimeZone(BERLIN, () -> {
			final LocalDateTime value = LocalDateTime.of(2024, 1, 31, 12, 0);
			assertEquals(utcDate(2024, 1, 31, 11, 0), DateUtilities.getDateForLocalDateTime(value));
			assertEquals(value, DateUtilities.getLocalDateTimeForDate(DateUtilities.getDateForLocalDateTime(value)));
		});
	}

	@Test
	public void testGetSqlTimestampForLocalDateTime() throws Exception {
		withDefaultTimeZone(BERLIN, () -> {
			final LocalDateTime value = LocalDateTime.of(2024, 1, 31, 12, 0, 1, 123_456_789);
			final java.sql.Timestamp timestamp = DateUtilities.getSqlTimestampForLocalDateTime(value);
			// Precision is milliseconds
			assertEquals(value.withNano(123_000_000), timestamp.toLocalDateTime());
			assertEquals(utcDate(2024, 1, 31, 11, 0).getTime() + 1123, timestamp.getTime());
		});
	}

	@Test
	public void testGetDateForLocalDate() throws Exception {
		withDefaultTimeZone(BERLIN, () -> {
			assertEquals(utcDate(2024, 1, 30, 23, 0), DateUtilities.getDateForLocalDate(LocalDate.of(2024, 1, 31)));
			final java.sql.Date sqlDate = DateUtilities.getSqlDateForLocalDate(LocalDate.of(2024, 1, 31));
			assertEquals(LocalDate.of(2024, 1, 31), sqlDate.toLocalDate());
			assertEquals("2024-01-31", sqlDate.toString());
		});
	}

	@Test
	public void testGetDateForZonedDateTime() throws Exception {
		withDefaultTimeZone(BERLIN, () -> {
			final ZonedDateTime value = ZonedDateTime.of(2024, 1, 31, 12, 0, 0, 0, NEW_YORK);
			assertEquals(value.toInstant().toEpochMilli(), DateUtilities.getDateForZonedDateTime(value).getTime());
		});
	}

	@Test
	public void testGetZonedDateTimeForDate() throws Exception {
		withDefaultTimeZone(BERLIN, () -> {
			final ZonedDateTime value = DateUtilities.getZonedDateTimeForDate(utcDate(2024, 1, 31, 11, 0));
			assertEquals(berlin(2024, 1, 31, 12, 0), value);
			assertEquals(BERLIN, value.getZone());
			// Also for java.sql.Date, whose toInstant() throws an exception
			assertEquals(berlin(2024, 1, 31, 0, 0), DateUtilities.getZonedDateTimeForDate(java.sql.Date.valueOf("2024-01-31")));
		});
	}

	// ------------------------------------------------------------------
	// ISO 8601
	// ------------------------------------------------------------------

	@Test
	public void testParseIso8601Empty() {
		assertNull(DateUtilities.parseIso8601DateTimeString(null, BERLIN));
		assertNull(DateUtilities.parseIso8601DateTimeString("", BERLIN));
		assertNull(DateUtilities.parseIso8601DateTimeString("  ", BERLIN));
		assertNull(DateUtilities.parseIso8601DateTimeString(null));
	}

	@Test
	public void testParseIso8601WithoutOffset() {
		assertEquals(ZonedDateTime.of(2024, 1, 31, 12, 34, 56, 0, BERLIN), DateUtilities.parseIso8601DateTimeString("2024-01-31T12:34:56", BERLIN));
		assertEquals(ZonedDateTime.of(2024, 1, 31, 12, 34, 0, 0, BERLIN), DateUtilities.parseIso8601DateTimeString("2024-01-31T12:34", BERLIN));
		assertEquals(ZonedDateTime.of(2024, 1, 31, 0, 0, 0, 0, BERLIN), DateUtilities.parseIso8601DateTimeString("2024-01-31", BERLIN));
		assertEquals(ZonedDateTime.of(2024, 1, 31, 12, 34, 56, 0, NEW_YORK), DateUtilities.parseIso8601DateTimeString(" 2024-01-31t12:34:56 ", NEW_YORK));
	}

	@Test
	public void testParseIso8601WithOffset() {
		final Instant expected = Instant.parse("2024-01-31T11:34:56Z");
		assertEquals(expected, DateUtilities.parseIso8601DateTimeString("2024-01-31T12:34:56+01:00", NEW_YORK).toInstant());
		assertEquals(expected, DateUtilities.parseIso8601DateTimeString("2024-01-31T12:34:56+0100", NEW_YORK).toInstant());
		assertEquals(expected, DateUtilities.parseIso8601DateTimeString("2024-01-31T12:34:56+01", NEW_YORK).toInstant());
		assertEquals(expected, DateUtilities.parseIso8601DateTimeString("2024-01-31T11:34:56Z", NEW_YORK).toInstant());
		assertEquals(expected, DateUtilities.parseIso8601DateTimeString("2024-01-31t11:34:56z", NEW_YORK).toInstant());
		assertEquals(expected, DateUtilities.parseIso8601DateTimeString("2024-01-31T06:04:56-05:30", NEW_YORK).toInstant());
		assertEquals(ZoneOffset.ofHoursMinutes(-5, -30), DateUtilities.parseIso8601DateTimeString("2024-01-31T06:04:56-05:30", NEW_YORK).getOffset());
		assertEquals(ZoneOffset.UTC, DateUtilities.parseIso8601DateTimeString("2024-01-31T11:34:56Z", NEW_YORK).getOffset());
	}

	@Test
	public void testParseIso8601DateWithOffset() {
		assertEquals(Instant.parse("2024-01-31T00:00:00Z"), DateUtilities.parseIso8601DateTimeString("2024-01-31Z", BERLIN).toInstant());
		assertEquals(Instant.parse("2024-01-30T22:00:00Z"), DateUtilities.parseIso8601DateTimeString("2024-01-31+02:00", BERLIN).toInstant());
		assertEquals(Instant.parse("2024-01-31T05:00:00Z"), DateUtilities.parseIso8601DateTimeString("2024-01-31-05:00", BERLIN).toInstant());
		assertEquals(Instant.parse("2024-01-31T05:00:00Z"), DateUtilities.parseIso8601DateTimeString("2024-01-31-0500", BERLIN).toInstant());
	}

	@Test
	public void testParseIso8601Fractions() {
		assertEquals(500_000_000, DateUtilities.parseIso8601DateTimeString("2024-01-31T12:34:56.5Z", BERLIN).getNano());
		assertEquals(123_000_000, DateUtilities.parseIso8601DateTimeString("2024-01-31T12:34:56.123", BERLIN).getNano());
		assertEquals(123_456_789, DateUtilities.parseIso8601DateTimeString("2024-01-31T12:34:56.123456789+01:00", BERLIN).getNano());
	}

	@Test
	public void testParseIso8601Invalid() {
		assertThrows(DateTimeParseException.class, () -> DateUtilities.parseIso8601DateTimeString("2024-02-30", BERLIN));
		assertThrows(DateTimeParseException.class, () -> DateUtilities.parseIso8601DateTimeString("2023-02-29T12:00:00", BERLIN));
		assertThrows(DateTimeParseException.class, () -> DateUtilities.parseIso8601DateTimeString("2024-01-31T25:00:00", BERLIN));
		assertThrows(DateTimeParseException.class, () -> DateUtilities.parseIso8601DateTimeString("2024-01-31T12:00:00+1", BERLIN));
		assertThrows(DateTimeParseException.class, () -> DateUtilities.parseIso8601DateTimeString("2024-01-31T12:00:00+01:0", BERLIN));
		assertThrows(DateTimeParseException.class, () -> DateUtilities.parseIso8601DateTimeString("2024-01-31 12:00", BERLIN));
		assertThrows(DateTimeParseException.class, () -> DateUtilities.parseIso8601DateTimeString("31.01.2024", BERLIN));
		assertThrows(DateTimeParseException.class, () -> DateUtilities.parseIso8601DateTimeString("abc", BERLIN));
	}

	@Test
	public void testParseIso8601UsesDefaultTimeZone() throws Exception {
		withDefaultTimeZone(NEW_YORK, () -> assertEquals(ZonedDateTime.of(2024, 1, 31, 12, 0, 0, 0, NEW_YORK), DateUtilities.parseIso8601DateTimeString("2024-01-31T12:00:00")));
	}

	@Test
	public void testFormatZonedDateTimeWithZuluTimezone() {
		assertEquals("2024-01-31T12:00:00Z", DateUtilities.formatZonedDateTimeWithZuluTimezone(berlin(2024, 1, 31, 13, 0)));
		assertEquals("2024-07-31T11:00:00Z", DateUtilities.formatZonedDateTimeWithZuluTimezone(berlin(2024, 7, 31, 13, 0)));
		assertEquals("2024-02-01T03:00:00Z", DateUtilities.formatZonedDateTimeWithZuluTimezone(ZonedDateTime.of(2024, 1, 31, 22, 0, 0, 0, NEW_YORK)));
		// Round trip
		final ZonedDateTime value = berlin(2024, 3, 31, 3, 30);
		assertEquals(value.toInstant(), DateUtilities.parseIso8601DateTimeString(DateUtilities.formatZonedDateTimeWithZuluTimezone(value), NEW_YORK).toInstant());
	}

	// ------------------------------------------------------------------
	// Time zone changes
	// ------------------------------------------------------------------

	@Test
	public void testChangeDateTimeZoneLocalDateTime() {
		assertNull(DateUtilities.changeDateTimeZone((LocalDateTime) null, UTC, BERLIN));
		assertEquals(LocalDateTime.of(2024, 1, 31, 13, 0), DateUtilities.changeDateTimeZone(LocalDateTime.of(2024, 1, 31, 12, 0), UTC, BERLIN));
		assertEquals(LocalDateTime.of(2024, 7, 31, 14, 0), DateUtilities.changeDateTimeZone(LocalDateTime.of(2024, 7, 31, 12, 0), UTC, BERLIN));
		assertEquals(LocalDateTime.of(2024, 1, 31, 18, 0), DateUtilities.changeDateTimeZone(LocalDateTime.of(2024, 1, 31, 12, 0), NEW_YORK, BERLIN));
		final LocalDateTime value = LocalDateTime.of(2024, 1, 31, 12, 0);
		assertSame(value, DateUtilities.changeDateTimeZone(value, BERLIN, BERLIN));
	}

	@Test
	public void testChangeDateTimeZoneLocalDateTimeWithDefaults() throws Exception {
		withDefaultTimeZone(BERLIN, () -> {
			assertEquals(LocalDateTime.of(2024, 1, 31, 13, 0), DateUtilities.changeDateTimeZone(LocalDateTime.of(2024, 1, 31, 12, 0), UTC, null));
			assertEquals(LocalDateTime.of(2024, 1, 31, 11, 0), DateUtilities.changeDateTimeZone(LocalDateTime.of(2024, 1, 31, 12, 0), null, UTC));
		});
	}

	@Test
	public void testChangeDateTimeZoneZonedDateTime() throws Exception {
		assertNull(DateUtilities.changeDateTimeZone((ZonedDateTime) null, BERLIN));
		final ZonedDateTime value = ZonedDateTime.of(2024, 1, 31, 12, 0, 0, 0, UTC);
		final ZonedDateTime changed = DateUtilities.changeDateTimeZone(value, BERLIN);
		assertEquals(berlin(2024, 1, 31, 13, 0), changed);
		assertEquals(value.toInstant(), changed.toInstant());
		assertSame(value, DateUtilities.changeDateTimeZone(value, UTC));
		withDefaultTimeZone(NEW_YORK, () -> assertEquals(NEW_YORK, DateUtilities.changeDateTimeZone(value, null).getZone()));
	}

	@Test
	public void testChangeDateTimeZoneDateWithZoneId() throws Exception {
		withDefaultTimeZone(UTC, () -> {
			assertNull(DateUtilities.changeDateTimeZone((Date) null, NEW_YORK, BERLIN));
			// The wall clock time 12:00 meant in New York is 18:00 in Berlin
			assertEquals(utcDate(2024, 1, 31, 18, 0), DateUtilities.changeDateTimeZone(utcDate(2024, 1, 31, 12, 0), NEW_YORK, BERLIN));
			assertEquals(utcDate(2024, 1, 31, 13, 0), DateUtilities.changeDateTimeZone(utcDate(2024, 1, 31, 12, 0), null, BERLIN));
			assertEquals(utcDate(2024, 1, 31, 11, 0), DateUtilities.changeDateTimeZone(utcDate(2024, 1, 31, 12, 0), BERLIN, null));
			final Date value = utcDate(2024, 1, 31, 12, 0);
			assertSame(value, DateUtilities.changeDateTimeZone(value, BERLIN, BERLIN));
		});
	}

	@Test
	public void testChangeDateTimeZoneDateWithTimeZone() throws Exception {
		withDefaultTimeZone(UTC, () -> {
			final TimeZone newYork = TimeZone.getTimeZone(NEW_YORK);
			final TimeZone berlin = TimeZone.getTimeZone(BERLIN);
			assertNull(DateUtilities.changeDateTimeZone((Date) null, newYork, berlin));
			assertEquals(utcDate(2024, 1, 31, 18, 0), DateUtilities.changeDateTimeZone(utcDate(2024, 1, 31, 12, 0), newYork, berlin));
			// Daylight saving time in both zones
			assertEquals(utcDate(2024, 7, 31, 18, 0), DateUtilities.changeDateTimeZone(utcDate(2024, 7, 31, 12, 0), newYork, berlin));
			assertEquals(utcDate(2024, 1, 31, 13, 0), DateUtilities.changeDateTimeZone(utcDate(2024, 1, 31, 12, 0), null, berlin));
			assertEquals(utcDate(2024, 7, 31, 10, 0), DateUtilities.changeDateTimeZone(utcDate(2024, 7, 31, 12, 0), berlin, null));
			final Date value = utcDate(2024, 1, 31, 12, 0);
			assertSame(value, DateUtilities.changeDateTimeZone(value, berlin, TimeZone.getTimeZone(BERLIN)));
		});
	}

	// ------------------------------------------------------------------
	// Formatters
	// ------------------------------------------------------------------

	@Test
	public void testGetDateFormatPattern() {
		assertEquals("dd.MM.yyyy", DateUtilities.getDateFormatPattern(Locale.GERMANY));
		assertEquals("M/d/yyyy", DateUtilities.getDateFormatPattern(Locale.US));
		assertEquals("dd/MM/yyyy", DateUtilities.getDateFormatPattern(Locale.UK));
		assertEquals("dd.MM.yyyy HH:mm", DateUtilities.getDateTimeFormatPattern(Locale.GERMANY));
		assertEquals("dd.MM.yyyy HH:mm:ss", DateUtilities.getDateTimeFormatWithSecondsPattern(Locale.GERMANY));
		assertEquals("M/d/yyyy HH:mm", DateUtilities.getDateTimeFormatPattern(Locale.US));
	}

	@Test
	public void testGetDateFormatter() {
		final DateTimeFormatter formatter = DateUtilities.getDateFormatter(Locale.GERMANY);
		assertEquals("05.01.2024", formatter.format(LocalDate.of(2024, 1, 5)));
		assertEquals(LocalDate.of(2024, 2, 29), LocalDate.parse("29.02.2024", formatter));
		// Strict resolving
		assertThrows(DateTimeParseException.class, () -> LocalDate.parse("29.02.2023", formatter));
		assertThrows(DateTimeParseException.class, () -> LocalDate.parse("31.04.2024", formatter));
		assertEquals("1/5/2024", DateUtilities.getDateFormatter(Locale.US).format(LocalDate.of(2024, 1, 5)));

		final DateTimeFormatter zonedFormatter = DateUtilities.getDateFormatter(Locale.GERMANY, NEW_YORK);
		assertEquals(NEW_YORK, zonedFormatter.getZone());
		// 03:00 UTC is still the previous day in New York
		assertEquals("31.01.2024", zonedFormatter.format(Instant.parse("2024-02-01T03:00:00Z")));
	}

	@Test
	public void testGetDateTimeFormatter() {
		final LocalDateTime value = LocalDateTime.of(2024, 1, 5, 7, 8, 9);
		assertEquals("05.01.2024 07:08", DateUtilities.getDateTimeFormatter(Locale.GERMANY).format(value));
		assertEquals("05.01.2024 07:08:09", DateUtilities.getDateTimeFormatterWithSeconds(Locale.GERMANY).format(value));
		assertEquals(LocalDateTime.of(2024, 1, 5, 7, 8), LocalDateTime.parse("05.01.2024 07:08", DateUtilities.getDateTimeFormatter(Locale.GERMANY)));
		assertThrows(DateTimeParseException.class, () -> LocalDateTime.parse("31.04.2024 07:08", DateUtilities.getDateTimeFormatter(Locale.GERMANY)));
		assertThrows(DateTimeParseException.class, () -> LocalDateTime.parse("30.04.2024 24:00:00", DateUtilities.getDateTimeFormatterWithSeconds(Locale.GERMANY)));

		final Instant instant = Instant.parse("2024-01-31T12:00:00Z");
		assertEquals("31.01.2024 13:00", DateUtilities.getDateTimeFormatter(Locale.GERMANY, BERLIN).format(instant));
		assertEquals("31.01.2024 07:00:00", DateUtilities.getDateTimeFormatterWithSeconds(Locale.GERMANY, NEW_YORK).format(instant));
		assertEquals(BERLIN, DateUtilities.getDateTimeFormatter(Locale.GERMANY, BERLIN).getZone());
	}

	// ------------------------------------------------------------------
	// formatDate
	// ------------------------------------------------------------------

	@Test
	public void testFormatDateNullValues() {
		assertNull(DateUtilities.formatDate("yyyy", (ZonedDateTime) null));
		assertNull(DateUtilities.formatDate("yyyy", (Date) null));
		assertNull(DateUtilities.formatDate("yyyy", (LocalDateTime) null));
		assertNull(DateUtilities.formatDate("yyyy", (LocalDate) null));
		assertNull(DateUtilities.formatDate("HH", (LocalTime) null));
		assertNull(DateUtilities.formatDate("yyyy", (Date) null, Locale.GERMANY, BERLIN));
		assertNull(DateUtilities.formatDate("yyyy", (LocalDateTime) null, Locale.GERMANY, BERLIN));
		assertNull(DateUtilities.formatDate("yyyy", (LocalDate) null, Locale.GERMANY, BERLIN));
		assertNull(DateUtilities.formatDate("yyyy", (Date) null, BERLIN));
		assertNull(DateUtilities.formatDate("yyyy", (LocalDateTime) null, BERLIN));
		assertNull(DateUtilities.formatDate("yyyy", (ZonedDateTime) null, BERLIN));
	}

	@Test
	public void testFormatDateTypes() {
		assertEquals("31.01.2024 13:00 +01:00", DateUtilities.formatDate("dd.MM.yyyy HH:mm xxx", berlin(2024, 1, 31, 13, 0)));
		assertEquals("31.01.2024 13:00", DateUtilities.formatDate("dd.MM.yyyy HH:mm", LocalDateTime.of(2024, 1, 31, 13, 0)));
		assertEquals("31.01.2024", DateUtilities.formatDate("dd.MM.yyyy", LocalDate.of(2024, 1, 31)));
		assertEquals("13:05:07", DateUtilities.formatDate("HH:mm:ss", LocalTime.of(13, 5, 7)));
	}

	@Test
	public void testFormatDateForDateInDefaultTimeZone() throws Exception {
		withDefaultTimeZone(BERLIN, () -> {
			assertEquals("31.01.2024 13:00", DateUtilities.formatDate("dd.MM.yyyy HH:mm", utcDate(2024, 1, 31, 12, 0)));
			// Within the daylight saving time overlap the same wall clock time occurs twice with different offsets
			assertEquals("27.10.2024 02:30 +02:00", DateUtilities.formatDate("dd.MM.yyyy HH:mm xxx", utcDate(2024, 10, 27, 0, 30)));
			assertEquals("27.10.2024 02:30 +01:00", DateUtilities.formatDate("dd.MM.yyyy HH:mm xxx", utcDate(2024, 10, 27, 1, 30)));
			assertEquals("31.01.2024", DateUtilities.formatDate("dd.MM.yyyy", java.sql.Date.valueOf("2024-01-31")));
		});
	}

	@Test
	public void testFormatDateWithTimeZone() {
		final Date date = utcDate(2024, 1, 31, 12, 0);
		assertEquals("31.01.2024 13:00", DateUtilities.formatDate("dd.MM.yyyy HH:mm", date, BERLIN));
		assertEquals("31.01.2024 07:00", DateUtilities.formatDate("dd.MM.yyyy HH:mm", date, NEW_YORK));
		assertEquals("27.10.2024 02:30 +01:00", DateUtilities.formatDate("dd.MM.yyyy HH:mm xxx", utcDate(2024, 10, 27, 1, 30), BERLIN));
		// A local date time is not converted, the zone is only used for zone fields
		assertEquals("31.01.2024 12:00 Europe/Berlin", DateUtilities.formatDate("dd.MM.yyyy HH:mm VV", LocalDateTime.of(2024, 1, 31, 12, 0), BERLIN));
		// A zoned date time is converted
		assertEquals("31.01.2024 13:00 Europe/Berlin", DateUtilities.formatDate("dd.MM.yyyy HH:mm VV", ZonedDateTime.of(2024, 1, 31, 12, 0, 0, 0, UTC), BERLIN));
	}

	@Test
	public void testFormatDateWithLocale() {
		final Date date = utcDate(2024, 3, 1, 11, 0);
		assertEquals("1. M\u00E4rz 2024 12:00", DateUtilities.formatDate("d. MMMM yyyy HH:mm", date, Locale.GERMAN, BERLIN));
		assertEquals("1. March 2024 06:00", DateUtilities.formatDate("d. MMMM yyyy HH:mm", date, Locale.ENGLISH, NEW_YORK));
		assertEquals("Mittwoch, 10. Januar 2024", DateUtilities.formatDate("EEEE, d. MMMM yyyy", LocalDate.of(2024, 1, 10), Locale.GERMAN, BERLIN));
		assertEquals("Wednesday 12:00 America/New_York", DateUtilities.formatDate("EEEE HH:mm VV", LocalDateTime.of(2024, 1, 10, 12, 0), Locale.ENGLISH, NEW_YORK));
	}

	// ------------------------------------------------------------------
	// Parsing
	// ------------------------------------------------------------------

	@Test
	public void testParseLocalDateIsSmart() {
		assertEquals(LocalDate.of(2024, 1, 31), DateUtilities.parseLocalDate("dd.MM.yyyy", "31.01.2024"));
		// Smart resolving moves the day to the end of the month
		assertEquals(LocalDate.of(2024, 4, 30), DateUtilities.parseLocalDate("dd.MM.yyyy", "31.04.2024"));
		assertThrows(DateTimeParseException.class, () -> DateUtilities.parseLocalDate("dd.MM.yyyy", "32.01.2024"));
		assertThrows(DateTimeParseException.class, () -> DateUtilities.parseLocalDate("dd.MM.yyyy", "2024-01-31"));
	}

	@Test
	public void testParseStrictLocalDate() {
		assertEquals(LocalDate.of(2024, 2, 29), DateUtilities.parseStrictLocalDate("dd.MM.yyyy", "29.02.2024"));
		assertEquals(LocalDate.of(2024, 2, 29), DateUtilities.parseStrictLocalDate("uuuuMMdd", "20240229"));
		assertThrows(DateTimeParseException.class, () -> DateUtilities.parseStrictLocalDate("dd.MM.yyyy", "29.02.2023"));
		assertThrows(DateTimeParseException.class, () -> DateUtilities.parseStrictLocalDate("dd.MM.yyyy", "31.04.2024"));
		assertThrows(DateTimeParseException.class, () -> DateUtilities.parseStrictLocalDate("ddMMyyyy", "00012024"));
	}

	@Test
	public void testParseLocalDateTime() {
		assertEquals(LocalDateTime.of(2024, 1, 5, 7, 8), DateUtilities.parseLocalDateTime("dd.MM.yyyy HH:mm", "05.01.2024 07:08"));
		assertEquals(LocalDateTime.of(2024, 1, 5, 7, 8, 9), DateUtilities.parseLocalDateTime(DateUtilities.YYYYMMDD_HHMMSS, "20240105-070809"));
		assertThrows(DateTimeParseException.class, () -> DateUtilities.parseLocalDateTime("dd.MM.yyyy HH:mm", "05.01.2024"));
	}

	@Test
	public void testParseLocalTime() {
		assertEquals(LocalTime.of(7, 8), DateUtilities.parseLocalTime("HH:mm", "07:08"));
		assertEquals(LocalTime.of(23, 59, 59), DateUtilities.parseLocalTime("HH:mm:ss", "23:59:59"));
		assertThrows(DateTimeParseException.class, () -> DateUtilities.parseLocalTime("HH:mm", "25:00"));
		assertThrows(DateTimeParseException.class, () -> DateUtilities.parseLocalTime("HH:mm", "24:00"));
		assertThrows(DateTimeParseException.class, () -> DateUtilities.parseLocalTime("HH:mm", "7:08"));
	}

	@Test
	public void testParseDateTimeInDefaultTimeZone() throws Exception {
		withDefaultTimeZone(BERLIN, () -> assertEquals(utcDate(2024, 1, 31, 11, 0), DateUtilities.parseDateTime("dd.MM.yyyy HH:mm", "31.01.2024 12:00")));
		withDefaultTimeZone(NEW_YORK, () -> assertEquals(utcDate(2024, 1, 31, 17, 0), DateUtilities.parseDateTime("dd.MM.yyyy HH:mm", "31.01.2024 12:00")));
	}

	@Test
	public void testParseDateTimeInGivenTimeZone() throws Exception {
		withDefaultTimeZone(BERLIN, () -> {
			assertEquals(utcDate(2024, 1, 31, 17, 0), DateUtilities.parseDateTime("dd.MM.yyyy HH:mm", "31.01.2024 12:00", NEW_YORK));
			assertEquals(utcDate(2024, 1, 31, 17, 0), DateUtilities.parseDateTime("dd.MM.yyyy HH:mm", "31.01.2024 12:00", TimeZone.getTimeZone(NEW_YORK)));
			// An offset in the text takes precedence
			assertEquals(utcDate(2024, 1, 31, 9, 0), DateUtilities.parseDateTime("dd.MM.yyyy HH:mm XXX", "31.01.2024 12:00 +03:00", NEW_YORK));
			assertThrows(DateTimeParseException.class, () -> DateUtilities.parseDateTime("dd.MM.yyyy HH:mm", "31.01.2024", NEW_YORK));
		});
	}

	@Test
	public void testParseZonedDateTime() {
		final ZonedDateTime parsed = DateUtilities.parseZonedDateTime("dd.MM.yyyy HH:mm", "31.01.2024 12:00", NEW_YORK);
		assertEquals(ZonedDateTime.of(2024, 1, 31, 12, 0, 0, 0, NEW_YORK), parsed);
		// An offset in the text takes precedence
		assertEquals(Instant.parse("2024-01-31T09:00:00Z"), DateUtilities.parseZonedDateTime("dd.MM.yyyy HH:mm XXX", "31.01.2024 12:00 +03:00", NEW_YORK).toInstant());
		// A zone in the text is used
		assertEquals(berlin(2024, 1, 31, 12, 0), DateUtilities.parseZonedDateTime("dd.MM.yyyy HH:mm VV", "31.01.2024 12:00 Europe/Berlin", NEW_YORK));
	}

	@Test
	public void testParseThreeLetterMonth() throws Exception {
		final String[] english = { "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec" };
		final String[] german = { "Jan", "Feb", "M\u00E4r", "Apr", "Mai", "Jun", "Jul", "Aug", "Sep", "Okt", "Nov", "Dez" };
		for (int i = 0; i < 12; i++) {
			assertEquals(i + 1, DateUtilities.parseThreeLetterMonth(english[i]));
			assertEquals(i + 1, DateUtilities.parseThreeLetterMonth(german[i]));
			assertEquals(i + 1, DateUtilities.parseThreeLetterMonth(english[i].toUpperCase(Locale.ROOT)));
			assertEquals(i + 1, DateUtilities.parseThreeLetterMonth(german[i].toLowerCase(Locale.ROOT)));
		}
		assertEquals(3, DateUtilities.parseThreeLetterMonth("M\u00C4R"));
		assertThrows(Exception.class, () -> DateUtilities.parseThreeLetterMonth("xyz"));
		assertThrows(Exception.class, () -> DateUtilities.parseThreeLetterMonth(""));
		assertThrows(Exception.class, () -> DateUtilities.parseThreeLetterMonth("January"));
		assertThrows(Exception.class, () -> DateUtilities.parseThreeLetterMonth("Mrz"));
	}
}
