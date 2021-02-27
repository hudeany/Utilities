package de.soderer.utilities.remote;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;

import org.apache.log4j.Logger;

import com.hierynomus.msdtyp.AccessMask;
import com.hierynomus.msfscc.FileAttributes;
import com.hierynomus.msfscc.fileinformation.FileAllInformation;
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
import de.soderer.utilities.Utilities;

public class SmbConnection implements RemoteConnection {
	/** The Constant logger. */
	@SuppressWarnings("unused")
	private static final transient Logger logger = Logger.getLogger(SmbConnection.class);

	/** The host. */
	private String hostname = null;

	/** The port. */
	private int port = 445;

	/** The user. */
	private String username = null;

	/** The password. */
	private char[] password = null;

	/** The domain. */
	private String sharename = null;

	/**
	 * Currently connected client
	 */
	private SMBClient client = null;
	private Connection connection = null;
	private Session session = null;
	private DiskShare diskShare = null;

	public SmbConnection(final String hostname, final int port, final String username, final String sharename, final char[] password) throws Exception {
		this.hostname = hostname;
		if (0 <= port && port <= 65535) {
			this.port = port;
		}
		this.username = username;
		this.sharename = sharename;
		this.password = password;

		connect();
	}

	public SmbConnection(final ServerAccessData serverEntry) throws Exception {
		if (!"smb".equalsIgnoreCase(serverEntry.getProtocol())) {
			throw new Exception("Invalid protocol '" + serverEntry.getProtocol() + "' for smb connection. Only 'smb' is allowed");
		} else {
			hostname = serverEntry.getHostname();
			if (0 <= serverEntry.getPort() && serverEntry.getPort() <= 65535) {
				port = serverEntry.getPort();
			}
			username = serverEntry.getUsername();
			sharename = serverEntry.getSharename();
			password = serverEntry.getPassword();

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
			session = connection.authenticate(new AuthenticationContext(username, password, ""));
			diskShare = (DiskShare) session.connectShare(sharename == null ? "" : sharename);
		} catch (final Exception e) {
			close();
			int level = 0;
			Throwable causeException = e;
			while (level < 10 && causeException.getCause() != null && causeException.getCause() != causeException) {
				if (causeException.getCause() instanceof SMB1NotSupportedException) {
					throw new Exception("SMBv1 is not supported");
				}
				causeException = causeException.getCause();
				level++;
			}
			throw new Exception("Connection failed");
		}
	}

	@Override
	public List<String> getFolders(String folderPath) throws Exception {
		checkForConnection();
		folderPath = FileUtilities.trimLeadingFileSeparator(FileUtilities.trimTrailingFileSeparator(folderPath.replace("\\", "/")));
		final List<String> folderNames = new ArrayList<>();
		for (final FileIdBothDirectoryInformation fileIdBothDirectoryInformation : diskShare.list(folderPath, "*")) {
			final String entryName = fileIdBothDirectoryInformation.getFileName();
			if (!".".equals(entryName) && !"..".equals(entryName)) {
				if (EnumWithValue.EnumUtils.isSet(fileIdBothDirectoryInformation.getFileAttributes(), FileAttributes.FILE_ATTRIBUTE_DIRECTORY)) {
					folderNames.add(entryName);
				}
			}
		}
		return folderNames;
	}

	@Override
	public boolean folderExists(String folderPath) throws Exception {
		checkForConnection();
		folderPath = FileUtilities.trimLeadingFileSeparator(FileUtilities.trimTrailingFileSeparator(folderPath.replace("\\", "/")));
		final String folderParentPath = FileUtilities.getParentFromPath(folderPath);
		final String folderName = FileUtilities.getFilenameFromPath(folderPath);
		try {
			for (final FileIdBothDirectoryInformation fileIdBothDirectoryInformation : diskShare.list(folderParentPath, folderName)) {
				return EnumWithValue.EnumUtils.isSet(fileIdBothDirectoryInformation.getFileAttributes(), FileAttributes.FILE_ATTRIBUTE_DIRECTORY);
			}
		} catch (@SuppressWarnings("unused") final Exception e) {
			return false;
		}
		return false;
	}

