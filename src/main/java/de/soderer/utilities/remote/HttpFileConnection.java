package de.soderer.utilities.remote;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.log4j.Logger;

import de.soderer.utilities.DateUtilities;
import de.soderer.utilities.FileUtilities;
import de.soderer.utilities.IoUtilities;
import de.soderer.utilities.ServerAccessData;
import de.soderer.utilities.Tuple;
import de.soderer.utilities.http.HttpUtilities;

/**
 * HttpFileConnection
 */
public class HttpFileConnection implements RemoteConnection {
	/** The Constant logger. */
	@SuppressWarnings("unused")
	private static final transient Logger logger = Logger.getLogger(HttpFileConnection.class);

	/** The useTLS. */
	private boolean useTLS = false;

	/** The host. */
	private String hostname = null;

	/** The startPath. */
	private String startPath = null;

	/** The user. */
	private String username = null;

	/** The password. */
	private String password = null;

	/** The port. */
	private int port = 80;

	public HttpFileConnection(final boolean useTLS, final String hostname, final String startPath) throws Exception {
		this.useTLS = useTLS;
		if (useTLS) {
			port = 443;
		}
		this.hostname = hostname;
		this.startPath = startPath;
	}

	public HttpFileConnection(final String hostname, final String startPath) throws Exception {
		this.hostname = hostname;
		this.startPath = startPath;
	}

	public HttpFileConnection(final boolean useTLS, final String hostname, final String startPath, final int port) throws Exception {
		this.useTLS = useTLS;
		this.hostname = hostname;
		this.startPath = startPath;
		this.port = port;
	}

	public HttpFileConnection(final String hostname, final String startPath, final int port) throws Exception {
		this.hostname = hostname;
		this.startPath = startPath;
		this.port = port;
	}

	public HttpFileConnection(final String hostname, final String startPath, final String username, final String password) throws Exception {
		this.hostname = hostname;
		this.startPath = startPath;
		this.username = username;
		this.password = password;
	}

	public HttpFileConnection(final boolean useTLS, final String hostname, final String startPath, final String username, final String password) throws Exception {
		this.useTLS = useTLS;
		if (useTLS) {
			port = 443;
		}
		this.hostname = hostname;
		this.startPath = startPath;
		this.username = username;
		this.password = password;
	}

	public HttpFileConnection(final boolean useTLS, final String hostname, final String startPath, final String username, final String password, final int port) throws Exception {
		this.useTLS = useTLS;
		this.hostname = hostname;
		this.startPath = startPath;
		this.username = username;
		this.password = password;
		this.port = port;
	}

	public HttpFileConnection(final String hostname, final String startPath, final String username, final String password, final int port) throws Exception {
		this.hostname = hostname;
		this.startPath = startPath;
		this.username = username;
		this.password = password;
		this.port = port;
	}

	public HttpFileConnection(final ServerAccessData serverEntry) throws Exception {
		if (!"http".equalsIgnoreCase(serverEntry.getProtocol()) && !"https".equalsIgnoreCase(serverEntry.getProtocol())) {
			throw new Exception("Invalid protocol '" + serverEntry.getProtocol() + "' for HTTP/HTTPS connection. Only 'http' and 'https' are allowed");
		} else {
			useTLS = "https".equalsIgnoreCase(serverEntry.getProtocol());
			hostname = serverEntry.getHostname();
			startPath = serverEntry.getSharename();
			username = serverEntry.getUsername();
			password = new String(serverEntry.getPassword());
			port = serverEntry.getPort();
		}
	}

	/**
	 * Put a file on the server.
	 *
	 * @param in the in
	 * @param dst the dst
	 */
	@Override
	public void uploadFile(final String filePath, final InputStream inputStream) throws Exception {
		throw new NotImplementedException("uploadFile is not supported by HttpFileConnection");
	}

	/**
	 * Gets the file.
	 *
	 * @param name the name
	 * @return the input stream
	 */
	@Override
	public InputStream downloadFile(String filePath) throws Exception {
		filePath = FileUtilities.trimLeadingFileSeparator(filePath);
		try {
			final URLConnection urlConnection = getUrl(filePath).openConnection();
			return new BufferedInputStream(urlConnection.getInputStream());
		} catch (final Exception e) {
			throw new Exception("Cannot download file '" + filePath + "': " + e.getMessage(), e);
		}
	}

