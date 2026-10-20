package de.soderer.utilities;

/**
 * Group of characters for {@link PasswordGenerator}, with the minimum and maximum number of characters of this group
 * that a generated password must contain. Instances are immutable.
 */
public class PasswordGeneratorCharacterGroup {
	/** Allowed characters of this group (never null or empty). A character given multiple times is picked more often. */
	private final char[] characters;

	/** Minimum number of characters of this group in a password (at least 0) */
	public final int minimum;

	/** Maximum number of characters of this group in a password, or -1 for unlimited */
	public final int maximum;

	/**
	 * Creates a group without minimum and with an unlimited maximum.
	 *
	 * @param characters allowed characters of this group
	 * @throws Exception if no characters are given
	 */
	public PasswordGeneratorCharacterGroup(final char... characters) throws Exception {
		this(0, characters);
	}

	/**
	 * Creates a group with a minimum and an unlimited maximum.
	 *
	 * @param minimum minimum number of characters of this group in a password
	 * @param characters allowed characters of this group
	 * @throws Exception if the minimum is negative or no characters are given
	 */
	public PasswordGeneratorCharacterGroup(final int minimum, final char... characters) throws Exception {
		this(minimum, -1, characters);
	}

	/**
	 * Creates a group with a minimum and maximum. If both limits are given in the wrong order, they are swapped.
	 *
	 * @param minimum minimum number of characters of this group in a password
	 * @param maximum maximum number of characters of this group in a password, or -1 for unlimited
	 * @param characters allowed characters of this group, copied, so later changes of the array have no effect
	 * @throws Exception if the limits are invalid (negative minimum, maximum 0 or below -1) or no characters are given
	 */
	public PasswordGeneratorCharacterGroup(final int minimum, final int maximum, final char... characters) throws Exception {
		if (maximum == -1) {
			this.minimum = minimum;
			this.maximum = maximum;
		} else {
			this.minimum = Math.min(minimum, maximum);
			this.maximum = Math.max(minimum, maximum);
		}

		if (this.minimum < 0 || this.maximum == 0 || this.maximum < -1) {
			throw new Exception("Invalid parameters: minimum " + minimum + ", maximum " + maximum);
		} else if (characters == null || characters.length == 0) {
			throw new Exception("Invalid parameters: no characters given");
		}

		// Copy, so the validated characters cannot be changed (e.g. emptied) afterwards by the caller
		this.characters = characters.clone();
	}

	/**
	 * Returns the allowed characters of this group.
	 *
	 * @return a copy of the allowed characters
	 */
	public char[] getCharacters() {
		return characters.clone();
	}

	/**
	 * Picks a random character of this group using a cryptographically strong random source.
	 *
	 * @return a random character of this group
	 */
	public char getRandomCharacter() {
		return characters[Utilities.getRandomNumber(characters.length)];
	}
}
