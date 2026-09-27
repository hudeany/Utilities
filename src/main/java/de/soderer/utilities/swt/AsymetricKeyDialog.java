package de.soderer.utilities.swt;

import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import de.soderer.utilities.LangResources;
import de.soderer.utilities.crypto.CryptographicUtilities;

public class AsymetricKeyDialog extends ModalDialog<Boolean> {
	private final PublicKey publicKey;
	private final PrivateKey privateKey;

	private StyledText publicKeyMd5Text;
	private StyledText publicKeySha1Text;
	private StyledText publicKeySha256Text;
	private StyledText publicKeySha384Text;
	private StyledText publicKeySha512Text;

	private StyledText privateKeyMd5Text;
	private StyledText privateKeySha1Text;
	private StyledText privateKeySha256Text;
	private StyledText privateKeySha384Text;
	private StyledText privateKeySha512Text;

	public AsymetricKeyDialog(final Shell shell, final String title, final PublicKey publicKey, final PrivateKey privateKey) {
		super(shell, SWT.RESIZE, title);
		this.publicKey = publicKey;
		this.privateKey = privateKey;
	}

	@Override
	protected void createComponents(final Shell shell) throws Exception {
		shell.setLayout(new GridLayout(1, false));

		Composite composite = new Composite(shell, SWT.NONE);
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		composite.setLayoutData(gridData);
		composite.setLayout(SwtUtilities.createNoMarginGridLayout(2, false));

		// Public key
		Label label = new Label(composite, SWT.NONE);
		label.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, true, 2, 1));
		label.setText(LangResources.get("publicKey"));

		if (publicKey != null) {
			label = new Label(composite, SWT.NONE);
			label.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, true, 1, 1));
			label.setText(LangResources.get("keyType"));

			StyledText textField = new StyledText(composite, SWT.READ_ONLY | SWT.BORDER);
			textField.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, true, 1, 1));
			textField.setText(publicKey.getAlgorithm());

			label = new Label(composite, SWT.NONE);
			label.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, true, 1, 1));
			label.setText(LangResources.get("keyFormat"));

			textField = new StyledText(composite, SWT.READ_ONLY | SWT.BORDER);
			textField.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, true, 1, 1));
			textField.setText(publicKey.getFormat());

			if (publicKey instanceof RSAPublicKey) {
				label = new Label(composite, SWT.NONE);
				label.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, true, 1, 1));
				label.setText(LangResources.get("keyStrength"));

				textField = new StyledText(composite, SWT.READ_ONLY | SWT.BORDER);
				textField.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, true, 1, 1));
				textField.setText(Integer.toString(((RSAPublicKey) publicKey).getModulus().bitLength()));

				label = new Label(composite, SWT.NONE);
				label.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, true, 1, 1));
				label.setText(LangResources.get("fingerprintMd5"));

				publicKeyMd5Text = addSizeLimitedTextField(composite, CryptographicUtilities.getOpenSshFingerPrintMd5(publicKey, ":").toUpperCase());

				label = new Label(composite, SWT.NONE);
				label.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, true, 1, 1));
				label.setText(LangResources.get("fingerprintSha1"));

				publicKeySha1Text = addSizeLimitedTextField(composite, CryptographicUtilities.getOpenSshFingerPrintSha1(publicKey, ":").toUpperCase());

				label = new Label(composite, SWT.NONE);
				label.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, true, 1, 1));
				label.setText(LangResources.get("fingerprintSha256"));

				publicKeySha256Text = addSizeLimitedTextField(composite, CryptographicUtilities.getOpenSshFingerPrintSha256Base64(publicKey));

				label = new Label(composite, SWT.NONE);
				label.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, true, 1, 1));
				label.setText(LangResources.get("fingerprintSha384"));

				publicKeySha384Text = addSizeLimitedTextField(composite, CryptographicUtilities.getOpenSshFingerPrintSha384Base64(publicKey));

				label = new Label(composite, SWT.NONE);
				label.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, true, 1, 1));
				label.setText(LangResources.get("fingerprintSha512"));

				publicKeySha512Text = addSizeLimitedTextField(composite, CryptographicUtilities.getOpenSshFingerPrintSha512Base64(publicKey));
			} else if (publicKey instanceof BCECPublicKey) {
				label = new Label(composite, SWT.NONE);
				label.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, true, 1, 1));
				label.setText(LangResources.get("ellipticCurveName"));

				textField = new StyledText(composite, SWT.READ_ONLY | SWT.BORDER);
				textField.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, true, 1, 1));
				textField.setText(CryptographicUtilities.getEllipticCurveName(publicKey));

				label = new Label(composite, SWT.NONE);
				label.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, true, 1, 1));
				label.setText(LangResources.get("fingerprintMd5"));

				publicKeyMd5Text = addSizeLimitedTextField(composite, CryptographicUtilities.getOpenSshFingerPrintMd5(publicKey, ":").toUpperCase());

				label = new Label(composite, SWT.NONE);
				label.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, true, 1, 1));
				label.setText(LangResources.get("fingerprintSha1"));

				publicKeySha1Text = addSizeLimitedTextField(composite, CryptographicUtilities.getOpenSshFingerPrintSha1(publicKey, ":").toUpperCase());

				label = new Label(composite, SWT.NONE);
				label.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, true, 1, 1));
				label.setText(LangResources.get("fingerprintSha256"));

				publicKeySha256Text = addSizeLimitedTextField(composite, CryptographicUtilities.getOpenSshFingerPrintSha256(publicKey, ":").toUpperCase());

				label = new Label(composite, SWT.NONE);
				label.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, true, 1, 1));
				label.setText(LangResources.get("fingerprintSha384"));

				publicKeySha384Text = addSizeLimitedTextField(composite, CryptographicUtilities.getOpenSshFingerPrintSha384(publicKey, ":").toUpperCase());

				label = new Label(composite, SWT.NONE);
				label.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, true, 1, 1));
				label.setText(LangResources.get("fingerprintSha512"));

				publicKeySha512Text = addSizeLimitedTextField(composite, CryptographicUtilities.getOpenSshFingerPrintSha512(publicKey, ":").toUpperCase());
			} else {
				label = new Label(composite, SWT.NONE);
				label.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, true, 1, 1));
				label.setText("PublicKey class");

				textField = new StyledText(composite, SWT.READ_ONLY | SWT.BORDER);
				textField.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, true, 1, 1));
				textField.setText(publicKey.getClass().getName());
			}
		} else {
			label = new Label(composite, SWT.NONE);
			label.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, true, 2, 1));
			label.setText(LangResources.get("notFound"));
		}

		final Label separator = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
		separator.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, true, 2, 1));

		// Private key
		label = new Label(composite, SWT.NONE);
		label.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, true, 2, 1));
		label.setText(LangResources.get("privateKey"));

		if (privateKey != null) {
			label = new Label(composite, SWT.NONE);
			label.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, true, 1, 1));
			label.setText(LangResources.get("keyType"));

			StyledText textField = new StyledText(composite, SWT.READ_ONLY | SWT.BORDER);
			textField.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, true, 1, 1));
			textField.setText(privateKey.getAlgorithm());

			label = new Label(composite, SWT.NONE);
			label.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, true, 1, 1));
			label.setText(LangResources.get("keyFormat"));

			textField = new StyledText(composite, SWT.READ_ONLY | SWT.BORDER);
			textField.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, true, 1, 1));
			textField.setText(privateKey.getFormat());

			if (privateKey instanceof RSAPrivateKey) {
				label = new Label(composite, SWT.NONE);
				label.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, true, 1, 1));
				label.setText(LangResources.get("keyStrength"));

				textField = new StyledText(composite, SWT.READ_ONLY | SWT.BORDER);
				textField.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, true, 1, 1));
				textField.setText(Integer.toString(((RSAPrivateKey) privateKey).getModulus().bitLength()));

				label = new Label(composite, SWT.NONE);
				label.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, true, 1, 1));
				label.setText(LangResources.get("fingerprintMd5"));

				privateKeyMd5Text = addSizeLimitedTextField(composite, "");

				label = new Label(composite, SWT.NONE);
				label.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, true, 1, 1));
				label.setText(LangResources.get("fingerprintSha1"));

				privateKeySha1Text = addSizeLimitedTextField(composite, "");

				label = new Label(composite, SWT.NONE);
				label.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, true, 1, 1));
				label.setText(LangResources.get("fingerprintSha256"));

				privateKeySha256Text = addSizeLimitedTextField(composite, "");

				label = new Label(composite, SWT.NONE);
				label.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, true, 1, 1));
				label.setText(LangResources.get("fingerprintSha384"));

				privateKeySha384Text = addSizeLimitedTextField(composite, "");

				label = new Label(composite, SWT.NONE);
				label.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, true, 1, 1));
				label.setText(LangResources.get("fingerprintSha512"));

				privateKeySha512Text = addSizeLimitedTextField(composite, "");
			} else if (privateKey instanceof BCECPrivateKey) {
				label = new Label(composite, SWT.NONE);
				label.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, true, 1, 1));
				label.setText(LangResources.get("ellipticCurveName"));

				textField = new StyledText(composite, SWT.READ_ONLY | SWT.BORDER);
				textField.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, true, 1, 1));
				textField.setText(CryptographicUtilities.getEllipticCurveName(privateKey));

				label = new Label(composite, SWT.NONE);
				label.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, true, 1, 1));
				label.setText(LangResources.get("fingerprintMd5"));

				privateKeyMd5Text = addSizeLimitedTextField(composite, "");

				label = new Label(composite, SWT.NONE);
				label.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, true, 1, 1));
				label.setText(LangResources.get("fingerprintSha1"));

				privateKeySha1Text = addSizeLimitedTextField(composite, "");

				label = new Label(composite, SWT.NONE);
				label.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, true, 1, 1));
				label.setText(LangResources.get("fingerprintSha256"));

				privateKeySha256Text = addSizeLimitedTextField(composite, "");

				label = new Label(composite, SWT.NONE);
				label.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, true, 1, 1));
				label.setText(LangResources.get("fingerprintSha384"));

				privateKeySha384Text = addSizeLimitedTextField(composite, "");

				label = new Label(composite, SWT.NONE);
				label.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, true, 1, 1));
				label.setText(LangResources.get("fingerprintSha512"));

				privateKeySha512Text = addSizeLimitedTextField(composite, "");
			} else {
				label = new Label(composite, SWT.NONE);
				label.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, true, 1, 1));
				label.setText("PrivateKey class");

				textField = new StyledText(composite, SWT.READ_ONLY | SWT.BORDER);
				textField.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, true, 1, 1));
				textField.setText(privateKey.getClass().getName());
			}
		} else {
			label = new Label(composite, SWT.NONE);
			label.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, true, 2, 1));
			label.setText(LangResources.get("notFound"));
		}

		composite = new Composite(shell, SWT.NONE);
		gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		composite.setLayoutData(gridData);
		composite.setLayout(SwtUtilities.createNoMarginGridLayout(1, true));

		//		final Button buttonSave = new Button(composite, SWT.PUSH);
		//		buttonSave.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false));
		//		buttonSave.setText(LangResources.get("save"));
		//		buttonSave.addSelectionListener(new SelectionAdapter() {
		//			@Override
		//			public void widgetSelected(final SelectionEvent event) {
		//				setReturnValue(true);
		//				getParent().close();
		//			}
		//		});

		final Button buttonHex = new Button(composite, SWT.PUSH);
		buttonHex.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false, 1, 1));
		buttonHex.setText(LangResources.get("hexFormat"));
		buttonHex.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				try {
					displayHexFormat();
				} catch (final Exception e) {
					showErrorMessage("Error in fingerprint creation", "Cannot create key fingerprint: " + e.getMessage());
					e.printStackTrace();
				}
			}
		});

		final Button buttonHexColon = new Button(composite, SWT.PUSH);
		buttonHexColon.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false, 1, 1));
		buttonHexColon.setText(LangResources.get("hexColonFormat"));
		buttonHexColon.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				try {
					displayHexColonFormat();
				} catch (final Exception e) {
					showErrorMessage("Error in fingerprint creation", "Cannot create key fingerprint: " + e.getMessage());
					e.printStackTrace();
				}
			}
		});

		final Button buttonBase64 = new Button(composite, SWT.PUSH);
		buttonBase64.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false, 1, 1));
		buttonBase64.setText(LangResources.get("base64Format"));
		buttonBase64.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				try {
					displayBase64Format();
				} catch (final Exception e) {
					showErrorMessage("Error in fingerprint creation", "Cannot create key fingerprint: " + e.getMessage());
					e.printStackTrace();
				}
			}
		});

		final Button buttonClose = new Button(composite, SWT.PUSH);
		buttonClose.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false, 1, 1));
		buttonClose.setText(LangResources.get("close"));
		buttonClose.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				setReturnValue(false);
				getParent().close();
			}
		});

		shell.pack();
		shell.setMinimumSize(500, shell.getSize().y);
		shell.setSize(500, shell.getSize().y);

		displayHexColonFormat();
	}

	private static StyledText addSizeLimitedTextField(final Composite composite, final String text) {
		final Composite compositeLimited = new Composite(composite, SWT.NONE);
		final GridData gridDataLimited = new GridData(SWT.FILL, SWT.FILL, true, true);
		compositeLimited.setLayoutData(gridDataLimited);
		compositeLimited.setLayout(SwtUtilities.createNoMarginGridLayout(1, false));

		final StyledText textField = new StyledText(compositeLimited, SWT.READ_ONLY | SWT.BORDER);
		textField.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		textField.setText(text);

		return textField;
	}

	private void displayHexFormat() throws Exception {
		try {
			if (publicKey != null) {
				publicKeyMd5Text.setText(CryptographicUtilities.getOpenSshFingerPrintMd5(publicKey, "").toUpperCase());
				publicKeySha1Text.setText(CryptographicUtilities.getOpenSshFingerPrintSha1(publicKey, "").toUpperCase());
				publicKeySha256Text.setText(CryptographicUtilities.getOpenSshFingerPrintSha256(publicKey, "").toUpperCase());
				publicKeySha384Text.setText(CryptographicUtilities.getOpenSshFingerPrintSha384(publicKey, "").toUpperCase());
				publicKeySha512Text.setText(CryptographicUtilities.getOpenSshFingerPrintSha512(publicKey, "").toUpperCase());
			}
			if (privateKey != null) {
				privateKeyMd5Text.setText(CryptographicUtilities.getOpenSshFingerPrintMd5(privateKey, "").toUpperCase());
				privateKeySha1Text.setText(CryptographicUtilities.getOpenSshFingerPrintSha1(privateKey, "").toUpperCase());
				privateKeySha256Text.setText(CryptographicUtilities.getOpenSshFingerPrintSha256(privateKey, "").toUpperCase());
				privateKeySha384Text.setText(CryptographicUtilities.getOpenSshFingerPrintSha384(privateKey, "").toUpperCase());
				privateKeySha512Text.setText(CryptographicUtilities.getOpenSshFingerPrintSha512(privateKey, "").toUpperCase());
			}
		} catch (@SuppressWarnings("unused") final NoSuchAlgorithmException e) {
			// do nothing, won't happen
		}
	}

	private void displayHexColonFormat() throws Exception {
		try {
			if (publicKey != null) {
				publicKeyMd5Text.setText(CryptographicUtilities.getOpenSshFingerPrintMd5(publicKey, ":").toUpperCase());
				publicKeySha1Text.setText(CryptographicUtilities.getOpenSshFingerPrintSha1(publicKey, ":").toUpperCase());
				publicKeySha256Text.setText(CryptographicUtilities.getOpenSshFingerPrintSha256(publicKey, ":").toUpperCase());
				publicKeySha384Text.setText(CryptographicUtilities.getOpenSshFingerPrintSha384(publicKey, ":").toUpperCase());
				publicKeySha512Text.setText(CryptographicUtilities.getOpenSshFingerPrintSha512(publicKey, ":").toUpperCase());
			}
			if (privateKey != null) {
				privateKeyMd5Text.setText(CryptographicUtilities.getOpenSshFingerPrintMd5(privateKey, ":").toUpperCase());
				privateKeySha1Text.setText(CryptographicUtilities.getOpenSshFingerPrintSha1(privateKey, ":").toUpperCase());
				privateKeySha256Text.setText(CryptographicUtilities.getOpenSshFingerPrintSha256(privateKey, ":").toUpperCase());
				privateKeySha384Text.setText(CryptographicUtilities.getOpenSshFingerPrintSha384(privateKey, ":").toUpperCase());
				privateKeySha512Text.setText(CryptographicUtilities.getOpenSshFingerPrintSha512(privateKey, ":").toUpperCase());
			}
		} catch (@SuppressWarnings("unused") final NoSuchAlgorithmException e) {
			// do nothing, won't happen
		}
	}

	private void displayBase64Format() throws Exception {
		try {
			if (publicKey != null) {
				publicKeyMd5Text.setText(CryptographicUtilities.getOpenSshFingerPrintMd5Base64(publicKey));
				publicKeySha1Text.setText(CryptographicUtilities.getOpenSshFingerPrintSha1Base64(publicKey));
				publicKeySha256Text.setText(CryptographicUtilities.getOpenSshFingerPrintSha256Base64(publicKey));
				publicKeySha384Text.setText(CryptographicUtilities.getOpenSshFingerPrintSha384Base64(publicKey));
				publicKeySha512Text.setText(CryptographicUtilities.getOpenSshFingerPrintSha512Base64(publicKey));
			}
			if (privateKey != null) {
				privateKeyMd5Text.setText(CryptographicUtilities.getOpenSshFingerPrintMd5Base64(privateKey));
				privateKeySha1Text.setText(CryptographicUtilities.getOpenSshFingerPrintSha1Base64(privateKey));
				privateKeySha256Text.setText(CryptographicUtilities.getOpenSshFingerPrintSha256Base64(privateKey));
				privateKeySha384Text.setText(CryptographicUtilities.getOpenSshFingerPrintSha384Base64(privateKey));
				privateKeySha512Text.setText(CryptographicUtilities.getOpenSshFingerPrintSha512Base64(privateKey));
			}
		} catch (@SuppressWarnings("unused") final NoSuchAlgorithmException e) {
			// do nothing, won't happen
		}
	}

	public PublicKey getPublicKey() {
		return publicKey;
	}

	public PrivateKey getPrivateKey() {
		return privateKey;
	}
}
