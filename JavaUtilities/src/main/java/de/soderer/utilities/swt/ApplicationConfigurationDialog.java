package de.soderer.utilities.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import de.soderer.utilities.ConfigurationProperties;
import de.soderer.utilities.LangResources;
import de.soderer.utilities.Utilities;

public class ApplicationConfigurationDialog extends ModalDialog<Boolean> {
	private String applicationName;
	private ConfigurationProperties applicationConfiguration;
	
	public ApplicationConfigurationDialog(Shell shell, ConfigurationProperties applicationConfiguration, String applicationName) throws Exception {   
		super(shell);
		this.applicationConfiguration = applicationConfiguration;
		this.applicationName = applicationName;
	}

	@Override
	protected void createComponents(Shell shell) {
		shell.setText(applicationName + " " + LangResources.get("configuration"));
		shell.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, true));
		shell.setLayout(new GridLayout(2, false));
		
		for (String key : Utilities.asSortedList(applicationConfiguration.getKeyList())) {
			Label label = new Label(shell, SWT.FILL);
			label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));
			label.setText(key);
			
			String value = applicationConfiguration.get(key);
			boolean isBooleanValue = "true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value);
			if (isBooleanValue) {
				Button keyCheckBox = new Button(shell, SWT.CHECK);
				GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
				gridData.widthHint = 300;
				keyCheckBox.setLayoutData(gridData);
				keyCheckBox.setSelection(Utilities.interpretAsBool(value));
				keyCheckBox.setSize(100, 20);
				keyCheckBox.addSelectionListener(new BooleanValueChangeListener(key));
			} else {
				Text keyTextfield = new Text(shell, SWT.BORDER);
				GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
				gridData.widthHint = 300;
				keyTextfield.setLayoutData(gridData);
				keyTextfield.setText(value);
				keyTextfield.setSize(100, 20);
				keyTextfield.addModifyListener(new StringValueChangeListener(key));
			}
		}
		
		Composite buttonArea = new Composite(shell, SWT.NONE);
		buttonArea.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, true, 2, 1));
		buttonArea.setLayout(new GridLayout(2, true));

		Button storeButton = new Button(buttonArea, SWT.PUSH);
		storeButton.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false, 1, 1));
		storeButton.setText(LangResources.get("save"));
		storeButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setReturnValue(true);
				getShell().close();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});

		Button cancelButton = new Button(buttonArea, SWT.PUSH);
		cancelButton.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false, 1, 1));
		cancelButton.setText(LangResources.get("cancel"));
		cancelButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setReturnValue(false);
				getShell().close();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		
		shell.pack();
	}
	
	private class StringValueChangeListener implements ModifyListener {
		private String key;
		
		private StringValueChangeListener(String key) {
			this.key = key;
		}
		
		@Override
		public void modifyText(ModifyEvent e) {
			applicationConfiguration.set(key, ((Text)e.widget).getText());
		}
	}
	
	private class BooleanValueChangeListener implements SelectionListener {
		private String key;
		
		private BooleanValueChangeListener(String key) {
			this.key = key;
		}

		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
			applicationConfiguration.set(key, ((Button)e.widget).getSelection());
		}

		@Override
		public void widgetSelected(SelectionEvent e) {
			applicationConfiguration.set(key, ((Button)e.widget).getSelection());
		}
	}
}
