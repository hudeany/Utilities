package de.soderer.utilities.zip;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;

import de.soderer.utilities.FileUtilities;
import de.soderer.utilities.IoUtilities;
import de.soderer.utilities.Utilities;

/**
 * Static helper methods for reading and writing gzip compressed tar archives (".tar.gz").
 */
public class TarGzUtilities {
	private TarGzUtilities() {
		// Utility class with static methods only
	}

	/**
	 * Returns the total size of all files in a tar.gz archive.
	 *
	 * @param tarGzFile tar.gz archive to read
	 * @return sum of the uncompressed sizes of all files in bytes
	 * @throws Exception if the archive does not exist or cannot be read
	 */
	public static long getUncompressedSize(final File tarGzFile) throws Exception {
		if (!tarGzFile.exists()) {
			throw new Exception("TarGz file does not exist: " + tarGzFile.getAbsolutePath());
		} else if (!tarGzFile.isFile()) {
			throw new Exception("TarGz file path is not a file: " + tarGzFile.getAbsolutePath());
		} else {
			try (TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(new GzipCompressorInputStream(new BufferedInputStream(new FileInputStream(tarGzFile))))) {
				TarArchiveEntry entry;
				long size = 0;
				while ((entry = tarArchiveInputStream.getNextTarEntry()) != null) {
					if (!entry.isDirectory()) {
						size += entry.getRealSize();
					}
				}
				return size;
			} catch (final Exception e) {
				throw new Exception("Cannot read '" + tarGzFile + "'", e);
			}
		}
	}

	/**
	 * Returns the number of files (without directories) in a tar.gz archive.
	 *
	 * @param tarGzFile tar.gz archive to read
	 * @return number of files
	 * @throws Exception if the archive does not exist or cannot be read
	 */
	public static int getFilesCount(final File tarGzFile) throws Exception {
		if (!tarGzFile.exists()) {
			throw new Exception("TarGz file does not exist: " + tarGzFile.getAbsolutePath());
		} else if (!tarGzFile.isFile()) {
			throw new Exception("TarGz file path is not a file: " + tarGzFile.getAbsolutePath());
		} else {
			try (TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(new GzipCompressorInputStream(new BufferedInputStream(new FileInputStream(tarGzFile))))) {
				TarArchiveEntry entry;
				int count = 0;
				while ((entry = tarArchiveInputStream.getNextTarEntry()) != null) {
					if (!entry.isDirectory()) {
						count++;
					}
				}
				return count;
			} catch (final Exception e) {
				throw new Exception("Cannot read '" + tarGzFile + "'", e);
			}
		}
	}

	/**
	 * Extracts all files of a tar.gz archive into a new directory. Entries pointing outside of the destination
	 * directory (path traversal) are rejected. On error the destination directory is deleted again.
	 *
	 * @param tarGzFile tar.gz archive to extract
	 * @param decompressToPath destination directory, which must not exist yet
	 * @throws Exception if the archive does not exist or cannot be extracted, or the destination already exists
	 */
	public static void decompress(final File tarGzFile, final File decompressToPath) throws Exception {
		if (!tarGzFile.exists()) {
			throw new Exception("TarGz file does not exist: " + tarGzFile.getAbsolutePath());
		} else if (!tarGzFile.isFile()) {
			throw new Exception("TarGz file path is not a file: " + tarGzFile.getAbsolutePath());
		} else if (decompressToPath.exists()) {
			throw new Exception("Destination path already exists: " + decompressToPath.getAbsolutePath());
		} else {
			try (TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(new GzipCompressorInputStream(new BufferedInputStream(new FileInputStream(tarGzFile))))) {
				decompressToPath.mkdirs();

				TarArchiveEntry entry;
				while ((entry = tarArchiveInputStream.getNextTarEntry()) != null) {
					if (entry.isDirectory()) {
						continue;
					}
					String entryFilePath = entry.getName();
					entryFilePath = entryFilePath.replace("\\", "/");
					if (entryFilePath.startsWith("/") || entryFilePath.startsWith("../") || entryFilePath.endsWith("/..") || entryFilePath.contains("/../")) {
						throw new Exception("Traversal error in tar gz file: " + tarGzFile.getAbsolutePath());
					}
					final File currentfile = new File(decompressToPath, entryFilePath);
					// Compare with trailing separator, otherwise a sibling directory like "dest2" would pass for "dest"
					if (!currentfile.getCanonicalPath().startsWith(decompressToPath.getCanonicalPath() + File.separator)) {
						throw new Exception("Traversal error in tar gz file: " + tarGzFile.getAbsolutePath() + "/");
					}
					final File parent = currentfile.getParentFile();
					if (!parent.exists()) {
						parent.mkdirs();
					}

					try(final FileOutputStream fileOutputStream = new FileOutputStream(currentfile)) {
						IoUtilities.copy(tarArchiveInputStream, fileOutputStream);
					}
				}
			} catch (final Exception e) {
				try {
					if (decompressToPath.exists()) {
						FileUtilities.delete(decompressToPath);
					}
				} catch (@SuppressWarnings("unused") final Exception e1) {
					// do nothing else
				}

				throw new Exception("Cannot decompress '" + tarGzFile + "'", e);
			}
		}
	}

