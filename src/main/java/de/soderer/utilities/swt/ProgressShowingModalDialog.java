package de.soderer.utilities.swt;

import org.apache.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;

public abstract class ProgressShowingModalDialog<T> extends ModalDialog<T> {
	private static final transient Logger logger = Logger.getLogger(ProgressShowingModalDialog.class);
	
	protected ProgressDialog progressDialog;
	
	public ProgressShowingModalDialog(Shell parentShell, String title) {
		super(parentShell, SWT.TITLE | SWT.BORDER | SWT.APPLICATION_MODAL, title);
	}
	
	public ProgressShowingModalDialog(Shell parentShell, int style, String title) {   
		super(parentShell, style | SWT.TITLE | SWT.BORDER | SWT.APPLICATION_MODAL, title);
	}
	
	public void showProgress(long todo, long done) {
		final long todoParam = todo;
		final long doneParam = done;
		getParent().getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				if (progressDialog != null && !progressDialog.isDisposed()) {
					if (todoParam > 0) {
						progressDialog.setProgress(todoParam, doneParam);
					} else {
						progressDialog.setProgressPercentage(0);
					}
				}
			}
		});
	}
	
	public void showUnlimitedProgress() {
		getParent().getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				if (progressDialog != null && !progressDialog.isDisposed()) {
					progressDialog.showUnlimitedProgress();
				}
			}
		});
	}
	
	public void showDone() {
		try {
			getParent().getDisplay().asyncExec(new Runnable() {
				@Override
				public void run() {
					try {
						if (progressDialog != null && !progressDialog.isDisposed()) {
							progressDialog.close();
						}
					} catch (Exception e) {
						logger.error("Error in showDone", e);
					}
				}
			});
		} catch (Exception e) {
			logger.error("Error in showDone", e);
		}
	}
}