	@Override
	public void deleteFolder(String folderPath) throws Exception {
		checkForConnection();
		folderPath = FileUtilities.trimLeadingFileSeparator(FileUtilities.trimTrailingFileSeparator(folderPath.replace("\\", "/")));
		diskShare.rmdir(folderPath, true);
	}

	@Override
	public void createFolder(String parentFolderPath, final String folderName) throws Exception {
		checkForConnection();
		parentFolderPath = FileUtilities.trimLeadingFileSeparator(FileUtilities.trimTrailingFileSeparator(parentFolderPath.replace("\\", "/")));
		diskShare.mkdir((parentFolderPath != null && parentFolderPath.length() > 0 ? parentFolderPath + "/" : "") + folderName);
	}

	@Override
	public List<String> getFiles(String folderPath) throws Exception {
		checkForConnection();
		folderPath = FileUtilities.trimLeadingFileSeparator(FileUtilities.trimTrailingFileSeparator(folderPath.replace("\\", "/")));
		final List<String> fileNames = new ArrayList<>();
		for (final FileIdBothDirectoryInformation fileIdBothDirectoryInformation : diskShare.list(folderPath, "*")) {
			final String entryName = fileIdBothDirectoryInformation.getFileName();
			if (!".".equals(entryName) && !"..".equals(entryName)) {
				if (!EnumWithValue.EnumUtils.isSet(fileIdBothDirectoryInformation.getFileAttributes(), FileAttributes.FILE_ATTRIBUTE_DIRECTORY)) {
					fileNames.add(entryName);
				}
			}
		}
		return fileNames;
	}

	@Override
	public boolean fileExists(String filePath) throws Exception {
		checkForConnection();
		filePath = FileUtilities.trimLeadingFileSeparator(FileUtilities.trimTrailingFileSeparator(filePath.replace("\\", "/")));
		final int lastPathSeparator = filePath.replace("\\", "/").lastIndexOf("/");
		final String folderPath = lastPathSeparator >= 0 ? filePath.substring(0, lastPathSeparator) : "";
		final String fileName = lastPathSeparator >= 0 ? filePath.substring(lastPathSeparator + 1) : filePath;
		try {
			for (final FileIdBothDirectoryInformation fileIdBothDirectoryInformation : diskShare.list(folderPath, fileName)) {
				return !EnumWithValue.EnumUtils.isSet(fileIdBothDirectoryInformation.getFileAttributes(), FileAttributes.FILE_ATTRIBUTE_DIRECTORY);
			}
		} catch (@SuppressWarnings("unused") final Exception e) {
			return false;
		}
		return false;
	}

	@Override
	public void deleteFile(String filePath) throws Exception {
		checkForConnection();
		filePath = FileUtilities.trimLeadingFileSeparator(FileUtilities.trimTrailingFileSeparator(filePath.replace("\\", "/")));
		diskShare.rm(filePath);
	}

	@Override
	public FileInfo getFileInfo(String filePath) throws Exception {
		checkForConnection();
		filePath = FileUtilities.trimLeadingFileSeparator(FileUtilities.trimTrailingFileSeparator(filePath.replace("\\", "/")));
		String fileName;
		if (filePath.contains("/")) {
			fileName = filePath.substring(filePath.lastIndexOf("/") + 1);
		} else {
			fileName = filePath;
		}
		try (final File remoteSmbjFile = diskShare.openFile(filePath, EnumSet.of(AccessMask.GENERIC_READ), null, SMB2ShareAccess.ALL, SMB2CreateDisposition.FILE_OPEN, null)) {
			final FileAllInformation fileAllInformation = remoteSmbjFile.getFileInformation();
			return new FileInfo(fileName, fileAllInformation.getStandardInformation().getAllocationSize(), fileAllInformation.getBasicInformation().getLastWriteTime().toDate());
		}
	}

