package de.soderer.utilities.swt;

import java.io.PrintWriter;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import de.soderer.restclient.worker.WorkerStats;
import de.soderer.utilities.DateUtilities;
import de.soderer.utilities.LangResources;
import de.soderer.utilities.swt.ModalDialog;
import de.soderer.utilities.worker.WorkerSimple;

public abstract class WorkerPoolDialog extends ModalDialog<Boolean> {
	private final String text;

	private Table table;
	private ProgressBar progressBar;
	private Button actionButton;
	private Button downloadButton;

	private final List<WorkerStats> workerStatsList = new ArrayList<>();
	private final AtomicInteger progress = new AtomicInteger(0);
	private volatile boolean cancelled = false;
	private volatile boolean finished = false;

	private ExecutorService executor;

	private int workerCount = 1;
	private int tasksPerWorker = 1;
	private Duration sleepTime = null;

	private int sortColumn = 0;
	private boolean ascending = true;

	public WorkerPoolDialog(final Shell applicationDialog, final String title, final String text) {
		super(applicationDialog, title);

		this.text = text;
	}

	public void setParallelWorkerAmount(final int workerCount) {
		this.workerCount = workerCount;
	}

	public void setRepetitionsPerWorker(final int tasksPerWorker) {
		this.tasksPerWorker = tasksPerWorker;
	}

	public void setSleepTime(final Duration sleepTime) {
		this.sleepTime = sleepTime;
	}

	@Override
	protected void createComponents(final Shell parentShell) throws Exception {
		parentShell.setLayout(new GridLayout(1, false));

		final Label description = new Label(parentShell, SWT.WRAP);
		description.setText(text);
		description.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

		progressBar = new ProgressBar(parentShell, SWT.NONE);
		progressBar.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		progressBar.setMinimum(0);
		if (tasksPerWorker >= 0) {
			progressBar.setMaximum(workerCount * tasksPerWorker);
		} else {
			progressBar.setMaximum(Integer.MAX_VALUE);
		}

		table = new Table(parentShell, SWT.BORDER | SWT.FULL_SELECTION | SWT.VIRTUAL | SWT.V_SCROLL);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		final GridData gdTable = new GridData(SWT.LEFT, SWT.TOP, false, false);
		final int rowHeight = table.getItemHeight();
		final int visibleRows = Math.min(workerCount, 15);
		gdTable.heightHint = visibleRows * rowHeight + table.getHeaderHeight();
		table.setLayoutData(gdTable);

		final String[] columnTitles = { "WorkerID",
				LangResources.get("successCount"),
				LangResources.get("errorCount"),
				LangResources.get("latestDuration"),
				LangResources.get("latestStatus"),
				LangResources.get("minDuration"),
				"Ø " + LangResources.get("duration"),
				LangResources.get("maxDuration") };
		for (int i = 0; i < columnTitles.length; i++) {
			final int colIndex = i;
			final TableColumn col = new TableColumn(table, SWT.NONE);
			col.setText(columnTitles[i]);
			col.pack();
			col.addListener(SWT.Selection, e -> {
				if (sortColumn == colIndex)
					ascending = !ascending;
				else {
					sortColumn = colIndex;
					ascending = true;
				}
				refreshTable();
			});
		}

		table.addListener(SWT.SetData, e -> {
			final TableItem item = (TableItem) e.item;
			final int index = table.indexOf(item);
			if (index >= workerStatsList.size())
				return;
			fillItem(item, workerStatsList.get(index));
		});

		final Composite buttonBar = new Composite(parentShell, SWT.NONE);
		buttonBar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		final GridLayout gl = new GridLayout(3, false);
		gl.marginWidth = 0;
		gl.marginHeight = 0;
		gl.horizontalSpacing = 10;
		buttonBar.setLayout(gl);

		final Label spacer = new Label(buttonBar, SWT.NONE);
		spacer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		downloadButton = new Button(buttonBar, SWT.PUSH);
		downloadButton.setText(LangResources.get("saveResults"));
		downloadButton.setEnabled(false);
		final GridData gdDownload = new GridData(SWT.RIGHT, SWT.CENTER, false, false);
		gdDownload.widthHint = 160;
		downloadButton.setLayoutData(gdDownload);
		downloadButton.addListener(SWT.Selection, e -> exportResults());

		actionButton = new Button(buttonBar, SWT.PUSH);
		actionButton.setText(LangResources.get("cancel"));
		final GridData gdAction = new GridData(SWT.RIGHT, SWT.CENTER, false, false);
		gdAction.widthHint = 120;
		actionButton.setLayoutData(gdAction);
		actionButton.addListener(SWT.Selection, e -> {
			if (!finished) {
				cancelExecution();
			} else {
				parentShell.dispose();
			}
		});

		parentShell.pack();

		initWorkers();
	}

