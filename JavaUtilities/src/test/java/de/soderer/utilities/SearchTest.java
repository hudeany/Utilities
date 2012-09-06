package de.soderer.utilities;

import org.junit.Assert;
import org.junit.Test;

public class SearchTest {
	@Test
	public void testSearch1() {
		try {
			String text = "AbcaBcabCabcaBC";
			String searchText = "bc";
			Tuple<Integer, Integer> position = Utilities.searchNextPosition(text, 0, searchText, false, false, false);
			Assert.assertEquals(1, position.getFirst().intValue());
			Assert.assertEquals(2, position.getSecond().intValue());
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void testSearch2() {
		try {
			String text = "AbcaBcabCabcaBC";
			String searchText = "bc";
			Tuple<Integer, Integer> position = Utilities.searchNextPosition(text, 2, searchText, false, false, false);
			Assert.assertEquals(4, position.getFirst().intValue());
			Assert.assertEquals(2, position.getSecond().intValue());
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void testSearch3() {
		try {
			String text = "ABCABCABC";
			String searchText = "bC";
			Tuple<Integer, Integer> position = Utilities.searchNextPosition(text, 0, searchText, false, true, false);
			Assert.assertEquals(null, position);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void testSearch4() {
		try {
			String text = "AbcaBcabCabcaBC";
			String searchText = "bC";
			Tuple<Integer, Integer> position = Utilities.searchNextPosition(text, 0, searchText, false, true, false);
			Assert.assertEquals(7, position.getFirst().intValue());
			Assert.assertEquals(2, position.getSecond().intValue());
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void testSearch5() {
		try {
			String text = "AbcaBcabCabcaBC";
			String searchText = "abC";
			Tuple<Integer, Integer> position = Utilities.searchNextPosition(text, 10, searchText, false, false, true);
			Assert.assertEquals(9, position.getFirst().intValue());
			Assert.assertEquals(3, position.getSecond().intValue());
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void testSearch6() {
		try {
			String text = "AbcaBcabCabcaBC";
			String searchText = "abC";
			Tuple<Integer, Integer> position = Utilities.searchNextPosition(text, 9, searchText, false, false, true);
			Assert.assertEquals(6, position.getFirst().intValue());
			Assert.assertEquals(3, position.getSecond().intValue());
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testSearch7() {
		try {
			String text = "aaaaaaaaaaaa";
			String searchText = "a";
			Tuple<Integer, Integer> position = Utilities.searchNextPosition(text, 5, searchText, false, false, false);
			Assert.assertEquals(5, position.getFirst().intValue());
			Assert.assertEquals(1, position.getSecond().intValue());
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testSearch8() {
		try {
			String text = "aaaaaaaaaaaa";
			String searchText = "a";
			Tuple<Integer, Integer> position = Utilities.searchNextPosition(text, 5, searchText, false, false, true);
			Assert.assertEquals(4, position.getFirst().intValue());
			Assert.assertEquals(1, position.getSecond().intValue());
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testSearch9() {
		try {
			String text = "aaaaaaaaaaaa";
			String searchText = "a";
			Tuple<Integer, Integer> position = Utilities.searchNextPosition(text, 5, searchText, true, false, false);
			Assert.assertEquals(5, position.getFirst().intValue());
			Assert.assertEquals(1, position.getSecond().intValue());
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testSearch10() {
		try {
			String text = "aaaaaaaaaaaa";
			String searchText = "a";
			Tuple<Integer, Integer> position = Utilities.searchNextPosition(text, 5, searchText, true, false, true);
			Assert.assertEquals(4, position.getFirst().intValue());
			Assert.assertEquals(1, position.getSecond().intValue());
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}
}
