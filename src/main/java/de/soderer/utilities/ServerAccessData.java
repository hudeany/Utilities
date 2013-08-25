package de.soderer.utilities;

import java.io.IOException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class ServerAccessData extends SecureDataEntry{
	public static final String PRELUDE = "\\\\";
	
	private String hostname;
	private int port;
	private String username;
	private String keyFile;
	private String password;
	private boolean checkHostKey;
	
	protected ServerAccessData() {
	}

	public ServerAccessData(String entryname, String hostname, String username, String keyFile, String password) {
		this(entryname, hostname, 22, username, keyFile, password, false);
	}

	public ServerAccessData(String entryname, String hostname, int port, String username, String keyFile, String password, boolean checkHostKey) {
		this.entryName = entryname;
		this.hostname = hostname;
		this.port = port;
		this.username = username;
		this.keyFile = keyFile;
		this.password = password;
	}
	
	public void loadData(List<String> dataParts) throws Exception {
		if (dataParts == null || dataParts.size() != 6) {
			throw new IOException("Invalid data in ServerEntry");
		} else {
			try {
				this.entryName = dataParts.get(0);
				this.hostname = dataParts.get(1);
				this.port = Integer.parseInt(dataParts.get(2));
				this.username = dataParts.get(3);
				this.keyFile = dataParts.get(4);
				this.password = dataParts.get(5);
			} catch (NumberFormatException e) {
				throw new IOException("Invalid data in ServerEntry");
			}
		}
	}
	
	public String toStorageString() {
		return CsvWriter.getCsvLine(',', '"', entryName, hostname, port, username, keyFile, password);
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
	
	public String getPassword() {
		return password;
	}
	
	public boolean getCheckHostKey() {
		return checkHostKey;
	}
	
	public String toReferenceString() {
		return PRELUDE + entryName + ":";
	}
	
	public static String parseFilestringToServerEntryName(String fileName) throws Exception {
		if (StringUtils.isNotBlank(fileName) && fileName.startsWith(PRELUDE) && fileName.contains(":")) {
			return fileName.substring(PRELUDE.length(), fileName.lastIndexOf(":"));
		} else {
			throw new Exception("Invalid SSH-filename");
		}
	}
	
	public static String parseFilestringToFilePath(String fileName) throws Exception {
		if (StringUtils.isNotBlank(fileName) && fileName.startsWith(PRELUDE) && fileName.contains(":")) {
			return fileName.substring(fileName.lastIndexOf(":") + 1);
		} else {
			throw new Exception("Invalid SSH-filename");
		}
	}
	
	public boolean equals(ServerAccessData otherData) {
		return entryName.equals(otherData.getEntryName());
	}
}