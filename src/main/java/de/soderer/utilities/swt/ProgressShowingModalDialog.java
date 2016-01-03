package de.soderer.utilities.swt;

import java.util.Date;

import org.apache.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;

import de.soderer.utilities.WorkerParentSimple;

public abstract class ProgressShowingModalDialog<T> extends ModalDialog<T> implements WorkerParentSimple {
	private static final transient Logger logger = Logger.getLogger(ProgressShowingModalDialog.class);
	
	protected ProgressDialog progressDialog;
	
	public ProgressShowingModalDialog(Shell parentShell, String title) {
		super(parentShell, SWT.TITLE | SWT.BORDER | SWT.APPLICATION_MODAL, title);
	}
	
	public ProgressShowingModalDialog(Shell parentShell, int style, String title) {   
		super(parentShell, style | SWT.TITLE | SWT.BORDER | SWT.APPLICATION_MODAL, title);
	}

	@Override
	public void showUnlimitedProgress() {
		try {
			getParent().getDisplay().asyncExec(new Runnable() {
				@Override
				public void run() {
					if (progressDialog != null && !progressDialog.isDisposed()) {
						progressDialog.showUnlimitedProgress();
					}
				}
			});
		} catch (Exception e) {
			logger.error("Error in showUnlimitedProgress", e);
		}
	}

	@Override
	public void showProgress(Date start, final long itemsToDo, final long itemsDone) {
		getParent().getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				if (progressDialog != null && !progressDialog.isDisposed()) {
					if (itemsToDo > 0) {
						progressDialog.setProgress(itemsToDo, itemsDone);
					} else {
						progressDialog.setProgressPercentage(0);
					}
				}
			}
		});
	}

	@Override
	public void showDone(Date start, Date end, long itemsDone) {
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
	
	@Override
	public void cancel() {
		// Do nothing
	}
}
