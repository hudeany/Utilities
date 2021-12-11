package de.soderer.utilities.swt;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import de.soderer.utilities.LangResources;
import de.soderer.utilities.Utilities;

public class MultiInputDialog extends ModalDialog<List<String>> {
	private final String prologText;
	private final String[] labelTexts;
	private String[] defaultTexts = null;
	private final List<Text> textFields = new ArrayList<>();

	public MultiInputDialog(final Shell shell, final String title, final String prologText, final String... labelTexts) {
		super(shell, title);

		this.prologText = prologText;
		this.labelTexts = labelTexts;
	}

	public MultiInputDialog setDefaultTexts(final String[] defaultTexts) {
		this.defaultTexts = defaultTexts;
		return this;
	}

	@Override
	protected void createComponents(final Shell shell) throws Exception {
		shell.setLayout(new GridLayout(2, false));

		if (Utilities.isNotBlank(prologText)) {
			final Label label = new Label(shell, SWT.NONE);
			label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, true, 2, 1));
			label.setText(prologText);
		}

		int index = 0;
		for (final String text : labelTexts) {
			final Label label = new Label(shell, SWT.NONE);
			label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, true));
			label.setText(text);

			final Text textField = new Text(shell, SWT.BORDER);
			textField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
			if (defaultTexts != null && defaultTexts.length > index && defaultTexts[index] != null) {
				textField.setText(defaultTexts[index]);
			}
			textField.addKeyListener(new KeyAdapter() {
				@Override
				public void keyReleased(final KeyEvent event) {
					// on Enter-key
					if (event.keyCode == SWT.CR || event.keyCode == SwtUtilities.BOTTOM_RIGHT_ENTER_KEY) {
						final List<String> retunValueList = new ArrayList<>();
						for (final Text textFieldForReturnValue: textFields) {
							retunValueList.add(textFieldForReturnValue.getText());
						}
						setReturnValue(retunValueList);
						getParent().close();
					}
				}
			});

			final GridData gridData = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
			gridData.widthHint = 200;
			textField.setLayoutData(gridData);

			textFields.add(textField);

			index++;
		}

		final Composite mainButtonComposite = new Composite(shell, SWT.NONE);
		mainButtonComposite.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false, 2, 1));
		mainButtonComposite.setLayout(SwtUtilities.createNoMarginGridLayout(2, true));

		final Button okButton = new Button(mainButtonComposite, SWT.PUSH);
		okButton.setLayoutData(new GridData(SWT.CENTER, SWT.UP, true, false));
		okButton.setText(LangResources.get("ok"));
		okButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				final List<String> retunValueList = new ArrayList<>();
				for (final Text textFieldForReturnValue: textFields) {
					retunValueList.add(textFieldForReturnValue.getText());
				}
				setReturnValue(retunValueList);
				getParent().close();
			}
		});

		final GridData gridDataOk = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gridDataOk.widthHint = 100;
		okButton.setLayoutData(gridDataOk);

		final Button cancelButton = new Button(mainButtonComposite, SWT.PUSH);
		cancelButton.setLayoutData(new GridData(SWT.CENTER, SWT.UP, true, false));
		cancelButton.setText(LangResources.get("cancel"));
		cancelButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				setReturnValue(null);
				getParent().close();
			}
		});

		final GridData gridDataCancel = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gridDataCancel.widthHint = 100;
		cancelButton.setLayoutData(gridDataCancel);

		shell.pack();
	}
}
