package de.soderer.utilities.remote;

import java.io.File;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import de.soderer.utilities.FileUtilities;
import de.soderer.utilities.swt.worker.DirectoryDownloadWorker;
import de.soderer.utilities.swt.worker.DirectoryUploadWorker;
import de.soderer.utilities.swt.worker.RemoteFileDownloadWorker;
import de.soderer.utilities.swt.worker.RemoteFileUploadWorker;
import de.soderer.utilities.zip.ZipUtilities;

public class FtpConnectionTest {
	private static final File localTestDirectory = new File(System.getProperty("user.home") + "\\Downloads\\test");
	private static final File localTestDirectory2 = new File(System.getProperty("user.home") + "\\Downloads\\test2");

	private RemoteConnection getConnection() throws Exception {
		return new FtpConnection(System.getenv().get("FTP_HOSTNAME"), System.getenv().get("FTP_USERNAME"), System.getenv().get("FTP_PASSWORD").toCharArray());
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
			final FolderInfo remoteFolderInfo = connection.getFolderInfo("/soderer.de/files/download");
			Assert.assertEquals("download", remoteFolderInfo.getFolderName());
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void directoryRoundTrip() {
		try (RemoteConnection connection = getConnection()) {
			if (connection.getFolderInfo("/soderer.de/files/download/test") != null) {
				connection.deleteFolder("/soderer.de/files/download/test");
			}
			try {
				final File testFolderZipFile = new File(getClass().getClassLoader().getResource("TestFolder.zip").getFile());
				ZipUtilities.unzipFile(testFolderZipFile, localTestDirectory);
				new DirectoryUploadWorker(null, connection, localTestDirectory, "/soderer.de/files/download", "test").work();
				final FolderInfo remoteFolderInfo = connection.getFolderInfo("/soderer.de/files/download/test");
				Assert.assertEquals("test", remoteFolderInfo.getFolderName());
				new DirectoryDownloadWorker(null, connection, "/soderer.de/files/download/test", localTestDirectory2.getParentFile(), "test2").work();
				Assert.assertTrue("Resulting directories differ", FileUtilities.directoriesAreEqual(localTestDirectory, localTestDirectory2));
			} catch (final Exception e) {
				e.printStackTrace();
				Assert.fail(e.getMessage());
			} finally {
				if (connection.getFolderInfo("/soderer.de/files/download/test") != null) {
					connection.deleteFolder("/soderer.de/files/download/test");
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
			if (connection.getFolderInfo("/soderer.de/files/download") != null) {
				connection.deleteFolder("/soderer.de/files/download/test");
			}
			connection.createFolder("/soderer.de/files/download", "test");
			localTestDirectory2.mkdir();
			try {
				final File testFolderZipFile = new File(getClass().getClassLoader().getResource("TestFolder.zip").getFile());
				ZipUtilities.unzipFile(testFolderZipFile, localTestDirectory);
				new RemoteFileUploadWorker(null, connection, new File(localTestDirectory, "TextFileWithUmlautÄ.txt"), "/soderer.de/files/download/test/TextFile With Umlaut Ä.txt").work();
				final FileInfo remoteFileInfo = connection.getFileInfo("/soderer.de/files/download/test/TextFile With Umlaut Ä.txt");
				Assert.assertEquals("TextFile With Umlaut Ä.txt", remoteFileInfo.getFileName());
				new RemoteFileDownloadWorker(null, connection, "/soderer.de/files/download/test/TextFile With Umlaut Ä.txt", new File(localTestDirectory2, "TextFileWithUmlautÄ.txt")).work();
				Assert.assertTrue("Resulting files differ", FileUtilities.filesAreEqual(new File(localTestDirectory, "TextFileWithUmlautÄ.txt"), new File(localTestDirectory2, "TextFileWithUmlautÄ.txt")));
			} catch (final Exception e) {
				e.printStackTrace();
				Assert.fail(e.getMessage());
			} finally {
				if (connection.getFolderInfo("/soderer.de/files/download/test") != null) {
					connection.deleteFolder("/soderer.de/files/download/test");
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
			final FolderInfo remoteFolderInfo = connection.getFolderInfo("/");
			Assert.assertEquals("/", remoteFolderInfo.getFolderName());
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}
}
