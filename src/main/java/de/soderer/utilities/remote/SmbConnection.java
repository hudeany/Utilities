package de.soderer.utilities.remote;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import com.hierynomus.msdtyp.AccessMask;
import com.hierynomus.msdtyp.FileTime;
import com.hierynomus.msfscc.FileAttributes;
import com.hierynomus.msfscc.fileinformation.FileAllInformation;
import com.hierynomus.msfscc.fileinformation.FileBasicInformation;
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation;
import com.hierynomus.mssmb.SMB1NotSupportedException;
import com.hierynomus.mssmb2.SMB2CreateDisposition;
import com.hierynomus.mssmb2.SMB2ShareAccess;
import com.hierynomus.protocol.commons.EnumWithValue;
import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.SmbConfig;
import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.connection.Connection;
import com.hierynomus.smbj.session.Session;
import com.hierynomus.smbj.share.DiskShare;
import com.hierynomus.smbj.share.File;

import de.soderer.utilities.FileUtilities;
import de.soderer.utilities.IoUtilities;
import de.soderer.utilities.ServerAccessData;
import de.soderer.utilities.Tuple;
import de.soderer.utilities.Utilities;

/**
 * SMB2/3 remote connection based on smbj.
 *
 * Lost connections (network drop, server restart) are detected and re-established
 * automatically on the next operation, as long as close() has not been called.
 */
public class SmbConnection implements RemoteConnection {
	/** The host. */
	private String hostname = null;

	/** The port. */
	private int port = 445;

	/** The user. */
	private String username = null;

	/** The password. Kept for automatic reconnect, wiped on close(). */
	private char[] password = null;

	/** The share name. */
	private String sharename = null;

	/** Set by close(); prevents any further automatic reconnect. */
	private boolean closed = false;

	/**
	 * Currently connected client
	 */
	private SMBClient client = null;
	private Connection connection = null;
	private Session session = null;
	private DiskShare diskShare = null;

	public SmbConnection(final String hostname, final int port, final String username, final String sharename, final char[] password) throws Exception {
		this.hostname = hostname;
		if (0 < port && port <= 65535) {
			this.port = port;
		}
		this.username = username;
		this.sharename = sharename;
		this.password = password == null ? null : password.clone();

		connect();
	}

	public SmbConnection(final ServerAccessData serverEntry) throws Exception {
		if (!"smb".equalsIgnoreCase(serverEntry.getProtocol())) {
			throw new Exception("Invalid protocol '" + serverEntry.getProtocol() + "' for smb connection. Only 'smb' is allowed");
		} else {
			hostname = serverEntry.getHostname();
			if (0 < serverEntry.getPort() && serverEntry.getPort() <= 65535) {
				port = serverEntry.getPort();
			}
			username = serverEntry.getUsername();
			sharename = serverEntry.getSharename();
			password = serverEntry.getPassword() == null ? null : serverEntry.getPassword().clone();

			connect();
		}
	}

	/**
	 * Connect.
	 */
	private void connect() throws Exception {
		try {
			final SmbConfig smbConfig = SmbConfig.builder().withMultiProtocolNegotiate(true).build();
			client = new SMBClient(smbConfig);
			connection = client.connect(hostname, port);
			session = connection.authenticate(new AuthenticationContext(username, password == null ? new char[0] : password, ""));
			diskShare = (DiskShare) session.connectShare(sharename == null ? "" : sharename);
		} catch (final Exception e) {
			disconnect();

			// Check the exception itself and its cause chain for missing SMB2/3 support
			int level = 0;
			Throwable causeException = e;
			while (causeException != null && level < 10) {
				if (causeException instanceof SMB1NotSupportedException) {
					throw new Exception("SMBv1 is not supported", e);
				}
				if (causeException.getCause() == causeException) {
					break;
				}
				causeException = causeException.getCause();
				level++;
			}

			// Keep the original exception as cause, so callers can tell unreachable host, auth failure and missing share apart
			throw new Exception("Connection to smb://" + hostname + ":" + port + "/" + (sharename == null ? "" : sharename) + " failed: " + e.getMessage(), e);
		}
	}

	/**
	 * Checks whether the connection is still usable and reconnects if it was lost.
	 */
	private void checkForConnection() throws Exception {
		if (closed) {
			throw new Exception("SmbConnection is closed");
		}

		if (client == null
				|| connection == null || !connection.isConnected()
				|| diskShare == null || !diskShare.isConnected()) {
			disconnect();
			connect();
		}
	}

