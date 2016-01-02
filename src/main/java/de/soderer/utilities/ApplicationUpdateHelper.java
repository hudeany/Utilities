package de.soderer.utilities;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.Date;
import java.util.concurrent.ExecutionException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import de.soderer.utilities.UserError.Reason;
import de.soderer.utilities.xml.XmlUtilities;

public class ApplicationUpdateHelper implements WorkerParentSimple {
	private String applicationName;
	private Version applicationVersion;
	private String versionIndexFileLocation;
	private UpdateParent updateParent;
	private FileDownloadWorker fileDownloadWorker;
	private String updateFileLocation;
	private String updateFileMd5Checksum = null;
	
	/**
	 * @param versionIndexFileLocation may include placeholders:<br />
	 *	&lt;system> : replaced by "windows" or "linux"<br />
	 * 	&lt;bitmode> : replaced by "32" or "64"<br />
	 * 	&lt;username> : interactive username dialog<br />
	 * 	&lt;password> : interactive password dialog
	 * @throws Exception 
	 */
	public ApplicationUpdateHelper(String applicationName, String applicationVersion, String versionIndexFileLocation, UpdateParent updateParent) throws Exception {
		if (versionIndexFileLocation == null || versionIndexFileLocation.trim().length() == 0) {
			throw new Exception("Invalid version index location");
		}
		
		this.applicationName = applicationName;
		this.applicationVersion = new Version(applicationVersion);
		this.versionIndexFileLocation = replacePlaceholders(versionIndexFileLocation);
		this.updateParent = updateParent;
	}
	
	public void executeUpdate() {
		if (askForUpdate(checkForNewVersionAvailable())) {
			updateApplication();
		}
	}

	private String checkForNewVersionAvailable() {
		try {
			if (!NetworkUtilities.checkForNetworkConnection()) {
				throw new Exception("error.missingNetworkConnection");
			} else if (!NetworkUtilities.ping(versionIndexFileLocation)) {
				throw new Exception("error.missingInternetConnection");
			}

			Document versionsDocument = null;
			if (versionIndexFileLocation.toLowerCase().startsWith("http")) {
				versionsDocument = XmlUtilities.downloadAndParseXmlFile(versionIndexFileLocation);
			} else if (new File(versionIndexFileLocation).exists()) {
				versionsDocument = XmlUtilities.parseXmlFile(new File(versionIndexFileLocation));
			}
			
			if (versionsDocument == null) {
				throw new Exception("Version index not found at location '" + versionIndexFileLocation + "'");
			}
			
			Node startFileNameNode = XmlUtilities.getSingleXPathNode(versionsDocument, "ApplicationVersions/" + applicationName);
			if (startFileNameNode == null) {
				throw new Exception("error.cannotFindUpdateVersionData");
			}
			Node versionNode = startFileNameNode.getAttributes().getNamedItem("version");
			String version = versionNode.getNodeValue();

			Node md5ChecksumNode = startFileNameNode.getAttributes().getNamedItem("md5CheckSum");
			if (md5ChecksumNode != null) {
				updateFileMd5Checksum = md5ChecksumNode.getNodeValue();
			}
			
			updateFileLocation = startFileNameNode.getFirstChild().getNodeValue();

			Version availableVersion = new Version(version);
			if (applicationVersion.compareTo(availableVersion) < 0 && updateFileLocation != null && updateFileLocation.trim().length() > 0) {
				updateFileLocation = replacePlaceholders(updateFileLocation);
				return version;
			} else {
				updateFileLocation = null;
				return null;
			}
		} catch (Exception e) {
			if (updateParent != null) {
				updateParent.showUpdateError("Update error:\n" + e.getMessage());
			} else {
				System.err.println("Update error:\n" + e.getMessage());
			}
			return null;
		}
	}

