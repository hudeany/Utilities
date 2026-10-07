package de.soderer.utilities.googlecloud;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.Proxy;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.security.interfaces.RSAPrivateKey;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;

import de.soderer.json.Json5Reader;
import de.soderer.json.JsonArray;
import de.soderer.json.JsonDataType;
import de.soderer.json.JsonNode;
import de.soderer.json.JsonObject;
import de.soderer.network.HttpMethod;
import de.soderer.network.HttpRequest;
import de.soderer.network.HttpResponse;
import de.soderer.network.HttpUtilities;
import de.soderer.pac.utilities.ProxyConfiguration;
import de.soderer.utilities.DateUtilities;
import de.soderer.utilities.TempFileInputStream;
import de.soderer.utilities.Tuple;
import de.soderer.utilities.Utilities;
import de.soderer.utilities.crypto.CryptographicUtilities;
import de.soderer.utilities.remote.FileInfo;
import de.soderer.utilities.remote.FolderInfo;
import de.soderer.utilities.remote.RemoteConnection;

/**
 * The Class GoogleCloudConnection
 */
public class GoogleCloudConnection implements RemoteConnection {
	private final String applicationCredentialsJsonString;

	private final ProxyConfiguration proxyConfiguration;

	private String accessToken = null;

	/** Time after which the access token is renewed (tokens expire after one hour). */
	private LocalDateTime accessTokenRenewalTime = null;

	/**
	 * Creates a connection to Google Cloud Storage, authenticated with the given service account credentials.
	 *
	 * @param applicationCredentialsJsonString content of the service account credentials JSON file
	 * @param proxyConfiguration proxy to use for the requests, may be null for a direct connection
	 * @throws Exception if the credentials are missing or the access token cannot be acquired
	 */
	public GoogleCloudConnection(final String applicationCredentialsJsonString, final ProxyConfiguration proxyConfiguration) throws Exception {
		this.applicationCredentialsJsonString = applicationCredentialsJsonString;
		if (Utilities.isBlank(applicationCredentialsJsonString)) {
			throw new Exception("Mandatory applicationCredentialsJsonString is missing");
		}

		this.proxyConfiguration = proxyConfiguration;

		connect();
	}

	private void connect() throws Exception {
		String clientEmail;
		final RSAPrivateKey rsaPrivateKey;
		final JsonNode credentialsNode = Json5Reader.readJsonItemString(applicationCredentialsJsonString);
		if (credentialsNode.getJsonDataType() != JsonDataType.OBJECT) {
			throw new Exception("Invalid credentials data. Must be JsonObject");
		} else {
			final JsonObject credentials = (JsonObject) credentialsNode;
			clientEmail = (String) credentials.getSimpleValue("client_email");
			if (Utilities.isBlank(clientEmail)) {
				throw new Exception("Mandatory credentials data is missing: client_email");
			}
			final String privateKeyString = (String) credentials.getSimpleValue("private_key");
			if (Utilities.isBlank(privateKeyString)) {
				throw new Exception("Mandatory credentials data is missing: private_key");
			}

			rsaPrivateKey = (RSAPrivateKey) CryptographicUtilities.getPrivateKeyFromString(privateKeyString, null);
		}

		final Algorithm algorithm = Algorithm.RSA256(null, rsaPrivateKey);

		final String signedJwtToken = JWT.create()
				.withClaim("iss", clientEmail)
				.withClaim("scope", "https://www.googleapis.com/auth/devstorage.full_control")
				.withClaim("aud", "https://oauth2.googleapis.com/token")
				.withExpiresAt(DateUtilities.getDateForLocalDateTime(LocalDateTime.now().plusHours(1))) // Maximum is "now + 1 hour"
				.withIssuedAt(DateUtilities.getDateForLocalDateTime(LocalDateTime.now()))
				.sign(algorithm);

		final String oauthUrl = "https://oauth2.googleapis.com/token";
		final HttpRequest accessTokenRequest = new HttpRequest(HttpMethod.POST, oauthUrl);
		accessTokenRequest.addPostParameter("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer");
		accessTokenRequest.addPostParameter("assertion", signedJwtToken);

		final HttpResponse accessTokenResponse = HttpUtilities.executeHttpRequest(accessTokenRequest, proxyConfiguration == null ? Proxy.NO_PROXY : proxyConfiguration.getProxy(oauthUrl));

		if (accessTokenResponse.getHttpCode() != 200) {
			throw new Exception("Request for access token failed: " + accessTokenResponse.toString());
		}

		final JsonNode accessTokenResponseNode = Json5Reader.readJsonItemString(accessTokenResponse.getContent());
		if (accessTokenResponseNode.getJsonDataType() != JsonDataType.OBJECT) {
			throw new Exception("Request for access token returned invalid data: Must be JsonObject, but was " + accessTokenResponseNode.getJsonDataType());
		}

		accessToken = (String) ((JsonObject) accessTokenResponseNode).getSimpleValue("access_token");
		long expiresInSeconds = 3600;
		final Object expiresInObject = ((JsonObject) accessTokenResponseNode).getSimpleValue("expires_in");
		if (expiresInObject instanceof Number) {
			expiresInSeconds = ((Number) expiresInObject).longValue();
		}
		// Renew a minute early, so a request does not start with a token that expires during the request
		accessTokenRenewalTime = LocalDateTime.now().plusSeconds(Math.max(0, expiresInSeconds - 60));
	}

