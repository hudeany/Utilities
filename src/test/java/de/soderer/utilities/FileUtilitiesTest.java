package de.soderer.utilities;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@SuppressWarnings("static-method")
public class FileUtilitiesTest {
	private static final Charset WINDOWS_1252 = Charset.forName("windows-1252");
	private static final byte[] UTF8_BOM = { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };

	@TempDir
	Path tempDir;

	@Test
	public void getSubfilesByNameAndExtension() throws Exception {
		createFile("data1.csv", "");
		createFile("data2.csv", "");
		createFile("data3.txt", "");
		createFile("other.csv", "");
		Files.createDirectory(tempDir.resolve("data4.csv"));

		assertEquals(List.of("data1.csv", "data2.csv"), sortedNames(FileUtilities.getSubfilesByNameAndExtension(tempDir.toFile(), "data", "csv")));
		assertEquals(List.of(), sortedNames(FileUtilities.getSubfilesByNameAndExtension(tempDir.resolve("missing").toFile(), "data", "csv")));
	}

	@Test
	public void getFilesWithWildcards() throws Exception {
		createFile("a.txt", "");
		createFile("b.log", "");
		createFile("sub/c.txt", "");
		createFile("sub/deeper/d.txt", "");
		// A directory matching the pattern is no file
		Files.createDirectory(tempDir.resolve("e.txt"));

		assertEquals(List.of("a.txt"), sortedNames(FileUtilities.getFilesWithWildcards(tempDir.toFile(), "*.txt", false)));
		assertEquals(List.of("a.txt", "c.txt", "d.txt"), sortedNames(FileUtilities.getFilesWithWildcards(tempDir.toFile(), "*.txt", true)));
		assertEquals(List.of(), sortedNames(FileUtilities.getFilesWithWildcards(tempDir.resolve("a.txt").toFile(), "*.txt", true)));
	}

	@Test
	public void fileSearchText() throws Exception {
		final File file = createFile("search.txt", "Hello World\nsecond line");

		assertTrue(FileUtilities.fileSearchText(file, "world", false, false, null));
		assertFalse(FileUtilities.fileSearchText(file, "world", true, false, null));
		assertTrue(FileUtilities.fileSearchText(file, "^second", true, true, null));
		assertFalse(FileUtilities.fileSearchText(file, "^World", true, true, null));
		assertFalse(FileUtilities.fileSearchText(file, "W.rld", true, false, null));
		assertTrue(FileUtilities.fileSearchText(file, Arrays.asList("missing", "line"), true, false, null));
		assertFalse(FileUtilities.fileSearchText(file, new ArrayList<>(), true, false, null));
		assertFalse(FileUtilities.fileSearchText(file, (List<String>) null, true, false, null));
	}

	@Test
	public void fileSearchText_emptyFile() throws Exception {
		assertFalse(FileUtilities.fileSearchText(createFile("empty.txt", ""), "x", true, false, null));
	}

	@Test
	public void fileSearchText_missingFile() {
		assertThrows(FileNotFoundException.class, () -> FileUtilities.fileSearchText(tempDir.resolve("missing.txt").toFile(), "x", true, false, null));
	}

	@Test
	public void fileSearchText_umlautsCaseInsensitive() throws Exception {
		final File file = createFile("umlaut.txt", "ÄPFEL");

		assertTrue(FileUtilities.fileSearchText(file, "äpfel", false, false, null));
	}

	@Test
	public void fileSearchText_windows1252File() throws Exception {
		final File file = createFile("ansi.txt", "Größe".getBytes(WINDOWS_1252));

		assertTrue(FileUtilities.fileSearchText(file, "Größe", true, false, null));
	}

	@Test
	public void fileSearchText_bomIsNoPartOfFirstLine() throws Exception {
		final File file = createFile("bom.txt", concat(UTF8_BOM, "first\nsecond".getBytes(StandardCharsets.UTF_8)));

		assertTrue(FileUtilities.fileSearchText(file, "^first", true, true, null));
	}

	@Test
	public void fileReplaceText_literal() throws Exception {
		final File file = createFile("replace.txt", "a.b.c");

		assertTrue(FileUtilities.fileReplaceText(file, ".", "$1\\", true, false, null));
		assertEquals("a$1\\b$1\\c", readUtf8(file));
	}

