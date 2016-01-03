package de.soderer.utilities.swt;

import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import de.soderer.utilities.EmailUtilities;
import de.soderer.utilities.ExceptionUtilities;
import de.soderer.utilities.VisibleException;

public class ErrorDialog extends ModalDialog<Boolean> {
	private String applicationName;
	private String emailRecipient;
	private Throwable throwable;
	private Button detailButton;
	private Composite detailPart;

	public ErrorDialog(Shell shell, String applicationName, Throwable throwable) {
		this(shell, applicationName, "", throwable);
	}

	public ErrorDialog(Shell shell, String applicationName, String emailRecipient, Throwable throwable) {
		super(shell, applicationName + " " + getText("ERROR"));
		this.throwable = throwable;
		this.applicationName = applicationName;
		this.emailRecipient = emailRecipient;
	}

	@Override
	protected void createComponents(Shell shell) throws Exception {
		try {
			shell.setImage(new Image(shell.getDisplay(), getClass().getResourceAsStream("exclamation.png")));
		} catch (Exception e) {
			e.printStackTrace();
		}
		shell.setLayout(new GridLayout(1, false));

		String text1 = getText("ERROR_OCCURRED");
		if (throwable instanceof VisibleException) {
			text1 = ((VisibleException)throwable).getVisibleMessage();
		}

		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, false);
		gridData.widthHint = 500;

		Text keyTextfield1 = new Text(shell, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.READ_ONLY | SWT.V_SCROLL);
		keyTextfield1.setLayoutData(gridData);
		keyTextfield1.setText(text1);

		Composite buttonComposite = new Composite(shell, SWT.NONE);
		buttonComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		buttonComposite.setLayout(new FillLayout());

		Button sendMailButton = new Button(buttonComposite, SWT.PUSH);
		sendMailButton.setText(getText("OPEN_MAIL"));
		sendMailButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				boolean success = EmailUtilities.openMailInStandardClient(emailRecipient, "Error in " + applicationName, ExceptionUtilities.throwableToString(throwable));
				if (!success) {
					new QuestionDialog(getParent(), getText(), getText("CANNOT_OPEN_MAIL"), "OK").open();
				}
			}
		});

		detailButton = new Button(buttonComposite, SWT.PUSH);
		detailButton.setText(getText("DETAILS_MORE"));
		detailButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (detailButton.getText().equals(getText("DETAILS_MORE"))) {
					detailButton.setText(getText("DETAILS_LESS"));
				} else {
					detailButton.setText(getText("DETAILS_MORE"));
				}
				Point currentLocation = getParent().getLocation();
				setupDetailPart();
				getParent().setLocation(currentLocation);
			}
		});

		detailPart = new Composite(shell, SWT.NONE);
		detailPart.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM | SWT.READ_ONLY, true, false, 1, 1));
		detailPart.setLayout(SwtUtilities.createNoMarginGridLayout(1, false));

		setupDetailPart();

		Button closeButton = new Button(shell, SWT.PUSH);
		closeButton.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false));
		closeButton.setText(getText("CLOSE"));
		closeButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setReturnValue(false);
				getParent().close();
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

			String text3 = ExceptionUtilities.throwableToString(throwable);

			Text keyTextfield2 = new Text(detailPart, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.READ_ONLY);
			GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, false);
			gridData.widthHint = 500;
			keyTextfield2.setLayoutData(gridData);
			keyTextfield2.setText(text2);
			if (StringUtils.isNotEmpty(text3)) {
				StyledText keyTextfield3 = new StyledText(detailPart, SWT.BORDER | SWT.MULTI | SWT.READ_ONLY | SWT.H_SCROLL | SWT.V_SCROLL);
				GridData gridData2 = new GridData(SWT.FILL, SWT.FILL, true, false);
				gridData2.widthHint = 500;
				gridData2.heightHint = 400;
				keyTextfield3.setLayoutData(gridData2);
				keyTextfield3.setText(text3);
				FontData[] fontData = keyTextfield3.getFont().getFontData();
				fontData[0].setHeight(7);
				keyTextfield3.setFont(new Font(getParent().getDisplay(), fontData[0]));
			}
		}

		detailPart.pack();
		detailPart.layout();
		getParent().pack();
		setPosition(getParent());
	}

	private static String getText(String key) {
		if ("DETAILS_MORE".equalsIgnoreCase(key)) {
			return "Details >>>";
		} else if ("DETAILS_LESS".equalsIgnoreCase(key)) {
			return "Details <<<";
		} else if (Locale.getDefault().getLanguage() == "de") {
			if ("OPEN_MAIL".equalsIgnoreCase(key)) {
				return "Mail mit Fehlerdaten an den Entwickler öffnen";
			} else if ("CANNOT_OPEN_MAIL".equalsIgnoreCase(key)) {
				return "Email client konnte nicht gefunden werden.";
			} else if ("CLOSE".equalsIgnoreCase(key)) {
				return "Schließen";
			} else if ("ERROR".equalsIgnoreCase(key)) {
				return "Fehler";
			} else if ("ERROR_OCCURRED".equalsIgnoreCase(key)) {
				return "Es ist ein Fehler aufgetreten";
			}
		} else {
			if ("OPEN_MAIL".equalsIgnoreCase(key)) {
				return "Open mail with errordata to developer";
			} else if ("CANNOT_OPEN_MAIL".equalsIgnoreCase(key)) {
				return "Email client was not found.";
			} else if ("CLOSE".equalsIgnoreCase(key)) {
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
