package de.soderer.utilities.swing;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.text.MessageFormat;
import java.util.Date;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

import de.soderer.utilities.worker.WorkerDual;
import de.soderer.utilities.worker.WorkerParentDual;

public class DualProgressDialog<T extends WorkerDual<?>> extends ProgressDialog<T> implements WorkerParentDual {
	private static final long serialVersionUID = 1891862599169713021L;

	private JLabel subItemLabel;
	private String subCommentStringFormat;
	private JProgressBar subItemProgressBar;

	public DualProgressDialog(final WorkerParentDual parent, final String title, final String commentStringFormat) {
		this((Window) parent, title, commentStringFormat, null);
	}

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
	public void showUnlimitedSubProgress() {
		if (SwingUtilities.isEventDispatchThread()) {
			subItemProgressBar.setIndeterminate(true);
			subItemProgressBar.setStringPainted(false);
		} else {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					showUnlimitedSubProgress();
				}
			});
		}
	}

	@Override
	public void showItemStart(final String itemName) {
		if (SwingUtilities.isEventDispatchThread()) {
			subCommentStringFormat = itemName;
			subItemLabel.setText(itemName);

			updateProgressBar(subItemProgressBar, new Date(), 100, 0);
			subItemLabel.setText(MessageFormat.format(subCommentStringFormat, 0, 100, 0 * 100 / 100));
		} else {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					showItemStart(itemName);
				}
			});
		}
	}

	@Override
	public void showItemProgress(final Date itemStart, final long subItemsToDo, final long subItemsDone) {
		if (SwingUtilities.isEventDispatchThread()) {
			updateProgressBar(subItemProgressBar, itemStart, subItemsToDo, subItemsDone);
			subItemLabel.setText(MessageFormat.format(subCommentStringFormat, subItemsDone, subItemsToDo, subItemsDone * 100 / subItemsToDo));
		} else {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					showItemProgress(itemStart, subItemsToDo, subItemsDone);
				}
			});
		}
	}

	@Override
	public void showItemDone(final Date itemStart, final Date itemEnd, final long subItemsDone) {
		if (SwingUtilities.isEventDispatchThread()) {
			updateProgressBar(subItemProgressBar, itemStart, subItemsDone, subItemsDone);
			subItemLabel.setText(MessageFormat.format(subCommentStringFormat, subItemsDone, subItemsDone, subItemsDone * 100 / subItemsDone));
		} else {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					showItemDone(itemStart, itemEnd, subItemsDone);
				}
			});
		}
	}
}
