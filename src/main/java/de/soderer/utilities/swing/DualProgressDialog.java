package de.soderer.utilities.swing;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.time.LocalDateTime;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

import de.soderer.utilities.worker.WorkerDual;
import de.soderer.utilities.worker.WorkerParentDual;

/**
 * Progress dialog for a {@link WorkerDual}, showing the overall progress and
 * the progress of the current item in a second progress bar.
 *
 * <p>
 * The item label shows the item name given by
 * {@link #receiveItemStartSignal(String, String)}, which may contain the
 * placeholders {@code {0}} (sub items done), {@code {1}} (sub items to do) and
 * {@code {2}} (percentage). All signals may be sent from any thread.
 * </p>
 *
 * @param <T> type of the worker
 */
public class DualProgressDialog<T extends WorkerDual<?>> extends ProgressDialog<T> implements WorkerParentDual {
	private static final long serialVersionUID = 1891862599169713021L;

	/** Label showing the current item, formatted from subCommentStringFormat */
	private JLabel subItemLabel;

	/** Item name of the current item, may contain the placeholders {0}, {1} and {2} */
	private String subCommentStringFormat;

	/** Progress bar of the current item */
	private JProgressBar subItemProgressBar;

	/**
	 * Creates a dual progress dialog without a worker. The dialog must then be
	 * closed by sending the done signal.
	 *
	 * @param parent owner, used as owner window if it is a {@link Window}, may be null
	 * @param title title of the dialog
	 * @param commentStringFormat format of the comment text, may be null for no
	 *            comment text
	 */
	public DualProgressDialog(final WorkerParentDual parent, final String title, final String commentStringFormat) {
		// A WorkerParentDual that is no Window must not cause a ClassCastException
		this(parent instanceof Window ? (Window) parent : null, title, commentStringFormat, null);
	}

	/**
	 * Creates a dual progress dialog for a worker. The dialog registers itself as
	 * the worker's parent.
	 *
	 * @param parent owner window, may be null
	 * @param title title of the dialog
	 * @param commentStringFormat format of the comment text, may be null for no
	 *            comment text
	 * @param worker worker to run and observe, may be null
	 */
	public DualProgressDialog(final Window parent, final String title, final String commentStringFormat, final T worker) {
		super(parent, title, commentStringFormat, worker);
	}

	@Override
	protected void createAdditionalComponents(final JPanel panel) {
		final JPanel subCommentLabelPanel = new JPanel();
		subCommentLabelPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
		subCommentStringFormat = "Item";
		subItemLabel = new JLabel("Item");
		subCommentLabelPanel.add(subItemLabel);
		panel.add(subCommentLabelPanel);

		final JPanel subItemPanel = new JPanel();
		subItemPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
		subItemProgressBar = new JProgressBar(0, 100);
		subItemProgressBar.setPreferredSize(new Dimension(380, 19));
		subItemPanel.add(subItemProgressBar);
		panel.add(subItemPanel);
	}

	@Override
	public void receiveUnlimitedSubProgressSignal() {
		if (SwingUtilities.isEventDispatchThread()) {
			subItemProgressBar.setIndeterminate(true);
			subItemProgressBar.setStringPainted(false);
		} else {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					receiveUnlimitedSubProgressSignal();
				}
			});
		}
	}

	@Override
	public void receiveItemStartSignal(final String itemName, final String description) {
		if (SwingUtilities.isEventDispatchThread()) {
			subCommentStringFormat = itemName;

			updateProgressBar(subItemProgressBar, LocalDateTime.now(), 100, 0);
			subItemLabel.setText(formatComment(subCommentStringFormat, "0", "100", "0"));
		} else {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					receiveItemStartSignal(itemName, description);
				}
			});
		}
	}

	@Override
	public void receiveItemProgressSignal(final LocalDateTime itemStart, final long subItemsToDo, final long subItemsDone, final String itemsUnitSign) {
		if (SwingUtilities.isEventDispatchThread()) {
			updateProgressBar(subItemProgressBar, itemStart, subItemsToDo, subItemsDone);
			// Percentage calculation must not divide by zero for an unknown number of sub items
			subItemLabel.setText(formatComment(subCommentStringFormat, Long.toString(subItemsDone), Long.toString(subItemsToDo), Long.toString(calculatePercentage(subItemsToDo, subItemsDone))));
		} else {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					receiveItemProgressSignal(itemStart, subItemsToDo, subItemsDone, itemsUnitSign);
				}
			});
		}
	}

	@Override
	public void receiveItemDoneSignal(final LocalDateTime itemStart, final LocalDateTime itemEnd, final long subItemsDone, final String itemsUnitSign, final String resultText) {
		if (SwingUtilities.isEventDispatchThread()) {
			updateProgressBar(subItemProgressBar, itemStart, subItemsDone, subItemsDone);
			subItemLabel.setText(formatComment(subCommentStringFormat, Long.toString(subItemsDone), Long.toString(subItemsDone), "100"));
		} else {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					receiveItemDoneSignal(itemStart, itemEnd, subItemsDone, itemsUnitSign, resultText);
				}
			});
		}
	}
}
