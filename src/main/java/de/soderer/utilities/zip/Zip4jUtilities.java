package de.soderer.utilities.zip;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipException;

import de.soderer.utilities.InputStreamWithOtherItemsToClose;
import de.soderer.utilities.Utilities;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.AesKeyStrength;
import net.lingala.zip4j.model.enums.CompressionMethod;
import net.lingala.zip4j.model.enums.EncryptionMethod;

/**
 * Static helper methods for password secured zip files, based on the zip4j library.
 */
public class Zip4jUtilities {
	private Zip4jUtilities() {
		// Utility class with static methods only
	}

	/**
	 * Returns the names of all entries of a (password secured) zip file.
	 *
	 * @param file zip file to read
	 * @param zipPassword password of the zip file, may be null for unsecured zip files
	 * @return names of all entries, including directories
	 * @throws ZipException if the file is no valid zip file
	 * @throws IOException if the file cannot be read
	 */
	public static List<String> getZipFileEntries(final File file, final char[] zipPassword) throws ZipException, IOException {
		final List<String> entries = new ArrayList<>();
		try (ZipFile zipFile = new ZipFile(file, zipPassword)) {
			final List<FileHeader> fileHeaders = zipFile.getFileHeaders();
			if (fileHeaders != null) {
				for (final FileHeader fileHeader : fileHeaders) {
					entries.add(fileHeader.getFileName());
				}
			}
		}
		return entries;
	}

	/**
	 * Replaces a zip file containing exactly one file by a password secured zip file with the same content.
	 * The file inside is named like the zip file without its ".zip" extension.
	 *
	 * @param originalZipFilePath path of the unsecured zip file, which is replaced
	 * @param zipPassword password for the new zip file
	 * @param useZipCrypto true for the weak but widely supported ZipCrypto encryption, false for AES-256
	 * @throws IOException if the zip file does not contain exactly one file or cannot be read or written
	 */
	public static void createPasswordSecuredZipFile(final String originalZipFilePath, final char[] zipPassword, final boolean useZipCrypto) throws IOException {
		final ZipParameters zipParameters = new ZipParameters();
		zipParameters.setCompressionMethod(CompressionMethod.DEFLATE);
		zipParameters.setEncryptFiles(true);
		if (!useZipCrypto) {
			zipParameters.setEncryptionMethod(EncryptionMethod.AES);
			zipParameters.setAesKeyStrength(AesKeyStrength.KEY_STRENGTH_256);
		} else {
			zipParameters.setEncryptionMethod(EncryptionMethod.ZIP_STANDARD);
		}
		String entryFileName = new File(originalZipFilePath).getName();
		if (entryFileName.toLowerCase(Locale.ROOT).endsWith(".zip")) {
			entryFileName = entryFileName.substring(0, entryFileName.length() - 4);
		}
		zipParameters.setFileNameInZip(entryFileName);
		final File tempFile = new File(originalZipFilePath + ".tmp");
		// A leftover temp file would otherwise be extended by zip4j instead of being replaced
		Files.deleteIfExists(tempFile.toPath());
		try (ZipFile zipFile = new ZipFile(tempFile, zipPassword)) {
			try (ZipFile unencryptedZipFile = new ZipFile(new File(originalZipFilePath))) {
				final List<FileHeader> fileHeaders = unencryptedZipFile.getFileHeaders();
				if (fileHeaders == null || fileHeaders.size() != 1 || fileHeaders.get(0).isDirectory()) {
					// Only the first entry is taken over, so any other entries would get lost with the original file
					throw new IOException("Zip file '" + originalZipFilePath + "' must contain exactly one file to be password secured");
				}
				try (InputStream inputStream = new InputStreamWithOtherItemsToClose(unencryptedZipFile.getInputStream(fileHeaders.get(0)), unencryptedZipFile)) {
					zipFile.addStream(inputStream, zipParameters);
				}
			}
		} catch (final IOException e) {
			Files.deleteIfExists(tempFile.toPath());
			throw e;
		}
		Files.move(tempFile.toPath(), new File(originalZipFilePath).toPath(), StandardCopyOption.REPLACE_EXISTING);
	}

