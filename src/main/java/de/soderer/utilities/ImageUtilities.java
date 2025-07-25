package de.soderer.utilities;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;

public class ImageUtilities {
	public static Set<Color> getColorsFromImage(BufferedImage image) {
		Set<Integer> colorInts = new HashSet<>();
		for (int x = 0; x < image.getWidth(); x++) {
			for (int y = 0; y < image.getHeight(); y++) {
				int colorInt = image.getRGB(x, y);
				colorInts.add(colorInt);
			}
		}
		
		Set<Color> colorSet = new HashSet<>();
		for (Integer colorInt : colorInts) {
			colorSet.add(new Color(colorInt));
		}
		return colorSet;
	}
}
