package de.soderer.utilities.swt;

import java.util.Date;
import java.util.Locale;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;

import de.soderer.utilities.DateUtilities;
import de.soderer.utilities.worker.WorkerDual;
import de.soderer.utilities.worker.WorkerParentDual;

public class DualProgressDialog<T extends WorkerDual<?>> extends ProgressDialog<T> implements WorkerParentDual {
	private Label subItemLabel;
	private String subCommentStringFormat;

	private Thread unlimitedSubRunner = null;

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

	protected void setSubProgress(final Date start, final long todo, final long done) {
		if (!isDisposed()) {
			stopUnlimitedProgress();
			subItemProgressBar.setSelection((int) (done * 100 / todo));

			final Date estimatedEnd = DateUtilities.calculateETA(start, todo, done);
			if (estimatedEnd != null) {
				subItemProgressBar.setToolTipText("ETA: " + DateUtilities.getDateTimeFormatWithSeconds(Locale.getDefault()).format(estimatedEnd));
			} else {
				subItemProgressBar.setToolTipText(null);
			}

			try {
				final String labelText = subCommentStringFormat.replace("{0}", Long.toString(done)).replace("{1}", Long.toString(todo)).replace("{2}", Long.toString(done * 100 / todo));
				subItemLabel.setText(labelText);
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void showUnlimitedSubProgress() {
		if (!isDisposed()) {
			getParent().getDisplay().asyncExec(new Runnable() {
				@Override
				public void run() {
					if (!isDisposed()) {
						unlimitedSubRunner = new Thread(new Runnable() {
							@Override
							public void run() {
								while (!isDisposed()) {
									getParent().getDisplay().asyncExec(new Runnable() {
										@Override
										public void run() {
											if (!isDisposed()) {
												int nextValue;
												if (subItemProgressBar.getMaximum() == 100) {
													nextValue = subItemProgressBar.getSelection() + 10;
												} else {
													nextValue = subItemProgressBar.getSelection() - 10;
												}

												if (nextValue > 100) {
													nextValue = 100;
													subItemProgressBar.setMaximum(101);
												} else if (nextValue < 0) {
													nextValue = 0;
													subItemProgressBar.setMaximum(100);
												}

												subItemProgressBar.setSelection(nextValue);
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
						unlimitedSubRunner.start();
					}
				}
			});
		}
	}

	@SuppressWarnings("deprecation")
	private void stopUnlimitedProgress() {
		if (unlimitedSubRunner != null) {
			unlimitedSubRunner.stop();
			unlimitedSubRunner = null;
			subItemProgressBar.setMaximum(100);
		}
	}

	@Override
	public void showItemStart(final String itemName) {
		if (!isDisposed()) {
			getParent().getDisplay().asyncExec(new Runnable() {
				@Override
				public void run() {
					if (!isDisposed()) {
						subCommentStringFormat = itemName;
						setSubProgress(new Date(), 100, 0);
					}
				}
			});
		}
	}

	@Override
	public void showItemProgress(final Date itemStart, final long subItemsToDo, final long subItemsDone) {
		if (!isDisposed()) {
			getParent().getDisplay().asyncExec(new Runnable() {
				@Override
				public void run() {
					if (!isDisposed()) {
						setSubProgress(itemStart, subItemsToDo, subItemsDone);
					}
				}
			});
		}
	}

	@Override
	public void showItemDone(final Date itemStart, final Date itemEnd, final long subItemsDone) {
		if (!isDisposed()) {
			getParent().getDisplay().asyncExec(new Runnable() {
				@Override
				public void run() {
					if (!isDisposed()) {
						setSubProgress(itemStart, subItemsDone, subItemsDone);
					}
				}
			});
		}
	}
}
