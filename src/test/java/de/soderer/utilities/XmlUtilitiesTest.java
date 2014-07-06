package de.soderer.utilities;

import org.junit.Assert;
import org.junit.Test;

import de.soderer.utilities.xml.XmlUtilities;

public class XmlUtilitiesTest {
	@Test
	public void testRemoveFrameTag() {
		try {
			String testString = "<abc def=\"123\">ghi</abc>";
			String resultString = XmlUtilities.removeFrameTag(testString);
			Assert.assertEquals("ghi", resultString);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}
}
