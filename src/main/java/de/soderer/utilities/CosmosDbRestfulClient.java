
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

import de.soderer.utilities.http.HttpRequest;
import de.soderer.utilities.http.HttpRequest.HttpMethod;
import de.soderer.utilities.http.HttpResponse;
import de.soderer.utilities.http.HttpUtilities;
import de.soderer.utilities.http.ProxyConfiguration;
import de.soderer.utilities.json.JsonArray;
import de.soderer.utilities.json.JsonObject;
import de.soderer.utilities.json.JsonReader;
import de.soderer.utilities.json.JsonWriter;

/**
 * For documentation see: https://docs.microsoft.com/en-gb/rest/api/cosmos-db/
 */
public class CosmosDbRestfulClient {
	private static final String COSMOSDB_API_VERSION = "2017-02-22";
	private static final String AUTH_TOKEN_TYPE = "master";
	private static final String AUTH_TOKEN_VERSION = "1.0";

	private final Mac sha256HMAC;
	private String cosmosDbEndpoint;
	private final String databaseID;
	private final String collectionID;

	private ProxyConfiguration proxyConfiguration = null;

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
		sha256HMAC = Mac.getInstance("HmacSHA256");
		final SecretKeySpec secret_key = new SecretKeySpec(masterkey, "HmacSHA256");
		sha256HMAC.init(secret_key);
	}

	public CosmosDbRestfulClient setProxyConfiguration(final ProxyConfiguration proxyConfiguration) {
		this.proxyConfiguration = proxyConfiguration;
		return this;
	}

	/**
	 * Store a JSON document in a CosmosDB
	 *
	 * @param documentJsonContent
	 * @return
	 * 		true for new document created<br />
	 * 		false for existing document replaced
	 * @throws Exception
	 */
	public boolean storeDocument(final String documentJsonContent) throws Exception {
		return storeDocument(documentJsonContent, false, null);
	}

	/**
	 * Store a JSON document in a CosmosDB
	 *
	 * @param documentJsonContent
	 * @param overwriteIfExists
	 * @return
	 * 		true for new document created<br />
	 * 		false for existing document replaced
	 * @throws Exception
	 */
	public boolean storeDocument(final String documentJsonContent, final boolean overwriteIfExists) throws Exception {
		return storeDocument(documentJsonContent, overwriteIfExists, null);
	}

	/**
	 * Store a JSON document in a CosmosDB
	 *
	 * @param documentJsonContent
	 * @param overwriteIfExists
	 * @param additionalHeaders like "x-ms-documentdb-post-trigger-include"="CreateNewDocumentPost"
	 * @return
	 * 		true for new document created<br />
	 * 		false for existing document replaced
	 * @throws Exception
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
	 * Remove a document from a CosmosDB
	 *
	 * @param documentID
	 * @return
	 * 		true for document removed<br />
	 * 		false for document does not exist
	 * @throws Exception
	 */
	public boolean removeDocument(final String documentID) throws Exception {
		return removeDocument(documentID, false);
	}

	/**
	 * Remove a document from a CosmosDB
	 *
	 * @param documentID
	 * @param throwExceptionIfNotExists
	 * @return
	 * 		true for document removed<br />
	 * 		false for document does not exist (or Exception if "throwExceptionIfNotExists" is true)
	 * @throws Exception
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
	 * Get a specific document by its id
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
	 * Execute a sql query<br />
	 * <br />
	 * Example query:<br />
	 *     SELECT * FROM root WHERE root.id = 'abc'
	 */
	public String executeSqlQuery(final String sqlQuery) throws Exception {
		return executeSqlQuery(sqlQuery, null);
	}

	/**
	 * Execute a sql query with optional parameters<br />
	 * <br />
	 * Example query:<br />
	 *     SELECT * FROM root WHERE root.id = @idname
	 * <br />
	 * <br />
	 * Example parameterMap:<br />
	 *     "@idname" = "abc"
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
	 * Get amaount of stored documents
	 */
	public int getNumberOfDocuments() throws Exception {
		final String responseContent = executeSqlQuery("SELECT COUNT(root.id) AS number FROM root");
		final JsonObject queryResponseJsonObject = ((JsonObject) JsonReader.readJsonItemString(responseContent).getValue());
		return (int) ((JsonObject) ((JsonArray) queryResponseJsonObject.get("Documents")).get(0)).get("number");
	}

	/**
	 * List all stored documents by their ids
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
				final JsonObject queryResponseJsonObject = ((JsonObject) JsonReader.readJsonItemString(response.getContent()).getValue());
				final JsonArray documentsArray = (JsonArray) queryResponseJsonObject.get("Documents");
				for (final Object documentObject : documentsArray) {
					documentIDs.add((String) ((JsonObject) documentObject).get("id"));
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
				httpVerb.toLowerCase(),
				resourceType.toLowerCase(),
				resourceID,
				authDate.toLowerCase(),
				""
				);

		final String base64AuthHashSignature = Base64.getEncoder().encodeToString(sha256HMAC.doFinal(authSignaturePayLoad.getBytes(StandardCharsets.UTF_8)));

		final String authHashSignature = String.format("type=%s&ver=%s&sig=%s",
				AUTH_TOKEN_TYPE,
				AUTH_TOKEN_VERSION,
				base64AuthHashSignature
				);

		return authHashSignature;
	}
}