	@Test
	public void fileReplaceText_regularExpressionWithGroups() throws Exception {
		final File file = createFile("replace.txt", "john@example\nJANE@test");

		assertTrue(FileUtilities.fileReplaceText(file, "^(\\w+)@(\\w+)$", "$2 at $1", false, true, null));
		assertEquals("example at john\ntest at JANE", readUtf8(file));
	}

	@Test
	public void fileReplaceText_multipleTextsInOrder() throws Exception {
		final File file = createFile("replace.txt", "abc");

		assertTrue(FileUtilities.fileReplaceText(file, Arrays.asList("a", "Xb"), Arrays.asList("X", "Y"), true, false, null));
		assertEquals("Yc", readUtf8(file));
	}

	@Test
	public void fileReplaceText_notFound() throws Exception {
		final File file = createFile("replace.txt", "abc");
		final long lastModified = file.lastModified();

		assertFalse(FileUtilities.fileReplaceText(file, "x", "y", true, false, null));
		assertEquals("abc", readUtf8(file));
		assertEquals(lastModified, file.lastModified());
	}

	@Test
	public void fileReplaceText_differentNumberOfTexts() throws Exception {
		final File file = createFile("replace.txt", "abc");

		assertThrows(IllegalArgumentException.class, () -> FileUtilities.fileReplaceText(file, Arrays.asList("a", "b"), Arrays.asList("x"), true, false, null));
	}

	@Test
	public void fileReplaceText_missingFile() {
		assertThrows(FileNotFoundException.class, () -> FileUtilities.fileReplaceText(tempDir.resolve("missing.txt").toFile(), "a", "b", true, false, null));
	}

	@Test
	public void fileReplaceText_keepsBom() throws Exception {
		final File file = createFile("bom.txt", concat(UTF8_BOM, "first\nsecond".getBytes(StandardCharsets.UTF_8)));

		assertTrue(FileUtilities.fileReplaceText(file, "^first", "1st", true, true, null));
		assertArrayEquals(concat(UTF8_BOM, "1st\nsecond".getBytes(StandardCharsets.UTF_8)), Files.readAllBytes(file.toPath()));
	}

	@Test
	public void fileReplaceText_keepsEncoding() throws Exception {
		final File file = createFile("ansi.txt", "Größe x".getBytes(WINDOWS_1252));

		assertTrue(FileUtilities.fileReplaceText(file, "x", "ä", true, false, null));
		assertArrayEquals("Größe ä".getBytes(WINDOWS_1252), Files.readAllBytes(file.toPath()));
	}

	@Test
	public void fileReplaceText_unencodableReplacement() throws Exception {
		final byte[] data = "Größe x".getBytes(WINDOWS_1252);
		final File file = createFile("ansi.txt", data);

		assertThrows(Exception.class, () -> FileUtilities.fileReplaceText(file, "x", "Ω", true, false, null));
		assertArrayEquals(data, Files.readAllBytes(file.toPath()));
	}

	@Test
	public void readFirstFileData() throws Exception {
		final File file = createFile("data.bin", "0123456789");

		assertArrayEquals("0123".getBytes(StandardCharsets.US_ASCII), FileUtilities.readFirstFileData(file, 4));
		assertArrayEquals("0123456789".getBytes(StandardCharsets.US_ASCII), FileUtilities.readFirstFileData(file, 100));
	}

	@Test
	public void getArrayOfFilesByBasename() throws Exception {
		createFile("log1.txt", "");
		createFile("log2.txt", "");
		createFile("xlog.txt", "");
		createFile("single.txt", "");
		Files.createDirectory(tempDir.resolve("logdir"));
		final String directory = tempDir.toString() + File.separator;

		assertEquals(List.of("log1.txt", "log2.txt"), sortedNames(FileUtilities.getArrayOfFilesByBasename(List.of(directory + "log*"))));
		assertEquals(List.of("single.txt"), sortedNames(FileUtilities.getArrayOfFilesByBasename(List.of(directory + "single.txt"))));
		assertEquals(List.of(), sortedNames(FileUtilities.getArrayOfFilesByBasename(List.of(directory + "single"))));
		// No duplicates
		assertEquals(3, FileUtilities.getArrayOfFilesByBasename(List.of(directory + "log*", directory + "log1.txt", directory + "single.txt")).size());
		assertEquals(List.of(), sortedNames(FileUtilities.getArrayOfFilesByBasename(List.of(directory + "missing" + File.separator + "x*"))));
	}

