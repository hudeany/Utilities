package de.soderer.utilities;

import javax.mail.internet.InternetAddress;

import org.junit.Assert;
import org.junit.Test;

import de.soderer.utilities.mail.MailUtilities;

public class MailUtilitiesTest {
	@Test
	public void testEmailAddressListParsing() {
		try {
			final InternetAddress[] addressList = MailUtilities.getEmailAddressesFromList("Test1@example.com Test2@example.com");

			Assert.assertEquals(2, addressList.length);

			Assert.assertEquals(null, addressList[0].getPersonal());
			Assert.assertEquals("test1@example.com", addressList[0].getAddress());

			Assert.assertEquals(null, addressList[1].getPersonal());
			Assert.assertEquals("test2@example.com", addressList[1].getAddress());

			final InternetAddress[] addressList1 = MailUtilities.getEmailAddressesFromList("First Test <Test1@example.com> Second Test <Test2@example.com>");
			Assert.assertEquals(2, addressList1.length);

			Assert.assertEquals("First Test", addressList1[0].getPersonal());
			Assert.assertEquals("test1@example.com", addressList1[0].getAddress());

			Assert.assertEquals("Second Test", addressList1[1].getPersonal());
			Assert.assertEquals("test2@example.com", addressList1[1].getAddress());

			final InternetAddress[] addressList2 = MailUtilities.getEmailAddressesFromList("First Test<Test1@example.com>,Second Test<Test2@example.com>");
			Assert.assertEquals(2, addressList2.length);

			Assert.assertEquals("First Test", addressList2[0].getPersonal());
			Assert.assertEquals("test1@example.com", addressList2[0].getAddress());

			Assert.assertEquals("Second Test", addressList2[1].getPersonal());
			Assert.assertEquals("test2@example.com", addressList2[1].getAddress());

			final InternetAddress[] addressList3 = MailUtilities.getEmailAddressesFromList("First Test<Test1@example.com>;Second Test<Test2@example.com>");
			Assert.assertEquals(2, addressList3.length);

			Assert.assertEquals("First Test", addressList3[0].getPersonal());
			Assert.assertEquals("test1@example.com", addressList3[0].getAddress());

			Assert.assertEquals("Second Test", addressList3[1].getPersonal());
			Assert.assertEquals("test2@example.com", addressList3[1].getAddress());
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}
}
