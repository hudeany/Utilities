package de.soderer.utilities.swt;

import org.apache.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;

public abstract class DualProgressShowingModalDialog<T> extends ProgressShowingModalDialog<T> {
	@SuppressWarnings("unused")
	private static final transient Logger logger = Logger.getLogger(DualProgressShowingModalDialog.class);
	
	public DualProgressShowingModalDialog(Shell parentShell) {
		super(parentShell, SWT.TITLE | SWT.BORDER | SWT.APPLICATION_MODAL);
	}
	
	public DualProgressShowingModalDialog(Shell parentShell, int style) {   
		super(parentShell, style | SWT.TITLE | SWT.BORDER | SWT.APPLICATION_MODAL);
	}
	
	public void showSubItemProgress(long subItemsTodo, long subItemsDone) {
		final long subItemTodoParam = subItemsTodo;
		final long subItemDoneParam = subItemsDone;
		getShell().getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				if (progressDialog instanceof DualProgressDialog) {
					DualProgressDialog dualProgressDialog = (DualProgressDialog) progressDialog;
					if (dualProgressDialog != null && !dualProgressDialog.isDisposed()) {
						if (subItemTodoParam > 0) {
							dualProgressDialog.setSubProgress(subItemTodoParam, subItemDoneParam);
						} else {
							dualProgressDialog.setSubProgressPercentage(0);
						}
					}
				}
			}
		});
	}
	
	public void setSubItemTitle(final String subItemTitle) {
		getShell().getDisplay().asyncExec(new Runnable() {
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
