package de.soderer.utilities.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import de.soderer.utilities.LangResources;
import de.soderer.utilities.Utilities;

public class LabeledFileText extends LabeledText {
	private boolean selectDirectory = false;
	private final Button dataBrowseButton;

	public LabeledFileText(final Shell shell, final Composite parent, final int style, final String labelText, final boolean openExisting) throws Exception {
		this(shell, parent, style, labelText, openExisting, null);
	}

	public LabeledFileText(final Shell shell, final Composite parent, final int style, final String labelText, final boolean openExisting, ArrangementStyle arrangementStyle) throws Exception {
		super(parent, style);

		final LabeledFileText labeledFileText = this;

		final Shell shellForFileDialog = shell;

		if (arrangementStyle == null) {
			arrangementStyle = ArrangementStyle.Vertical;
		}

		final GridLayout gridLayout = SwtUtilities.createNoMarginGridLayout(arrangementStyle == ArrangementStyle.Vertical ? 1 : (labelText == null ? 2 : 3), false);
		gridLayout.marginWidth = 0;
		gridLayout.marginHeight = 0;
		setLayout(gridLayout);

		if (labelText != null) {
			final Label label = new Label(this, SWT.NONE);
			label.setText(labelText);
		}

		textField = new Text(this, SWT.BORDER | style);
		textField.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		textField.setText("");
		textField.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(final ModifyEvent arg0) {
				final Event event = new Event();
				event.widget = labeledFileText;
				labeledFileText.notifyListeners(SWT.Modify, event);
			}
		});

		dataBrowseButton = new Button(this, SWT.PUSH);
		dataBrowseButton.setText(LangResources.get("browse"));
		if (selectDirectory) {
			dataBrowseButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					final DirectoryDialog dialog = new DirectoryDialog(shellForFileDialog, openExisting ? SWT.OPEN : SWT.SAVE);
					dialog.setText(LangResources.get("openDataDirectory"));
					final String fileName = dialog.open();
					if (Utilities.isNotBlank(fileName)) {
						textField.setText(fileName);
					}
				}
			});
		} else {
			dataBrowseButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					final FileDialog dialog = new FileDialog(shellForFileDialog, openExisting ? SWT.OPEN : SWT.SAVE);
					dialog.setText(LangResources.get("openFileDirectory"));
					final String fileName = dialog.open();
					if (Utilities.isNotBlank(fileName)) {
						textField.setText(fileName);
					}
				}
			});
		}
	}

	public void setSelectDirectory(final boolean selectDirectory) {
		this.selectDirectory = selectDirectory;
	}

	@Override
	public void setEnabled(final boolean enabled) {
		textField.setEnabled(enabled);
		dataBrowseButton.setEnabled(enabled);
	}

	@Override
	public String getText() {
		return Utilities.replaceUsersHome(super.getText());
	}
}
