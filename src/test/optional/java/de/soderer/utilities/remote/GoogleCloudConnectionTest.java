package de.soderer.utilities.remote;

import java.io.File;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import de.soderer.utilities.FileUtilities;
import de.soderer.utilities.Tuple;
import de.soderer.utilities.googlecloud.GoogleCloudConnection;
import de.soderer.utilities.swt.worker.DirectoryDownloadWorker;
import de.soderer.utilities.swt.worker.DirectoryUploadWorker;
import de.soderer.utilities.swt.worker.RemoteFileDownloadWorker;
import de.soderer.utilities.swt.worker.RemoteFileUploadWorker;
import de.soderer.utilities.zip.ZipUtilities;

public class GoogleCloudConnectionTest {
	public static String applicationCredentialsJsonString =
			"{\n"
					+ "	\"private_key\": \"-----BEGIN PRIVATE KEY-----\nxxx\n-----END PRIVATE KEY-----\n\",\n"
					+ "	\"client_email\": \"xxx\",\n"
					+ "}\n";

	private static final File localTestDirectory = new File(System.getProperty("user.home") + "\\Downloads\\test");
	private static final File localTestDirectory2 = new File(System.getProperty("user.home") + "\\Downloads\\test2");

	private RemoteConnection getConnection() throws Exception {
		return new GoogleCloudConnection(applicationCredentialsJsonString);
	}

	@Before
	public void setup() throws Exception {
		if (localTestDirectory.exists()) {
			FileUtilities.delete(localTestDirectory);
		}
		localTestDirectory.mkdir();

		if (localTestDirectory2.exists()) {
			FileUtilities.delete(localTestDirectory2);
		}
	}

	@After
	public void tearDown() throws Exception {
		if (localTestDirectory.exists()) {
			FileUtilities.delete(localTestDirectory);
		}
		if (localTestDirectory2.exists()) {
			FileUtilities.delete(localTestDirectory2);
		}
	}

	@Test
	public void testFolderInfo() {
		try (RemoteConnection connection = getConnection()) {
			final FolderInfo remoteFolderInfo = connection.getFolderInfo("/statistiken/provider-stats");
			Assert.assertEquals("provider-stats", remoteFolderInfo.getFolderName());
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void directoryRoundTrip() {
		try (RemoteConnection connection = getConnection()) {
			if (connection.getFolderInfo("/statistiken/provider-stats/test") != null) {
				connection.deleteFolder("/statistiken/provider-stats/test");
			}

			final Tuple<List<FolderInfo>, List<FileInfo>> preTestDirectory = connection.getDirectory("/statistiken/provider-stats/test");
			Assert.assertNull(preTestDirectory);

			try {
				final File testFolderZipFile = new File(getClass().getClassLoader().getResource("TestFolder.zip").getFile());
				ZipUtilities.unzipFile(testFolderZipFile, localTestDirectory);
				new DirectoryUploadWorker(null, connection, localTestDirectory, "/statistiken/provider-stats", "test").work();
				final FolderInfo remoteFolderInfo = connection.getFolderInfo("/statistiken/provider-stats/test");
				Assert.assertEquals("test", remoteFolderInfo.getFolderName());
				new DirectoryDownloadWorker(null, connection, "/statistiken/provider-stats/test", localTestDirectory2.getParentFile(), "test2").work();
				Assert.assertTrue("Resulting directories differ", FileUtilities.directoriesAreEqual(localTestDirectory, localTestDirectory2));

				final Tuple<List<FolderInfo>, List<FileInfo>> testDirectory = connection.getDirectory("/statistiken/provider-stats/test");
				Assert.assertNotNull(testDirectory);
				Assert.assertEquals(4, testDirectory.getFirst().size());
				Assert.assertEquals(6, testDirectory.getSecond().size());
			} catch (final Exception e) {
				e.printStackTrace();
				Assert.fail(e.getMessage());
			} finally {
				if (connection.getFolderInfo("/statistiken/provider-stats/test") != null) {
					connection.deleteFolder("/statistiken/provider-stats/test");
				}
			}
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void fileRoundTrip() {
		try (RemoteConnection connection = getConnection()) {
			if (connection.getFolderInfo("/statistiken/provider-stats/test") != null) {
				connection.deleteFolder("/statistiken/provider-stats/test");
			}

			final Tuple<List<FolderInfo>, List<FileInfo>> preTestDirectory = connection.getDirectory("/statistiken/provider-stats/test");
			Assert.assertNull(preTestDirectory);

			connection.createFolder("/statistiken/provider-stats", "test");
			localTestDirectory2.mkdir();
			try {
				final File testFolderZipFile = new File(getClass().getClassLoader().getResource("TestFolder.zip").getFile());
				ZipUtilities.unzipFile(testFolderZipFile, localTestDirectory);
				new RemoteFileUploadWorker(null, connection, new File(localTestDirectory, "TextFileWithUmlautÄ.txt"), "/statistiken/provider-stats/test/TextFile With Umlaut Ä.txt").work();
				final FileInfo remoteFileInfo = connection.getFileInfo("/statistiken/provider-stats/test/TextFile With Umlaut Ä.txt");
				Assert.assertEquals("TextFile With Umlaut Ä.txt", remoteFileInfo.getFileName());
				new RemoteFileDownloadWorker(null, connection, "/statistiken/provider-stats/test/TextFile With Umlaut Ä.txt", new File(localTestDirectory2, "TextFileWithUmlautÄ.txt")).work();
				Assert.assertTrue("Resulting files differ", FileUtilities.filesAreEqual(new File(localTestDirectory, "TextFileWithUmlautÄ.txt"), new File(localTestDirectory2, "TextFileWithUmlautÄ.txt")));

				final Tuple<List<FolderInfo>, List<FileInfo>> testDirectory = connection.getDirectory("/statistiken/provider-stats/test");
				Assert.assertNotNull(testDirectory);
				Assert.assertEquals(0, testDirectory.getFirst().size());
				Assert.assertEquals(1, testDirectory.getSecond().size());
			} catch (final Exception e) {
				e.printStackTrace();
				Assert.fail(e.getMessage());
			} finally {
				if (connection.getFolderInfo("/statistiken/provider-stats/test") != null) {
					connection.deleteFolder("/statistiken/provider-stats/test");
				}
			}
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testRootFolderInfo() {
		try (RemoteConnection connection = getConnection()) {
			final FolderInfo remoteFolderInfo = connection.getFolderInfo("/statistiken/");
			Assert.assertEquals("statistiken", remoteFolderInfo.getFolderName());
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}
}
