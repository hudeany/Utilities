package de.soderer.utilities.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
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

public class DbConnectionConfigDialog extends ModalDialog<Boolean> {
	private String text = null;

	private boolean isOracle = false;
	private String dbServerHostname = null;
	private int dbServerPort = -1;
	private String dbName = null;
	private String userName = null;
	private String password = null;

	private RadioButtonGroup dbTypeRadioButtonGroup;
	private Text dbServerHostnameText;
	private NumberText dbServerPortText;
	private Text dbNameText;
	private Text userNameText;
	private Text passwordText;

	private Button okButton;

	public DbConnectionConfigDialog(final Shell shell, final String title, final String text) {
		super(shell, title);
		this.text = text;
	}

	@Override
	protected void createComponents(final Shell shell) throws Exception {
		shell.setLayout(new GridLayout(1, true));

		if (Utilities.isNotBlank(text)) {
			final Label label = new Label(shell, SWT.NONE);
			label.setLayoutData(new GridData(SWT.LEFT, SWT.UP, false, false, 1, 1));
			label.setText(text);
		}

		dbTypeRadioButtonGroup = new RadioButtonGroup(shell, LangResources.get("dbType"), SWT.NONE, ArrangementStyle.Horizontal, "Oracle", "MySQL");
		dbTypeRadioButtonGroup.setSelectedButtonIndex(1);
		dbTypeRadioButtonGroup.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(final ModifyEvent event) {
				isOracle = ((RadioButtonGroup) event.widget).getSelectedButtonIndex() == 0;
			}
		});

		final Composite credentialsComposite = new Composite(shell, SWT.NONE);
		credentialsComposite.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));
		credentialsComposite.setLayout(SwtUtilities.createNoMarginGridLayout(2, false));

		final Label hostnameLabel = new Label(credentialsComposite, SWT.NONE);
		hostnameLabel.setText(LangResources.get("hostname") + ": ");

		dbServerHostnameText = new Text(credentialsComposite, SWT.BORDER);
		dbServerHostnameText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(final ModifyEvent event) {
				dbServerHostname = ((Text) event.widget).getText();
				checkStatus();
			}
		});

		final Label portLabel = new Label(credentialsComposite, SWT.NONE);
		portLabel.setText(LangResources.get("port") + ": ");

		dbServerPortText = new NumberText(credentialsComposite, SWT.BORDER, 5);
		dbServerPortText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		dbServerPortText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(final ModifyEvent event) {
				dbServerPort = ((NumberText) event.widget).getValue();
				checkStatus();
			}
		});

		final Label dbNameLabel = new Label(credentialsComposite, SWT.NONE);
		dbNameLabel.setText(LangResources.get("dbName") + ": ");

		dbNameText = new Text(credentialsComposite, SWT.BORDER);
		dbNameText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(final ModifyEvent event) {
				dbName = ((Text) event.widget).getText();
				checkStatus();
			}
		});

		final Label userLabel = new Label(credentialsComposite, SWT.NONE);
		userLabel.setText(LangResources.get("username") + ": ");

		userNameText = new Text(credentialsComposite, SWT.BORDER);
		userNameText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(final ModifyEvent event) {
				userName = ((Text) event.widget).getText();
				checkStatus();
			}
		});

		final Label passwordLabel = new Label(credentialsComposite, SWT.NONE);
		passwordLabel.setText(LangResources.get("password") + ": ");

		passwordText = new Text(credentialsComposite, SWT.PASSWORD | SWT.BORDER);
		passwordText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(final ModifyEvent event) {
				password = ((Text) event.widget).getText();
				checkStatus();
			}
		});

		final ModalDialog<Boolean> finalParentDialog = this;

		final Composite mainButtonComposite = new Composite(shell, SWT.NONE);
		mainButtonComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		mainButtonComposite.setLayout(SwtUtilities.createNoMarginGridLayout(2, true));

		okButton = new Button(mainButtonComposite, SWT.PUSH);
		okButton.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false));
		okButton.setText(LangResources.get("ok"));
		okButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				finalParentDialog.setReturnValue(true);
				finalParentDialog.getParent().close();
			}
		});

		final Button cancelButton = new Button(mainButtonComposite, SWT.PUSH);
		cancelButton.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false));
		cancelButton.setText(LangResources.get("cancel"));
		cancelButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				finalParentDialog.setReturnValue(false);
				finalParentDialog.getParent().close();
			}
		});

		shell.pack();

		shell.setSize(shell.getSize().x, shell.getSize().y);
		shell.setMinimumSize(shell.getSize().x, shell.getSize().y);

		checkStatus();
	}

	private void checkStatus() {
		okButton.setEnabled(Utilities.isNotBlank(dbServerHostnameText.getText()) && Utilities.isNotBlank(dbNameText.getText()) && Utilities.isNotBlank(userNameText.getText())
				&& Utilities.isNotBlank(passwordText.getText()));
	}

	public String getDbServerHostname() {
		return dbServerHostname;
	}

	public int getDbServerPort() {
		return dbServerPort;
	}

	public String getDbName() {
		return dbName;
	}

	public String getUserName() {
		return userName;
	}

	public String getPassword() {
		return password;
	}

	public Text getDbNameText() {
		return dbNameText;
	}

	public boolean isOracle() {
		return isOracle;
	}
}
