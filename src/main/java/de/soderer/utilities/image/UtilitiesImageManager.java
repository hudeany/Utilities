package de.soderer.utilities.image;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;

import de.soderer.utilities.VisibleException;

public class UtilitiesImageManager {
	private static UtilitiesImageManager instance = null;

	private Shell shell;
	private Map<String, Image> store = new HashMap<>();

	public UtilitiesImageManager(Shell shell) {
		this.shell = shell;
		instance = this;
	}

	private Image getImageFromString(String name) {
		if (!store.containsKey(name)) {
			store.put(name, new Image(shell.getDisplay(), getClass().getResourceAsStream(name)));
		}

		return store.get(name);
	}

	public static Image getImage(String name) throws VisibleException {
		if (instance == null) {
			throw new VisibleException("ImageManager needs to be initialized before usage");
		}

		return instance.getImageFromString(name);
	}
}
