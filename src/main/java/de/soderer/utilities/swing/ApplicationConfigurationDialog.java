package de.soderer.utilities.swing;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.Window;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import de.soderer.utilities.ConfigurationProperties;
import de.soderer.utilities.LangResources;
import de.soderer.utilities.Version;

public class ApplicationConfigurationDialog extends JDialog {
	private static final long serialVersionUID = 422519084107817691L;

	private final String applicationName;
	private final ConfigurationProperties applicationConfiguration;
	private final byte[] iconImageDataWindows;
	private final Image iconImageLinux;

	/** The db type combo. */
	private final JComboBox<String> languageCombo;

	public ApplicationConfigurationDialog(final Window parent, final String applicationName, final Version applicationVersion, final ConfigurationProperties applicationConfiguration) throws Exception {
		this(parent, applicationName, applicationVersion, applicationConfiguration, null, null);
	}

	public ApplicationConfigurationDialog(final Window parent, final String applicationName, final Version applicationVersion, final ConfigurationProperties applicationConfiguration, final byte[] iconImageWindows, final Image iconImageLinux) throws Exception {
		super(parent, applicationName + " (" + applicationVersion.toString() + ")", Dialog.ModalityType.DOCUMENT_MODAL);

		//		try {
		//			new ApplicationUpdateHelper(DbImport.APPLICATION_NAME, DbImport.VERSION, DbImport.VERSIONINFO_DOWNLOAD_URL, dbImportGui, "gui", DbImport.TRUSTED_UPDATE_CA_CERTIFICATE).executeUpdate();
		//		} catch (final Exception e) {
		//			final TextDialog textDialog = new TextDialog(dbImportGui, DbImport.APPLICATION_NAME + " ERROR", "ERROR:\n" + e.getMessage(), LangResources.get("close"), false, Color.PINK);
		//			textDialog.setVisible(true);
		//		}

		this.applicationConfiguration = applicationConfiguration;
		this.applicationName = applicationName;
		iconImageDataWindows = iconImageWindows;
		this.iconImageLinux = iconImageLinux;

		if (iconImageLinux != null) {
			setIconImage(iconImageLinux);
		}

		setLocationRelativeTo(null);

		setResizable(false);

		final JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));

		add(panel);

		setLayout(new BoxLayout(getContentPane(), BoxLayout.PAGE_AXIS));

		// Parameter Panel
		final JPanel parameterPanel = new JPanel();
		parameterPanel.setLayout(new BoxLayout(parameterPanel, BoxLayout.LINE_AXIS));

		// CheckBox parameter Panel
		final JPanel mandatoryParameterPanel = new JPanel();
		mandatoryParameterPanel.setLayout(new BoxLayout(mandatoryParameterPanel, BoxLayout.PAGE_AXIS));

		// Language Panel
		final JPanel languagePanel = new JPanel();
		languagePanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
		final JLabel dbTypeLabel = new JLabel(LangResources.get("language"));
		languagePanel.add(dbTypeLabel);
		languageCombo = new JComboBox<>();
		languageCombo.setToolTipText(LangResources.get("language_help"));
		for (final String language : new String[] {"de", "en"}) {
			languageCombo.addItem(language);
		}
		languageCombo.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(final ItemEvent e) {
				//				checkButtonStatus();
			}
		});
		languagePanel.add(languageCombo, BorderLayout.EAST);

		mandatoryParameterPanel.add(languagePanel);

		panel.add(mandatoryParameterPanel);

		pack();

		setLocationRelativeTo(parent);
	}
}
