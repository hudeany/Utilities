package de.soderer.utilities.swt;

import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import de.soderer.utilities.TextUtilities;
import de.soderer.utilities.VisibleException;

public class ErrorDialog extends ModalDialog<Boolean> {
	private String applicationName;
	private Throwable throwable;
	private Button detailButton;
	private Composite detailPart;
	
	public ErrorDialog(Shell shell, String applicationName, Throwable throwable) {   
		super(shell);
		this.throwable = throwable;
		this.applicationName = applicationName;
	}

	@Override
	protected void createComponents(Shell shell) throws Exception {
		shell.setText(applicationName + " " + getText("ERROR"));
		try {
			shell.setImage(new Image(shell.getDisplay(), getClass().getResourceAsStream("exclamation.png")));
		} catch (Exception e) {
		}
		shell.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, true));
		shell.setLayout(new GridLayout(1, false));
		
		String text1 = getText("ERROR_OCCURRED"); 
		if (throwable instanceof VisibleException) {
			text1 = ((VisibleException) throwable).getVisibleMessage();
		}
		
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, false);
		gridData.widthHint = 500;
		
		Text keyTextfield1 = new Text(shell, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.READ_ONLY | SWT.V_SCROLL);
		keyTextfield1.setLayoutData(gridData);
		keyTextfield1.setText(text1);
		
		detailButton = new Button(shell, SWT.PUSH);
		detailButton.setLayoutData(new GridData(SWT.LEFT, SWT.UP, true, false));
		detailButton.setText(getText("DETAILS_MORE"));
		detailButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (detailButton.getText().equals(getText("DETAILS_MORE"))) {
					detailButton.setText(getText("DETAILS_LESS"));
				} else {
					detailButton.setText(getText("DETAILS_MORE"));
				}
				setupDetailPart();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});

		detailPart = new Composite(shell, SWT.NONE);
		detailPart.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM | SWT.READ_ONLY, true, false, 1, 1));
		detailPart.setLayout(new GridLayout(1, false));
		
		setupDetailPart();

		Button closeButton = new Button(shell, SWT.PUSH);
		closeButton.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false));
		closeButton.setText(getText("CLOSE"));
		closeButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setReturnValue(false);
				getShell().close();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		
		shell.pack();
	}

	private void setupDetailPart() {
		for (Control item : detailPart.getChildren()) {
			item.dispose();
		}
		
		if (detailButton.getText().equals(getText("DETAILS_LESS"))) {
			String text2 = throwable.getClass().getSimpleName() + ": " + throwable.getMessage();
			
			String text3 = null;
			if (throwable.getStackTrace() != null) {
				StringBuilder stackTraceBuilder = new StringBuilder();
				for (StackTraceElement element : throwable.getStackTrace()) {
					stackTraceBuilder.append(element);
					stackTraceBuilder.append("\n");
				}
				text3 = stackTraceBuilder.toString();
				text3 = TextUtilities.trimToMaxNumberOfLines(text3, 15);
			}
			
			GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, false);
			gridData.widthHint = 500;
			
			Text keyTextfield2 = new Text(detailPart, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.READ_ONLY);
			keyTextfield2.setLayoutData(gridData);
			keyTextfield2.setText(text2);
			if (StringUtils.isNotEmpty(text3)) {
				GridData gridData2 = new GridData(SWT.FILL, SWT.FILL, true, false);
				gridData.widthHint = 500;
				Text keyTextfield3 = new Text(detailPart, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.READ_ONLY);
				keyTextfield3.setLayoutData(gridData2);
				keyTextfield3.setText(text3);
				FontData[] fontData = keyTextfield3.getFont().getFontData();
				fontData[0].setHeight(7);
				keyTextfield3.setFont(new Font(getShell().getDisplay(), fontData[0]));
			}
		}
		
		detailPart.pack();
		detailPart.layout();
		getShell().pack();
		setPosition(getShell());
	}
	
	private static String getText(String key) {
		if ("DETAILS_MORE".equalsIgnoreCase(key)) {
			return "Details >>>";
		} else if ("DETAILS_LESS".equalsIgnoreCase(key)) {
			return "Details <<<";
		} else if (Locale.getDefault().getLanguage() == "de") {
			if ("CLOSE".equalsIgnoreCase(key)) {
				return "Schließen";
			} else if ("ERROR".equalsIgnoreCase(key)) {
				return "Fehler";
			}else if ("ERROR_OCCURRED".equalsIgnoreCase(key)) {
				return "Es ist ein Fehler aufgetreten";
			}
		} else  {
			if ("CLOSE".equalsIgnoreCase(key)) {
				return "Close";
			} else if ("ERROR".equalsIgnoreCase(key)) {
				return "Error";
			} else if ("ERROR_OCCURRED".equalsIgnoreCase(key)) {
				return "An error occurred";
			}
		}
		return key;
	}
	
	@Override
	protected Boolean getDefaultReturnValue() {
		return false;
	}
}
