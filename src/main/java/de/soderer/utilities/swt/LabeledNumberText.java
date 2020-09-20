package de.soderer.utilities.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TypedListener;

public class LabeledNumberText extends Composite {
	protected NumberText textField;

	protected LabeledNumberText(final Composite parent, final int style) throws Exception {
		super(parent, style);
	}

	public LabeledNumberText(final Composite parent, final int style, final String labelText) throws Exception {
		this(parent, style, labelText, null);
	}

	public LabeledNumberText(final Composite parent, final int style, final String labelText, ArrangementStyle arrangementStyle) throws Exception {
		super(parent, style);

		final LabeledNumberText labeledText = this;

		if (arrangementStyle == null) {
			arrangementStyle = ArrangementStyle.Vertical;
		}

		final GridLayout gridLayout = SwtUtilities.createNoMarginGridLayout(arrangementStyle == ArrangementStyle.Vertical ? 1 : 2, false);
		gridLayout.marginWidth = 0;
		gridLayout.marginHeight = 0;
		setLayout(gridLayout);

		final Label label = new Label(this, SWT.NONE);
		label.setText(labelText);

		textField = new NumberText(this, SWT.BORDER | style);
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

	public int getValue() {
		return textField.getValue();
	}

	public void setValue(final int value) {
		textField.setValue(value);
	}

	@Override
	public void setEnabled(final boolean enabled) {
		textField.setEnabled(enabled);
	}
}
