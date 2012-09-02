package de.soderer.utilities;

import org.junit.Assert;
import org.junit.Test;

public class SearchTest {
	@Test
	public void testSearch1() {
		try {
			String text = "AbcaBcabCabcaBC";
			String searchText = "bc";
			int position = Utilities.searchNextPosition(text, 0, searchText, false, false, false);
			Assert.assertEquals(1, position);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void testSearch2() {
		try {
			String text = "AbcaBcabCabcaBC";
			String searchText = "bc";
			int position = Utilities.searchNextPosition(text, 2, searchText, false, false, false);
			Assert.assertEquals(4, position);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void testSearch3() {
		try {
			String text = "ABCABCABC";
			String searchText = "bC";
			int position = Utilities.searchNextPosition(text, 0, searchText, false, true, false);
			Assert.assertEquals(-1, position);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void testSearch4() {
		try {
			String text = "AbcaBcabCabcaBC";
			String searchText = "bC";
			int position = Utilities.searchNextPosition(text, 0, searchText, false, true, false);
			Assert.assertEquals(7, position);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void testSearch5() {
		try {
			String text = "AbcaBcabCabcaBC";
			String searchText = "abC";
			int position = Utilities.searchNextPosition(text, 10, searchText, false, false, true);
			Assert.assertEquals(9, position);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void testSearch6() {
		try {
			String text = "AbcaBcabCabcaBC";
			String searchText = "abC";
			int position = Utilities.searchNextPosition(text, 9, searchText, false, false, true);
			Assert.assertEquals(6, position);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}
}
