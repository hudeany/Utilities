package de.soderer.utilities.swt;

import java.io.InputStream;

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

import de.soderer.utilities.ExceptionUtilities;
import de.soderer.utilities.LangResources;
import de.soderer.utilities.Utilities;
import de.soderer.utilities.VisibleException;
import de.soderer.utilities.mail.MailUtilities;

public class ErrorDialog extends ModalDialog<Boolean> {
	private final String applicationName;
	private final String applicationVersion;
	private final String emailRecipient;
	private final Throwable throwable;
	private Button detailButton;
	private Composite detailPart;

	public ErrorDialog(final Shell shell, final String applicationName, final String applicationVersion, final String emailRecipient, final Throwable throwable) {
		super(shell, SWT.CLOSE, applicationName + (Utilities.isNotBlank(applicationVersion) ? " (" + applicationVersion + ")" : "") + " " + LangResources.get("error"));
		this.throwable = throwable;
		this.applicationName = applicationName;
		this.applicationVersion = applicationVersion;
		this.emailRecipient = emailRecipient;
	}

	@Override
	protected void createComponents(final Shell shell) throws Exception {
		try (InputStream errorImageStream = getClass().getClassLoader().getResourceAsStream("images/icons/exclamation.png")) {
			if (errorImageStream == null) {
				throw new Exception("Missing image file: " + "exclamation.png");
			}
			shell.setImage(new Image(shell.getDisplay(), errorImageStream));
		} catch (final Exception e) {
			e.printStackTrace();
		}
		shell.setLayout(new GridLayout(1, false));

		String text1 = LangResources.get("errorOccurred");
		if (throwable instanceof VisibleException) {
			text1 = ((VisibleException) throwable).getVisibleMessage();
		} else {
			text1 = throwable.getMessage();
		}

		final GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, false);
		gridData.widthHint = 500;

		final Text keyTextfield1 = new Text(shell, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.READ_ONLY | SWT.V_SCROLL);
		keyTextfield1.setLayoutData(gridData);
		keyTextfield1.setText(text1);

		final Composite buttonComposite = new Composite(shell, SWT.NONE);
		buttonComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		buttonComposite.setLayout(new FillLayout());

		final Button sendMailButton = new Button(buttonComposite, SWT.PUSH);
		sendMailButton.setText(LangResources.get("openMail"));
		sendMailButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				final boolean success = MailUtilities.openMailInStandardClient(emailRecipient, "Error in " + applicationName + (Utilities.isNotBlank(applicationVersion) ? " (" + applicationVersion + ")" : ""), ExceptionUtilities.throwableToString(throwable));
				if (!success) {
					new QuestionDialog(getParent(), getText(), LangResources.get("cannotOpenMail"), "OK").setBackgroundColor(SwtColor.LightRed).open();
				}
			}
		});

		detailButton = new Button(buttonComposite, SWT.PUSH);
		detailButton.setText(LangResources.get("detailsMore"));
		detailButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				if (detailButton.getText().equals(LangResources.get("detailsMore"))) {
					detailButton.setText(LangResources.get("detailsLess"));
				} else {
					detailButton.setText(LangResources.get("detailsMore"));
				}
				final Point currentLocation = getParent().getLocation();
				setupDetailPart();
				getParent().setLocation(currentLocation);
			}
		});

		detailPart = new Composite(shell, SWT.NONE);
		detailPart.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM | SWT.READ_ONLY, true, false, 1, 1));
		detailPart.setLayout(SwtUtilities.createNoMarginGridLayout(1, false));

		setupDetailPart();

		final Button closeButton = new Button(shell, SWT.PUSH);
		closeButton.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false));
		closeButton.setText(LangResources.get("close"));
		closeButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				setReturnValue(false);
				getParent().close();
			}
		});

		shell.pack();
	}

	private void setupDetailPart() {
		for (final Control item : detailPart.getChildren()) {
			item.dispose();
		}

		if (detailButton.getText().equals(LangResources.get("detailsLess"))) {
			final String text2 = throwable.getClass().getSimpleName() + ": " + throwable.getMessage();

			final String text3 = ExceptionUtilities.throwableToString(throwable);

			final Text keyTextfield2 = new Text(detailPart, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.READ_ONLY);
			final GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, false);
			gridData.widthHint = 500;
			keyTextfield2.setLayoutData(gridData);
			keyTextfield2.setText(text2);
			if (Utilities.isNotEmpty(text3)) {
				final StyledText keyTextfield3 = new StyledText(detailPart, SWT.BORDER | SWT.MULTI | SWT.READ_ONLY | SWT.H_SCROLL | SWT.V_SCROLL);
				final GridData gridData2 = new GridData(SWT.FILL, SWT.FILL, true, false);
				gridData2.widthHint = 500;
				gridData2.heightHint = 400;
				keyTextfield3.setLayoutData(gridData2);
				keyTextfield3.setText(text3);
				final FontData[] fontData = keyTextfield3.getFont().getFontData();
				fontData[0].setHeight(7);
				keyTextfield3.setFont(new Font(getParent().getDisplay(), fontData[0]));
			}
		}

		detailPart.pack();
		detailPart.layout();
		getParent().pack();
		setPosition(getParent());
	}

	@Override
	protected Boolean getDefaultReturnValue() {
		return false;
	}
}
