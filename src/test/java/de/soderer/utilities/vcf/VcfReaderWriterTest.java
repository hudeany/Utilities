package de.soderer.utilities.vcf;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.junit.Assert;
import org.junit.Test;

import de.soderer.utilities.BOMInputStream;

public class VcfReaderWriterTest {
	@SuppressWarnings("resource")
	@Test
	public void test1() throws Exception {
		final File testfile = new File(getClass().getClassLoader().getResource("vcf/test.vcf").toURI());

		File tempFile = File.createTempFile("VcfReaderWriterTest", ".vcf");

		try {
			int cardCount = 0;
			try (VcfReader reader = new VcfReader(new FileInputStream(testfile));
					VcfWriter writer = new VcfWriter(new FileOutputStream(tempFile), true)) {
				VcfCard nextCard;
				while ((nextCard = reader.readNextCard()) != null) {
					cardCount++;
					String version;
					switch (cardCount) {
						case 2:
							version = "3.0";
							break;
						case 3:
							version = "4.0";
							break;
						default:
							version = "2.1";
					}

					writer.writeCard(nextCard, version);
				}
			} catch (final Exception e) {
				e.printStackTrace();
				Assert.fail(e.getMessage());
			}

			try (BufferedReader readerOriginal = new BufferedReader(new InputStreamReader(new BOMInputStream(new FileInputStream(testfile)).skipBOM(), StandardCharsets.UTF_8));
					BufferedReader readerTest = new BufferedReader(new InputStreamReader(new BOMInputStream(new FileInputStream(tempFile)).skipBOM(), StandardCharsets.UTF_8))) {
				int line = 0;
				String nextLineOriginal;
				String nextLineTest;
				while ((nextLineOriginal = readerOriginal.readLine()) != null) {
					nextLineTest = readerTest.readLine();
					line++;
					Assert.assertEquals("Unexpected data in line " + line, nextLineOriginal, nextLineTest);
				}
			}
		} finally {
			if (tempFile != null && tempFile.exists()) {
				tempFile.delete();
				tempFile = null;
			}
		}
	}
}
