package de.soderer.utilities;

public class ServerAccessData {
	public static final String PRELUDE = "\\\\";

	private String protocol;
	private String hostname;
	private String sharename;
	private int port = 0;
	private String username;
	private String keyFilePath;
	private char[] password;

	/**
	 * hostKey
	 * if null: do not check<br />
	 * if empty: check against local known host keys (~/.ssh/known_hosts)<br />
	 * if filepath: check against specific known host keys file<br />
	 * if filled with hash value: check against this fingerprint<br />
	 */
	private String hostKey;

	protected ServerAccessData() {
	}

	/**
	 * Constructor
	 *
	 * @param protocol
	 * @param hostname
	 * @param port
	 * @param domain
	 * @param sharename
	 * @param username
	 * @param keyFilePath
	 * @param password
	 * @param hostKey (used by ssh and sftp protocol only)<br />
	 * if null: do not check<br />
	 * if empty: check against local known host keys (~/.ssh/known_hosts)<br />
	 * if filepath: check against specific known host keys file<br />
	 * if filled with hash value: check against this fingerprint<br />
	 */
	public ServerAccessData(final String protocol, final String hostname, final int port, final String sharename, final String username, final String keyFilePath, final char[] password, final String hostKey) {
		if (Utilities.isBlank(protocol)) {
			throw new IllegalArgumentException("Invalid empty protocol");
		} else {
			this.protocol = protocol.trim().toLowerCase();
			if ("ssh".equalsIgnoreCase(this.protocol) || "sftp".equalsIgnoreCase(this.protocol)) {
				this.hostname = hostname;
				this.port = port;
				this.username = username;
				this.keyFilePath = keyFilePath;
				this.password = password;
				this.hostKey = hostKey;
				if (Utilities.isNotBlank(sharename)) {
					throw new IllegalArgumentException("Invalid argument sharename '" + sharename + "' for protocol: " + this.protocol);
				}
			} else 	if ("ftp".equalsIgnoreCase(this.protocol)) {
				this.hostname = hostname;
				this.port = port;
				this.username = username;
				this.password = password;
				if (Utilities.isNotBlank(keyFilePath)) {
					throw new IllegalArgumentException("Invalid argument keyFilePath '" + keyFilePath + "' for protocol: " + this.protocol);
				} else if (Utilities.isNotBlank(hostKey)) {
					throw new IllegalArgumentException("Invalid argument hostKey '" + hostKey + "' for protocol: " + this.protocol);
				} else if (Utilities.isNotBlank(sharename)) {
					throw new IllegalArgumentException("Invalid argument sharename '" + sharename + "' for protocol: " + this.protocol);
				}
			} else if ("smb".equalsIgnoreCase(this.protocol) || "smb1".equalsIgnoreCase(this.protocol)) {
				this.hostname = hostname;
				this.port = port;
				this.sharename = sharename;
				this.username = username;
				this.password = password;
				if (Utilities.isNotBlank(keyFilePath)) {
					throw new IllegalArgumentException("Invalid argument keyFilePath '" + keyFilePath + "' for protocol: " + this.protocol);
				} else if (Utilities.isNotBlank(hostKey)) {
					throw new IllegalArgumentException("Invalid argument hostKey '" + hostKey + "' for protocol: " + this.protocol);
				}
			} else if ("http".equalsIgnoreCase(this.protocol) || "https".equalsIgnoreCase(this.protocol)) {
				this.hostname = hostname;
				this.sharename = sharename;
				this.port = port;
				this.username = username;
				this.password = password;
				if (Utilities.isNotBlank(keyFilePath)) {
					throw new IllegalArgumentException("Invalid argument keyFilePath '" + keyFilePath + "' for protocol: " + this.protocol);
				} else if (Utilities.isNotBlank(hostKey)) {
					throw new IllegalArgumentException("Invalid argument hostKey '" + hostKey + "' for protocol: " + this.protocol);
				}
			} else {
				throw new IllegalArgumentException("Unsupported protocol: " + this.protocol);
			}
		}
	}

	public String getProtocol() {
		return protocol;
	}

	public String getHostname() {
		return hostname;
	}

	public int getPort() {
		if (port == 0) {
			if ("ssh".equalsIgnoreCase(protocol)) {
				return 22;
			} else if ("sftp".equalsIgnoreCase(protocol)) {
				return 22;
			} else if ("ftp".equalsIgnoreCase(protocol)) {
				return 21;
			} else if ("smb".equalsIgnoreCase(protocol)) {
				return 445;
			} else if ("smb1".equalsIgnoreCase(protocol)) {
				return 445;
			} else {
				return port;
			}
		} else {
			return port;
		}
	}

	public String getSharename() {
		return sharename;
	}

	public String getUsername() {
		return username;
	}

	public String getKeyFile() {
		return keyFilePath;
	}

	public char[] getPassword() {
		return password;
	}

	public String getHostKey() {
		return hostKey;
	}

	public static String parseFilestringToServerEntryName(final String fileName) throws Exception {
		if (Utilities.isNotBlank(fileName) && fileName.startsWith(PRELUDE) && fileName.contains(":")) {
			return fileName.substring(PRELUDE.length(), fileName.lastIndexOf(":"));
		} else {
			throw new Exception("Invalid SSH-filename");
		}
	}

	public static String parseFilestringToFilePath(final String fileName) throws Exception {
		if (Utilities.isNotBlank(fileName) && fileName.startsWith(PRELUDE) && fileName.contains(":")) {
			return fileName.substring(fileName.lastIndexOf(":") + 1);
		} else {
			throw new Exception("Invalid SSH-filename");
		}
	}

	@Override
	public String toString() {
		final StringBuilder returnValue = new StringBuilder();
		if (protocol != null) {
			returnValue.append(protocol);
		} else {
			returnValue.append("<No protocol defined>");
		}
		returnValue.append("://");
		if (username != null) {
			returnValue.append(username);
			returnValue.append("@");
		}
		if (hostname != null) {
			returnValue.append(hostname);
		} else {
			returnValue.append("<No host defined>");
		}
		if (port != 0) {
			returnValue.append(":");
			returnValue.append(port);
		}
		if ("smb".equalsIgnoreCase(protocol) || "smb1".equalsIgnoreCase(protocol)) {
			if (sharename != null) {
				returnValue.append("/");
				returnValue.append(sharename);
			}
		}
		return returnValue.toString();
	}
}