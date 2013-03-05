package de.soderer.utilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.io.IOUtils;

public class ServerEntryKeyStore {
	private File keystoreFile;
	private Map<String, ServerAccessData> serverEntries = new HashMap<String, ServerAccessData>();
	
	public ServerEntryKeyStore(File keystoreFile) {
		this.keystoreFile = keystoreFile;
	}
	
	public void loadKeyStore(char[] passwordArray) throws Exception {
		FileInputStream fis = null;
		try {
			KeyStore keyStore = KeyStore.getInstance("JCEKS");

			if (keystoreFile.exists()) {
				fis = new FileInputStream(keystoreFile);
			}
			keyStore.load(fis, passwordArray);
			
			serverEntries = new HashMap<String, ServerAccessData>();
			
			for (String keyAlias : Collections.list(keyStore.aliases())) {
				SecretKey secretKey = (SecretKey) keyStore.getKey(keyAlias, passwordArray);
				String data = new String(secretKey.getEncoded(), "UTF-8");
				ServerAccessData entry = new ServerAccessData(data);
				serverEntries.put(entry.getEntryname(), entry);
			}
		} finally {
			Arrays.fill(passwordArray, '\u0000');
			IOUtils.closeQuietly(fis);
		}
	}
	
	public void saveKeyStore(char[] passwordArray) {
		FileOutputStream fos = null;
		try {
			KeyStore keyStore = KeyStore.getInstance("JCEKS");
			
			keyStore.load(null, passwordArray);
			
			fos = new FileOutputStream(keystoreFile);
			for (Entry<String, ServerAccessData> entry : serverEntries.entrySet()) {
				keyStore.setKeyEntry(entry.getKey(), new SecretKeySpec(entry.getValue().toString().getBytes("UTF-8"), "AES"), passwordArray, null);
			}
			keyStore.store(fos, passwordArray);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			Arrays.fill(passwordArray, '\u0000');
			IOUtils.closeQuietly(fos);
		}
	}
	
	public Set<String> getServerEntryNames() {
		return serverEntries.keySet();
	}
	
	public void setEntry(String key, ServerAccessData entryData) {
		serverEntries.put(key, entryData);
	}
	
	public ServerAccessData getEntry(String selectionName) {
		return serverEntries.get(selectionName);
	}
	
	public void remove(String key) {
		serverEntries.remove(key);
	}
}
