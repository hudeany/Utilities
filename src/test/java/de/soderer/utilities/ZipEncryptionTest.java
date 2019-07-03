package de.soderer.utilities;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;

public class ZipEncryptionTest {
	@Test
	public void test() {
		try {
			final String testString = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 !\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~";

			final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			final OutputStream zipOutputStream = new ZipOutputStream(new ZipEncryptOutputStream(outputStream, "Test".toCharArray()));
			((ZipOutputStream) zipOutputStream).putNextEntry(new ZipEntry("Test.txt"));
			IOUtils.copy(new ByteArrayInputStream(testString.getBytes("UTF-8")), zipOutputStream);
			((ZipOutputStream) zipOutputStream).closeEntry();
			zipOutputStream.close();

			byte[] data = outputStream.toByteArray();

			final InputStream inputStream = new ZipInputStream(new ZipDecryptInputStream(new ByteArrayInputStream(data), "Test".toCharArray()));
			((ZipInputStream) inputStream).getNextEntry();

			data = IOUtils.toByteArray(inputStream);

			Assert.assertEquals(testString, new String(data, "UTF-8"));
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}
}
