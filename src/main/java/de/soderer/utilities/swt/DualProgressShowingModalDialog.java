package de.soderer.utilities.swt;

import java.util.Date;

import org.apache.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;

import de.soderer.utilities.worker.WorkerParentDual;

public abstract class DualProgressShowingModalDialog<T> extends ProgressShowingModalDialog<T> implements WorkerParentDual {
	@SuppressWarnings("unused")
	private static final transient Logger logger = Logger.getLogger(DualProgressShowingModalDialog.class);

	public DualProgressShowingModalDialog(final Shell parentShell, final String title) {
		super(parentShell, SWT.TITLE | SWT.BORDER | SWT.APPLICATION_MODAL, title);
	}

	public DualProgressShowingModalDialog(final Shell parentShell, final int style, final String title) {
		super(parentShell, style | SWT.TITLE | SWT.BORDER | SWT.APPLICATION_MODAL, title);
	}

	public void showSubItemProgress(final long subItemsToDo, final long subItemsDone) {
		if (progressDialog != null && !progressDialog.isDisposed() && progressDialog instanceof DualProgressDialog) {
			final long subItemToDoParam = subItemsToDo;
			final long subItemDoneParam = subItemsDone;
			getParent().getDisplay().asyncExec(new Runnable() {
				@Override
				public void run() {
					if (progressDialog != null && !progressDialog.isDisposed() && progressDialog instanceof DualProgressDialog) {
						final DualProgressDialog<?> dualProgressDialog = (DualProgressDialog<?>) progressDialog;
						if (subItemToDoParam > 0) {
							dualProgressDialog.setSubProgress(subItemToDoParam, subItemDoneParam);
						} else {
							dualProgressDialog.setSubProgressPercentage(0);
						}
					}
				}
			});
		}
	}

	public void setSubItemTitle(final String subItemTitle) {
		if (progressDialog != null && !progressDialog.isDisposed() && progressDialog instanceof DualProgressDialog) {
			getParent().getDisplay().asyncExec(new Runnable() {
				@Override
				public void run() {
					if (progressDialog != null && !progressDialog.isDisposed() && progressDialog instanceof DualProgressDialog) {
						final DualProgressDialog<?> dualProgressDialog = (DualProgressDialog<?>) progressDialog;
						if (dualProgressDialog != null && !dualProgressDialog.isDisposed()) {
							dualProgressDialog.setSubItemTitle(subItemTitle);
						}
					}
				}
			});
		}
	}

	@Override
	public void showUnlimitedSubProgress() {
		if (progressDialog != null && !progressDialog.isDisposed() && progressDialog instanceof DualProgressDialog) {
			getParent().getDisplay().asyncExec(new Runnable() {
				@Override
				public void run() {
					if (progressDialog != null && !progressDialog.isDisposed() && progressDialog instanceof DualProgressDialog) {
						final DualProgressDialog<?> dualProgressDialog = (DualProgressDialog<?>) progressDialog;
						if (dualProgressDialog != null && !dualProgressDialog.isDisposed()) {
							dualProgressDialog.showUnlimitedProgress();
						}
					}
				}
			});
		}
	}

	@Override
	public void showItemStart(final String itemName) {
		if (progressDialog != null && !progressDialog.isDisposed() && progressDialog instanceof DualProgressDialog) {
			getParent().getDisplay().asyncExec(new Runnable() {
				@Override
				public void run() {
					if (progressDialog != null && !progressDialog.isDisposed() && progressDialog instanceof DualProgressDialog) {
						final DualProgressDialog<?> dualProgressDialog = (DualProgressDialog<?>) progressDialog;
						if (dualProgressDialog != null && !dualProgressDialog.isDisposed()) {
							dualProgressDialog.setSubItemTitle(itemName);
							dualProgressDialog.setSubProgress(100, 0);
						}
					}
				}
			});
		}
	}

	@Override
	public void showItemProgress(final Date itemStart, final long subItemToDo, final long subItemDone) {
		if (progressDialog != null && !progressDialog.isDisposed() && progressDialog instanceof DualProgressDialog) {
			getParent().getDisplay().asyncExec(new Runnable() {
				@Override
				public void run() {
					if (progressDialog != null && !progressDialog.isDisposed() && progressDialog instanceof DualProgressDialog) {
						final DualProgressDialog<?> dualProgressDialog = (DualProgressDialog<?>) progressDialog;
						if (dualProgressDialog != null && !dualProgressDialog.isDisposed()) {
							dualProgressDialog.setSubProgress(subItemToDo, subItemDone);
						}
					}
				}
			});
		}
	}

	@Override
	public void showItemDone(final Date itemStart, final Date itemEnd, final long subItemsDone) {
		if (progressDialog != null && !progressDialog.isDisposed() && progressDialog instanceof DualProgressDialog) {
			getParent().getDisplay().asyncExec(new Runnable() {
				@Override
				public void run() {
					if (progressDialog != null && !progressDialog.isDisposed() && progressDialog instanceof DualProgressDialog) {
						final DualProgressDialog<?> dualProgressDialog = (DualProgressDialog<?>) progressDialog;
						if (dualProgressDialog != null && !dualProgressDialog.isDisposed()) {
							dualProgressDialog.setSubProgress(subItemsDone, subItemsDone);
						}
					}
				}
			});
		}
	}
}
