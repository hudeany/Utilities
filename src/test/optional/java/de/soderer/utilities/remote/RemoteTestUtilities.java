package de.soderer.utilities.remote;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Set;
import java.util.TreeSet;

/**
 * Assertions for comparing local files and directories in the remote connection tests.
 */
final class RemoteTestUtilities {
	private RemoteTestUtilities() {
		// Utility class
	}

	/**
	 * Asserts that both files exist and have exactly the same content.
	 *
	 * @param expectedFile original file
	 * @param actualFile file to check
	 * @throws IOException if a file cannot be read
	 */
	static void assertFilesEqual(final File expectedFile, final File actualFile) throws IOException {
		assertTrue(expectedFile.isFile(), "Missing expected file: " + expectedFile);
		assertTrue(actualFile.isFile(), "Missing file: " + actualFile);
		final long mismatchPosition = Files.mismatch(expectedFile.toPath(), actualFile.toPath());
		assertEquals(-1L, mismatchPosition, () -> "Files differ at byte " + mismatchPosition + ": " + expectedFile + " <-> " + actualFile);
	}

	/**
	 * Asserts recursively that both directories contain the same file and directory names
	 * and that all files have exactly the same content.
	 *
	 * @param expectedDirectory original directory
	 * @param actualDirectory directory to check
	 * @throws IOException if a file cannot be read
	 */
	static void assertDirectoriesEqual(final File expectedDirectory, final File actualDirectory) throws IOException {
		assertTrue(expectedDirectory.isDirectory(), "Missing expected directory: " + expectedDirectory);
		assertTrue(actualDirectory.isDirectory(), "Missing directory: " + actualDirectory);

		final Set<String> expectedNames = getEntryNames(expectedDirectory);
		assertEquals(expectedNames, getEntryNames(actualDirectory), "Directory contents differ: " + expectedDirectory + " <-> " + actualDirectory);

		for (final String name : expectedNames) {
			final File expectedEntry = new File(expectedDirectory, name);
			final File actualEntry = new File(actualDirectory, name);
			if (expectedEntry.isDirectory()) {
				assertDirectoriesEqual(expectedEntry, actualEntry);
			} else {
				assertFilesEqual(expectedEntry, actualEntry);
			}
		}
	}

	/**
	 * Names of all entries of a directory, sorted for a readable failure message.
	 */
	private static Set<String> getEntryNames(final File directory) {
		final Set<String> names = new TreeSet<>();
		final String[] entries = directory.list();
		if (entries != null) {
			for (final String entry : entries) {
				names.add(entry);
			}
		}
		return names;
	}
}
