package de.soderer.utilities.googlecloud;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.security.interfaces.RSAPrivateKey;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;

import de.soderer.utilities.DateUtilities;
import de.soderer.utilities.TempFileInputStream;
import de.soderer.utilities.Tuple;
import de.soderer.utilities.Utilities;
import de.soderer.utilities.crypto.CryptographicUtilities;
import de.soderer.utilities.http.HttpRequest;
import de.soderer.utilities.http.HttpRequest.HttpMethod;
import de.soderer.utilities.http.HttpResponse;
import de.soderer.utilities.http.HttpUtilities;
import de.soderer.utilities.json.Json5Reader;
import de.soderer.utilities.json.JsonArray;
import de.soderer.utilities.json.JsonDataType;
import de.soderer.utilities.json.JsonNode;
import de.soderer.utilities.json.JsonObject;
import de.soderer.utilities.remote.FileInfo;
import de.soderer.utilities.remote.FolderInfo;
import de.soderer.utilities.remote.RemoteConnection;

/**
 * The Class GoogleCloudConnection
 */
public class GoogleCloudConnection implements RemoteConnection {
	private final String applicationCredentialsJsonString;

	private String accessToken = null;

	/**
	 * GoogleCloudConnection
	 *
	 * @param applicationCredentialsJsonString
	 * @throws Exception
	 */
	public GoogleCloudConnection(final String applicationCredentialsJsonString) throws Exception {
		this.applicationCredentialsJsonString = applicationCredentialsJsonString;
		if (Utilities.isBlank(applicationCredentialsJsonString)) {
			throw new Exception("Mandatory applicationCredentialsJsonString is missing");
		}

		connect();
	}

	private void connect() throws Exception {
		String clientEmail;
		final RSAPrivateKey rsaPrivateKey;
		final JsonNode credentialsNode = Json5Reader.readJsonItemString(applicationCredentialsJsonString);
		if (credentialsNode.getJsonDataType() != JsonDataType.OBJECT) {
			throw new Exception("Invalid credentials data. Must be JsonObject");
		} else {
			final JsonObject credentials = (JsonObject) credentialsNode.getValue();
			clientEmail = (String) credentials.get("client_email");
			if (Utilities.isBlank(clientEmail)) {
				throw new Exception("Mandatory credentials data is missing: client_email");
			}
			final String privateKeyString = (String) credentials.get("private_key");
			if (Utilities.isBlank(clientEmail)) {
				throw new Exception("Mandatory credentials data is missing: private_key");
			}

			rsaPrivateKey = (RSAPrivateKey) CryptographicUtilities.getPrivateKeyFromString(privateKeyString, null);
		}

		final Algorithm algorithm = Algorithm.RSA256(null, rsaPrivateKey);

		final String signedJwtToken = JWT.create()
				.withClaim("iss", clientEmail)
				.withClaim("scope", "https://www.googleapis.com/auth/devstorage.full_control")
				.withClaim("aud", "https://oauth2.googleapis.com/token")
				.withExpiresAt(DateUtilities.addMinutesToDate(new Date(), 60)) // Maximum is "now + 1 hour"
				.withIssuedAt(new Date())
				.sign(algorithm);

		final HttpRequest accessTokenRequest = new HttpRequest(HttpMethod.POST, "https://oauth2.googleapis.com/token");
		accessTokenRequest.addPostParameter("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer");
		accessTokenRequest.addPostParameter("assertion", signedJwtToken);

		final HttpResponse accessTokenResponse = HttpUtilities.executeHttpRequest(accessTokenRequest);

		if (accessTokenResponse.getHttpCode() != 200) {
			throw new Exception("Request for access token failed: " + accessTokenResponse.toString());
		}

		final JsonNode accessTokenResponseNode = Json5Reader.readJsonItemString(accessTokenResponse.getContent());
		if (accessTokenResponseNode.getJsonDataType() != JsonDataType.OBJECT) {
			throw new Exception("Request for access token returned invalid data: Must be JsonObject, but was " + accessTokenResponseNode.getJsonDataType());
		}

		accessToken = (String) ((JsonObject) accessTokenResponseNode.getValue()).get("access_token");
	}

