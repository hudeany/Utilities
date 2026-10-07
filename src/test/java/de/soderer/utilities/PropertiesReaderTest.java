package de.soderer.utilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.Test;

@SuppressWarnings("static-method")
public class PropertiesReaderTest {
	@Test
	public void test() {
		try {
			final String dataString = "key1 value 1" + "\n"
					+ " key2=V\\al\u00FC 2" + "\r\n"
					+ "   ! Comment for key3" + "\n"
					+ "key3 = value 3 " + "\r"
					+ "key4=value 41 \\" + "\n"
					+ "    value 42" + "\n"
					+ " # Comment for key5" + "\n"
					+ " # Comment line2 for key5" + "\n"
					+ "key5=value5\\n" + "\n"
					+ "key6 : value6" + "\n"
					+ "key7=\\ value7\\ " + "\n";
			Map<String, String> properties;
			Map<String, String> comments;
			try (PropertiesReader propertiesReader = new PropertiesReader(new ByteArrayInputStream(dataString.getBytes(StandardCharsets.ISO_8859_1)))) {
				properties = propertiesReader.read();
				comments = propertiesReader.getComments();
			}

			assertEquals("value 1", properties.get("key1"));
			assertEquals("Valü 2", properties.get("key2"));
			assertEquals("value 3 ", properties.get("key3"));
			assertEquals("value 41 value 42", properties.get("key4"));
			assertEquals("value5\n", properties.get("key5"));
			assertEquals("value6", properties.get("key6"));
			assertEquals(" value7 ", properties.get("key7"));

			assertEquals("Comment for key3", comments.get("key3"));
			assertEquals("Comment for key5\nComment line2 for key5", comments.get("key5"));
		} catch (final Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void test2() {
		try {
			final String dataString = "key1=C:\\\\My Dir" + "\n";
			Map<String, String> properties;
			try (PropertiesReader propertiesReader = new PropertiesReader(new ByteArrayInputStream(dataString.getBytes(StandardCharsets.ISO_8859_1)))) {
				properties = propertiesReader.read();
			}

			assertEquals("C:\\My Dir", properties.get("key1"));
		} catch (final Exception e) {
			fail(e.getMessage());
		}
	}
}