	@Test
	public void getArrayOfFilesByStammname() throws Exception {
		createFile("log1.txt", "");
		createFile("single.txt", "");
		final String directory = tempDir.toString() + File.separator;

		assertEquals(List.of("log1.txt", "single.txt"), sortedNames(FileUtilities.getArrayOfFilesByStammname(directory + "log*;" + directory + "single.txt")));
	}

	@Test
	public void getFilesByPattern() throws Exception {
		createFile("data1.csv", "");
		createFile("data22.csv", "");
		createFile("sub/data3.csv", "");
		Files.createDirectory(tempDir.resolve("data4.csv"));

		assertEquals(List.of("data1.csv"), sortedNames(FileUtilities.getFilesByPattern(tempDir.toFile(), "data\\d\\.csv", false)));
		assertEquals(List.of("data1.csv", "data3.csv"), sortedNames(FileUtilities.getFilesByPattern(tempDir.toFile(), "data\\d\\.csv", true)));
		// The pattern must match the complete name
		assertEquals(List.of(), sortedNames(FileUtilities.getFilesByPattern(tempDir.toFile(), "data", true)));
		assertEquals(List.of(), sortedNames(FileUtilities.getFilesByPattern(tempDir.resolve("data1.csv").toFile(), ".*", true)));
	}

	@Test
	public void getAllSubfiles() throws Exception {
		createFile("a.txt", "12");
		createFile("sub/b.txt", "345");
		createFile("sub/deeper/c.txt", "6789");
		Files.createDirectory(tempDir.resolve("emptyDir"));

		assertEquals(List.of("a.txt", "b.txt", "c.txt"), sortedNames(FileUtilities.getAllSubfiles(tempDir.toFile())));
		assertEquals(3, FileUtilities.getAllSubfilesNumber(tempDir.toFile()));
		assertEquals(9, FileUtilities.getAllSubfilesSize(tempDir.toFile()));
	}

	@Test
	public void getAllSubfiles_symbolicLinksToDirectoriesAreNotFollowed() throws Exception {
		createFile("a.txt", "12");
		createFile("sub/b.txt", "345");
		// Link to a parent directory would cause an endless recursion
		createSymbolicLinkOrSkip(tempDir.resolve("sub/loop"), tempDir);

		assertEquals(List.of("a.txt", "b.txt"), sortedNames(FileUtilities.getAllSubfiles(tempDir.toFile())));
		assertEquals(2, FileUtilities.getAllSubfilesNumber(tempDir.toFile()));
		assertEquals(5, FileUtilities.getAllSubfilesSize(tempDir.toFile()));
		assertEquals(List.of("a.txt", "b.txt"), sortedNames(FileUtilities.getFilesWithWildcards(tempDir.toFile(), "*.txt", true)));
		assertEquals(List.of("a.txt", "b.txt"), sortedNames(FileUtilities.getFilesByPattern(tempDir.toFile(), ".*\\.txt", true)));
	}

	@Test
	public void getLineCount_textFile() throws Exception {
		assertEquals(3, FileUtilities.getLineCount(createFile("lines.txt", "a\nb\r\nc\n")));
		assertEquals(0, FileUtilities.getLineCount(createFile("empty.txt", "")));
	}

