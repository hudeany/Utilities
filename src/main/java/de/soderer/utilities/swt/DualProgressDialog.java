package de.soderer.utilities.swt;

import java.text.MessageFormat;
import java.util.Date;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;

import de.soderer.utilities.worker.WorkerDual;
import de.soderer.utilities.worker.WorkerParentDual;

public class DualProgressDialog<T extends WorkerDual<?>> extends ProgressDialog<T> implements WorkerParentDual {
	private String subCommentStringFormat;
	private Label subItemLabel;
	private ProgressBar subItemProgressBar;

	public DualProgressDialog(final Shell shell, final String title, final String commentStringFormat) {
		this(shell, title, commentStringFormat, null);
	}

	public DualProgressDialog(final Shell shell, final String title, final String commentStringFormat, final T worker) {
		super(shell, title, commentStringFormat, worker);
	}

	@Override
	protected void createAdditionalComponents(final Composite composite) {
		subItemLabel = new Label(composite, SWT.NONE);
		subCommentStringFormat = "Item";
		subItemLabel.setText("Item");
		subItemLabel.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false));

		subItemProgressBar = new ProgressBar(composite, SWT.SMOOTH);
		subItemProgressBar.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false));
		subItemProgressBar.setMinimum(0);
		subItemProgressBar.setMaximum(100);
		subItemProgressBar.setSelection(0);
	}

	protected void setSubProgress(final long todo, final long done) {
		if (!isClosed) {
			subItemProgressBar.setSelection((int) (done * 100 / todo));
			subItemLabel.setText(MessageFormat.format(subCommentStringFormat, done, todo, done * 100 / todo));
		}
	}

	@Override
	public void showUnlimitedSubProgress() {
		if (!isClosed && !isDisposed()) {
			getParent().getDisplay().asyncExec(new Runnable() {
				@Override
				public void run() {
					if (!isClosed && !isDisposed()) {
						showUnlimitedProgress();
					}
				}
			});
		}
	}

	@Override
	public void showItemStart(final String itemName) {
		if (!isClosed && !isDisposed()) {
			getParent().getDisplay().asyncExec(new Runnable() {
				@Override
				public void run() {
					if (!isClosed && !isDisposed()) {
						subCommentStringFormat = itemName;
						subItemLabel.setText(MessageFormat.format(subCommentStringFormat, 0, 0, 0));
						setSubProgress(100, 0);
					}
				}
			});
		}
	}

	@Override
	public void showItemProgress(final Date itemStart, final long subItemToDo, final long subItemDone) {
		if (!isClosed && !isDisposed()) {
			getParent().getDisplay().asyncExec(new Runnable() {
				@Override
				public void run() {
					if (!isClosed && !isDisposed()) {
						setSubProgress(subItemToDo, subItemDone);
					}
				}
			});
		}
	}

	@Override
	public void showItemDone(final Date itemStart, final Date itemEnd, final long subItemsDone) {
		if (!isClosed && !isDisposed()) {
			getParent().getDisplay().asyncExec(new Runnable() {
				@Override
				public void run() {
					if (!isClosed && !isDisposed()) {
						setSubProgress(subItemsDone, subItemsDone);
					}
				}
			});
		}
	}
}
