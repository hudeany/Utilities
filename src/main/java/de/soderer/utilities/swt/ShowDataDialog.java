package de.soderer.utilities.swt;

import java.io.File;
import java.nio.charset.StandardCharsets;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

import de.soderer.utilities.FileUtilities;
import de.soderer.utilities.LangResources;

public class ShowDataDialog extends ModalDialog<Integer> {
	private final String dataString;
	private boolean wordWrap = false;
	private final boolean showSaveButton;

	private int width = 300;
	private int height = 300;

	public ShowDataDialog(final Shell shell, final String title, final String dataString, final boolean showSaveButton) {
		super(shell, SWT.RESIZE, title);
		this.dataString = dataString;
		this.showSaveButton = showSaveButton;
	}

	public ShowDataDialog(final Shell shell, final String title, final String dataString) {
		super(shell, SWT.RESIZE, title);
		this.dataString = dataString;
		showSaveButton = false;
	}

	public void setWordWrap(final boolean value) {
		wordWrap = value;
	}

	@Override
	protected void createComponents(final Shell shell) throws Exception {
		shell.setLayout(new GridLayout(1, true));

		final Composite composite = new Composite(shell, SWT.NONE);
		final GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		composite.setLayoutData(gridData);
		composite.setLayout(SwtUtilities.createNoMarginGridLayout(2, true));

		final StyledText textField = new StyledText(composite, SWT.READ_ONLY | SWT.BORDER | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		textField.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		textField.setText(dataString);
		textField.setWordWrap(wordWrap);

		textField.addKeyListener(new KeyAdapter() {
		    @Override
		    public void keyPressed(final KeyEvent e) {
		        if ((e.stateMask & SWT.MOD1) != 0 && (e.keyCode == 'a' || e.keyCode == 'A')) {
		            textField.selectAll();
		            e.doit = false;
		        } else if ((e.stateMask & SWT.MOD1) != 0 && (e.keyCode == 'c' || e.keyCode == 'C')) {
		            textField.copy();
		            e.doit = false;
		        }
		    }
		});

		if (showSaveButton) {
			final Button buttonSave = new Button(composite, SWT.PUSH);
			buttonSave.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false));
			buttonSave.setText(LangResources.get("save"));
			buttonSave.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent event) {
					saveDataInteractive(getParent(), LangResources.get("savedata"), dataString.getBytes(StandardCharsets.UTF_8));
				}
			});
		}

		final Button buttonClose = new Button(composite, SWT.PUSH);
		buttonClose.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false, (showSaveButton ? 1 : 2), 1));
		buttonClose.setText(LangResources.get("close"));
		buttonClose.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				setReturnValue(0);
				getParent().close();
			}
		});

		shell.pack();
		shell.setSize(width, height);
	}

	public ShowDataDialog setSize(final int width, final int height) {
		this.width = width;
		this.height = height;
		return this;
	}

	public static String saveDataInteractive(final Shell shell, final String title, final byte[] data) {
		try {
			boolean retry = true;
			while (retry) {
				final FileDialog dialog = new FileDialog(shell, SWT.SAVE);
				dialog.setText(title + " " + LangResources.get("saveFile"));
				final String fileName = dialog.open();
				if (fileName != null) {
					if (new File(fileName).exists() && new File(fileName).isDirectory()) {
						final QuestionDialog questionDialog = new QuestionDialog(shell, title, LangResources.get("pathIsDirectory"), LangResources.get("ok"), LangResources.get("cancel"));
						if (questionDialog.open() != 0) {
							retry = false;
						}
					} else {
						if (new File(fileName).exists()) {
							new File(fileName).delete();
						}

						if (retry) {
							FileUtilities.write(new File(fileName), data);
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
		} catch (final Exception e) {
			new QuestionDialog(shell, title, LangResources.get("fileWasNotSaved"), LangResources.get("error") + ":\n" + e.getMessage(), LangResources.get("ok")).setBackgroundColor(SwtColor.LightRed).open();
			return null;
		}
	}
}
