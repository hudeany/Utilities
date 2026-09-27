package de.soderer.utilities.swt;

import java.util.regex.Pattern;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
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
		super(SwtUtilities.createCenteredShellIfNeeded(shell), title);

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

		final StyledText styledText = new StyledText(shell, SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI);
		styledText.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, true));
		styledText.setLayout(SwtUtilities.createSmallMarginGridLayout(1, false));
		styledText.setText(text);
		styledText.setBackground(shell.getBackground());
		styledText.setAlwaysShowScrollBars(false);
		styledText.setEditable(false);

		final Composite textFieldComposite = new Composite(shell, SWT.NONE);
		textFieldComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		textFieldComposite.setLayout(SwtUtilities.createNoMarginGridLayout(1, true));

		textField = new Text(textFieldComposite, SWT.BORDER);
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

		if (shell.getSize().x > 600 || shell.getSize().y > 400) {
			shell.setSize(Math.min(600, shell.getSize().x), Math.min(400, shell.getSize().y));
		}
		if (!styledText.getHorizontalBar().getVisible()) {
			styledText.setSize(styledText.getSize().x, styledText.getSize().y - styledText.getHorizontalBar().getSize().y);
			shell.setSize(shell.getSize().x, shell.getSize().y - styledText.getHorizontalBar().getSize().y);
		}
		textField.forceFocus();
	}
}