	@Test
	public void getLineCount_zipFile() throws Exception {
		final File zipFile = tempDir.resolve("lines.ZIP").toFile();
		try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(zipFile))) {
			zipOutputStream.putNextEntry(new ZipEntry("first.txt"));
			zipOutputStream.write("a\nb\nc\nd".getBytes(StandardCharsets.UTF_8));
			zipOutputStream.closeEntry();
			zipOutputStream.putNextEntry(new ZipEntry("second.txt"));
			zipOutputStream.write("x".getBytes(StandardCharsets.UTF_8));
			zipOutputStream.closeEntry();
		}

		assertEquals(4, FileUtilities.getLineCount(zipFile));
	}

	@Test
	public void getLineCount_emptyZipFile() throws Exception {
		final File zipFile = tempDir.resolve("empty.zip").toFile();
		try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(zipFile))) {
			zipOutputStream.finish();
		}

		assertEquals(0, FileUtilities.getLineCount(zipFile));
	}

	@Test
	public void writeAndRead() throws Exception {
		final File file = tempDir.resolve("data.txt").toFile();

		FileUtilities.writeStringToFile(file, "Grüße", StandardCharsets.UTF_8);
		assertEquals("Grüße", FileUtilities.readFileToString(file, StandardCharsets.UTF_8));

		FileUtilities.write(file, new byte[] { 1, 2 });
		assertArrayEquals(new byte[] { 1, 2 }, FileUtilities.readFileToByteArray(file));

		FileUtilities.append(file, new byte[] { 3 });
		FileUtilities.append(file, "ä", StandardCharsets.ISO_8859_1);
		assertArrayEquals(new byte[] { 1, 2, 3, (byte) 0xE4 }, FileUtilities.readFileToByteArray(file));
	}

	@Test
	public void append_createsFile() throws Exception {
		final File file = tempDir.resolve("new.txt").toFile();

		FileUtilities.append(file, "abc", StandardCharsets.UTF_8);

		assertEquals("abc", readUtf8(file));
	}

	@Test
	public void readFileToByteArray_missingFile() {
		assertThrows(Exception.class, () -> FileUtilities.readFileToByteArray(tempDir.resolve("missing").toFile()));
	}

	@Test
	public void readLinesFromStream_withoutLinebreaks() throws Exception {
		assertEquals(List.of("a", "b", "c", "d"), readLines("a\r\nb\rc\nd", false));
		assertEquals(List.of("a", "", "b"), readLines("a\n\nb", false));
		assertEquals(List.of("", ""), readLines("\r\n\r\n", false));
		assertEquals(List.of("a"), readLines("a\n", false));
		assertEquals(List.of(), readLines("", false));
	}

	@Test
	public void readLinesFromStream_withLinebreaks() throws Exception {
		assertEquals(List.of("a\r\n", "b\r", "c\n", "d"), readLines("a\r\nb\rc\nd", true));
		assertEquals(List.of("\r\n", "\r\n"), readLines("\r\n\r\n", true));
		assertEquals(List.of("\n", "\r"), readLines("\n\r", true));
	}

	@Test
	public void readLinesFromStream_multiByteCharacters() throws Exception {
		assertEquals(List.of("äö", "ü"), FileUtilities.readLinesFromStream(new ByteArrayInputStream("äö\nü".getBytes(StandardCharsets.UTF_8)), false, StandardCharsets.UTF_8));
	}

	@Test
	public void readLinesFromFile() throws Exception {
		final File file = createFile("lines.txt", "a\r\nb");

		assertEquals(List.of("a", "b"), FileUtilities.readLinesFromFile(file, false, StandardCharsets.UTF_8));
		assertEquals(List.of("a\r\n", "b"), FileUtilities.readLinesFromFile(file, true, StandardCharsets.UTF_8));
	}

	@Test
	public void trimLeadingFileSeparator() {
		assertEquals("a/b", FileUtilities.trimLeadingFileSeparator("/a/b"));
		assertEquals("/a", FileUtilities.trimLeadingFileSeparator("//a"));
		assertEquals("a", FileUtilities.trimLeadingFileSeparator("a"));
		assertNull(FileUtilities.trimLeadingFileSeparator(null));
	}

	@Test
	public void trimTrailingFileSeparator() {
		assertEquals("a/b", FileUtilities.trimTrailingFileSeparator("a/b/"));
		assertEquals("a/", FileUtilities.trimTrailingFileSeparator("a//"));
		assertEquals("a", FileUtilities.trimTrailingFileSeparator("a"));
		assertNull(FileUtilities.trimTrailingFileSeparator(null));
	}

	@Test
	public void getFilenameFromPath() {
		assertEquals("c.txt", FileUtilities.getFilenameFromPath("a/b/c.txt"));
		assertEquals("c.txt", FileUtilities.getFilenameFromPath("/c.txt"));
		assertEquals("c.txt", FileUtilities.getFilenameFromPath("c.txt"));
		assertEquals("", FileUtilities.getFilenameFromPath("a/"));
		assertNull(FileUtilities.getFilenameFromPath(null));
	}

	@Test
	public void getParentFromPath() {
		assertEquals("/a", FileUtilities.getParentFromPath("/a/b"));
		assertEquals("a", FileUtilities.getParentFromPath("a/b"));
		assertEquals("/", FileUtilities.getParentFromPath("/a"));
		assertEquals("/", FileUtilities.getParentFromPath("/"));
		assertEquals("", FileUtilities.getParentFromPath("a"));
		assertEquals("", FileUtilities.getParentFromPath(null));
	}

	@Test
	public void delete_directoryTree() throws Exception {
		createFile("root/a.txt", "a");
		createFile("root/sub/b.txt", "b");
		final File root = tempDir.resolve("root").toFile();

		assertTrue(FileUtilities.delete(root));

		assertFalse(root.exists());
	}

	@Test
	public void delete_singleFile() throws Exception {
		final File file = createFile("a.txt", "a");

		assertTrue(FileUtilities.delete(file));

		assertFalse(file.exists());
	}

	@Test
	public void delete_symbolicLinkTargetIsKept() throws Exception {
		final File target = createFile("target/keep.txt", "keep");
		createFile("root/a.txt", "a");
		createSymbolicLinkOrSkip(tempDir.resolve("root/link"), tempDir.resolve("target"));

		assertTrue(FileUtilities.delete(tempDir.resolve("root").toFile()));

		assertFalse(tempDir.resolve("root").toFile().exists());
		assertTrue(target.exists());
	}

	@Test
	public void delete_missingFile() {
		assertFalse(FileUtilities.delete(tempDir.resolve("missing").toFile()));
	}

	@Test
	public void removePropertiesCommentLines() {
		final String properties = ""
				+ "# comment\n"
				+ "key=value\n"
				+ "  ! comment 2\n"
				+ "key2=a\\\n"
				+ "# no comment, but continued value\n"
				+ "key3=b\\\\\n"
				+ "# comment after escaped backslash\n"
				+ "\t\f#comment after whitespace";

		assertEquals(""
				+ "\n"
				+ "key=value\n"
				+ "\n"
				+ "key2=a\\\n"
				+ "# no comment, but continued value\n"
				+ "key3=b\\\\\n"
				+ "\n", FileUtilities.removePropertiesCommentLines(properties));
	}

	@Test
	public void removePropertiesCommentLines_keepsLinebreaks() {
		assertEquals("\r\na=1\r\n\rb=2", FileUtilities.removePropertiesCommentLines("#x\r\na=1\r\n!y\rb=2"));
	}

	@Test
	public void removePropertiesCommentLines_commentIsNeverContinued() {
		assertEquals("\n\na=1", FileUtilities.removePropertiesCommentLines("# c\\\n# d\na=1"));
	}

	@Test
	public void removePropertiesCommentLines_valueContainingHash() {
		assertEquals("a=#1\n", FileUtilities.removePropertiesCommentLines("a=#1\n"));
		assertEquals("", FileUtilities.removePropertiesCommentLines(""));
	}

	@Test
	public void isValidFilePath() {
		assertFalse(FileUtilities.isValidFilePath(null));
		assertFalse(FileUtilities.isValidFilePath(""));
		assertFalse(FileUtilities.isValidFilePath("   "));
		assertFalse(FileUtilities.isValidFilePath("a\u0000b"));
		assertTrue(FileUtilities.isValidFilePath("dir/file.txt"));
		assertTrue(FileUtilities.isValidFilePath(tempDir.resolve("does-not-exist.txt").toString()));
	}

	@Test
	public void getFileTypeByExtension() {
		assertFileType(FileDataType.CSV, null, "data.csv");
		assertFileType(FileDataType.CSV, null, "data.tsv");
		assertFileType(FileDataType.CSV, null, "data.dsv");
		assertFileType(FileDataType.CSV, null, "data.del");
		assertFileType(FileDataType.CSV, FileCompressionType.ZIP, "data.CSV.ZIP");
		assertFileType(FileDataType.JSON, FileCompressionType.TARGZ, "data.json.tar.gz");
		assertFileType(FileDataType.YAML, FileCompressionType.TGZ, "data.yaml.tgz");
		assertFileType(FileDataType.XML, FileCompressionType.GZ, "data.xml.gz");
		assertFileType(FileDataType.EXCEL, null, "data.xls");
		assertFileType(FileDataType.EXCEL, null, "data.xlsx");
		assertFileType(FileDataType.JSON, null, "data.jsn");
		assertFileType(FileDataType.YAML, null, "data.yml");
		assertFileType(FileDataType.KDBX, null, "data.kdbx");
		assertFileType(FileDataType.ODS, null, "data.ods");
		assertFileType(FileDataType.SQL, null, "data.sql");
		assertFileType(FileDataType.VCF, null, "data.vcf");
		assertFileType(null, null, "data.txt");
		assertFileType(null, FileCompressionType.ZIP, "data.zip");
		assertFileType(null, FileCompressionType.GZ, "data.gz");
	}

	@Test
	public void detectFileCharset_bom() throws Exception {
		final File file = createFile("bom.txt", concat(UTF8_BOM, "abc".getBytes(StandardCharsets.UTF_8)));

		assertEquals(StandardCharsets.UTF_8, FileUtilities.detectFileCharset(file, StandardCharsets.ISO_8859_1));
	}

	@Test
	public void detectFileCharset_byUmlauts() throws Exception {
		final File utf8File = createFile("utf8.txt", "Grüße".getBytes(StandardCharsets.UTF_8));
		final File isoFile = createFile("iso.txt", "Grüße".getBytes(StandardCharsets.ISO_8859_1));

		assertEquals(StandardCharsets.UTF_8, FileUtilities.detectFileCharset(utf8File, StandardCharsets.UTF_8, StandardCharsets.ISO_8859_1));
		assertEquals(StandardCharsets.ISO_8859_1, FileUtilities.detectFileCharset(isoFile, StandardCharsets.UTF_8, StandardCharsets.ISO_8859_1));
		assertNull(FileUtilities.detectFileCharset(isoFile, StandardCharsets.UTF_8));
	}

	@Test
	public void detectFileCharset_undetectable() throws Exception {
		assertNull(FileUtilities.detectFileCharset(createFile("empty.txt", ""), StandardCharsets.UTF_8));
		assertNull(FileUtilities.detectFileCharset(createFile("ascii.txt", "abc"), StandardCharsets.UTF_8, StandardCharsets.ISO_8859_1));
	}

	@Test
	public void detectFileCharset_missingFile() {
		assertThrows(FileNotFoundException.class, () -> FileUtilities.detectFileCharset(tempDir.resolve("missing").toFile(), StandardCharsets.UTF_8));
	}

	@Test
	public void subarray() {
		final byte[] array = { 1, 2, 3, 4 };

		assertNull(FileUtilities.subarray(null, 0, 1));
		assertArrayEquals(new byte[] { 2, 3 }, FileUtilities.subarray(array, 1, 3));
		assertArrayEquals(new byte[] { 1, 2 }, FileUtilities.subarray(array, -5, 2));
		assertArrayEquals(new byte[] { 3, 4 }, FileUtilities.subarray(array, 2, 99));
		assertArrayEquals(new byte[0], FileUtilities.subarray(array, 3, 1));
		assertArrayEquals(new byte[0], FileUtilities.subarray(array, 10, 20));
	}

	private File createFile(final String relativePath, final String content) throws IOException {
		return createFile(relativePath, content.getBytes(StandardCharsets.UTF_8));
	}

	private File createFile(final String relativePath, final byte[] content) throws IOException {
		final Path path = tempDir.resolve(relativePath);
		Files.createDirectories(path.getParent());
		Files.write(path, content);
		return path.toFile();
	}

	private void createSymbolicLinkOrSkip(final Path link, final Path target) {
		try {
			Files.createSymbolicLink(link, target);
		} catch (@SuppressWarnings("unused") final IOException | UnsupportedOperationException e) {
			// Creating symbolic links needs special privileges on Windows
			Assumptions.assumeTrue(false, "Symbolic links are not supported");
		}
	}

	private static String readUtf8(final File file) throws IOException {
		return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
	}

	private static List<String> readLines(final String data, final boolean keepLinebreakInLines) throws Exception {
		return FileUtilities.readLinesFromStream(new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8)), keepLinebreakInLines, StandardCharsets.UTF_8);
	}

	private static List<String> sortedNames(final File[] files) {
		return sortedNames(Arrays.asList(files));
	}

	private static List<String> sortedNames(final List<File> files) {
		final List<String> names = new ArrayList<>();
		for (final File file : files) {
			names.add(file.getName());
		}
		names.sort(null);
		return names;
	}

	private static byte[] concat(final byte[] first, final byte[] second) {
		final byte[] result = Arrays.copyOf(first, first.length + second.length);
		System.arraycopy(second, 0, result, first.length, second.length);
		return result;
	}

	private static void assertFileType(final FileDataType expectedDataType, final FileCompressionType expectedCompressionType, final String filePath) {
		final FileType fileType = FileUtilities.getFileTypeByExtension(filePath);
		assertEquals(expectedDataType, fileType.getFileDataType(), filePath);
		assertEquals(expectedCompressionType, fileType.getFileCompressionType(), filePath);
	}
}
