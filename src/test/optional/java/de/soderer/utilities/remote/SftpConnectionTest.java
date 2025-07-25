package de.soderer.utilities.remote;

import java.io.File;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import de.soderer.utilities.FileUtilities;
import de.soderer.utilities.worker.DirectoryDownloadWorker;
import de.soderer.utilities.worker.DirectoryUploadWorker;
import de.soderer.utilities.worker.RemoteFileDownloadWorker;
import de.soderer.utilities.worker.RemoteFileUploadWorker;
import de.soderer.utilities.zip.ZipUtilities;

public class SftpConnectionTest {
	private static final File localTestDirectory = new File(System.getProperty("user.home") + "\\Downloads\\test");
	private static final File localTestDirectory2 = new File(System.getProperty("user.home") + "\\Downloads\\test2");

	private RemoteConnection getConnection() throws Exception {
		final byte[] keyFileData = FileUtilities.readFileToByteArray(new File(System.getenv().get("SFTP_KEYFILE")));
		return new SftpConnection(System.getenv().get("SFTP_HOSTNAME"), System.getenv().get("SFTP_USERNAME"), keyFileData, System.getenv().get("SFTP_PASSWORD").toCharArray(), 22, System.getenv().get("SFTP_HOSTKEY"));
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
	public void directoryRoundTrip() {
		try (RemoteConnection connection = getConnection()) {
			if (connection.getFolderInfo("/home/andreas/test") != null) {
				connection.deleteFolder("/home/andreas/test");
			}
			try {
				final File testFolderZipFile = new File(getClass().getClassLoader().getResource("TestFolder.zip").getFile());
				ZipUtilities.unzipFile(testFolderZipFile, localTestDirectory);
				new DirectoryUploadWorker(null, connection, localTestDirectory, "/home/andreas", "test").work();
				final FolderInfo remoteFolderInfo = connection.getFolderInfo("/home/andreas/test");
				Assert.assertEquals("test", remoteFolderInfo.getFolderName());
				new DirectoryDownloadWorker(null, connection, "/home/andreas/test", localTestDirectory2.getParentFile(), "test2").work();
				Assert.assertTrue("Resulting directories differ", FileUtilities.directoriesAreEqual(localTestDirectory, localTestDirectory2));
			} catch (final Exception e) {
				e.printStackTrace();
				Assert.fail(e.getMessage());
			} finally {
				if (connection.getFolderInfo("/home/andreas/test") != null) {
					connection.deleteFolder("/home/andreas/test");
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
			if (connection.getFolderInfo("/home/andreas/test") != null) {
				connection.deleteFolder("/home/andreas/test");
			}
			connection.createFolder("/home/andreas", "test");
			localTestDirectory2.mkdir();
			try {
				final File testFolderZipFile = new File(getClass().getClassLoader().getResource("TestFolder.zip").getFile());
				ZipUtilities.unzipFile(testFolderZipFile, localTestDirectory);
				new RemoteFileUploadWorker(null, connection, new File(localTestDirectory, "TextFileWithUmlautÄ.txt"), "/home/andreas/test/TextFile With Umlaut Ä.txt").work();
				final FileInfo remoteFileInfo = connection.getFileInfo("/home/andreas/test/TextFile With Umlaut Ä.txt");
				Assert.assertEquals("TextFile With Umlaut Ä.txt", remoteFileInfo.getFileName());
				new RemoteFileDownloadWorker(null, connection, "/home/andreas/test/TextFile With Umlaut Ä.txt", new File(localTestDirectory2, "TextFileWithUmlautÄ.txt")).work();
				Assert.assertTrue("Resulting files differ", FileUtilities.filesAreEqual(new File(localTestDirectory, "TextFileWithUmlautÄ.txt"), new File(localTestDirectory2, "TextFileWithUmlautÄ.txt")));
			} catch (final Exception e) {
				e.printStackTrace();
				Assert.fail(e.getMessage());
			} finally {
				if (connection.getFolderInfo("/home/andreas/test") != null) {
					connection.deleteFolder("/home/andreas/test");
				}
			}
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}
}
