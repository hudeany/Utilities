package de.soderer.utilities.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.TypedListener;

public class LabeledText extends Composite {
	protected StyledText textField;
	protected Text passwordTextField = null;
	
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
		
		final LabeledText labeledText = this;
		
		if (arrangementStyle == null) {
			arrangementStyle = ArrangementStyle.Vertical;
		}
		
		GridLayout gridLayout = SwtUtilities.createNoMarginGridLayout(arrangementStyle == ArrangementStyle.Vertical ? 1 : 2, false);
		gridLayout.marginWidth = 0;
		gridLayout.marginHeight = 0;
		setLayout(gridLayout);

		Label label = new Label(this, SWT.NONE);
		label.setText(labelText);
		
		if (style != SWT.PASSWORD) {
			textField = new StyledText(this, SWT.BORDER | style);
			textField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			textField.setText("");
			textField.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent arg0) {
					Event event = new Event();
					event.widget = labeledText;
					labeledText.notifyListeners(SWT.Modify, event);
				}
			});
		} else {
			passwordTextField = new Text(this, SWT.BORDER | style);
			passwordTextField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			passwordTextField.setText("");
			passwordTextField.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent arg0) {
					Event event = new Event();
					event.widget = labeledText;
					labeledText.notifyListeners(SWT.Modify, event);
				}
			});
		}
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
	public void setEnabled(boolean enabled) {
		if (passwordTextField == null) {
			textField.setEnabled(enabled);
		} else {
			passwordTextField.setEnabled(enabled);
		}
	}
}
