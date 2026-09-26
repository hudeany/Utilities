package de.soderer.utilities.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import de.soderer.utilities.DefaultLangResources;

public abstract class ModalDialog<T> extends Dialog {
	private static final int GTK_TITLEBAR_EXTRA_WIDTH = 100;

	private Point location = null;
	private T returnValue;

	public ModalDialog(final Shell parentShell, final String title) {
		super(new Shell(parentShell, SWT.TITLE | SWT.BORDER | SWT.APPLICATION_MODAL));
		getParent().setText(title);
		setText(title);
	}

	public ModalDialog(final Shell parentShell, final int style, final String title) {
		super(new Shell(parentShell, style | SWT.TITLE | SWT.BORDER | SWT.APPLICATION_MODAL));
		getParent().setText(title);
		setText(title);
	}

	public T open() {
		final Shell shell = getParent();
		try {
			createComponents(shell);
		} catch (final Exception e) {
			throw new RuntimeException("Cannot create dialog components", e);
		}

		// Must happen before positioning, because centering depends on the final shell size
		ensureMinimumWidthForTitle(shell);

		setPosition(shell);

		shell.open();

		if (isGtk()) {
			// GTK may still adjust the window allocation after mapping the window, so re-layout once afterwards
			shell.getDisplay().asyncExec(() -> {
				if (!shell.isDisposed()) {
					shell.layout(true, true);
				}
			});
		}

		final Display display = shell.getDisplay();
		while (!shell.isDisposed() && !display.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}

		if (returnValue == null) {
			return getDefaultReturnValue();
		} else {
			return returnValue;
		}
	}

	public void hide() {
		getParent().setVisible(false);
	}

	public void show() {
		getParent().setVisible(true);
	}

	public void setLocation(final Point value) {
		location = value;
	}

	public boolean isDisposed() {
		return getParent().isDisposed();
	}

	protected void setReturnValue(final T value) {
		returnValue = value;
	}

	protected void setPosition(final Shell shell) {
		if (!shell.isDisposed()) {
			int xPosition;
			int yPosition;
			if (location == null) {
				// Sets the position centered of parent
				try {
					xPosition = shell.getParent().getLocation().x + shell.getParent().getSize().x / 2 - shell.getSize().x / 2;
					yPosition = shell.getParent().getLocation().y + shell.getParent().getSize().y / 2 - shell.getSize().y / 2;
				} catch (@SuppressWarnings("unused") final Exception e) {
					xPosition = 50;
					yPosition = 50;
				}
			} else {
				xPosition = location.x;
				yPosition = location.y;
			}
			xPosition = Math.max(0, xPosition);
			yPosition = Math.max(0, yPosition);
			shell.setLocation(xPosition, yPosition);
		}
	}

	/**
	 * On GTK the header bar (client side decoration) enforces a minimum window width derived from the title text.
	 * If the packed content is narrower, GTK silently enlarges the window, but SWT keeps laying out the content
	 * for the smaller size, so controls do not fill the dialog. This makes the shell width explicit and re-layouts.
	 */
	protected static void ensureMinimumWidthForTitle(final Shell shell) {
		if (shell.isDisposed() || !isGtk()) {
			return;
		}

		final String title = shell.getText();
		if (title == null || title.isEmpty()) {
			return;
		}

		final int titleWidth = measureTitleWidth(shell, title);
		final int maximumWidth = shell.getDisplay().getClientArea().width;
		final int requiredWidth = Math.min(titleWidth + GTK_TITLEBAR_EXTRA_WIDTH, maximumWidth);

		final Point size = shell.getSize();
		if (size.x < requiredWidth) {
			shell.setSize(requiredWidth, size.y);
			shell.layout(true, true);
		}
	}

	private static int measureTitleWidth(final Shell shell, final String title) {
		final Display display = shell.getDisplay();
		final GC gc = new GC(shell);
		Font boldFont = null;
		try {
			// GTK header bars render the title in bold system font
			final FontData[] fontDataArray = display.getSystemFont().getFontData();
			for (final FontData fontData : fontDataArray) {
				fontData.setStyle(fontData.getStyle() | SWT.BOLD);
			}
			boldFont = new Font(display, fontDataArray);
			gc.setFont(boldFont);
			return gc.textExtent(title).x;
		} finally {
			gc.dispose();
			if (boldFont != null) {
				boldFont.dispose();
			}
		}
	}

	private static boolean isGtk() {
		return "gtk".equals(SWT.getPlatform());
	}

	protected T getDefaultReturnValue() {
		return null;
	}

	protected void showMessage(final String title, final String text) {
		new QuestionDialog(getParent(), title, text, DefaultLangResources.getI18NStringOrDefault("ok")).open();
	}

	protected void showErrorMessage(final String title, final String text) {
		new QuestionDialog(getParent(), title, text, DefaultLangResources.getI18NStringOrDefault("ok")).setBackgroundColor(SwtColor.LightRed).open();
	}

	protected boolean showQuestion(final String text, final String... buttonTextProperties) {
		if (buttonTextProperties == null) {
			return new QuestionDialog(getParent(), getText(), text, DefaultLangResources.getI18NStringOrDefault("ok"), DefaultLangResources.getI18NStringOrDefault("cancel")).open() == 0;
		} else {
			final String[] buttonTexts = new String[buttonTextProperties.length];
			for (int i = 0; i < buttonTextProperties.length; i++) {
				buttonTexts[i] = DefaultLangResources.getI18NStringOrDefault(buttonTextProperties[i]);
			}
			return new QuestionDialog(getParent(), getText(), text, buttonTexts).open() == 0;
		}
	}

	protected abstract void createComponents(Shell parentShell) throws Exception;
}
