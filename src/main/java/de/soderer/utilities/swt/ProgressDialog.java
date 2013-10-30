package de.soderer.utilities.swt;

import java.text.MessageFormat;

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
import org.eclipse.swt.widgets.ProgressBar;

import de.soderer.utilities.LangResources;

public class ProgressDialog extends ModalDialog<Boolean> {
	protected String title;
	protected String commentStringFormat;
	protected Label commentLabel;
	protected ProgressBar bar;
	protected boolean isClosed = false;

	public ProgressDialog(String title, String commentStringFormat, Shell shell) {   
		super(shell);
		
		this.title = title;
		this.commentStringFormat = commentStringFormat;
	}
	
	@Override
	protected void createComponents(Shell shell) throws Exception {
		try {
			if (isClosed) {
				shell.close();
			} else {
				if (StringUtils.isEmpty(title)) {
					shell.setText(getText());
				} else {
					shell.setText(title);
				}
				shell.setLayout(new FillLayout());
				
				Composite composite = new Composite(shell, SWT.NONE);
				composite.setLayout(new GridLayout(1, true));
				
				if (StringUtils.isNotBlank(commentStringFormat)) {
					commentLabel = new Label(composite, SWT.NONE);
					commentLabel.setText(MessageFormat.format(commentStringFormat, 0, 0, 0));
					GridData labelGridData = new GridData(SWT.FILL, SWT.UP, true, false, 1, 1);
					commentLabel.setLayoutData(labelGridData);
				}
				
				bar = new ProgressBar(composite, SWT.SMOOTH);
				GridData gridData = new GridData(SWT.FILL, SWT.UP, true, false);
				bar.setLayoutData(gridData);
				bar.setMinimum(0);
				bar.setMaximum(100);
				bar.setSelection(0);
	
				Button buttonClose = new Button(composite, SWT.PUSH);
				buttonClose.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false));
				buttonClose.setText(LangResources.get("cancel"));
				buttonClose.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						setReturnValue(true);
						getShell().close();
					}
				});
	
				shell.pack();
	
				shell.setMinimumSize(400, shell.getSize().y);
				shell.setSize(400, shell.getSize().y);
			}
		} catch (Exception e) {
			throw e;
		}
	}
	
	public void setProgressPercentage(int value) {
		if (bar != null) {
			bar.setSelection(value);
		}
	}
	
	public void setProgress(long todo, long done) {
		if (bar != null) {
			bar.setSelection((int) (done * 100 / todo));
		}
		
		if (commentLabel != null) {
			commentLabel.setText(MessageFormat.format(commentStringFormat, done, todo, done * 100 / todo));
		}
	}
	
	public void close() {
		isClosed = true;
		setReturnValue(false);
		getShell().close();
	}
	
	@Override
	protected Boolean getDefaultReturnValue() {
		return false;
	}
}
