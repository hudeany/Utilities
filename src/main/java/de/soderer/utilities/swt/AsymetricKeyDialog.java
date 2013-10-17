package de.soderer.utilities.swt;

import java.security.PrivateKey;
import java.security.PublicKey;

import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.jce.provider.JCERSAPrivateCrtKey;
import org.bouncycastle.jce.provider.JCERSAPublicKey;
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
	private String title;
	private PublicKey publicKey;
	private PrivateKey privateKey;

	public AsymetricKeyDialog(String title, Shell shell, PublicKey publicKey, PrivateKey privateKey) {   
		super(shell, SWT.RESIZE);
		this.title = title;
		this.publicKey = publicKey;
		this.privateKey = privateKey;
	}
	
	@Override
	protected void createComponents(Shell shell) throws Exception {
		if (StringUtils.isEmpty(title)) {
			shell.setText(getText());
		} else {
			shell.setText(title);
		}
		shell.setLayout(new GridLayout());
		
		Composite composite = new Composite(shell, SWT.NONE);
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		composite.setLayoutData(gridData);
		composite.setLayout(new GridLayout(2, false));

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
	
			if (publicKey instanceof JCERSAPublicKey) {
				label = new Label(composite, SWT.NONE);
				label.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, true, 1, 1));
				label.setText(LangResources.get("keyStrength"));
				
				textField = new StyledText(composite, SWT.READ_ONLY | SWT.BORDER);
				textField.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, true, 1, 1));
				textField.setText(Integer.toString(((JCERSAPublicKey) publicKey).getModulus().bitLength()));
			}
		} else {
			label = new Label(composite, SWT.NONE);
			label.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, true, 2, 1));
			label.setText(LangResources.get("notFound"));
		}
			
		Label separator = new Label (composite, SWT.SEPARATOR | SWT.HORIZONTAL);
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
			
			if (privateKey instanceof JCERSAPrivateCrtKey) {
				label = new Label(composite, SWT.NONE);
				label.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, true, 1, 1));
				label.setText(LangResources.get("keyStrength"));
				
				textField = new StyledText(composite, SWT.READ_ONLY | SWT.BORDER);
				textField.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, true, 1, 1));
				textField.setText(Integer.toString(((JCERSAPrivateCrtKey) privateKey).getModulus().bitLength()));
			}
		} else {
			label = new Label(composite, SWT.NONE);
			label.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, true, 2, 1));
			label.setText(LangResources.get("notFound"));
		}
		
		composite = new Composite(shell, SWT.NONE);
		gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		composite.setLayoutData(gridData);
		composite.setLayout(new GridLayout(2, true));
	    
		Button buttonSave = new Button(composite, SWT.PUSH);
		buttonSave.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false));
		buttonSave.setText(LangResources.get("save"));
		buttonSave.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setReturnValue(true);
				getShell().close();
			}
		});

		Button buttonClose = new Button(composite, SWT.PUSH);
		buttonClose.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false, 1, 1));
		buttonClose.setText(LangResources.get("close"));
		buttonClose.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setReturnValue(false);
				getShell().close();
			}
		});

		shell.pack();
		shell.setSize(300, 300);
	}

	public PublicKey getPublicKey() {
		return publicKey;
	}

	public PrivateKey getPrivateKey() {
		return privateKey;
	}
}
