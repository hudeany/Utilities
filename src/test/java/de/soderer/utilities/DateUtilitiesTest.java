package de.soderer.utilities;

import static org.junit.Assert.assertEquals;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import javax.mail.internet.InternetAddress;

import org.junit.Assert;
import org.junit.Test;

public class DateUtilitiesTest {

	@Test
	public void test1() {
		try {
			final List<InternetAddress> participants = new ArrayList<>();
			participants.add(new InternetAddress("test1@example.com", "First Test"));
			participants.add(new InternetAddress("test2@example.com"));

			final String result = DateUtilities.createICalEventInvitation(
					"Test date",
					"Lots of description text and a <a href=\"https://example.com\">link</a>.",
					ZonedDateTime.now(),
					ZonedDateTime.now().plusHours(5),
					"here",
					new InternetAddress("test@example.com"),
					participants);
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void rezoneDateTest() {
		try {
			final Date originalDate = DateUtilities.parseDateTime("dd.MM.yyyy HH:mm:ss", "01.03.2014 22:11:10");
			final Date rezonedDate = DateUtilities.changeDateTimeZone(originalDate, ZoneId.of("Europe/Berlin"), ZoneId.of("Z"));
			final String resultString = DateUtilities.formatDate("dd.MM.yyyy HH:mm:ss", rezonedDate);
			Assert.assertEquals("01.03.2014 21:11:10", resultString);
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}

		try {
			final Date originalDate = DateUtilities.parseDateTime("dd.MM.yyyy HH:mm:ss", "04.12.2022 21:11:10");
			final Date rezonedDate = DateUtilities.changeDateTimeZone(originalDate, ZoneId.of("Europe/Berlin"), ZoneId.of("Europe/Sofia"));
			final String resultString = DateUtilities.formatDate("dd.MM.yyyy HH:mm:ss", rezonedDate);
			Assert.assertEquals("04.12.2022 22:11:10", resultString);
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}

		try {
			final Date originalDate = DateUtilities.parseDateTime("dd.MM.yyyy HH:mm:ss", "04.12.2022 21:11:10");
			final Date rezonedDate = DateUtilities.changeDateTimeZone(originalDate, ZoneId.of("Europe/Berlin"), ZoneId.of("Europe/Dublin"));
			final String resultString = DateUtilities.formatDate("dd.MM.yyyy HH:mm:ss", rezonedDate);
			Assert.assertEquals("04.12.2022 20:11:10", resultString);
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}

		try {
			final Date originalDate = DateUtilities.parseDateTime("dd.MM.yyyy HH:mm:ss", "04.12.2022 22:11:10");
			final Date rezonedDate = DateUtilities.changeDateTimeZone(originalDate, TimeZone.getTimeZone("Europe/Sofia"), TimeZone.getTimeZone("Europe/Dublin"));
			final String resultString = DateUtilities.formatDate("dd.MM.yyyy HH:mm:ss", rezonedDate);
			Assert.assertEquals("04.12.2022 20:11:10", resultString);
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}

		try {
			final Date originalDate = DateUtilities.parseDateTime("dd.MM.yyyy HH:mm:ss", "04.12.2022 22:11:10");
			final Date rezonedDate = DateUtilities.changeDateTimeZone(originalDate, ZoneId.of("Europe/Sofia"), ZoneId.of("Europe/Dublin"));
			final String resultString = DateUtilities.formatDate("dd.MM.yyyy HH:mm:ss", rezonedDate);
			Assert.assertEquals("04.12.2022 20:11:10", resultString);
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testParseISO_DateWithTime_WithTimezoneChar() {
		ZonedDateTime resultZonedDateTime = null;
		try {
			resultZonedDateTime = DateUtilities.parseIso8601DateTimeString("2014-03-01T22:11:10Z");
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
		Assert.assertNotNull(resultZonedDateTime);
		Assert.assertEquals("Z", resultZonedDateTime.getZone().getId());
		Assert.assertEquals(22, resultZonedDateTime.getHour());
	}

	@Test
	public void testParseISO_DateWithTime_WithTimezone() {
		ZonedDateTime resultZonedDateTime = null;
		try {
			resultZonedDateTime = DateUtilities.parseIso8601DateTimeString("2014-03-01T22:11:10+00:00");
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
		Assert.assertNotNull(resultZonedDateTime);
		Assert.assertEquals("Z", resultZonedDateTime.getZone().getId());
		Assert.assertEquals(22, resultZonedDateTime.getHour());
	}

	@Test
	public void testParseISO_DateWithTime_WithShortTimezone() {
		ZonedDateTime resultZonedDateTime = null;
		try {
			resultZonedDateTime = DateUtilities.parseIso8601DateTimeString("2014-03-01T22:11:10+00");
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
		Assert.assertNotNull(resultZonedDateTime);
		Assert.assertEquals("Z", resultZonedDateTime.getZone().getId());
		Assert.assertEquals(22, resultZonedDateTime.getHour());
	}

	@Test
	public void testParseISO_DateWithTime_WithOutTimezone() {
		ZonedDateTime resultZonedDateTime = null;
		try {
			resultZonedDateTime = DateUtilities.parseIso8601DateTimeString("2014-03-01T22:11:10");
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
		Assert.assertNotNull(resultZonedDateTime);
		Assert.assertEquals("Europe/Berlin", resultZonedDateTime.getZone().getId());
		Assert.assertEquals(22, resultZonedDateTime.getHour());
	}

	@Test
	public void testParseISO_DateWithoutTime_WithoutTimezone() {
		ZonedDateTime resultZonedDateTime = null;
		try {
			resultZonedDateTime = DateUtilities.parseIso8601DateTimeString("2014-03-01");
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
		Assert.assertNotNull(resultZonedDateTime);
		Assert.assertEquals("Europe/Berlin", resultZonedDateTime.getZone().getId());
		Assert.assertEquals(0, resultZonedDateTime.getHour());
	}

	@Test
	public void testParseISO_DateWithoutTime_WithTimezone() {
		ZonedDateTime resultZonedDateTime = null;
		try {
			resultZonedDateTime = DateUtilities.parseIso8601DateTimeString("2014-03-01+00:00");
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
		Assert.assertNotNull(resultZonedDateTime);
		Assert.assertEquals("Z", resultZonedDateTime.getZone().getId());
		Assert.assertEquals(0, resultZonedDateTime.getHour());
	}

	@Test
	public void testParseISO_DateWithMillisTime_WithTimezone() {
		ZonedDateTime resultZonedDateTime = null;
		try {
			resultZonedDateTime = DateUtilities.parseIso8601DateTimeString("2014-03-01T22:11:10.123+00:00");
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
		Assert.assertNotNull(resultZonedDateTime);
		Assert.assertEquals("Z", resultZonedDateTime.getZone().getId());
		Assert.assertEquals(22, resultZonedDateTime.getHour());
	}

	@Test
	public void testParseISO_DateWithMillisTime_WithOutTimezone() {
		ZonedDateTime resultZonedDateTime = null;
		try {
			resultZonedDateTime = DateUtilities.parseIso8601DateTimeString("2014-03-01T22:11:10.123");
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
		Assert.assertNotNull(resultZonedDateTime);
		Assert.assertEquals("Europe/Berlin", resultZonedDateTime.getZone().getId());
		Assert.assertEquals(22, resultZonedDateTime.getHour());
	}

	@Test
	public void testParseISO_DateWithFractalTime_WithTimezone() {
		ZonedDateTime resultZonedDateTime = null;
		try {
			resultZonedDateTime = DateUtilities.parseIso8601DateTimeString("2014-03-01T22:11:10.123456+00:00");
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
		Assert.assertNotNull(resultZonedDateTime);
		Assert.assertEquals("Z", resultZonedDateTime.getZone().getId());
		Assert.assertEquals(22, resultZonedDateTime.getHour());
	}

	@Test
	public void testParseISO_DateWithFractalTime_WithOutTimezone() {
		ZonedDateTime resultZonedDateTime = null;
		try {
			resultZonedDateTime = DateUtilities.parseIso8601DateTimeString("2014-03-01T22:11:10.123456");
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
		Assert.assertNotNull(resultZonedDateTime);
		Assert.assertEquals("Europe/Berlin", resultZonedDateTime.getZone().getId());
		Assert.assertEquals(22, resultZonedDateTime.getHour());
	}

	@Test
	public void testHolidayRule1() throws Exception {
		final ZonedDateTime calculatedNextStartDateTime = DateUtilities.calculateNextJobStart("0000;!" + DateUtilities.formatDate(DateUtilities.DDMMYYYY, LocalDateTime.now()));
		final ZonedDateTime expectedNextStartDateTime = LocalDate.now().plusDays(1).atTime(LocalTime.of(0, 0)).atZone(ZoneId.systemDefault());
		assertEquals(expectedNextStartDateTime, calculatedNextStartDateTime);
	}

	@Test
	public void testHolidayRule2() throws Exception {
		final ZonedDateTime calculatedNextStartDateTime = DateUtilities.calculateNextJobStart("**00;!" + DateUtilities.formatDate(DateUtilities.DDMMYYYY, LocalDateTime.now()));
		final ZonedDateTime expectedNextStartDateTime = LocalDate.now().plusDays(1).atTime(LocalTime.of(0, 0)).atZone(ZoneId.systemDefault());
		assertEquals(expectedNextStartDateTime, calculatedNextStartDateTime);
	}

	@Test
	public void testHolidayRule3() throws Exception {
		final ZonedDateTime calculatedNextStartDateTime = DateUtilities.calculateNextJobStart("*100;!" + DateUtilities.formatDate(DateUtilities.DDMMYYYY, LocalDateTime.now()));
		final ZonedDateTime expectedNextStartDateTime = LocalDate.now().plusDays(1).atTime(LocalTime.of(1, 0)).atZone(ZoneId.systemDefault());
		assertEquals(expectedNextStartDateTime, calculatedNextStartDateTime);
	}

	@Test
	public void testHolidayRule4() throws Exception {
		final ZonedDateTime calculatedNextStartDateTime = DateUtilities.calculateNextJobStart("1742;!" + DateUtilities.formatDate(DateUtilities.DDMMYYYY, LocalDateTime.now()));
		final ZonedDateTime expectedNextStartDateTime = LocalDate.now().plusDays(1).atTime(LocalTime.of(17, 42)).atZone(ZoneId.systemDefault());
		assertEquals(expectedNextStartDateTime, calculatedNextStartDateTime);
	}

	@Test
	public void testUltimoRule() throws Exception {
		final ZonedDateTime calculatedNextStartDateTime = DateUtilities.calculateNextJobStart("M99:0000");
		ZonedDateTime expectedNextStartDateTime = ZonedDateTime.now();
		if (expectedNextStartDateTime.getDayOfMonth() == expectedNextStartDateTime.with(TemporalAdjusters.lastDayOfMonth()).getDayOfMonth()) {
			// If today is ultimo take the next month's ultimo
			expectedNextStartDateTime = expectedNextStartDateTime.with(TemporalAdjusters.firstDayOfMonth());
			expectedNextStartDateTime = expectedNextStartDateTime.plusMonths(1);
		}
		expectedNextStartDateTime = expectedNextStartDateTime.with(TemporalAdjusters.lastDayOfMonth()).with(LocalTime.of(0, 0));
		assertEquals(expectedNextStartDateTime, calculatedNextStartDateTime);
	}

	@Test
	public void testUltimoRuleOnUltimoDay() throws Exception {
		final ZonedDateTime ultimoDay = ZonedDateTime.now().with(TemporalAdjusters.lastDayOfMonth());
		final ZonedDateTime calculatedNextStartDateTime = DateUtilities.calculateNextJobStart(ultimoDay, "M99:0000", null);
		ZonedDateTime expectedNextStartDateTime = ZonedDateTime.now();
		expectedNextStartDateTime = expectedNextStartDateTime.with(TemporalAdjusters.firstDayOfMonth());
		expectedNextStartDateTime = expectedNextStartDateTime.plusMonths(1);
		expectedNextStartDateTime = expectedNextStartDateTime.with(TemporalAdjusters.lastDayOfMonth()).with(LocalTime.of(0, 0));
		assertEquals(expectedNextStartDateTime, calculatedNextStartDateTime);
	}

	@Test
	public void testEvenWeeksRule1() throws Exception {
		final ZonedDateTime calulationStartDateTime = ZonedDateTime.of(2016, 1, 23, 0, 0, 0, 0, ZoneId.systemDefault());
		final ZonedDateTime calculatedNextStartDateTime = DateUtilities.calculateNextJobStart(calulationStartDateTime, "MiEv:0000", null);
		final ZonedDateTime expectedNextStartDateTime = ZonedDateTime.of(2016, 1, 27, 0, 0, 0, 0, ZoneId.systemDefault());
		assertEquals(expectedNextStartDateTime, calculatedNextStartDateTime);
	}

	@Test
	public void testEvenWeeksRule2() throws Exception {
		final ZonedDateTime calulationStartDateTime = ZonedDateTime.of(2013, 1, 23, 0, 0, 0, 0, ZoneId.systemDefault());
		final ZonedDateTime calculatedNextStartDateTime = DateUtilities.calculateNextJobStart(calulationStartDateTime, "MiEv:0000", null);
		final ZonedDateTime expectedNextStartDateTime = ZonedDateTime.of(2013, 2, 6, 0, 0, 0, 0, ZoneId.systemDefault());
		assertEquals(expectedNextStartDateTime, calculatedNextStartDateTime);
	}

	@Test
	public void testEvenWeeksRule3() throws Exception {
		final ZonedDateTime calulationStartDateTime = ZonedDateTime.of(2013, 1, 30, 0, 0, 0, 0, ZoneId.systemDefault());
		final ZonedDateTime calculatedNextStartDateTime = DateUtilities.calculateNextJobStart(calulationStartDateTime, "MiEv:0000", null);
		final ZonedDateTime expectedNextStartDateTime = ZonedDateTime.of(2013, 2, 6, 0, 0, 0, 0, ZoneId.systemDefault());
		assertEquals(expectedNextStartDateTime, calculatedNextStartDateTime);
	}

	@Test
	public void testEvenWeeksRule4() throws Exception {
		final ZonedDateTime calulationStartDateTime = ZonedDateTime.of(2013, 1, 30, 0, 1, 0, 0, ZoneId.systemDefault());
		final ZonedDateTime calculatedNextStartDateTime = DateUtilities.calculateNextJobStart(calulationStartDateTime, "MiEv:0000", null);
		final ZonedDateTime expectedNextStartDateTime = ZonedDateTime.of(2013, 2, 6, 0, 0, 0, 0, ZoneId.systemDefault());
		assertEquals(expectedNextStartDateTime, calculatedNextStartDateTime);
	}

	@Test
	public void testOddWeeksRule1() throws Exception {
		final ZonedDateTime calulationStartDateTime = ZonedDateTime.of(2013, 1, 23, 0, 0, 0, 0, ZoneId.systemDefault());
		final ZonedDateTime calculatedNextStartDateTime = DateUtilities.calculateNextJobStart(calulationStartDateTime, "MiOd:0000", null);
		final ZonedDateTime expectedNextStartDateTime = ZonedDateTime.of(2013, 1, 30, 0, 0, 0, 0, ZoneId.systemDefault());
		assertEquals(expectedNextStartDateTime, calculatedNextStartDateTime);
	}

	@Test
	public void testOddWeeksRule2() throws Exception {
		final ZonedDateTime calulationStartDateTime = ZonedDateTime.of(2013, 1, 23, 0, 1, 0, 0, ZoneId.systemDefault());
		final ZonedDateTime calculatedNextStartDateTime = DateUtilities.calculateNextJobStart(calulationStartDateTime, "MiOd:0000", null);
		final ZonedDateTime expectedNextStartDateTime = ZonedDateTime.of(2013, 1, 30, 0, 0, 0, 0, ZoneId.systemDefault());
		assertEquals(expectedNextStartDateTime, calculatedNextStartDateTime);
	}

	@Test
	public void testOddWeeksRule3() throws Exception {
		final ZonedDateTime calulationStartDateTime = ZonedDateTime.of(2016, 1, 30, 0, 0, 0, 0, ZoneId.systemDefault());
		final ZonedDateTime calculatedNextStartDateTime = DateUtilities.calculateNextJobStart(calulationStartDateTime, "MiOd:0000", null);
		final ZonedDateTime expectedNextStartDateTime = ZonedDateTime.of(2016, 2, 3, 0, 0, 0, 0, ZoneId.systemDefault());
		assertEquals(expectedNextStartDateTime, calculatedNextStartDateTime);
	}

	@Test
	public void testXMonthRule1() throws Exception {
		final ZonedDateTime calulationStartDateTime = ZonedDateTime.of(2013, 1, 30, 0, 0, 0, 0, ZoneId.systemDefault());
		final ZonedDateTime calculatedNextStartDateTime = DateUtilities.calculateNextJobStart(calulationStartDateTime, "06M14:0000", null);
		final ZonedDateTime expectedNextStartDateTime = ZonedDateTime.of(2013, 7, 14, 0, 0, 0, 0, ZoneId.systemDefault());
		assertEquals(expectedNextStartDateTime, calculatedNextStartDateTime);
	}

	@Test
	public void testXMonthRule2() throws Exception {
		final ZonedDateTime calulationStartDateTime = ZonedDateTime.of(2013, 7, 14, 0, 0, 0, 0, ZoneId.systemDefault());
		final ZonedDateTime calculatedNextStartDateTime = DateUtilities.calculateNextJobStart(calulationStartDateTime, "06M14:0000", null);
		final ZonedDateTime expectedNextStartDateTime = ZonedDateTime.of(2014, 1, 14, 0, 0, 0, 0, ZoneId.systemDefault());
		assertEquals(expectedNextStartDateTime, calculatedNextStartDateTime);
	}

	@Test
	public void testXMonthRuleUltimo1() throws Exception {
		final ZonedDateTime calulationStartDateTime = ZonedDateTime.of(2013, 12, 30, 0, 0, 0, 0, ZoneId.systemDefault());
		final ZonedDateTime calculatedNextStartDateTime = DateUtilities.calculateNextJobStart(calulationStartDateTime, "12M99:2359", null);
		final ZonedDateTime expectedNextStartDateTime = ZonedDateTime.of(2014, 11, 30, 23, 59, 0, 0, ZoneId.systemDefault());
		assertEquals(expectedNextStartDateTime, calculatedNextStartDateTime);
	}

	@Test
	public void testXMonthRuleUltimo2() throws Exception {
		final ZonedDateTime calulationStartDateTime = ZonedDateTime.of(2013, 12, 31, 0, 0, 0, 0, ZoneId.systemDefault());
		final ZonedDateTime calculatedNextStartDateTime = DateUtilities.calculateNextJobStart(calulationStartDateTime, "12M99:2359", null);
		final ZonedDateTime expectedNextStartDateTime = ZonedDateTime.of(2014, 11, 30, 23, 59, 0, 0, ZoneId.systemDefault());
		assertEquals(expectedNextStartDateTime, calculatedNextStartDateTime);
	}

	@Test
	public void testXMonthRuleUltimo3() throws Exception {
		final ZonedDateTime calulationStartDateTime = ZonedDateTime.of(2013, 12, 31, 23, 59, 1, 0, ZoneId.systemDefault());
		final ZonedDateTime calculatedNextStartDateTime = DateUtilities.calculateNextJobStart(calulationStartDateTime, "12M99:2359", null);
		final ZonedDateTime expectedNextStartDateTime = ZonedDateTime.of(2014, 12, 31, 23, 59, 0, 0, ZoneId.systemDefault());
		assertEquals(expectedNextStartDateTime, calculatedNextStartDateTime);
	}

	@Test
	public void testXMonthRuleFirst1() throws Exception {
		final ZonedDateTime calulationStartDateTime = ZonedDateTime.of(2013, 12, 31, 23, 59, 59, 0, ZoneId.systemDefault());
		final ZonedDateTime calculatedNextStartDateTime = DateUtilities.calculateNextJobStart(calulationStartDateTime, "12M01:0000", null);
		final ZonedDateTime expectedNextStartDateTime = ZonedDateTime.of(2014, 12, 1, 0, 0, 0, 0, ZoneId.systemDefault());
		assertEquals(expectedNextStartDateTime, calculatedNextStartDateTime);
	}

	@Test
	public void testXMonthRuleFirst2() throws Exception {
		final ZonedDateTime calulationStartDateTime = ZonedDateTime.of(2013, 1, 1, 23, 59, 59, 0, ZoneId.systemDefault());
		final ZonedDateTime calculatedNextStartDateTime = DateUtilities.calculateNextJobStart(calulationStartDateTime, "12M01:0000", null);
		final ZonedDateTime expectedNextStartDateTime = ZonedDateTime.of(2014, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());
		assertEquals(expectedNextStartDateTime, calculatedNextStartDateTime);
	}

	@Test
	public void testXMonthRuleFirst3() throws Exception {
		final ZonedDateTime calulationStartDateTime = ZonedDateTime.of(2013, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());
		final ZonedDateTime calculatedNextStartDateTime = DateUtilities.calculateNextJobStart(calulationStartDateTime, "12M01:0000", null);
		final ZonedDateTime expectedNextStartDateTime = ZonedDateTime.of(2014, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());
		assertEquals(expectedNextStartDateTime, calculatedNextStartDateTime);
	}

	@Test
	public void test3TuesdayInMonth() throws Exception {
		final ZonedDateTime calulationStartDateTime = ZonedDateTime.of(2018, 2, 1, 0, 0, 1, 0, ZoneId.systemDefault());
		final ZonedDateTime calculatedNextStartDateTime = DateUtilities.calculateNextJobStart(calulationStartDateTime, "3Tu:0300", null);
		final ZonedDateTime expectedNextStartDateTime = ZonedDateTime.of(2018, 2, 20, 3, 0, 0, 0, ZoneId.systemDefault());
		assertEquals(expectedNextStartDateTime, calculatedNextStartDateTime);
	}

	@Test
	public void test2ThursdayInMonth() throws Exception {
		final ZonedDateTime calulationStartDateTime = ZonedDateTime.of(2018, 3, 1, 0, 0, 1, 0, ZoneId.systemDefault());
		final ZonedDateTime calculatedNextStartDateTime = DateUtilities.calculateNextJobStart(calulationStartDateTime, "2Th:0000", null);
		final ZonedDateTime expectedNextStartDateTime = ZonedDateTime.of(2018, 3, 8, 0, 0, 0, 0, ZoneId.systemDefault());
		assertEquals(expectedNextStartDateTime, calculatedNextStartDateTime);
	}

	@Test
	public void test5ThursdayInMonth() throws Exception {
		final ZonedDateTime calulationStartDateTime = ZonedDateTime.of(2018, 3, 1, 0, 0, 1, 0, ZoneId.systemDefault());
		final ZonedDateTime calculatedNextStartDateTime = DateUtilities.calculateNextJobStart(calulationStartDateTime, "5Th:0000", null);
		final ZonedDateTime expectedNextStartDateTime = ZonedDateTime.of(2018, 3, 29, 0, 0, 0, 0, ZoneId.systemDefault());
		assertEquals(expectedNextStartDateTime, calculatedNextStartDateTime);
	}

	@Test
	public void testMondaysAt6() throws Exception {
		final ZonedDateTime calulationStartDateTime = ZonedDateTime.of(2019, 3, 1, 0, 0, 1, 0, ZoneId.systemDefault());
		final ZonedDateTime calculatedNextStartDateTime = DateUtilities.calculateNextJobStart(calulationStartDateTime, "Mo:0600", null);
		final ZonedDateTime expectedNextStartDateTime = ZonedDateTime.of(2019, 3, 4, 6, 0, 0, 0, ZoneId.systemDefault());
		assertEquals(expectedNextStartDateTime, calculatedNextStartDateTime);
	}

	@Test
	public void test6ThursdayInMonth() {
		try {
			final ZonedDateTime calulationStartDateTime = ZonedDateTime.of(2019, 3, 1, 0, 0, 1, 0, ZoneId.systemDefault());
			final ZonedDateTime calculatedNextStartDateTime = DateUtilities.calculateNextJobStart(calulationStartDateTime, "6Th:0000", null);
			Assert.fail("Missing expected exception");
		} catch (final Exception e) {
			// expected exception
		}
	}

	@Test
	public void testIso8601DateTimeFormat() throws Exception {
		Assert.assertNull(DateUtilities.parseIso8601DateTimeString(null));
		Assert.assertNull(DateUtilities.parseIso8601DateTimeString(""));
		Assert.assertNull(DateUtilities.parseIso8601DateTimeString(" "));
		Assert.assertNull(DateUtilities.parseIso8601DateTimeString("\t"));
		Assert.assertNull(DateUtilities.parseIso8601DateTimeString("\n"));

		assertEquals(ZonedDateTime.of(2008, 10, 31, 0, 0, 0, 0, ZoneId.systemDefault()), DateUtilities.parseIso8601DateTimeString("2008-10-31"));
		assertEquals(ZonedDateTime.of(2008, 10, 31, 0, 0, 0, 0, ZoneId.of("Z")), DateUtilities.parseIso8601DateTimeString("2008-10-31Z"));
		assertEquals(ZonedDateTime.of(2008, 10, 31, 0, 0, 0, 0, ZoneId.of("+05:00")), DateUtilities.parseIso8601DateTimeString("2008-10-31+05:00"));
		assertEquals(ZonedDateTime.of(2008, 10, 31, 0, 0, 0, 0, ZoneId.of("-05:00")), DateUtilities.parseIso8601DateTimeString("2008-10-31-05:00"));
		assertEquals(ZonedDateTime.of(2008, 10, 31, 15, 7, 38, 0, ZoneId.systemDefault()), DateUtilities.parseIso8601DateTimeString("2008-10-31T15:07:38"));
		assertEquals(ZonedDateTime.of(2008, 10, 31, 15, 7, 38, 0, ZoneId.of("Z")), DateUtilities.parseIso8601DateTimeString("2008-10-31T15:07:38Z"));
		assertEquals(ZonedDateTime.of(2008, 10, 31, 15, 7, 38, 0, ZoneId.of("+05:00")), DateUtilities.parseIso8601DateTimeString("2008-10-31T15:07:38+05:00"));
		assertEquals(ZonedDateTime.of(2008, 10, 31, 15, 7, 38, 0, ZoneId.of("-05:00")), DateUtilities.parseIso8601DateTimeString("2008-10-31T15:07:38-05:00"));
		assertEquals(ZonedDateTime.of(2017, 11, 23, 20, 56, 40, 186000000, ZoneId.systemDefault()), DateUtilities.parseIso8601DateTimeString("2017-11-23T20:56:40.186"));
		assertEquals(ZonedDateTime.of(2017, 11, 23, 20, 56, 40, 186000000, ZoneId.systemDefault()), DateUtilities.parseIso8601DateTimeString("2017-11-23T20:56:40.186"));
	}
}
