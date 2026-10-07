package de.soderer.utilities;

import java.net.Proxy;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import de.soderer.json.JsonArray;
import de.soderer.json.JsonObject;
import de.soderer.json.JsonReader;
import de.soderer.json.JsonWriter;
import de.soderer.network.HttpMethod;
import de.soderer.network.HttpRequest;
import de.soderer.network.HttpResponse;
import de.soderer.network.HttpUtilities;
import de.soderer.pac.utilities.ProxyConfiguration;

/**
 * Minimal REST client for the documents of one collection of an Azure Cosmos DB (SQL API), authenticated by master key.
 * Instances are thread-safe.
 *
 * <p>
 * For documentation see: <a href="https://docs.microsoft.com/en-gb/rest/api/cosmos-db/">https://docs.microsoft.com/en-gb/rest/api/cosmos-db/</a>
 * </p>
 */
public class CosmosDbRestfulClient {
	private static final String COSMOSDB_API_VERSION = "2017-02-22";
	private static final String AUTH_TOKEN_TYPE = "master";
	private static final String AUTH_TOKEN_VERSION = "1.0";

	/** Master key; a new Mac instance is created per request, because Mac instances are not thread-safe */
	private final SecretKeySpec masterKeySpec;
	private String cosmosDbEndpoint;
	private final String databaseID;
	private final String collectionID;

	private ProxyConfiguration proxyConfiguration = null;

	/**
	 * Creates a client for a collection.
	 *
	 * @param cosmosDbEndpoint endpoint URL like "https://myaccount.documents.azure.com:443" (a trailing slash is removed)
	 * @param masterkeyBase64 base64 encoded master key of the account
	 * @param databaseID id of the database
	 * @param collectionID id of the collection (a trailing slash is removed)
	 * @throws Exception if the master key is invalid
	 */
	public CosmosDbRestfulClient(final String cosmosDbEndpoint, final String masterkeyBase64, final String databaseID, String collectionID) throws Exception {
		// trim trailing slash
		if (collectionID != null && collectionID.endsWith("/")) {
			collectionID = collectionID.substring(0, collectionID.length() - 1);
		}

		this.cosmosDbEndpoint = cosmosDbEndpoint;
		if (this.cosmosDbEndpoint.endsWith("/")) {
			this.cosmosDbEndpoint = this.cosmosDbEndpoint.substring(0, this.cosmosDbEndpoint.length() -1 );
		}
		this.databaseID = databaseID;
		this.collectionID = collectionID;

		final byte[] masterkey = Base64.getDecoder().decode(masterkeyBase64);
		masterKeySpec = new SecretKeySpec(masterkey, "HmacSHA256");
		// Check the key once, so invalid keys are reported here
		Mac.getInstance("HmacSHA256").init(masterKeySpec);
	}

	/**
	 * Sets the proxy configuration for all requests.
	 *
	 * @param proxyConfiguration proxy configuration, or null for direct connections
	 * @return this instance for chaining
	 */
	public CosmosDbRestfulClient setProxyConfiguration(final ProxyConfiguration proxyConfiguration) {
		this.proxyConfiguration = proxyConfiguration;
		return this;
	}

	/**
	 * Stores a new JSON document. Fails if a document with the same id already exists.
	 *
	 * @param documentJsonContent JSON document including its "id" property
	 * @return true for new document created, false for existing document replaced
	 * @throws Exception if the request fails, the document already exists (and overwriting is disabled),
	 *             authentication fails or there are too many requests
	 */
	public boolean storeDocument(final String documentJsonContent) throws Exception {
		return storeDocument(documentJsonContent, false, null);
	}

	/**
	 * Stores a JSON document.
	 *
	 * @param documentJsonContent JSON document including its "id" property
	 * @param overwriteIfExists true to replace an existing document with the same id (upsert)
	 * @return true for new document created, false for existing document replaced
	 * @throws Exception if the request fails, the document already exists (and overwriting is disabled),
	 *             authentication fails or there are too many requests
	 */
	public boolean storeDocument(final String documentJsonContent, final boolean overwriteIfExists) throws Exception {
		return storeDocument(documentJsonContent, overwriteIfExists, null);
	}

