package de.soderer.utilities;

import java.util.function.Supplier;

/**
 * Three-state configuration value: a fixed decision (YES / NO) or ASK to request the decision from the user on each use.
 * Stored as "true", "false" or "ask", so YES and NO stay readable by {@link ConfigurationProperties#getBoolean(String)}
 * and by older application versions.
 */
public enum YesNoAsk {
	/** Fixed decision: yes */
	YES("true"),
	/** Fixed decision: no */
	NO("false"),
	/** Ask the user on each use */
	ASK("ask");

	private final String storageValue;

	YesNoAsk(final String storageValue) {
		this.storageValue = storageValue;
	}

	/**
	 * Value as stored in the configuration file.
	 *
	 * @return "true", "false" or "ask"
	 */
	public String getStorageValue() {
		return storageValue;
	}

	/**
	 * Parses a stored value. "ask" (case-insensitive) or "?" is ASK, any other non-blank value is interpreted as boolean
	 * (see {@link Utilities#interpretAsBool(String)}).
	 *
	 * @param value stored value
	 * @param defaultValue value for null or blank input
	 * @return parsed value
	 */
	public static YesNoAsk fromString(final String value, final YesNoAsk defaultValue) {
		if (value == null || value.isBlank()) {
			return defaultValue;
		}
		final String trimmedValue = value.trim();
		if ("ask".equalsIgnoreCase(trimmedValue) || "?".equals(trimmedValue)) {
			return ASK;
		}
		return Utilities.interpretAsBool(trimmedValue) ? YES : NO;
	}

	/**
	 * Resolves this value to a decision.
	 *
	 * @param question called only for ASK, returns the user's decision or null if the user cancelled
	 * @return decision, or null if the user cancelled the question
	 */
	public Boolean resolve(final Supplier<Boolean> question) {
		return switch (this) {
			case YES -> Boolean.TRUE;
			case NO -> Boolean.FALSE;
			case ASK -> question.get();
		};
	}

	@Override
	public String toString() {
		return storageValue;
	}
}
