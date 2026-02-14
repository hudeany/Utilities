package de.soderer.utilities;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import de.soderer.json.Json5Reader;
import de.soderer.json.JsonNode;
import de.soderer.json.JsonObject;

public class JwtUtilities {
	public static ZonedDateTime getJwtTokenValidity(final String jwtToken) throws Exception {
		try {
			final String[] jwtParts = jwtToken.split("\\.");

			if (jwtParts.length < 2) {
				throw new Exception("Missing JSON data part in JWT token");
			} else {
				final String jwtPart = jwtParts[1];
				String data;
				try {
					data = new String(Utilities.decodeBase64(jwtPart), StandardCharsets.UTF_8);
				} catch (@SuppressWarnings("unused") final Exception e) {
					data = null;
				}

				if (data == null) {
					throw new Exception("Invalid JSON data in JWT token");
				} else {
					try {
						final JsonNode jsonItem = Json5Reader.readJsonItemString(data);
						if (jsonItem == null || !(jsonItem instanceof JsonObject)) {
							throw new Exception("Invalid JSON data in JWT token");
						} else {
							final JsonObject dataJsonObject = (JsonObject) jsonItem;
							final Object expireDateObject = dataJsonObject.getSimpleValue("exp");
							// exp = Seconds since 1970-01-01T00:00:00Z (Not milliseconds !)
							if (expireDateObject == null) {
								return null;
							} else if (expireDateObject instanceof Long) {
								return ZonedDateTime.ofInstant(Instant.ofEpochSecond((Long) expireDateObject), ZoneId.of("UTC"));
							} else if (expireDateObject instanceof Integer) {
								return ZonedDateTime.ofInstant(Instant.ofEpochSecond((Integer) expireDateObject), ZoneId.of("UTC"));
							} else if (expireDateObject instanceof String) {
								final Long epochSecondsExpireDate = Long.parseLong((String) expireDateObject);
								return ZonedDateTime.ofInstant(Instant.ofEpochSecond(epochSecondsExpireDate), ZoneId.of("UTC"));
							}
						}
					} catch (final Exception e) {
						throw new Exception("Invalid JSON data in JWT token: " + e.getMessage(), e);
					}
				}

				return null;
			}
		} catch (final Exception e) {
			throw new Exception("Invalid JWT token: " + e.getMessage(), e);
		}
	}
}
