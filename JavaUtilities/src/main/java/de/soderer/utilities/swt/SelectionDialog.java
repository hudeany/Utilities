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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

import de.soderer.utilities.LangResources;

public class SelectionDialog extends ModalDialog<String> {
	private String title;
	private String text;
	private List<String> items;
	private Table table;
	private Button okButton;

	public SelectionDialog(Shell shell, String title, String text, List<String> items) {   
		super(shell, SWT.RESIZE);
		this.title = title;
		this.text = text;
		this.items = items;
	}
	
	public SelectionDialog(Shell shell, String text, List<String> items) {   
		super(shell, SWT.RESIZE);
		this.text = text;
		this.items = items;
	}
	
	@Override
	protected void createComponents(Shell shell) throws Exception {
		if (StringUtils.isEmpty(title)) {
			shell.setText(getText());
		} else {
			shell.setText(title);
		}
		shell.setLayout(new FillLayout());
		shell.setSize(475, 250);
		shell.setMinimumSize(475, 250);
		Composite composite = new Composite(shell, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, true));
		composite.setLayout(new GridLayout(2, false));
		
		Label label = new Label(composite, SWT.FILL);
		label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, true, 2, 1));
		label.setText(text);
	    
	    table = new Table(composite, SWT.FILL);
	    table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
	    table.setHeaderVisible(false);
	    table.setLinesVisible(true);
	    table.addSelectionListener(new ItemSelectionListener());
	    if (items != null && items.size() > 0) {
	    	for (String itemText : items) {
	    		TableItem tableItem = new TableItem(table, SWT.FILL);
	    		tableItem.setText(itemText);
	    	}
	    	table.select(items.size() - 1);
	    } else {
			okButton.setEnabled(false);
	    }
	    table.pack();
	    table.setRedraw(true);
	    
	    okButton = new Button(composite, SWT.PUSH);
	    okButton.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false));
		okButton.setText(LangResources.get("ok"));
		okButton.addSelectionListener(new OkButtonSelectionListener());
		
	    Button cancelButton = new Button(composite, SWT.PUSH);
	    cancelButton.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false));
	    cancelButton.setText(LangResources.get("cancel"));
	    cancelButton.addSelectionListener(new CancelButtonSelectionListener());
	}

	private class ItemSelectionListener extends SelectionAdapter {
		@Override
		public void widgetSelected(SelectionEvent e) {
			okButton.setEnabled(true);
		}
	}

	private class OkButtonSelectionListener extends SelectionAdapter {
		@Override
		public void widgetSelected(SelectionEvent e) {
			if (table.getSelectionIndex() > -1) {
				setReturnValue(table.getItems()[table.getSelectionIndex()].getText());
			}
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
