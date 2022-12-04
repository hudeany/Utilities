package de.soderer.utilities;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

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
}
