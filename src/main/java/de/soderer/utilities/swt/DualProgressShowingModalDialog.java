package de.soderer.utilities.swt;

import org.apache.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;

public abstract class DualProgressShowingModalDialog<T> extends ProgressShowingModalDialog<T> {
	@SuppressWarnings("unused")
	private static final transient Logger logger = Logger.getLogger(DualProgressShowingModalDialog.class);
	
	public DualProgressShowingModalDialog(Shell parentShell, String title) {
		super(parentShell, SWT.TITLE | SWT.BORDER | SWT.APPLICATION_MODAL, title);
	}
	
	public DualProgressShowingModalDialog(Shell parentShell, int style, String title) {   
		super(parentShell, style | SWT.TITLE | SWT.BORDER | SWT.APPLICATION_MODAL, title);
	}
	
	public void showSubItemProgress(long subItemsToDo, long subItemsDone) {
		final long subItemToDoParam = subItemsToDo;
		final long subItemDoneParam = subItemsDone;
		getParent().getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				if (progressDialog instanceof DualProgressDialog) {
					DualProgressDialog dualProgressDialog = (DualProgressDialog) progressDialog;
					if (dualProgressDialog != null && !dualProgressDialog.isDisposed()) {
						if (subItemToDoParam > 0) {
							dualProgressDialog.setSubProgress(subItemToDoParam, subItemDoneParam);
						} else {
							dualProgressDialog.setSubProgressPercentage(0);
						}
					}
				}
			}
		});
	}
	
	public void setSubItemTitle(final String subItemTitle) {
		getParent().getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				if (progressDialog instanceof DualProgressDialog) {
					DualProgressDialog dualProgressDialog = (DualProgressDialog) progressDialog;
					if (dualProgressDialog != null && !dualProgressDialog.isDisposed()) {
						dualProgressDialog.setSubItemTitle(subItemTitle);
					}
				}
			}
		});
	}
}
