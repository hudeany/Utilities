package de.soderer.utilities.remote;

import java.io.File;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import de.soderer.utilities.FileUtilities;
import de.soderer.utilities.swt.worker.DirectoryDownloadWorker;
import de.soderer.utilities.swt.worker.RemoteFileDownloadWorker;

public class FtpConnectionTest {
	private static final File localTestDirectory = new File("C:\\Users\\Andreas\\Downloads\\test");

	private RemoteConnection getConnection() throws Exception {
		return new FtpConnection(System.getenv().get("FTP_HOSTNAME"), System.getenv().get("FTP_USERNAME"), System.getenv().get("FTP_PASSWORD").toCharArray());
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
			final FolderInfo remoteFolderInfo = connection.getFolderInfo("/soderer.de/files/download");
			Assert.assertEquals("download", remoteFolderInfo.getFolderName());
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testFileInfo() {
		try (RemoteConnection connection = getConnection()) {
			final FileInfo remoteFileInfo = connection.getFileInfo("/soderer.de/files/download/certificates/andreas_at_soderer.de.crt");
			Assert.assertEquals("andreas_at_soderer.de.crt", remoteFileInfo.getFileName());
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testFolderDownload() {
		try (RemoteConnection connection = getConnection()) {
			new DirectoryDownloadWorker(null, connection, "/soderer.de/files/download/certificates", localTestDirectory).work();
			Assert.assertEquals("certificates", localTestDirectory.listFiles()[0].getName());
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testFileDownload() {
		try (RemoteConnection connection = getConnection()) {
			new RemoteFileDownloadWorker(null, connection, "/soderer.de/files/download/certificates/andreas_at_soderer.de.crt", new File(localTestDirectory.getAbsolutePath(), "testFile1.txt")).work();
			Assert.assertEquals("testFile1.txt", localTestDirectory.listFiles()[0].getName());
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}
}