	/**
	 * Returns the total uncompressed size of all entries of a (password secured) zip file.
	 *
	 * @param zipFilePath zip file to read
	 * @param zipPassword password of the zip file, may be null for unsecured zip files
	 * @return sum of the uncompressed sizes in bytes, or -1 if the size of any entry is unknown
	 * @throws IOException if the file cannot be read
	 */
	public static long getUncompressedSize(final File zipFilePath, final char[] zipPassword) throws IOException {
		try (ZipFile zipFile = new ZipFile(zipFilePath, zipPassword)) {
			final List<FileHeader> fileHeaders = zipFile.getFileHeaders();
			long uncompressedSize = 0;
			for (final FileHeader fileHeader : fileHeaders) {
				final long originalSize = fileHeader.getUncompressedSize();
				if (originalSize >= 0) {
					uncompressedSize += originalSize;
				} else {
					// -1 indicates, that size is unknown
					uncompressedSize = originalSize;
					break;
				}
			}
			return uncompressedSize;
		}
	}

	/**
	 * Opens the only file of a (password secured) zip file for reading.
	 *
	 * @param importFilePathOrData path of the zip file
	 * @param zipPassword password of the zip file, may be null for unsecured zip files
	 * @return stream with the content of the file, closing it also closes the zip file
	 * @throws Exception if the zip file cannot be read, is empty or contains more than one file
	 */
	public static InputStreamWithOtherItemsToClose openPasswordSecuredZipFile(final String importFilePathOrData, final char[] zipPassword) throws Exception {
		return openPasswordSecuredZipFile(importFilePathOrData, zipPassword, null);
	}

	/**
	 * Opens a file of a (password secured) zip file for reading.
	 *
	 * @param importFilePathOrData path of the zip file
	 * @param zipPassword password of the zip file, may be null for unsecured zip files
	 * @param zippedFilePathAndName path of the file inside the zip file, or null if the zip file contains only one file
	 * @return stream with the content of the file, closing it also closes the zip file
	 * @throws Exception if the zip file cannot be read or does not contain the file
	 */
	@SuppressWarnings("resource")
	public static InputStreamWithOtherItemsToClose openPasswordSecuredZipFile(final String importFilePathOrData, final char[] zipPassword, final String zippedFilePathAndName) throws Exception {
		ZipFile zipFile = null;
		try {
			zipFile = new ZipFile(importFilePathOrData, zipPassword);
			final List<FileHeader> fileHeaders = zipFile.getFileHeaders();
			FileHeader selectedFileHeader = null;

			if (fileHeaders != null && fileHeaders.size() > 0) {
				if (Utilities.isBlank(zippedFilePathAndName)) {
					if (fileHeaders.size() == 1 && !fileHeaders.get(0).isDirectory()) {
						return new InputStreamWithOtherItemsToClose(zipFile.getInputStream(fileHeaders.get(0)), fileHeaders.get(0).getFileName(), zipFile);
					} else {
						throw new Exception("Zip file '" + importFilePathOrData + "' contains more than one file");
					}
				} else {
					for (final FileHeader fileHeader : fileHeaders) {
						if (!fileHeader.isDirectory() && fileHeader.getFileName().equals(zippedFilePathAndName)) {
							selectedFileHeader = fileHeader;
							break;
						}
					}
					if (selectedFileHeader != null) {
						return new InputStreamWithOtherItemsToClose(zipFile.getInputStream(selectedFileHeader), selectedFileHeader.getFileName(), zipFile);
					} else {
						throw new Exception("Zip file '" + importFilePathOrData + "' does not include defined zipped file '" + zippedFilePathAndName + "'");
					}
				}
			} else {
				throw new Exception("Zip file '" + importFilePathOrData + "' is empty");
			}
		} catch (final Exception e) {
			try {
				if (zipFile != null) {
					zipFile.close();
				}
			} catch (@SuppressWarnings("unused") final IOException e1) {
				// Do nothing
			}
			throw e;
		}
	}
}
