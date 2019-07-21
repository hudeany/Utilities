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

public class Smb1ConnectionTest {
	private static final File localTestDirectory = new File("C:\\Users\\Andreas\\Downloads\\test");
	private static final File localTestDirectory2 = new File("C:\\Users\\Andreas\\Downloads\\test2");

	private RemoteConnection getConnection() throws Exception {
		return new Smb1Connection(System.getenv().get("SMB1_HOSTNAME"), System.getenv().get("SMB1_USERNAME"), System.getenv().get("SMB1_SHARENAME"), System.getenv().get("SMB1_PASSWORD").toCharArray());
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
			if (connection.folderExists("/home/andreas/test")) {
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
				if (connection.folderExists("/home/andreas/test")) {
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
			if (connection.folderExists("/home/andreas/test")) {
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
				if (connection.folderExists("/home/andreas/test")) {
					connection.deleteFolder("/home/andreas/test");
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

	@Test
	public void testRootFolderDownload() {
		try (RemoteConnection connection = getConnection()) {
			new DirectoryDownloadWorker(null, connection, "/", localTestDirectory).work();
			Assert.assertEquals(2, localTestDirectory.listFiles().length);
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}
}
