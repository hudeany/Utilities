package de.soderer.utilities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.soderer.utilities.Utilities;

public class PasswordGenerator {
	public static String generatePassword(List<PasswordGeneratorCharacterGroup> characterGroups, int minimumLength, int maximumLength) throws Exception {
		minimumLength = Math.min(minimumLength, minimumLength);
		maximumLength = Math.max(minimumLength, maximumLength);
		
		List<Character> passwordLetters = new ArrayList<Character>();
		Map<PasswordGeneratorCharacterGroup, Integer> groupsLeftToUse = new HashMap<PasswordGeneratorCharacterGroup, Integer>();
		for (PasswordGeneratorCharacterGroup group : characterGroups) {
			List<Character> nextPasswordLetters = new ArrayList<Character>();
			for (int i = 0; i < group.minimum; i++) {
				nextPasswordLetters.add(group.getRandomCharacter());
			}
			if (group.maximum > group.minimum) {
				groupsLeftToUse.put(group, group.minimum);
			}
			passwordLetters.addAll(nextPasswordLetters);
		}
		if (passwordLetters.size() > maximumLength) {
			throw new Exception("Too many fix set letters");
		}
		int passwordLength = minimumLength + Utilities.getRandomNumber(maximumLength - minimumLength + 1);
		for (int i = passwordLetters.size(); i < passwordLength; i++) {
			if (groupsLeftToUse.size() <= 0) {
				throw new Exception("Too few free letters to use");
			}
			PasswordGeneratorCharacterGroup[] keys = groupsLeftToUse.keySet().toArray(new PasswordGeneratorCharacterGroup[0]);
			PasswordGeneratorCharacterGroup nextGroupToUse = keys[Utilities.getRandomNumber(keys.length)];
			passwordLetters.add(nextGroupToUse.getRandomCharacter());
			int timesUsed = groupsLeftToUse.get(nextGroupToUse) + 1;
			if (timesUsed >= nextGroupToUse.maximum) {
				groupsLeftToUse.remove(nextGroupToUse);
			} else {
				groupsLeftToUse.put(nextGroupToUse, timesUsed);
			}
		}
		StringBuilder password = new StringBuilder();
		for (int i = 0; i < passwordLength; i++) {
			Character nextCharacter = passwordLetters.get(Utilities.getRandomNumber(passwordLetters.size()));
			password.append(nextCharacter);
			passwordLetters.remove(nextCharacter);
		}
		return password.toString();
	}
	
	public class PasswordGeneratorCharacterGroup {
		public char[] characters;
		public int minimum;
		public int maximum;
		
		public PasswordGeneratorCharacterGroup(int minimum, int maximum, char... characters) throws Exception {
			if (minimum < 0 || maximum <= 0 || characters == null || characters.length <= 0) {
				throw new Exception("Invalid parameters");
			}
			
			minimum = Math.min(minimum, maximum);
			maximum = Math.max(minimum, maximum);
			
			this.minimum = minimum;
			this.maximum = maximum;
			this.characters = characters;
		}
		
		public char getRandomCharacter() {
			return characters[Utilities.getRandomNumber(characters.length)];
		}
	}
}
