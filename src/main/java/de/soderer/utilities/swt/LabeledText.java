package de.soderer.utilities.swt;

import java.util.regex.Pattern;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.TypedListener;

public class LabeledText extends Composite {
	protected Text textField;
	protected Text passwordTextField = null;

	protected LabeledText(final Composite parent, final int style) throws Exception {
		super(parent, style);
	}

	public LabeledText(final Composite parent, final int style, final String labelText) throws Exception {
		this(parent, style, labelText, null);
	}

	public LabeledText(final Composite parent, final int style, final String labelText, final ArrangementStyle arrangementStyle) throws Exception {
		this(parent, style, labelText, arrangementStyle, null);
	}

	public LabeledText(final Composite parent, final int style, final String labelText, ArrangementStyle arrangementStyle, final String verifyPattern) throws Exception {
		super(parent, style);

		final LabeledText labeledText = this;

		if (arrangementStyle == null) {
			arrangementStyle = ArrangementStyle.Vertical;
		}

		setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true, 1, 1));
		setLayout(SwtUtilities.createNoMarginGridLayout(arrangementStyle == ArrangementStyle.Vertical ? 1 : 2, false));

		final Label label = new Label(this, SWT.NONE);
		label.setText(labelText);

		if (style != SWT.PASSWORD) {
			textField = new Text(this, SWT.BORDER | style);
			textField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			textField.setText("");
			textField.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(final ModifyEvent arg0) {
					final Event event = new Event();
					event.widget = labeledText;
					labeledText.notifyListeners(SWT.Modify, event);
				}
			});

			if (verifyPattern != null) {
				textField.addVerifyListener(new VerifyListener() {
					@Override
					public void verifyText(final VerifyEvent e) {
						final Text text = (Text) e.getSource();

						final String oldContent = text.getText();
						final String newContent = oldContent.substring(0, e.start) + e.text + oldContent.substring(e.end);

						if (!Pattern.matches(verifyPattern, newContent)) {
							e.doit = false;
						}
					}
				});
			}
		} else {
			passwordTextField = new Text(this, SWT.BORDER | style);
			passwordTextField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			passwordTextField.setText("");
			passwordTextField.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(final ModifyEvent arg0) {
					final Event event = new Event();
					event.widget = labeledText;
					labeledText.notifyListeners(SWT.Modify, event);
				}
			});
		}
	}

	public void addModifyListener(final ModifyListener listener) throws Exception {
		checkWidget();
		if (listener == null) {
			throw new Exception("Missing listener");
		} else {
			final TypedListener typedListener = new TypedListener(listener);
			addListener(SWT.Modify, typedListener);
		}
	}

	public String getText() {
		if (passwordTextField == null) {
			return textField.getText();
		} else {
			return passwordTextField.getText();
		}
	}

	public void setText(String text) {
		if (text == null) {
			text = "";
		}

		if (passwordTextField == null) {
			textField.setText(text);
		} else {
			passwordTextField.setText(text);
		}
	}

	@Override
	public void setEnabled(final boolean enabled) {
		if (passwordTextField == null) {
			textField.setEnabled(enabled);
		} else {
			passwordTextField.setEnabled(enabled);
		}
	}

	public void setReadOnly(final boolean readOnly) {
		if (passwordTextField == null) {
			textField.setEditable(!readOnly);
		} else {
			passwordTextField.setEditable(!readOnly);
		}
	}
}
