package de.soderer.utilities;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.log4j.Logger;

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

	@Override
	public List<String> getFiles(String folderPath) throws Exception {
		if (folderPath.startsWith("/")) {
			folderPath = folderPath.substring(1);
		}
		try {
			final URLConnection urlConnection = getUrl(folderPath).openConnection();
			try (BufferedInputStream inputStream = new BufferedInputStream(urlConnection.getInputStream());
					ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
				Utilities.copy(inputStream, outputStream);
				final List<String> returnList = new ArrayList<>();
				final byte[] data = outputStream.toByteArray();
				final String rawDataString = new String(data, "UTF-8");
				final int dataStart = rawDataString.indexOf("<pre>");
				if (dataStart >= 0) {
					final int dataEnd = rawDataString.indexOf("</pre>", dataStart + 5);
					if (dataEnd >= 0) {
						final String dataString = rawDataString.substring(dataStart + 5, dataEnd);
						final BufferedReader reader = new BufferedReader(new StringReader(dataString));
						String nextLine;
						while ((nextLine = reader.readLine()) != null) {
							if (!nextLine.contains("alt=\"[PARENTDIR]\">") && !nextLine.contains("alt=\"[DIR]\">")) {
								final int fileNameStart = nextLine.indexOf("<a href=\"");
								if (fileNameStart >= 0) {
									final int fileNameEnd = nextLine.indexOf("\"", fileNameStart + 9);
									if (fileNameEnd >= 0) {
										returnList.add(nextLine.substring(fileNameStart + 9, fileNameEnd));
									}
								}
							}
						}
					}
				}
				return returnList;
			}
		} catch (final Exception e) {
			throw new Exception("Cannot download directory data for '" + folderPath + "': " + e.getMessage(), e);
		}
	}

	@Override
	public List<String> getFolders(String folderPath) throws Exception {
		if (folderPath.startsWith("/")) {
			folderPath = folderPath.substring(1);
		}
		try {
			final URLConnection urlConnection = getUrl(folderPath).openConnection();
			try (BufferedInputStream inputStream = new BufferedInputStream(urlConnection.getInputStream());
					ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
				Utilities.copy(inputStream, outputStream);
				final List<String> returnList = new ArrayList<>();
				final byte[] data = outputStream.toByteArray();
				final String rawDataString = new String(data, "UTF-8");
				final int dataStart = rawDataString.indexOf("<pre>");
				if (dataStart >= 0) {
					final int dataEnd = rawDataString.indexOf("</pre>", dataStart + 5);
					if (dataEnd >= 0) {
						final String dataString = rawDataString.substring(dataStart + 5, dataEnd);
						final BufferedReader reader = new BufferedReader(new StringReader(dataString));
						String nextLine;
						while ((nextLine = reader.readLine()) != null) {
							if (!nextLine.contains("alt=\"[PARENTDIR]\">") && nextLine.contains("alt=\"[DIR]\">")) {
								final int folderNameStart = nextLine.indexOf("<a href=\"");
								if (folderNameStart >= 0) {
									final int folderNameEnd = nextLine.indexOf("\"", folderNameStart + 9);
									if (folderNameEnd >= 0) {
										String folderName = nextLine.substring(folderNameStart + 9, folderNameEnd);
										if (folderName.endsWith("/")) {
											folderName = folderName.substring(0, folderName.length() - 1);
										}
										returnList.add(folderName);
									}
								}
							}
						}
					}
				}
				return returnList;
			}
		} catch (final Exception e) {
			throw new Exception("Cannot download directory data for '" + folderPath + "': " + e.getMessage(), e);
		}
	}

	/**
	 * Check if folder exists
	 */
	@Override
	public boolean folderExists(String folderPath) throws Exception {
		if (folderPath.startsWith("/")) {
			folderPath = folderPath.substring(1);
		}
		if (folderPath.endsWith("/")) {
			folderPath = folderPath.substring(0, folderPath.length() - 1);
		}
		String parentFolderPath;
		String folderName;
		if (folderPath.contains("/")) {
			parentFolderPath = folderPath.substring(0, folderPath.lastIndexOf("/"));
			folderName = folderPath.substring(folderPath.lastIndexOf("/") + 1);
		} else {
			parentFolderPath = "";
			folderName = folderPath;
		}
		try {
			final URLConnection urlConnection = getUrl(parentFolderPath).openConnection();
			try (BufferedInputStream inputStream = new BufferedInputStream(urlConnection.getInputStream());
					ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
				Utilities.copy(inputStream, outputStream);
				final byte[] data = outputStream.toByteArray();
				final String rawDataString = new String(data, "UTF-8");
				final int dataStart = rawDataString.indexOf("<pre>");
				if (dataStart >= 0) {
					final int dataEnd = rawDataString.indexOf("</pre>", dataStart + 5);
					if (dataEnd >= 0) {
						final String dataString = rawDataString.substring(dataStart + 5, dataEnd);
						final BufferedReader reader = new BufferedReader(new StringReader(dataString));
						String nextLine;
						while ((nextLine = reader.readLine()) != null) {
							if (!nextLine.contains("alt=\"[PARENTDIR]\">") && nextLine.contains("alt=\"[DIR]\">") && (nextLine.contains("<a href=\"" + folderName + "\">") || nextLine.contains("<a href=\"" + folderName + "/\">"))) {
								return true;
							}
						}
						return false;
					} else {
						return false;
					}
				} else {
					return false;
				}
			}
		} catch (final Exception e) {
			throw new Exception("Cannot check folder exists for '" + folderPath + "': " + e.getMessage(), e);
		}
	}

	/**
	 * Check if file exists on sftp server
	 *
	 * @param filePath
	 * @return
	 * @throws Exception on invalid directory
	 */
	@Override
	public boolean fileExists(String filePath) throws Exception {
		if (filePath.startsWith("/")) {
			filePath = filePath.substring(1);
		}
		String folderPath;
		String fileName;
		if (filePath.contains("/")) {
			folderPath = filePath.substring(0, filePath.lastIndexOf("/"));
			fileName = filePath.substring(filePath.lastIndexOf("/") + 1);
		} else {
			folderPath = "";
			fileName = filePath;
		}
		try {
			final URLConnection urlConnection = getUrl(folderPath).openConnection();
			try (BufferedInputStream inputStream = new BufferedInputStream(urlConnection.getInputStream());
					ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
				Utilities.copy(inputStream, outputStream);
				final byte[] data = outputStream.toByteArray();
				final String rawDataString = new String(data, "UTF-8");
				final int dataStart = rawDataString.indexOf("<pre>");
				if (dataStart >= 0) {
					final int dataEnd = rawDataString.indexOf("</pre>", dataStart + 5);
					if (dataEnd >= 0) {
						final String dataString = rawDataString.substring(dataStart + 5, dataEnd);
						final BufferedReader reader = new BufferedReader(new StringReader(dataString));
						String nextLine;
						while ((nextLine = reader.readLine()) != null) {
							if (!nextLine.contains("alt=\"[PARENTDIR]\">") && !nextLine.contains("alt=\"[DIR]\">") && nextLine.contains("<a href=\"" + fileName + "\">")) {
								return true;
							}
						}
						return false;
					} else {
						return false;
					}
				} else {
					return false;
				}
			}
		} catch (final Exception e) {
			throw new Exception("Cannot check file exists for '" + filePath + "': " + e.getMessage(), e);
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
	public InputStream getFile(String filePath) throws Exception {
		if (filePath.startsWith("/")) {
			filePath = filePath.substring(1);
		}
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
	public RemoteFile getFileInfo(String filePath) throws Exception {
		if (filePath.startsWith("/")) {
			filePath = filePath.substring(1);
		}
		String folderPath;
		String fileName;
		if (filePath.contains("/")) {
			folderPath = filePath.substring(0, filePath.lastIndexOf("/"));
			fileName = filePath.substring(filePath.lastIndexOf("/") + 1);
		} else {
			folderPath = "";
			fileName = filePath;
		}
		try {
			final URLConnection urlConnection = getUrl(folderPath).openConnection();
			try (BufferedInputStream inputStream = new BufferedInputStream(urlConnection.getInputStream());
					ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
				Utilities.copy(inputStream, outputStream);
				final byte[] data = outputStream.toByteArray();
				final String rawDataString = new String(data, "UTF-8");
				final int dataStart = rawDataString.indexOf("<pre>");
				if (dataStart >= 0) {
					final int dataEnd = rawDataString.indexOf("</pre>", dataStart + 5);
					if (dataEnd >= 0) {
						final String dataString = rawDataString.substring(dataStart + 5, dataEnd);
						final BufferedReader reader = new BufferedReader(new StringReader(dataString));
						String nextLine;
						while ((nextLine = reader.readLine()) != null) {
							if (nextLine.contains("<a href=\"" + fileName + "\">")) {
								final String fileInfoPart = nextLine.substring(nextLine.lastIndexOf("</a>") + 4).trim();
								final int separatorIndex = fileInfoPart.lastIndexOf(" ");
								final String changeDateString = fileInfoPart.substring(0, separatorIndex).trim();
								final Date modificationDate = new SimpleDateFormat(DateUtilities.YYYY_MM_DD_HH_MM).parse(changeDateString);
								final String sizeString = fileInfoPart.substring(separatorIndex + 1).trim().toLowerCase();
								long fileSize;
								if (sizeString.endsWith("k")) {
									fileSize = (long) (Double.parseDouble(sizeString.substring(0, sizeString.length() - 1)) * 1024);
								} else if (sizeString.endsWith("m")) {
									fileSize = (long) (Double.parseDouble(sizeString.substring(0, sizeString.length() - 1)) * 1024 * 1024);
								} else if (sizeString.endsWith("g")) {
									fileSize = (long) (Double.parseDouble(sizeString.substring(0, sizeString.length() - 1)) * 1024 * 1024 * 1024);
								} else if (sizeString.endsWith("t")) {
									fileSize = (long) (Double.parseDouble(sizeString.substring(0, sizeString.length() - 1)) * 1024 * 1024 * 1024 * 1024);
								} else {
									fileSize = Long.parseLong(sizeString);
								}
								return new RemoteFile(filePath, fileSize, modificationDate);
							}
						}
						return null;
					} else {
						return null;
					}
				} else {
					return null;
				}
			}
		} catch (final Exception e) {
			throw new Exception("Cannot download file info for '" + filePath + "': " + e.getMessage(), e);
		}
	}

	private URL getUrl(final String path) throws IOException, MalformedURLException {
		return new URL(
				(useTLS ? "https://" : "http://")
				+ hostname
				+ (useTLS ? (port == 443 ? "" : ":" + port) : (port == 80 ? "" : ":" + port))
				+ (startPath != null ? "/" + startPath : "")
				+ "/" + path
				+ (username != null ? "?username=" + URLEncoder.encode(username, "UTF-8") + (password != null ? "&password=" + URLEncoder.encode(password, "UTF-8") : "") : "")
				);
	}
}