	/**
	 * Close connection
	 */
	@Override
	public void close() {
		// do nothing
	}

	@Override
	public void deleteFile(final String filePath) throws Exception {
		throw new NotImplementedException("deleteFile is not supported by HttpFileConnection");
	}

	@Override
	public void deleteFolder(final String folderPath) throws Exception {
		throw new NotImplementedException("deleteFolder is not supported by HttpFileConnection");
	}

	@Override
	public void createFolder(final String parentFolderPath, final String folderName) throws Exception {
		throw new NotImplementedException("createFolder is not supported by HttpFileConnection");
	}

	@Override
	public FileInfo getFileInfo(String filePath) throws Exception {
		filePath = FileUtilities.trimLeadingFileSeparator(filePath);
		final String folderPath = FileUtilities.getParentFromPath(filePath);
		final String fileName = FileUtilities.getFilenameFromPath(filePath);
		try {
			final URLConnection urlConnection = getUrl(folderPath).openConnection();
			try (BufferedInputStream inputStream = new BufferedInputStream(urlConnection.getInputStream())) {
				final byte[] data = IoUtilities.toByteArray(inputStream);
				for (final String fileLine : getFileLines(data)) {
					final String fileNameFromLine = getEntryNameFromLine(fileLine);
					if (fileNameFromLine != null && fileNameFromLine.equals(fileName)) {
						final String fileInfoPart = fileLine.substring(fileLine.lastIndexOf("</a>") + 4).trim();
						final int separatorIndex = fileInfoPart.lastIndexOf(" ");
						final String changeDateString = fileInfoPart.substring(0, separatorIndex).trim();
						final Date modificationDate = new SimpleDateFormat(DateUtilities.YYYY_MM_DD_HH_MM).parse(changeDateString);
						final String sizeString = fileInfoPart.substring(separatorIndex + 1).trim().toLowerCase();
						final long fileSize = getFileSize(sizeString);
						return new FileInfo(fileName, fileSize, modificationDate);
					}
				}
				return null;
			}
		} catch (final Exception e) {
			throw new Exception("Cannot download file info for '" + filePath + "': " + e.getMessage(), e);
		}
	}

	@Override
	public FolderInfo getFolderInfo(String folderPath) throws Exception {
		folderPath = FileUtilities.trimLeadingFileSeparator(FileUtilities.trimTrailingFileSeparator(folderPath));
		final String folderName = FileUtilities.getFilenameFromPath(folderPath);
		try {
			final URLConnection urlConnection = getUrl(folderPath).openConnection();
			try (BufferedInputStream inputStream = new BufferedInputStream(urlConnection.getInputStream())) {
				// Check existence only. Modification date is not available in general.
				return new FolderInfo(folderName, null);
			}
		} catch (final Exception e) {
			throw new Exception("Cannot download directory data for '" + folderPath + "': " + e.getMessage(), e);
		}
	}

	private URL getUrl(final String path) throws IOException, MalformedURLException {
		return new URL(
				(useTLS ? "https://" : "http://")
				+ hostname
				+ (useTLS ? (port == 443 ? "" : ":" + port) : (port == 80 ? "" : ":" + port))
				+ (startPath != null ? "/" + startPath : "")
				+ "/" + path
				+ (username != null ? "?username=" + HttpUtilities.urlEncode(username, StandardCharsets.UTF_8) + (password != null ? "&password=" + HttpUtilities.urlEncode(password, StandardCharsets.UTF_8) : "") : "")
				);
	}

	private static List<String> getEntryLines(final byte[] data) throws Exception {
		final String rawDataString = new String(data, StandardCharsets.UTF_8);
		final int dataStart = rawDataString.indexOf("<pre>");
		if (dataStart >= 0) {
			final int dataEnd = rawDataString.indexOf("</pre>", dataStart + 5);
			if (dataEnd >= 0) {
				final String dataString = rawDataString.substring(dataStart + 5, dataEnd);
				final BufferedReader reader = new BufferedReader(new StringReader(dataString));
				final List<String> entryLines = new ArrayList<>();
				String nextLine;
				while ((nextLine = reader.readLine()) != null) {
					if (!nextLine.contains("alt=\"[PARENTDIR]\">")) {
						entryLines.add(nextLine);
					}
				}
				return entryLines;
			} else {
				return null;
			}
		} else {
			return null;
		}
	}