	@Override
	public void close() throws IOException {
		accessToken = null;
	}

	@Override
	public void deleteFile(final String filePath) throws Exception {
		checkForConnection();

		final Tuple<String, String> pathForExecution = prepareFilePath(filePath);

		final String requestUrl = "https://storage.googleapis.com/storage/v1/b/" + urlEncodePath(pathForExecution.getFirst()) + "/o/" + urlEncodePath(pathForExecution.getSecond());
		final HttpRequest request = new HttpRequest(HttpMethod.DELETE, requestUrl);
		request.addHeader("Authorization", "Bearer " + URLEncoder.encode(accessToken, "UTF-8"));

		final HttpResponse response = HttpUtilities.executeHttpRequest(request, proxyConfiguration == null ? Proxy.NO_PROXY : proxyConfiguration.getProxy(requestUrl));

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

		String filePathToCheck = pathForExecution.getSecond();
		if (filePathToCheck.length() > 0) {
			filePathToCheck = filePathToCheck + "/";
		}

		final List<JsonObject> objects = listObjects(pathForExecution.getFirst(), filePathToCheck);
		if (objects.isEmpty() && filePathToCheck.length() > 0) {
			// Neither the folder object nor any object inside of it exists
			return null;
		}

		for (final JsonObject itemObject : objects) {
			final String name = (String) itemObject.getSimpleValue("name");
			final String relativeObjectPath = name.substring(filePathToCheck.length());
			if (relativeObjectPath.length() > 0) {
				if (!relativeObjectPath.contains("/")) {
					// file found
					final ZonedDateTime modificationDate = DateUtilities.parseIso8601DateTimeString((String) itemObject.getSimpleValue("updated"));
					final long size = Long.parseLong(itemObject.getSimpleValue("size").toString());
					files.add(new FileInfo(relativeObjectPath, size, modificationDate));
				} else if (relativeObjectPath.indexOf("/") == relativeObjectPath.lastIndexOf("/") && relativeObjectPath.endsWith("/")) {
					// real folder found
					final ZonedDateTime modificationDate = DateUtilities.parseIso8601DateTimeString((String) itemObject.getSimpleValue("updated"));
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

		return new Tuple<>(new ArrayList<>(interimFolders.values()), files);
	}

	@Override
	public FolderInfo getFolderInfo(final String folderPath) throws Exception {
		checkForConnection();

		final Tuple<String, String> pathForExecution = prepareFilePath(folderPath);

		String filePathToCheck = pathForExecution.getSecond();
		if (filePathToCheck.length() > 0) {
			filePathToCheck = filePathToCheck + "/";
		}

		final String folderName = pathForExecution.getSecond().substring(pathForExecution.getSecond().lastIndexOf("/") + 1);
		FolderInfo pseudoFolder = null;
		for (final JsonObject itemObject : listObjects(pathForExecution.getFirst(), filePathToCheck)) {
			final String name = (String) itemObject.getSimpleValue("name");
			final String relativeObjectPath = name.substring(filePathToCheck.length());
			if (relativeObjectPath.length() == 0) {
				// real folder found
				final ZonedDateTime modificationDate = DateUtilities.parseIso8601DateTimeString((String) itemObject.getSimpleValue("updated"));
				return new FolderInfo(folderName, modificationDate);
			} else if (pseudoFolder == null) {
				// pseudo folder found (objects with this path prefix exist)
				pseudoFolder = new FolderInfo(Utilities.isNotEmpty(folderName) ? folderName : pathForExecution.getFirst(), null);
			}
		}
		return pseudoFolder;
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

		final String requestUrl = "https://storage.googleapis.com/storage/v1/b/" + urlEncodePath(pathForExecution.getFirst()) + "/o/" + urlEncodePath(pathForExecution.getSecond() + "/");
		final HttpRequest request = new HttpRequest(HttpMethod.DELETE, requestUrl);
		request.addHeader("Authorization", "Bearer " + URLEncoder.encode(accessToken, "UTF-8"));

		final HttpResponse response = HttpUtilities.executeHttpRequest(request, proxyConfiguration == null ? Proxy.NO_PROXY : proxyConfiguration.getProxy(requestUrl));

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

		final String requestUrl = "https://storage.googleapis.com/upload/storage/v1/b/" + urlEncodePath(pathForExecution.getFirst()) + "/o?uploadType=media&name=" + urlEncodePath(pathForExecution.getSecond() + "/" + folderName + "/");
		final HttpRequest request = new HttpRequest(HttpMethod.POST, requestUrl);
		request.addHeader("Authorization", "Bearer " + URLEncoder.encode(accessToken, "UTF-8"));

		request.setRequestBodyContentStream(new ByteArrayInputStream(new byte[0]));

		final HttpResponse response = HttpUtilities.executeHttpRequest(request, proxyConfiguration == null ? Proxy.NO_PROXY : proxyConfiguration.getProxy(requestUrl));

		if (response.getHttpCode() != 200) {
			throw new Exception("Request for createFolder failed: " + response.toString());
		}
	}

	@Override
	public FileInfo getFileInfo(final String filePath) throws Exception {
		checkForConnection();

		final Tuple<String, String> pathForExecution = prepareFilePath(filePath);
		if (pathForExecution.getSecond().length() == 0) {
			// A bucket is no file
			return null;
		}

		// Request the object's metadata directly instead of searching it in the (paged) object list
		final String requestUrl = "https://storage.googleapis.com/storage/v1/b/" + urlEncodePath(pathForExecution.getFirst()) + "/o/" + urlEncodePath(pathForExecution.getSecond());
		final HttpRequest request = new HttpRequest(HttpMethod.GET, requestUrl);
		request.addHeader("Authorization", "Bearer " + URLEncoder.encode(accessToken, "UTF-8"));

		final HttpResponse response = HttpUtilities.executeHttpRequest(request, proxyConfiguration == null ? Proxy.NO_PROXY : proxyConfiguration.getProxy(requestUrl));
		if (response.getHttpCode() == 404) {
			return null;
		} else if (response.getHttpCode() != 200) {
			throw new Exception("Request for getFileInfo failed: " + response.toString());
		}

		final JsonNode responseNode = Json5Reader.readJsonItemString(response.getContent());
		if (responseNode.getJsonDataType() != JsonDataType.OBJECT) {
			throw new Exception("Request for getFileInfo returned invalid data: Must be JsonObject, but was " + responseNode.getJsonDataType());
		}
		final JsonObject itemObject = (JsonObject) responseNode;
		final String name = (String) itemObject.getSimpleValue("name");
		final ZonedDateTime modificationDate = DateUtilities.parseIso8601DateTimeString((String) itemObject.getSimpleValue("updated"));
		final long size = Long.parseLong(itemObject.getSimpleValue("size").toString());
		return new FileInfo(name.substring(name.lastIndexOf("/") + 1), size, modificationDate);
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
			final String requestUrl = "https://storage.googleapis.com/storage/v1/b/" + urlEncodePath(pathForExecution.getFirst()) + "/o/" + urlEncodePath(pathForExecution.getSecond()) + "?alt=media";
			final HttpRequest request = new HttpRequest(HttpMethod.GET, requestUrl);
			request.addHeader("Authorization", "Bearer " + URLEncoder.encode(accessToken, "UTF-8"));

			request.setDownloadStream(outputStream);

			response = HttpUtilities.executeHttpRequest(request, proxyConfiguration == null ? Proxy.NO_PROXY : proxyConfiguration.getProxy(requestUrl));
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

		final String requestUrl = "https://storage.googleapis.com/upload/storage/v1/b/" + urlEncodePath(pathForExecution.getFirst()) + "/o?uploadType=media&name=" + urlEncodePath(pathForExecution.getSecond());
		final HttpRequest request = new HttpRequest(HttpMethod.POST, requestUrl);
		request.addHeader("Authorization", "Bearer " + URLEncoder.encode(accessToken, "UTF-8"));
		request.addHeader("Content-Type", mimeType);

		request.setRequestBodyContentStream(fileInputStream);

		final HttpResponse response = HttpUtilities.executeHttpRequest(request, proxyConfiguration == null ? Proxy.NO_PROXY : proxyConfiguration.getProxy(requestUrl));

		if (response.getHttpCode() != 200) {
			throw new Exception("Request for uploadFile '" + filePath + "' failed: " + response.toString());
		}
	}

	private void checkForConnection() throws Exception {
		if (accessToken == null) {
			throw new Exception("GoogleCloudConnection is not connected");
		} else if (accessTokenRenewalTime != null && LocalDateTime.now().isAfter(accessTokenRenewalTime)) {
			// The access token expires after one hour, long running sessions need a new one
			connect();
		}
	}

	/**
	 * Lists all objects of a bucket whose names start with the given prefix. Follows the paging of the API,
	 * which returns at most 1000 objects per request.
	 */
	private List<JsonObject> listObjects(final String bucketName, final String prefix) throws Exception {
		final List<JsonObject> objects = new ArrayList<>();
		String pageToken = null;
		do {
			final String requestUrl = "https://storage.googleapis.com/storage/v1/b/" + urlEncodePath(bucketName) + "/o?prefix=" + urlEncodePath(prefix)
					+ (pageToken != null ? "&pageToken=" + urlEncodePath(pageToken) : "");
			final HttpRequest request = new HttpRequest(HttpMethod.GET, requestUrl);
			request.addHeader("Authorization", "Bearer " + URLEncoder.encode(accessToken, "UTF-8"));

			final HttpResponse response = HttpUtilities.executeHttpRequest(request, proxyConfiguration == null ? Proxy.NO_PROXY : proxyConfiguration.getProxy(requestUrl));
			if (response.getHttpCode() != 200) {
				throw new Exception("Request for object list failed: " + response.toString());
			}
			final JsonNode responseNode = Json5Reader.readJsonItemString(response.getContent());
			if (responseNode.getJsonDataType() != JsonDataType.OBJECT) {
				throw new Exception("Request for object list returned invalid data: Must be JsonObject, but was " + responseNode.getJsonDataType());
			}
			final Object itemsArrayObject = ((JsonObject) responseNode).get("items");
			if (itemsArrayObject instanceof JsonArray) {
				for (final Object itemObject : (JsonArray) itemsArrayObject) {
					objects.add((JsonObject) itemObject);
				}
			} else if (itemsArrayObject != null) {
				throw new Exception("Request for object list returned invalid data: Must be JsonArray of items, but was " + itemsArrayObject.getClass().getSimpleName());
			}
			// "items" is missing if no objects match
			final Object nextPageToken = ((JsonObject) responseNode).getSimpleValue("nextPageToken");
			pageToken = nextPageToken == null ? null : nextPageToken.toString();
		} while (pageToken != null);
		return objects;
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
