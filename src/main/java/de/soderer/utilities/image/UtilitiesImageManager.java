package de.soderer.utilities.image;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;

import de.soderer.utilities.VisibleException;

public class UtilitiesImageManager {
	private static UtilitiesImageManager instance = null;

	private final Shell shell;
	private final Map<String, Image> store = new HashMap<>();

	public UtilitiesImageManager(final Shell shell) {
		this.shell = shell;
		instance = this;
	}

	private Image getImageFromString(final String name) throws IOException {
		if (!store.containsKey(name)) {
			try (InputStream inputStream = getClass().getResourceAsStream(name)) {
				store.put(name, new Image(shell.getDisplay(), inputStream));
			}
		}

		return store.get(name);
	}

	public static Image getImage(final String name) throws VisibleException, IOException {
		if (instance == null) {
			throw new VisibleException("ImageManager needs to be initialized before usage");
		}

		return instance.getImageFromString(name);
	}
}
