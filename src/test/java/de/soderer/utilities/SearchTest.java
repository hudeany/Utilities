package de.soderer.utilities;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

@SuppressWarnings("static-method")
public class SearchTest {
	@Test
	public void testSearch1() {
		try {
			final String text = "AbcaBcabCabcaBC";
			final String searchText = "bc";
			final Tuple<Integer, Integer> position = TextUtilities.searchNextPosition(text, 0, searchText, false, false, false);
			assertEquals(1, position.getFirst().intValue());
			assertEquals(2, position.getSecond().intValue());
		} catch (final Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testSearch2() {
		try {
			final String text = "AbcaBcabCabcaBC";
			final String searchText = "bc";
			final Tuple<Integer, Integer> position = TextUtilities.searchNextPosition(text, 2, searchText, false, false, false);
			assertEquals(4, position.getFirst().intValue());
			assertEquals(2, position.getSecond().intValue());
		} catch (final Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testSearch3() {
		try {
			final String text = "ABCABCABC";
			final String searchText = "bC";
			final Tuple<Integer, Integer> position = TextUtilities.searchNextPosition(text, 0, searchText, false, true, false);
			assertEquals(null, position);
		} catch (final Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testSearch4() {
		try {
			final String text = "AbcaBcabCabcaBC";
			final String searchText = "bC";
			final Tuple<Integer, Integer> position = TextUtilities.searchNextPosition(text, 0, searchText, false, true, false);
			assertEquals(7, position.getFirst().intValue());
			assertEquals(2, position.getSecond().intValue());
		} catch (final Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testSearch5() {
		try {
			final String text = "AbcaBcabCabcaBC";
			final String searchText = "abC";
			final Tuple<Integer, Integer> position = TextUtilities.searchNextPosition(text, 10, searchText, false, false, true);
			assertEquals(9, position.getFirst().intValue());
			assertEquals(3, position.getSecond().intValue());
		} catch (final Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testSearch6() {
		try {
			final String text = "AbcaBcabCabcaBC";
			final String searchText = "abC";
			final Tuple<Integer, Integer> position = TextUtilities.searchNextPosition(text, 9, searchText, false, false, true);
			assertEquals(6, position.getFirst().intValue());
			assertEquals(3, position.getSecond().intValue());
		} catch (final Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testSearch7() {
		try {
			final String text = "aaaaaaaaaaaa";
			final String searchText = "a";
			final Tuple<Integer, Integer> position = TextUtilities.searchNextPosition(text, 5, searchText, false, false, false);
			assertEquals(5, position.getFirst().intValue());
			assertEquals(1, position.getSecond().intValue());
		} catch (final Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testSearch8() {
		try {
			final String text = "aaaaaaaaaaaa";
			final String searchText = "a";
			final Tuple<Integer, Integer> position = TextUtilities.searchNextPosition(text, 5, searchText, false, false, true);
			assertEquals(4, position.getFirst().intValue());
			assertEquals(1, position.getSecond().intValue());
		} catch (final Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testSearch9() {
		try {
			final String text = "aaaaaaaaaaaa";
			final String searchText = "a";
			final Tuple<Integer, Integer> position = TextUtilities.searchNextPosition(text, 5, searchText, true, false, false);
			assertEquals(5, position.getFirst().intValue());
			assertEquals(1, position.getSecond().intValue());
		} catch (final Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testSearch10() {
		try {
			final String text = "aaaaaaaaaaaa";
			final String searchText = "a";
			final Tuple<Integer, Integer> position = TextUtilities.searchNextPosition(text, 5, searchText, true, false, true);
			assertEquals(4, position.getFirst().intValue());
			assertEquals(1, position.getSecond().intValue());
		} catch (final Exception e) {
			fail(e.getMessage());
		}
	}
}
