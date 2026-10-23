package de.soderer.utilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import org.junit.jupiter.api.Test;

@SuppressWarnings("static-method")
public class TextPropertiesReaderTest {
	private static TextPropertiesReader read(final String text) {
		final TextPropertiesReader reader = new TextPropertiesReader(text);
		reader.readProperties();
		return reader;
	}

	private static void assertProperties(final String text, final int characters, final int nonWhitespaceCharacters, final int lines, final int words, final LineBreak lineBreak) {
		final TextPropertiesReader reader = read(text);
		assertEquals(characters, reader.getCharactersCount(), "characters");
		assertEquals(nonWhitespaceCharacters, reader.getNonWhitespaceCharactersCount(), "non whitespace characters");
		assertEquals(lines, reader.getLinesCount(), "lines");
		assertEquals(words, reader.getWordsCount(), "words");
		assertEquals(lineBreak, reader.getLinebreakType(), "line break type");
	}

	@Test
	public void testBeforeReading() {
		final TextPropertiesReader reader = new TextPropertiesReader("text");
		assertEquals(-1, reader.getCharactersCount());
		assertEquals(-1, reader.getNonWhitespaceCharactersCount());
		assertEquals(-1, reader.getLinesCount());
		assertEquals(-1, reader.getWordsCount());
		assertNull(reader.getFoundCharacters());
		assertEquals(LineBreak.Unknown, reader.getLinebreakType());
	}

	@Test
	public void testEmptyText() {
		assertProperties("", 0, 0, 0, 0, LineBreak.Unknown);
		assertEquals(0, read("").getFoundCharacters().size());
	}

	@Test
	public void testSingleLine() {
		assertProperties("one", 3, 3, 1, 1, LineBreak.Unknown);
		assertProperties("x\ty  z", 6, 3, 1, 3, LineBreak.Unknown);
		assertProperties("  leading and trailing  ", 24, 18, 1, 3, LineBreak.Unknown);
		assertProperties(" ", 1, 0, 1, 0, LineBreak.Unknown);
	}

	@Test
	public void testLineBreakTypes() {
		assertProperties("a b\nc", 5, 3, 2, 3, LineBreak.Unix);
		assertProperties("a\r\nb", 4, 2, 2, 2, LineBreak.Windows);
		assertProperties("a\rb", 3, 2, 2, 2, LineBreak.Mac);
		assertProperties("a\nb\r\nc", 6, 3, 3, 3, LineBreak.Mixed);
		assertProperties("a\rb\nc", 5, 3, 3, 3, LineBreak.Mixed);
		assertProperties("a\r\nb\rc", 6, 3, 3, 3, LineBreak.Mixed);
	}

	@Test
	public void testTrailingAndEmptyLines() {
		// A trailing line break does not start a further line
		assertProperties("a\n", 2, 1, 1, 1, LineBreak.Unix);
		assertProperties("a\n\n", 3, 1, 2, 1, LineBreak.Unix);
		assertProperties("\n", 1, 0, 1, 0, LineBreak.Unix);
		assertProperties("a\r\n", 3, 1, 1, 1, LineBreak.Windows);
		// A last line with only whitespace counts
		assertProperties("a\n ", 3, 1, 2, 1, LineBreak.Unix);
	}

	@Test
	public void testWordsAtLineBreaks() {
		// Words directly before a line break are counted
		assertProperties("one\ntwo\r\nthree\rfour", 19, 15, 4, 4, LineBreak.Mixed);
	}

	@Test
	public void testFoundCharacters() {
		final Map<Character, Integer> found = read("abca\r\nb\n").getFoundCharacters();
		assertEquals(Integer.valueOf(2), found.get('a'));
		assertEquals(Integer.valueOf(2), found.get('b'));
		assertEquals(Integer.valueOf(1), found.get('c'));
		assertEquals(Integer.valueOf(1), found.get('\r'));
		// The LF of the CRLF is counted as well
		assertEquals(Integer.valueOf(2), found.get('\n'));
		assertEquals(5, found.size());
	}

	@Test
	public void testNonBreakingSpaceIsNoWhitespace() {
		// Character.isWhitespace() is false for the non-breaking space
		assertProperties("a\u00A0b", 3, 3, 1, 1, LineBreak.Unknown);
	}

	@Test
	public void testReadingAgain() {
		final TextPropertiesReader reader = read("a b");
		reader.readProperties();
		assertEquals(2, reader.getWordsCount());
		assertEquals(Integer.valueOf(1), reader.getFoundCharacters().get('a'));
	}

	@Test
	public void testNullText() {
		assertThrows(NullPointerException.class, () -> new TextPropertiesReader(null).readProperties());
	}
}
