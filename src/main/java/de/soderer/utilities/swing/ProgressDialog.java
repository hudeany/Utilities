package de.soderer.utilities.swing;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDateTime;
import java.util.Locale;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import de.soderer.utilities.DateUtilities;
import de.soderer.utilities.LangResources;
import de.soderer.utilities.Result;
import de.soderer.utilities.Utilities;
import de.soderer.utilities.worker.WorkerParentSimple;
import de.soderer.utilities.worker.WorkerSimple;

/**
 * Modal dialog showing the progress of a {@link WorkerSimple}.
 *
 * <p>
 * The dialog registers itself as parent of the worker, starts the worker in a
 * new thread when it is opened and closes itself when the worker signals that
 * it is done. All progress signals may be sent from any thread, they are
 * forwarded to the Swing event dispatch thread. The user can cancel the worker
 * via the cancel button or the window decoration (after a confirmation).
 * </p>
 *
 * <p>
 * The optional comment text format may contain the placeholders {@code {0}}
 * (items done), {@code {1}} (items to do) and {@code {2}} (percentage).
 * </p>
 *
 * @param <T> type of the worker
 */
public class ProgressDialog<T extends WorkerSimple<?>> extends ModalDialog<Result> implements WorkerParentSimple {
	private static final long serialVersionUID = 4039220978715450890L;

	/**
	 * Label showing the comment text, null if no comment text format was given.
	 */
	protected JLabel commentLabel;

	/**
	 * Format of the comment text with the placeholders {@code {0}}, {@code {1}}
	 * and {@code {2}}, may be null.
	 */
	protected String commentStringFormat;

	/**
	 * Worker whose progress is shown, may be null.
	 */
	protected T worker;

	/** Progress bar of the overall progress */
	private final JProgressBar progressBar;

	/**
	 * Cancel button of the dialog.
	 */
	protected JButton buttonClose;

	/**
	 * True after the user canceled the worker.
	 */
	protected volatile boolean canceledByUser = false;

	/**
	 * Creates a progress dialog without a worker. The dialog must then be closed
	 * by calling {@link #receiveDoneSignal(LocalDateTime, LocalDateTime, long, String, String)}.
	 *
	 * @param parent owner window, may be null
	 * @param title title of the dialog
	 * @param commentStringFormat format of the comment text, may be null for no
	 *            comment text
	 */
	public ProgressDialog(final Window parent, final String title, final String commentStringFormat) {
		this(parent, title, commentStringFormat, (T) null);
	}

