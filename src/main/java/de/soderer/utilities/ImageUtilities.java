package de.soderer.utilities;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;

/**
 * Helpers for working with AWT images.
 */
public class ImageUtilities {
	private ImageUtilities() {
		// Utility class
	}

	/**
	 * Collect all distinct colors used in an image.
	 * The alpha channel is ignored, so pixels differing only in transparency yield the same opaque color.
	 *
	 * @param image
	 *            image to scan, must not be null
	 * @return set of the distinct (opaque) colors of all pixels
	 */
	public static Set<Color> getColorsFromImage(final BufferedImage image) {
		final Set<Integer> colorInts = new HashSet<>();
		for (int x = 0; x < image.getWidth(); x++) {
			for (int y = 0; y < image.getHeight(); y++) {
				final int colorInt = image.getRGB(x, y);
				colorInts.add(colorInt);
			}
		}

		final Set<Color> colorSet = new HashSet<>();
		for (final Integer colorInt : colorInts) {
			colorSet.add(new Color(colorInt));
		}
		return colorSet;
	}
}
