package de.soderer.utilities.swt;

import java.text.MessageFormat;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;

import de.soderer.utilities.LangResources;
import de.soderer.utilities.Utilities;

public class ProgressDialog extends ModalDialog<Boolean> {
	protected String commentStringFormat;
	protected Label commentLabel;
	protected ProgressBar bar;
	protected boolean isClosed = false;
	private Thread unlimitedRunner = null;

	public ProgressDialog(Shell shell, String title, String commentStringFormat) {   
		super(shell, title);
		
		this.commentStringFormat = commentStringFormat;
	}
	
	@Override
	protected void createComponents(Shell shell) throws Exception {
		try {
			if (isClosed) {
				shell.close();
			} else {
				shell.setLayout(new GridLayout(1, true));
				
				Composite composite = new Composite(shell, SWT.NONE);
				composite.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false));
				composite.setLayout(SwtUtilities.createNoMarginGridLayout(1, true));
				
				if (Utilities.isNotBlank(commentStringFormat)) {
					commentLabel = new Label(composite, SWT.NONE);
					commentLabel.setText(MessageFormat.format(commentStringFormat, 0, 0, 0));
					GridData labelGridData = new GridData(SWT.FILL, SWT.UP, true, false);
					commentLabel.setLayoutData(labelGridData);
				}
				
				bar = new ProgressBar(composite, SWT.SMOOTH);
				bar.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false));
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
						getParent().close();
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
		stopUnlimitedProgress();
		
		if (bar != null) {
			bar.setSelection(value);
		}
	}
	
	public void setProgress(long todo, long done) {
		stopUnlimitedProgress();
		
		if (bar != null) {
			bar.setSelection((int) (done * 100 / todo));
		}
		
		if (commentLabel != null) {
			commentLabel.setText(MessageFormat.format(commentStringFormat, done, todo, done * 100 / todo));
		}
	}
	
	public void showUnlimitedProgress() {
		unlimitedRunner = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					while (true) {
						getParent().getDisplay().asyncExec(new Runnable() {
							@Override
							public void run() {
								int nextValue;
								if (bar.getMaximum() == 100) {
									nextValue = bar.getSelection() + 10;
								} else {
									nextValue = bar.getSelection() - 10;
								}
								
								if (nextValue > 100) {
									nextValue = 100;
									bar.setMaximum(101);
								} else if (nextValue < 0) {
									nextValue = 0;
									bar.setMaximum(100);
								}
								
								bar.setSelection(nextValue);
							}
						});
						
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							// do nothing
						}
					}
				} catch (Throwable e) {
					// do nothing
				}
			}
		});
		unlimitedRunner.start();
	}
	
	@SuppressWarnings("deprecation")
	public void stopUnlimitedProgress() {
		if (unlimitedRunner != null) {
			unlimitedRunner.stop();
			unlimitedRunner = null;
			bar.setMaximum(100);
		}
	}
	
	public void close() {
		isClosed = true;
		setReturnValue(false);
		getParent().close();
	}
	
	@Override
	protected Boolean getDefaultReturnValue() {
		return false;
	}
}
