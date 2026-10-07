package de.soderer.utilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

public class PropertiesWriterTest {
	@SuppressWarnings("static-method")
	@Test
	public void test() {
		try {
			final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
			try (PropertiesWriter propertiesWriter = new PropertiesWriter(byteArrayOutputStream)) {

				propertiesWriter.writeProperty("key1", "value 1");
				propertiesWriter.writeComment("Comment for key2\nComment line2 for key2");
				propertiesWriter.writeProperty("key2", "Valü 2");
				propertiesWriter.writeProperty("key3", "value31\nvalue32");
				propertiesWriter.writeProperty("key4", "value 4");
				propertiesWriter.writeProperty("key5", " value 5 ");
			}

			final String data = new String(byteArrayOutputStream.toByteArray(), StandardCharsets.ISO_8859_1);
			final String dataExpected = "key1=value 1\n"
					+ "# Comment for key2\n"
					+ "# Comment line2 for key2\n"
					+ "key2=Val\\u00FC 2\n"
					+ "key3=value31\\nvalue32\n"
					+ "key4=value 4\n"
					+ "key5=\\ value 5 \n";

			assertEquals(dataExpected, data);
		} catch (final Exception e) {
			fail(e.getMessage());
		}
	}
}
