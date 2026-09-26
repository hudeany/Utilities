package de.soderer.utilities.swt;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;

import de.soderer.utilities.DefaultLangResources;

public class MultipleWorkerConfigurationDialog extends ModalDialog<Boolean> {
	private int workerCount;
	private String repetitions;
	private int pauseSeconds;

	public MultipleWorkerConfigurationDialog(final Shell applicationDialog, final String title) {
		super(applicationDialog, title);
	}

	@Override
	protected void createComponents(final Shell parentShell) throws Exception {
		parentShell.setLayout(new GridLayout(2, false));

		final Label lblWorkers = new Label(parentShell, SWT.NONE);
		lblWorkers.setText(DefaultLangResources.getI18NStringOrDefault("numberOfParallelWorkers") + " (1 ≤ x ≤ 100):");
		final Spinner spnWorkers = new Spinner(parentShell, SWT.BORDER);
		spnWorkers.setMinimum(1);
		spnWorkers.setMaximum(100);
		spnWorkers.setSelection(1);
		spnWorkers.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		final Label lblReps = new Label(parentShell, SWT.NONE);
		lblReps.setText(DefaultLangResources.getI18NStringOrDefault("numberOfRepetitionsPerWorker") + ":");
		// Editable like the former SWT.DROP_DOWN Combo: presets 1..50, but any other value may be typed in
		final List<String> repetitionItems = new ArrayList<>();
		repetitionItems.add("∞");
		for (int i = 1; i <= 50; i++) {
			repetitionItems.add(String.valueOf(i));
		}
		final DropDown cmbReps = new DropDown(parentShell, SWT.NONE);
		cmbReps.setMatchMode(DropDown.MatchMode.STARTS_WITH);
		cmbReps.setAllowCustomValues(true);
		cmbReps.setItems(repetitionItems);
		cmbReps.setText("∞");
		cmbReps.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		final Label lblPause = new Label(parentShell, SWT.NONE);
		lblPause.setText(DefaultLangResources.getI18NStringOrDefault("workerSleepTime") + " (" + DefaultLangResources.getI18NStringOrDefault("seconds") + ", 0 ≤ x):");
		final Spinner spnPause = new Spinner(parentShell, SWT.BORDER);
		spnPause.setMinimum(0);
		spnPause.setMaximum(Integer.MAX_VALUE);
		spnPause.setSelection(0);
		spnPause.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		final Composite buttonBar = new Composite(parentShell, SWT.NONE);
		buttonBar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		buttonBar.setLayout(SwtUtilities.createSmallMarginGridLayout(2, true));

		final Button btnStart = new Button(buttonBar, SWT.PUSH);
		btnStart.setText(DefaultLangResources.getI18NStringOrDefault("start"));
		btnStart.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		btnStart.addListener(SWT.Selection, e -> {
			workerCount = spnWorkers.getSelection();
			final String repetitionsText = cmbReps.getText();
			repetitions = repetitionsText == null ? "" : repetitionsText.trim();
			pauseSeconds = spnPause.getSelection();

			setReturnValue(true);

			parentShell.dispose();
		});

		final Button btnCancel = new Button(buttonBar, SWT.PUSH);
		btnCancel.setText(DefaultLangResources.getI18NStringOrDefault("cancel"));
		btnCancel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		btnCancel.addListener(SWT.Selection, e -> parentShell.dispose());

		parentShell.pack();
	}

	public int getWorkerCount() {
		return workerCount;
	}

	public String getRepetitions() {
		return repetitions;
	}

	public int getPauseSeconds() {
		return pauseSeconds;
	}
}
