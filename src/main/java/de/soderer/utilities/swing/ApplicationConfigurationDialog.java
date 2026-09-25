package de.soderer.utilities.swing;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ItemEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import de.soderer.pac.utilities.ProxyConfiguration;
import de.soderer.pac.utilities.ProxyConfiguration.ProxyConfigurationType;
import de.soderer.utilities.ConfigurationProperties;
import de.soderer.utilities.DateUtilities;
import de.soderer.utilities.DefaultLangResources;
import de.soderer.utilities.FileUtilities;
import de.soderer.utilities.LangResources;
import de.soderer.utilities.Result;
import de.soderer.utilities.SystemUtilities;
import de.soderer.utilities.Utilities;
import de.soderer.utilities.Version;
import de.soderer.utilities.appupdate.ApplicationUpdateUtilities;

/**
 * Configuration dialog of an application.
 *
 * <p>
 * Shows every configuration value of the application (like the SWT variant):
 * values with a "maxsize" companion key get a spinner plus text field, values
 * with an "enum" companion key get a combo box, "true"/"false" values get a
 * checkbox and everything else a text field. Values with a "conf.KEY.help"
 * language resource get a help button.
 * </p>
 *
 * <p>
 * Changes are collected and only written to the configuration when the user
 * saves. {@link #open()} returns {@link Result#OK} after saving, otherwise
 * {@link Result#CANCELED}.
 * </p>
 */
public class ApplicationConfigurationDialog extends ModalDialog<Result> {
	private static final long serialVersionUID = 422519084107817691L;

	private static final List<String> DEFAULT_PROXY_CONFIGURATION_TYPES = Utilities.getList("None", "System", "Proxy-URL", "WPAD", "PAC-URL");

	private static final int MAX_INITIAL_HEIGHT = 600;

	private final UpdateableGuiApplication updateParent;
	private final String applicationName;
	private final String applicationClassName;
	private final Version applicationVersion;
	private final LocalDateTime applicationBuildTime;
	private final ConfigurationProperties applicationConfiguration;
	private final byte[] iconImageDataWindows;
	private final Image iconImageLinux;
	private final String versioninfoDownloadURL;
	private final String trustedUpdateCaCertificates;
	private final List<String> jvmParameters;
	private final List<String> appParameters;
	private List<String> availableLanguages = Utilities.getList("de", "en");

	/** Changed values not yet written to the configuration (String or Boolean) */
	private final Map<String, Object> pendingChanges = new LinkedHashMap<>();

	/** Tracks whether any configuration field was modified by the user since the dialog was opened */
	private boolean configurationChanged = false;

	/** Suppresses change tracking and info messages while values are set programmatically */
	private boolean technicalChange = false;

	private final Map<String, JComponent> valueComponents = new HashMap<>();

	/** Language combo filled from "availableLanguages" (null if the language has an enum definition in the configuration) */
	private JComboBox<String> availableLanguagesCombo;

	private String lastLanguageValue;

