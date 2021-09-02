package de.soderer.utilities.swing;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
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
	private JProgressBar subItemProgressBar;

	public DualProgressDialog(final WorkerParentDual parent, final String title) {
		this((Window) parent, title, null);
	}

	public DualProgressDialog(final Window parent, final String title, final T worker) {
		super(parent, title, worker);
	}

	@Override
	protected void createAdditionalComponents(final JPanel panel) {
		final JPanel subCommentLabelPanel = new JPanel();
		subCommentLabelPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
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
			subItemLabel.setText(itemName);

			final int value = 0;
			subItemProgressBar.setIndeterminate(false);
			subItemProgressBar.setValue(value);
			subItemProgressBar.setString(value + "%");
			subItemProgressBar.setStringPainted(true);
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
	public void showItemProgress(final Date itemStart, final long subItemToDo, final long subItemDone) {
		if (SwingUtilities.isEventDispatchThread()) {
			updateProgressBar(subItemProgressBar, itemStart, subItemToDo, subItemDone);
		} else {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					showItemProgress(itemStart, subItemToDo, subItemDone);
				}
			});
		}
	}

	@Override
	public void showItemDone(final Date itemStart, final Date itemEnd, final long subItemsDone) {
		if (SwingUtilities.isEventDispatchThread()) {
			final int value = 100;
			subItemProgressBar.setIndeterminate(false);
			subItemProgressBar.setValue(value);
			subItemProgressBar.setString(value + "%");
			subItemProgressBar.setStringPainted(true);
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
