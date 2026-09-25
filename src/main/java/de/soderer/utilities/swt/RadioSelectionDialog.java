package de.soderer.utilities.swt;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import de.soderer.utilities.DefaultLangResources;

public class RadioSelectionDialog extends ModalDialog<Integer> {
	private final String text;
	private final List<String> items;
	private Group radioButtonGroup;
	private final List<Button> radioButtons = new ArrayList<>();
	private Button okButton;
	private final int preSelectedItemIndex;

	public RadioSelectionDialog(final Shell shell, final String title, final String text, final List<String> items) {
		this(shell, title, text, items, -1);
	}

	public RadioSelectionDialog(final Shell shell, final String title, final String text, final List<String> items, final int preSelectedItemIndex) {
		super(shell, SWT.RESIZE, title);

		this.text = text;
		this.items = items;
		this.preSelectedItemIndex = preSelectedItemIndex;
	}

	@Override
	protected void createComponents(final Shell shell) throws Exception {
		shell.setLayout(new GridLayout(1, true));

		final Composite composite = new Composite(shell, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, true));
		composite.setLayout(SwtUtilities.createNoMarginGridLayout(2, false));

		final Label label = new Label(composite, SWT.FILL);
		label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, true, 2, 2));
		label.setText(text);

		if (items != null && items.size() > 0) {
			radioButtonGroup = new Group(composite, SWT.NONE);
			radioButtonGroup.setLayout(new RowLayout(SWT.VERTICAL));
			radioButtonGroup.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, true, 2, 2));
			for (final String itemText : items) {
				final Button radio = new Button(radioButtonGroup, SWT.RADIO);
				radio.setText(itemText);
				radioButtons.add(radio);
			}
			if (preSelectedItemIndex >= 0) {
				radioButtons.get(preSelectedItemIndex).setSelection(true);
			} else {
				radioButtons.get(preSelectedItemIndex);
			}
		}

		okButton = new Button(composite, SWT.PUSH);
		okButton.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false));
		okButton.setText(DefaultLangResources.getI18NStringOrDefault("ok"));
		okButton.setEnabled(true);
		okButton.addSelectionListener(new OkButtonSelectionListener());

		final Button cancelButton = new Button(composite, SWT.PUSH);
		cancelButton.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false));
		cancelButton.setText(DefaultLangResources.getI18NStringOrDefault("cancel"));
		cancelButton.addSelectionListener(new CancelButtonSelectionListener());

		shell.setMinimumSize(250, 50);
		shell.pack();
	}

	private class OkButtonSelectionListener extends SelectionAdapter {
		@Override
		public void widgetSelected(final SelectionEvent event) {
			for (int i = 0; i < radioButtons.size(); i++) {
				if (radioButtons.get(i).getSelection()) {
					setReturnValue(i);
				}
			}
			getParent().close();
		}
	}

	private class CancelButtonSelectionListener extends SelectionAdapter {
		@Override
		public void widgetSelected(final SelectionEvent event) {
			getParent().close();
		}
	}
}
