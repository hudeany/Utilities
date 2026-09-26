package de.soderer.utilities.swt;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;

import de.soderer.utilities.ConfigurationProperties;
import de.soderer.utilities.DefaultLangResources;
import de.soderer.utilities.FileUtilities;
import de.soderer.utilities.LangResources;
import de.soderer.utilities.SystemUtilities;
import de.soderer.utilities.Utilities;

public class ApplicationConfigurationDialog extends ModalDialog<Boolean> {
	/** Language of the root language file without language suffix ("LanguageProperties.properties") */
	private static final String DEFAULT_LANGUAGE_CODE = "en";

	private final String applicationName;
	private final String startUpClassName;
	private final ConfigurationProperties applicationConfiguration;
	private final byte[] iconImageDataWindows;
	private final Image iconImageLinux;

	private boolean configurationChanged = false;
	private final Map<String, Object> pendingChanges = new LinkedHashMap<>();

	/** Languages offered for selection, if the configuration contains no enum definition for the language key */
	private List<String> availableLanguages = LangResources.getAvailableLanguages(DEFAULT_LANGUAGE_CODE);

	/** Per drop down key: stored value -> text shown in the drop down */
	private final Map<String, Map<String, String>> dropDownDisplayValues = new HashMap<>();

	public ApplicationConfigurationDialog(final Shell shell, final ConfigurationProperties applicationConfiguration, final String applicationName, final String startUpClassName) {
		super(shell, SWT.RESIZE, applicationName + " " + DefaultLangResources.getI18NStringOrDefault("configuration"));
		this.applicationConfiguration = applicationConfiguration;
		this.applicationName = applicationName;
		this.startUpClassName = startUpClassName;
		iconImageDataWindows = null;
		iconImageLinux = null;
	}

	public ApplicationConfigurationDialog(final Shell shell, final ConfigurationProperties applicationConfiguration, final String applicationName, final String startUpClassName, final byte[] iconImageWindows, final Image iconImageLinux) {
		super(shell, SWT.RESIZE, applicationName + " " + DefaultLangResources.getI18NStringOrDefault("configuration"));
		this.applicationConfiguration = applicationConfiguration;
		this.applicationName = applicationName;
		this.startUpClassName = startUpClassName;
		iconImageDataWindows = iconImageWindows;
		this.iconImageLinux = iconImageLinux;
	}

