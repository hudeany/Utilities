package de.soderer.utilities.swt;

import java.text.DecimalFormat;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Text;

public class NumberText extends Text {
	private boolean showMessageBoxOnInvalidData = false;
	private DecimalFormat decimalFormat;

	public NumberText(Composite parent, int style) {
		this(parent, style, -1, false);
	}

	public NumberText(Composite parent, int style, int maxLength) {
		this(parent, style, maxLength, false);
	}

	public NumberText(Composite parent, int style, int maxLength, boolean pShowMessageBoxOnInvalidData) {
		super(parent, style);

		showMessageBoxOnInvalidData = pShowMessageBoxOnInvalidData;

		if (maxLength > 0) {
			setTextLimit(maxLength);
		}

		addVerifyListener(new VerifyListener() {
			@Override
			public void verifyText(VerifyEvent event) {
				event.doit = event.text.trim().length() == 0 || event.text.matches("\\d*");
				if (!event.doit && showMessageBoxOnInvalidData) {
					MessageBox messageBox = new MessageBox(getShell(), SWT.ICON_ERROR | SWT.OK);
					messageBox.setText("Ungültige Eingabedaten");
					messageBox.setMessage("Dieses Eingabefeld lässt nur Ziffern zu!");
					messageBox.open();
				}
			}
		});

		setText("0");
	}

	@Override
	protected void checkSubclass() {
		// do nothing
	}

	public int getValue() {
		if (getText().trim().length() == 0) {
			return 0;
		} else {
			return Integer.parseInt(getText().trim());
		}
	}

	public void setValue(int value) {
		if (decimalFormat != null) {
			setText(decimalFormat.format(value));
		} else {
			setText(Integer.toString(value));
		}
	}

	public void setDisplayFormat(String formatString) {
		decimalFormat = new DecimalFormat(formatString);
		setText(decimalFormat.format(Integer.parseInt(getText())));
	}
}
