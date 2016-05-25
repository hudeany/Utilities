package de.soderer.utilities;

public class CsvFormat {
	/** The Constant DEFAULT_SEPARATOR. */
	public static final char DEFAULT_SEPARATOR = ',';

	/** The Constant DEFAULT_STRING_QUOTE. */
	public static final char DEFAULT_STRING_QUOTE = '"';

	/** Mandatory separating charactor. */
	private char separator;

	/** Character for stringquotes: if set to null, no quoting will be done. */
	private char stringQuote;

	/**
	 * Character to escape the stringquote character within quoted strings.
	 * By default this is the stringquote character itself, so it is doubled in quoted strings,
	 * but may also be configured to a backslash '\'.
	 */
	private char stringQuoteEscapeCharacter;

	/** Since stringQuote is a simple char this activates or deactivates the quoting. */
	private boolean useStringQuote;

	/** Allow linebreaks in data texts without the effect of a new data set line. */
	private boolean lineBreakInDataAllowed = true;

	/** Allow double stringquotes to use it as a character in data text. */
	private boolean escapedStringQuoteInDataAllowed = true;

	/** Allow lines with less than the expected number of data entries per line. */
	private boolean fillMissingTrailingColumnsWithNull = false;
	
	/** Trim all data values */
	private boolean alwaysTrim = false;
	
	public CsvFormat() {
	}

	public CsvFormat(char separator, char stringQuote, char stringQuoteEscapeCharacter, boolean useStringQuote, boolean lineBreakInDataAllowed, boolean escapedStringQuoteInDataAllowed, boolean fillMissingTrailingColumnsWithNull, boolean alwaysTrim) {
		super();
		
		this.separator = separator;
		this.stringQuote = stringQuote;
		this.stringQuoteEscapeCharacter = stringQuoteEscapeCharacter;
		this.useStringQuote = useStringQuote;
		this.lineBreakInDataAllowed = lineBreakInDataAllowed;
		this.escapedStringQuoteInDataAllowed = escapedStringQuoteInDataAllowed;
		this.fillMissingTrailingColumnsWithNull = fillMissingTrailingColumnsWithNull;
		this.alwaysTrim = alwaysTrim;
	}

	public char getSeparator() {
		return separator;
	}

	public void setSeparator(char separator) {
		this.separator = separator;
	}

	public char getStringQuote() {
		return stringQuote;
	}

	public void setStringQuote(char stringQuote) {
		this.stringQuote = stringQuote;
	}

	public char getStringQuoteEscapeCharacter() {
		return stringQuoteEscapeCharacter;
	}

	public void setStringQuoteEscapeCharacter(char stringQuoteEscapeCharacter) {
		this.stringQuoteEscapeCharacter = stringQuoteEscapeCharacter;
	}

	public boolean isUseStringQuote() {
		return useStringQuote;
	}

	public void setUseStringQuote(boolean useStringQuote) {
		this.useStringQuote = useStringQuote;
	}

	public boolean isLineBreakInDataAllowed() {
		return lineBreakInDataAllowed;
	}

	public void setLineBreakInDataAllowed(boolean lineBreakInDataAllowed) {
		this.lineBreakInDataAllowed = lineBreakInDataAllowed;
	}

	public boolean isEscapedStringQuoteInDataAllowed() {
		return escapedStringQuoteInDataAllowed;
	}

	public void setEscapedStringQuoteInDataAllowed(boolean escapedStringQuoteInDataAllowed) {
		this.escapedStringQuoteInDataAllowed = escapedStringQuoteInDataAllowed;
	}

	public boolean isFillMissingTrailingColumnsWithNull() {
		return fillMissingTrailingColumnsWithNull;
	}

	public void setFillMissingTrailingColumnsWithNull(boolean fillMissingTrailingColumnsWithNull) {
		this.fillMissingTrailingColumnsWithNull = fillMissingTrailingColumnsWithNull;
	}

	public boolean isAlwaysTrim() {
		return alwaysTrim;
	}

	public void setAlwaysTrim(boolean alwaysTrim) {
		this.alwaysTrim = alwaysTrim;
	}
}
