package de.soderer.utilities.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;

import de.soderer.utilities.ConfigurationProperties;
import de.soderer.utilities.LangResources;
import de.soderer.utilities.Utilities;
import de.soderer.utilities.swt.ModalDialog;

public class ApplicationConfigurationDialog extends ModalDialog<Boolean> {
	private String applicationName;
	private ConfigurationProperties applicationConfiguration;
	
	public ApplicationConfigurationDialog(Shell shell, ConfigurationProperties applicationConfiguration, String applicationName) throws Exception {   
		super(shell, SWT.RESIZE);
		this.applicationConfiguration = applicationConfiguration;
		this.applicationName = applicationName;
	}

	@Override
	protected void createComponents(Shell shell) {
		shell.setText(applicationName + " " + LangResources.get("configuration"));
		shell.setLayout(new FillLayout(SWT.VERTICAL));
		shell.setMinimumSize(500, 300);
		shell.setSize(500, 300);
		
		Composite mainComposite = new Composite(shell, SWT.NONE);
		mainComposite.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, true));
		mainComposite.setLayout(new GridLayout(1, false));
		
		ScrolledComposite scrolledPart = new ScrolledComposite(mainComposite, SWT.H_SCROLL | SWT.V_SCROLL);
		scrolledPart.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, true));
		scrolledPart.setLayout(new GridLayout(1, false));
		
		Composite dataPart = new Composite(scrolledPart, SWT.NONE);
		dataPart.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false, 1, 1));
		dataPart.setLayout(new GridLayout(3, false));
		
		scrolledPart.setContent(dataPart);
		scrolledPart.setExpandHorizontal(true);
		scrolledPart.setExpandVertical(true);
		
		for (String key : Utilities.asSortedList(applicationConfiguration.getKeyList())) {
			if (!key.endsWith(ConfigurationProperties.MAXSIZE_EXTENSION)) {
				Label label = new Label(dataPart, SWT.FILL);
				label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));
				label.setText(key);
				
				String value = applicationConfiguration.get(key);
				if (applicationConfiguration.containsKey(key + ConfigurationProperties.MAXSIZE_EXTENSION)) {
					Spinner maxSizeTextfield = new Spinner(dataPart, SWT.NONE);
					maxSizeTextfield.setValues(applicationConfiguration.getInteger(key + ConfigurationProperties.MAXSIZE_EXTENSION), 1, 50, 0, 1, 10);
					maxSizeTextfield.addModifyListener(new SpinnerValueChangeListener(key + ConfigurationProperties.MAXSIZE_EXTENSION));
					GridData gridData1 = new GridData(SWT.FILL, SWT.UP, false, false, 1, 1);
					gridData1.widthHint = 15;
					maxSizeTextfield.setLayoutData(gridData1);
					
					Text valueTextfield = new Text(dataPart, SWT.BORDER);
					valueTextfield.setText(value);
					valueTextfield.addModifyListener(new TextValueChangeListener(key));
					GridData gridData2 = new GridData(SWT.FILL, SWT.UP, true, false, 1, 1);
					valueTextfield.setLayoutData(gridData2);
				} else if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
					Button keyCheckBox = new Button(dataPart, SWT.CHECK);
					GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
					keyCheckBox.setLayoutData(gridData);
					keyCheckBox.setSelection(Utilities.interpretAsBool(value));
					keyCheckBox.setSize(100, 20);
					keyCheckBox.addSelectionListener(new CheckboxValueChangeListener(key));
				} else {
					Text keyTextfield = new Text(dataPart, SWT.BORDER);
					GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
					keyTextfield.setLayoutData(gridData);
					keyTextfield.setText(value);
					keyTextfield.setSize(100, 20);
					keyTextfield.addModifyListener(new TextValueChangeListener(key));
				}
			}
		}
		
		scrolledPart.pack();
		scrolledPart.setMinSize(200, scrolledPart.getSize().y);
		
		Composite buttonArea = new Composite(mainComposite, SWT.NONE);
		buttonArea.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, true, false, 1, 1));
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
		
		buttonArea.pack();
	}
	
	private class SpinnerValueChangeListener implements ModifyListener {
		private String key;
		
		private SpinnerValueChangeListener(String key) {
			this.key = key;
		}
		
		@Override
		public void modifyText(ModifyEvent e) {
			applicationConfiguration.set(key, Integer.toString(((Spinner)e.widget).getSelection()));
		}
	}
	
	private class TextValueChangeListener implements ModifyListener {
		private String key;
		
		private TextValueChangeListener(String key) {
			this.key = key;
		}
		
		@Override
		public void modifyText(ModifyEvent e) {
			applicationConfiguration.set(key, ((Text)e.widget).getText());
		}
	}
	
	private class CheckboxValueChangeListener implements SelectionListener {
		private String key;
		
		private CheckboxValueChangeListener(String key) {
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
	
	@Override
	protected Boolean getDefaultReturnValue() {
		return false;
	}
}