	public ApplicationConfigurationDialog(final UpdateableGuiApplication updateParent, final String applicationName, final String applicationClassName, final Version applicationVersion, final LocalDateTime applicationBuildTime, final ConfigurationProperties applicationConfiguration, final byte[] iconImageDataWindows, final Image iconImageLinux, final String versioninfoDownloadURL, final String trustedUpdateCaCertificates, final List<String> jvmParameters, final List<String> appParameters) {
		super(updateParent, applicationName + " (Version " + applicationVersion.toString() + ") " + LangResources.get("configuration"));

		this.updateParent = updateParent;
		this.applicationName = applicationName;
		this.applicationClassName = applicationClassName;
		this.applicationVersion = applicationVersion;
		this.applicationBuildTime = applicationBuildTime;
		this.applicationConfiguration = applicationConfiguration;
		this.iconImageDataWindows = iconImageDataWindows;
		this.iconImageLinux = iconImageLinux;
		this.versioninfoDownloadURL = versioninfoDownloadURL;
		this.trustedUpdateCaCertificates = trustedUpdateCaCertificates;
		this.jvmParameters = jvmParameters;
		this.appParameters = appParameters;

		lastLanguageValue = applicationConfiguration.get(ConfigurationProperties.CONFIG_KEY_LANGUAGE);

		returnValue = Result.CANCELED;

		if (iconImageLinux != null) {
			setIconImage(iconImageLinux);
		}

		final int margin = 5;

		final JPanel mainPanel = new JPanel(new BorderLayout(margin, margin));
		mainPanel.setBorder(BorderFactory.createEmptyBorder(margin, margin, margin, margin));

		final JScrollPane valuesScrollPane = new JScrollPane(createConfigurationValuesPanel());
		valuesScrollPane.setBorder(BorderFactory.createEmptyBorder());
		valuesScrollPane.getVerticalScrollBar().setUnitIncrement(16);
		mainPanel.add(valuesScrollPane, BorderLayout.CENTER);

		mainPanel.add(createButtonPanel(), BorderLayout.SOUTH);

		setContentPane(mainPanel);

		// Closing via the window decoration behaves like the cancel button (asks for unsaved changes)
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(final WindowEvent event) {
				cancel();
			}
		});

		setResizable(true);
		pack();
		setMinimumSize(new Dimension(500, 300));
		setSize(Math.max(500, getWidth()), Math.max(300, Math.min(getHeight(), MAX_INITIAL_HEIGHT)));
		setLocationRelativeTo(updateParent);
	}

	/**
	 * The list of all configuration values: one row per key, labels left
	 * aligned in the first column (like the SWT variant).
	 */
	private JPanel createConfigurationValuesPanel() {
		final JPanel valuesPanel = new JPanel(new GridBagLayout());

		int row = 0;

		if (applicationBuildTime != null) {
			addLabel(valuesPanel, row, LangResources.get("buildTime"));
			final JTextField buildTimeText = new JTextField(DateUtilities.formatDate(DateUtilities.YYYY_MM_DD_HHMMSS, applicationBuildTime));
			buildTimeText.setEditable(false);
			valuesPanel.add(buildTimeText, createValueConstraints(row, 1, 2));
			row++;
		}

		technicalChange = true;
		try {
			for (final String key : Utilities.asSortedList(applicationConfiguration.getKeyList())) {
				if (key.endsWith(ConfigurationProperties.MAXSIZE_EXTENSION) || key.endsWith(ConfigurationProperties.ENUM_EXTENSION) || ConfigurationProperties.isInternalKey(key)) {
					continue;
				}

				String description = key;
				if (LangResources.existsKey("conf." + key) && Utilities.isNotBlank(LangResources.get("conf." + key))) {
					description = LangResources.get("conf." + key);
				}
				addLabel(valuesPanel, row, description);

				final String value = applicationConfiguration.get(key);
				if (applicationConfiguration.containsKey(key + ConfigurationProperties.MAXSIZE_EXTENSION)) {
					final String maxSizeKey = key + ConfigurationProperties.MAXSIZE_EXTENSION;
					final int maxSize = Math.max(1, Math.min(50, applicationConfiguration.getInteger(maxSizeKey)));
					final JSpinner maxSizeSpinner = new JSpinner(new SpinnerNumberModel(maxSize, 1, 50, 1));
					((JSpinner.DefaultEditor) maxSizeSpinner.getEditor()).getTextField().setColumns(3);
					maxSizeSpinner.addChangeListener(event -> registerChange(maxSizeKey, maxSizeSpinner.getValue().toString()));
					valuesPanel.add(maxSizeSpinner, createValueConstraints(row, 1, 1));

					final JTextField valueTextField = createTextField(key, value);
					valuesPanel.add(valueTextField, createValueConstraints(row, 2, 1));
				} else if (applicationConfiguration.containsKey(key + ConfigurationProperties.ENUM_EXTENSION)) {
					valuesPanel.add(createComboBox(key, Utilities.splitAndTrimList(applicationConfiguration.get(key + ConfigurationProperties.ENUM_EXTENSION)), value), createValueConstraints(row, 1, 2));
				} else if (ConfigurationProperties.CONFIG_KEY_LANGUAGE.equalsIgnoreCase(key)) {
					// No enum definition in the configuration: offer the languages known to the application
					availableLanguagesCombo = createComboBox(key, availableLanguages, value);
					valuesPanel.add(availableLanguagesCombo, createValueConstraints(row, 1, 2));
				} else if (ConfigurationProperties.CONFIG_KEY_PROXY_CONFIGURATION_TYPE.equalsIgnoreCase(key)) {
					valuesPanel.add(createComboBox(key, DEFAULT_PROXY_CONFIGURATION_TYPES, value), createValueConstraints(row, 1, 2));
				} else if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
					final JCheckBox checkBox = new JCheckBox();
					checkBox.setSelected(Utilities.interpretAsBool(value));
					checkBox.addItemListener(event -> registerChange(key, checkBox.isSelected()));
					valueComponents.put(key, checkBox);
					valuesPanel.add(checkBox, createValueConstraints(row, 1, 2));
				} else {
					valuesPanel.add(createTextField(key, value), createValueConstraints(row, 1, 2));
				}

				if (LangResources.existsKey("conf." + key + ".help") && Utilities.isNotBlank(LangResources.get("conf." + key + ".help"))) {
					valuesPanel.add(createHelpButton(LangResources.get("conf." + key + ".help")), createHelpButtonConstraints(row));
				}

				row++;
			}
		} finally {
			technicalChange = false;
		}

		updateProxyUrlEnabledStatus();

		// Filler below the last row, keeps all rows at the top
		final GridBagConstraints fillerConstraints = new GridBagConstraints();
		fillerConstraints.gridx = 0;
		fillerConstraints.gridy = row;
		fillerConstraints.gridwidth = 4;
		fillerConstraints.weighty = 1;
		fillerConstraints.fill = GridBagConstraints.BOTH;
		valuesPanel.add(new JPanel(), fillerConstraints);

		return valuesPanel;
	}

	private static void addLabel(final JPanel valuesPanel, final int row, final String text) {
		final GridBagConstraints labelConstraints = new GridBagConstraints();
		labelConstraints.gridx = 0;
		labelConstraints.gridy = row;
		labelConstraints.anchor = GridBagConstraints.WEST;
		labelConstraints.insets = new Insets(2, 2, 2, 8);
		valuesPanel.add(new JLabel(text), labelConstraints);
	}

	private static GridBagConstraints createValueConstraints(final int row, final int column, final int columnSpan) {
		final GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = column;
		constraints.gridy = row;
		constraints.gridwidth = columnSpan;
		constraints.anchor = GridBagConstraints.WEST;
		constraints.insets = new Insets(2, 0, 2, 4);
		// Only the value column (column 2) takes the extra width, so labels and spinners stay compact on the left
		if (column + columnSpan > 2) {
			constraints.weightx = 1;
			constraints.fill = GridBagConstraints.HORIZONTAL;
		}
		return constraints;
	}

	private static GridBagConstraints createHelpButtonConstraints(final int row) {
		final GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = 3;
		constraints.gridy = row;
		constraints.anchor = GridBagConstraints.WEST;
		constraints.insets = new Insets(2, 0, 2, 2);
		return constraints;
	}

	private JTextField createTextField(final String key, final String value) {
		final JTextField textField = new JTextField(value == null ? "" : value, 20);
		textField.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(final DocumentEvent event) {
				registerChange(key, textField.getText());
			}

			@Override
			public void removeUpdate(final DocumentEvent event) {
				registerChange(key, textField.getText());
			}

			@Override
			public void changedUpdate(final DocumentEvent event) {
				// Attribute changes only, no text change
			}
		});
		valueComponents.put(key, textField);
		return textField;
	}

	/**
	 * Combo box for a value with a fixed set of options. "None" is shown in the
	 * user's language but stored as "None".
	 */
	private JComboBox<String> createComboBox(final String key, final List<String> options, final String value) {
		final JComboBox<String> comboBox = new JComboBox<>();
		fillComboBox(comboBox, options, value);
		comboBox.addItemListener(event -> {
			if (event.getStateChange() == ItemEvent.SELECTED) {
				comboValueSelected(key, fromDisplayValue((String) event.getItem()));
			}
		});
		valueComponents.put(key, comboBox);
		return comboBox;
	}

	private static void fillComboBox(final JComboBox<String> comboBox, final List<String> options, final String value) {
		comboBox.removeAllItems();
		for (final String option : options) {
			comboBox.addItem(toDisplayValue(option));
		}
		if (Utilities.isBlank(value)) {
			if (comboBox.getItemCount() > 0) {
				comboBox.setSelectedIndex(0);
			}
		} else {
			final String displayValue = toDisplayValue(value);
			boolean found = false;
			for (int i = 0; i < comboBox.getItemCount(); i++) {
				if (comboBox.getItemAt(i).equalsIgnoreCase(displayValue)) {
					comboBox.setSelectedIndex(i);
					found = true;
					break;
				}
			}
			if (!found) {
				// Keep a configured value that is not part of the options visible (and unchanged)
				comboBox.addItem(displayValue);
				comboBox.setSelectedItem(displayValue);
			}
		}
	}

	private void comboValueSelected(final String key, final String newValue) {
		registerChange(key, newValue);

		if (technicalChange) {
			return;
		}

		if (ConfigurationProperties.CONFIG_KEY_LANGUAGE.equalsIgnoreCase(key)) {
			final boolean valueUnchanged = lastLanguageValue == null ? newValue == null : lastLanguageValue.equals(newValue);
			if (!valueUnchanged) {
				lastLanguageValue = newValue;
				JOptionPane.showMessageDialog(this, LangResources.get("languageChangeRequiresRestart"), applicationName + " " + LangResources.get("configuration"), JOptionPane.INFORMATION_MESSAGE);
			}
		} else if (ConfigurationProperties.CONFIG_KEY_PROXY_CONFIGURATION_TYPE.equalsIgnoreCase(key)) {
			updateProxyUrlEnabledStatus();
		}
	}

	/**
	 * The proxy URL is only relevant for the proxy types that use it.
	 */
	private void updateProxyUrlEnabledStatus() {
		final JComponent proxyUrlComponent = valueComponents.get(ConfigurationProperties.CONFIG_KEY_PROXY_URL);
		if (proxyUrlComponent != null && valueComponents.containsKey(ConfigurationProperties.CONFIG_KEY_PROXY_CONFIGURATION_TYPE)) {
			final String proxyType = getProxyConfigurationType() == null ? "" : getProxyConfigurationType().replaceAll("[^A-Za-z]", "").toLowerCase();
			proxyUrlComponent.setEnabled(!"none".equals(proxyType) && !"system".equals(proxyType) && !"wpad".equals(proxyType));
		}
	}

	private static String toDisplayValue(final String value) {
		return "None".equalsIgnoreCase(value) ? LangResources.get("None") : value;
	}

	private static String fromDisplayValue(final String displayValue) {
		return displayValue != null && displayValue.equals(LangResources.get("None")) ? "None" : displayValue;
	}

	private JButton createHelpButton(final String helpText) {
		final JButton helpButton = new JButton();
		try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("images/icons/question.png")) {
			if (inputStream != null) {
				helpButton.setIcon(new ImageIcon(inputStream.readAllBytes()));
			} else {
				helpButton.setText("?");
			}
		} catch (@SuppressWarnings("unused") final IOException e) {
			helpButton.setText("?");
		}
		helpButton.setMargin(new Insets(1, 1, 1, 1));
		helpButton.setToolTipText(helpText);
		helpButton.addActionListener(e -> new QuestionDialog(this, applicationName, helpText, LangResources.get("ok")).open());
		return helpButton;
	}

	private void registerChange(final String key, final Object value) {
		pendingChanges.put(key, value);
		if (!technicalChange) {
			configurationChanged = true;
		}
	}

	private JPanel createButtonPanel() {
		final JPanel buttonPanel = new JPanel(new GridBagLayout());

		final List<JButton> buttons = new ArrayList<>();

		final JButton saveButton = new JButton(LangResources.get("save"));
		saveButton.addActionListener(e -> {
			applyPendingChanges();
			returnValue = Result.OK;
			dispose();
		});
		buttons.add(saveButton);

		final JButton createDesktopLinkButton = new JButton(LangResources.get("createDesktopLink"));
		createDesktopLinkButton.addActionListener(e -> createDesktopLink());
		buttons.add(createDesktopLinkButton);

		final JButton showSystemPropertiesButton = new JButton(LangResources.get("showSystemProperties"));
		showSystemPropertiesButton.addActionListener(e -> showProperties(LangResources.get("systemProperties"), Utilities.getSystemPropertiesMap()));
		buttons.add(showSystemPropertiesButton);

		final JButton showRuntimePropertiesButton = new JButton(LangResources.get("showRuntimeProperties"));
		showRuntimePropertiesButton.addActionListener(e -> showProperties(LangResources.get("runtimeProperties"), SystemUtilities.getRuntimeProperties(true)));
		buttons.add(showRuntimePropertiesButton);

		// Swing counterpart of the SWT variant's "SWT properties"
		final JButton showSwingPropertiesButton = new JButton(DefaultLangResources.getI18NStringOrDefault("showSwingProperties"));
		showSwingPropertiesButton.addActionListener(e -> showProperties(DefaultLangResources.getI18NStringOrDefault("swingProperties"), getSwingProperties()));
		buttons.add(showSwingPropertiesButton);

		if (Utilities.isNotBlank(versioninfoDownloadURL)) {
			final JButton updateButton = new JButton(LangResources.get("checkUpdate"));
			updateButton.addActionListener(e -> checkUpdate());
			buttons.add(updateButton);
		}

		final JButton cancelButton = new JButton(LangResources.get("cancel"));
		cancelButton.addActionListener(e -> cancel());
		buttons.add(cancelButton);

		// Two equally wide columns; with an odd number of buttons the last one spans both columns
		for (int i = 0; i < buttons.size(); i++) {
			final GridBagConstraints constraints = new GridBagConstraints();
			constraints.gridx = i % 2;
			constraints.gridy = i / 2;
			constraints.weightx = 1;
			constraints.fill = GridBagConstraints.HORIZONTAL;
			constraints.insets = new Insets(2, 2, 2, 2);
			if (i == buttons.size() - 1 && i % 2 == 0) {
				constraints.gridwidth = 2;
			}
			buttonPanel.add(buttons.get(i), constraints);
		}

		getRootPane().setDefaultButton(saveButton);

		return buttonPanel;
	}

	private void cancel() {
		if (configurationChanged) {
			final int result = JOptionPane.showConfirmDialog(this, LangResources.get("discardUnsavedConfigurationChanges"), applicationName + " " + LangResources.get("configuration"), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
			if (result != JOptionPane.YES_OPTION) {
				// Keep the dialog open, discard nothing
				return;
			}
		}
		returnValue = Result.CANCELED;
		dispose();
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

	private void showProperties(final String title, final Map<String, String> properties) {
		final Map<String, String> displayProperties = new LinkedHashMap<>();
		for (final Entry<String, String> entry : properties.entrySet()) {
			final String value = entry.getValue() == null ? "" : entry.getValue();
			displayProperties.put(entry.getKey(), value.replace("\n", "\\n").replace("\t", "\\t").replace("\r", "\\r"));
		}
		new ShowDataDialog(this, applicationName + " " + title, Utilities.getStringFromMap(displayProperties, "\n", " = ", true)).withResizable(true).open();
	}

	/**
	 * Look and feel, toolkit and screen information, useful for analyzing
	 * display problems (e.g. HiDPI scaling on Linux).
	 */
	private static Map<String, String> getSwingProperties() {
		final Map<String, String> properties = new LinkedHashMap<>();
		properties.put("LookAndFeel", UIManager.getLookAndFeel() == null ? "" : UIManager.getLookAndFeel().getName() + " (" + UIManager.getLookAndFeel().getClass().getName() + ")");
		properties.put("SystemLookAndFeel", UIManager.getSystemLookAndFeelClassName());
		properties.put("Toolkit", Toolkit.getDefaultToolkit().getClass().getName());
		try {
			properties.put("ScreenResolution (dpi)", Integer.toString(Toolkit.getDefaultToolkit().getScreenResolution()));
		} catch (final Exception e) {
			properties.put("ScreenResolution (dpi)", e.getClass().getSimpleName());
		}
		properties.put("Headless", Boolean.toString(GraphicsEnvironment.isHeadless()));
		if (!GraphicsEnvironment.isHeadless()) {
			final GraphicsDevice[] screens = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
			properties.put("Screens", Integer.toString(screens.length));
			for (int i = 0; i < screens.length; i++) {
				final GraphicsConfiguration configuration = screens[i].getDefaultConfiguration();
				properties.put("Screen " + (i + 1), configuration.getBounds().width + "x" + configuration.getBounds().height
						+ " at " + configuration.getBounds().x + "," + configuration.getBounds().y
						+ ", scale " + configuration.getDefaultTransform().getScaleX() + "x" + configuration.getDefaultTransform().getScaleY());
			}
		}
		return properties;
	}

	private void checkUpdate() {
		try {
			final String proxyConfigurationType = getProxyConfigurationType();
			ApplicationUpdateUtilities.executeUpdate(updateParent, versioninfoDownloadURL, new ProxyConfiguration(ProxyConfigurationType.getFromString(proxyConfigurationType), getProxyUrl()), applicationName, applicationVersion, trustedUpdateCaCertificates, null, null, jvmParameters, appParameters, true, false);
		} catch (final Exception e) {
			new QuestionDialog(this, applicationName + " ERROR", "ERROR:\n" + e.getMessage()).setBackgroundColor(SwingColor.LightRed).open();
		}
	}

	private void createDesktopLink() {
		try {
			String applicationJarFileName = SystemUtilities.getCurrentlyRunningJarFilePath();
			if (Utilities.isBlank(applicationJarFileName)) {
				applicationJarFileName = openJarFilePathDialog();
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

					Runtime.getRuntime().exec(new String[] { "wscript", "\"" + tempScriptFile + "\"" }, null, null);

					JOptionPane.showMessageDialog(this, LangResources.get("desktopLinkWasCreatedAt", desktopLinkFilePath), LangResources.get("createDesktopLink"), JOptionPane.PLAIN_MESSAGE);

					new File(tempScriptFile).delete();
				} catch (final IOException e) {
					JOptionPane.showMessageDialog(this, LangResources.get("errorMessage", e.getMessage()), LangResources.get("createDesktopLink"), JOptionPane.ERROR_MESSAGE);
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
					ImageIO.write(toBufferedImage(iconImageLinux), "png", new File(iconPath));
					data.append("Icon=" + iconPath + "\n");
				}

				// %U-parameter allows drag and drop of files on starter bar in unity
				// JVM option "--add-opens=java.desktop/sun.awt.X11=ALL-UNNAMED" allows the application to be pinned in the Linux Gnome applications dock.
				// JVM options must be placed before "-jar", everything after the jar file path is passed to the application.
				data.append("Exec=java --add-opens=java.desktop/sun.awt.X11=ALL-UNNAMED -jar \"" + applicationJarFileName + "\" %U\n");
				data.append("Terminal=false\n");
				data.append("Type=Application\n");

				if (applicationClassName != null) {
					// StartupWMClass must match the WM_CLASS set by the main application, so the launcher can group all windows of the same application
					data.append("StartupWMClass=" + applicationClassName + "\n");
				}

				try {
					FileUtilities.writeStringToFile(new File(desktopLinkFilePath), data.toString(), StandardCharsets.UTF_8);

					new File(desktopLinkFilePath).setExecutable(true);

					JOptionPane.showMessageDialog(this, LangResources.get("desktopLinkWasCreatedAt", desktopLinkFilePath), LangResources.get("createDesktopLink"), JOptionPane.PLAIN_MESSAGE);
				} catch (final IOException e) {
					JOptionPane.showMessageDialog(this, LangResources.get("errorMessage", e.getMessage()), LangResources.get("createDesktopLink"), JOptionPane.ERROR_MESSAGE);
				}
			}
		} catch (final Exception e) {
			JOptionPane.showMessageDialog(this, LangResources.get("errorMessage", e.getMessage()), LangResources.get("createDesktopLink"), JOptionPane.ERROR_MESSAGE);
		}
	}

	/**
	 * ImageIO can only write RenderedImages, but e.g. ImageIcon.getImage() returns
	 * a toolkit image, so any other image type is drawn into a BufferedImage.
	 */
	private static BufferedImage toBufferedImage(final Image image) {
		if (image instanceof BufferedImage) {
			return (BufferedImage) image;
		}

		final ImageIcon loadedImage = new ImageIcon(image); // Waits until the image data is completely loaded
		final BufferedImage bufferedImage = new BufferedImage(Math.max(1, loadedImage.getIconWidth()), Math.max(1, loadedImage.getIconHeight()), BufferedImage.TYPE_INT_ARGB);
		final Graphics2D graphics = bufferedImage.createGraphics();
		try {
			graphics.drawImage(loadedImage.getImage(), 0, 0, null);
		} finally {
			graphics.dispose();
		}
		return bufferedImage;
	}

	private String openJarFilePathDialog() {
		final JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle(LangResources.get("createDesktopLink"));
		if (JFileChooser.APPROVE_OPTION == fileChooser.showOpenDialog(this)) {
			return fileChooser.getSelectedFile().getAbsolutePath();
		} else {
			return null;
		}
	}

	/**
	 * Current (possibly not yet saved) value of a configuration key.
	 */
	private String getCurrentValue(final String key) {
		if (pendingChanges.containsKey(key)) {
			final Object value = pendingChanges.get(key);
			return value == null ? null : value.toString();
		} else {
			return applicationConfiguration.get(key);
		}
	}

	/**
	 * Sets the value shown for a key, which is saved together with the user's
	 * changes.
	 */
	private void setCurrentValue(final String key, final String value) {
		technicalChange = true;
		try {
			final JComponent component = valueComponents.get(key);
			if (component instanceof JComboBox) {
				@SuppressWarnings("unchecked")
				final JComboBox<String> comboBox = (JComboBox<String>) component;
				comboBox.setSelectedItem(toDisplayValue(value));
			} else if (component instanceof JTextField) {
				((JTextField) component).setText(value == null ? "" : value);
			} else if (component instanceof JCheckBox) {
				((JCheckBox) component).setSelected(Utilities.interpretAsBool(value));
			}
			registerChange(key, value == null ? "" : value);
		} finally {
			technicalChange = false;
		}
		updateProxyUrlEnabledStatus();
	}

	public String getLanguage() {
		return getCurrentValue(ConfigurationProperties.CONFIG_KEY_LANGUAGE);
	}

	public void setLanguage(final String language) {
		lastLanguageValue = language;
		setCurrentValue(ConfigurationProperties.CONFIG_KEY_LANGUAGE, language);
	}

	public String getProxyConfigurationType() {
		return getCurrentValue(ConfigurationProperties.CONFIG_KEY_PROXY_CONFIGURATION_TYPE);
	}

	public void setProxyConfigurationType(final String proxyConfigurationType) {
		setCurrentValue(ConfigurationProperties.CONFIG_KEY_PROXY_CONFIGURATION_TYPE, proxyConfigurationType);
	}

	public String getProxyUrl() {
		return getCurrentValue(ConfigurationProperties.CONFIG_KEY_PROXY_URL);
	}

	public void setProxyUrl(final String proxyUrl) {
		setCurrentValue(ConfigurationProperties.CONFIG_KEY_PROXY_URL, proxyUrl);
	}

	public List<String> getAvailableLanguages() {
		return availableLanguages;
	}

	/**
	 * Languages offered for selection, if the configuration itself contains no
	 * enum definition for the language key.
	 */
	public ApplicationConfigurationDialog setAvailableLanguages(final List<String> availableLanguages) {
		this.availableLanguages = availableLanguages;

		if (availableLanguagesCombo != null) {
			technicalChange = true;
			try {
				fillComboBox(availableLanguagesCombo, availableLanguages, getLanguage());
			} finally {
				technicalChange = false;
			}
		}

		return this;
	}
}