	private void fillItem(final TableItem item, final WorkerStats ws) {
		item.setText(new String[] {
				String.valueOf(ws.getWorkerId()),
				String.valueOf(ws.getSuccessCount()),
				String.valueOf(ws.getErrorCount()),
				(ws.getLatestDuration() == null ? "" : DateUtilities.getShortHumanReadableTimespan(ws.getLatestDuration(), true, false)),
				(ws.getLatestStatusWasSuccess() == null ? "" : (ws.getLatestStatusWasSuccess() ? LangResources.get("success") : LangResources.get("error"))),
				(ws.getMinimumDuration() == null ? "" : DateUtilities.getShortHumanReadableTimespan(ws.getMinimumDuration(), true, false)),
				(ws.getAverageDuration() == null ? "" : DateUtilities.getShortHumanReadableTimespan(ws.getAverageDuration(), true, false)),
				(ws.getMaximumDuration() == null ? "" : DateUtilities.getShortHumanReadableTimespan(ws.getMaximumDuration(), true, false))
		});

		if (ws.getLatestStatusWasSuccess() == null) {
			item.setForeground(4, null);
		} else if (ws.getLatestStatusWasSuccess()) {
			item.setForeground(4, getParent().getDisplay().getSystemColor(SWT.COLOR_DARK_GREEN));
		} else {
			item.setForeground(4, getParent().getDisplay().getSystemColor(SWT.COLOR_RED));
		}
	}

	private void initWorkers() {
		executor = Executors.newFixedThreadPool(workerCount);
		final Display display = getParent().getDisplay();

		for (int i = 0; i < workerCount; i++) {
			workerStatsList.add(new WorkerStats(i + 1));
		}

		table.setItemCount(workerStatsList.size());

		for (int i = 0; i < workerStatsList.size(); i++) {
			final int idx = i;
			final WorkerStats ws = workerStatsList.get(i);

			executor.submit(() -> {
				for (int j = 0; (tasksPerWorker == -1  || j < tasksPerWorker) && !cancelled; j++) {
					final WorkerSimple<?> worker = createWorker();

					final LocalDateTime start = LocalDateTime.now();
					try {
						final Object workerResult = worker.work();
						if (checkForSuccess(workerResult)) {
							ws.addSuccess(Duration.between(start, LocalDateTime.now()));
						} else {
							ws.addError(Duration.between(start, LocalDateTime.now()));
						}
					} catch (@SuppressWarnings("unused") final Exception e) {
						ws.addError(Duration.between(start, LocalDateTime.now()));
					}

					final int current = progress.incrementAndGet();
					display.asyncExec(() -> {
						if (!table.isDisposed()) {
							refreshTableItem(idx);
							progressBar.setSelection(current);
							checkFinished();
						}
					});

					if ((tasksPerWorker == -1  || j < tasksPerWorker - 1) && !cancelled && sleepTime != null) {
						try {
							Thread.sleep(sleepTime.toMillis());
						} catch (@SuppressWarnings("unused") final InterruptedException ex) {
							return;
						}
					}
				}
				display.asyncExec(this::checkFinished);
			});
		}
	}

	protected abstract boolean checkForSuccess(Object workerResult);

	private void refreshTableItem(final int idx) {
		if (idx < table.getItemCount()) {
			final TableItem item = table.getItem(idx);
			fillItem(item, workerStatsList.get(idx));
		}
	}

