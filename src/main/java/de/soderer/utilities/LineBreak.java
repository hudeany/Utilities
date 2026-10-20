package de.soderer.utilities;

/**
 * Enum to represent a linebreak type of an text
 */
public enum LineBreak {
	/**
	 * No linebreak
	 */
	Unknown(null),

	/**
	 * Multiple linebreak types
	 */
	Mixed(null),

	/**
	 * Unix/Linux linebreak ("\n")
	 */
	Unix("\n"),

	/**
	 * Mac/Apple linebreak ("\r")
	 */
	Mac("\r"),

	/**
	 * Windows linebreak ("\r\n")
	 */
	Windows("\r\n");

	private final String representationString;

	@Override
	public String toString() {
		return representationString;
	}

	LineBreak(final String representationString) {
		this.representationString = representationString;
	}

	/**
	 * Get the linebreak type by its name, ignoring case.
	 *
	 * @param lineBreakTypeName "Windows", "Unix" or "Mac", may be null
	 * @return matching linebreak type, or {@link #Unknown} for any other name
	 */
	public static LineBreak getLineBreakTypeByName(final String lineBreakTypeName) {
		if ("WINDOWS".equalsIgnoreCase(lineBreakTypeName)) {
			return LineBreak.Windows;
		} else if ("UNIX".equalsIgnoreCase(lineBreakTypeName)) {
			return LineBreak.Unix;
		} else if ("MAC".equalsIgnoreCase(lineBreakTypeName)) {
			return LineBreak.Mac;
		} else {
			return LineBreak.Unknown;
		}
	}
}
