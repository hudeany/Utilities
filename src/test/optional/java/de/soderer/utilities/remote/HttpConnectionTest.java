package de.soderer.utilities.remote;

import java.io.File;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import de.soderer.utilities.FileUtilities;
import de.soderer.utilities.swt.worker.DirectoryDownloadWorker;
import de.soderer.utilities.swt.worker.RemoteFileDownloadWorker;

public class HttpConnectionTest {
	private static final File localTestDirectory = new File("C:\\Users\\Andreas\\Downloads\\test");

	private RemoteConnection getConnection() throws Exception {
		return new HttpFileConnection(true, "www-us.apache.org", "dist", null, null, 443);
	}

	@Before
	public void setup() throws Exception {
		if (localTestDirectory.exists()) {
			FileUtilities.delete(localTestDirectory);
		}
	}

	@After
	public void tearDown() throws Exception {
		if (localTestDirectory.exists()) {
			FileUtilities.delete(localTestDirectory);
		}
	}

	@Test
	public void directoryRoundTrip() {
		try (RemoteConnection connection = getConnection()) {
			new DirectoryDownloadWorker(null, connection, "/tomcat/tomcat-9/v9.0.43/bin", localTestDirectory.getParentFile(), "test").work();
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void fileRoundTrip() {
		try (RemoteConnection connection = getConnection()) {
			new RemoteFileDownloadWorker(null, connection, "/tomcat/tomcat-9/v9.0.43/bin/apache-tomcat-9.0.43.tar.gz", new File(localTestDirectory, "apache-tomcat-9.0.22.tar.gz")).work();
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}
}