	private static String getEntryNameFromLine(final String line) {
		final int entryNameStart = line.indexOf("<a href=\"");
		if (entryNameStart >= 0) {
			final int entryNameEnd = line.indexOf("\"", entryNameStart + 9);
			if (entryNameEnd >= 0) {
				return line.substring(entryNameStart + 9, entryNameEnd);
			} else {
				return null;
			}
		} else {
			return null;
		}
	}

	private static List<String> getFileLines(final byte[] data) throws Exception {
		final List<String> fileLines = new ArrayList<>();
		for (final String nextLine : getEntryLines(data)) {
			if (nextLine.contains("alt=") && !nextLine.contains("alt=\"[DIR]\">")) {
				fileLines.add(nextLine);
			}
		}
		return fileLines;
	}

	private static List<String> getFolderNames(final byte[] data) throws Exception {
		final List<String> folderLines = new ArrayList<>();
		for (final String nextLine : getEntryLines(data)) {
			if (nextLine.contains("alt=\"[DIR]\">")) {
				final String folderName = FileUtilities.trimTrailingFileSeparator(getEntryNameFromLine(nextLine));
				folderLines.add(folderName);
			}
		}
		return folderLines;
	}

	/**
	 * This size data is very imprecise
	 */
	private static long getFileSize(final String sizeString) {
		long fileSize;
		if (sizeString.endsWith("k")) {
			fileSize = (long) (Double.parseDouble(sizeString.substring(0, sizeString.length() - 1)) * 1024);
			fileSize = (fileSize / 1000) * 1000;
		} else if (sizeString.endsWith("m")) {
			fileSize = (long) (Double.parseDouble(sizeString.substring(0, sizeString.length() - 1)) * 1024 * 1024);
			fileSize = (fileSize / (long) Math.pow(1000, 2)) * (long) Math.pow(1000, 2);
		} else if (sizeString.endsWith("g")) {
			fileSize = (long) (Double.parseDouble(sizeString.substring(0, sizeString.length() - 1)) * 1024 * 1024 * 1024);
			fileSize = (fileSize / (long) Math.pow(1000, 3)) * (long) Math.pow(1000, 3);
		} else if (sizeString.endsWith("t")) {
			fileSize = (long) (Double.parseDouble(sizeString.substring(0, sizeString.length() - 1)) * 1024 * 1024 * 1024 * 1024);
			fileSize = (fileSize / (long) Math.pow(1000, 4)) * (long) Math.pow(1000, 4);
		} else {
			fileSize = Long.parseLong(sizeString);
		}
		return fileSize;
	}

	@Override
	public Tuple<List<FolderInfo>, List<FileInfo>> getDirectory(String folderPath) throws Exception {
		folderPath = FileUtilities.trimLeadingFileSeparator(FileUtilities.trimTrailingFileSeparator(folderPath));
		try {
			final URLConnection urlConnection = getUrl(folderPath).openConnection();
			try (BufferedInputStream inputStream = new BufferedInputStream(urlConnection.getInputStream())) {
				final byte[] data = IoUtilities.toByteArray(inputStream);
				final List<FolderInfo> folderInfoList = new ArrayList<>();
				for (final String subFolderName : getFolderNames(data)) {
					folderInfoList.add(getFolderInfo(folderPath + "/" + subFolderName));
				}
				final List<FileInfo> fileInfoList = new ArrayList<>();
				for (final String fileLine : getFileLines(data)) {
					final String fileName = getEntryNameFromLine(fileLine);
					final String fileInfoPart = fileLine.substring(fileLine.lastIndexOf("</a>") + 4).trim();
					final int separatorIndex = fileInfoPart.lastIndexOf(" ");
					final String changeDateString = fileInfoPart.substring(0, separatorIndex).trim();
					final Date modificationDate = new SimpleDateFormat(DateUtilities.YYYY_MM_DD_HH_MM).parse(changeDateString);
					final String sizeString = fileInfoPart.substring(separatorIndex + 1).trim().toLowerCase();
					final long fileSize = getFileSize(sizeString);
					fileInfoList.add(new FileInfo(fileName, fileSize, modificationDate));
				}
				return new Tuple<>(folderInfoList, fileInfoList);
			}
		} catch (final Exception e) {
			throw new Exception("Cannot download directory data for '" + folderPath + "': " + e.getMessage(), e);
		}
	}
}
