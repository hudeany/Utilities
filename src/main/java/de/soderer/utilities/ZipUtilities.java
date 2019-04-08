package de.soderer.utilities;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class ZipUtilities {
	/**
	 * Zip a file or recursively all files and subdirectories of a directory. The zipped file is placed in the same directory as the zipped file.
	 *
	 * @param sourceFile
	 * @return
	 * @throws IOException
	 */
	public static File zipFile(final File sourceFile) throws IOException {
		final File zippedFile = new File(sourceFile.getAbsolutePath() + ".zip");
		zipFile(sourceFile, zippedFile);
		return zippedFile;
	}

	/**
	 * Zip a file or recursively all files and subdirectories of a directory.
	 *
	 * @param sourceFile
	 * @param destinationZipFile
	 * @throws IOException
	 */
	public static void zipFile(final File sourceFile, final File destinationZipFile) throws IOException {
		if (!sourceFile.exists()) {
			throw new IOException("SourceFile does not exist");
		}

		if (destinationZipFile.exists()) {
			throw new IOException("DestinationFile already exists");
		}

		try (ZipOutputStream zipOutputStream = openNewZipOutputStream(destinationZipFile)) {
			addFileToOpenZipFileStream(sourceFile, zipOutputStream);
		} catch (final IOException e) {
			if (destinationZipFile.exists()) {
				destinationZipFile.delete();
			}
			throw e;
		}
	}

	/**
	 * Zip a file or recursively all files and subdirectories of a directory. A new relative path is started.
	 *
	 * @param sourceFile
	 * @param destinationZipFileSream
	 * @throws IOException
	 */
	public static void addFileToOpenZipFileStream(final File sourceFile, final ZipOutputStream destinationZipFileSream) throws IOException {
		addFileToOpenZipFileStream(sourceFile, File.separator, destinationZipFileSream);
	}

	/**
	 * Zip a file or recursively all files and subdirectories of a directory.
	 *
	 * @param sourceFile
	 * @param destinationZipFileSream
	 * @throws IOException
	 */
	public static void addFileToOpenZipFileStream(final File sourceFile, final String relativeDirPath, final ZipOutputStream destinationZipFileSream) throws IOException {
		if (!sourceFile.exists()) {
			throw new IOException("SourceFile does not exist");
		}

		if (destinationZipFileSream == null) {
			throw new IOException("DestinationStream is not ready");
		}

		if (relativeDirPath == null || (!relativeDirPath.endsWith("/") && !relativeDirPath.endsWith("\\"))) {
			throw new IOException("RelativeDirPath is invalid");
		}

		try {
			if (!sourceFile.isDirectory()) {
				final ZipEntry entry = new ZipEntry(relativeDirPath + sourceFile.getName());
				entry.setTime(sourceFile.lastModified());
				destinationZipFileSream.putNextEntry(entry);

				try (BufferedInputStream bufferedFileInputStream = new BufferedInputStream(new FileInputStream(sourceFile))) {
					final byte[] bufferArray = new byte[4096];
					int byteBufferFillLength = bufferedFileInputStream.read(bufferArray);
					while (byteBufferFillLength > -1) {
						destinationZipFileSream.write(bufferArray, 0, byteBufferFillLength);
						byteBufferFillLength = bufferedFileInputStream.read(bufferArray);
					}

					destinationZipFileSream.flush();
					destinationZipFileSream.closeEntry();
				}
			} else {
				for (final File sourceSubFile : sourceFile.listFiles()) {
					addFileToOpenZipFileStream(sourceSubFile, relativeDirPath + sourceFile.getName() + File.separator, destinationZipFileSream);
				}
			}
		} catch (final IOException e) {
			throw e;
		}
	}

	/**
	 * Add a file to an opened ZipOutputStream
	 *
	 * @param fileData
	 * @param filename
	 * @param destinationZipFileSream
	 * @throws IOException
	 */
	public static void addFileDataToOpenZipFileStream(final byte[] fileData, final String filename, final ZipOutputStream destinationZipFileSream) throws IOException {
		addFileDataToOpenZipFileStream(fileData, File.separator, filename, destinationZipFileSream);
	}

	/**
	 * Open a ZipOutputStream based on a file in which is written
	 *
	 * @param destinationZipFile
	 * @return
	 * @throws IOException
	 */
	public static ZipOutputStream openNewZipOutputStream(final File destinationZipFile) throws IOException {
		if (destinationZipFile.exists()) {
			throw new IOException("DestinationFile already exists");
		} else if (!destinationZipFile.getParentFile().exists()) {
			throw new IOException("DestinationDirectory does not exist");
		}

		try {
			return new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(destinationZipFile)));
		} catch (final IOException e) {
			if (destinationZipFile.exists()) {
				destinationZipFile.delete();
			}
			throw e;
		}
	}

	/**
	 * Open a ZipOutputStream based on a OutputStream in which is written
	 *
	 * @param destinationZipStream
	 * @return
	 * @throws IOException
	 */
	public static ZipOutputStream openNewZipOutputStream(final OutputStream destinationZipStream) throws IOException {
		if (destinationZipStream == null) {
			throw new IOException("DestinationStream is missing");
		}

		return new ZipOutputStream(new BufferedOutputStream(destinationZipStream));
	}

	/**
	 * Add a file to an opened ZipOutputStream
	 *
	 * @param fileData
	 * @param relativeDirPath
	 * @param filename
	 * @param destinationZipFileStream
	 * @throws IOException
	 */
	public static void addFileDataToOpenZipFileStream(final byte[] fileData, final String relativeDirPath, final String filename, final ZipOutputStream destinationZipFileStream) throws IOException {
		if (fileData == null) {
			throw new IOException("FileData is missing");
		}

		if (Utilities.isEmpty(filename) || filename.trim().length() == 0) {
			throw new IOException("Filename is missing");
		}

		if (destinationZipFileStream == null) {
			throw new IOException("DestinationStream is not ready");
		}

		if (relativeDirPath == null || (!relativeDirPath.endsWith("/") && !relativeDirPath.endsWith("\\"))) {
			throw new IOException("RelativeDirPath is invalid");
		}

		final ZipEntry entry = new ZipEntry(relativeDirPath + filename);
		entry.setTime(new Date().getTime());
		destinationZipFileStream.putNextEntry(entry);

		destinationZipFileStream.write(fileData);

		destinationZipFileStream.flush();
		destinationZipFileStream.closeEntry();
	}

	/**
	 * Add a file or recursively all files and subdirectories of a directory to an existing ZipFile. All existing entries are preserved.
	 *
	 * @param sourceFile
	 * @return
	 * @throws IOException
	 */
	public static void addFileToExistingzipFile(final File sourceFile, final File zipFile) throws IOException {
		try (ZipOutputStream zipOutputStream = openExistingZipFileForExtension(zipFile)) {
			addFileToOpenZipFileStream(sourceFile, zipOutputStream);
		}
	}

	/**
	 * Add a list of files or recursively all files and subdirectories of a directory to an existing ZipFile. All existing entries are preserved.
	 *
	 * @param sourceFile
	 * @return
	 * @throws IOException
	 */
	public static void addFileToExistingzipFile(final List<File> sourceFiles, final File zipFile) throws IOException {
		try (ZipOutputStream zipOutputStream = openExistingZipFileForExtension(zipFile)) {
			for (final File file : sourceFiles) {
				addFileToOpenZipFileStream(file, zipOutputStream);
			}
		}
	}

	/**
	 * Open an exsiting ZipFile for appending new files or create a new ZipFile if it does not exist yet.
	 *
	 * @param file
	 * @return
	 * @throws IOException
	 */
	public static ZipOutputStream openExistingZipFileForExtensionOrCreateNewZipFile(final File zipFile) throws IOException {
		if (zipFile.exists()) {
			return openExistingZipFileForExtension(zipFile);
		} else {
			return openNewZipOutputStream(zipFile);
		}
	}

	/**
	 * Open an exsiting ZipFile for appending new files. All existing entries are preserved.
	 *
	 * @param zipFile
	 * @return
	 * @throws IOException
	 * @throws ZipException
	 */
	public static ZipOutputStream openExistingZipFileForExtension(final File zipFile) throws IOException {
		final File originalFileTemp = new File(zipFile.getParentFile().getAbsolutePath() + "/" + String.valueOf(System.currentTimeMillis()));
		zipFile.renameTo(originalFileTemp);

		final ZipOutputStream zipOutputStream = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFile)));

		ZipFile sourceZipFile = null;
		try {
			sourceZipFile = new ZipFile(originalFileTemp);
			final Enumeration<? extends ZipEntry> srcEntries = sourceZipFile.entries();
			while (srcEntries.hasMoreElements()) {
				final ZipEntry sourceZipFileEntry = srcEntries.nextElement();
				zipOutputStream.putNextEntry(sourceZipFileEntry);

				try (BufferedInputStream bufferedInputStream = new BufferedInputStream(sourceZipFile.getInputStream(sourceZipFileEntry))) {
					final byte[] bufferArray = new byte[4096];
					int byteBufferFillLength = bufferedInputStream.read(bufferArray);
					while (byteBufferFillLength > -1) {
						zipOutputStream.write(bufferArray, 0, byteBufferFillLength);
						byteBufferFillLength = bufferedInputStream.read(bufferArray);
					}

					zipOutputStream.closeEntry();
				}
			}

			zipOutputStream.flush();
			sourceZipFile.close();
			originalFileTemp.delete();

			return zipOutputStream;
		} catch (final IOException e) {
			if (zipFile.exists()) {
				Utilities.closeQuietly(zipOutputStream);
				zipFile.delete();
			}

			originalFileTemp.renameTo(zipFile);
			throw e;
		} finally {
			Utilities.closeQuietly(sourceZipFile);
		}
	}

	/**
	 * Read a ZipFile
	 *
	 * @param zipFile
	 * @return All entries in a map
	 * @throws IOException
	 */
	public static Map<String, byte[]> readExistingZipFile(final File zipFile) throws IOException {
		final Map<String, byte[]> returnMap = new HashMap<>();
		try (ZipFile sourceZipFile = new ZipFile(zipFile)) {
			final Enumeration<? extends ZipEntry> srcEntries = sourceZipFile.entries();
			while (srcEntries.hasMoreElements()) {
				final ZipEntry sourceZipFileEntry = srcEntries.nextElement();

				try (BufferedInputStream bufferedInputStream = new BufferedInputStream(sourceZipFile.getInputStream(sourceZipFileEntry))) {
					final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

					final byte[] bufferArray = new byte[4096];
					int byteBufferFillLength = bufferedInputStream.read(bufferArray);
					while (byteBufferFillLength > -1) {
						byteArrayOutputStream.write(bufferArray, 0, byteBufferFillLength);
						byteBufferFillLength = bufferedInputStream.read(bufferArray);
					}

					returnMap.put(sourceZipFileEntry.getName(), byteArrayOutputStream.toByteArray());
				}
			}
		}
		return returnMap;
	}

	public static String[] getZipFileEntries(final File file) throws ZipException, IOException {
		try (ZipFile zipFile = new ZipFile(file)) {
			final Enumeration<? extends ZipEntry> entries = zipFile.entries();
			final List<String> entryList = new ArrayList<>();
			while (entries.hasMoreElements()) {
				final ZipEntry entry = entries.nextElement();
				entryList.add(entry.getName());
			}
			return entryList.toArray(new String[0]);
		}
	}
}
