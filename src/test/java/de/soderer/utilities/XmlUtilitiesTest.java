package de.soderer.utilities;

import org.junit.Assert;
import org.junit.Test;

import de.soderer.utilities.xml.XmlUtilities;

@SuppressWarnings("static-method")
public class XmlUtilitiesTest {
	@Test
	public void testRemoveFrameTag() {
		try {
			final String testString = "<abc def=\"123\">ghi</abc>";
			final String resultString = XmlUtilities.removeFrameTag(testString);
			Assert.assertEquals("ghi", resultString);
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}
}
