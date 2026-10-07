package de.soderer.utilities.swing;

import java.awt.Dialog;
import java.awt.Window;

import javax.swing.JDialog;

/**
 * Base class of document modal dialogs that return a value.
 *
 * <p>
 * Subclasses set {@link #returnValue} (e.g. in a button listener) and dispose
 * the dialog. {@link #open()} blocks until the dialog is closed and returns that
 * value.
 * </p>
 *
 * @param <T> type of the value returned by {@link #open()}
 */
public abstract class ModalDialog<T> extends JDialog {
	private static final long serialVersionUID = -528812797227988224L;

	/**
	 * Value returned by {@link #open()}. If it is null, the value of
	 * {@link #getDefaultReturnValue()} is returned instead.
	 */
	protected T returnValue;

	/**
	 * Creates a document modal dialog.
	 *
	 * @param owner owner window, may be null
	 * @param title title of the dialog
	 */
	public ModalDialog(final Window owner, final String title) {
		super(owner, title, Dialog.ModalityType.DOCUMENT_MODAL);
	}

	/**
	 * Shows the dialog and blocks until it is closed.
	 *
	 * @return the value set by the dialog, or {@link #getDefaultReturnValue()} if
	 *         none was set (e.g. when the dialog was closed via the window
	 *         decoration)
	 */
	public T open() {
		setVisible(true);

		if (returnValue == null) {
			return getDefaultReturnValue();
		} else {
			return returnValue;
		}
	}

	/**
	 * Returns the value returned by {@link #open()} if the dialog set no return
	 * value.
	 *
	 * @return default return value, null by default
	 */
	protected T getDefaultReturnValue() {
		return null;
	}
}
