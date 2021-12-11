package de.soderer.utilities.swt;

import java.text.MessageFormat;
import java.util.Date;

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
import de.soderer.utilities.worker.WorkerParentSimple;
import de.soderer.utilities.worker.WorkerSimple;

public class ProgressDialog<T extends WorkerSimple<?>> extends ModalDialog<Result> implements WorkerParentSimple {
	protected String commentStringFormat;
	protected Label commentLabel;

	private ProgressBar progressBar;
	protected Button buttonClose;

	private Thread unlimitedRunner = null;

	protected T worker;

	protected boolean canceledByUser = false;

	public ProgressDialog(final Shell shell, final String title, final String commentStringFormat) {
		this(shell, title, commentStringFormat, (T) null);
	}

	public ProgressDialog(final Shell shell, final String title, final String commentStringFormat, final T worker) {
		super(shell, title);

		this.commentStringFormat = commentStringFormat;
		this.worker = worker;
		if (worker != null) {
			worker.setParent(this);
		}
	}

	@Override
	protected void createComponents(final Shell shell) throws Exception {
		shell.setLayout(new GridLayout(1, true));

		final Composite composite = new Composite(shell, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false));
		composite.setLayout(SwtUtilities.createNoMarginGridLayout(1, true));

		if (Utilities.isNotBlank(commentStringFormat)) {
			commentLabel = new Label(composite, SWT.NONE);
			commentLabel.setText(MessageFormat.format(commentStringFormat, 0, 0, 0));
			final GridData labelGridData = new GridData(SWT.FILL, SWT.UP, true, false);
			commentLabel.setLayoutData(labelGridData);
		}

		progressBar = new ProgressBar(composite, SWT.SMOOTH);
		progressBar.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false));
		progressBar.setMinimum(0);
		progressBar.setMaximum(100);
		progressBar.setSelection(0);

		createAdditionalComponents(composite);

		buttonClose = new Button(composite, SWT.PUSH);
		buttonClose.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false));
		buttonClose.setText(LangResources.get("cancel"));
		buttonClose.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				cancel();
			}
		});

		shell.pack();

		shell.setMinimumSize(400, shell.getSize().y);
		shell.setSize(400, shell.getSize().y);
	}

	protected void createAdditionalComponents(@SuppressWarnings("unused") final Composite composite) {
		// May be filled in sub classes
	}

	@Override
	public void showProgress(final Date start, final long itemsToDo, final long itemsDone) {
		if (!isDisposed()) {
			getParent().getDisplay().asyncExec(new Runnable() {
				@Override
				public void run() {
					if (!isDisposed()) {
						stopUnlimitedProgress();
						if (itemsToDo > 0) {
							if (progressBar != null) {
								progressBar.setSelection((int) (itemsDone * 100 / itemsToDo));
							}

							if (commentLabel != null) {
								commentLabel.setText(MessageFormat.format(commentStringFormat, itemsDone, itemsToDo, itemsDone * 100 / itemsToDo));
							}
						} else {
							if (progressBar != null) {
								progressBar.setSelection(0);
							}
						}
					}
				}
			});
		}
	}

	@Override
	public void showUnlimitedProgress() {
		if (!isDisposed()) {
			unlimitedRunner = new Thread(new Runnable() {
				@Override
				public void run() {
					while (!isDisposed()) {
						getParent().getDisplay().asyncExec(new Runnable() {
							@Override
							public void run() {
								if (!isDisposed()) {
									int nextValue;
									if (progressBar.getMaximum() == 100) {
										nextValue = progressBar.getSelection() + 10;
									} else {
										nextValue = progressBar.getSelection() - 10;
									}

									if (nextValue > 100) {
										nextValue = 100;
										progressBar.setMaximum(101);
									} else if (nextValue < 0) {
										nextValue = 0;
										progressBar.setMaximum(100);
									}

									progressBar.setSelection(nextValue);
								}
							}
						});

						try {
							Thread.sleep(100);
						} catch (@SuppressWarnings("unused") final InterruptedException e) {
							// do nothing
						} catch (@SuppressWarnings("unused") final ThreadDeath e) {
							break;
						}
					}
				}
			});
			unlimitedRunner.start();
		}
	}

	@SuppressWarnings("deprecation")
	private void stopUnlimitedProgress() {
		if (unlimitedRunner != null) {
			unlimitedRunner.stop();
			unlimitedRunner = null;
			progressBar.setMaximum(100);
		}
	}

	@Override
	public Result open() {
		if (worker != null) {
			new Thread(worker).start();
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

	@Override
	public void showDone(final Date start, final Date end, final long itemsDone) {
		if (!isDisposed()) {
			getParent().getDisplay().asyncExec(new Runnable() {
				@Override
				public void run() {
					if (!isDisposed()) {
						stopUnlimitedProgress();
						getParent().close();
						getParent().dispose();
					}
				}
			});
		}
	}

	@Override
	public void changeTitle(final String text) {
		// Do nothing
	}

	@Override
	public void cancel() {
		if (!canceledByUser && !isDisposed()) {
			getParent().getDisplay().asyncExec(new Runnable() {
				@Override
				public void run() {
					if (!canceledByUser && !isDisposed()) {
						canceledByUser = true;
						buttonClose.setEnabled(false);
						stopUnlimitedProgress();
						setReturnValue(Result.CANCELED);
						if (worker != null) {
							worker.cancel();
						}
					}
				}
			});
		}
	}

	public T getWorker() {
		return worker;
	}
}
