package de.soderer.utilities.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import de.soderer.utilities.LangResources;

public abstract class MainDialog extends Shell {
	private Display display;

	public MainDialog(Display display) throws Exception {
		super(display, SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MIN | SWT.MAX);

		this.display = display;
	}

	@Override
	public Display getDisplay() {
		return display;
	}

	public void run() throws Exception {
		try {
			open();
			Display display = getDisplay();
			while (!isDisposed()) {
				if (!display.readAndDispatch()) {
					display.sleep();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}

	@Override
	protected void checkSubclass() {
		// do nothing
	}

	public boolean showQuestion(String text, String... buttonTextProperties) {
		if (buttonTextProperties == null) {
			return new QuestionDialog(getShell(), text, LangResources.get("ok"), LangResources.get("cancel")).open() == 0;
		} else {
			String[] buttonTexts = new String[buttonTextProperties.length];
			for (int i = 0; i < buttonTextProperties.length; i++) {
				buttonTexts[i] = LangResources.get(buttonTextProperties[i]);
			}
			return new QuestionDialog(getShell(), getText(), text, buttonTexts).open() == 0;
		}
	}
}
