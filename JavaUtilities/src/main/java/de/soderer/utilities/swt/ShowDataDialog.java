package de.soderer.utilities.swt;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class ShowDataDialog extends ModalDialog<Integer> {
	private String title;
	private String dataString;
	private String buttonText;

	public ShowDataDialog(String title, String dataString, Shell shell, String buttonText) {   
		super(shell);
		this.title = title;
		this.dataString = dataString;
		this.buttonText = buttonText;
	}
	
	public ShowDataDialog(String dataString, Shell shell, String buttonText) {   
		super(shell);
		this.dataString = dataString;
		this.buttonText = buttonText;
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
		composite.setLayout(new GridLayout(1, true));
		
		Text textField = new Text(composite, SWT.READ_ONLY | SWT.BORDER | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		GridData gridData = new GridData(SWT.LEFT, SWT.CENTER, true, true, 1, 1);
		gridData.heightHint = 200;
		textField.setLayoutData(gridData);
		textField.setText(dataString);
		textField.setSize(100, 200);
	    
		Button button = new Button(composite, SWT.PUSH);
	    button.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false));
	    button.setText(buttonText);
	    button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setReturnValue(0);
				getShell().close();
			}
		});

		shell.pack();
	}
}
