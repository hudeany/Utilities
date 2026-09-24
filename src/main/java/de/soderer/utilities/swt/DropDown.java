package de.soderer.utilities.swt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.function.Consumer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import de.soderer.utilities.Utilities;

/**
 * An SWT combo with autocomplete support.
 *
 * <p>
 * Supported features:
 * </p>
 * <ul>
 * <li>STARTS_WITH or CONTAINS search</li>
 * <li>Case-sensitive / insensitive</li>
 * <li>Suggestion is displayed in light gray</li>
 * <li>Multiple matches selectable via arrow keys</li>
 * <li>PageUp / PageDown</li>
 * <li>Tab and Enter accept the suggestion</li>
 * <li>Free input optional</li>
 * <li>With exactly one match and free input disabled: automatic acceptance</li>
 * <li>With no match and free input disabled: input is kept and displayed in
 * red</li>
 * <li>Selection event only fires when a value was actually accepted</li>
 * <li>ValidationListener fires whenever the valid/invalid state changes</li>
 * <li>Delete key removes the selected entry from the popup (optional)</li>
 * <li>Modify events are fired via the Composite</li>
 * </ul>
 */
public class DropDown extends Composite {
	/**
	 * Search mode.
	 */
	public enum MatchMode {
		STARTS_WITH,
		CONTAINS
	}

	private final StyledText text;
	private final Button dropdownButton;
	private final Composite dropdownButtonBorder;

	private Shell popup;
	private List popupList;

	private final java.util.List<String> items = new ArrayList<>();

	/**
	 * Current matches.
	 */
	private java.util.List<String> matches = Collections.emptyList();

	/**
	 * Currently selected match.
	 */
	private int selectedMatch = -1;

	/**
	 * Text entered by the user.
	 */
	private String userText = "";

	/**
	 * Last validly accepted value.
	 */
	private String acceptedText = "";

	/**
	 * Prevents recursive processing of our own setText() calls.
	 */
	private boolean updating;

	/**
	 * Set via VerifyListener when a change actually comes from the user.
	 */
	private boolean userEdit;

	/**
	 * Start offset of a currently displayed, unselected gray suggestion tail (see
	 * renderText()), or -1 if none is shown. Lets the VerifyListener transparently
	 * consume that tail on the next keystroke - as if it had been selected -
	 * without ever actually showing a text selection for it.
	 */
	private int suggestionTailStart = -1;

	/**
	 * Explicit caret offset to apply on the next renderText() call, or -1 to fall
	 * back to the widget's current (real) caret offset.
	 *
	 * Needed because the VerifyListener sometimes cancels the native edit
	 * (e.doit = false) and applies the change to "userText" itself instead, e.g.
	 * when typing into or backspacing over the gray suggestion tail. In that case
	 * the widget's own caret offset is never updated by SWT and no longer
	 * reflects where the caret should end up, so renderText() cannot simply read
	 * text.getCaretOffset().
	 */
	private int pendingCaretOffset = -1;

	/**
	 * Current validity state.
	 */
	private boolean valid = true;

	/**
	 * Are values allowed that are not in the list?
	 */
	private boolean allowCustomValues = false;

	/**
	 * Consider case sensitivity?
	 */
	private boolean caseSensitive = false;

	/**
	 * Search mode.
	 */
	private MatchMode matchMode = MatchMode.STARTS_WITH;

	private final Color normalColor;
	private final Color suggestionColor;
	private final Color invalidColor;

	/**
	 * Placeholder/hint text shown in light gray while the field is empty
	 * - the {@link StyledText}-based equivalent of
	 * {@link org.eclipse.swt.widgets.Text#setMessage(String)}, which
	 * StyledText itself has no native support for. Purely a paint-time
	 * overlay (see the PaintListener in the constructor) - never part of
	 * the actual editable content, so it never interacts with
	 * "userText", the suggestion logic, or the VerifyListener.
	 */
	private String hintText;

	/**
	 * Whether entries can be reordered via drag&amp;drop in the popup.
	 */
	private boolean reorderable = false;

	/**
	 * Notified with the new item order whenever the user reorders entries via
	 * drag&amp;drop in the popup.
	 */
	private Consumer<java.util.List<String>> itemsReorderedListener;

	/**
	 * Whether the currently selected entry in the popup can be removed from the
	 * item list via the Delete key.
	 */
	private boolean deletable = false;

	/**
	 * Notified with the value of the entry the user just removed via the Delete
	 * key in the popup (see {@link #isDeletable()}).
	 */
	private Consumer<String> itemDeletedListener;

	/**
	 * Notified whenever the validity state (see {@link #isValidValue()}) actually
	 * changes, in either direction (valid -&gt; invalid or invalid -&gt; valid).
	 *
	 * <p>
	 * Unlike the {@link SelectionListener}, which only fires when a value was
	 * actually accepted, this fires for every change of the red/invalid display
	 * state, so callers can react immediately when the effective value (see
	 * {@link #getText()}) becomes {@code null}.
	 * </p>
	 */
	private Consumer<Boolean> validationListener;

	private Composite insertionMarker;
	private int dragSourceIndex = -1;
	private boolean dragInProgress = false;
	private int insertionIndex = -1;

	/**
	 * Maximum pointer movement (in pixels) between mouse-down and mouse-up on a
	 * popup entry that still counts as a plain click rather than a drag&amp;drop
	 * reorder attempt.
	 */
	private static final int CLICK_MOVEMENT_THRESHOLD_PX = 4;

	/**
	 * Page size for PageUp/PageDown while no popup is shown.
	 */
	private static final int DEFAULT_PAGE_SIZE = 10;

	private int mouseDownX = -1;
	private int mouseDownY = -1;

	/**
	 * Closes the popup on a mouse click anywhere outside of it and outside of this
	 * combo's own input field. Registered on the Display, since a click can land on
	 * any widget in the application, not just ours.
	 */
	private final Listener outsideClickFilter;

	/**
	 * Deferred hide triggered by the owning Shell's SWT.Deactivate.
	 *
	 * <p>
	 * Not called directly - see the SWT.Deactivate listener in the constructor for
	 * why this needs to run delayed rather than immediately.
	 * </p>
	 */
	private final Runnable deferredDeactivateHide = this::hidePopup;

