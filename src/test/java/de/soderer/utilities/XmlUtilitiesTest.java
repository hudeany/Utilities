package de.soderer.utilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

import de.soderer.utilities.xml.XmlUtilities;

@SuppressWarnings("static-method")
public class XmlUtilitiesTest {
	@Test
	public void testRemoveFrameTag() {
		try {
			final String testString = "<abc def=\"123\">ghi</abc>";
			final String resultString = XmlUtilities.removeFrameTag(testString);
			assertEquals("ghi", resultString);
		} catch (final Exception e) {
			fail(e.getMessage());
		}
	}
}
