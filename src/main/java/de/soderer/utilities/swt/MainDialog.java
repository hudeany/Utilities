package de.soderer.utilities.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import de.soderer.utilities.LangResources;

public abstract class MainDialog extends Shell {
	private final Display display;

	public MainDialog(final Display display) {
		super(display, SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MIN | SWT.MAX);

		this.display = display;
	}

	@Override
	public final Display getDisplay() {
		return display;
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
		if (buttonTextProperties == null) {
			return new QuestionDialog(getShell(), text, LangResources.get("ok"), LangResources.get("cancel")).open() == 0;
		} else {
			final String[] buttonTexts = new String[buttonTextProperties.length];
			for (int i = 0; i < buttonTextProperties.length; i++) {
				buttonTexts[i] = LangResources.get(buttonTextProperties[i]);
			}
			return new QuestionDialog(getShell(), getText(), text, buttonTexts).open() == 0;
		}
	}
}