	/**
	 * Creates an AutoCompleteCombo.
	 *
	 * @param parent parent composite
	 * @param style  SWT style
	 */
	public DropDown(final Composite parent, final int style) {
		super(parent, style);

		final GridLayout gridLayout = new GridLayout(2, false);
		gridLayout.marginWidth = 0;
		gridLayout.marginHeight = 0;
		gridLayout.horizontalSpacing = 0;

		setLayout(gridLayout);

		text = new StyledText(this, SWT.SINGLE | SWT.BORDER);
		text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		normalColor = text.getForeground();
		suggestionColor = new Color(getDisplay(), 170, 170, 170);
		invalidColor = new Color(getDisplay(), 220, 70, 70);

		/*
		 * Placeholder text overlay - painted only when the field is
		 * genuinely empty (no real content, no gray suggestion tail
		 * either). Drawn directly via GC rather than put into the
		 * StyledText content itself, so it can never be edited into,
		 * selected, or accidentally consumed by the suggestion-tail
		 * handling in the VerifyListener - exactly like the native
		 * Text widget's own message/placeholder behaves.
		 */
		text.addPaintListener(e -> {
			if (Utilities.isEmpty(hintText) || text.getCharCount() > 0) {
				return;
			}

			e.gc.setForeground(suggestionColor);
			e.gc.drawString(hintText, 2, 0, true);
		});

		dropdownButtonBorder = new Composite(this, SWT.BORDER);
		final GridLayout dropdownButtonBorderLayout = new GridLayout(1, false);
		dropdownButtonBorderLayout.marginWidth = 0;
		dropdownButtonBorderLayout.marginHeight = 0;
		dropdownButtonBorder.setLayout(dropdownButtonBorderLayout);
		final GridData dropdownButtonBorderLayoutData = new GridData(SWT.FILL, SWT.FILL, false, true);
		dropdownButtonBorderLayoutData.widthHint = 20;
		dropdownButtonBorder.setLayoutData(dropdownButtonBorderLayoutData);

		dropdownButton = new Button(dropdownButtonBorder, SWT.ARROW | SWT.DOWN);
		dropdownButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		dropdownButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				/*
				 * Clicking this button takes keyboard focus away from "text" on
				 * mouseDown, before this Selection event even fires - which triggers
				 * "text"'s FocusLost handler below and schedules a deferred hidePopup()
				 * roughly 50ms from now (see deferredDeactivateHide). Left unchecked,
				 * that would auto-close the popup we are about to open here shortly
				 * after it appears, regardless of text.setFocus() below - restoring
				 * focus does not itself cancel an already-scheduled timerExec(). Cancel
				 * it first, the same way popupList's own mouseDown handler already does
				 * for the equivalent case.
				 */
				getDisplay().timerExec(-1, deferredDeactivateHide);

				showAllItems();

				/*
				 * A Button takes keyboard focus on click, so without this the arrow keys,
				 * Delete and Escape would go to "dropdownButton" instead of "text" -
				 * silently doing nothing, since only "text" has the KeyListener that
				 * drives popup navigation/deletion (see handleKey()).
				 */
				text.setFocus();
			}
		});

		text.addVerifyListener(new VerifyListener() {
			@Override
			public void verifyText(final VerifyEvent e) {
				if (!updating) {
					userEdit = true;
				}

				if (suggestionTailStart >= 0 && e.start == e.end && e.start == suggestionTailStart) {
					e.doit = false;

					final String typed = e.text;
					final int insertionPoint = suggestionTailStart;

					suggestionTailStart = -1;

					getDisplay().asyncExec(() -> {
						if (text.isDisposed()) {
							return;
						}

						userText = userText + typed;
						pendingCaretOffset = insertionPoint + typed.length();

						processUserInput();
					});

					return;
				}

				/*
				 * Backspace pressed right at the boundary between the
				 * real input and the gray suggestion tail: the same
				 * problem as above applies in reverse. The default
				 * deletion would remove a character from the
				 * DISPLAYED suggestion text (which may not even be
				 * the last character the user actually typed - see
				 * matchStart > 0 in renderText()), not from the real
				 * "userText". So this is intercepted the same way:
				 * cancel the default edit and shrink "userText" by
				 * one character ourselves, then re-search/re-render
				 * from that.
				 */
				if (suggestionTailStart >= 0 &&
					Utilities.isEmpty(e.text) &&
					e.end == suggestionTailStart &&
					e.start == suggestionTailStart - 1) {

					e.doit = false;

					final int deletionPoint = suggestionTailStart - 1;

					suggestionTailStart = -1;

					getDisplay().asyncExec(() -> {
						if (text.isDisposed()) {
							return;
						}

						userText = userText.isEmpty()
							? ""
							: userText.substring(0, userText.length() - 1);
						pendingCaretOffset = deletionPoint;

						processUserInput();
					});

					return;
				}

				suggestionTailStart = -1;
			}
		});

		text.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(final KeyEvent e) {
				handleKey(e);
			}
		});

		text.addModifyListener(e -> {
			if (updating) {
				return;
			}

			userText = text.getText();

			if (userEdit) {
				userEdit = false;
				processUserInput();
			} else {
				updateMatches(false);
			}
		});

		/*
		 * Close the popup on focus loss.
		 *
		 * Deferred, not immediate - clicking into "popupList" itself
		 * moves keyboard focus away from "text" first, firing this
		 * BEFORE the click's mouseUp (and therefore acceptValue())
		 * on popupList ever gets a chance to run. An immediate
		 * hidePopup() here closed the popup right away, silently
		 * discarding the click before it could be accepted. Reusing
		 * the same short grace period already used for the Shell's
		 * SWT.Deactivate case lets popupList.mouseDown() (below)
		 * cancel this exactly the same way it already cancels that
		 * one.
		 */
		text.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(final FocusEvent e) {
				getDisplay().timerExec(50, deferredDeactivateHide);
			}
		});

		/*
		 * Close the popup when the owning application window itself loses focus (e.g.
		 * Alt-Tab to another application).
		 *
		 * The popup Shell uses SWT.ON_TOP, which keeps it floating above every window
		 * system-wide, independent of this application's focus. Without this,
		 * deactivating the app would leave the popup visibly stranded on top of
		 * whatever window the user switched to, while the rest of this application's UI
		 * drops behind it.
		 *
		 * On Windows, WM_ACTIVATE (-> SWT.Deactivate) for this owning Shell
		 * structurally fires BEFORE WM_LBUTTONDOWN is even delivered for a click
		 * landing on "popupList" - that's how Windows processes clicks on another
		 * top-level window, regardless of SWT.NO_FOCUS / WS_EX_NOACTIVATE on the popup.
		 * So at the moment this listener runs, there is no reliable way to tell a real
		 * "switched to another application" deactivation apart from this spurious one
		 * caused by merely clicking our own popup list - hiding immediately closed the
		 * popup before mouseDown/mouseUp on "popupList" ever got a chance to run.
		 * Instead, defer the hide by a short grace period; the mouseDown handler below
		 * cancels it if it turns out to be this spurious case.
		 */
		getShell().addListener(SWT.Deactivate, e -> getDisplay().timerExec(50, deferredDeactivateHide));

		/*
		 * Close the popup on a click anywhere outside of it and outside of this combo's
		 * own input field.
		 *
		 * A focus change alone (handled above) does not cover every case: clicking on a
		 * non-focusable area of the same application (e.g. a Label, or empty space in a
		 * Composite) does not move keyboard focus away from "text", so the popup would
		 * otherwise stay open.
		 */
		outsideClickFilter = event -> {
			if (popup == null || popup.isDisposed() || !popup.isVisible()) {
				return;
			}

			if (!(event.widget instanceof Control)) {
				return;
			}

			final Control clickedControl = (Control) event.widget;

			if (isDescendantOf(clickedControl, popup) || isDescendantOf(clickedControl, this)) {
				return;
			}

			hidePopup();
		};

		getDisplay().addFilter(SWT.MouseDown, outsideClickFilter);

		/*
		 * Mouse click on the field: Open the popup if there are multiple matches.
		 */
		text.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(final MouseEvent e) {
				if (!matches.isEmpty() && matches.size() > 1) {
					showPopup();
				}
			}
		});

		/*
		 * Release resources.
		 */
		addListener(SWT.Dispose, e -> disposeResources());
	}

	/**
	 * Sets the values of the suggestion list.
	 */
	public void setItems(final String... values) {
		checkWidget();

		items.clear();

		if (values != null) {
			items.addAll(Arrays.asList(values));
		}

		selectedMatch = -1;

		updateMatches(false);
	}

	/**
	 * Sets the values of the suggestion list and returns this instance for
	 * chaining.
	 */
	public DropDown withItems(final String... values) {
		setItems(values);

		return this;
	}

	/**
	 * Sets the values of the suggestion list.
	 */
	public void setItems(final java.util.List<String> values) {
		checkWidget();

		items.clear();

		if (values != null) {
			items.addAll(values);
		}

		selectedMatch = -1;

		updateMatches(false);
	}

	/**
	 * Sets the values of the suggestion list and returns this instance for
	 * chaining.
	 */
	public DropDown withItems(final java.util.List<String> values) {
		setItems(values);

		return this;
	}

	/**
	 * Adds a value to the suggestion list.
	 */
	public void add(final String value) {
		checkWidget();

		if (value == null) {
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		}

		items.add(value);

		updateMatches(false);
	}

	/**
	 * Removes a value by its index.
	 */
	public void remove(final int index) {
		checkWidget();

		items.remove(index);

		updateMatches(false);
	}

	/**
	 * Removes a value.
	 */
	public void remove(final String value) {
		checkWidget();

		items.remove(value);

		updateMatches(false);
	}

	/**
	 * Removes all values.
	 */
	public void removeAll() {
		checkWidget();

		items.clear();
		matches = Collections.emptyList();
		selectedMatch = -1;

		hidePopup();
	}

	/**
	 * Returns all suggestion values.
	 */
	public String[] getItems() {
		checkWidget();

		return items.toArray(new String[0]);
	}

	/**
	 * Returns the number of suggestion values.
	 */
	public int getItemCount() {
		checkWidget();

		return items.size();
	}

	/**
	 * Returns the current text.
	 *
	 * <p>
	 * Returns {@code null} if custom values are not allowed and the current text
	 * does not match any item (i.e. it is shown in red / {@link #isValidValue()}
	 * would return {@code false}).
	 * </p>
	 */
	public String getText() {
		checkWidget();

		if (!allowCustomValues && !valid) {
			return null;
		}

		return userText;
	}

	/**
	 * Sets the text programmatically.
	 *
	 * <p>
	 * Important: no autocomplete suggestion is automatically accepted while doing
	 * this.
	 * </p>
	 */
	public void setText(final String value) {
		checkWidget();

		if (value == null) {
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
			return;
		}

		// A programmatic value always places the caret at its end
		pendingCaretOffset = -1;

		updating = true;

		try {
			userText = value;
			acceptedText = value;
			setValid(true);

			text.setText(value);
			text.setStyleRange(null);
			text.setSelection(value.length());

		} finally {
			updating = false;
		}

		selectedMatch = -1;

		updateMatches(false);
	}

	/**
	 * Sets the text programmatically and returns this instance for chaining.
	 *
	 * <p>
	 * Important: no autocomplete suggestion is automatically accepted while doing
	 * this.
	 * </p>
	 */
	public DropDown withText(final String value) {
		setText(value);

		return this;
	}

	/**
	 * Checks whether the current text is valid.
	 */
	public boolean isValidValue() {
		checkWidget();

		return valid;
	}

	/**
	 * Are free-form values allowed?
	 */
	public boolean isAllowCustomValues() {
		return allowCustomValues;
	}

	/**
	 * Configures whether values outside the suggestion list are allowed.
	 */
	public void setAllowCustomValues(final boolean allowCustomValues) {
		checkWidget();

		this.allowCustomValues = allowCustomValues;

		updateMatches(false);
	}

	/**
	 * Configures whether values outside the suggestion list are allowed and
	 * returns this instance for chaining.
	 */
	public DropDown withAllowCustomValues(final boolean allowCustomValuesParam) {
		setAllowCustomValues(allowCustomValuesParam);

		return this;
	}

	/**
	 * Is the search case-sensitive?
	 */
	public boolean isCaseSensitive() {
		return caseSensitive;
	}

	/**
	 * Sets case sensitivity.
	 */
	public void setCaseSensitive(final boolean caseSensitive) {
		checkWidget();

		this.caseSensitive = caseSensitive;

		updateMatches(false);
	}

	/**
	 * Sets case sensitivity and returns this instance for chaining.
	 */
	public DropDown withCaseSensitive(final boolean caseSensitiveValue) {
		setCaseSensitive(caseSensitiveValue);

		return this;
	}

	/**
	 * Returns the search mode.
	 */
	public MatchMode getMatchMode() {
		return matchMode;
	}

	/**
	 * Sets the search mode.
	 */
	public void setMatchMode(final MatchMode matchMode) {
		checkWidget();

		if (matchMode == null) {
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		}

		this.matchMode = matchMode;

		updateMatches(false);
	}

	/**
	 * Sets the search mode and returns this instance for chaining.
	 */
	public DropDown withMatchMode(final MatchMode matchModeValue) {
		setMatchMode(matchModeValue);

		return this;
	}

	/**
	 * Whether entries can be reordered via drag&amp;drop in the popup.
	 *
	 * <p>
	 * Reordering is only possible while the popup shows the full, unfiltered item
	 * list (i.e. the input text matches every item).
	 * </p>
	 */
	public boolean isReorderable() {
		return reorderable;
	}

	/**
	 * Enables or disables drag&amp;drop reordering of entries in the popup.
	 */
	public void setReorderable(final boolean reorderable) {
		checkWidget();

		this.reorderable = reorderable;
	}

	/**
	 * Enables or disables drag&amp;drop reordering of entries in the popup and
	 * returns this instance for chaining.
	 */
	public DropDown withReorderable(final boolean reorderableValue) {
		setReorderable(reorderableValue);

		return this;
	}

	/**
	 * Whether the currently selected entry in the popup can be removed from the
	 * item list via the Delete key.
	 */
	public boolean isDeletable() {
		return deletable;
	}

	/**
	 * Enables or disables removing the currently selected popup entry via the
	 * Delete key.
	 */
	public void setDeletable(final boolean deletable) {
		checkWidget();

		this.deletable = deletable;
	}

	/**
	 * Enables or disables removing the currently selected popup entry via the
	 * Delete key and returns this instance for chaining.
	 */
	public DropDown withDeletable(final boolean deletableValue) {
		setDeletable(deletableValue);

		return this;
	}

	/**
	 * Disabling this Composite alone would not, on every platform,
	 * visually gray out its children the way a native SWT
	 * {@link org.eclipse.swt.widgets.Combo} does when disabled - so
	 * this explicitly propagates to the text field and the dropdown
	 * button, and also grays the field's background/foreground to
	 * match the native disabled look, since a StyledText has no
	 * built-in disabled appearance of its own.
	 */
	@Override
	public void setEnabled(final boolean enabled) {
		checkWidget();

		super.setEnabled(enabled);

		text.setEnabled(enabled);
		dropdownButton.setEnabled(enabled);

		if (enabled) {
			text.setBackground(null);

			/*
			 * Re-render from the real current state, rather than just
			 * restoring the style range colors in place, so the
			 * field's content and coloring are correct even if
			 * something changed (e.g. via setText()/setItems()) while
			 * it was disabled.
			 */
			updateMatches(false);
		} else {
			hidePopup();

			text.setBackground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));

			/*
			 * Uniformly gray, deliberately not preserving the
			 * distinction between typed and suggested text here -
			 * that distinction only matters while the field is
			 * interactive.
			 */
			final StyleRange disabledStyle = new StyleRange();
			disabledStyle.start = 0;
			disabledStyle.length = text.getCharCount();
			disabledStyle.foreground = suggestionColor;
			text.setStyleRange(disabledStyle);
		}
	}

	/**
	 * Enables or disables this control and returns this instance for chaining.
	 */
	public DropDown withEnabled(final boolean enabled) {
		setEnabled(enabled);

		return this;
	}

	/**
	 * Sets a hint/placeholder text shown in light gray while the field is
	 * empty, mirroring {@link org.eclipse.swt.widgets.Text#setMessage(String)}
	 * - StyledText (which this control is built on) has no built-in
	 * equivalent. Pass {@code null} or an empty string to remove it.
	 */
	public void setMessage(final String message) {
		checkWidget();

		hintText = message;

		text.redraw();
	}

	/**
	 * Sets a hint/placeholder text and returns this instance for chaining.
	 */
	public DropDown withMessage(final String message) {
		setMessage(message);

		return this;
	}

	/**
	 * Returns whether the built-in dropdown "browse all" button is shown next to
	 * the text field.
	 */
	public boolean isShowDropdownButton() {
		checkWidget();

		return dropdownButtonBorder.getVisible();
	}

	/**
	 * Shows or hides the built-in dropdown "browse all" button next to the text
	 * field. Shown by default.
	 */
	public void setShowDropdownButton(final boolean show) {
		checkWidget();

		dropdownButtonBorder.setVisible(show);

		((GridData) dropdownButtonBorder.getLayoutData()).exclude = !show;

		layout();
	}

	/**
	 * Shows or hides the built-in dropdown "browse all" button and returns this
	 * instance for chaining.
	 */
	public DropDown withShowDropdownButton(final boolean show) {
		setShowDropdownButton(show);

		return this;
	}

	/**
	 * Registers a listener that is notified with the new item order whenever the
	 * user reorders entries via drag&amp;drop in the popup.
	 */
	public void addItemsReorderedListener(final Consumer<java.util.List<String>> listener) {
		itemsReorderedListener = listener;
	}

	/**
	 * Registers a listener that is notified with the value of the entry the user
	 * just removed via the Delete key in the popup (see {@link #isDeletable()}).
	 */
	public void addItemDeletedListener(final Consumer<String> listener) {
		itemDeletedListener = listener;
	}

	/**
	 * Registers a listener that is notified whenever the validity state (see
	 * {@link #isValidValue()}) actually changes, in either direction.
	 *
	 * <p>
	 * Unlike {@link #addSelectionListener(SelectionListener)}, which only fires
	 * when a value was actually accepted, this fires immediately whenever the
	 * text switches to/from the invalid (red) display state - in particular when
	 * {@link #getText()} switches to/from returning {@code null} (only relevant
	 * while {@link #isAllowCustomValues()} is {@code false}).
	 * </p>
	 */
	public void addValidationListener(final Consumer<Boolean> listener) {
		validationListener = listener;
	}

	/**
	 * Adds a listener notified when a value is accepted by the user -
	 * same {@code widgetSelected}/{@code widgetDefaultSelected} contract
	 * as a standard SWT {@link Combo}. Internally just a typed wrapper
	 * around the existing {@code SWT.Selection} event (see
	 * {@link #fireSelectionEvent()}), for callers who'd rather not go
	 * through {@code addListener(SWT.Selection, ...)} directly.
	 */
	public void addSelectionListener(final SelectionListener listener) {
		checkWidget();

		if (listener == null) {
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		}

		addTypedListener(listener, SWT.Selection, SWT.DefaultSelection);
	}

	/**
	 * Removes a listener previously added via
	 * {@link #addSelectionListener(SelectionListener)}.
	 */
	public void removeSelectionListener(final SelectionListener listener) {
		checkWidget();

		if (listener == null) {
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		}

		removeTypedListener(SWT.Selection, listener);
		removeTypedListener(SWT.DefaultSelection, listener);
	}

	/**
	 * Shows the popup with the full, unfiltered item list, regardless of the
	 * current input text.
	 *
	 * <p>
	 * Useful for a "browse all" affordance (e.g. a dropdown arrow button placed
	 * next to the field), and the only way to trigger drag&amp;drop reordering,
	 * since reordering requires the popup to show every item.
	 * </p>
	 */
	public void showAllItems() {
		checkWidget();

		if (items.isEmpty()) {
			return;
		}

		matches = new ArrayList<>(items);
		selectedMatch = -1;

		showPopup();
	}

	/**
	 * Selects the entire text.
	 */
	public void selectAll() {
		checkWidget();

		text.selectAll();
	}

	/**
	 * Returns the current text selection.
	 */
	public Point getSelection() {
		checkWidget();

		return text.getSelection();
	}

	@Override
	public boolean setFocus() {
		checkWidget();

		return text.setFocus();
	}

	/**
	 * Returns the internal StyledText control.
	 *
	 * <p>
	 * Can be useful when special SWT functionality is needed.
	 * </p>
	 */
	public StyledText getTextControl() {
		checkWidget();

		return text;
	}

	/**
	 * Processes an actual user input.
	 */
	private void processUserInput() {
		selectedMatch = -1;

		updateMatches(true);
	}

	/**
	 * Search for matching values.
	 *
	 * @param userInitiated true if the change came from the user
	 */
	private void updateMatches(final boolean userInitiated) {
		if (updating) {
			return;
		}

		if (userText.isEmpty()) {
			matches = Collections.emptyList();
			selectedMatch = -1;

			setValid(allowCustomValues || acceptedText.isEmpty());

			renderText(userText, null);

			hidePopup();

			return;
		}

		final String search = normalize(userText);

		final java.util.List<String> result = new ArrayList<>();
		for (final String item : items) {
			if (item != null) {
				final String candidate = normalize(item);
				if (matchMode == MatchMode.STARTS_WITH) {
					if (candidate.startsWith(search)) {
						result.add(item);
					}
				} else {
					if (candidate.contains(search)) {
						result.add(item);
					}
				}
			}
		}

		matches = result;

		if (matches.isEmpty()) {
			selectedMatch = -1;

			if (allowCustomValues) {
				setValid(true);
				renderText(userText, null);
			} else {
				setValid(false);
				renderInvalidText();
			}

			hidePopup();

			return;
		} else if (!allowCustomValues && matches.size() == 1 && userInitiated) {
			acceptValue(matches.get(0));

			return;
		} else {
			if (selectedMatch < 0 || selectedMatch >= matches.size()) {
				/*
				 * Prefer an exact match over the first (arbitrary, item-order-dependent)
				 * substring/prefix match - otherwise, under MatchMode.CONTAINS, typing or
				 * re-applying (e.g. via setText()) a value that is itself a complete item
				 * but also happens to be a substring of an earlier item in the list (e.g.
				 * "SIT-1" is contained in "PRESIT-1") would display that unrelated, longer
				 * item instead of the one actually typed/selected.
				 */
				final int exactMatchIndex = findExactMatchIndex(matches, search);
				selectedMatch = exactMatchIndex >= 0 ? exactMatchIndex : 0;
			}

			final String suggestion = matches.get(selectedMatch);

			setValid(true);

			renderText(userText, suggestion);

			/*
			 * Only pop the list open for genuine user interaction (typing). A
			 * non-interactive update - setText(), setItems(), setMatchMode(),
			 * setEnabled(true), etc. - must stay purely visual, per their documented
			 * contract ("no autocomplete suggestion is automatically accepted");
			 * otherwise re-applying an already-accepted value from outside code (e.g.
			 * on focus-lost of some other control) would spontaneously reopen the
			 * popup on a field the user never even clicked.
			 */
			if (matches.size() > 1 && userInitiated) {
				showPopup();
			} else {
				hidePopup();
			}
		}
	}

	private String normalize(final String value) {
		if (caseSensitive) {
			return value;
		} else {
			return value.toLowerCase();
		}
	}

	private void renderText(final String input, final String suggestion) {
		/*
		 * Prefer an explicitly recorded target caret offset (set when the
		 * VerifyListener canceled the native edit and applied the change to
		 * "userText" itself instead - see pendingCaretOffset). Otherwise the edit
		 * was applied natively and the widget's own caret offset already reflects
		 * the correct position.
		 *
		 * Consumed unconditionally, BEFORE the empty-input check: previously the
		 * empty-input branch left it untouched, so e.g. a Backspace over the
		 * suggestion-tail boundary that emptied the field left a stale offset
		 * behind, which the next render then applied to an unrelated native edit
		 * (caret jumped back to 0, following keystrokes were inserted in reverse).
		 */
		final int caretBeforeUpdate = pendingCaretOffset >= 0
			? pendingCaretOffset
			: text.getCaretOffset();
		pendingCaretOffset = -1;

		updating = true;
		try {
			if (Utilities.isEmpty(input)) {
				text.setText("");
				suggestionTailStart = -1;
			} else {
				String displayText = input;

				int matchStart = -1;
				int matchLength = 0;

				if (suggestion != null) {
					final String normalizedInput = normalize(input);
					final String normalizedSuggestion = normalize(suggestion);
					final int foundAt = normalizedSuggestion.indexOf(normalizedInput);

					matchStart = foundAt;
					displayText = suggestion;
					matchLength = input.length();
				}

				text.setText(displayText);

				final StyleRange normal = new StyleRange();
				normal.start = 0;
				normal.length = displayText.length();
				normal.foreground = normalColor;
				text.setStyleRange(normal);

				if (matchStart >= 0) {
					final StyleRange before = new StyleRange();
					before.start = 0;
					before.length = matchStart;
					before.foreground = suggestionColor;
					text.setStyleRange(before);

					final int afterStart = matchStart + matchLength;
					if (afterStart < displayText.length()) {
						final StyleRange after = new StyleRange();
						after.start = afterStart;
						after.length = displayText.length() - afterStart;
						after.foreground = suggestionColor;
						text.setStyleRange(after);
					}

					text.setSelection(afterStart);
					suggestionTailStart = afterStart;
				} else if (suggestion == null) {
					final int caretPosition = Math.min(caretBeforeUpdate, displayText.length());
					text.setSelection(caretPosition);
					suggestionTailStart = -1;
				} else {
					text.setSelection(displayText.length());
					suggestionTailStart = displayText.length();
				}
			}
		} finally {
			updating = false;
		}
	}

	/**
	 * Renders an invalid text.
	 *
	 * <p>
	 * Keeps the caret where the user's edit put it (same rules as in
	 * renderText()), so editing in the middle of an invalid (red) text does not
	 * make the caret jump to the end on every keystroke.
	 * </p>
	 */
	private void renderInvalidText() {
		// Must be read before text.setText(), which resets the widget's caret
		final int caretBeforeUpdate = pendingCaretOffset >= 0
			? pendingCaretOffset
			: text.getCaretOffset();
		pendingCaretOffset = -1;

		updating = true;
		try {
			text.setText(userText);

			final StyleRange invalid = new StyleRange();
			invalid.start = 0;
			invalid.length = userText.length();
			invalid.foreground = invalidColor;
			text.setStyleRange(invalid);

			text.setSelection(Math.max(0, Math.min(caretBeforeUpdate, userText.length())));

			suggestionTailStart = -1;
		} finally {
			updating = false;
		}
	}

	private void handleKey(final KeyEvent e) {
		switch (e.keyCode) {
			case SWT.ARROW_DOWN:
				if (matches.isEmpty() && userText.isEmpty() && !items.isEmpty()) {
					openFullListFromKeyboard(0);

					e.doit = false;
				} else if (!matches.isEmpty()) {
					selectNextMatch();

					e.doit = false;
				}
				break;
			case SWT.ARROW_UP:
				if (matches.isEmpty() && userText.isEmpty() && !items.isEmpty()) {
					openFullListFromKeyboard(items.size() - 1);

					e.doit = false;
				} else if (!matches.isEmpty()) {
					selectPreviousMatch();

					e.doit = false;
				}
				break;
			case SWT.PAGE_DOWN:
				if (!matches.isEmpty()) {
					selectPage(1);

					e.doit = false;
				}
				break;
			case SWT.PAGE_UP:
				if (!matches.isEmpty()) {
					selectPage(-1);

					e.doit = false;
				}
				break;
			case SWT.TAB:
				if (acceptCurrentSuggestion()) {
					/*
					 * We let Tab proceed so that focus moves to the next control.
					 */
					e.doit = true;
				} else if (!allowCustomValues && !valid) {
					/*
					 * No match: focus stays in the field.
					 */
					e.doit = false;

					text.setFocus();
				}
				break;
			case SWT.CR:
			case SWT.KEYPAD_CR:
				if (acceptCurrentSuggestion()) {
					e.doit = false;
				} else if (!allowCustomValues && !valid) {
					e.doit = false;

					text.setFocus();
				}
				break;
			case SWT.ESC:
				hidePopup();

				e.doit = true;

				break;
			case SWT.DEL:
				if (deletable && isPopupVisibleWithSelection()) {
					deleteSelectedPopupEntry();

					e.doit = false;
				}
				break;
			default:
				break;
		}
	}

	/**
	 * Whether the popup is currently open and showing a valid selection, i.e.
	 * whether a Delete key press should be interpreted as "remove the selected
	 * popup entry" rather than as normal text editing.
	 */
	private boolean isPopupVisibleWithSelection() {
		return popup != null && !popup.isDisposed() && popup.getVisible() && selectedMatch >= 0 && selectedMatch < matches.size();
	}

	/**
	 * Removes the currently selected popup entry from "items" and "matches",
	 * updates the popup list and selection accordingly, and notifies
	 * {@link #itemDeletedListener}. Called from {@link #handleKey(KeyEvent)} when
	 * the Delete key is pressed while {@link #deletable} is enabled and the popup
	 * shows a valid selection (see {@link #isPopupVisibleWithSelection()}).
	 */
	private void deleteSelectedPopupEntry() {
		if (!isPopupVisibleWithSelection()) {
			return;
		}

		final int deletedIndex = selectedMatch;

		final String deletedItem = matches.get(deletedIndex);

		items.remove(deletedItem);
		matches.remove(deletedIndex);
		popupList.remove(deletedIndex);

		if (matches.isEmpty()) {
			/*
			 * No items left to show a selection for - fully reset via the public
			 * setText() path (clears userText/acceptedText, recomputes validity and
			 * hides the popup) rather than leaving the field showing the just-deleted
			 * entry's now-stale text.
			 */
			setText("");
		} else {
			selectedMatch = Math.min(deletedIndex, matches.size() - 1);

			renderText(userText, matches.get(selectedMatch));

			selectPopupItem();

			positionPopup();
		}

		if (itemDeletedListener != null) {
			itemDeletedListener.accept(deletedItem);
		}
	}

	/**
	 * Opens the popup showing the full, unfiltered item list, for an arrow key
	 * press while no text has been typed yet (so "matches" - which updateMatches()
	 * only ever populates for non-empty input - is still empty). Mirrors how a
	 * normal dropdown combo behaves when opened via the keyboard.
	 */
	private void openFullListFromKeyboard(final int startIndex) {
		matches = new ArrayList<>(items);

		selectedMatch = Math.max(0, Math.min(startIndex, matches.size() - 1));

		setValid(true);

		renderText(userText, matches.get(selectedMatch));

		showPopup();

		selectPopupItem();
	}

	/**
	 * Selects the next match.
	 */
	private void selectNextMatch() {
		if (matches.isEmpty()) {
			return;
		}

		selectedMatch++;

		if (selectedMatch >= matches.size()) {
			selectedMatch = 0;
		}

		renderText(userText, matches.get(selectedMatch));

		selectPopupItem();
	}

	/**
	 * Selects the previous match.
	 */
	private void selectPreviousMatch() {
		if (matches.isEmpty()) {
			return;
		}

		selectedMatch--;

		if (selectedMatch < 0) {
			selectedMatch = matches.size() - 1;
		}

		renderText(userText, matches.get(selectedMatch));

		selectPopupItem();
	}

	/**
	 * Jumps one page forward/backward.
	 */
	private void selectPage(final int direction) {
		if (matches.isEmpty()) {
			return;
		}

		final int pageSize = getPageSize();

		// Start from the first entry if nothing is selected yet
		selectedMatch = Math.max(selectedMatch, 0) + direction * pageSize;

		if (selectedMatch < 0) {
			selectedMatch = 0;
		}

		if (selectedMatch >= matches.size()) {
			selectedMatch = matches.size() - 1;
		}

		renderText(userText, matches.get(selectedMatch));

		selectPopupItem();
	}

	/**
	 * Number of entries one PageUp/PageDown moves: the number of rows currently
	 * visible in the popup, or DEFAULT_PAGE_SIZE while no popup is shown.
	 */
	private int getPageSize() {
		if (popup != null && !popup.isDisposed() && popup.getVisible() && popupList != null && !popupList.isDisposed()) {
			final int itemHeight = popupList.getItemHeight();
			if (itemHeight > 0) {
				return Math.max(1, popupList.getClientArea().height / itemHeight);
			}
		}

		return DEFAULT_PAGE_SIZE;
	}

	/**
	 * Accepts the currently selected suggestion.
	 */
	private boolean acceptCurrentSuggestion() {
		if (matches.isEmpty()) {
			return false;
		}

		if (selectedMatch < 0 || selectedMatch >= matches.size()) {

			selectedMatch = 0;
		}

		acceptValue(matches.get(selectedMatch));

		return true;
	}

	/**
	 * Accepts a value.
	 */
	private void acceptValue(final String value) {
		if (value == null) {
			return;
		}

		// An accepted value always places the caret at its end
		pendingCaretOffset = -1;

		updating = true;

		try {
			userText = value;
			acceptedText = value;
			setValid(true);

			text.setText(value);

			text.setStyleRange(null);

			text.setSelection(value.length());

			suggestionTailStart = -1;

		} finally {
			updating = false;
		}

		selectedMatch = -1;

		hidePopup();

		/*
		 * Selection means:
		 *
		 * A value was actually accepted.
		 */
		fireSelectionEvent();
	}

	/**
	 * Creates and shows the matches popup.
	 */
	private void showPopup() {
		/*
		 * A single match must be showable as well: the dropdown button and the arrow
		 * keys open the full item list, which may consist of only one entry. Without
		 * the popup that entry would not be selectable at all. Callers reacting to
		 * typing (updateMatches(), mouse click into the field) check for more than
		 * one match themselves, because there a single match is already shown as
		 * suggestion.
		 */
		if (matches.isEmpty()) {
			return;
		}

		if (popup == null || popup.isDisposed()) {

			createPopup();
		}

		popupList.removeAll();

		final int count = matches.size();

		for (int i = 0; i < count; i++) {

			popupList.add(matches.get(i));
		}

		if (selectedMatch >= 0 && selectedMatch < popupList.getItemCount()) {

			popupList.select(selectedMatch);
		}

		positionPopup();

		popup.setVisible(true);
	}

	/**
	 * Creates the popup.
	 */
	private void createPopup() {
		popup = new Shell(getShell(), SWT.ON_TOP | SWT.NO_FOCUS | SWT.TOOL);

		/*
		 * No automatic layout: "insertionMarker" already positions itself manually via
		 * setBounds() while dragging (see the mouseMove handling below), and
		 * "popupList" is sized to the full client area in positionPopup(). A FillLayout
		 * here would instead split the width equally between BOTH children - including
		 * the invisible insertionMarker, which still counts as a layout child even
		 * while hidden - leaving popupList with only half the popup's width and a gray,
		 * unused strip on the other half.
		 */
		popup.setLayout(null);

		popupList = new List(popup, SWT.SINGLE | SWT.V_SCROLL | SWT.BORDER);

		/*
		 * Thin marker bar showing exactly where a dragged entry would be inserted,
		 * positioned manually on top of the list while dragging.
		 */
		insertionMarker = new Composite(popup, SWT.NONE);
		insertionMarker.setBackground(getDisplay().getSystemColor(SWT.COLOR_BLUE));
		insertionMarker.setVisible(false);

		/*
		 * Single click: select the match, but don't accept it yet.
		 */
		popupList.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(final SelectionEvent e) {

				final int index = popupList.getSelectionIndex();

				if (index >= 0 && index < matches.size()) {

					selectedMatch = index;

					renderText(userText, matches.get(selectedMatch));
				}
			}
		});

		popupList.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(final MouseEvent e) {
				getDisplay().timerExec(-1, deferredDeactivateHide);

				mouseDownX = e.x;
				mouseDownY = e.y;

				dragSourceIndex = popupList.getSelectionIndex();

				dragInProgress = false;
				insertionIndex = -1;
			}

			@Override
			public void mouseUp(final MouseEvent e) {
				if (dragInProgress) {
					finishReorder();

					dragInProgress = false;
					insertionMarker.setVisible(false);
					dragSourceIndex = -1;
					insertionIndex = -1;

					return;
				}

				/*
				 * No drag actually happened ("dragInProgress" above
				 * already tracks that precisely, updated live during
				 * mouseMove). A separate re-measurement of the
				 * down/up distance used to live here as a second
				 * "was this a click?" check - but on a real mouse
				 * click, a few pixels of unavoidable hand/trackpad
				 * jitter between press and release easily exceed
				 * CLICK_MOVEMENT_THRESHOLD_PX even though no drag
				 * ever started, silently swallowing the click and
				 * skipping acceptValue() below. The popup still
				 * closed regardless, via the text field's
				 * FocusLost when focus moved to this list - leaving
				 * the field stuck showing the black/gray live
				 * preview from the item's Selection event during
				 * mouseDown, never actually accepted.
				 */

				final int index = popupList.getSelectionIndex();

				if (index >= 0 && index < matches.size()) {
					selectedMatch = index;

					acceptValue(matches.get(index));
				}
			}
		});

		popupList.addMouseMoveListener(e -> {
			if ((e.stateMask & SWT.BUTTON1) == 0) {
				/*
				 * Passive hover (no button held): highlight the entry
				 * under the cursor. Independent of "reorderable" -
				 * this is plain popup-selection UX, not part of the
				 * drag&drop feature, and should work the same whether
				 * or not reordering is enabled for this combo.
				 */
				final int itemHeight = popupList.getItemHeight();

				if (itemHeight <= 0) {
					return;
				}

				int hoveredIndex = popupList.getTopIndex() + e.y / itemHeight;

				if (hoveredIndex < 0) {
					hoveredIndex = 0;
				} else if (hoveredIndex >= matches.size()) {
					hoveredIndex = matches.size() - 1;
				}

				if (popupList.getSelectionIndex() != hoveredIndex) {
					popupList.select(hoveredIndex);

					/*
					 * Keep "selectedMatch" in sync with the mouse-hovered row too, not just
					 * with keyboard navigation - deleteSelectedPopupEntry() (Delete key) acts
					 * on "selectedMatch", and should remove whichever entry is currently
					 * highlighted, exactly like Firefox's own dropdown lists: hover, then
					 * press Delete, deletes the entry under the cursor. Deliberately not
					 * calling renderText()/showing a live preview here though - passive hover
					 * has never accepted or previewed a value in the text field (see the
					 * comment above), and this fix should not change that.
					 */
					selectedMatch = hoveredIndex;
				}

				return;
			}

			if (!reorderable) {
				return;
			}

			/*
			 * Button held: this is a potential or active drag.
			 */
			if (dragSourceIndex < 0 || matches.size() != items.size()) {
				return;
			}

			if (!dragInProgress) {
				final int deltaX = Math.abs(e.x - mouseDownX);

				final int deltaY = Math.abs(e.y - mouseDownY);

				if (deltaX <= CLICK_MOVEMENT_THRESHOLD_PX && deltaY <= CLICK_MOVEMENT_THRESHOLD_PX) {

					return;
				}

				dragInProgress = true;
			}

			if (popupList.getSelectionIndex() != dragSourceIndex) {
				popupList.select(dragSourceIndex);
			}

			final int topIndex = popupList.getTopIndex();

			final int itemHeight = popupList.getItemHeight();

			insertionIndex = computeInsertionIndex(e.y, itemHeight, matches.size(), topIndex);

			final int popupHeight = popup.getClientArea().height;

			final int popupWidth = popup.getClientArea().width;

			final int markerY = Math.max(0, Math.min(popupHeight - 2, (insertionIndex - topIndex) * itemHeight - 1));

			insertionMarker.setBounds(0, markerY, popupWidth, 2);

			insertionMarker.setVisible(true);
			insertionMarker.moveAbove(popupList);
		});
	}

	/**
	 * Moves the dragged entry ("dragSourceIndex") to "insertionIndex" within
	 * "items", called on mouse-up at the end of a drag&amp;drop reorder gesture.
	 */
	private void finishReorder() {
		if (dragSourceIndex < 0 || insertionIndex < 0) {
			return;
		}

		if (insertionIndex != dragSourceIndex && insertionIndex != dragSourceIndex + 1) {
			final String movedItem = items.remove(dragSourceIndex);

			final int insertAt = insertionIndex > dragSourceIndex ? insertionIndex - 1 : insertionIndex;

			items.add(insertAt, movedItem);

			selectedMatch = insertAt;

			/*
			 * Refresh the popup to show the new order and keep it open - drag&drop
			 * reordering only ever happens while browsing the full, unfiltered list (see
			 * the dragSourceIndex/matches.size() guard in the mouseMove handling above), so
			 * "matches" is simply "items" again in the new order. Routing through
			 * updateMatches() instead would treat the still-empty "userText" as nothing
			 * having been typed and close the popup right away (see its empty-text branch)
			 * - undoing the very reorder the user just did before they can see the result
			 * or make another one. The popup still closes normally once an item is actually
			 * selected or focus is lost, via the existing separate paths for those.
			 */
			matches = new ArrayList<>(items);

			showPopup();

			if (itemsReorderedListener != null) {
				itemsReorderedListener.accept(new ArrayList<>(items));
			}
		}
	}

	/**
	 * Checks whether "control" is "ancestor" itself or one of its descendants in
	 * the widget tree.
	 */
	private static boolean isDescendantOf(final Control control, final Control ancestor) {
		Control current = control;

		while (current != null) {
			if (current == ancestor) {
				return true;
			}

			current = current.getParent();
		}

		return false;
	}

	/**
	 * Translates a mouse y-coordinate inside the popup list into the index a
	 * dragged entry would be inserted at (0 = before the first entry, itemCount =
	 * after the last entry), based on which half of the hovered row the pointer is
	 * currently in.
	 */
	private static int computeInsertionIndex(final int localY, final int itemHeight, final int itemCount, final int topIndex) {
		if (itemHeight <= 0 || itemCount <= 0) {
			return 0;
		}

		int hoveredIndex = topIndex + localY / itemHeight;

		if (hoveredIndex < 0) {
			hoveredIndex = 0;
		}

		if (hoveredIndex >= itemCount) {
			hoveredIndex = itemCount - 1;
		}

		final int remainderInRow = localY - (hoveredIndex - topIndex) * itemHeight;

		int insertAt = remainderInRow < itemHeight / 2 ? hoveredIndex : hoveredIndex + 1;

		if (insertAt < 0) {
			insertAt = 0;
		}

		if (insertAt > itemCount) {
			insertAt = itemCount;
		}

		return insertAt;
	}

	/**
	 * Synchronizes the popup's selection.
	 */
	private void selectPopupItem() {
		if (popupList == null || popupList.isDisposed()) {

			return;
		}

		if (selectedMatch >= 0 && selectedMatch < popupList.getItemCount()) {
			popupList.select(selectedMatch);

			popupList.showSelection();
		}
	}

	/**
	 * Positions the popup below the input field.
	 */
	private void positionPopup() {
		final Point size = getSize();

		final int itemHeight = popupList.getItemHeight();

		final int maxRows = Math.max(1, 300 / itemHeight);

		final int rowsToShow = Math.min(matches.size(), maxRows);

		final int desiredClientHeight = itemHeight * rowsToShow;

		final Rectangle bounds = text.getBounds();

		final Point location = toDisplay(bounds.x, bounds.y + bounds.height);

		final int width = size.x;

		final Rectangle listOuter = popupList.computeTrim(0, 0, width, desiredClientHeight);

		final Rectangle shellOuter = popup.computeTrim(0, 0, width, listOuter.height);

		popup.setBounds(location.x, location.y, width, shellOuter.height);

		popupList.setBounds(popup.getClientArea());
	}

	/**
	 * Hides the popup.
	 */
	private void hidePopup() {
		if (popup != null && !popup.isDisposed()) {
			popup.setVisible(false);
		}
	}

	/**
	 * Fires an SWT.Selection event.
	 *
	 * <p>
	 * The event is only fired when a value was actually accepted.
	 * </p>
	 */
	private void fireSelectionEvent() {
		final Event event = new Event();

		event.widget = this;
		event.display = getDisplay();

		notifyListeners(SWT.Selection, event);
		notifyListeners(SWT.DefaultSelection, event);
	}

	/**
	 * Updates the "valid" field and notifies the {@link #validationListener}, but
	 * only if the validity state actually changed.
	 */
	private void setValid(final boolean newValid) {
		if (valid == newValid) {
			return;
		}

		valid = newValid;

		if (validationListener != null) {
			validationListener.accept(valid);
		}
	}

	private void disposeResources() {
		if (popup != null && !popup.isDisposed()) {
			popup.dispose();
			popup = null;
		}

		if (suggestionColor != null && !suggestionColor.isDisposed()) {
			suggestionColor.dispose();
		}

		if (invalidColor != null && !invalidColor.isDisposed()) {
			invalidColor.dispose();
		}

		if (outsideClickFilter != null && !getDisplay().isDisposed()) {
			getDisplay().removeFilter(SWT.MouseDown, outsideClickFilter);
		}
	}

	/**
	 * Returns the index of the match that equals normalizedSearch exactly (after
	 * normalize()), or -1 if none of the matches is an exact match.
	 */
	private int findExactMatchIndex(final java.util.List<String> candidateMatches, final String normalizedSearch) {
		for (int i = 0; i < candidateMatches.size(); i++) {
			if (normalize(candidateMatches.get(i)).equals(normalizedSearch)) {
				return i;
			}
		}
		return -1;
	}
}