	/**
	 * Normalizes a path to the smbj convention: forward slashes, no leading or trailing separator.
	 */
	private static String normalizePath(final String path) {
		if (path == null) {
			return "";
		} else {
			final String normalized = FileUtilities.trimLeadingFileSeparator(FileUtilities.trimTrailingFileSeparator(path.replace("\\", "/")));
			return normalized == null ? "" : normalized;
		}
	}

	private static String getLastPathElement(final String normalizedPath) {
		if (normalizedPath.contains("/")) {
			return normalizedPath.substring(normalizedPath.lastIndexOf("/") + 1);
		} else {
			return normalizedPath;
		}
	}

	private static ZonedDateTime toZonedDateTime(final FileTime fileTime) {
		return Instant.ofEpochMilli(fileTime.toEpochMillis()).atZone(ZoneId.systemDefault());
	}

	@Override
	public void deleteFolder(final String folderPath) throws Exception {
		checkForConnection();
		diskShare.rmdir(normalizePath(folderPath), true);
	}

	@Override
	public void createFolder(final String parentFolderPath, final String folderName) throws Exception {
		checkForConnection();
		final String parent = normalizePath(parentFolderPath);
		diskShare.mkdir((parent.length() > 0 ? parent + "/" : "") + folderName);
	}

	/**
	 * Creates a folder including all missing parent folders (like mkdir -p).
	 */
	public void createFolderPath(final String folderPath) throws Exception {
		checkForConnection();
		final String normalized = normalizePath(folderPath);
		if (normalized.length() == 0) {
			return;
		}

		final StringBuilder currentPath = new StringBuilder();
		for (final String pathElement : normalized.split("/")) {
			if (pathElement.length() == 0) {
				continue;
			}
			if (currentPath.length() > 0) {
				currentPath.append("/");
			}
			currentPath.append(pathElement);

			if (!diskShare.folderExists(currentPath.toString())) {
				diskShare.mkdir(currentPath.toString());
			}
		}
	}

	@Override
	public void deleteFile(final String filePath) throws Exception {
		checkForConnection();
		diskShare.rm(normalizePath(filePath));
	}

	public boolean fileExists(final String filePath) throws Exception {
		checkForConnection();
		return diskShare.fileExists(normalizePath(filePath));
	}

	public boolean folderExists(final String folderPath) throws Exception {
		checkForConnection();
		return diskShare.folderExists(normalizePath(folderPath));
	}

	@Override
	public FileInfo getFileInfo(final String filePath) throws Exception {
		checkForConnection();
		final String normalized = normalizePath(filePath);
		final FileAllInformation fileAllInformation = diskShare.getFileInformation(normalized);
		// Use end-of-file (logical size), not allocation size (cluster-rounded)
		return new FileInfo(getLastPathElement(normalized), fileAllInformation.getStandardInformation().getEndOfFile(), toZonedDateTime(fileAllInformation.getBasicInformation().getLastWriteTime()));
	}

	@Override
	public InputStream downloadFile(final String filePath) throws Exception {
		checkForConnection();
		@SuppressWarnings("resource")
		final File remoteSmbjFile = diskShare.openFile(normalizePath(filePath), EnumSet.of(AccessMask.GENERIC_READ), null, SMB2ShareAccess.ALL, SMB2CreateDisposition.FILE_OPEN, null);
		try {
			return new SmbInputStream(remoteSmbjFile.getInputStream(), remoteSmbjFile);
		} catch (final Exception e) {
			// Do not leak the open file handle if the stream cannot be created
			remoteSmbjFile.close();
			throw e;
		}
	}

	/**
	 * Uploads a new file. Fails if the file already exists (FILE_CREATE is an atomic exclusive create),
	 * which also makes this method usable for creating lock files.
	 */
	@Override
	public void uploadFile(final String filePath, final InputStream fileInputStream) throws Exception {
		checkForConnection();
		// GENERIC_WRITE is sufficient; GENERIC_ALL would also require WRITE_DAC/WRITE_OWNER and fail for plain write permission
		try (File newFile = diskShare.openFile(normalizePath(filePath), EnumSet.of(AccessMask.GENERIC_WRITE), null, SMB2ShareAccess.ALL, SMB2CreateDisposition.FILE_CREATE, null);
				OutputStream outputStream = newFile.getOutputStream()) {
			IoUtilities.copy(fileInputStream, outputStream);
		}
	}

