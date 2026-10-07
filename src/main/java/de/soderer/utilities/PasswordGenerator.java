package de.soderer.utilities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Generator for random passwords built from configurable character groups.
 * All random decisions use the cryptographically strong random source of {@link Utilities#getRandomNumber(int)}.
 */
public class PasswordGenerator {
	/**
	 * Utility class
	 */
	private PasswordGenerator() {
		// Utility class
	}

	/**
	 * Generates a password of the given size containing at least one lower case letter, upper case letter, digit,
	 * special character and extended (non ASCII) character.
	 *
	 * @param size number of characters of the password (must be at least 5)
	 * @return the generated password
	 * @throws Exception if the password cannot be generated with the given size
	 */
	public static char[] generatePassword(final int size) throws Exception {
		return PasswordGenerator.generatePassword(size, "abcdefghijklmnopqrstuvwxyz", "ABCDEFGHIJKLMNOPQRSTUVWXYZ", "0123456789", "!?\"'`#$%&*+-<>=.,:;/|\\@^_(){}[]~", "§€äöüÄÖÜß°²³µ");
	}

	/**
	 * Generates a password of the given size containing at least one and at most 99 characters of each given character group.
	 *
	 * @param size number of characters of the password (must be at least the number of character groups)
	 * @param characterGroups character groups, each given as a string of its allowed characters
	 * @return the generated password
	 * @throws Exception if a character group is empty or the password cannot be generated with the given size
	 */
	public static char[] generatePassword(final int size, final String... characterGroups) throws Exception {
		final List<PasswordGeneratorCharacterGroup> groups = new ArrayList<>();
		for (final String characterGroup : characterGroups) {
			groups.add(new PasswordGeneratorCharacterGroup(1, 99, characterGroup.toCharArray()));
		}
		return PasswordGenerator.generatePassword(groups, size, size);
	}

	/**
	 * Generates a password of a fixed length from the given character groups, respecting each group's minimum and maximum.
	 *
	 * @param characterGroups character groups to use
	 * @param length number of characters of the password
	 * @return the generated password
	 * @throws Exception if the length conflicts with the minimums and maximums of the character groups
	 */
	public static char[] generatePassword(final List<PasswordGeneratorCharacterGroup> characterGroups, final int length) throws Exception {
		return generatePassword(characterGroups, length, length);
	}

	/**
	 * Generates a password with a random length within the given limits from the given character groups.
	 * Each group contributes at least its minimum and at most its maximum number of characters (maximum -1 means unlimited).
	 * The order of the two length parameters does not matter. If the minimums of all groups add up to more than the
	 * randomly chosen length, the password is extended to that sum (but never beyond the maximum length).
	 *
	 * @param characterGroups character groups to use
	 * @param minimumLengthParam minimum number of characters of the password
	 * @param maximumLengthParam maximum number of characters of the password
	 * @return the generated password
	 * @throws Exception if the minimums of the groups exceed the maximum length ("Too many fix set letters")
	 *             or the maximums of the groups do not allow the chosen length ("Too few free letters to use")
	 */
	public static char[] generatePassword(final List<PasswordGeneratorCharacterGroup> characterGroups, final int minimumLengthParam, final int maximumLengthParam) throws Exception {
		final int minimumLength = Math.min(minimumLengthParam, maximumLengthParam);
		final int maximumLength = Math.max(minimumLengthParam, maximumLengthParam);

		final List<Character> passwordLetters = new ArrayList<>();
		final Map<PasswordGeneratorCharacterGroup, Integer> groupsLeftToUse = new HashMap<>();
		for (final PasswordGeneratorCharacterGroup group : characterGroups) {
			final List<Character> nextPasswordLetters = new ArrayList<>();
			for (int i = 0; i < group.minimum; i++) {
				nextPasswordLetters.add(group.getRandomCharacter());
			}
			if (group.maximum == -1 || group.maximum > group.minimum) {
				groupsLeftToUse.put(group, group.minimum);
			}
			passwordLetters.addAll(nextPasswordLetters);
		}
		if (passwordLetters.size() > maximumLength) {
			throw new Exception("Too many fix set letters");
		}
		// The mandatory letters of all groups must always be contained, even if the random length is shorter
		final int passwordLength = Math.max(passwordLetters.size(), minimumLength + Utilities.getRandomNumber(maximumLength - minimumLength + 1));
		for (int i = passwordLetters.size(); i < passwordLength; i++) {
			if (groupsLeftToUse.size() <= 0) {
				throw new Exception("Too few free letters to use");
			}
			final PasswordGeneratorCharacterGroup[] keys = groupsLeftToUse.keySet().toArray(new PasswordGeneratorCharacterGroup[0]);
			final PasswordGeneratorCharacterGroup nextGroupToUse = keys[Utilities.getRandomNumber(keys.length)];
			passwordLetters.add(nextGroupToUse.getRandomCharacter());
			final int timesUsed = groupsLeftToUse.get(nextGroupToUse) + 1;
			if (nextGroupToUse.maximum != -1 && timesUsed >= nextGroupToUse.maximum) {
				groupsLeftToUse.remove(nextGroupToUse);
			} else {
				groupsLeftToUse.put(nextGroupToUse, timesUsed);
			}
		}
		final char[] password = new char[passwordLength];
		for (int i = 0; i < passwordLength; i++) {
			final Character nextCharacter = passwordLetters.get(Utilities.getRandomNumber(passwordLetters.size()));
			password[i] = nextCharacter;
			passwordLetters.remove(nextCharacter);
		}
		return password;
	}
}
