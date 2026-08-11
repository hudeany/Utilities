package de.soderer.utilities;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

import de.soderer.json.JsonArray;
import de.soderer.json.JsonObject;
import de.soderer.json.JsonReader;
import de.soderer.json.JsonWriter;
import de.soderer.network.HttpMethod;
import de.soderer.network.HttpRequest;
import de.soderer.network.HttpResponse;
import de.soderer.network.HttpUtilities;

public class DeepLHelper {
	public final static String DEEPL_BASEURL_FREE_DEFAULT = "https://api-free.deepl.com";
	public final static String DEEPL_BASEURL_DEFAULT = "https://api.deepl.com";

	// Maximum number of retries on HTTP 429 (Too Many Requests)
	private final static int MAX_RETRY_COUNT = 5;

	// Initial backoff delay in milliseconds, doubled after each retry
	private final static long INITIAL_BACKOFF_MILLISECONDS = 500;

	// Upper bound for the backoff delay, to avoid excessively long waits
	private final static long MAX_BACKOFF_MILLISECONDS = 16_000;

	// DeepL-specific: translation quota exceeded, retrying will not help
	private final static int HTTP_QUOTA_EXCEEDED = 456;

	// HTTP code for rate limiting, retry with backoff makes sense
	private final static int HTTP_TOO_MANY_REQUESTS = 429;

	private final String baseUrl;
	private final String apiKey;

	private List<String> supportedLanguages = null;

	public DeepLHelper(final String apiKey) {
		baseUrl = DEEPL_BASEURL_FREE_DEFAULT;
		this.apiKey = apiKey;
	}

	public DeepLHelper(final String baseUrl, final String apiKey) {
		this.baseUrl = baseUrl;
		this.apiKey = apiKey;
	}

	public List<String> getSupportedLanguages() throws Exception {
		if (supportedLanguages == null) {
			try {
				final HttpRequest request = new HttpRequest(HttpMethod.GET , baseUrl + "/v2/languages");

				request.addHeader("Authorization", "DeepL-Auth-Key " + apiKey);

				int retryCount = 0;
				while (true) {
					final HttpResponse response = HttpUtilities.executeHttpRequest(request);
					if (response.getHttpCode() == HttpURLConnection.HTTP_OK) {
						final JsonArray jsonData = (JsonArray) JsonReader.readJsonItemString(response.getContent());
						final List<String> languages = new ArrayList<>();
						for (final Object item : jsonData) {
							languages.add((String) ((JsonObject) item).getSimpleValue("language"));
						}
						supportedLanguages = languages;
						break;
					} else if (response.getHttpCode() == HTTP_TOO_MANY_REQUESTS || response.getHttpCode() >= 500) {
						if (retryCount < MAX_RETRY_COUNT) {
							// too many requests or temporary server error => retry with exponential backoff
							Thread.sleep(getBackoffMilliseconds(retryCount));
							retryCount++;
						} else {
							throw new Exception("Error read available DeepL languages. HTTP code: " + response.getHttpCode() + "\nToo many requests or server error, retries exhausted\nContent: " + response.getContent());
						}
					} else if (response.getHttpCode() == HTTP_QUOTA_EXCEEDED) {
						throw new Exception("Error read available DeepL languages. Translation quota exceeded (HTTP code 456)\nContent: " + response.getContent());
					} else if (response.getHttpCode() == HttpURLConnection.HTTP_FORBIDDEN) {
						throw new Exception("Error read available DeepL languages. Invalid or unauthorized API key (HTTP code 403)\nContent: " + response.getContent());
					} else {
						throw new Exception("Error read available DeepL languages. HTTP code: " + response.getHttpCode() + "\nContent: " + response.getContent());
					}
				}
			} catch (final Exception e) {
				throw new Exception("Cannot read available DeepL languages: " + e.getMessage());
			}
		}
		return supportedLanguages;
	}

	/**
	 * Auto detect the source language
	 */
	public String translate(final String sourceText, final String targetLanguage) throws Exception {
		return translate(null, sourceText, targetLanguage);
	}

	public String translate(final String sourceLanguage, final String sourceText, final String targetLanguage) throws Exception {
		try {
			final HttpRequest request = new HttpRequest(HttpMethod.POST, baseUrl+ "/v2/translate");

			request.addHeader("Authorization", "DeepL-Auth-Key " + apiKey);
			request.addHeader("Content-Type", "application/json");

			final JsonObject bodyJsonObject = new JsonObject();
			if (Utilities.isNotBlank(sourceLanguage)) {
				if (!getSupportedLanguages().contains(sourceLanguage.toUpperCase())) {
					throw new Exception("Unsupported source language: " + sourceLanguage);
				}
				bodyJsonObject.add("source_lang", sourceLanguage.toUpperCase());
			}
			if (!getSupportedLanguages().contains(targetLanguage.toUpperCase())) {
				throw new Exception("Unsupported target language: " + targetLanguage);
			}
			bodyJsonObject.add("target_lang", targetLanguage.toUpperCase());
			bodyJsonObject.add("text", new JsonArray().add(sourceText));

			request.setRequestBody(JsonWriter.getJsonItemString(bodyJsonObject));

			int retryCount = 0;
			while (true) {
				final HttpResponse response = HttpUtilities.executeHttpRequest(request);
				if (response.getHttpCode() == HttpURLConnection.HTTP_OK) {
					final JsonObject jsonData = (JsonObject) JsonReader.readJsonItemString(response.getContent());
					return (String) ((JsonObject) ((JsonArray) jsonData.get("translations")).get(0)).getSimpleValue("text");
				} else if (response.getHttpCode() == HTTP_TOO_MANY_REQUESTS || response.getHttpCode() >= 500) {
					if (retryCount < MAX_RETRY_COUNT) {
						// too many requests or temporary server error => retry with exponential backoff
						Thread.sleep(getBackoffMilliseconds(retryCount));
						retryCount++;
					} else {
						throw new Exception("Error http code: " + response.getHttpCode() + "\nToo many requests or server error, retries exhausted\nContent: " + response.getContent());
					}
				} else if (response.getHttpCode() == HTTP_QUOTA_EXCEEDED) {
					throw new Exception("Translation quota exceeded (HTTP code 456)\nContent: " + response.getContent());
				} else if (response.getHttpCode() == HttpURLConnection.HTTP_FORBIDDEN) {
					throw new Exception("Invalid or unauthorized API key (HTTP code 403)\nContent: " + response.getContent());
				} else {
					throw new Exception("Error http code: " + response.getHttpCode() + "\nContent: " + response.getContent());
				}
			}
		} catch (final Exception e) {
			throw new Exception("Cannot translate text by DeepL: " + e.getMessage());
		}
	}

	/**
	 * Calculates the exponential backoff delay for a given retry attempt (0-based),
	 * capped at MAX_BACKOFF_MILLISECONDS and with a small random jitter added
	 * to avoid synchronized retries when multiple requests are throttled at once.
	 */
	private static long getBackoffMilliseconds(final int retryCount) {
		final long exponentialDelay = INITIAL_BACKOFF_MILLISECONDS * (1L << retryCount);
		final long cappedDelay = Math.min(exponentialDelay, MAX_BACKOFF_MILLISECONDS);
		final long jitter = (long) (Math.random() * cappedDelay * 0.2);
		return cappedDelay + jitter;
	}
}
