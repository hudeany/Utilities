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

public class DualProgressDialog extends ProgressDialog {
	private String subCommentStringFormat;
	private Label subCommentLabel;
	private ProgressBar subBar;

	public DualProgressDialog(String title, String commentStringFormat, String subCommentStringFormat, Shell shell) {   
		super(title, commentStringFormat, shell);
		
		this.subCommentStringFormat = subCommentStringFormat;
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
					commentLabel.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false));
				}

				bar = new ProgressBar(composite, SWT.SMOOTH);
				bar.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false));
				bar.setMinimum(0);
				bar.setMaximum(100);
				bar.setSelection(0);
				
				subCommentLabel = new Label(composite, SWT.NONE);
				subCommentLabel.setText(MessageFormat.format(subCommentStringFormat, 0, 0, 0));
				subCommentLabel.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false));
				
				subBar = new ProgressBar(composite, SWT.SMOOTH);
				subBar.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false));
				subBar.setMinimum(0);
				subBar.setMaximum(100);
				subBar.setSelection(0);
	
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
	
	public void setSubProgressPercentage(int value) {
		if (subBar != null) {
			subBar.setSelection(value);
		}
	}
	
	public void setSubItemTitle(String subItemTitle) {
		subCommentStringFormat = subItemTitle;
		if (subCommentLabel != null) {
			subCommentLabel.setText(MessageFormat.format(subCommentStringFormat, 0, 0, 0));
		}
	}
	
	public void setSubProgress(long todo, long done) {
		if (subBar != null) {
			subBar.setSelection((int) (done * 100 / todo));
		}
		
		if (subCommentLabel != null) {
			subCommentLabel.setText(MessageFormat.format(subCommentStringFormat, done, todo, done * 100 / todo));
		}
	}
}
