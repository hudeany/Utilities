package de.soderer.utilities;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Set;

/**
 * Helpers for working with AWT images.
 */
public class ImageUtilities {
	/** Number of distinct RGB colors (24 bit) */
	private static final int RGB_COLOR_COUNT = 1 << 24;

	private ImageUtilities() {
		// Utility class
	}

	/**
	 * Collect all distinct colors used in an image.
	 * The alpha channel is ignored, so pixels differing only in transparency yield the same opaque color. This also
	 * applies to fully transparent pixels, whose (invisible) RGB value is included.
	 * <p>
	 * The colors are collected in a bit set of all 2^24 RGB values (2 MB), so the memory needed while scanning does
	 * not depend on the image size. Only the result set grows with the number of distinct colors.
	 * </p>
	 *
	 * @param image
	 *            image to scan, must not be null
	 * @return set of the distinct (opaque) colors of all pixels
	 */
	public static Set<Color> getColorsFromImage(final BufferedImage image) {
		final int width = image.getWidth();
		final int height = image.getHeight();
		final BitSet usedColors = new BitSet(RGB_COLOR_COUNT);

		// Reading a whole row at once avoids the per pixel overhead of getRGB(x, y)
		final int[] rowPixels = new int[width];
		for (int y = 0; y < height; y++) {
			image.getRGB(0, y, width, 1, rowPixels, 0, width);
			for (final int argb : rowPixels) {
				usedColors.set(argb & 0xFFFFFF);
			}
		}

		final Set<Color> colorSet = new HashSet<>(Math.max(16, (int) (usedColors.cardinality() / 0.75f) + 1));
		for (int rgb = usedColors.nextSetBit(0); rgb >= 0; rgb = usedColors.nextSetBit(rgb + 1)) {
			colorSet.add(new Color(rgb));
		}
		return colorSet;
	}
}
