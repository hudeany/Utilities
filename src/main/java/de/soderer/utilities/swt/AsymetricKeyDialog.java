package de.soderer.utilities.swt;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

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

public class AsymetricKeyDialog extends ModalDialog<Boolean> {
	private final PublicKey publicKey;
	private final PrivateKey privateKey;

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
			label.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, true, 1, 1));
			label.setText(LangResources.get("keyType"));

			StyledText textField = new StyledText(composite, SWT.READ_ONLY | SWT.BORDER);
			textField.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, true, 1, 1));
			textField.setText(publicKey.getAlgorithm());

			label = new Label(composite, SWT.NONE);
			label.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, true, 1, 1));
			label.setText(LangResources.get("keyFormat"));

			textField = new StyledText(composite, SWT.READ_ONLY | SWT.BORDER);
			textField.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, true, 1, 1));
			textField.setText(publicKey.getFormat());

			if (publicKey instanceof RSAPublicKey) {
				label = new Label(composite, SWT.NONE);
				label.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, true, 1, 1));
				label.setText(LangResources.get("keyStrength"));

				textField = new StyledText(composite, SWT.READ_ONLY | SWT.BORDER);
				textField.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, true, 1, 1));
				textField.setText(Integer.toString(((RSAPublicKey) publicKey).getModulus().bitLength()));
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
			label.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, true, 1, 1));
			label.setText(LangResources.get("keyType"));

			StyledText textField = new StyledText(composite, SWT.READ_ONLY | SWT.BORDER);
			textField.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, true, 1, 1));
			textField.setText(privateKey.getAlgorithm());

			label = new Label(composite, SWT.NONE);
			label.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, true, 1, 1));
			label.setText(LangResources.get("keyFormat"));

			textField = new StyledText(composite, SWT.READ_ONLY | SWT.BORDER);
			textField.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, true, 1, 1));
			textField.setText(privateKey.getFormat());

			if (privateKey instanceof RSAPrivateKey) {
				label = new Label(composite, SWT.NONE);
				label.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, true, 1, 1));
				label.setText(LangResources.get("keyStrength"));

				textField = new StyledText(composite, SWT.READ_ONLY | SWT.BORDER);
				textField.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, true, 1, 1));
				textField.setText(Integer.toString(((RSAPrivateKey) privateKey).getModulus().bitLength()));
			}
		} else {
			label = new Label(composite, SWT.NONE);
			label.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, true, 2, 1));
			label.setText(LangResources.get("notFound"));
		}

		composite = new Composite(shell, SWT.NONE);
		gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		composite.setLayoutData(gridData);
		composite.setLayout(SwtUtilities.createNoMarginGridLayout(2, true));

		final Button buttonSave = new Button(composite, SWT.PUSH);
		buttonSave.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false));
		buttonSave.setText(LangResources.get("save"));
		buttonSave.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				setReturnValue(true);
				getParent().close();
			}
		});

		final Button buttonClose = new Button(composite, SWT.PUSH);
		buttonClose.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false, 1, 1));
		buttonClose.setText(LangResources.get("close"));
		buttonClose.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				setReturnValue(false);
				getParent().close();
			}
		});

		shell.pack();
		shell.setMinimumSize(300, shell.getSize().y);
		shell.setSize(300, shell.getSize().y);
	}

	public PublicKey getPublicKey() {
		return publicKey;
	}

	public PrivateKey getPrivateKey() {
		return privateKey;
	}
}