	@Override
	protected void createComponents(final Shell shell) throws Exception {
		shell.setLayout(new FillLayout(SWT.VERTICAL));
		shell.setMinimumSize(500, 300);
		shell.setSize(500, 300);

		final Composite mainComposite = new Composite(shell, SWT.NONE);
		mainComposite.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, true));
		mainComposite.setLayout(SwtUtilities.createSmallMarginGridLayout(1, false));

		final ScrolledComposite scrolledPart = new ScrolledComposite(mainComposite, SWT.H_SCROLL | SWT.V_SCROLL);
		scrolledPart.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, true));
		scrolledPart.setLayout(SwtUtilities.createSmallMarginGridLayout(1, false));

		final Composite dataPart = new Composite(scrolledPart, SWT.NONE);
		dataPart.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false, 1, 1));
		dataPart.setLayout(SwtUtilities.createSmallMarginGridLayout(4, false));

		scrolledPart.setContent(dataPart);
		scrolledPart.setExpandHorizontal(true);
		scrolledPart.setExpandVertical(true);

		for (final String key : Utilities.asSortedList(applicationConfiguration.getKeyList())) {
			if (!key.endsWith(ConfigurationProperties.MAXSIZE_EXTENSION) && !key.endsWith(ConfigurationProperties.ENUM_EXTENSION) || ConfigurationProperties.isInternalKey(key)) {
				String description = key;
				if (LangResources.existsKey("conf." + key) && Utilities.isNotBlank(DefaultLangResources.getI18NStringOrDefault("conf." + key))) {
					description = DefaultLangResources.getI18NStringOrDefault("conf." + key);
				}

				final Label label = new Label(dataPart, SWT.FILL);
				label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));
				label.setText(description);

				final String value = applicationConfiguration.get(key);
				if (applicationConfiguration.containsKey(key + ConfigurationProperties.MAXSIZE_EXTENSION)) {
					final Spinner maxSizeTextfield = new Spinner(dataPart, SWT.NONE);
					maxSizeTextfield.setValues(applicationConfiguration.getInteger(key + ConfigurationProperties.MAXSIZE_EXTENSION), 1, 50, 0, 1, 10);
					maxSizeTextfield.addModifyListener(new SpinnerValueChangeListener(key + ConfigurationProperties.MAXSIZE_EXTENSION));
					final GridData gridData1 = new GridData(SWT.FILL, SWT.UP, false, false, 1, 1);
					gridData1.widthHint = 15;
					maxSizeTextfield.setLayoutData(gridData1);

					final Text valueTextfield = new Text(dataPart, SWT.BORDER);
					valueTextfield.setText(value);
					valueTextfield.addModifyListener(new TextValueChangeListener(key));
					final GridData gridData2 = new GridData(SWT.FILL, SWT.UP, true, false, 1, 1);
					valueTextfield.setLayoutData(gridData2);
				} else if (applicationConfiguration.containsKey(key + ConfigurationProperties.ENUM_EXTENSION)) {
					final DropDown dropDown = createDropDown(dataPart, key, Utilities.splitAndTrimList(applicationConfiguration.get(key + ConfigurationProperties.ENUM_EXTENSION)), value);
					dropDown.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false, 2, 1));
				} else if (ConfigurationProperties.CONFIG_KEY_LANGUAGE.equalsIgnoreCase(key)) {
					// No enum definition in the configuration: offer the languages of the existing language files
					final DropDown dropDown = createDropDown(dataPart, key, availableLanguages == null ? new ArrayList<>() : availableLanguages, value);
					dropDown.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false, 2, 1));
				} else if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
					final Button keyCheckBox = new Button(dataPart, SWT.CHECK);
					final GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
					keyCheckBox.setLayoutData(gridData);
					keyCheckBox.setSelection(Utilities.interpretAsBool(value));
					keyCheckBox.setSize(100, 20);
					keyCheckBox.addSelectionListener(new CheckboxValueChangeListener(key));
				} else {
					final Text keyTextfield = new Text(dataPart, SWT.BORDER);
					final GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
					keyTextfield.setLayoutData(gridData);
					keyTextfield.setText(value);
					keyTextfield.setSize(100, 20);
					keyTextfield.addModifyListener(new TextValueChangeListener(key));
				}

				if (LangResources.existsKey("conf." + key + ".help") && Utilities.isNotBlank(DefaultLangResources.getI18NStringOrDefault("conf." + key + ".help"))) {
					final String helpText = DefaultLangResources.getI18NStringOrDefault("conf." + key + ".help");
					final Button button = new Button(dataPart, SWT.PUSH);
					try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("images/icons/question.png")) {
						button.setImage(new Image(shell.getDisplay(), inputStream));
					}
					button.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));
					button.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(final SelectionEvent arg0) {
							new QuestionDialog(getParent(), applicationName, helpText, DefaultLangResources.getI18NStringOrDefault("ok")).open();
						}
					});
				} else {
					final Label dummyLabel = new Label(dataPart, SWT.FILL);
					dummyLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));
				}
			}
		}

		scrolledPart.pack();
		scrolledPart.setMinSize(200, scrolledPart.getSize().y);

		final Composite buttonArea = new Composite(mainComposite, SWT.NONE);
		buttonArea.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, true, false, 1, 1));
		buttonArea.setLayout(SwtUtilities.createSmallMarginGridLayout(2, true));

		final Button storeButton = new Button(buttonArea, SWT.PUSH);
		storeButton.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false, 1, 1));
		storeButton.setText(DefaultLangResources.getI18NStringOrDefault("save"));
		storeButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				applyPendingChanges();
				setReturnValue(true);
				getParent().close();
			}
		});

		final Button createLinkButton = new Button(buttonArea, SWT.PUSH);
		createLinkButton.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false, 1, 1));
		createLinkButton.setText(DefaultLangResources.getI18NStringOrDefault("createDesktopLink"));
		createLinkButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				try {
					String applicationJarFileName = SystemUtilities.getCurrentlyRunningJarFilePath();
					if (Utilities.isBlank(applicationJarFileName)) {
						final FileDialog dialog = new FileDialog(getParent(), SWT.OPEN);
						dialog.setText(DefaultLangResources.getI18NStringOrDefault("createDesktopLink"));
						applicationJarFileName = dialog.open();
					}

					if (Utilities.isBlank(applicationJarFileName)) {
						return;
					}

					final String javaHome = System.getProperty("java.home");

					if (SystemUtilities.isWindowsSystem()) {
						final String desktopLinkFilePath = System.getProperty("user.home") + File.separator + "Desktop" + File.separator + applicationName + ".lnk";
						if (new File(desktopLinkFilePath).exists()) {
							new File(desktopLinkFilePath).delete();
						}

						final String userHomeApplicationDirectory = System.getProperty("user.home") + File.separator + "." + applicationName;
						final String iconPath = userHomeApplicationDirectory + File.separator + applicationName + ".ico";
						if (!new File(userHomeApplicationDirectory).exists()) {
							new File(userHomeApplicationDirectory).mkdir();
						}
						if (new File(iconPath).exists()) {
							new File(iconPath).delete();
						}

						final String tempScriptFile = System.getProperty("java.io.tmpdir") + File.separator + applicationName + "_createlink.vbs";
						if (new File(tempScriptFile).exists()) {
							new File(tempScriptFile).delete();
						}

						if (iconImageDataWindows != null) {
							FileUtilities.write(new File(iconPath), iconImageDataWindows);
						}

						final StringBuilder data = new StringBuilder();
						data.append("set shortcut = CreateObject(\"WScript.Shell\").CreateShortcut(\"" + desktopLinkFilePath + "\")\n");
						data.append("shortcut.Description = \"" + applicationName + "\"\n");
						if (Utilities.isNotBlank(javaHome)) {
							data.append("shortcut.TargetPath = \"" + javaHome + "\\bin\\javaw.exe\"\n");
							data.append("shortcut.Arguments = \"-jar \"\"" + applicationJarFileName + "\"\"\"\n");
						} else {
							data.append("shortcut.TargetPath = \"" + applicationJarFileName + "\"\n");
						}
						data.append("shortcut.IconLocation = \"" + iconPath + "\"\n");
						data.append("shortcut.Save\n");

						try {
							FileUtilities.writeStringToFile(new File(tempScriptFile), data.toString(), StandardCharsets.UTF_8);

							Runtime.getRuntime().exec(new String[]{ "wscript", "\"" + tempScriptFile + "\"" }, null, null);

							final MessageBox messageBox = new MessageBox(getParent(), SWT.OK);
							messageBox.setText(DefaultLangResources.getI18NStringOrDefault("createDesktopLink"));
							messageBox.setMessage(DefaultLangResources.getI18NStringOrDefault("desktopLinkWasCreatedAt", desktopLinkFilePath));
							messageBox.open();

							new File(tempScriptFile).delete();
						} catch (final IOException e1) {
							final MessageBox messageBox = new MessageBox(getParent(), SWT.ICON_ERROR | SWT.OK);
							messageBox.setText(DefaultLangResources.getI18NStringOrDefault("createDesktopLink"));
							messageBox.setMessage(DefaultLangResources.getI18NStringOrDefault("errorMessage", e1.getMessage()));
							messageBox.open();
						}
					} else if (SystemUtilities.isLinuxSystem()) {
						String desktopLinkFilePath;
						String iconPath;
						final File localShareApplicationsDirectory = new File(System.getProperty("user.home") + "/.local/share/applications");
						if (localShareApplicationsDirectory.exists()) {
							desktopLinkFilePath = localShareApplicationsDirectory.getAbsolutePath() + File.separator + applicationName + ".desktop";
							iconPath = localShareApplicationsDirectory.getAbsolutePath() + File.separator + applicationName + ".png";
						} else {
							desktopLinkFilePath = System.getProperty("user.home") + File.separator + "Desktop" + File.separator + applicationName + ".desktop";
							iconPath = System.getProperty("user.home") + File.separator + applicationName + ".png";
						}
						if (new File(desktopLinkFilePath).exists()) {
							new File(desktopLinkFilePath).delete();
						}
						if (new File(iconPath).exists()) {
							new File(iconPath).delete();
						}

						final StringBuilder data = new StringBuilder();
						data.append("[Desktop Entry]\n");
						data.append("Encoding=UTF-8\n");
						data.append("Name=" + applicationName + "\n");

						if (iconImageLinux != null) {
							final ImageLoader loader = new ImageLoader();
							loader.data = new ImageData[] { iconImageLinux.getImageData() };
							loader.save(iconPath, SWT.IMAGE_PNG);

							data.append("Icon=" + iconPath + "\n");
						}

						// %U-parameter allows drag and drop of files on starter bar in unity
						// Set environment parameter UBUNTU_MENUPROXY to keep menubar docked to application window in unity
						data.append("Exec=java -jar " + applicationJarFileName + " %U\n");
						data.append("Terminal=false\n");
						data.append("Type=Application\n");

						if (startUpClassName != null) {
							// StartupWMClass must be used for Display-Class init by the main application, so the Launcher can group all windows of the same application
							data.append("StartupWMClass=" + startUpClassName + "\n");
						}

						try {
							FileUtilities.writeStringToFile(new File(desktopLinkFilePath), data.toString(), StandardCharsets.UTF_8);

							new File(desktopLinkFilePath).setExecutable(true);

							final MessageBox messageBox = new MessageBox(getParent(), SWT.OK);
							messageBox.setText(DefaultLangResources.getI18NStringOrDefault("createDesktopLink"));
							messageBox.setMessage(DefaultLangResources.getI18NStringOrDefault("desktopLinkWasCreatedAt", desktopLinkFilePath));
							messageBox.open();
						} catch (final IOException e1) {
							final MessageBox messageBox = new MessageBox(getParent(), SWT.ICON_ERROR | SWT.OK);
							messageBox.setText(DefaultLangResources.getI18NStringOrDefault("createDesktopLink"));
							messageBox.setMessage(DefaultLangResources.getI18NStringOrDefault("errorMessage", e1.getMessage()));
							messageBox.open();
						}
					}
				} catch (final Exception e1) {
					showErrorMessage(DefaultLangResources.getI18NStringOrDefault("createDesktopLink"), DefaultLangResources.getI18NStringOrDefault("errorMessage", e1.getMessage()));
				}
			}
		});

		final Button showSystemPropertiesButton = new Button(buttonArea, SWT.PUSH);
		showSystemPropertiesButton.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false, 1, 1));
		showSystemPropertiesButton.setText(DefaultLangResources.getI18NStringOrDefault("showSystemProperties"));
		showSystemPropertiesButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				final Map<String, String> properties = Utilities.getSystemPropertiesMap();
				for (final Entry<String, String> entry : properties.entrySet()) {
					properties.put(entry.getKey(), entry.getValue().replace("\n", "\\n").replace("\t", "\\t").replace("\r", "\\r"));
				}
				new ShowDataDialog(getParent(), applicationName + " " + DefaultLangResources.getI18NStringOrDefault("systemProperties"), Utilities.getStringFromMap(properties, "\n", " = ", true), true).open();
			}
		});

		final Button showRuntimePropertiesButton = new Button(buttonArea, SWT.PUSH);
		showRuntimePropertiesButton.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false, 1, 1));
		showRuntimePropertiesButton.setText(DefaultLangResources.getI18NStringOrDefault("showRuntimeProperties"));
		showRuntimePropertiesButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				final Map<String, String> properties = SystemUtilities.getRuntimeProperties(true);
				for (final Entry<String, String> entry : properties.entrySet()) {
					properties.put(entry.getKey(), entry.getValue().replace("\n", "\\n").replace("\t", "\\t").replace("\r", "\\r"));
				}
				new ShowDataDialog(getParent(), applicationName + " " + DefaultLangResources.getI18NStringOrDefault("runtimeProperties"), Utilities.getStringFromMap(properties, "\n", " = ", true), true).open();
			}
		});

		final Button showSwtPropertiesButton = new Button(buttonArea, SWT.PUSH);
		showSwtPropertiesButton.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false, 1, 1));
		showSwtPropertiesButton.setText(DefaultLangResources.getI18NStringOrDefault("showSwtProperties"));
		showSwtPropertiesButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				new ShowDataDialog(getParent(), applicationName + " " + DefaultLangResources.getI18NStringOrDefault("swtProperties"), Utilities.getStringFromMap(SwtUtilities.getSwtHandlesStatus(getParent().getDisplay()), "\n", " = ", true))
				.open();
			}
		});

		final Button cancelButton = new Button(buttonArea, SWT.PUSH);
		cancelButton.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false, 1, 1));
		cancelButton.setText(DefaultLangResources.getI18NStringOrDefault("cancel"));
		cancelButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				if (configurationChanged) {
					final MessageBox messageBox = new MessageBox(getParent(), SWT.ICON_QUESTION | SWT.YES | SWT.NO);
					messageBox.setText(applicationName + " " + DefaultLangResources.getI18NStringOrDefault("configuration"));
					messageBox.setMessage(DefaultLangResources.getI18NStringOrDefault("discardUnsavedConfigurationChanges"));
					if (messageBox.open() == SWT.YES) {
						setReturnValue(false);
						getParent().close();
					}
				} else {
					setReturnValue(false);
					getParent().close();
				}
			}
		});

		buttonArea.pack();
	}

	private class SpinnerValueChangeListener implements ModifyListener {
		private final String key;

		private SpinnerValueChangeListener(final String key) {
			this.key = key;
		}

		@Override
		public void modifyText(final ModifyEvent event) {
			pendingChanges.put(key, Integer.toString(((Spinner) event.widget).getSelection()));
			configurationChanged = true;
		}
	}

	private class TextValueChangeListener implements ModifyListener {
		private final String key;

		private TextValueChangeListener(final String key) {
			this.key = key;
		}

		@Override
		public void modifyText(final ModifyEvent event) {
			pendingChanges.put(key, ((Text) event.widget).getText());
			configurationChanged = true;
		}
	}

	private class CheckboxValueChangeListener implements SelectionListener {
		private final String key;

		private CheckboxValueChangeListener(final String key) {
			this.key = key;
		}

		@Override
		public void widgetDefaultSelected(final SelectionEvent event) {
			pendingChanges.put(key, ((Button) event.widget).getSelection());
			configurationChanged = true;
		}

		@Override
		public void widgetSelected(final SelectionEvent event) {
			pendingChanges.put(key, ((Button) event.widget).getSelection());
			configurationChanged = true;
		}
	}

	/**
	 * Drop down for a value with a fixed set of options. The options are shown
	 * with a display text (see {@link #toDisplayValue(String, String)}), but
	 * stored with their original value.
	 */
	private DropDown createDropDown(final Composite parent, final String key, final List<String> options, final String value) {
		final DropDown dropDown = new DropDown(parent, SWT.NONE);
		dropDown.setAllowCustomValues(false);
		fillDropDown(key, dropDown, options, value);

		final String[] lastValueHolder = new String[] { value };
		dropDown.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				// getText() returns null while the entered text matches no option
				final String displayValue = dropDown.getText();
				if (displayValue != null) {
					final String newValue = fromDisplayValue(key, displayValue);
					pendingChanges.put(key, newValue);
					configurationChanged = true;

					final boolean valueUnchanged = lastValueHolder[0] == null ? newValue == null : lastValueHolder[0].equals(newValue);
					if (!valueUnchanged) {
						lastValueHolder[0] = newValue;
						if (ConfigurationProperties.CONFIG_KEY_LANGUAGE.equalsIgnoreCase(key)) {
							final MessageBox messageBox = new MessageBox(getParent(), SWT.ICON_INFORMATION | SWT.OK);
							messageBox.setText(applicationName + " " + DefaultLangResources.getI18NStringOrDefault("configuration"));
							messageBox.setMessage(DefaultLangResources.getI18NStringOrDefault("languageChangeRequiresRestart"));
							messageBox.open();
						}
					}
				}
			}
		});

		return dropDown;
	}

	private void fillDropDown(final String key, final DropDown dropDown, final List<String> options, final String value) {
		final Map<String, String> displayValues = new LinkedHashMap<>();
		for (final String option : options) {
			displayValues.put(option, toDisplayValue(key, option));
		}

		String selectedValue = null;
		if (Utilities.isBlank(value)) {
			if (!options.isEmpty()) {
				selectedValue = options.get(0);
			}
		} else {
			for (final String option : options) {
				if (option.equalsIgnoreCase(value)) {
					selectedValue = option;
					break;
				}
			}
			if (selectedValue == null) {
				// Keep a configured value that is not part of the options visible (and unchanged)
				selectedValue = value;
				displayValues.put(value, toDisplayValue(key, value));
			}
		}

		dropDownDisplayValues.put(key, displayValues);
		dropDown.setItems(new ArrayList<>(displayValues.values()));
		if (selectedValue != null) {
			dropDown.setText(displayValues.get(selectedValue));
		}
	}

	/**
	 * Text shown in a drop down for a stored value: "None" in the user's
	 * language, language codes with their name (e.g. "Deutsch (de)") if a
	 * language resource exists for the code, otherwise the value itself.
	 */
	private static String toDisplayValue(final String key, final String value) {
		if ("None".equalsIgnoreCase(value)) {
			return DefaultLangResources.getI18NStringOrDefault("None");
		} else if (ConfigurationProperties.CONFIG_KEY_LANGUAGE.equalsIgnoreCase(key) && Utilities.isNotBlank(value) && LangResources.existsKey(value) && Utilities.isNotBlank(LangResources.get(value))) {
			return LangResources.get(value) + " (" + value + ")";
		} else {
			return value;
		}
	}

	/**
	 * Stored value for a text shown in a drop down (reverse of
	 * {@link #toDisplayValue(String, String)}).
	 */
	private String fromDisplayValue(final String key, final String displayValue) {
		final Map<String, String> displayValues = dropDownDisplayValues.get(key);
		if (displayValues != null) {
			for (final Entry<String, String> entry : displayValues.entrySet()) {
				if (entry.getValue().equals(displayValue)) {
					return entry.getKey();
				}
			}
		}
		return displayValue != null && displayValue.equals(DefaultLangResources.getI18NStringOrDefault("None")) ? "None" : displayValue;
	}

	public List<String> getAvailableLanguages() {
		return availableLanguages;
	}

	/**
	 * Languages offered for selection, if the configuration itself contains no
	 * enum definition for the language key. Must be set before the dialog is
	 * opened.
	 */
	public void setAvailableLanguages(final List<String> availableLanguages) {
		this.availableLanguages = availableLanguages;
	}

	public ApplicationConfigurationDialog withAvailableLanguages(final List<String> availableLanguagesParam) {
		setAvailableLanguages(availableLanguagesParam);
		return this;
	}

	private void applyPendingChanges() {
		for (final Entry<String, Object> entry : pendingChanges.entrySet()) {
			final Object value = entry.getValue();
			if (value instanceof Boolean) {
				applicationConfiguration.set(entry.getKey(), (Boolean) value);
			} else {
				applicationConfiguration.set(entry.getKey(), (String) value);
			}
		}
	}

	@Override
	protected Boolean getDefaultReturnValue() {
		return false;
	}
}