	@SuppressWarnings("resource")
	@Override
	public InputStream getFile(String filePath) throws Exception {
		checkForConnection();
		filePath = FileUtilities.trimLeadingFileSeparator(FileUtilities.trimTrailingFileSeparator(filePath.replace("\\", "/")));
		final File remoteSmbjFile = diskShare.openFile(filePath, EnumSet.of(AccessMask.GENERIC_READ), null, SMB2ShareAccess.ALL, SMB2CreateDisposition.FILE_OPEN, null);
		return new SmbInputStream(remoteSmbjFile.getInputStream(), remoteSmbjFile);
	}

	@Override
	public void uploadFile(String filePath, final InputStream fileInputStream) throws Exception {
		checkForConnection();
		filePath = FileUtilities.trimLeadingFileSeparator(FileUtilities.trimTrailingFileSeparator(filePath.replace("\\", "/")));
		try (final File newFile = diskShare.openFile(filePath, new HashSet<>(Arrays.asList(new AccessMask[]{AccessMask.GENERIC_ALL})), null, SMB2ShareAccess.ALL, SMB2CreateDisposition.FILE_CREATE, null)) {
			try (OutputStream outputStream = newFile.getOutputStream()) {
				IoUtilities.copy(fileInputStream, outputStream);
			}
		}
	}

	@Override
	public FolderInfo getFolderInfo(String folderPath) throws Exception {
		checkForConnection();
		folderPath = FileUtilities.trimLeadingFileSeparator(FileUtilities.trimTrailingFileSeparator(folderPath.replace("\\", "/")));
		String folderName;
		if (folderPath.contains("/")) {
			folderName = folderPath.substring(folderPath.lastIndexOf("/") + 1);
		} else {
			folderName = folderPath;
		}

		if (Utilities.isEmpty(folderName)) {
			folderName = "/";
		}

		Date modificationDate = null;

		final List<FolderInfo> folderInfoList = new ArrayList<>();
		final List<FileInfo> fileInfoList = new ArrayList<>();
		for (final FileIdBothDirectoryInformation fileIdBothDirectoryInformation : diskShare.list(folderPath, "*")) {
			final String entryName = fileIdBothDirectoryInformation.getFileName();
			if (".".equals(entryName)) {
				try (final File remoteSmbjFile = diskShare.openFile(folderPath + "/" + entryName, EnumSet.of(AccessMask.GENERIC_READ), null, SMB2ShareAccess.ALL, SMB2CreateDisposition.FILE_OPEN, null)) {
					final FileAllInformation fileAllInformation = remoteSmbjFile.getFileInformation();
					modificationDate = fileAllInformation.getBasicInformation().getLastWriteTime().toDate();
				}
			} else if (!"..".equals(entryName)) {
				if (EnumWithValue.EnumUtils.isSet(fileIdBothDirectoryInformation.getFileAttributes(), FileAttributes.FILE_ATTRIBUTE_DIRECTORY)) {
					folderInfoList.add(getFolderInfo(folderPath + "/" + entryName));
				} else {
					try (final File remoteSmbjFile = diskShare.openFile(folderPath + "/" + entryName, EnumSet.of(AccessMask.GENERIC_READ), null, SMB2ShareAccess.ALL, SMB2CreateDisposition.FILE_OPEN, null)) {
						final FileAllInformation fileAllInformation = remoteSmbjFile.getFileInformation();
						fileInfoList.add(new FileInfo(entryName, fileAllInformation.getStandardInformation().getAllocationSize(), fileAllInformation.getBasicInformation().getLastWriteTime().toDate()));
					}
				}
			}
		}

		return new FolderInfo(folderName, folderInfoList, fileInfoList, modificationDate);
	}

	@Override
	public void close() {
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

	private void checkForConnection() throws Exception {
		if (client == null) {
			throw new Exception("SmbConnection is not connected");
		}
	}

	class SmbInputStream extends FilterInputStream {
		private final File remoteSmbjFile;
		private boolean isClosed = false;

		protected SmbInputStream(final InputStream inputStream, final File remoteSmbjFile) {
			super(inputStream);
			this.remoteSmbjFile = remoteSmbjFile;
		}

		@Override
		public void close() throws IOException {
			super.close();
			if (!isClosed) {
				remoteSmbjFile.close();
			}

			isClosed = true;
		}
	}
}
