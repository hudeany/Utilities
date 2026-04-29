package de.soderer.utilities;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

@SuppressWarnings("static-method")
public class ConfigurationPropertiesTest {
	@Test
	public void test() {
		final File configFile = new File("C:\\Users\\Andreas\\.jUnitTestApp.config");
		final String backslashTestValue = "\\some\\real\\good\\test\\with\\backslashes";
		try {
			if (configFile.exists()) {
				configFile.delete();
			}
			final ConfigurationProperties testConfigurationProperties = new ConfigurationProperties("jUnitTestApp");
			testConfigurationProperties.set("myTestValue1", backslashTestValue);
			testConfigurationProperties.set("myTestValue2", "__;/src/test/;\\src\\test\\;/bin/;\\bin\\");
			testConfigurationProperties.save();

			final ConfigurationProperties testConfigurationProperties_Reloaded = new ConfigurationProperties("jUnitTestApp");
			Assert.assertEquals(backslashTestValue, testConfigurationProperties_Reloaded.get("myTestValue1"));
			Assert.assertEquals("__;/src/test/;\\src\\test\\;/bin/;\\bin\\", testConfigurationProperties_Reloaded.get("myTestValue2"));
		} catch (final Exception e) {
			e.printStackTrace();
		} finally {
			if (configFile.exists()) {
				configFile.delete();
			}
		}
	}

	@Test
	public void testList() {
		final File configFile = new File("C:\\Users\\Andreas\\.jUnitTestApp.config");
		final String backslashTestValue = "\\normally\\some\\real\\good\\test\\with\\backslashes";
		try {
			if (configFile.exists()) {
				configFile.delete();
			}
			final ConfigurationProperties testConfigurationProperties = new ConfigurationProperties("jUnitTestApp");
			final List<String> valuesList = new ArrayList<>();
			valuesList.add(backslashTestValue);
			valuesList.add(backslashTestValue);
			testConfigurationProperties.set("myTestValueList", valuesList);
			testConfigurationProperties.save();

			final ConfigurationProperties testConfigurationProperties_Reloaded = new ConfigurationProperties("jUnitTestApp");
			final List<String> reloadedList = testConfigurationProperties_Reloaded.getList("myTestValueList");
			Assert.assertEquals(2, reloadedList.size());
			Assert.assertEquals(backslashTestValue, reloadedList.get(0));
			Assert.assertEquals(backslashTestValue, reloadedList.get(1));
		} catch (final Exception e) {
			e.printStackTrace();
		} finally {
			if (configFile.exists()) {
				configFile.delete();
			}
		}
	}
}
