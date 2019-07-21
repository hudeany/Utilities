package de.soderer.utilities.remote;

import java.io.File;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import de.soderer.utilities.FileUtilities;
import de.soderer.utilities.swt.worker.DirectoryDownloadWorker;
import de.soderer.utilities.swt.worker.RemoteFileDownloadWorker;

public class SmbConnectionTest {
	private static final File localTestDirectory = new File("C:\\Users\\Andreas\\Downloads\\test");

	private RemoteConnection getConnection() throws Exception {
		return new SmbConnection(System.getenv().get("SMB_HOSTNAME"), 445, System.getenv().get("SMB_USERNAME"), System.getenv().get("SMB_SHARENAME"), System.getenv().get("SMB_PASSWORD").toCharArray());
	}

	@Before
	public void setup() throws Exception {
		if (localTestDirectory.exists()) {
			FileUtilities.delete(localTestDirectory);
		}
		localTestDirectory.mkdir();
	}

	@After
	public void tearDown() throws Exception {
		if (localTestDirectory.exists()) {
			FileUtilities.delete(localTestDirectory);
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
	public void testFolderInfo() {
		try (RemoteConnection connection = getConnection()) {
			final FolderInfo remoteFolderInfo = connection.getFolderInfo("/TestFolder");
			Assert.assertEquals("TestFolder", remoteFolderInfo.getFolderName());
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testFileInfo() {
		try (RemoteConnection connection = getConnection()) {
			final FileInfo remoteFileInfo = connection.getFileInfo("/test.txt");
			Assert.assertEquals("test.txt", remoteFileInfo.getFileName());
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

	@Test
	public void testFolderDownload() {
		try (RemoteConnection connection = getConnection()) {
			new DirectoryDownloadWorker(null, connection, "/TestFolder", localTestDirectory).work();
			Assert.assertEquals("TestFolder", localTestDirectory.listFiles()[0].getName());
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testFileDownload() {
		try (RemoteConnection connection = getConnection()) {
			new RemoteFileDownloadWorker(null, connection, "/TestFolder/testFile1.txt", new File(localTestDirectory.getAbsolutePath(), "testFile1.txt")).work();
			Assert.assertEquals("testFile1.txt", localTestDirectory.listFiles()[0].getName());
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}
}
