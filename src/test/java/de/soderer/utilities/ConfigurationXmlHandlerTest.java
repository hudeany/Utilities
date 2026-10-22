package de.soderer.utilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import org.junit.jupiter.api.Test;

import de.soderer.utilities.xml.ConfigurationXmlHandler;

@SuppressWarnings("static-method")
public class ConfigurationXmlHandlerTest {
	private static Map<String, String> parse(final String xml) throws Exception {
		return new ConfigurationXmlHandler(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))).getConfigurationValueMap();
	}

	/**
	 * Input stream that remembers whether it was closed.
	 */
	private static class CloseTrackingInputStream extends ByteArrayInputStream {
		boolean closed = false;

		CloseTrackingInputStream(final byte[] data) {
			super(data);
		}

		@Override
		public void close() throws IOException {
			closed = true;
			super.close();
		}
	}

	@Test
	public void testReadValues() throws Exception {
		final Map<String, String> values = parse("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
				+ "<Configuration>"
				+ "<Entry name=\"b\" value=\"2\"/>"
				+ "<Entry name=\"a\" value=\"1\"></Entry>"
				+ "<Entry name=\"special\" value=\"&lt;&amp;&quot;\u00E4\"/>"
				+ "<Entry name=\"empty\" value=\"\"/>"
				+ "<Entry name=\"noValue\"/>"
				+ "</Configuration>");
		// Document order is kept
		assertEquals(Arrays.asList("b", "a", "special", "empty", "noValue"), new ArrayList<>(values.keySet()));
		assertEquals("2", values.get("b"));
		assertEquals("1", values.get("a"));
		assertEquals("<&\"\u00E4", values.get("special"));
		assertEquals("", values.get("empty"));
		assertTrue(values.containsKey("noValue"));
		assertNull(values.get("noValue"));
	}

	@Test
	public void testEmptyConfiguration() throws Exception {
		assertTrue(parse("<Configuration/>").isEmpty());
		assertTrue(parse("<Configuration>\n  <!-- nothing -->\n</Configuration>").isEmpty());
	}

	@Test
	public void testDuplicateNamesLastWins() throws Exception {
		final Map<String, String> values = parse("<Configuration><Entry name=\"a\" value=\"1\"/><Entry name=\"a\" value=\"2\"/></Configuration>");
		assertEquals(1, values.size());
		assertEquals("2", values.get("a"));
	}

	@Test
	public void testOtherElementsAreIgnored() throws Exception {
		final Map<String, String> values = parse("<Configuration><Comment>x</Comment><Group><Entry name=\"nested\" value=\"n\"/></Group><Entry name=\"a\" value=\"1\"/></Configuration>");
		// Entries are recognized at any depth below the root tag
		assertEquals("n", values.get("nested"));
		assertEquals("1", values.get("a"));
		assertEquals(2, values.size());
	}

	@Test
	public void testNamesAreCaseSensitive() throws Exception {
		final Map<String, String> values = parse("<Configuration><entry name=\"a\" value=\"1\"/><Entry Name=\"b\" value=\"2\" name=\"c\"/></Configuration>");
		assertFalse(values.containsKey("a"));
		assertFalse(values.containsKey("b"));
		assertEquals("2", values.get("c"));
	}

	@Test
	public void testInvalidRootTag() {
		final Exception exception = assertThrows(Exception.class, () -> parse("<Config><Entry name=\"a\" value=\"1\"/></Config>"));
		assertEquals("Error while parsing XmlConfiguration", exception.getMessage());
		assertTrue(exception.getCause().getMessage().contains("Invalid RootTag"), exception.getCause().getMessage());
		assertThrows(Exception.class, () -> parse("<configuration/>"));
	}

	@Test
	public void testEntryWithoutName() {
		final Exception exception = assertThrows(Exception.class, () -> parse("<Configuration><Entry value=\"1\"/></Configuration>"));
		assertTrue(exception.getCause().getMessage().contains("Missing attribute 'name'"), exception.getCause().getMessage());
	}

	@Test
	public void testMalformedXml() {
		assertThrows(Exception.class, () -> parse("<Configuration><Entry name=\"a\" value=\"1\"></Configuration>"));
		assertThrows(Exception.class, () -> parse(""));
		assertThrows(Exception.class, () -> parse("no xml"));
	}

	@Test
	public void testDoctypeAndExternalEntitiesAreRejected() throws Exception {
		final File secretFile = File.createTempFile("ConfigurationXmlHandlerTest", ".txt");
		secretFile.deleteOnExit();
		Files.write(secretFile.toPath(), "TOP-SECRET".getBytes(StandardCharsets.UTF_8));

		assertThrows(Exception.class, () -> parse("<!DOCTYPE Configuration [<!ENTITY xxe SYSTEM \"" + secretFile.toURI() + "\">]>"
				+ "<Configuration><Entry name=\"a\" value=\"&xxe;\"/></Configuration>"));
		assertThrows(Exception.class, () -> parse("<!DOCTYPE Configuration [<!ENTITY a \"aaaaaaaaaa\"><!ENTITY b \"&a;&a;&a;&a;&a;&a;&a;&a;&a;&a;\">]>"
				+ "<Configuration><Entry name=\"a\" value=\"&b;\"/></Configuration>"));
		assertThrows(Exception.class, () -> parse("<!DOCTYPE Configuration SYSTEM \"http://invalid.invalid/config.dtd\"><Configuration/>"));
	}

	@Test
	public void testInputStreamIsNotClosed() throws Exception {
		final CloseTrackingInputStream inputStream = new CloseTrackingInputStream("<Configuration><Entry name=\"a\" value=\"1\"/></Configuration>".getBytes(StandardCharsets.UTF_8));
		final ConfigurationXmlHandler handler = new ConfigurationXmlHandler(inputStream);
		assertEquals("1", handler.getConfigurationValueMap().get("a"));
		assertFalse(inputStream.closed);
	}

	@Test
	public void testEncodingFromDeclaration() throws Exception {
		final byte[] data = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><Configuration><Entry name=\"\u00E4\" value=\"\u00F6\"/></Configuration>".getBytes(StandardCharsets.ISO_8859_1);
		try (InputStream inputStream = new ByteArrayInputStream(data)) {
			final Map<String, String> values = new ConfigurationXmlHandler(inputStream).getConfigurationValueMap();
			assertEquals("\u00F6", values.get("\u00E4"));
		}
	}
}
