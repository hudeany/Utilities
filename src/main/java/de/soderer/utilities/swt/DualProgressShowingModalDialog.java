package de.soderer.utilities.swt;

import java.util.Date;

import org.apache.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;

import de.soderer.utilities.WorkerParentDual;

public abstract class DualProgressShowingModalDialog<T> extends ProgressShowingModalDialog<T> implements WorkerParentDual {
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

	@Override
	public void showUnlimitedSubProgress() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void showItemStart(String itemName) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void showItemProgress(Date itemStart, long subItemToDo, long subItemDone) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void showItemDone(Date itemStart, Date itemEnd, long subItemsDone) {
		// TODO Auto-generated method stub
		
	}
}
