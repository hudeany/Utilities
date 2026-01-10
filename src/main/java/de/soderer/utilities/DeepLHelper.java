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

				final HttpResponse response = HttpUtilities.executeHttpRequest(request);

				if (response.getHttpCode() == HttpURLConnection.HTTP_OK) {
					final JsonArray jsonData = (JsonArray) JsonReader.readJsonItemString(response.getContent());
					final List<String> languages = new ArrayList<>();
					for (final Object item : jsonData) {
						languages.add((String) ((JsonObject) item).getSimpleValue("language"));
					}
					supportedLanguages = languages;
				} else {
					throw new Exception("Error http code: " + response.getHttpCode() + "\nContent: " + response.getContent());
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

			final HttpResponse response = HttpUtilities.executeHttpRequest(request);

			if (response.getHttpCode() == HttpURLConnection.HTTP_OK) {
				final JsonObject jsonData = (JsonObject) JsonReader.readJsonItemString(response.getContent());
				return (String) ((JsonObject) ((JsonArray) jsonData.get("translations")).get(0)).getSimpleValue("text");
			} else {
				throw new Exception("Error http code: " + response.getHttpCode() + "\nContent: " + response.getContent());
			}
		} catch (final Exception e) {
			throw new Exception("Cannot translate text by DeepL: " + e.getMessage());
		}
	}
}
