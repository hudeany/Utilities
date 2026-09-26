package de.soderer.utilities.swt;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;

public class LabeledCombo extends Composite {
	protected DropDown comboBox;
	/** Offered items, needed for the index based API (DropDown itself works on texts only) */
	protected final List<String> items = new ArrayList<>();

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

		// Fixed list: no custom values (replaces the former read-only SWT Combo)
		comboBox = new DropDown(this, style);
		comboBox.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		comboBox.setCaseSensitive(false);
		comboBox.setMatchMode(DropDown.MatchMode.CONTAINS);
		comboBox.setAllowCustomValues(false);
		if (items != null) {
			this.items.addAll(items);
		}
		comboBox.setItems(new ArrayList<>(this.items));
		if (!this.items.isEmpty()) {
			if (preSelectedItemIndex < 0 || preSelectedItemIndex >= this.items.size()) {
				comboBox.setText(this.items.get(this.items.size() - 1));
			} else {
				comboBox.setText(this.items.get(preSelectedItemIndex));
			}
		}

		// Forward only accepted values as SWT.Modify of this composite.
		// Invalid intermediate texts (red) are not forwarded, because listeners typically
		// evaluate getSelectedItemIndex(), which would be -1 then.
		comboBox.addListener(SWT.Selection, e -> {
			final Event event = new Event();
			event.widget = labeledCombo;
			labeledCombo.notifyListeners(SWT.Modify, event);
		});
	}

	public void addModifyListener(final ModifyListener listener) throws Exception {
		checkWidget();
		if (listener == null) {
			throw new Exception("Missing listener");
		} else {
			addTypedListener(listener, SWT.Modify);
		}
	}

	@Override
	public void setEnabled(final boolean enabled) {
		comboBox.setEnabled(enabled);
	}

	/**
	 * Selects the item at the given index.
	 * Out of range indices are ignored, like Combo.select() did before.
	 */
	public void setSelectedItemIndex(final int index) {
		if (index >= 0 && index < items.size()) {
			comboBox.setText(items.get(index));
		}
	}

	/**
	 * Index of the currently shown item, or -1 if the text is currently invalid
	 */
	public int getSelectedItemIndex() {
		final String text = comboBox.getText();
		if (text == null) {
			return -1;
		}

		// Exact match first, then case-insensitive (matching is case-insensitive)
		final int exactIndex = items.indexOf(text);
		if (exactIndex >= 0) {
			return exactIndex;
		}
		for (int i = 0; i < items.size(); i++) {
			if (items.get(i).equalsIgnoreCase(text)) {
				return i;
			}
		}
		return -1;
	}
}
