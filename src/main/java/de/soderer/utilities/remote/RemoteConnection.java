package de.soderer.utilities.remote;

import java.io.Closeable;
import java.io.InputStream;
import java.util.List;

import de.soderer.utilities.Tuple;

public interface RemoteConnection extends Closeable {
	Tuple<List<FolderInfo>, List<FileInfo>> getDirectory(String path) throws Exception;

	FolderInfo getFolderInfo(String folderPath) throws Exception;

	void deleteFolder(String folderPath) throws Exception;

	void createFolder(String parentFolderPath, String folderName) throws Exception;

	FileInfo getFileInfo(String filePath) throws Exception;

	void deleteFile(String filePath) throws Exception;

	InputStream downloadFile(String filePath) throws Exception;

	void uploadFile(String filePath, InputStream fileInputStream) throws Exception;
}