	@Override
	public void close() throws IOException {
		accessToken = null;
	}

	@Override
	public void deleteFile(final String filePath) throws Exception {
		checkForConnection();

		final Tuple<String, String> pathForExecution = prepareFilePath(filePath);

		final HttpRequest request = new HttpRequest(HttpMethod.DELETE, "https://storage.googleapis.com/storage/v1/b/" + urlEncodePath(pathForExecution.getFirst()) + "/o/" + urlEncodePath(pathForExecution.getSecond()));
		request.addHeader("Authorization", "Bearer " + URLEncoder.encode(accessToken, "UTF-8"));

		final HttpResponse response = HttpUtilities.executeHttpRequest(request);

		if (response.getHttpCode() != 204) {
			throw new Exception("Request for deleteFile failed: " + response.toString());
		}
	}

	@Override
	public Tuple<List<FolderInfo>, List<FileInfo>> getDirectory(final String path) throws Exception {
		checkForConnection();

		final Tuple<String, String> pathForExecution = prepareFilePath(path);

		// Using LinkedHashMap to keep the insertion order
		final Map<String, FolderInfo> interimFolders = new LinkedHashMap<>();

		final List<FileInfo> files = new ArrayList<>();

		final HttpRequest request = new HttpRequest(HttpMethod.GET, "https://storage.googleapis.com/storage/v1/b/" + urlEncodePath(pathForExecution.getFirst()) + "/o");
		request.addHeader("Authorization", "Bearer " + URLEncoder.encode(accessToken, "UTF-8"));

		final HttpResponse response = HttpUtilities.executeHttpRequest(request);

		if (response.getHttpCode() == 200) {
			final JsonNode responseNode = Json5Reader.readJsonItemString(response.getContent());
			if (responseNode.getJsonDataType() != JsonDataType.OBJECT) {
				throw new Exception("Request for getDirectory returned invalid data: Must be JsonObject, but was " + responseNode.getJsonDataType());
			}
			final Object itemsArrayObject = ((JsonObject) responseNode.getValue()).get("items");
			if (itemsArrayObject instanceof JsonArray) {
				String filePathToCheck = pathForExecution.getSecond();
				if (filePathToCheck.length() > 0) {
					filePathToCheck = filePathToCheck + "/";
				}

				boolean pathExists = false;
				for (final Object itemObject : (JsonArray) itemsArrayObject) {
					final String name = (String) ((JsonObject) itemObject).get("name");
					if (name.startsWith(filePathToCheck)) {
						pathExists = true;
						final String relativeObjectPath = name.substring(filePathToCheck.length());
						if (relativeObjectPath.length() > 0) {
							if (!relativeObjectPath.contains("/")) {
								// file found
								final Date modificationDate = DateUtilities.getDateForZonedDateTime(DateUtilities.parseIso8601DateTimeString((String) ((JsonObject) itemObject).get("updated")));
								final long size = Long.parseLong((String) ((JsonObject) itemObject).get("size"));
								files.add(new FileInfo(relativeObjectPath, size, modificationDate));
							} else if (relativeObjectPath.indexOf("/") == relativeObjectPath.lastIndexOf("/")) {
								// real folder found
								final Date modificationDate = DateUtilities.getDateForZonedDateTime(DateUtilities.parseIso8601DateTimeString((String) ((JsonObject) itemObject).get("updated")));
								final String folderName = relativeObjectPath.substring(0, relativeObjectPath.indexOf("/"));
								interimFolders.put(folderName, new FolderInfo(folderName, modificationDate));
							} else {
								// pseudo folder found
								final String folderName = relativeObjectPath.substring(0, relativeObjectPath.indexOf("/"));
								if (!interimFolders.containsKey(folderName)) {
									interimFolders.put(folderName, new FolderInfo(folderName, null));
								}
							}
						}
					}
				}

				if (!pathExists) {
					return null;
				} else {
					final List<FolderInfo> folders = new ArrayList<>();
					for (final FolderInfo folderInfo : interimFolders.values()) {
						folders.add(folderInfo);
					}

					return new Tuple<>(folders, files);
				}
			} else {
				throw new Exception("Request for getDirectory returned invalid data: Must be JsonArray of items, but was " + itemsArrayObject.getClass().getSimpleName());
			}
		} else {
			throw new Exception("Request for getDirectory failed: " + response.toString());
		}
	}

