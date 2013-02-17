package de.soderer.utilities.swt;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import de.soderer.utilities.LangResources;
import de.soderer.utilities.SystemUtilities;
import de.soderer.utilities.Utilities;

public class UpdateApplicationDialog extends ModalDialog<Boolean> {
	private static final transient Logger logger = Logger.getLogger(UpdateApplicationDialog.class);
	
	private String title;
	private String text;
	private String downloadUrl;
	
	public UpdateApplicationDialog(String title, String text, Shell shell, String downloadUrl) {
		super(shell);
		this.title = title;
		this.text = text;
		this.downloadUrl = downloadUrl;
	}

	@Override
	protected void createComponents(Shell shell) throws Exception {
		if (StringUtils.isEmpty(title)) {
			shell.setText(getText());
		} else {
			shell.setText(title);
		}
		shell.setLayout(new FillLayout());
		
		Composite composite = new Composite(shell, SWT.NONE);
		composite.setLayout(new GridLayout(2, true));
		
		Label label = new Label(composite, SWT.FILL);
		label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, true, 2, 1));
		label.setText(text);
	    
		Button updateButton = new Button(composite, SWT.PUSH);
		updateButton.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false));
		updateButton.setText(LangResources.get("update"));
	    updateButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateAppliaction();
				setReturnValue(true);
				getShell().close();
			}
		});
	    
		Button cancelButton = new Button(composite, SWT.PUSH);
		cancelButton.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false));
		cancelButton.setText(LangResources.get("cancel"));
		cancelButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setReturnValue(false);
				getShell().close();
			}
		});

		shell.pack();
	}
	
	private void updateAppliaction() {
		try {
			String jarFilePath = System.getProperty("process.jar");
			if (StringUtils.isNotBlank(jarFilePath)) {
				File downloadTempFile = new File(new File(jarFilePath).getParent() + File.separator + "temp_" + new File(jarFilePath).getName());
				downloadTempFile.delete();
				if (downloadUrl.contains("<username>") && downloadUrl.contains("<password>")) {
					CredentialsDialog credentialsDialog = new CredentialsDialog(title, LangResources.get("enterDownloadCredentials"), getShell());
					Boolean success = credentialsDialog.open();
					if (success) {
						downloadUrl = downloadUrl.replaceAll("<username>", credentialsDialog.getUsername());
						downloadUrl = downloadUrl.replaceAll("<password>", credentialsDialog.getPassword());
					} else {
						return;
					}
				} else {
					if (downloadUrl.contains("<username>")) {
						SimpleInputDialog usernameDialog = new SimpleInputDialog(title, LangResources.get("enterDownloadUserName"), getShell(), LangResources.get("ok"), LangResources.get("cancel"));
						String username = usernameDialog.open();
						if (StringUtils.isNotBlank(username)) {
							downloadUrl = downloadUrl.replaceAll("<username>", username);
						} else {
							return;
						}
					}
					if (downloadUrl.contains("<password>")) {
						PasswordDialog passwordDialog = new PasswordDialog(title, LangResources.get("enterDownloadPassword"), getShell(), LangResources.get("ok"), LangResources.get("cancel"));
						String password = passwordDialog.open();
						if (StringUtils.isNotBlank(password)) {
							downloadUrl = downloadUrl.replaceAll("<password>", password);
						} else {
							return;
						}
					}
				}
				if (downloadUrl.contains("<system>")) {
					if (SystemUtilities.isWindowsSystem()) {
						downloadUrl = downloadUrl.replaceAll("<system>", "Windows");
					} else if (SystemUtilities.isLinuxSystem()) {
						downloadUrl = downloadUrl.replaceAll("<system>", "Linux");
					}
				}
				if (downloadUrl.contains("<bitmode>")) {
					if (System.getProperty("os.arch") != null && System.getProperty("os.arch").contains("64")) {
						downloadUrl = downloadUrl.replaceAll("<bitmode>", "64");
					} else if (SystemUtilities.isLinuxSystem()) {
						downloadUrl = downloadUrl.replaceAll("<bitmode>", "32");
					}
				}
				Utilities.downloadFile(downloadUrl, downloadTempFile.getAbsolutePath());
				
				if (SystemUtilities.isWindowsSystem()) {
					File batchFile = new File(new File(jarFilePath).getParent() + File.separator + "batchUpdate_" + new File(jarFilePath).getName() + ".cmd");
					
					//Create Batchfile
					FileUtils.write(batchFile,
						"@echo off\r\n"
						+ "del \"" + jarFilePath + "\"\r\n"
						+ "if exist \"" + jarFilePath + "\" (\r\n"
						+ "ping -n 3 127.0.0.1 >nul\r\n"
						+ "del \"" + jarFilePath + "\"\r\n"
						+ "if exist \"" + jarFilePath + "\" (\r\n"
						+ "ping -n 3 127.0.0.1 >nul\r\n"
						+ "del \"" + jarFilePath + "\"\r\n"
						+ "if exist \"" + jarFilePath + "\" (\r\n"
						+ "ping -n 3 127.0.0.1 >nul\r\n"
						+ "del \"" + jarFilePath + "\"\r\n"
						+ ")\r\n"
						+ ")\r\n"
						+ ")\r\n"
						+ "ren \"" + downloadTempFile.getAbsolutePath() + "\" \"" + new File(jarFilePath).getName() + "\"\r\n"
						+ "if not exist \"" + downloadTempFile.getAbsolutePath() + "\" (\r\n"
						+ "java -jar \"" + jarFilePath + "\"\r\n"
						+ "del \"" + batchFile.getAbsolutePath() + "\"\r\n"
						+ ")\r\n");
					
					Runtime.getRuntime().exec("cmd /c start /B " + batchFile.getAbsolutePath());
					Runtime.getRuntime().exit(0);
				} else if (SystemUtilities.isLinuxSystem()) {
					File batchFile = new File(new File(jarFilePath).getParent() + File.separator + "batchUpdate_" + new File(jarFilePath).getName() + ".sh");
					
					//Create Batchfile
					FileUtils.write(batchFile,
						"#!/bin/bash\n"
						+ "rm \"" + jarFilePath + "\"\n"
						+ "if [ -f \"" + jarFilePath + "\" ]; then\n"
						+ "sleep 3\n"
						+ "rm \"" + jarFilePath + "\"\n"
						+ "if [ -f \"" + jarFilePath + "\" ]; then\n"
						+ "sleep 3\n"
						+ "rm \"" + jarFilePath + "\"\n"
						+ "if [ -f \"" + jarFilePath + "\" ]; then\n"
						+ "sleep 3\n"
						+ "rm \"" + jarFilePath + "\"\n"
						+ "fi\n"
						+ "fi\n"
						+ "fi\n"
						+ "mv \"" + downloadTempFile.getAbsolutePath() + "\" \"" + jarFilePath + "\"\n"
						+ "if ! [ -f \"" + downloadTempFile.getAbsolutePath() + "\" ]; then\n"
						+ "java -jar \"" + jarFilePath + "\"\n"
						+ "rm \"" + batchFile.getAbsolutePath() + "\"\n"
						+ "fi\n");
					
					Runtime.getRuntime().exec("sh " + batchFile.getAbsolutePath());
					Runtime.getRuntime().exit(0);
				}
			} else {
				new QuestionDialog(title, LangResources.get("cannotUpdate", "Jar file not found"), getShell()).open();
			}
		} catch (Exception e) {
			logger.error(LangResources.get("cannotUpdate", e.getMessage()), e);
		}
	}
}