	/**
	 * Stores a JSON document with additional request headers.
	 *
	 * @param documentJsonContent JSON document including its "id" property
	 * @param overwriteIfExists true to replace an existing document with the same id (upsert)
	 * @param additionalHeaders additional HTTP headers like "x-ms-documentdb-post-trigger-include"="CreateNewDocumentPost", may be null
	 * @return true for new document created, false for existing document replaced
	 * @throws Exception if the request fails, the document already exists (and overwriting is disabled),
	 *             authentication fails or there are too many requests
	 */
	public boolean storeDocument(final String documentJsonContent, final boolean overwriteIfExists, final Map<String, String> additionalHeaders) throws Exception {
		final String requestUrl = cosmosDbEndpoint + "/dbs/" + databaseID + "/colls/" + collectionID + "/docs";
		final HttpRequest request = new HttpRequest(HttpMethod.POST, requestUrl);
		request.setRequestBody(documentJsonContent);

		final String authDate = getCurrentDateRFC7231();

		// Mandatory parameters
		request.addHeader("x-ms-version", COSMOSDB_API_VERSION);
		request.addHeader("x-ms-date", authDate);
		request.addHeader("authorization", HttpUtilities.urlEncode(generateAuthSignature("POST", "docs", "dbs/" + databaseID + "/colls/" + collectionID, authDate), StandardCharsets.UTF_8));

		// Optional parameters
		if (additionalHeaders != null && additionalHeaders.size() > 0) {
			for (final Entry<String, String> entry : additionalHeaders.entrySet()) {
				request.addHeader(entry.getKey(), entry.getValue());
			}
		}
		if (overwriteIfExists) {
			request.addHeader("x-ms-documentdb-is-upsert", "true");
		}

		// Other optional parameters (unused by now)
		//		request.addHeader("x-ms-documentdb-isquery", "TRUE");
		//		request.addHeader("x-ms-indexing-directive", "Include");
		//		request.addHeader("x-ms-documentdb-pre-trigger-include", "CreateNewDocument");
		//		request.addHeader("content-type", "application/json");

		final HttpResponse response = HttpUtilities.executeHttpRequest(request, proxyConfiguration == null ? Proxy.NO_PROXY : proxyConfiguration.getProxy(requestUrl));

		if (response.getHttpCode() == 200) {
			// Replaced existing document
			return false;
		} else if (response.getHttpCode() == 201) {
			// Created a new document
			return true;
		} else if (response.getHttpCode() == 409) {
			// Document already exists
			throw new Exception("Document already exists" + (response.getContent() != null ? ": " + response.getContent() : ""));
		} else if (response.getHttpCode() == 401) {
			// Authentication error. Maybe the masterkey is invalid or the authhashsignature payload is invalid
			throw new Exception("Authentication error while storing json document in cosmosdb" + (response.getContent() != null ? ": " + response.getContent() : ""));
		} else if (response.getHttpCode() == 429) {
			// Too many requests
			throw new Exception("Too many requests to cosmosdb" + (response.getContent() != null ? ": " + response.getContent() : ""));
		} else {
			throw new Exception("Unknown error (HttpResponseCode " + response.getHttpCode() + ") while storing json document in cosmosdb" + (response.getContent() != null ? ": " + response.getContent() : ""));
		}
	}

	/**
	 * Removes a document.
	 *
	 * @param documentID id of the document
	 * @return true for document removed, false if the document does not exist
	 * @throws Exception if the request fails, authentication fails or there are too many requests
	 */
	public boolean removeDocument(final String documentID) throws Exception {
		return removeDocument(documentID, false);
	}

