package de.soderer.utilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

@SuppressWarnings("static-method")
public class RecordReaderTest {
	/** Columns: id (3 chars), name (8 chars), amount (5 chars) */
	private static List<Tuple<String, Integer>> createDescription() {
		final List<Tuple<String, Integer>> description = new ArrayList<>();
		description.add(new Tuple<>("id", 3));
		description.add(new Tuple<>("name", 8));
		description.add(new Tuple<>("amount", 5));
		return description;
	}

	/** One fixed width line: id left aligned in 3 chars, name left aligned in 8 chars, amount right aligned in 5 chars */
	private static String line(final String id, final String name, final String amount) {
		return String.format("%-3s%-8s%5s", id, name, amount);
	}

	private static InputStream stream(final String text) {
		return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
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
	public void testReadRecords() throws Exception {
		try (RecordReader reader = new RecordReader(stream(line("001", "Alice", "12.5") + "\n" + line("002", "Bob", "7.0") + "\r\n" + line("003", "Carol", "100.0")), createDescription())) {
			assertEquals(Arrays.asList("001", "Alice", "12.5"), reader.readNextRecord());
			assertEquals(Arrays.asList("002", "Bob", "7.0"), reader.readNextRecord());
			assertEquals(Arrays.asList("003", "Carol", "100.0"), reader.readNextRecord());
			assertNull(reader.readNextRecord());
			assertNull(reader.readNextRecord());
			assertEquals(3, reader.getLinesRead());
		}
	}

	@Test
	public void testValuesAreTrimmed() throws Exception {
		try (RecordReader reader = new RecordReader(stream("  1  Al ice  1.5 \n"), createDescription())) {
			assertEquals(Arrays.asList("1", "Al ice", "1.5"), reader.readNextRecord());
		}
	}

	@Test
	public void testBlankLinesAreSkippedButCounted() throws Exception {
		try (RecordReader reader = new RecordReader(stream("\n   \n" + line("001", "Alice", "12.5") + "\n\t\n\n"), createDescription())) {
			assertEquals("001", reader.readNextRecord().get(0));
			assertEquals(3, reader.getLinesRead());
			assertNull(reader.readNextRecord());
			// "\t" and the empty line before the final line break
			assertEquals(5, reader.getLinesRead());
		}
	}

	@Test
	public void testCharactersBehindLastColumnAreIgnored() throws Exception {
		try (RecordReader reader = new RecordReader(stream(line("001", "Alice", "12.5") + " trailing data"), createDescription())) {
			assertEquals(Arrays.asList("001", "Alice", "12.5"), reader.readNextRecord());
		}
	}

	@Test
	public void testUnderfilledLines() throws Exception {
		try (RecordReader reader = new RecordReader(stream("001Alice   12\n"), createDescription())) {
			assertFalse(reader.isAllowUnderfilledLines());
			final Exception exception = assertThrows(Exception.class, reader::readNextRecord);
			assertTrue(exception.getMessage().contains("line 1"), exception.getMessage());
			assertTrue(exception.getMessage().contains("'amount'"), exception.getMessage());
		}

		try (RecordReader reader = new RecordReader(stream("001Alice   12\n002Bo\n00"), createDescription())) {
			reader.setAllowUnderfilledLines(true);
			assertTrue(reader.isAllowUnderfilledLines());
			// Only completely contained columns are returned
			assertEquals(Arrays.asList("001", "Alice"), reader.readNextRecord());
			assertEquals(Arrays.asList("002"), reader.readNextRecord());
			assertEquals(new ArrayList<>(), reader.readNextRecord());
			assertNull(reader.readNextRecord());
		}
	}

	@Test
	public void testZeroLengthColumn() throws Exception {
		final List<Tuple<String, Integer>> description = new ArrayList<>();
		description.add(new Tuple<>("a", 2));
		description.add(new Tuple<>("empty", 0));
		description.add(new Tuple<>("b", 2));
		try (RecordReader reader = new RecordReader(stream("xxyy"), description)) {
			assertEquals(Arrays.asList("xx", "", "yy"), reader.readNextRecord());
		}
	}

	@Test
	public void testEmptyDescription() throws Exception {
		try (RecordReader reader = new RecordReader(stream("anything\n"), new ArrayList<>())) {
			assertEquals(new ArrayList<>(), reader.readNextRecord());
			assertNull(reader.readNextRecord());
		}
	}

	@Test
	public void testByteOrderMarkIsSkipped() throws Exception {
		final byte[] data = ("\uFEFF" + line("001", "Alice", "12.5")).getBytes(StandardCharsets.UTF_8);
		try (RecordReader reader = new RecordReader(new ByteArrayInputStream(data), createDescription())) {
			assertEquals(Arrays.asList("001", "Alice", "12.5"), reader.readNextRecord());
		}
	}

	@Test
	public void testEncoding() throws Exception {
		final byte[] data = line("001", "M\u00FCller", "12.5").getBytes(StandardCharsets.ISO_8859_1);
		try (RecordReader reader = new RecordReader(new ByteArrayInputStream(data), StandardCharsets.ISO_8859_1, createDescription())) {
			assertEquals(Arrays.asList("001", "M\u00FCller", "12.5"), reader.readNextRecord());
		}
		// Columns count characters, not bytes
		try (RecordReader reader = new RecordReader(stream(line("001", "\u00C4\u00D6\u00DC\u20AC\u00DF", "12.5")), createDescription())) {
			assertEquals(Arrays.asList("001", "\u00C4\u00D6\u00DC\u20AC\u00DF", "12.5"), reader.readNextRecord());
		}
	}

	@Test
	public void testEmptyData() throws Exception {
		try (RecordReader reader = new RecordReader(stream(""), createDescription())) {
			assertNull(reader.readNextRecord());
			assertEquals(0, reader.getLinesRead());
		}
	}

	@Test
	public void testInvalidArguments() {
		final List<Tuple<String, Integer>> description = createDescription();
		assertThrows(IllegalArgumentException.class, () -> new RecordReader(null, description));
		assertThrows(IllegalArgumentException.class, () -> new RecordReader(stream(""), null, description));
		assertThrows(IllegalArgumentException.class, () -> new RecordReader(stream(""), null));

		final List<Tuple<String, Integer>> withNull = createDescription();
		withNull.add(null);
		assertThrows(IllegalArgumentException.class, () -> new RecordReader(stream(""), withNull));

		final List<Tuple<String, Integer>> withNullLength = createDescription();
		withNullLength.add(new Tuple<>("x", null));
		assertThrows(IllegalArgumentException.class, () -> new RecordReader(stream(""), withNullLength));

		final List<Tuple<String, Integer>> withNegativeLength = createDescription();
		withNegativeLength.add(new Tuple<>("x", -1));
		assertThrows(IllegalArgumentException.class, () -> new RecordReader(stream(""), withNegativeLength));
	}

	@Test
	public void testDescriptionIsCopied() throws Exception {
		final List<Tuple<String, Integer>> description = createDescription();
		try (RecordReader reader = new RecordReader(stream(line("001", "Alice", "12.5")), description)) {
			// Later changes of the caller's list do not affect the reader
			description.add(new Tuple<>("invalid", -1));
			description.remove(0);
			assertEquals(Arrays.asList("001", "Alice", "12.5"), reader.readNextRecord());
		}
	}

	@Test
	public void testCloseClosesStream() throws Exception {
		final CloseTrackingInputStream unread = new CloseTrackingInputStream(line("001", "Alice", "12.5").getBytes(StandardCharsets.UTF_8));
		new RecordReader(unread, createDescription()).close();
		assertTrue(unread.closed);

		final CloseTrackingInputStream read = new CloseTrackingInputStream(line("001", "Alice", "12.5").getBytes(StandardCharsets.UTF_8));
		try (final RecordReader reader = new RecordReader(read, createDescription())) {
			reader.readNextRecord();
		}
		assertTrue(read.closed);
	}
}
