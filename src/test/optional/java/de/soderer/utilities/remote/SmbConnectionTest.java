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

public class SmbConnectionTest {
	private static final File localTestDirectory = new File(System.getProperty("user.home") + "\\Downloads\\test");
	private static final File localTestDirectory2 = new File(System.getProperty("user.home") + "\\Downloads\\test2");

	private RemoteConnection getConnection() throws Exception {
		return new SmbConnection(System.getenv().get("SMB_HOSTNAME"), 445, System.getenv().get("SMB_USERNAME"), System.getenv().get("SMB_SHARENAME"), System.getenv().get("SMB_PASSWORD").toCharArray());
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
			if (connection.getFolderInfo("/test") != null) {
				connection.deleteFolder("/test");
			}
			try {
				final File testFolderZipFile = new File(getClass().getClassLoader().getResource("TestFolder.zip").getFile());
				ZipUtilities.unzipFile(testFolderZipFile, localTestDirectory);
				new DirectoryUploadWorker(null, connection, localTestDirectory, "/", "test").work();

				final FolderInfo remoteFolderInfo = connection.getFolderInfo("/test");
				Assert.assertEquals("test", remoteFolderInfo.getFolderName());

				final FolderInfo rootFolderInfo = connection.getFolderInfo("/");
				Assert.assertEquals("/", rootFolderInfo.getFolderName());

				new DirectoryDownloadWorker(null, connection, "/test", localTestDirectory2.getParentFile(), "test2").work();
				Assert.assertTrue("Resulting directories differ", FileUtilities.directoriesAreEqual(localTestDirectory, localTestDirectory2));
			} catch (final Exception e) {
				e.printStackTrace();
				Assert.fail(e.getMessage());
			} finally {
				if (connection.getFolderInfo("/test") != null) {
					connection.deleteFolder("/test");
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
			if (connection.getFolderInfo("/test") != null) {
				connection.deleteFolder("/test");
			}
			connection.createFolder("/", "test");
			localTestDirectory2.mkdir();
			try {
				final File testFolderZipFile = new File(getClass().getClassLoader().getResource("TestFolder.zip").getFile());
				ZipUtilities.unzipFile(testFolderZipFile, localTestDirectory);
				new RemoteFileUploadWorker(null, connection, new File(localTestDirectory, "TextFileWithUmlautÄ.txt"), "/test/TextFile With Umlaut Ä.txt").work();
				final FileInfo remoteFileInfo = connection.getFileInfo("/test/TextFile With Umlaut Ä.txt");
				Assert.assertEquals("TextFile With Umlaut Ä.txt", remoteFileInfo.getFileName());
				new RemoteFileDownloadWorker(null, connection, "/test/TextFile With Umlaut Ä.txt", new File(localTestDirectory2, "TextFileWithUmlautÄ.txt")).work();
				Assert.assertTrue("Resulting files differ", FileUtilities.filesAreEqual(new File(localTestDirectory, "TextFileWithUmlautÄ.txt"), new File(localTestDirectory2, "TextFileWithUmlautÄ.txt")));
			} catch (final Exception e) {
				e.printStackTrace();
				Assert.fail(e.getMessage());
			} finally {
				if (connection.getFolderInfo("/test") != null) {
					connection.deleteFolder("/test");
				}
			}
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}
}
