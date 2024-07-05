package de.soderer.utilities.remote;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;

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

import de.soderer.utilities.DateUtilities;
import de.soderer.utilities.FileUtilities;
import de.soderer.utilities.IoUtilities;
import de.soderer.utilities.ServerAccessData;
import de.soderer.utilities.Tuple;
import de.soderer.utilities.Utilities;

public class SmbConnection implements RemoteConnection {
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
		try (File remoteSmbjFile = diskShare.openFile(filePath, EnumSet.of(AccessMask.GENERIC_READ), null, SMB2ShareAccess.ALL, SMB2CreateDisposition.FILE_OPEN, null)) {
			final FileAllInformation fileAllInformation = remoteSmbjFile.getFileInformation();
			final ZonedDateTime changeDate = DateUtilities.getLocalDateTimeForDate(fileAllInformation.getBasicInformation().getLastWriteTime().toDate()).atZone(ZoneId.systemDefault());
			return new FileInfo(fileName, fileAllInformation.getStandardInformation().getAllocationSize(), changeDate);
		}
	}

	@Override
	public InputStream downloadFile(String filePath) throws Exception {
		checkForConnection();
		filePath = FileUtilities.trimLeadingFileSeparator(FileUtilities.trimTrailingFileSeparator(filePath.replace("\\", "/")));
		final File remoteSmbjFile = diskShare.openFile(filePath, EnumSet.of(AccessMask.GENERIC_READ), null, SMB2ShareAccess.ALL, SMB2CreateDisposition.FILE_OPEN, null);
		return new SmbInputStream(remoteSmbjFile.getInputStream(), remoteSmbjFile);
	}

	@Override
	public void uploadFile(String filePath, final InputStream fileInputStream) throws Exception {
		checkForConnection();
		filePath = FileUtilities.trimLeadingFileSeparator(FileUtilities.trimTrailingFileSeparator(filePath.replace("\\", "/")));
		try (File newFile = diskShare.openFile(filePath, new HashSet<>(Arrays.asList(new AccessMask[]{AccessMask.GENERIC_ALL})), null, SMB2ShareAccess.ALL, SMB2CreateDisposition.FILE_CREATE, null)) {
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

		ZonedDateTime modificationDate = null;

		for (final FileIdBothDirectoryInformation fileIdBothDirectoryInformation : diskShare.list(folderPath, "*")) {
			final String entryName = fileIdBothDirectoryInformation.getFileName();
			if (".".equals(entryName)) {
				try (File remoteSmbjFile = diskShare.openFile(folderPath + "/" + entryName, EnumSet.of(AccessMask.GENERIC_READ), null, SMB2ShareAccess.ALL, SMB2CreateDisposition.FILE_OPEN, null)) {
					final FileAllInformation fileAllInformation = remoteSmbjFile.getFileInformation();
					modificationDate = DateUtilities.getLocalDateTimeForDate(fileAllInformation.getBasicInformation().getLastWriteTime().toDate()).atZone(ZoneId.systemDefault());
				}
				break;
			}
		}

		return new FolderInfo(folderName, modificationDate);
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

	@Override
	public Tuple<List<FolderInfo>, List<FileInfo>> getDirectory(String folderPath) throws Exception {
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

		final List<FolderInfo> folderInfoList = new ArrayList<>();
		final List<FileInfo> fileInfoList = new ArrayList<>();
		for (final FileIdBothDirectoryInformation fileIdBothDirectoryInformation : diskShare.list(folderPath, "*")) {
			final String entryName = fileIdBothDirectoryInformation.getFileName();
			if (!".".equals(entryName) && !"..".equals(entryName)) {
				if (EnumWithValue.EnumUtils.isSet(fileIdBothDirectoryInformation.getFileAttributes(), FileAttributes.FILE_ATTRIBUTE_DIRECTORY)) {
					folderInfoList.add(getFolderInfo(folderPath + "/" + entryName));
				} else {
					try (File remoteSmbjFile = diskShare.openFile(folderPath + "/" + entryName, EnumSet.of(AccessMask.GENERIC_READ), null, SMB2ShareAccess.ALL, SMB2CreateDisposition.FILE_OPEN, null)) {
						final FileAllInformation fileAllInformation = remoteSmbjFile.getFileInformation();
						final ZonedDateTime changeDate = DateUtilities.getLocalDateTimeForDate(fileAllInformation.getBasicInformation().getLastWriteTime().toDate()).atZone(ZoneId.systemDefault());
						fileInfoList.add(new FileInfo(entryName, fileAllInformation.getStandardInformation().getAllocationSize(), changeDate));
					}
				}
			}
		}

		return new Tuple<>(folderInfoList, fileInfoList);
	}
}