	private boolean askForUpdate(String availableNewVersion) {
		if (updateParent != null) {
			try {
				return updateParent.askForUpdate(availableNewVersion);
			} catch (Exception e) {
				if (updateParent != null) {
					updateParent.showUpdateError("Update error:\n" + e.getMessage());
				} else {
					System.err.println("Update error:\n" + e.getMessage());
				}
				return false;
			}
		} else {
			return false;
		}
	}

	private void updateApplication() {
		try {
			String jarFilePath = System.getProperty(SystemUtilities.SYSTEM_PARAMETER_NAME_CURRENT_RUNNING_JAR);

			if (jarFilePath == null) {
				jarFilePath = System.getenv(SystemUtilities.SYSTEM_PARAMETER_NAME_CURRENT_RUNNING_JAR);
			}
			
			if (jarFilePath == null) {
				try {
					String currentJarUrlPath = getClass().getResource(getClass().getSimpleName() + ".class").toString();
					jarFilePath = currentJarUrlPath.substring(0, currentJarUrlPath.lastIndexOf("!")).replaceFirst("jar:file:", "");
				} catch (Exception e) {
					jarFilePath = null;
				}
			}
			
			// Replace ~ by user.home
			jarFilePath = jarFilePath.replace("~", System.getProperty("user.home"));
			
			if (jarFilePath == null || !new File(jarFilePath).exists()) {
				throw new Exception("Current running jar file was not found");
			}
			
			File downloadTempFile = new File(new File(jarFilePath).getParent() + File.separator + "temp_" + new File(jarFilePath).getName());

			if (updateFileLocation.toLowerCase().startsWith("http")) {
				// Download file
				boolean retryDownload = true;
				while (retryDownload) {
					String downloadUrlWithCredentials = updateFileLocation;
					if (downloadUrlWithCredentials.contains("<username>")) {
						if (updateParent != null) {
							String username = updateParent.aquireUserName();
							if (username == null || username.trim().length() == 0) {
								updateParent.showUpdateError("Update error:\nusername required");
								return;
							} else {
								downloadUrlWithCredentials = downloadUrlWithCredentials.replace("<username>", username);
							}
						} else {
							if (updateParent != null) {
								updateParent.showUpdateError("Update error:\n" + "username required");
							} else {
								System.err.println("Update error:\n" + "username required");
							}
							return;
						}
					}
					
					if (downloadUrlWithCredentials.contains("<password>")) {
						if (updateParent != null) {
							String password = new String(updateParent.aquirePassword());
							if (password == null || password.trim().length() == 0) {
								updateParent.showUpdateError("Update error:\npassword required");
								return;
							} else {
								downloadUrlWithCredentials = downloadUrlWithCredentials.replace("<password>", password);
							}
						} else {
							if (updateParent != null) {
								updateParent.showUpdateError("Update error:\n" + "password required");
							} else {
								System.err.println("Update error:\n" + "password required");
							}
							return;
						}
					}
					
					retryDownload = false;
					try {
						if (downloadTempFile.exists()) {
							downloadTempFile.delete();
						}
						boolean success = downloadUpdateFile(downloadTempFile, downloadUrlWithCredentials);
						if (!success) {
							// download stopped by user
							updateParent.showUpdateError("Canceled by user");
							return;
						}
					} catch (UserError e) {
						if (updateParent != null) {
							updateParent.showUpdateError("Update error:\n" + e.getMessage());
							
							if (e.getReason() == Reason.UnauthenticatedOrUnauthorized) {
								if (updateFileLocation.contains("<username>") || updateFileLocation.contains("<password>")) {
									retryDownload = true;
								} else {
									System.err.println("Update error:\nAuthentication error");
									return;
								}
							} else {
								return;
							}
						} else {
							System.err.println("Update error:\n" + e.getMessage());
							return;
						}
					} catch (Exception e) {
						if (e instanceof ExecutionException && e.getCause() instanceof UserError) {
							if (updateParent != null) {
								updateParent.showUpdateError("Update error:\n" + ((UserError)e.getCause()).getMessage());
								
								if (((UserError)e.getCause()).getReason() == Reason.UnauthenticatedOrUnauthorized) {
									if (updateFileLocation.contains("<username>") || updateFileLocation.contains("<password>")) {
										retryDownload = true;
									} else {
										System.err.println("Update error:\nAuthentication error");
										return;
									}
								} else {
									return;
								}
							} else {
								System.err.println("Update error:\n" + ((UserError)e.getCause()).getMessage());
								return;
							}
						} else {
							if (updateParent != null) {
								updateParent.showUpdateError("Update error:\n" + e.getMessage());
							} else {
								System.err.println("Update error:\n" + e.getMessage());
							}
							return;
						}
					}
				}
			} else {
				// Copy file
				Files.copy(new File(updateFileLocation).toPath(), downloadTempFile.toPath());
			}
			
			if (updateFileMd5Checksum != null && !updateFileMd5Checksum.equalsIgnoreCase("") && !updateFileMd5Checksum.equalsIgnoreCase("NONE") && !updateFileMd5Checksum.equalsIgnoreCase(createMd5Checksum(downloadTempFile))) {
				if (updateParent != null) {
					updateParent.showUpdateError("Update error:\n" + "MD5-Checksum of updatefile is invalid");
				} else {
					System.err.println("Update error:\n" + "MD5-Checksum of updatefile is invalid");
				}
				return;
			}

			if (SystemUtilities.isWindowsSystem()) {
				File batchFile = new File(new File(jarFilePath).getParent() + File.separator + "batchUpdate_" + new File(jarFilePath).getName() + ".cmd");

				// Create Batchfile
				writeFile(batchFile,
					"@echo off\r\n"
					+ "del \"" + jarFilePath + "\"\r\n"
					+ "if exist \"" + jarFilePath + "\" (\r\n"
						+ "ping -n 3 127.0.0.1 >nul\r\n"
						+ "del \"" + jarFilePath + "\"\r\n"
						+ "if exist \"" + jarFilePath + "\" (\r\n"
							+ "ping -n 3 127.0.0.1 >nul\r\n"
							+ "del \"" + jarFilePath + "\"\r\n"
							+ "if exist \"" + jarFilePath + "\" (\r\n"
								+ "ping -n 3 127.0.0.1 >nul\r\n"
								+ "del \"" + jarFilePath + "\"\r\n"
							+ ")\r\n"
						+ ")\r\n"
					+ ")\r\n"
					+ "ren \"" + downloadTempFile.getAbsolutePath() + "\" \"" + new File(jarFilePath).getName() + "\"\r\n"
					+ "if not exist \"" + downloadTempFile.getAbsolutePath() + "\" (\r\n"
						+ "java -jar \"" + jarFilePath + "\"\r\n"
						+ "del \"" + batchFile.getAbsolutePath() + "\"\r\n"
					+ ")\r\n");

				Runtime.getRuntime().exec("cmd /c start /B " + batchFile.getAbsolutePath());
				Runtime.getRuntime().exit(0);
			} else if (SystemUtilities.isLinuxSystem()) {
				File batchFile = new File(new File(jarFilePath).getParent() + File.separator + "batchUpdate_" + new File(jarFilePath).getName() + ".sh");

				// Create Batchfile
				writeFile(batchFile,
					"#!/bin/bash\n"
					+ "rm \"" + jarFilePath + "\"\n"
					+ "if [ -f \"" + jarFilePath + "\" ]; then\n"
						+ "sleep 3\n"
						+ "rm \"" + jarFilePath + "\"\n"
						+ "if [ -f \"" + jarFilePath + "\" ]; then\n"
							+ "sleep 3\n"
							+ "rm \"" + jarFilePath + "\"\n"
							+ "if [ -f \"" + jarFilePath + "\" ]; then\n"
								+ "sleep 3\n"
								+ "rm \"" + jarFilePath + "\"\n"
							+ "fi\n"
						+ "fi\n"
					+ "fi\n"
					+ "mv \"" + downloadTempFile.getAbsolutePath() + "\" \"" + jarFilePath + "\"\n"
					+ "if ! [ -f \"" + downloadTempFile.getAbsolutePath() + "\" ]; then\n"
						+ "java -jar \"" + jarFilePath + "\"\n"
						+ "rm \"" + batchFile.getAbsolutePath() + "\"\n"
					+ "fi\n");

				Runtime.getRuntime().exec("sh " + batchFile.getAbsolutePath());
				Runtime.getRuntime().exit(0);
			}
		} catch (Exception e) {
			if (updateParent != null) {
				updateParent.showUpdateError("Update error:\n" + e.getMessage());
			} else {
				System.err.println("Update error:\n" + e.getMessage());
			}
			return;
		}
	}
	
