package de.soderer.utilities;

public class ServerAccessData {
	public static final String PRELUDE = "\\\\";

	private String protocol;
	private String hostname;
	private int port;
	private String username;
	private String keyFile;
	private char[] password;
	private boolean checkHostKey;

	protected ServerAccessData() {
	}

	public ServerAccessData(final String protocol, final String hostname, final int port, final String username, final String keyFile, final char[] password, final boolean checkHostKey) {
		this.protocol = protocol;
		this.hostname = hostname;
		this.port = port;
		this.username = username;
		this.keyFile = keyFile;
		this.password = password;
		this.checkHostKey = checkHostKey;
	}

	public String getProtocol() {
		return protocol;
	}

	public String getHostname() {
		return hostname;
	}

	public int getPort() {
		return port;
	}

	public String getUsername() {
		return username;
	}

	public String getKeyFile() {
		return keyFile;
	}

	public char[] getPassword() {
		return password;
	}

	public boolean getCheckHostKey() {
		return checkHostKey;
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
		return returnValue.toString();
	}
}