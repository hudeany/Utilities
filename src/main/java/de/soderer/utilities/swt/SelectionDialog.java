package de.soderer.utilities.swt;

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

import de.soderer.utilities.LangResources;

public class SelectionDialog extends ModalDialog<String> {
	private String text;
	private List<String> items;
	private Table table;
	private Button okButton;

	public SelectionDialog(Shell shell, String title, String text, List<String> items) {
		super(shell, SWT.RESIZE, title);

		this.text = text;
		this.items = items;
	}

	@Override
	protected void createComponents(Shell shell) throws Exception {
		shell.setLayout(new FillLayout());
		shell.setSize(475, 250);
		shell.setMinimumSize(475, 250);
		Composite composite = new Composite(shell, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, true));
		composite.setLayout(SwtUtilities.createNoMarginGridLayout(2, false));

		Label label = new Label(composite, SWT.NONE);
		label.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false, 2, 1));
		label.setText(text);

		table = new Table(composite, SWT.FILL);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		table.setHeaderVisible(false);
		table.setLinesVisible(true);
		table.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				checkStatus();
			}
		});
		if (items != null && items.size() > 0) {
			for (String itemText : items) {
				TableItem tableItem = new TableItem(table, SWT.FILL);
				tableItem.setText(itemText);
			}
			table.select(items.size() - 1);
		}
		table.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDoubleClick(MouseEvent arg0) {
				if (table.getSelectionIndex() > -1) {
					setReturnValue(table.getSelection()[0].getText());
					getParent().close();
				}
			}
		});
		table.pack();
		table.setRedraw(true);

		okButton = new Button(composite, SWT.PUSH);
		okButton.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false));
		okButton.setText(LangResources.get("ok"));
		okButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (table.getSelectionIndex() > -1) {
					setReturnValue(table.getSelection()[0].getText());
				}
				getParent().close();
			}
		});

		Button cancelButton = new Button(composite, SWT.PUSH);
		cancelButton.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false));
		cancelButton.setText(LangResources.get("cancel"));
		cancelButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				getParent().close();
			}
		});

		checkStatus();
	}

	private void checkStatus() {
		okButton.setEnabled(table.getSelectionCount() > 0);
	}
}
