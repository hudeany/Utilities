package de.soderer.utilities;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.Set;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.Assert;
import org.junit.Test;

public class SecureDataStoreTest {
	public static final String TEST_PASSWORD = "test@PasswordÄÖÜäöüß";
	public static final String TEST_SALT = "testSalt";
	
	public static final String TEST_ENTRYNAME = "entry Näme";
	
	@Test
	public void test() throws Exception {
		File testStoreFile = File.createTempFile("test", ".secpref");
		try {
			Date now = DateUtils.truncate(new Date(), Calendar.SECOND);
			SecureDataStore testKeyStore_ForSave = new SecureDataStore();
			TestSecureDataStoreEntry secureDataEntry = new TestSecureDataStoreEntry();
			secureDataEntry.valueString = "valueString";
			secureDataEntry.valueInt = 5;
			secureDataEntry.valueDate = now;
			secureDataEntry.valueBoolean = true;
			testKeyStore_ForSave.addEntry(TEST_ENTRYNAME, secureDataEntry);
			testKeyStore_ForSave.save(testStoreFile, TEST_PASSWORD.toCharArray(), TEST_SALT.getBytes("UTF-8"));
			
			SecureDataStore testKeyStore_ForLoad = new SecureDataStore();
			testKeyStore_ForLoad.load(testStoreFile, TEST_PASSWORD.toCharArray(), TEST_SALT.getBytes("UTF-8"));

			Set<String> loadedSecureDataEntryNames = testKeyStore_ForLoad.getEntryNames(TestSecureDataStoreEntry.class);
			Assert.assertEquals(1, loadedSecureDataEntryNames.size());
			Assert.assertTrue(loadedSecureDataEntryNames.contains(TEST_ENTRYNAME));
			
			TestSecureDataStoreEntry loadedSecureDataEntry = testKeyStore_ForLoad.getEntry(TestSecureDataStoreEntry.class, TEST_ENTRYNAME);
			Assert.assertNotNull(loadedSecureDataEntry);
			Assert.assertEquals("valueString", loadedSecureDataEntry.valueString);
			Assert.assertEquals(5, loadedSecureDataEntry.valueInt);
			Assert.assertEquals(now, loadedSecureDataEntry.valueDate);
			Assert.assertEquals(true, loadedSecureDataEntry.valueBoolean);
		
			try {
				new SecureDataStore().load(testStoreFile, "abc".toCharArray(), TEST_SALT.getBytes("UTF-8"));
				Assert.fail("Missing expected exception");
			} catch (WrongPasswordException e) {
				Assert.assertNull(e.getMessage());
			} catch (Exception e) {
				Assert.fail("Other than expected exception");
			}
		} finally {
			if (testStoreFile.exists()) {
				testStoreFile.delete();
			}
		}
	}
}