	/**
	 * Removes a document.
	 *
	 * @param documentID id of the document
	 * @param throwExceptionIfNotExists true to throw an exception instead of returning false if the document does not exist
	 * @return true for document removed, false if the document does not exist
	 * @throws Exception if the request fails, authentication fails, there are too many requests or the document
	 *             does not exist and throwExceptionIfNotExists is set
	 */
	public boolean removeDocument(final String documentID, final boolean throwExceptionIfNotExists) throws Exception {
		final String requestUrl = cosmosDbEndpoint + "/dbs/" + databaseID + "/colls/" + collectionID + "/docs/" + documentID;
		final HttpRequest request = new HttpRequest(HttpMethod.DELETE, requestUrl);

		final String authDate = getCurrentDateRFC7231();

		request.addHeader("x-ms-version", COSMOSDB_API_VERSION);
		request.addHeader("x-ms-date", authDate);
		request.addHeader("authorization", HttpUtilities.urlEncode(generateAuthSignature("DELETE", "docs", "dbs/" + databaseID + "/colls/" + collectionID + "/docs/" + documentID, authDate), StandardCharsets.UTF_8));

		final HttpResponse response = HttpUtilities.executeHttpRequest(request, proxyConfiguration == null ? Proxy.NO_PROXY : proxyConfiguration.getProxy(requestUrl));

		if (response.getHttpCode() == 204) {
			// Removed existing document
			return true;
		} else if (response.getHttpCode() == 404) {
			// Document with this id does not exist
			if (throwExceptionIfNotExists) {
				throw new Exception("Document with id '" + documentID + "' was not found in cosmosdb" + (response.getContent() != null ? ": " + response.getContent() : ""));
			} else {
				return false;
			}
		} else if (response.getHttpCode() == 401) {
			// Authentication error. Maybe the masterkey is invalid or the authhashsignature payload is invalid
			throw new Exception("Authentication error while removing json document from cosmosdb" + (response.getContent() != null ? ": " + response.getContent() : ""));
		} else if (response.getHttpCode() == 429) {
			// Too many requests
			throw new Exception("Too many requests to cosmosdb" + (response.getContent() != null ? ": " + response.getContent() : ""));
		} else {
			throw new Exception("Unknown error (HttpResponseCode " + response.getHttpCode() + ") while removing json document from cosmosdb" + (response.getContent() != null ? ": " + response.getContent() : ""));
		}
	}

	/**
	 * Reads a document by its id.
	 *
	 * @param documentID id of the document
	 * @return JSON content of the document, or null if it does not exist
	 * @throws Exception if the request fails, authentication fails or there are too many requests
	 */
	public String getDocument(final String documentID) throws Exception {
		final String requestUrl = cosmosDbEndpoint + "/dbs/" + databaseID + "/colls/" + collectionID + "/docs/" + documentID;
		final HttpRequest request = new HttpRequest(HttpMethod.GET, requestUrl);

		final String authDate = getCurrentDateRFC7231();

		request.addHeader("x-ms-version", COSMOSDB_API_VERSION);
		request.addHeader("x-ms-date", authDate);
		request.addHeader("authorization", HttpUtilities.urlEncode(generateAuthSignature("GET", "docs", "dbs/" + databaseID + "/colls/" + collectionID + "/docs/" + documentID, authDate), StandardCharsets.UTF_8));

		final HttpResponse response = HttpUtilities.executeHttpRequest(request, proxyConfiguration == null ? Proxy.NO_PROXY : proxyConfiguration.getProxy(requestUrl));

		if (response.getHttpCode() == 200) {
			// Document was found
			return response.getContent();
		} else if (response.getHttpCode() == 404) {
			// Document with this document id was not found
			return null;
		} else if (response.getHttpCode() == 401) {
			// Authentication error. Maybe the masterkey is invalid or the authhashsignature payload is invalid
			throw new Exception("Authentication error while getting json document from cosmosdb" + (response.getContent() != null ? ": " + response.getContent() : ""));
		} else if (response.getHttpCode() == 429) {
			// Too many requests
			throw new Exception("Too many requests to cosmosdb" + (response.getContent() != null ? ": " + response.getContent() : ""));
		} else {
			throw new Exception("Unknown error (HttpResponseCode " + response.getHttpCode() + ") while getting json document from cosmosdb" + (response.getContent() != null ? ": " + response.getContent() : ""));
		}
	}

	/**
	 * Executes a SQL query on the collection, e.g. "SELECT * FROM root WHERE root.id = 'abc'".
	 * Prefer {@link #executeSqlQuery(String, Map)} with parameters for values from untrusted sources.
	 *
	 * @param sqlQuery SQL query
	 * @return JSON response content (with the result documents in the array "Documents")
	 * @throws Exception if the request fails, authentication fails or there are too many requests
	 */
	public String executeSqlQuery(final String sqlQuery) throws Exception {
		return executeSqlQuery(sqlQuery, null);
	}

