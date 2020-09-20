package de.soderer.utilities.swt;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TypedListener;

public class RadioButtonGroup extends Composite {
	private final List<Button> radioButtons;

	public RadioButtonGroup(final Composite parent, final int style, final String... buttonTexts) throws Exception {
		this(parent, null, style, null, buttonTexts);
	}

	public RadioButtonGroup(final Composite parent, final int style, final ArrangementStyle arrangementStyle, final String... buttonTexts) throws Exception {
		this(parent, null, style, arrangementStyle, buttonTexts);
	}

	public RadioButtonGroup(final Composite parent, final String labelText, final int style, final String... buttonTexts) throws Exception {
		this(parent, labelText, style, null, buttonTexts);
	}

	public RadioButtonGroup(final Composite parent, final String labelText, final int style, ArrangementStyle arrangementStyle, final String... buttonTexts) throws Exception {
		super(parent, style);

		if (arrangementStyle == null) {
			arrangementStyle = ArrangementStyle.Vertical;
		}

		final RadioButtonGroup radioButtonGroup = this;

		radioButtons = new ArrayList<>();

		if (buttonTexts == null || buttonTexts.length < 2) {
			throw new Exception("Less than 2 texts for RadioButtonGroup");
		}

		final GridLayout gridLayout = SwtUtilities.createNoMarginGridLayout(arrangementStyle == ArrangementStyle.Vertical ? 1 : buttonTexts.length + (labelText != null ? 1 : 0), false);
		gridLayout.marginWidth = 0;
		gridLayout.marginHeight = 0;
		setLayout(gridLayout);

		if (labelText != null) {
			final Label label = new Label(this, SWT.NONE);
			label.setText(labelText + ":");
		}

		for (int i = 0; i < buttonTexts.length; i++) {
			final Button radioButton = new Button(this, SWT.RADIO);
			radioButton.setSelection(i == 0);
			radioButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
			radioButton.setText(buttonTexts[i]);
			radioButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					final Event event = new Event();
					event.widget = radioButtonGroup;
					radioButtonGroup.notifyListeners(SWT.Modify, event);
				}
			});

			radioButtons.add(radioButton);
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

	public void setSelectedButtonIndex(final int index) {
		for (int i = 0; i < radioButtons.size(); i++) {
			radioButtons.get(i).setSelection(i == index);
		}
	}

	public int getSelectedButtonIndex() {
		for (int i = 0; i < radioButtons.size(); i++) {
			if (radioButtons.get(i).getSelection()) {
				return i;
			}
		}
		throw new RuntimeException("Invalid state of RadioButtonGroup");
	}

	@Override
	public void setEnabled(final boolean enabled) {
		for (int i = 0; i < radioButtons.size(); i++) {
			radioButtons.get(i).setEnabled(enabled);
		}
	}
}
