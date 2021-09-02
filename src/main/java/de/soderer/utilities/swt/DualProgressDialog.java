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
	private Label subCommentLabel;
	private ProgressBar subBar;

	public DualProgressDialog(final Shell shell, final String title, final String commentStringFormat, final String subCommentStringFormat) {
		this(shell, title, commentStringFormat, subCommentStringFormat, null);
	}

	public DualProgressDialog(final Shell shell, final String title, final String commentStringFormat, final String subCommentStringFormat, final T worker) {
		super(shell, title, commentStringFormat, worker);

		this.subCommentStringFormat = subCommentStringFormat;
	}

	@Override
	protected void createAdditionalComponents(final Composite composite) {
		subCommentLabel = new Label(composite, SWT.NONE);
		subCommentLabel.setText(MessageFormat.format(subCommentStringFormat, 0, 0, 0));
		subCommentLabel.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false));

		subBar = new ProgressBar(composite, SWT.SMOOTH);
		subBar.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false));
		subBar.setMinimum(0);
		subBar.setMaximum(100);
		subBar.setSelection(0);
	}

	public void setSubItemTitle(final String subItemTitle) {
		if (!isClosed) {
			subCommentStringFormat = subItemTitle;
			if (subCommentLabel != null) {
				subCommentLabel.setText(MessageFormat.format(subCommentStringFormat, 0, 0, 0));
			}
		}
	}

	public void setSubProgressPercentage(final int value) {
		if (!isClosed) {
			if (subBar != null) {
				subBar.setSelection(value);
			}
		}
	}

	public void setSubProgress(final long todo, final long done) {
		if (!isClosed) {
			if (subBar != null) {
				subBar.setSelection((int) (done * 100 / todo));
			}

			if (subCommentLabel != null) {
				subCommentLabel.setText(MessageFormat.format(subCommentStringFormat, done, todo, done * 100 / todo));
			}
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
						setSubItemTitle(itemName);
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
