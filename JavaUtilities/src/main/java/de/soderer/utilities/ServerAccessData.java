package de.soderer.utilities;

import java.io.IOException;
import java.util.List;

public class ServerAccessData {
	private String entryname;
	private String hostname;
	private int port;
	private String username;
	private String keyFile;
	private String password;
	private boolean checkHostKey;
	
	public ServerAccessData(String data) throws IOException {
		List<String> dataParts = CsvReader.parseCsvLine(',', '"', data);
		if (dataParts == null || dataParts.size() != 6) {
			throw new IOException("Invalid data in ServerEntry");
		} else {
			try {
				this.entryname = dataParts.get(0);
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

	public ServerAccessData(String entryname, String hostname, String username, String keyFile, String password) {
		this(entryname, hostname, 22, username, keyFile, password, false);
	}

	public ServerAccessData(String entryname, String hostname, int port, String username, String keyFile, String password, boolean checkHostKey) {
		this.entryname = entryname;
		this.hostname = hostname;
		this.port = port;
		this.username = username;
		this.keyFile = keyFile;
		this.password = password;
	}

	public String getEntryname() {
		return entryname;
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
	
	public String toString() {
		return CsvWriter.getCsvLine(',', '"', entryname, hostname, port, username, keyFile, password);
	}
	
	public String toDisplayString() {
		return CsvWriter.getCsvLine(',', '"', entryname, hostname, port, username, keyFile, password);
	}
	
	public boolean getCheckHostKey() {
		return checkHostKey;
	}
}