	/**
	 * Creates a progress dialog for a worker. The dialog registers itself as the
	 * worker's parent.
	 *
	 * @param parent owner window, may be null
	 * @param title title of the dialog
	 * @param commentStringFormat format of the comment text, may be null for no
	 *            comment text
	 * @param worker worker to run and observe, may be null
	 */
	public ProgressDialog(final Window parent, final String title, final String commentStringFormat, final T worker) {
		super(parent, title);

		setTitle(title);

		this.commentStringFormat = commentStringFormat;

		this.worker = worker;
		if (worker != null) {
			worker.setParent(this);
		}

		setResizable(false);

		final JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));

		if (Utilities.isNotBlank(commentStringFormat)) {
			final JPanel commentLabelPanel = new JPanel();
			commentLabelPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
			commentLabel = new JLabel(formatComment(commentStringFormat, "0", "0", "0"));
			commentLabelPanel.add(commentLabel);
			panel.add(commentLabelPanel);
		}

		final JPanel itemPanel = new JPanel();
		itemPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
		progressBar = new JProgressBar(0, 100);
		progressBar.setPreferredSize(new Dimension(380, 19));
		itemPanel.add(progressBar);
		panel.add(itemPanel);

		createAdditionalComponents(panel);

		// Cancel Button
		final JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
		buttonClose = new JButton(LangResources.get("cancel"));
		buttonClose.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent event) {
				askForCancel();
			}
		});
		buttonPanel.add(buttonClose);
		panel.add(buttonPanel);

		add(panel);

		// Closing via the window decoration must not just hide the dialog while the worker keeps running
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowOpened(final WindowEvent event) {
				// Started only now, so a fast worker cannot close the dialog before it is shown
				if (ProgressDialog.this.worker != null) {
					if (ProgressDialog.this.worker.getStartTime() == null) {
						new Thread(ProgressDialog.this.worker).start();
					} else if (ProgressDialog.this.worker.isDone()) {
						// Worker was started externally and already finished before the dialog was shown
						dispose();
					}
				}
			}

			@Override
			public void windowClosing(final WindowEvent event) {
				if (!canceledByUser) {
					askForCancel();
				}
			}
		});

		pack();

		setLocationRelativeTo(parent);

		getRootPane().setDefaultButton(buttonClose);
	}

	private void askForCancel() {
		final Integer answer = new QuestionDialog(this, LangResources.get("cancel"), LangResources.get("sureQuestion"), LangResources.get("no"), LangResources.get("yes")).open();
		if (answer != null && answer == 1) {
			cancel();
		}
	}

	/**
	 * Hook for subclasses to add further components below the progress bar.
	 * Called once from the constructor, so subclass fields are not yet initialized
	 * at that time. Does nothing by default.
	 *
	 * @param panel panel to add the components to
	 */
	protected void createAdditionalComponents(final JPanel panel) {
		// May be filled in sub classes
	}

	/**
	 * Returns the worker whose progress is shown.
	 *
	 * @return worker, may be null
	 */
	public T getWorker() {
		return worker;
	}

	/**
	 * Shows the dialog, starts the worker (if it was not started yet) and blocks
	 * until the worker is done or the user canceled it.
	 *
	 * @return {@link Result#CANCELED} if the user canceled, {@link Result#ERROR}
	 *         if the worker failed, otherwise {@link Result#OK}
	 */
	@Override
	public Result open() {
		setVisible(true);

		if (canceledByUser) {
			return Result.CANCELED;
		} else if (worker == null || worker.getError() == null) {
			return Result.OK;
		} else {
			return Result.ERROR;
		}
	}

	@Override
	public void receiveUnlimitedProgressSignal() {
		if (SwingUtilities.isEventDispatchThread()) {
			progressBar.setIndeterminate(true);
			progressBar.setStringPainted(false);
		} else {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					receiveUnlimitedProgressSignal();
				}
			});
		}
	}

	@Override
	public void receiveProgressSignal(final LocalDateTime start, final long itemsToDo, final long itemsDone, final String itemsUnitSign) {
		if (SwingUtilities.isEventDispatchThread()) {
			updateProgressBar(progressBar, start, itemsToDo, itemsDone);
			if (commentLabel != null) {
				commentLabel.setText(formatComment(commentStringFormat, Long.toString(itemsDone), Long.toString(itemsToDo), Long.toString(calculatePercentage(itemsToDo, itemsDone))));
			}
		} else {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					receiveProgressSignal(start, itemsToDo, itemsDone, itemsUnitSign);
				}
			});
		}
	}

	@Override
	public void receiveDoneSignal(final LocalDateTime start, final LocalDateTime end, final long itemsDone, final String itemsUnitSign, final String resultText) {
		if (SwingUtilities.isEventDispatchThread()) {
			dispose();
		} else {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					receiveDoneSignal(start, end, itemsDone, itemsUnitSign, resultText);
				}
			});
		}
	}

	/**
	 * Cancels the worker (once). May be called from any thread. Also cancels the
	 * owner window, if it is a {@link WorkerParentSimple} itself.
	 *
	 * @return always true
	 */
	@Override
	public boolean cancel() {
		if (SwingUtilities.isEventDispatchThread()) {
			if (!canceledByUser) {
				canceledByUser = true;
				buttonClose.setEnabled(false);
				if (worker != null) {
					worker.cancel();
				}
				if (progressBar.isIndeterminate()) {
					progressBar.setIndeterminate(false);
				}
				if (getParent() != null && getParent() instanceof WorkerParentSimple) {
					((WorkerParentSimple) getParent()).cancel();
				}
			}
		} else {
			if (!canceledByUser) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						cancel();
					}
				});
			}
		}
		return true;
	}

	/**
	 * Sets value, percentage text and ETA tooltip of a progress bar. Must be called
	 * on the event dispatch thread.
	 *
	 * @param progressBar progress bar to update
	 * @param start start time of the work, used for the ETA calculation
	 * @param itemsToDo total number of items, 0 or below if unknown (shows 0%)
	 * @param itemsDone number of items done
	 */
	protected static void updateProgressBar(final JProgressBar progressBar, final LocalDateTime start, final long itemsToDo, final long itemsDone) {
		final int value = (int) Math.max(0, Math.min(100, calculatePercentage(itemsToDo, itemsDone)));

		if (progressBar.isIndeterminate()) {
			progressBar.setIndeterminate(false);
		}
		progressBar.setValue(value);
		progressBar.setString(value + "%");
		progressBar.setStringPainted(true);

		final LocalDateTime estimatedEnd = DateUtilities.calculateETA(start, itemsToDo, itemsDone);
		if (estimatedEnd != null) {
			progressBar.setToolTipText("ETA: " + DateUtilities.formatDate(DateUtilities.getDateTimeFormatWithSecondsPattern(Locale.getDefault()), estimatedEnd));
		} else {
			progressBar.setToolTipText(null);
		}
	}

	/**
	 * Calculates the percentage of done items.
	 *
	 * @param itemsToDo total number of items, 0 or below if unknown
	 * @param itemsDone number of items done
	 * @return percentage, 0 if the total number of items is unknown
	 */
	protected static long calculatePercentage(final long itemsToDo, final long itemsDone) {
		if (itemsToDo > 0) {
			return itemsDone * 100 / itemsToDo;
		} else {
			return 0;
		}
	}

	/**
	 * Fills the placeholders of a comment text format.
	 *
	 * @param format comment text format, may be null
	 * @param itemsDone text for {@code {0}}
	 * @param itemsToDo text for {@code {1}}
	 * @param percentage text for {@code {2}}
	 * @return comment text, empty for a null format
	 */
	protected static String formatComment(final String format, final String itemsDone, final String itemsToDo, final String percentage) {
		if (format == null) {
			return "";
		} else {
			return format.replace("{0}", itemsDone).replace("{1}", itemsToDo).replace("{2}", percentage);
		}
	}

	/**
	 * Changes the comment text format. The counters are shown as "n/a" until the
	 * next progress signal. May be called from any thread. Has no effect if the
	 * dialog was created without a comment text format.
	 *
	 * @param newCommentStringFormat new comment text format
	 */
	@Override
	public void changeTitle(final String newCommentStringFormat) {
		if (SwingUtilities.isEventDispatchThread()) {
			if (commentLabel != null) {
				commentStringFormat = newCommentStringFormat;
				commentLabel.setText(formatComment(commentStringFormat, "n/a", "n/a", "n/a"));
			}
		} else {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					changeTitle(newCommentStringFormat);
				}
			});
		}
	}
}
