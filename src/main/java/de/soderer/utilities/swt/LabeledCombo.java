package de.soderer.utilities.swt;

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TypedListener;

public class LabeledCombo extends Composite {
	protected Combo comboBox;

	protected LabeledCombo(final Composite parent, final int style) {
		super(parent, style);
	}

	public LabeledCombo(final Composite parent, final int style, final String labelText, final List<String> items, final int preSelectedItemIndex) {
		this(parent, style, labelText, items, preSelectedItemIndex, null);
	}

	public LabeledCombo(final Composite parent, final int style, final String labelText, final List<String> items, final int preSelectedItemIndex, ArrangementStyle arrangementStyle) {
		super(parent, style);

		final LabeledCombo labeledCombo = this;

		if (arrangementStyle == null) {
			arrangementStyle = ArrangementStyle.Vertical;
		}

		setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true, 1, 1));
		setLayout(SwtUtilities.createNoMarginGridLayout(arrangementStyle == ArrangementStyle.Vertical ? 1 : 2, false));

		final Label label = new Label(this, SWT.NONE);
		label.setText(labelText);

		comboBox = new Combo(this, SWT.BORDER | SWT.READ_ONLY | style);
		comboBox.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		comboBox.setText("");
		comboBox.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(final ModifyEvent arg0) {
				final Event event = new Event();
				event.widget = labeledCombo;
				labeledCombo.notifyListeners(SWT.Modify, event);
			}
		});
		if (items != null && items.size() > 0) {
			final String[] comboBoxItems = items.toArray(new String[items.size()]);
			comboBox.setItems(comboBoxItems);
			if (preSelectedItemIndex < 0) {
				comboBox.select(comboBox.getItemCount() - 1);
			} else {
				comboBox.select(preSelectedItemIndex);
			}
		}
	}

	public void addModifyListener(final ModifyListener listener) throws Exception {
		checkWidget();
		if (listener == null) {
			throw new Exception("Missing listener");
		} else {
			final TypedListener typedListener = new TypedListener(listener);
			addListener(SWT.Modify, typedListener);
		}
	}

	@Override
	public void setEnabled(final boolean enabled) {
		comboBox.setEnabled(enabled);
	}

	public void setSelectedItemIndex(final int index) {
		comboBox.select(index);
	}

	public int getSelectedItemIndex() {
		return comboBox.getSelectionIndex();
	}
}