	private String replacePlaceholders(String value) {
		if (value == null) {
			return null;
		} else {
			if (value.contains("<system>")) {
				if (SystemUtilities.isWindowsSystem()) {
					value = value.replace("<system>", "windows");
				} else if (SystemUtilities.isLinuxSystem()) {
					value = value.replace("<system>", "linux");
				}
			}
			
			if (value.contains("<bitmode>")) {
				if (System.getProperty("os.arch") != null && System.getProperty("os.arch").contains("64")) {
					value = value.replace("<bitmode>", "64");
				} else if (SystemUtilities.isLinuxSystem()) {
					value = value.replace("<bitmode>", "32");
				}
			}
			
			return value;
		}
	}
	
	private void writeFile(File file, String string) throws Exception {
		try (FileOutputStream out = new FileOutputStream(file)) {
			out.write(string.getBytes("UTF-8"));
		} catch (Exception e) {
			throw new Exception("Cannot write file " + file.getAbsolutePath(), e);
		}
	}

	private boolean downloadUpdateFile(File downloadTempFile, String downloadUrl) throws Exception {
		try {
			fileDownloadWorker = new FileDownloadWorker(this, downloadUrl, downloadTempFile);
			new Thread(fileDownloadWorker).start();
			
			while (!fileDownloadWorker.isDone()) {
				// Wait for download process
				Thread.sleep(1000);
			}
			
			if (!fileDownloadWorker.get()) {
				throw new Exception("Download was not successful");
			}
			
			return !fileDownloadWorker.isCancelled();
		} catch (Exception e) {
			if (e instanceof ExecutionException && e.getCause() instanceof UserError) {
				throw (UserError)e.getCause();
			} else {
				throw e;
			}
		}
	}


