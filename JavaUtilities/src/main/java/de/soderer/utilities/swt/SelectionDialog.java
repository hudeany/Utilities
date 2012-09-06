package de.soderer.utilities.swt;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import de.soderer.utilities.LangResources;

public class SelectionDialog extends ModalDialog<String> {
	private String title;
	private String text;
	private List<String> items;
	private Combo comboBox;
	private Button okButton;

	public SelectionDialog(Shell shell, String title, String text, List<String> items) {   
		super(shell);
		this.title = title;
		this.text = text;
		this.items = items;
	}
	
	public SelectionDialog(Shell shell, String text, List<String> items) {   
		super(shell);
		this.text = text;
		this.items = items;
	}
	
	@Override
	protected void createComponents(Shell shell) throws Exception {
		if (StringUtils.isEmpty(title))
			shell.setText(getText());
		else
			shell.setText(title);
		shell.setLayout(new FillLayout());
		shell.setSize(475, 120);
		shell.setMinimumSize(475, 120);
		Composite composite = new Composite(shell, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, true));
		composite.setLayout(new GridLayout(2, false));
		
		Label label = new Label(composite, SWT.FILL);
		label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, true, 2, 1));
		label.setText(text);
		
		comboBox = new Combo(composite, SWT.FILL | SWT.READ_ONLY);
		comboBox.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		comboBox.addSelectionListener(new ComboBoxButtonSelectionListener());
	    if (items != null && items.size() > 0) {
		    comboBox.setItems(items.toArray(new String[items.size()]));
	    	comboBox.select(comboBox.getItemCount() - 1);
	    }
	    
	    okButton = new Button(composite, SWT.PUSH);
	    okButton.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false));
		okButton.setText(LangResources.get("ok"));
		okButton.setEnabled(comboBox.getItemCount() > 0);
		okButton.addSelectionListener(new OkButtonSelectionListener());
		
	    Button cancelButton = new Button(composite, SWT.PUSH);
	    cancelButton.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false));
	    cancelButton.setText(LangResources.get("cancel"));
	    cancelButton.addSelectionListener(new CancelButtonSelectionListener());
	}

	private class ComboBoxButtonSelectionListener extends SelectionAdapter {
		@Override
		public void widgetSelected(SelectionEvent e) {
			okButton.setEnabled(true);
		}
	}

	private class OkButtonSelectionListener extends SelectionAdapter {
		@Override
		public void widgetSelected(SelectionEvent e) {
			if (comboBox.getSelectionIndex() > -1)
				setReturnValue(comboBox.getItems()[comboBox.getSelectionIndex()]);
			getShell().close();
		}
	}

	private class CancelButtonSelectionListener extends SelectionAdapter {
		@Override
		public void widgetSelected(SelectionEvent e) {
			getShell().close();
		}
	}
}