	/**
	 * Executes a parameterized SQL query on the collection, e.g. "SELECT * FROM root WHERE root.id = @idname"
	 * with the parameter "@idname" = "abc".
	 *
	 * @param sqlQuery SQL query with parameter names like "@name"
	 * @param parameters parameter values by name (a missing leading "@" is added), may be null
	 * @return JSON response content (with the result documents in the array "Documents")
	 * @throws Exception if the request fails, authentication fails or there are too many requests
	 */
	public String executeSqlQuery(final String sqlQuery, final Map<String, Object> parameters) throws Exception {
		final String requestUrl = cosmosDbEndpoint + "/dbs/" + databaseID + "/colls/" + collectionID + "/docs";
		final HttpRequest request = new HttpRequest(HttpMethod.POST, requestUrl);

		final String authDate = getCurrentDateRFC7231();

		request.addHeader("x-ms-version", COSMOSDB_API_VERSION);
		request.addHeader("x-ms-date", authDate);
		request.addHeader("authorization", HttpUtilities.urlEncode(generateAuthSignature("POST", "docs", "dbs/" + databaseID + "/colls/" + collectionID, authDate), StandardCharsets.UTF_8));
		request.addHeader("Content-Type", "application/query+json");
		request.addHeader("x-ms-documentdb-isquery", "true");


		final JsonObject requestObject = new JsonObject();
		requestObject.add("query", sqlQuery);
		if (parameters != null && parameters.size() > 0) {
			final JsonArray parameterArray = new JsonArray();
			for (final Entry<String, Object> parameter : parameters.entrySet()) {
				final JsonObject parameterObject = new JsonObject();
				parameterObject.add("name", (parameter.getKey().startsWith("@") ? "" : "@") + parameter.getKey());
				parameterObject.add("value", parameter.getValue());
				parameterArray.add(parameterObject);
			}
			requestObject.add("parameters", parameterArray);
		}

		request.setRequestBody(JsonWriter.getJsonItemString(requestObject));

		final HttpResponse response = HttpUtilities.executeHttpRequest(request, proxyConfiguration == null ? Proxy.NO_PROXY : proxyConfiguration.getProxy(requestUrl));

		if (response.getHttpCode() == 200) {
			return response.getContent();
		} else if (response.getHttpCode() == 401) {
			// Authentication error. Maybe the masterkey is invalid or the authhashsignature payload is invalid
			throw new Exception("Authentication error while executing sql query on cosmosdb" + (response.getContent() != null ? ": " + response.getContent() : ""));
		} else if (response.getHttpCode() == 429) {
			// Too many requests
			throw new Exception("Too many requests to cosmosdb" + (response.getContent() != null ? ": " + response.getContent() : ""));
		} else {
			throw new Exception("Unknown error (HttpResponseCode " + response.getHttpCode() + ") while executing sql query on cosmosdb" + (response.getContent() != null ? ": " + response.getContent() : ""));
		}
	}

	/**
	 * Number of documents in the collection.
	 *
	 * @return number of documents
	 * @throws Exception if the request fails
	 */
	public int getNumberOfDocuments() throws Exception {
		final String responseContent = executeSqlQuery("SELECT COUNT(root.id) AS number FROM root");
		final JsonObject queryResponseJsonObject = ((JsonObject) JsonReader.readJsonItemString(responseContent));
		// The JSON reader may return Integer or Long depending on the size of the number
		return ((Number) ((JsonObject) ((JsonArray) queryResponseJsonObject.get("Documents")).get(0)).getSimpleValue("number")).intValue();
	}

