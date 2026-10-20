package de.soderer.utilities;

import java.util.ArrayList;
import java.util.List;

/**
 * Generator for random passwords built from configurable character groups.
 * All random decisions use the cryptographically strong random source of {@link Utilities#getRandomNumber(int)}.
 */
public class PasswordGenerator {
	/** Upper limit for the password length, protecting against huge memory allocations */
	public static final int MAX_PASSWORD_LENGTH = 1_000_000;

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
	 * @throws Exception if a character group is null or empty or the password cannot be generated with the given size
	 */
	public static char[] generatePassword(final int size, final String... characterGroups) throws Exception {
		if (characterGroups == null) {
			throw new Exception("Invalid parameters: no character groups given");
		}
		final List<PasswordGeneratorCharacterGroup> groups = new ArrayList<>();
		for (final String characterGroup : characterGroups) {
			if (characterGroup == null) {
				throw new Exception("Invalid parameters: character group is null");
			}
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
	 * The order of the two length parameters does not matter.
	 * <p>
	 * The length is chosen uniformly from all lengths within the limits, which the groups can fill: at least the sum of
	 * the group minimums and at most the sum of the group maximums. Characters beyond the group minimums are taken from
	 * a randomly chosen group, which has not reached its maximum yet (each such group with equal probability).
	 * </p>
	 *
	 * @param characterGroups character groups to use, a group listed multiple times counts as multiple groups
	 * @param minimumLengthParam minimum number of characters of the password
	 * @param maximumLengthParam maximum number of characters of the password
	 * @return the generated password
	 * @throws Exception if a length is negative or exceeds {@link #MAX_PASSWORD_LENGTH}, if the minimums of the groups
	 *             exceed the maximum length ("Too many fix set letters") or the maximums of the groups do not allow
	 *             the minimum length ("Too few free letters to use")
	 */
	public static char[] generatePassword(final List<PasswordGeneratorCharacterGroup> characterGroups, final int minimumLengthParam, final int maximumLengthParam) throws Exception {
		final int minimumLength = Math.min(minimumLengthParam, maximumLengthParam);
		final int maximumLength = Math.max(minimumLengthParam, maximumLengthParam);
		if (minimumLength < 0) {
			throw new Exception("Invalid password length: " + minimumLength);
		} else if (maximumLength > MAX_PASSWORD_LENGTH) {
			throw new Exception("Invalid password length " + maximumLength + ", maximum is " + MAX_PASSWORD_LENGTH);
		} else if (characterGroups == null) {
			throw new Exception("Invalid parameters: no character groups given");
		}

		final PasswordGeneratorCharacterGroup[] groups = characterGroups.toArray(new PasswordGeneratorCharacterGroup[0]);
		long fixedCount = 0;
		long capacity = 0;
		boolean unlimitedCapacity = false;
		for (final PasswordGeneratorCharacterGroup group : groups) {
			if (group == null) {
				throw new Exception("Invalid parameters: character group is null");
			}
			fixedCount += group.minimum;
			if (group.maximum == -1) {
				unlimitedCapacity = true;
			} else {
				capacity += group.maximum;
			}
		}

		// Checked before choosing the random length, so a generation never fails only for some random lengths
		if (fixedCount > maximumLength) {
			throw new Exception("Too many fix set letters");
		}
		final int effectiveMaximumLength = unlimitedCapacity ? maximumLength : (int) Math.min(maximumLength, capacity);
		final int effectiveMinimumLength = (int) Math.max(minimumLength, fixedCount);
		if (effectiveMaximumLength < effectiveMinimumLength) {
			throw new Exception("Too few free letters to use");
		}
		final int passwordLength = effectiveMinimumLength + Utilities.getRandomNumber(effectiveMaximumLength - effectiveMinimumLength + 1);

		final char[] password = new char[passwordLength];
		int position = 0;

		// Mandatory characters of each group
		final int[] timesUsed = new int[groups.length];
		for (int groupIndex = 0; groupIndex < groups.length; groupIndex++) {
			for (int i = 0; i < groups[groupIndex].minimum; i++) {
				password[position++] = groups[groupIndex].getRandomCharacter();
			}
			timesUsed[groupIndex] = groups[groupIndex].minimum;
		}

		// Free characters from groups, which have not reached their maximum yet
		final List<Integer> groupsLeftToUse = new ArrayList<>();
		for (int groupIndex = 0; groupIndex < groups.length; groupIndex++) {
			if (groups[groupIndex].maximum == -1 || timesUsed[groupIndex] < groups[groupIndex].maximum) {
				groupsLeftToUse.add(groupIndex);
			}
		}
		while (position < passwordLength) {
			final int listIndex = Utilities.getRandomNumber(groupsLeftToUse.size());
			final int groupIndex = groupsLeftToUse.get(listIndex);
			password[position++] = groups[groupIndex].getRandomCharacter();
			timesUsed[groupIndex]++;
			if (groups[groupIndex].maximum != -1 && timesUsed[groupIndex] >= groups[groupIndex].maximum) {
				groupsLeftToUse.remove(listIndex);
			}
		}

		// Fisher-Yates shuffle, so the mandatory characters are not at predictable positions
		for (int i = passwordLength - 1; i > 0; i--) {
			final int j = Utilities.getRandomNumber(i + 1);
			final char swap = password[i];
			password[i] = password[j];
			password[j] = swap;
		}
		return password;
	}
}
