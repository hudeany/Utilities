package de.soderer.utilities;

import org.junit.Assert;
import org.junit.Test;

public class BitUtilitiesTest {

	@Test
	public void test1() {
		try {
			String result = BitUtilities.getBitString((byte) 1);
			Assert.assertEquals("00000001", result);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void test2() {
		try {
			String result = BitUtilities.getBitString((byte) 127);
			Assert.assertEquals("01111111", result);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void test3() {
		try {
			String result = BitUtilities.getBitString((byte) 128);
			Assert.assertEquals("10000000", result);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void test4() {
		try {
			String result = BitUtilities.getBitString((byte) 255);
			Assert.assertEquals("11111111", result);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void test5() {
		try {
			String result = BitUtilities.getBitString((byte) 256);
			Assert.assertEquals("00000000", result);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void test6() {
		try {
			String result = BitUtilities.getBitString(new byte[]{ 1, 2, 3});
			Assert.assertEquals("00000001 00000010 00000011", result);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}
}