	/**
	 * Creates a tar.gz archive containing a single file, stored under its file name.
	 *
	 * @param tarGzFile tar.gz archive to create, must not exist yet
	 * @param fileToCompress file to put into the archive
	 * @throws IOException if the archive exists already or cannot be written
	 */
	public static void compress(final File tarGzFile, final File fileToCompress) throws IOException {
		compress(tarGzFile, fileToCompress, null);
	}

	/**
	 * Creates a tar.gz archive containing a single file.
	 *
	 * @param tarGzFile tar.gz archive to create, must not exist yet
	 * @param fileToCompress file to put into the archive
	 * @param filePathInTarGzFile path of the file inside the archive, or null to use the file name
	 * @throws IOException if the archive exists already or cannot be written
	 */
	public static void compress(final File tarGzFile, final File fileToCompress, String filePathInTarGzFile) throws IOException {
		if (tarGzFile.exists()) {
			throw new IOException("TarGz file already exists: " + tarGzFile.getAbsolutePath());
		} else if (!tarGzFile.getAbsoluteFile().getParentFile().exists()) {
			throw new IOException("Parent directory for TarGz file does not exist: " + tarGzFile.getAbsoluteFile().getParentFile().getAbsolutePath());
		}

		if (filePathInTarGzFile == null) {
			filePathInTarGzFile = fileToCompress.getName();
		}

		try (TarArchiveOutputStream tarArchiveOutputStream = new TarArchiveOutputStream(new GzipCompressorOutputStream(new BufferedOutputStream(new FileOutputStream(tarGzFile))));
				FileInputStream fileInputStream = new FileInputStream(fileToCompress)) {
			tarArchiveOutputStream.putArchiveEntry(new TarArchiveEntry(fileToCompress, filePathInTarGzFile));
			IoUtilities.copy(fileInputStream, tarArchiveOutputStream);
			tarArchiveOutputStream.closeArchiveEntry();
			tarArchiveOutputStream.finish();
		} catch (final IOException e) {
			if (tarGzFile.exists()) {
				tarGzFile.delete();
			}
			throw e;
		}
	}

	/**
	 * Opens the first file of a tar.gz archive for reading.
	 *
	 * @param tarGzFile tar.gz archive to read
	 * @return stream positioned at the content of the first file, the caller must close it
	 * @throws Exception if the archive does not exist, cannot be read or contains no file
	 */
	public static InputStream openCompressedFile(final File tarGzFile) throws Exception {
		return openCompressedFile(tarGzFile, null);
	}

	/**
	 * Opens a file of a tar.gz archive for reading.
	 *
	 * @param tarGzFile tar.gz archive to read
	 * @param filePathInTarGzFile path of the file inside the archive, or null for the first file
	 * @return stream positioned at the content of the file, the caller must close it
	 * @throws Exception if the archive does not exist, cannot be read or does not contain the file
	 */
	public static InputStream openCompressedFile(final File tarGzFile, final String filePathInTarGzFile) throws Exception {
		if (!tarGzFile.exists()) {
			throw new Exception("TarGz file does not exist: " + tarGzFile.getAbsolutePath());
		} else if (!tarGzFile.isFile()) {
			throw new Exception("TarGz file path is not a file: " + tarGzFile.getAbsolutePath());
		} else {
			TarArchiveInputStream tarArchiveInputStream = null;
			try {
				tarArchiveInputStream = new TarArchiveInputStream(new GzipCompressorInputStream(new BufferedInputStream(new FileInputStream(tarGzFile))));
				TarArchiveEntry entry = null;
				while ((entry = tarArchiveInputStream.getNextTarEntry()) != null) {
					if (entry.isDirectory()) {
						continue;
					} else {
						if (Utilities.isBlank(filePathInTarGzFile)) {
							return tarArchiveInputStream;
						} else {
							String entryFilePath = entry.getName();
							entryFilePath = entryFilePath.replace("\\", "/");
							if (entryFilePath.equals(filePathInTarGzFile)) {
								return tarArchiveInputStream;
							}
						}
					}
				}

				if (Utilities.isBlank(filePathInTarGzFile)) {
					throw new Exception("TarGz file '" + tarGzFile.getAbsolutePath() + "' is empty");
				} else {
					throw new Exception("TarGz file does not contain file '" + filePathInTarGzFile + "'");
				}
			} catch (final Exception e) {
				try {
					if (tarArchiveInputStream != null) {
						tarArchiveInputStream.close();
					}
				} catch (@SuppressWarnings("unused") final IOException e1) {
					// Do nothing
				}
				throw e;
			}
		}
	}
}