	@Override
	public FolderInfo getFolderInfo(final String folderPath) throws Exception {
		checkForConnection();

		final Tuple<String, String> pathForExecution = prepareFilePath(folderPath);

		final HttpRequest request = new HttpRequest(HttpMethod.GET, "https://storage.googleapis.com/storage/v1/b/" + urlEncodePath(pathForExecution.getFirst()) + "/o");
		request.addHeader("Authorization", "Bearer " + URLEncoder.encode(accessToken, "UTF-8"));

		final HttpResponse response = HttpUtilities.executeHttpRequest(request);

		if (response.getHttpCode() == 200) {
			final JsonNode responseNode = Json5Reader.readJsonItemString(response.getContent());
			if (responseNode.getJsonDataType() != JsonDataType.OBJECT) {
				throw new Exception("Request for getFolderInfo returned invalid data: Must be JsonObject, but was " + responseNode.getJsonDataType());
			}
			final Object itemsArrayObject = ((JsonObject) responseNode.getValue()).get("items");
			if (itemsArrayObject instanceof JsonArray) {
				String filePathToCheck = pathForExecution.getSecond();
				if (filePathToCheck.length() > 0) {
					filePathToCheck = filePathToCheck + "/";
				}

				FolderInfo pseudoFolder = null;

				for (final Object itemObject : (JsonArray) itemsArrayObject) {
					final String name = (String) ((JsonObject) itemObject).get("name");
					if (name.startsWith(filePathToCheck)) {
						final String relativeObjectPath = name.substring(filePathToCheck.length());
						if (relativeObjectPath.length() == 0) {
							// real folder found
							final Date modificationDate = DateUtilities.getDateForZonedDateTime(DateUtilities.parseIso8601DateTimeString((String) ((JsonObject) itemObject).get("updated")));
							final String folderName = pathForExecution.getSecond().substring(pathForExecution.getSecond().lastIndexOf("/") + 1);
							return new FolderInfo(folderName, modificationDate);
						} else if (relativeObjectPath.contains("/")) {
							// pseudo folder found
							final String folderName = pathForExecution.getSecond().substring(pathForExecution.getSecond().lastIndexOf("/") + 1);
							if (Utilities.isNotEmpty(folderName)) {
								pseudoFolder = new FolderInfo(folderName, null);
							} else {
								pseudoFolder = new FolderInfo(pathForExecution.getFirst(), null);
							}
						}
					}
				}
				return pseudoFolder;
			} else {
				throw new Exception("Request for getFolderInfo returned invalid data: Must be JsonArray of items, but was " + itemsArrayObject.getClass().getSimpleName());
			}
		} else {
			throw new Exception("Request for getFolderInfo failed: " + response.toString());
		}
	}

