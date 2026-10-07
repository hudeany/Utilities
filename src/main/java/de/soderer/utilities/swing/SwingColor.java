package de.soderer.utilities.swing;

import java.awt.Color;

/**
 * Commonly used colors of the Swing dialogs, e.g. the background of error
 * messages.
 */
public class SwingColor {
	/**
	 * Light red, used as background of error messages.
	 */
	public static Color LightRed = new Color(255, 127, 127);

	/**
	 * Yellow, used e.g. as background of warnings.
	 */
	public static Color Yellow = Color.yellow;

	/**
	 * Green, used e.g. as background of success messages.
	 */
	public static Color Green = Color.green;

	private SwingColor() {
		// Utility class
	}
}
