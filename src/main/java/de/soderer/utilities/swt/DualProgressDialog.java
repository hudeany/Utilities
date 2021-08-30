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
import de.soderer.utilities.Result;
import de.soderer.utilities.Utilities;
import de.soderer.utilities.worker.WorkerDual;

public class DualProgressDialog<T extends WorkerDual<?>> extends ProgressDialog<T> {
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
	protected void createComponents(final Shell shell) throws Exception {
		try {
			if (isClosed) {
				shell.close();
			} else {
				shell.setLayout(new GridLayout(1, true));

				final Composite composite = new Composite(shell, SWT.NONE);
				composite.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false));
				composite.setLayout(SwtUtilities.createNoMarginGridLayout(1, true));

				if (Utilities.isNotBlank(commentStringFormat)) {
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

				final Button buttonClose = new Button(composite, SWT.PUSH);
				buttonClose.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false));
				buttonClose.setText(LangResources.get("cancel"));
				buttonClose.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						setReturnValue(Result.CANCELED);
						canceledByUser = true;
						if (worker != null) {
							worker.cancel();
						}
						getParent().close();
					}
				});

				shell.pack();

				shell.setMinimumSize(400, shell.getSize().y);
				shell.setSize(400, shell.getSize().y);
			}
		} catch (final Exception e) {
			throw e;
		}
	}

	public void setSubProgressPercentage(final int value) {
		if (!isClosed) {
			if (subBar != null) {
				subBar.setSelection(value);
			}
		}
	}

	public void setSubItemTitle(final String subItemTitle) {
		if (!isClosed) {
			subCommentStringFormat = subItemTitle;
			if (subCommentLabel != null) {
				subCommentLabel.setText(MessageFormat.format(subCommentStringFormat, 0, 0, 0));
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
	public Result open() {
		if (worker != null) {
			if (worker.getStartTime() == null) {
				new Thread(worker).start();
			}
		}

		super.open();

		if (canceledByUser) {
			return Result.CANCELED;
		} else if (worker == null || worker.getError() == null) {
			return Result.OK;
		} else {
			return Result.ERROR;
		}
	}
}
