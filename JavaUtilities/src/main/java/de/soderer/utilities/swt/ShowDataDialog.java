package de.soderer.utilities.swt;

import java.io.File;
import java.io.UnsupportedEncodingException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import de.soderer.utilities.LangResources;

public class ShowDataDialog extends ModalDialog<Integer> {
	private String title;
	private String dataString;
	private String buttonTextSave;
	private String buttonTextClose;

	public ShowDataDialog(String title, String dataString, Shell shell, String buttonTextSave, String buttonTextClose) {   
		super(shell);
		this.title = title;
		this.dataString = dataString;
		this.buttonTextSave = buttonTextSave;
		this.buttonTextClose = buttonTextClose;
	}
	
	public ShowDataDialog(String dataString, Shell shell, String buttonTextSave, String buttonTextClose) {   
		super(shell);
		this.dataString = dataString;
		this.buttonTextSave = buttonTextSave;
		this.buttonTextClose = buttonTextClose;
	}
	
	public ShowDataDialog(String title, String dataString, Shell shell, String buttonTextClose) {   
		super(shell);
		this.title = title;
		this.dataString = dataString;
		buttonTextSave = null;
		this.buttonTextClose = buttonTextClose;
	}
	
	public ShowDataDialog(String dataString, Shell shell, String buttonTextClose) {   
		super(shell);
		this.dataString = dataString;
		buttonTextSave = null;
		this.buttonTextClose = buttonTextClose;
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
		composite.setLayout(new GridLayout(2, true));
		
		Text textField = new Text(composite, SWT.READ_ONLY | SWT.BORDER | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		GridData gridData = new GridData(SWT.LEFT, SWT.CENTER, true, true, 2, 1);
		gridData.heightHint = 200;
		gridData.widthHint = 200;
		textField.setLayoutData(gridData);
		textField.setText(dataString);
		textField.setSize(200, 200);
	    
		if (buttonTextSave != null) {
			Button buttonSave = new Button(composite, SWT.PUSH);
			buttonSave.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false));
			buttonSave.setText(buttonTextSave);
			buttonSave.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					try {
						saveDataInteractive(LangResources.get("savedata"), dataString.getBytes("UTF-8"), getShell());
					} catch (UnsupportedEncodingException e1) {
						e1.printStackTrace();
					}
			        
					setReturnValue(0);
					getShell().close();
				}
			});
		}

		Button buttonClose = new Button(composite, SWT.PUSH);
		buttonClose.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false, (buttonTextSave == null ? 2 : 1), 1));
		buttonClose.setText(buttonTextClose);
		buttonClose.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setReturnValue(0);
				getShell().close();
			}
		});

		shell.pack();
	}
	
	public static String saveDataInteractive(String title, byte[] data, Shell shell) {
		try {
			boolean retry = true;
			while (retry) {
				FileDialog dialog = new FileDialog(shell, SWT.SAVE);
			    dialog.setText(title + " " + LangResources.get("savefile"));
			    String fileName = dialog.open();
			    if (fileName != null) {
			        if (new File(fileName).exists() && new File(fileName).isDirectory()) {
			        	QuestionDialog questionDialog = new QuestionDialog(LangResources.get("pathisdirectory"), shell, LangResources.get("ok"), LangResources.get("cancel"));
			        	if (questionDialog.open() != 0) {
			        		retry = false;
			        	}
			        } else {
				        if (new File(fileName).exists()) {
				        	QuestionDialog questionDialog = new QuestionDialog(LangResources.get("filealreadyexists"), shell, LangResources.get("ok"), LangResources.get("cancel"));
				        	if (questionDialog.open() != 0) {
				        		retry = false;
				        	}
				        }
				        	
				        if (retry) {
			        		FileUtils.writeByteArrayToFile(new File(fileName), data);
			        		return fileName;
				        } else {
				        	return null;
				        }
			        }
			    } else {
			    	retry = false;
			    }
			}
			return null;
		} catch (Exception e) {
			new QuestionDialog(LangResources.get("filewasnotsaved"), LangResources.get("error") + ":\n" + e.getMessage(), shell, LangResources.get("ok")).open();
			return null;
		}
	}
}
