package de.soderer.utilities.swt;

import java.util.regex.Pattern;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import de.soderer.utilities.LangResources;
import de.soderer.utilities.Utilities;

public class SimpleInputDialog extends ModalDialog<String> {
	private final String text;
	private Text textField;
	private String currentText = "";
	private int currentPosition = 0;
	private Pattern inputPattern = null;
	private boolean isTechnicalChange = false;
	private String defaultText = null;

	public SimpleInputDialog(final Shell shell, final String title, final String text) {
		super(shell, title);

		this.text = text;
	}

	public void setDefaultText(final String defaultText) {
		this.defaultText = defaultText;
	}

	public void setInputPattern(final String patternString) {
		if (Utilities.isBlank(patternString)) {
			inputPattern = null;
		} else {
			inputPattern = Pattern.compile(patternString);
		}
	}

	@Override
	protected void createComponents(final Shell shell) throws Exception {
		shell.setLayout(new GridLayout(1, true));

		final Label label = new Label(shell, SWT.NONE);
		label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, true));
		label.setText(text);

		textField = new Text(shell, SWT.BORDER);
		textField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
		textField.setText(defaultText != null ? defaultText : "");
		textField.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(final ModifyEvent event) {
				if (!isTechnicalChange) {
					if (inputPattern == null || inputPattern.matcher(textField.getText()).matches()) {
						currentText = textField.getText();
					} else {
						// Remove the invalid input character
						currentPosition = textField.getCaretPosition();
						isTechnicalChange = true;
						textField.setText(currentText);
						textField.setSelection(Math.min(currentText.length(), Math.max(0, currentPosition - 1)));
						isTechnicalChange = false;
					}
				}
			}
		});
		textField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(final KeyEvent event) {
				// on Enter-key
				if (event.keyCode == SWT.CR || event.keyCode == SwtUtilities.BOTTOM_RIGHT_ENTER_KEY) {
					setReturnValue(textField.getText());
					getParent().close();
				}
			}
		});

		final Composite mainButtonComposite = new Composite(shell, SWT.NONE);
		mainButtonComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		mainButtonComposite.setLayout(SwtUtilities.createNoMarginGridLayout(2, true));

		final Button okButton = new Button(mainButtonComposite, SWT.PUSH);
		okButton.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false));
		okButton.setText(LangResources.get("ok"));
		okButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				setReturnValue(textField.getText());
				getParent().close();
			}
		});

		final Button cancelButton = new Button(mainButtonComposite, SWT.PUSH);
		cancelButton.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false));
		cancelButton.setText(LangResources.get("cancel"));
		cancelButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				setReturnValue(null);
				getParent().close();
			}
		});

		shell.pack();
	}
}
