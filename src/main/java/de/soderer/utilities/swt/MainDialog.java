package de.soderer.utilities.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import de.soderer.utilities.DefaultLangResources;

public abstract class MainDialog extends Shell {
	private static final int GTK_TITLEBAR_EXTRA_WIDTH = 100;

	private final Display display;

	public MainDialog(final Display display) {
		super(display, SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MIN | SWT.MAX);

		this.display = display;
	}

	@Override
	public final Display getDisplay() {
		return display;
	}

	@Override
	public void open() {
		// Must happen before opening, so SWT and GTK agree on the window width
		ensureMinimumWidthForTitle(this);
		super.open();
		relayoutAfterOpen(this);
	}

	public void run() {
		try {
			open();
			while (!isDisposed()) {
				if (!display.readAndDispatch()) {
					display.sleep();
				}
			}
		} catch (final Exception e) {
			e.printStackTrace();
			throw e;
		}
	}

	@Override
	protected void checkSubclass() {
		// do nothing
	}

	public boolean showQuestion(final String text, final String... buttonTextProperties) {
		if (buttonTextProperties == null || buttonTextProperties.length == 0) {
			return new QuestionDialog(getShell(), getText(), text, DefaultLangResources.getI18NStringOrDefault("ok"), DefaultLangResources.getI18NStringOrDefault("cancel")).open() == 0;
		} else {
			final String[] buttonTexts = new String[buttonTextProperties.length];
			for (int i = 0; i < buttonTextProperties.length; i++) {
				buttonTexts[i] = DefaultLangResources.getI18NStringOrDefault(buttonTextProperties[i]);
			}
			return new QuestionDialog(getShell(), getText(), text, buttonTexts).open() == 0;
		}
	}

	public static boolean isGtk() {
		return "gtk".equals(SWT.getPlatform());
	}

	/**
	 * Call after the shell content was created and packed/sized, but before the shell is opened and positioned.
	 */
	public static void ensureMinimumWidthForTitle(final Shell shell) {
		if (shell == null || shell.isDisposed() || !isGtk()) {
			return;
		}

		final String title = shell.getText();
		if (title == null || title.isEmpty()) {
			return;
		}

		final int titleWidth = measureTitleWidth(shell, title);
		final int maximumWidth = shell.getDisplay().getClientArea().width;
		final int requiredWidth = Math.min(titleWidth + GTK_TITLEBAR_EXTRA_WIDTH, maximumWidth);

		// Keep SWT's idea of the minimum size consistent with what GTK enforces anyway
		final Point minimumSize = shell.getMinimumSize();
		if (minimumSize.x < requiredWidth) {
			shell.setMinimumSize(requiredWidth, minimumSize.y);
		}

		final Point size = shell.getSize();
		if (size.x < requiredWidth) {
			shell.setSize(requiredWidth, size.y);
			shell.layout(true, true);
		}
	}

	/**
	 * Call directly after shell.open(). GTK may still adjust the window allocation after mapping the window,
	 * so the content is laid out once more afterwards.
	 */
	public static void relayoutAfterOpen(final Shell shell) {
		if (shell == null || shell.isDisposed() || !isGtk()) {
			return;
		}

		shell.getDisplay().asyncExec(() -> {
			if (!shell.isDisposed()) {
				shell.layout(true, true);
			}
		});
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
}