	/**
	 * Renames or moves a file within the same share.
	 *
	 * @param toPath target path relative to the share root
	 * @param replaceExisting if true an existing target file is replaced atomically
	 */
	public void renameFile(final String fromPath, final String toPath, final boolean replaceExisting) throws Exception {
		checkForConnection();
		// DELETE access is required on the source for a rename
		try (File file = diskShare.openFile(normalizePath(fromPath), EnumSet.of(AccessMask.DELETE, AccessMask.GENERIC_READ), null, SMB2ShareAccess.ALL, SMB2CreateDisposition.FILE_OPEN, null)) {
			file.rename(normalizePath(toPath), replaceExisting);
		}
	}

	/**
	 * Sets the last write time of a file, leaving all other timestamps and attributes untouched.
	 */
	public void setLastModified(final String filePath, final long epochMillis) throws Exception {
		checkForConnection();
		try (File file = diskShare.openFile(normalizePath(filePath), EnumSet.of(AccessMask.FILE_WRITE_ATTRIBUTES), null, SMB2ShareAccess.ALL, SMB2CreateDisposition.FILE_OPEN, null)) {
			// Attribute value 0 means "do not change attributes"
			file.setFileInformation(new FileBasicInformation(
					FileBasicInformation.DONT_SET,
					FileBasicInformation.DONT_SET,
					FileTime.ofEpochMillis(epochMillis),
					FileBasicInformation.DONT_SET,
					0));
		}
	}

	@Override
	public FolderInfo getFolderInfo(final String folderPath) throws Exception {
		checkForConnection();
		final String normalized = normalizePath(folderPath);
		String folderName = getLastPathElement(normalized);
		if (Utilities.isEmpty(folderName)) {
			folderName = "/";
		}

		// Query the folder itself directly instead of listing its whole content to find the "." entry
		final FileAllInformation fileAllInformation = diskShare.getFileInformation(normalized);
		return new FolderInfo(folderName, toZonedDateTime(fileAllInformation.getBasicInformation().getLastWriteTime()));
	}

	@Override
	public Tuple<List<FolderInfo>, List<FileInfo>> getDirectory(final String folderPath) throws Exception {
		checkForConnection();
		final String normalized = normalizePath(folderPath);

		final List<FolderInfo> folderInfoList = new ArrayList<>();
		final List<FileInfo> fileInfoList = new ArrayList<>();
		for (final FileIdBothDirectoryInformation entry : diskShare.list(normalized, "*")) {
			final String entryName = entry.getFileName();
			if (".".equals(entryName) || "..".equals(entryName)) {
				continue;
			}

			// Directory listing already contains size and timestamps, no need to open each entry
			final ZonedDateTime changeDate = toZonedDateTime(entry.getLastWriteTime());
			if (EnumWithValue.EnumUtils.isSet(entry.getFileAttributes(), FileAttributes.FILE_ATTRIBUTE_DIRECTORY)) {
				folderInfoList.add(new FolderInfo(entryName, changeDate));
			} else {
				// Use end-of-file (logical size), not allocation size (cluster-rounded)
				fileInfoList.add(new FileInfo(entryName, entry.getEndOfFile(), changeDate));
			}
		}

		return new Tuple<>(folderInfoList, fileInfoList);
	}

	/**
	 * Closes the connection permanently and wipes the stored password.
	 */
	@Override
	public void close() {
		closed = true;
		disconnect();
		if (password != null) {
			Arrays.fill(password, '\0');
			password = null;
		}
	}

	/**
	 * Releases all smbj resources without marking the connection as closed (used for reconnect).
	 */
	private void disconnect() {
		if (diskShare != null) {
			try {
				diskShare.close();
			} catch (@SuppressWarnings("unused") final Exception e) {
				// do nothing
			}
			diskShare = null;
		}

		if (session != null) {
			try {
				session.close();
			} catch (@SuppressWarnings("unused") final Exception e) {
				// do nothing
			}
			session = null;
		}

		if (connection != null) {
			try {
				connection.close();
			} catch (@SuppressWarnings("unused") final Exception e) {
				// do nothing
			}
			connection = null;
		}

		if (client != null) {
			try {
				client.close();
			} catch (@SuppressWarnings("unused") final Exception e) {
				// do nothing
			}
			client = null;
		}
	}

	/**
	 * InputStream wrapper that also closes the remote file handle.
	 */
	static class SmbInputStream extends FilterInputStream {
		private final File remoteSmbjFile;
		private boolean isClosed = false;

		protected SmbInputStream(final InputStream inputStream, final File remoteSmbjFile) {
			super(inputStream);
			this.remoteSmbjFile = remoteSmbjFile;
		}

		@Override
		public void close() throws IOException {
			if (isClosed) {
				return;
			}
			isClosed = true;
			try {
				super.close();
			} finally {
				// Always release the remote handle, even if closing the stream failed
				remoteSmbjFile.close();
			}
		}
	}
}
