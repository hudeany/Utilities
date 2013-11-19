package de.soderer.utilities.swt;

import java.io.File;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RunnableFuture;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import de.soderer.utilities.LangResources;
import de.soderer.utilities.SystemUtilities;
import de.soderer.utilities.UserError;
import de.soderer.utilities.UserError.Reason;

public class UpdateApplicationDialog extends ProgressShowingModalDialog<Boolean> {
	private static final transient Logger logger = Logger.getLogger(UpdateApplicationDialog.class);
	
	private String text;
	private String downloadUrl;
	private boolean dailyUpdateCheck;
	
	public UpdateApplicationDialog(Shell shell, String title, String text, String downloadUrl, boolean dailyUpdateCheck) {
		super(shell, title);

		this.text = text;
		this.downloadUrl = downloadUrl;
		this.dailyUpdateCheck = dailyUpdateCheck;
	}

	@Override
	protected void createComponents(Shell shell) throws Exception {
		shell.setLayout(new FillLayout());
		
		Composite composite = new Composite(shell, SWT.NONE);
		composite.setLayout(SwtUtilities.createNoMarginGridLayout(2, true));
		
		Label label = new Label(composite, SWT.FILL);
		label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, true, 2, 1));
		label.setText(text);
		
		Button dailyUpdateCheckCheckBox = new Button(composite, SWT.CHECK);
		dailyUpdateCheckCheckBox.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true, 2, 1));
		dailyUpdateCheckCheckBox.setSelection(dailyUpdateCheck);
		dailyUpdateCheckCheckBox.setText(LangResources.get("dailyUpdateCheck"));
		dailyUpdateCheckCheckBox.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				dailyUpdateCheck = ((Button) e.widget).getSelection();
			}
		});
	    
		Button updateButton = new Button(composite, SWT.PUSH);
		updateButton.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false));
		updateButton.setText(LangResources.get("update"));
	    updateButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateApplication();
				setReturnValue(true);
				getParent().close();
			}
		});
	    
		Button cancelButton = new Button(composite, SWT.PUSH);
		cancelButton.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false));
		cancelButton.setText(LangResources.get("cancel"));
		cancelButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setReturnValue(false);
				getParent().close();
			}
		});

		shell.pack();
	}
	
	private void updateApplication() {
		try {
			String jarFilePath = System.getProperty("process.jar");
			File downloadTempFile = new File(new File(jarFilePath).getParent() + File.separator + "temp_" + new File(jarFilePath).getName());
			
			if (StringUtils.isNotBlank(jarFilePath)) {
				boolean tryDownloadAgain = true;
				while (tryDownloadAgain) {
					tryDownloadAgain = false;
					try {
						boolean success = downloadUpdateFile(downloadTempFile, downloadUrl);
						if (!success) {
							// download stopped by user
							return;
						}
					} catch (UserError e) {
						showMessage(LangResources.get("menu.updateFileDownload"), e.getMessage());
						if (e.getReason() == Reason.UnauthenticatedOrUnauthorized) {
							tryDownloadAgain = true;
						} else {
							return;
						}
					} catch (Exception e) {
						if (e instanceof ExecutionException && e.getCause() instanceof UserError) {
							showMessage(LangResources.get("menu.updateFileDownload"), ((UserError) e.getCause()).getMessage());
							if (((UserError) e.getCause()).getReason() == Reason.UnauthenticatedOrUnauthorized) {
								tryDownloadAgain = true;
							} else {
								return;
							}
						} else {
							showMessage(LangResources.get("menu.updateFileDownload"), e.getMessage());
						}
						return;
					}
				}
				
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
				new QuestionDialog(getParent(), getText(), LangResources.get("cannotUpdate", "Jar file not found")).open();
			}
		} catch (Exception e) {
			logger.error(LangResources.get("cannotUpdate", e.getMessage()), e);
		}
	}
	
	public boolean downloadUpdateFile(File downloadTempFile, String downloadUrl) throws Exception {
		try {
			downloadTempFile.delete();
			if (downloadUrl.contains("<username>") && downloadUrl.contains("<password>")) {
				CredentialsDialog credentialsDialog = new CredentialsDialog(getParent(), getText(), LangResources.get("enterDownloadCredentials"));
				Boolean success = credentialsDialog.open();
				if (success) {
					downloadUrl = downloadUrl.replaceAll("<username>", credentialsDialog.getUsername());
					downloadUrl = downloadUrl.replaceAll("<password>", new String(credentialsDialog.getPassword()));
				} else {
					return false;
				}
			} else {
				if (downloadUrl.contains("<username>")) {
					SimpleInputDialog usernameDialog = new SimpleInputDialog(getParent(), getText(), LangResources.get("enterDownloadUserName"));
					String username = usernameDialog.open();
					if (StringUtils.isNotBlank(username)) {
						downloadUrl = downloadUrl.replaceAll("<username>", username);
					} else {
						return false;
					}
				}
				if (downloadUrl.contains("<password>")) {
					PasswordDialog passwordDialog = new PasswordDialog(getParent(), getText(), LangResources.get("enterDownloadPassword"));
					char[] password = passwordDialog.open();
					if (password != null) {
						downloadUrl = downloadUrl.replaceAll("<password>", new String(password));
					} else {
						return false;
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
			
			RunnableFuture<Boolean> worker = new FileDownloadWorker(this, downloadUrl, downloadTempFile.getAbsolutePath());
			progressDialog = new ProgressDialog(getParent(), LangResources.get("menu.updateFileDownload"), LangResources.get("menu.updateFileDownload"));
			new Thread(worker).start();
			if (progressDialog.open()) {
				worker.cancel(true);
				showMessage(LangResources.get("menu.updateFileDownload"), LangResources.get("canceledByUser"));
				return false;
			}
			
			return worker.get();
		} catch (Exception e) {
			if (e instanceof ExecutionException && e.getCause() instanceof UserError) {
				throw (UserError) e.getCause();
			} else {
				throw e;
			}
		}
	}

	public boolean getDailyUpdateCheck() {
		return dailyUpdateCheck;
	}
}
