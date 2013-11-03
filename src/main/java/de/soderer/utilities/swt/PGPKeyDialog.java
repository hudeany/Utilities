package de.soderer.utilities.swt;

import java.security.PrivateKey;
import java.util.LinkedHashMap;
import java.util.Map;

import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKey;
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

import de.soderer.utilities.BitUtilities;
import de.soderer.utilities.LangResources;

public class PGPKeyDialog extends ModalDialog<Boolean> {
	private PGPPublicKey publicKey;
	private PGPPrivateKey privateKey;

	public PGPKeyDialog(Shell shell, String title, PGPPublicKey publicKey, PGPPrivateKey privateKey) {   
		super(shell, SWT.RESIZE, title);
		this.publicKey = publicKey;
		this.privateKey = privateKey;
	}
	
	@Override
	protected void createComponents(Shell shell) throws Exception {
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
			Map<String, String> map = new LinkedHashMap<String, String>();
			map.put("keyType", Integer.toString(publicKey.getAlgorithm()));
			map.put("keyStrength", Integer.toString(publicKey.getBitStrength()));
			map.put("validDays", Integer.toString(publicKey.getValidDays()));
			map.put("validSeconds", Long.toString(publicKey.getValidSeconds()));
			map.put("version", Integer.toString(publicKey.getVersion()));
			map.put("creationTime", publicKey.getCreationTime().toString());
			map.put("fingerprint", BitUtilities.getHexString(publicKey.getFingerprint()));
			map.put("keyID", Long.toString(publicKey.getKeyID()));
			
			for (Map.Entry<String, String> entry : map.entrySet()) {
				label = new Label(composite, SWT.NONE);
				label.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, true, 1, 1));
				label.setText(LangResources.get(entry.getKey()));
				
				StyledText textField = new StyledText(composite, SWT.READ_ONLY | SWT.BORDER);
				textField.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, true, 1, 1));
				textField.setText(entry.getValue());
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
			Map<String, String> map = new LinkedHashMap<String, String>();
			map.put("keyType", privateKey.getKey().getAlgorithm());
			map.put("keyFormat", privateKey.getKey().getFormat());
			map.put("keyID", Long.toString(privateKey.getKeyID()));

			for (Map.Entry<String, String> entry : map.entrySet()) {
				label = new Label(composite, SWT.NONE);
				label.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, true, 1, 1));
				label.setText(LangResources.get(entry.getKey()));
				
				StyledText textField = new StyledText(composite, SWT.READ_ONLY | SWT.BORDER);
				textField.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, true, 1, 1));
				textField.setText(entry.getValue());
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
				getParent().close();
			}
		});

		Button buttonClose = new Button(composite, SWT.PUSH);
		buttonClose.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false, 1, 1));
		buttonClose.setText(LangResources.get("close"));
		buttonClose.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setReturnValue(false);
				getParent().close();
			}
		});

		shell.pack();
	}

	public PGPPublicKey getPublicKey() {
		return publicKey;
	}

	public PrivateKey getPrivateKey() {
		return privateKey.getKey();
	}
}
