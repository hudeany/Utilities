package de.soderer.utilities;

import java.io.Closeable;
import java.io.InputStream;
import java.util.List;

public interface RemoteConnection extends Closeable {
	List<String> getFolders(String folderPath) throws Exception;

	boolean folderExists(String folderPath) throws Exception;

	RemoteFolderInfo getFolderInfo(String folderPath) throws Exception;

	void deleteFolder(String folderPath) throws Exception;

	void createFolder(String parentFolderPath, String folderName) throws Exception;

	List<String> getFiles(String folderPath) throws Exception;

	boolean fileExists(String filePath) throws Exception;

	void deleteFile(String filePath) throws Exception;

	RemoteFileInfo getFileInfo(String filePath) throws Exception;

	InputStream getFile(String filePath) throws Exception;

	void uploadFile(String filePath, InputStream fileInputStream) throws Exception;

	void close();
}