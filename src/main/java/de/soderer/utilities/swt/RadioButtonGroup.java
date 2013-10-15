package de.soderer.utilities.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

public class RadioButtonGroup extends Composite {
	public enum ArrangementStyle {
		Vertical,
		Horizontal
	}
	
	private int selectedButtonIndex;

	public RadioButtonGroup (Composite parent, int style, String... buttonTexts) throws Exception {
		this(null, parent, style, ArrangementStyle.Vertical, 0, buttonTexts);
	}

	public RadioButtonGroup (String labelText, Composite parent, int style, String... buttonTexts) throws Exception {
		this(labelText, parent, style, ArrangementStyle.Vertical, 0, buttonTexts);
	}
	
	public RadioButtonGroup (String labelText, Composite parent, int style, ArrangementStyle arrangementStyle, int selectedButtonIndexParam, String... buttonTexts) throws Exception {
		super(parent, style);
		
		selectedButtonIndex = selectedButtonIndexParam;
		
		if (buttonTexts == null || buttonTexts.length < 2) {
			throw new Exception("Less than 2 texts for RadioButtonGroup");
		}
		
		setLayout(new GridLayout(arrangementStyle == ArrangementStyle.Vertical ? 1 : buttonTexts.length, true));

		if (labelText != null) {
			Label label = new Label(this, SWT.NONE);
			label.setText(labelText + ":");
		}
		
		for (int i = 0; i < buttonTexts.length; i++) {
			String text = buttonTexts[i];
			Button radioButton = new Button(this, SWT.RADIO);
			radioButton.setSelection(i == selectedButtonIndex);
			radioButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
			radioButton.setText(text);
			final int radioButtonIndex = i;
			radioButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					selectedButtonIndex = radioButtonIndex;
				}
			});
		}
	}
	
	public int getSelectedButtonIndex() {
		return selectedButtonIndex;
	}
}