	private String createMd5Checksum(File file) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			try (InputStream is = Files.newInputStream(Paths.get("file.txt")); DigestInputStream dis = new DigestInputStream(is, md)) {
			  // Just read the file
			}
			return bytesToHexString(md.digest());
		} catch (Exception e) {
			return null;
		}
	}
	
	private static String bytesToHexString(byte[] bytes) {
		char[] hexArray = "0123456789ABCDEF".toCharArray();
		
	    char[] hexChars = new char[bytes.length * 2];
	    for ( int j = 0; j < bytes.length; j++ ) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 2] = hexArray[v >>> 4];
	        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	    }
	    return new String(hexChars);
	}
	
	@Override
	public void showUnlimitedProgress() {
		// Do nothing
	}

	@Override
	public void showProgress(Date start, long itemsToDo, long itemsDone) {
		if (updateParent != null) {
			updateParent.showUpdateProgress(start, itemsToDo, itemsDone);
		}
	}

	@Override
	public void showDone(Date start, Date end, long itemsDone) {
		if (updateParent != null) {
			try {
				if (fileDownloadWorker.get()) {
					updateParent.showUpdateDone(start, end, itemsDone);
				}
			} catch (Exception e) {
				updateParent.showUpdateError("Update error:\n" + e.getMessage());
			}
		}
	}

	@Override
	public void cancel() {
		// Do nothing
	}
}
