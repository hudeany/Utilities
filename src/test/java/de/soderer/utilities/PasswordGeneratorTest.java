package de.soderer.utilities;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class PasswordGeneratorTest {
	@Test
	public void test1() {
		try {
			List<PasswordGeneratorCharacterGroup> groups = new ArrayList<PasswordGeneratorCharacterGroup>();
			groups.add(new PasswordGeneratorCharacterGroup(1, 99, "abcdefghijklmnopqrstuvwxyz".toCharArray()));
			groups.add(new PasswordGeneratorCharacterGroup(1, 99, "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray()));
			groups.add(new PasswordGeneratorCharacterGroup(1, 99, "0123456789".toCharArray()));
			groups.add(new PasswordGeneratorCharacterGroup(1, 99, "!§$%&?*+-/".toCharArray()));
			
			String password = new String(PasswordGenerator.generatePassword(groups, 10, 10));
			Assert.assertTrue(password.length() == 10);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void test2() {
		try {
			List<PasswordGeneratorCharacterGroup> groups = new ArrayList<PasswordGeneratorCharacterGroup>();
			groups.add(new PasswordGeneratorCharacterGroup(1, 99, "a".toCharArray()));
			groups.add(new PasswordGeneratorCharacterGroup(1, 99, "A".toCharArray()));
			groups.add(new PasswordGeneratorCharacterGroup(1, 99, "0".toCharArray()));
			groups.add(new PasswordGeneratorCharacterGroup(1, 99, "!".toCharArray()));
			
			String password = new String(PasswordGenerator.generatePassword(groups, 4, 4));
			Assert.assertTrue(password.length() == 4);
			Assert.assertTrue(password.contains("a"));
			Assert.assertTrue(password.contains("A"));
			Assert.assertTrue(password.contains("0"));
			Assert.assertTrue(password.contains("!"));
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}
}
