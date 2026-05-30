package de.soderer.utilities.remote;

public class PermissionDeniedException extends Exception {
	private static final long serialVersionUID = 6255198725581994792L;

	private final String folderPath;

	public PermissionDeniedException(final String message, final String folderPath) {
		super(message);

		this.folderPath = folderPath;
	}

	public String getFolderPath() {
		return folderPath;
	}
}