	private void refreshTable() {
		workerStatsList.sort((w1, w2) -> {
			int result = 0;
			switch (sortColumn) {
				case 0:
					result = Integer.compare(w1.getWorkerId(), w2.getWorkerId());
					break;
				case 1:
					result = Integer.compare(w1.getSuccessCount(), w2.getSuccessCount());
					break;
				case 2:
					result = Integer.compare(w1.getErrorCount(), w2.getErrorCount());
					break;
				case 3:
					result = w1.getLatestDuration().compareTo(w2.getLatestDuration());
					break;
				case 4:
					result = Boolean.compare(w1.getLatestStatusWasSuccess(), w2.getLatestStatusWasSuccess());
					break;
				case 5:
					result = w1.getMinimumDuration().compareTo(w2.getMinimumDuration());
					break;
				case 6:
					result = w1.getAverageDuration().compareTo(w2.getAverageDuration());
					break;
				case 7:
					result = w1.getMaximumDuration().compareTo(w2.getMaximumDuration());
					break;
				default:
					break;
			}
			return ascending ? result : -result;
		});
		table.clearAll();
	}

	private void cancelExecution() {
		cancelled = true;
		executor.shutdownNow();
		checkFinished();
	}

	private void checkFinished() {
		if (!finished) {
			final boolean allDone = cancelled || (tasksPerWorker >= 0 && progress.get() >= progressBar.getMaximum());
			if (allDone) {
				finished = true;
				actionButton.setText(LangResources.get("close"));
				downloadButton.setEnabled(true);
				getParent().layout();
			}
		}
	}

	private void exportResults() {
		final FileDialog dialog = new FileDialog(getParent(), SWT.SAVE);
		dialog.setFilterExtensions(new String[] { "*.csv" });
		dialog.setFileName("worker_ergebnisse.csv");
		final String path = dialog.open();
		if (path == null)
			return;

		try (PrintWriter writer = new PrintWriter(path, "UTF-8")) {
			writer.println("WorkerID;Success count;Error count;Latest duration;Latest status;Minimum duration;Average duration;Maximum duration");
			for (final WorkerStats ws : workerStatsList) {
				writer.printf("%d;%d;%d;%s;%s;%s;%s;%s%n",
						ws.getWorkerId(),
						ws.getSuccessCount(),
						ws.getErrorCount(),
						(ws.getLatestDuration() == null ? "" : DateUtilities.getShortHumanReadableTimespan(ws.getLatestDuration(), true, true)),
						(ws.getLatestStatusWasSuccess() == null ? "" : (ws.getLatestStatusWasSuccess() ? "success" : "error")),
						(ws.getMinimumDuration() == null ? "" : DateUtilities.getShortHumanReadableTimespan(ws.getMinimumDuration(), true, true)),
						(ws.getAverageDuration() == null ? "" : DateUtilities.getShortHumanReadableTimespan(ws.getAverageDuration(), true, true)),
						(ws.getMaximumDuration() == null ? "" : DateUtilities.getShortHumanReadableTimespan(ws.getMaximumDuration(), true, true)));
			}
		} catch (final Exception ex) {
			final MessageBox box = new MessageBox(getParent(), SWT.ICON_ERROR);
			box.setMessage("Fehler beim Export: " + ex.getMessage());
			box.open();
		}
	}

	public String getResultsCSV() {
		final StringBuilder result = new StringBuilder();
		result.append("WorkerID;Success count;Error count;Latest duration;Latest status;Minimum duration;Average duration;Maximum duration\n");
		for (final WorkerStats ws : workerStatsList) {
			result.append(String.format("%d;%d;%d;%s;%s;%s;%s;%s%n",
					ws.getWorkerId(),
					ws.getSuccessCount(),
					ws.getErrorCount(),
					(ws.getLatestDuration() == null ? "" : DateUtilities.getShortHumanReadableTimespan(ws.getLatestDuration(), true, true)),
					(ws.getLatestStatusWasSuccess() == null ? "" : (ws.getLatestStatusWasSuccess() ? "success" : "error")),
					(ws.getMinimumDuration() == null ? "" : DateUtilities.getShortHumanReadableTimespan(ws.getMinimumDuration(), true, true)),
					(ws.getAverageDuration() == null ? "" : DateUtilities.getShortHumanReadableTimespan(ws.getAverageDuration(), true, true)),
					(ws.getMaximumDuration() == null ? "" : DateUtilities.getShortHumanReadableTimespan(ws.getMaximumDuration(), true, true))));
		}
		return result.toString();
	}

	protected abstract WorkerSimple<?> createWorker();
}
