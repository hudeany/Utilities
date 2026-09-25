package de.soderer.utilities.swt;

import java.time.LocalDate;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Shell;

import de.soderer.utilities.DefaultLangResources;

public class DateInputDialog extends ModalDialog<LocalDate> {
	private final String text;

	public DateInputDialog(final Shell shell, final String title, final String text) {
		super(SwtUtilities.createCenteredShellIfNeeded(shell), title);
		this.text = text;
	}

	@Override
	protected void createComponents(final Shell shell) throws Exception {
		final GridLayout layout = new GridLayout(1, true);
		layout.verticalSpacing = 0;
		shell.setLayout(layout);

		final StyledText styledText = new StyledText(shell, SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI);
		final GridData textData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		textData.heightHint = 10;
		styledText.setLayoutData(textData);
		styledText.setText(text);
		styledText.setBackground(shell.getBackground());
		styledText.setAlwaysShowScrollBars(false);
		styledText.setEditable(false);

		final DateTime datePicker = new DateTime(shell, SWT.DATE | SWT.DROP_DOWN);
		datePicker.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		final Composite mainButtonComposite = new Composite(shell, SWT.NONE);
		mainButtonComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		mainButtonComposite.setLayout(SwtUtilities.createNoMarginGridLayout(2, true));

		final Button okButton = new Button(mainButtonComposite, SWT.PUSH);
		okButton.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false));
		okButton.setText(DefaultLangResources.getI18NStringOrDefault("ok"));
		okButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				final LocalDate date = LocalDate.of(datePicker.getYear(),  datePicker.getMonth() + 1, datePicker.getDay());
				setReturnValue(date);
				getParent().close();
			}
		});

		final Button cancelButton = new Button(mainButtonComposite, SWT.PUSH);
		cancelButton.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false));
		cancelButton.setText(DefaultLangResources.getI18NStringOrDefault("cancel"));
		cancelButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				setReturnValue(null);
				getParent().close();
			}
		});

		shell.pack();
	}
}