package de.soderer.utilities.swing;

import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Date;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import de.soderer.utilities.WorkerParentDual;

public class DualProgressDialog extends JDialog {
	private static final long serialVersionUID = 775327679829244076L;

	private JProgressBar progressBar;
	private JLabel label;
	private JProgressBar subItemProgressBar;
	private boolean canceledByUser = false;

	public DualProgressDialog(final Frame parent, String title) {
		super(parent, title, Dialog.ModalityType.DOCUMENT_MODAL);

		final DualProgressDialog dualProgressDialog = this;

		setResizable(false);

		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));

		JPanel itemPanel = new JPanel();
		itemPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
		progressBar = new JProgressBar(0, 100);
		progressBar.setPreferredSize(new Dimension(380, 19));
		itemPanel.add(progressBar);
		panel.add(itemPanel);

		JPanel labelPanel = new JPanel();
		labelPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
		label = new JLabel("Item");
		labelPanel.add(label);
		panel.add(labelPanel);

		JPanel subItemPanel = new JPanel();
		subItemPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
		subItemProgressBar = new JProgressBar(0, 100);
		subItemProgressBar.setPreferredSize(new Dimension(380, 19));
		subItemPanel.add(subItemProgressBar);
		panel.add(subItemPanel);

		// Cancel Button
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				canceledByUser = true;
				dualProgressDialog.dispose();
				if (parent instanceof WorkerParentDual) {
					((WorkerParentDual) parent).cancel();
				}
			}
		});
		buttonPanel.add(cancelButton);
		panel.add(buttonPanel);

		add(panel);

		pack();

		setLocationRelativeTo(null);
	}

	public void setIndeterminate() {
		progressBar.setIndeterminate(true);
	}

	public void setProgressPercentage(int value) {
		progressBar.setIndeterminate(false);
		progressBar.setValue(value);
		progressBar.setString(value + "%");
		progressBar.setStringPainted(true);
	}

	public void setProgress(long itemsToDo, long itemsDone) {
		if (itemsToDo > 0) {
			setProgressPercentage((int) (itemsDone * 100 / itemsToDo));
		} else {
			setProgressPercentage(0);
		}
	}

	public void setETA(Date estimatedEnd) {
		if (estimatedEnd != null) {
			progressBar.setToolTipText(estimatedEnd.toString());
		} else {
			progressBar.setToolTipText(null);
		}
	}

	public void setSubItemTitle(String subItemTitle) {
		label.setText(subItemTitle);
		setSubProgressPercentage(0);
	}

	public void setSubItemIndeterminate() {
		subItemProgressBar.setIndeterminate(true);
	}

	public void setSubProgressPercentage(int value) {
		subItemProgressBar.setIndeterminate(false);
		subItemProgressBar.setValue(value);
		subItemProgressBar.setString(value + "%");
		subItemProgressBar.setStringPainted(true);
	}

	public void setSubProgress(long itemsToDo, long itemsDone) {
		if (itemsToDo > 0) {
			setSubProgressPercentage((int) (itemsDone * 100 / itemsToDo));
		} else {
			setSubProgressPercentage(0);
		}
	}

	public void setSubETA(Date estimatedEnd) {
		if (estimatedEnd != null) {
			subItemProgressBar.setToolTipText(estimatedEnd.toString());
		} else {
			subItemProgressBar.setToolTipText(null);
		}
	}

	public boolean wasCanceledByUser() {
		return canceledByUser;
	}
}
