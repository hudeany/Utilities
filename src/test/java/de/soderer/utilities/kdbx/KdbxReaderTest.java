package de.soderer.utilities.kdbx;

import org.junit.Assert;
import org.junit.Test;

import de.soderer.utilities.IoUtilities;
import de.soderer.utilities.kdbx.data.KdbxUUID;

public class KdbxReaderTest {
	@Test
	public void test_AES256_AESKDF_GZIP() {
		try (KdbxReader kdbxReader = new KdbxReader(getClass().getClassLoader().getResourceAsStream("kdbx/Database_AES256_AES-KDF.kdbx"))) {
			final KdbxDatabase database = kdbxReader.readKdbxDatabase("Äbc123@".toCharArray());
			Assert.assertEquals("Test Database", database.getMeta().getDatabaseName());

			Assert.assertEquals(1, database.getGroups().size());
			Assert.assertEquals("Database", database.getGroups().get(0).getName());

			Assert.assertEquals(6, database.getGroups().get(0).getGroups().size());
			Assert.assertEquals("General", database.getGroups().get(0).getGroups().get(0).getName());

			Assert.assertEquals(2, database.getAllEntries().size());
			Assert.assertEquals("Test Password", database.getEntryByUUID(KdbxUUID.fromHex("957C944A05D8E9489787D94EF07C8319")).getPassword());
			Assert.assertEquals("Test Pässword #2", database.getEntryByUUID(KdbxUUID.fromHex("FE30E9479289424F81439234970F59AA")).getPassword());
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void test_ChaCha20_Argon2d_GZIP() {
		try (KdbxReader kdbxReader = new KdbxReader(getClass().getClassLoader().getResourceAsStream("kdbx/Database_ChaCha20_Argon2d.kdbx"))) {
			final KdbxDatabase database = kdbxReader.readKdbxDatabase("Äbc123@".toCharArray());
			Assert.assertEquals("Test Database", database.getMeta().getDatabaseName());

			Assert.assertEquals(1, database.getGroups().size());
			Assert.assertEquals("Database", database.getGroups().get(0).getName());

			Assert.assertEquals(6, database.getGroups().get(0).getGroups().size());
			Assert.assertEquals("General", database.getGroups().get(0).getGroups().get(0).getName());

			Assert.assertEquals(2, database.getAllEntries().size());
			Assert.assertEquals("Test Password", database.getEntryByUUID(KdbxUUID.fromHex("957C944A05D8E9489787D94EF07C8319")).getPassword());
			Assert.assertEquals("Test Pässword #2", database.getEntryByUUID(KdbxUUID.fromHex("FE30E9479289424F81439234970F59AA")).getPassword());
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void test_ChaCha20_Argon2id_GZIP() {
		try (KdbxReader kdbxReader = new KdbxReader(getClass().getClassLoader().getResourceAsStream("kdbx/Database_ChaCha20_Argon2id.kdbx"))) {
			final KdbxDatabase database = kdbxReader.readKdbxDatabase("Äbc123@".toCharArray());
			Assert.assertEquals("Test Database", database.getMeta().getDatabaseName());

			Assert.assertEquals(1, database.getGroups().size());
			Assert.assertEquals("Database", database.getGroups().get(0).getName());

			Assert.assertEquals(6, database.getGroups().get(0).getGroups().size());
			Assert.assertEquals("General", database.getGroups().get(0).getGroups().get(0).getName());

			Assert.assertEquals(2, database.getAllEntries().size());
			Assert.assertEquals("Test Password", database.getEntryByUUID(KdbxUUID.fromHex("957C944A05D8E9489787D94EF07C8319")).getPassword());
			Assert.assertEquals("Test Pässword #2", database.getEntryByUUID(KdbxUUID.fromHex("FE30E9479289424F81439234970F59AA")).getPassword());
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}
	@Test
	public void test_AES256_AESKDF_NoZIP() {
		try (KdbxReader kdbxReader = new KdbxReader(getClass().getClassLoader().getResourceAsStream("kdbx/Database_AES256_AES-KDF_NoZip.kdbx"))) {
			final KdbxDatabase database = kdbxReader.readKdbxDatabase("Äbc123@".toCharArray());
			Assert.assertEquals("Test Database", database.getMeta().getDatabaseName());

			Assert.assertEquals(1, database.getGroups().size());
			Assert.assertEquals("Database", database.getGroups().get(0).getName());

			Assert.assertEquals(6, database.getGroups().get(0).getGroups().size());
			Assert.assertEquals("General", database.getGroups().get(0).getGroups().get(0).getName());

			Assert.assertEquals(2, database.getAllEntries().size());
			Assert.assertEquals("Test Password", database.getEntryByUUID(KdbxUUID.fromHex("957C944A05D8E9489787D94EF07C8319")).getPassword());
			Assert.assertEquals("Test Pässword #2", database.getEntryByUUID(KdbxUUID.fromHex("FE30E9479289424F81439234970F59AA")).getPassword());
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void test_AES256_AESKDF_PwdAndTxtKeyFile_GZIP() {
		try (KdbxReader kdbxReader = new KdbxReader(getClass().getClassLoader().getResourceAsStream("kdbx/Database_AES256_AES-KDF_PwdTxtKeyFile.kdbx"))) {
			final byte[] keyFileData = IoUtilities.toByteArray(getClass().getClassLoader().getResourceAsStream("kdbx/Database_AES256_AES-KDF_PwdTxtKeyFile.txt"));
			final KdbxCredentials credentials = new KdbxCredentials("Äbc123@".toCharArray(), keyFileData);
			final KdbxDatabase database = kdbxReader.readKdbxDatabase(credentials);
			Assert.assertEquals("Test Database", database.getMeta().getDatabaseName());

			Assert.assertEquals(1, database.getGroups().size());
			Assert.assertEquals("Database", database.getGroups().get(0).getName());

			Assert.assertEquals(6, database.getGroups().get(0).getGroups().size());
			Assert.assertEquals("General", database.getGroups().get(0).getGroups().get(0).getName());

			Assert.assertEquals(2, database.getAllEntries().size());
			Assert.assertEquals("Test Password", database.getEntryByUUID(KdbxUUID.fromHex("957C944A05D8E9489787D94EF07C8319")).getPassword());
			Assert.assertEquals("Test Pässword #2", database.getEntryByUUID(KdbxUUID.fromHex("FE30E9479289424F81439234970F59AA")).getPassword());
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void test_AES256_AESKDF_PwdAndKeyFileV1_GZIP() {
		try (KdbxReader kdbxReader = new KdbxReader(getClass().getClassLoader().getResourceAsStream("kdbx/Database_AES256_AES-KDF_PwdKeyFileV1.kdbx"))) {
			final byte[] keyFileData = IoUtilities.toByteArray(getClass().getClassLoader().getResourceAsStream("kdbx/Database_AES256_AES-KDF_PwdKeyFileV1.keyx"));
			final KdbxCredentials credentials = new KdbxCredentials("Äbc123@".toCharArray(), keyFileData);
			final KdbxDatabase database = kdbxReader.readKdbxDatabase(credentials);
			Assert.assertEquals("Test Database", database.getMeta().getDatabaseName());

			Assert.assertEquals(1, database.getGroups().size());
			Assert.assertEquals("Database", database.getGroups().get(0).getName());

			Assert.assertEquals(6, database.getGroups().get(0).getGroups().size());
			Assert.assertEquals("General", database.getGroups().get(0).getGroups().get(0).getName());

			Assert.assertEquals(2, database.getAllEntries().size());
			Assert.assertEquals("Test Password", database.getEntryByUUID(KdbxUUID.fromHex("957C944A05D8E9489787D94EF07C8319")).getPassword());
			Assert.assertEquals("Test Pässword #2", database.getEntryByUUID(KdbxUUID.fromHex("FE30E9479289424F81439234970F59AA")).getPassword());
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void test_AES256_AESKDF_PwdAndKeyFileV2_GZIP() {
		try (KdbxReader kdbxReader = new KdbxReader(getClass().getClassLoader().getResourceAsStream("kdbx/Database_AES256_AES-KDF_PwdKeyFileV2.kdbx"))) {
			final byte[] keyFileData = IoUtilities.toByteArray(getClass().getClassLoader().getResourceAsStream("kdbx/Database_AES256_AES-KDF_PwdKeyFileV2.keyx"));
			final KdbxCredentials credentials = new KdbxCredentials("Äbc123@".toCharArray(), keyFileData);
			final KdbxDatabase database = kdbxReader.readKdbxDatabase(credentials);
			Assert.assertEquals("Test Database", database.getMeta().getDatabaseName());

			Assert.assertEquals(1, database.getGroups().size());
			Assert.assertEquals("Database", database.getGroups().get(0).getName());

			Assert.assertEquals(6, database.getGroups().get(0).getGroups().size());
			Assert.assertEquals("General", database.getGroups().get(0).getGroups().get(0).getName());

			Assert.assertEquals(2, database.getAllEntries().size());
			Assert.assertEquals("Test Password", database.getEntryByUUID(KdbxUUID.fromHex("957C944A05D8E9489787D94EF07C8319")).getPassword());
			Assert.assertEquals("Test Pässword #2", database.getEntryByUUID(KdbxUUID.fromHex("FE30E9479289424F81439234970F59AA")).getPassword());
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void test_AES256_AESKDF_KeyFile() {
		try (KdbxReader kdbxReader = new KdbxReader(getClass().getClassLoader().getResourceAsStream("kdbx/Database_AES256_AES-KDF_KeyFile.kdbx"))) {
			final byte[] keyFileData = IoUtilities.toByteArray(getClass().getClassLoader().getResourceAsStream("kdbx/Database_AES256_AES-KDF_KeyFile.keyx"));
			final KdbxCredentials credentials = new KdbxCredentials(keyFileData);
			final KdbxDatabase database = kdbxReader.readKdbxDatabase(credentials);
			Assert.assertEquals("Test Database", database.getMeta().getDatabaseName());

			Assert.assertEquals(1, database.getGroups().size());
			Assert.assertEquals("Database", database.getGroups().get(0).getName());

			Assert.assertEquals(6, database.getGroups().get(0).getGroups().size());
			Assert.assertEquals("General", database.getGroups().get(0).getGroups().get(0).getName());

			Assert.assertEquals(2, database.getAllEntries().size());
			Assert.assertEquals("Test Password", database.getEntryByUUID(KdbxUUID.fromHex("957C944A05D8E9489787D94EF07C8319")).getPassword());
			Assert.assertEquals("Test Pässword #2", database.getEntryByUUID(KdbxUUID.fromHex("FE30E9479289424F81439234970F59AA")).getPassword());
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}
}
