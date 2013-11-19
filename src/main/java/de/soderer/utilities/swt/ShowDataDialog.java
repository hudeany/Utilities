package de.soderer.utilities.swt;

import java.io.File;
import java.io.UnsupportedEncodingException;

import org.apache.commons.io.FileUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

import de.soderer.utilities.LangResources;

public class ShowDataDialog extends ModalDialog<Integer> {
	private String dataString;
	private boolean wordWrap = false;
	private boolean showSaveButton;

	public ShowDataDialog(Shell shell, String title, String dataString, boolean showSaveButton) {
		super(shell, SWT.RESIZE, title);
		this.dataString = dataString;
		this.showSaveButton = showSaveButton;
	}
	
	public ShowDataDialog(Shell shell, String title, String dataString) {   
		super(shell, SWT.RESIZE, title);
		this.dataString = dataString;
		showSaveButton = false;
	}
	
	public void setWordWrap(boolean value) {
		wordWrap = value;
	}
	
	@Override
	protected void createComponents(Shell shell) throws Exception {
		shell.setLayout(new GridLayout(1, true));
		
		Composite composite = new Composite(shell, SWT.NONE);
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		composite.setLayoutData(gridData);
		composite.setLayout(SwtUtilities.createNoMarginGridLayout(2, true));
		
		StyledText textField = new StyledText(composite, SWT.READ_ONLY | SWT.BORDER | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		textField.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		textField.setText(dataString);
		textField.setWordWrap(wordWrap);
	    
		if (showSaveButton) {
			Button buttonSave = new Button(composite, SWT.PUSH);
			buttonSave.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false));
			buttonSave.setText(LangResources.get("save"));
			buttonSave.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					try {
						saveDataInteractive(getParent(), LangResources.get("savedata"), dataString.getBytes("UTF-8"));
					} catch (UnsupportedEncodingException e1) {
						e1.printStackTrace();
					}
				}
			});
		}

		Button buttonClose = new Button(composite, SWT.PUSH);
		buttonClose.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false, (showSaveButton ? 1 : 2), 1));
		buttonClose.setText(LangResources.get("close"));
		buttonClose.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setReturnValue(0);
				getParent().close();
			}
		});

		shell.pack();
		shell.setSize(300, 300);
	}
	
	public static String saveDataInteractive(Shell shell, String title, byte[] data) {
		try {
			boolean retry = true;
			while (retry) {
				FileDialog dialog = new FileDialog(shell, SWT.SAVE);
			    dialog.setText(title + " " + LangResources.get("savefile"));
			    String fileName = dialog.open();
			    if (fileName != null) {
			        if (new File(fileName).exists() && new File(fileName).isDirectory()) {
			        	QuestionDialog questionDialog = new QuestionDialog(shell, title, LangResources.get("pathisdirectory"), LangResources.get("ok"), LangResources.get("cancel"));
			        	if (questionDialog.open() != 0) {
			        		retry = false;
			        	}
			        } else {
				        if (new File(fileName).exists()) {
				        	QuestionDialog questionDialog = new QuestionDialog(shell, title, LangResources.get("filealreadyexists"), LangResources.get("ok"), LangResources.get("cancel"));
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
			new QuestionDialog(shell, title, LangResources.get("filewasnotsaved"), LangResources.get("error") + ":\n" + e.getMessage(), LangResources.get("ok")).open();
			return null;
		}
	}
}
