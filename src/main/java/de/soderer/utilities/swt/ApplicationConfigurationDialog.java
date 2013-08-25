package de.soderer.utilities.swt;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;

import de.soderer.utilities.ConfigurationProperties;
import de.soderer.utilities.LangResources;
import de.soderer.utilities.SystemUtilities;
import de.soderer.utilities.Utilities;

public class ApplicationConfigurationDialog extends ModalDialog<Boolean> {
	private String applicationName;
	private ConfigurationProperties applicationConfiguration;
	private Image iconImage;
	
	public ApplicationConfigurationDialog(Shell shell, ConfigurationProperties applicationConfiguration, String applicationName) throws Exception {   
		super(shell, SWT.RESIZE);
		this.applicationConfiguration = applicationConfiguration;
		this.applicationName = applicationName;
		this.iconImage = null;
	}

	public ApplicationConfigurationDialog(Shell shell, ConfigurationProperties applicationConfiguration, String applicationName, Image iconImage) throws Exception {   
		super(shell, SWT.RESIZE);
		this.applicationConfiguration = applicationConfiguration;
		this.applicationName = applicationName;
		this.iconImage = iconImage;
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
		dataPart.setLayout(new GridLayout(4, false));
		
		scrolledPart.setContent(dataPart);
		scrolledPart.setExpandHorizontal(true);
		scrolledPart.setExpandVertical(true);
		
		for (String key : Utilities.asSortedList(applicationConfiguration.getKeyList())) {
			if (!key.endsWith(ConfigurationProperties.MAXSIZE_EXTENSION)) {
				String description = key;
				if (LangResources.existsKey("conf." + key) && StringUtils.isNotBlank(LangResources.get("conf." + key))) {
					description = LangResources.get("conf." + key);
				}
				
				Label label = new Label(dataPart, SWT.FILL);
				label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));
				label.setText(description);
				
				String value = ConfigurationProperties.escape(applicationConfiguration.get(key));
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
				
				// add Help Icon
				if (LangResources.existsKey("conf." + key + ".help") && StringUtils.isNotBlank(LangResources.get("conf." + key + ".help"))) {
					final String helpText = LangResources.get("conf." + key + ".help");
					Button button = new Button(dataPart, SWT.PUSH);
				    button.setImage(new Image(shell.getDisplay(), getClass().getResourceAsStream("question.png")));
				    button.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));
				    button.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent arg0) {
							new QuestionDialog(applicationName, helpText, getShell(), LangResources.get("ok")).open();
						}
					});
				} else {
					Label dummyLabel = new Label(dataPart, SWT.FILL);
					dummyLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));
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
		storeButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setReturnValue(true);
				getShell().close();
			}
		});
		
		Button createLinkButton = new Button(buttonArea, SWT.PUSH);
		createLinkButton.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false, 1, 1));
		createLinkButton.setText(LangResources.get("createDesktopLink"));
		createLinkButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (!SystemUtilities.isJavaWebstartApp()) {
					if (SystemUtilities.isWindowsSystem()) {
						String desktopLinkFilePath = System.getProperty("user.home") + File.separator + "Desktop" + File.separator + applicationName + ".lnk";
						if (new File(desktopLinkFilePath).exists()) {
							new File(desktopLinkFilePath).delete();
						}
						
						String iconPath = System.getProperty("user.home") + File.separator + applicationName + ".ico";
						if (new File(iconPath).exists()) {
							new File(iconPath).delete();
						}
	
			        	String tempScriptFile = System.getProperty("java.io.tmpdir") + File.separator + applicationName + "_createlink.vbs";
			        	if (new File(tempScriptFile).exists()) {
							new File(tempScriptFile).delete();
						}
						
						if (iconImage != null) {
							ImageLoader loader = new ImageLoader();
						    loader.data = new ImageData[] {iconImage.getImageData()};
						    loader.save(iconPath, SWT.IMAGE_ICO);
						}
						
						FileDialog dialog = new FileDialog(getShell(), SWT.OPEN);
				        dialog.setText(LangResources.get("createDesktopLink"));
				        String fileName = dialog.open();
				        if (fileName != null) {
							StringBuilder data = new StringBuilder();
							data.append("set shortcut = CreateObject(\"WScript.Shell\").CreateShortcut(\"" + desktopLinkFilePath + "\")\n");
							data.append("shortcut.Description = \""+ applicationName + "\"\n");
							data.append("shortcut.TargetPath = \"" + fileName + "\"\n");
							data.append("shortcut.IconLocation = \"" + iconPath + "\"\n");
							data.append("shortcut.Save\n");
							
							try {
								FileUtils.writeStringToFile(new File(tempScriptFile), data.toString(), "UTF-8");
								
								Runtime.getRuntime().exec("wscript \"" + tempScriptFile + "\"");
								
								MessageBox messageBox = new MessageBox(getShell(), SWT.OK);
								messageBox.setText(LangResources.get("createDesktopLink"));
								messageBox.setMessage(LangResources.get("desktopLinkWasCreatedAt", desktopLinkFilePath));
								messageBox.open();
								
								new File(tempScriptFile).delete();
							} catch (IOException e1) {
								MessageBox messageBox = new MessageBox(getShell(), SWT.ICON_ERROR | SWT.OK);
								messageBox.setText(LangResources.get("createDesktopLink"));
								messageBox.setMessage(LangResources.get("errorMessage", e1.getMessage()));
								messageBox.open();
							}
				        }	
					} else if (SystemUtilities.isLinuxSystem()) {
						String desktopLinkFilePath = System.getProperty("user.home") + File.separator + "Desktop" + File.separator + applicationName + ".desktop";
						if (new File(desktopLinkFilePath).exists()) {
							new File(desktopLinkFilePath).delete();
						}
						
						String iconPath = System.getProperty("user.home") + File.separator + applicationName + ".png";
						if (new File(iconPath).exists()) {
							new File(iconPath).delete();
						}
						
						FileDialog dialog = new FileDialog(getShell(), SWT.OPEN);
				        dialog.setText(LangResources.get("createDesktopLink"));
				        String fileName = dialog.open();
				        if (fileName != null) {
							StringBuilder data = new StringBuilder();
							data.append("[Desktop Entry]\n");
							data.append("Encoding=UTF-8\n");
							data.append("Name=" + applicationName + "\n");
							
							if (iconImage != null) {
								ImageLoader loader = new ImageLoader();
							    loader.data = new ImageData[] {iconImage.getImageData()};
							    loader.save(iconPath, SWT.IMAGE_PNG);
							    
								data.append("Icon=" + iconPath + "\n");
							}
							
							// %U-parameter allows drag and drop of files in unity on starter
							data.append("Exec=java -jar " + fileName + " %U\n");
							data.append("Terminal=false\n");
							data.append("Type=Application\n");
							
							try {
								FileUtils.writeStringToFile(new File(desktopLinkFilePath), data.toString(), "UTF-8");
								
								MessageBox messageBox = new MessageBox(getShell(), SWT.OK);
								messageBox.setText(LangResources.get("createDesktopLink"));
								messageBox.setMessage(LangResources.get("desktopLinkWasCreatedAt", desktopLinkFilePath));
								messageBox.open();
							} catch (IOException e1) {
								MessageBox messageBox = new MessageBox(getShell(), SWT.ICON_ERROR | SWT.OK);
								messageBox.setText(LangResources.get("createDesktopLink"));
								messageBox.setMessage(LangResources.get("errorMessage", e1.getMessage()));
								messageBox.open();
							}
				        }
					}
				} else {
//					try {
//						String desktopLinkFilePath = System.getProperty("user.home") + File.separator + "Desktop" + File.separator + applicationName + ".desktop";
//						if (new File(desktopLinkFilePath).exists()) {
//							new File(desktopLinkFilePath).delete();
//						}
//						
//						Runtime.getRuntime().exec("javaws -uninstall http://www.soderer.de/index.php?download=javawebstart/jnlp/" + applicationName + ".jnlp");
//						Runtime.getRuntime().exec("javaws -import http://www.soderer.de/index.php?download=javawebstart/jnlp/" + applicationName + ".jnlp");
//						Runtime.getRuntime().exec("javaws -import -silent -shortcut http://www.soderer.de/index.php?download=javawebstart/jnlp/" + applicationName + ".jnlp");
//						
//						MessageBox messageBox = new MessageBox(getShell(), SWT.OK);
//						messageBox.setText(LangResources.get("createDesktopLink"));
//						messageBox.setMessage(LangResources.get("desktopLinkWasCreatedAt", desktopLinkFilePath));
//						messageBox.open();
//					} catch (IOException e1) {
//						MessageBox messageBox = new MessageBox(getShell(), SWT.ICON_ERROR | SWT.OK);
//						messageBox.setText(LangResources.get("createDesktopLink"));
//						messageBox.setMessage(LangResources.get("errorMessage", e1.getMessage()));
//						messageBox.open();
//					}
				}
			}
		});

		Button showSystemPropertiesButton = new Button(buttonArea, SWT.PUSH);
		showSystemPropertiesButton.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false, 1, 1));
		showSystemPropertiesButton.setText(LangResources.get("showSystemProperties"));
		showSystemPropertiesButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Map<String, String> properties = Utilities.getSystemPropertiesMap();
				for (Entry<String, String> entry : properties.entrySet()) {
					properties.put(entry.getKey(), entry.getValue().replace("\n", "\\n").replace("\t", "\\t").replace("\r", "\\r"));
				}
				new ShowDataDialog(applicationName + " " + LangResources.get("systemProperties"), Utilities.getStringFromMap(properties, "\n", " = ", true), getShell(), LangResources.get("close")).open();
			}
		});
		
		Button showRuntimePropertiesButton = new Button(buttonArea, SWT.PUSH);
		showRuntimePropertiesButton.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false, 1, 1));
		showRuntimePropertiesButton.setText(LangResources.get("showRuntimeProperties"));
		showRuntimePropertiesButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Map<String, String> properties = SystemUtilities.getRuntimeProperties(true);
				for (Entry<String, String> entry : properties.entrySet()) {
					properties.put(entry.getKey(), entry.getValue().replace("\n", "\\n").replace("\t", "\\t").replace("\r", "\\r"));
				}
				new ShowDataDialog(applicationName + " " + LangResources.get("runtimeProperties"), Utilities.getStringFromMap(properties, "\n", " = ", true), getShell(), LangResources.get("close")).open();
			}
		});
		
		Button showSwtPropertiesButton = new Button(buttonArea, SWT.PUSH);
		showSwtPropertiesButton.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false, 1, 1));
		showSwtPropertiesButton.setText(LangResources.get("showSwtProperties"));
		showSwtPropertiesButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				new ShowDataDialog(applicationName + " " + LangResources.get("swtProperties"), Utilities.getStringFromMap(SwtUtilities.getSwtHandlesStatus(null), "\n", " = ", true), getShell(), LangResources.get("close")).open();
			}
		});

		Button cancelButton = new Button(buttonArea, SWT.PUSH);
		cancelButton.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false, 1, 1));
		cancelButton.setText(LangResources.get("cancel"));
		cancelButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setReturnValue(false);
				getShell().close();
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
			applicationConfiguration.set(key, ConfigurationProperties.unEscape(((Text)e.widget).getText()));
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
