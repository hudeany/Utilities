package de.soderer.utilities.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TypedListener;

public class LabeledText extends Composite {
	protected StyledText textField;
	
	public enum ArrangementStyle {
		Vertical,
		Horizontal
	}
	
	protected LabeledText(Composite parent, int style) throws Exception {
		super(parent, style);
	}

	public LabeledText(Composite parent, int style, String labelText) throws Exception {
		this(parent, style, labelText, null);
	}
	
	public LabeledText(Composite parent, int style, String labelText, ArrangementStyle arrangementStyle) throws Exception {
		super(parent, style);
		
		if (arrangementStyle == null) {
			arrangementStyle = ArrangementStyle.Vertical;
		}
		
		GridLayout gridLayout = new GridLayout(arrangementStyle == ArrangementStyle.Vertical ? 1 : 2, false);
		gridLayout.marginWidth = 0;
		gridLayout.marginHeight = 0;
		setLayout(gridLayout);

		Label label = new Label(this, SWT.NONE);
		label.setText(labelText);
		
		textField = new StyledText(this, SWT.BORDER | style);
		textField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		textField.setText("");
	}
	
	public void addModifyListener(ModifyListener listener) {
		checkWidget();
		if (listener == null) {
			throw new RuntimeException("Missing listener");
		} else {
			TypedListener typedListener = new TypedListener(listener);
			addListener(SWT.Modify, typedListener);
		}
	}
	
	public String getText() {
		return textField.getText();
	}
	
	public void setText(String text) {
		textField.setText(text);
	}
	
	@Override
	public void setEnabled(boolean enabled) {
		textField.setEnabled(enabled);
	}
}