	@Override
	public void deleteFolder(final String folderPath) throws Exception {
		checkForConnection();

		final Tuple<String, String> pathForExecution = prepareFilePath(folderPath);

		final Tuple<List<FolderInfo>, List<FileInfo>> directory = getDirectory(folderPath);
		for (final FileInfo fileInfo : directory.getSecond()) {
			deleteFile(folderPath + "/" + fileInfo.getFileName());
		}
		for (final FolderInfo folderInfo : directory.getFirst()) {
			deleteFolder(folderPath + "/" + folderInfo.getFolderName());
		}

		final HttpRequest request = new HttpRequest(HttpMethod.DELETE, "https://storage.googleapis.com/storage/v1/b/" + urlEncodePath(pathForExecution.getFirst()) + "/o/" + urlEncodePath(pathForExecution.getSecond() + "/"));
		request.addHeader("Authorization", "Bearer " + URLEncoder.encode(accessToken, "UTF-8"));

		final HttpResponse response = HttpUtilities.executeHttpRequest(request);

		if (response.getHttpCode() == 404) {
			// Deletion of pseudo folders leads to 404
		} else if (response.getHttpCode() != 204) {
			throw new Exception("Request for deleteFile failed: " + response.toString());
		}
	}

	@Override
	public void createFolder(final String parentFolderPath, final String folderName) throws Exception {
		checkForConnection();

		final Tuple<String, String> pathForExecution = prepareFilePath(parentFolderPath);

		String mimeType = URLConnection.getFileNameMap().getContentTypeFor(pathForExecution.getSecond());
		if (Utilities.isEmpty(mimeType)) {
			mimeType = "application/octet-stream";
		}

		final HttpRequest request = new HttpRequest(HttpMethod.POST, "https://storage.googleapis.com/upload/storage/v1/b/" + urlEncodePath(pathForExecution.getFirst()) + "/o?uploadType=media&name=" + urlEncodePath(pathForExecution.getSecond() + "/" + folderName + "/"));
		request.addHeader("Authorization", "Bearer " + URLEncoder.encode(accessToken, "UTF-8"));

		request.setRequestBodyContentStream(new ByteArrayInputStream(new byte[0]));

		final HttpResponse response = HttpUtilities.executeHttpRequest(request);

		if (response.getHttpCode() != 200) {
			throw new Exception("Request for createFolder failed: " + response.toString());
		}
	}

	@Override
	public FileInfo getFileInfo(final String filePath) throws Exception {
		checkForConnection();

		final Tuple<String, String> pathForExecution = prepareFilePath(filePath);

		final HttpRequest request = new HttpRequest(HttpMethod.GET, "https://storage.googleapis.com/storage/v1/b/" + urlEncodePath(pathForExecution.getFirst()) + "/o");
		request.addHeader("Authorization", "Bearer " + URLEncoder.encode(accessToken, "UTF-8"));

		final HttpResponse response = HttpUtilities.executeHttpRequest(request);

		if (response.getHttpCode() == 200) {
			final JsonNode responseNode = Json5Reader.readJsonItemString(response.getContent());
			if (responseNode.getJsonDataType() != JsonDataType.OBJECT) {
				throw new Exception("Request for getFolderInfo returned invalid data: Must be JsonObject, but was " + responseNode.getJsonDataType());
			}
			final Object itemsArrayObject = ((JsonObject) responseNode.getValue()).get("items");
			if (itemsArrayObject instanceof JsonArray) {
				final String filePathToCheck = pathForExecution.getSecond();

				for (final Object itemObject : (JsonArray) itemsArrayObject) {
					final String name = (String) ((JsonObject) itemObject).get("name");
					if (name.equals(filePathToCheck)) {
						final Date modificationDate = DateUtilities.getDateForZonedDateTime(DateUtilities.parseIso8601DateTimeString((String) ((JsonObject) itemObject).get("updated")));
						final long size = Long.parseLong((String) ((JsonObject) itemObject).get("size"));
						return new FileInfo(name.substring(name.lastIndexOf("/") + 1), size, modificationDate);
					}
				}
				return null;
			} else {
				throw new Exception("Request for getFolderInfo returned invalid data: Must be JsonArray of items, but was " + itemsArrayObject.getClass().getSimpleName());
			}
		} else {
			throw new Exception("Request for getFolderInfo failed: " + response.toString());
		}
	}

