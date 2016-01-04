package de.soderer.utilities.swt;

import org.apache.tomcat.dbcp.dbcp.BasicDataSource;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import de.soderer.utilities.DbUtilities;
import de.soderer.utilities.LangResources;
import de.soderer.utilities.Utilities;
import de.soderer.utilities.swt.RadioButtonGroup.ArrangementStyle;

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
	
	public DbConnectionConfigDialog(Shell shell, String title, String text) {
		super(shell, title);
		this.text = text;
	}
	
	@Override
	protected void createComponents(Shell shell) throws Exception {
		shell.setLayout(new GridLayout(1, true));
		
		if (Utilities.isNotBlank(text)) {
			Label label = new Label(shell, SWT.NONE);
			label.setLayoutData(new GridData(SWT.LEFT, SWT.UP, false, false, 1, 1));
			label.setText(text);
		}

		dbTypeRadioButtonGroup = new RadioButtonGroup(shell, LangResources.get("dbType"), SWT.NONE, ArrangementStyle.Horizontal, "Oracle", "MySQL");
		dbTypeRadioButtonGroup.setSelectedButtonIndex(1);
		dbTypeRadioButtonGroup.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				isOracle = ((RadioButtonGroup) e.widget).getSelectedButtonIndex() == 0;
			}
		});
		
		
		Composite credentialsComposite = new Composite(shell, SWT.NONE);
		credentialsComposite.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));
		credentialsComposite.setLayout(SwtUtilities.createNoMarginGridLayout(2, false));
		
		Label hostnameLabel = new Label(credentialsComposite, SWT.NONE);
		hostnameLabel.setText(LangResources.get("hostname") + ": ");
		
		dbServerHostnameText = new Text(credentialsComposite, SWT.BORDER);
		dbServerHostnameText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				dbServerHostname = ((Text) e.widget).getText();
				checkStatus();
			}
		});
		
		Label portLabel = new Label(credentialsComposite, SWT.NONE);
		portLabel.setText(LangResources.get("port") + ": ");
		
		dbServerPortText = new NumberText(credentialsComposite, SWT.BORDER, 5);
		dbServerPortText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		dbServerPortText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				dbServerPort = ((NumberText) e.widget).getValue();
				checkStatus();
			}
		});
		
		Label dbNameLabel = new Label(credentialsComposite, SWT.NONE);
		dbNameLabel.setText(LangResources.get("dbName") + ": ");
		
		dbNameText = new Text(credentialsComposite, SWT.BORDER);
		dbNameText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				dbName = ((Text) e.widget).getText();
				checkStatus();
			}
		});
		
		Label userLabel = new Label(credentialsComposite, SWT.NONE);
		userLabel.setText(LangResources.get("userName") + ": ");
		
		userNameText = new Text(credentialsComposite, SWT.BORDER);
		userNameText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				userName = ((Text) e.widget).getText();
				checkStatus();
			}
		});
		
		Label passwordLabel = new Label(credentialsComposite, SWT.NONE);
		passwordLabel.setText(LangResources.get("password") + ": ");
		
		passwordText = new Text(credentialsComposite, SWT.PASSWORD | SWT.BORDER);
		passwordText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				password = ((Text) e.widget).getText();
				checkStatus();
			}
		});
		
		SwtUtilities.addMainButtonComposite(shell, this);

		shell.pack();

		shell.setSize(shell.getSize().x, shell.getSize().y);
		shell.setMinimumSize(shell.getSize().x, shell.getSize().y);
	    
	    checkStatus();
	}

	private void checkStatus() {
		okButton.setEnabled(Utilities.isNotBlank(dbServerHostnameText.getText()) && Utilities.isNotBlank(dbNameText.getText()) && Utilities.isNotBlank(userNameText.getText()) && Utilities.isNotBlank(passwordText.getText()));
	}

	public BasicDataSource getDbDatasource() {
		BasicDataSource basicDataSource = new BasicDataSource();
		if (isOracle) {
			basicDataSource.setDriverClassName(DbUtilities.DB_DRIVER_NAME_ORACLE);
		} else {
			basicDataSource.setDriverClassName(DbUtilities.DB_DRIVER_NAME_MYSQL);
		}
	    basicDataSource.setUrl(DbUtilities.generateUrlConnectionString(isOracle, dbServerHostname, dbServerPort, dbName));
	 
	    basicDataSource.setUsername(userName);
	    basicDataSource.setPassword(password);
	 
//	    basicDataSource.setMaxActive();
//	    basicDataSource.setMaxIdle();
//	    basicDataSource.setMaxWait();
//	    basicDataSource.setMinIdle();
//	    basicDataSource.setDefaultAutoCommit();
//	    basicDataSource.setDefaultReadOnly();
//	    basicDataSource.setTestOnBorrow();
//	    basicDataSource.setTestOnReturn();
//	    basicDataSource.setTestWhileIdle();
//		basicDataSource.setMinEvictableIdleTimeMillis();
//		basicDataSource.setTimeBetweenEvictionRunsMillis();
//		basicDataSource.setNumTestsPerEvictionRun();
//		basicDataSource.setMaxOpenPreparedStatements();
//		basicDataSource.setAccessToUnderlyingConnectionAllowed();
//		basicDataSource.setInitialSize();
//		basicDataSource.setPoolPreparedStatements();
//		basicDataSource.setDefaultTransactionIsolation();
//		basicDataSource.setDefaultCatalog();
//	    basicDataSource.setValidationQuery();
	 
	    return basicDataSource;
	}
}
