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
}