	@Override
	public InputStream downloadFile(final String filePath) throws Exception {
		checkForConnection();

		final Tuple<String, String> pathForExecution = prepareFilePath(filePath);

		String mimeType = URLConnection.getFileNameMap().getContentTypeFor(pathForExecution.getSecond());
		if (Utilities.isEmpty(mimeType)) {
			mimeType = "application/octet-stream";
		}

		final HttpResponse response;
		final File tempFile = File.createTempFile("GoogleCloudConnection_Download_", "");
		try (FileOutputStream outputStream = new FileOutputStream(tempFile)) {
			final HttpRequest request = new HttpRequest(HttpMethod.GET, "https://storage.googleapis.com/storage/v1/b/" + urlEncodePath(pathForExecution.getFirst()) + "/o/" + urlEncodePath(pathForExecution.getSecond()) + "?alt=media");
			request.addHeader("Authorization", "Bearer " + URLEncoder.encode(accessToken, "UTF-8"));

			request.setDownloadStream(outputStream);

			response = HttpUtilities.executeHttpRequest(request);
		}

		if (response == null) {
			if (tempFile.exists()) {
				tempFile.delete();
			}
			throw new Exception("Request for downloadFile '" + filePath + "' failed");
		} else if (response.getHttpCode() != 200) {
			if (tempFile.exists()) {
				tempFile.delete();
			}
			throw new Exception("Request for downloadFile '" + filePath + "' failed: " + response.toString());
		} else {
			return new TempFileInputStream(tempFile);
		}
	}

	@Override
	public void uploadFile(final String filePath, final InputStream fileInputStream) throws Exception {
		checkForConnection();

		final Tuple<String, String> pathForExecution = prepareFilePath(filePath);

		String mimeType = URLConnection.getFileNameMap().getContentTypeFor(pathForExecution.getSecond());
		if (Utilities.isEmpty(mimeType)) {
			mimeType = "application/octet-stream";
		}

		final HttpRequest request = new HttpRequest(HttpMethod.POST, "https://storage.googleapis.com/upload/storage/v1/b/" + urlEncodePath(pathForExecution.getFirst()) + "/o?uploadType=media&name=" + urlEncodePath(pathForExecution.getSecond()));
		request.addHeader("Authorization", "Bearer " + URLEncoder.encode(accessToken, "UTF-8"));
		request.addHeader("Content-Type", mimeType);

		request.setRequestBodyContentStream(fileInputStream);

		final HttpResponse response = HttpUtilities.executeHttpRequest(request);

		if (response.getHttpCode() != 200) {
			throw new Exception("Request for uploadFile '" + filePath + "' failed: " + response.toString());
		}
	}

	private void checkForConnection() throws Exception {
		if (accessToken == null) {
			throw new Exception("GoogleCloudConnection is not connected");
		}
	}

	private static Tuple<String, String> prepareFilePath(String path) throws Exception {
		path = Utilities.trim(path, '/');

		if (path == null || Utilities.isEmpty(path)) {
			throw new Exception("At least the bucketname must be part of the filepath. Extend name by leading '/<bucketname>/...'");
		}

		final String bucketName;
		if (path.contains("/")) {
			bucketName = path.substring(0, path.indexOf("/"));
			path = path.substring(path.indexOf("/") + 1);
		} else {
			bucketName = path;
			path = "";
		}

		String directoryPath;
		final String objectName;
		if (path.contains("/")) {
			directoryPath = path.substring(0, path.lastIndexOf("/"));
			objectName = path.substring(path.lastIndexOf("/") + 1);
		} else {
			directoryPath = "";
			objectName = path;
		}

		return new Tuple<>(bucketName, (Utilities.isNotEmpty(directoryPath) ? directoryPath + "/" : "") + objectName);
	}

	private static String urlEncodePath(final String path) throws UnsupportedEncodingException {
		return URLEncoder.encode(path, "UTF-8").replace("+", "%20");
	}
}