	/**
	 * Ids of all documents in the collection (following continuation tokens).
	 *
	 * @return list of document ids
	 * @throws Exception if a request fails
	 */
	public List<String> listDocumentIDs() throws Exception {
		final List<String> documentIDs = new ArrayList<>();
		String continuationID = null;

		do {
			final String requestUrl = cosmosDbEndpoint + "/dbs/" + databaseID + "/colls/" + collectionID + "/docs";
			final HttpRequest request = new HttpRequest(HttpMethod.POST, requestUrl);

			final String authDate = getCurrentDateRFC7231();

			request.addHeader("x-ms-version", COSMOSDB_API_VERSION);
			request.addHeader("x-ms-date", authDate);
			request.addHeader("authorization", HttpUtilities.urlEncode(generateAuthSignature("POST", "docs", "dbs/" + databaseID + "/colls/" + collectionID, authDate), StandardCharsets.UTF_8));
			request.addHeader("Content-Type", "application/query+json");
			request.addHeader("x-ms-documentdb-isquery", "true");
			request.addHeader("x-ms-max-item-count", "1000000");
			if (continuationID != null) {
				request.addHeader("x-ms-continuation", continuationID);
			}

			final JsonObject requestObject = new JsonObject();
			requestObject.add("query", "SELECT root.id FROM root");

			request.setRequestBody(JsonWriter.getJsonItemString(requestObject));

			final HttpResponse response = HttpUtilities.executeHttpRequest(request, proxyConfiguration == null ? Proxy.NO_PROXY : proxyConfiguration.getProxy(requestUrl));

			if (response.getHttpCode() == 200) {
				final JsonObject queryResponseJsonObject = ((JsonObject) JsonReader.readJsonItemString(response.getContent()));
				final JsonArray documentsArray = (JsonArray) queryResponseJsonObject.get("Documents");
				for (final Object documentObject : documentsArray) {
					documentIDs.add((String) ((JsonObject) documentObject).getSimpleValue("id"));
				}
			} else if (response.getHttpCode() == 401) {
				// Authentication error. Maybe the masterkey is invalid or the authhashsignature payload is invalid
				throw new Exception("Authentication error while getting list of document ids from cosmosdb" + (response.getContent() != null ? ": " + response.getContent() : ""));
			} else if (response.getHttpCode() == 429) {
				// Too many requests
				throw new Exception("Too many requests to cosmosdb" + (response.getContent() != null ? ": " + response.getContent() : ""));
			} else {
				throw new Exception("Unknown error (HttpResponseCode " + response.getHttpCode() + ") while getting list of document ids from cosmosdb" + (response.getContent() != null ? ": " + response.getContent() : ""));
			}

			if (response.getHeaders() != null && response.getHeaders().containsKey("x-ms-continuation")) {
				continuationID = response.getHeaders().get("x-ms-continuation");
			} else {
				continuationID = null;
			}
		} while (continuationID != null);
		return documentIDs;
	}

	/**
	 * Get the current date as in RFC 7231, e.g. "Tue, 01 Nov 1994 08:12:31 GMT"
	 */
	private static String getCurrentDateRFC7231() {
		final Calendar calendar = Calendar.getInstance();
		return DateUtilities.formatDate("EEE, dd MMM yyyy HH:mm:ss z", calendar.getTime(), Locale.US, ZoneId.of("GMT"));
	}

	/**
	 * Generate the sigature for cosmos database auth header
	 *
	 * @param httpVerb GET, POST, PUT
	 * @param resourceType "dbs", "colls", "docs"
	 * @param resourceID Email for docs in OptInHub
	 * @param authDate Date as in RFC 7231, e.g. "Tue, 01 Nov 1994 08:12:31 GMT"
	 */
	private String generateAuthSignature(final String httpVerb, final String resourceType, final String resourceID, final String authDate) throws Exception {
		final String authSignaturePayLoad = String.format("%s\n%s\n%s\n%s\n%s\n",
				httpVerb.toLowerCase(Locale.ROOT),
				resourceType.toLowerCase(Locale.ROOT),
				resourceID,
				authDate.toLowerCase(Locale.ROOT),
				""
				);

		final Mac sha256HMAC = Mac.getInstance("HmacSHA256");
		sha256HMAC.init(masterKeySpec);
		final String base64AuthHashSignature = Base64.getEncoder().encodeToString(sha256HMAC.doFinal(authSignaturePayLoad.getBytes(StandardCharsets.UTF_8)));

		final String authHashSignature = String.format("type=%s&ver=%s&sig=%s",
				AUTH_TOKEN_TYPE,
				AUTH_TOKEN_VERSION,
				base64AuthHashSignature
				);

		return authHashSignature;
	}
}
