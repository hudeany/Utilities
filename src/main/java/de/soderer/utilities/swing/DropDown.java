package de.soderer.utilities.swing;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.HierarchyBoundsAdapter;
import java.awt.event.HierarchyEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicArrowButton;
import javax.swing.plaf.basic.BasicGraphicsUtils;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.BoxView;
import javax.swing.text.ComponentView;
import javax.swing.text.DocumentFilter;
import javax.swing.text.Element;
import javax.swing.text.IconView;
import javax.swing.text.LabelView;
import javax.swing.text.ParagraphView;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.text.StyledEditorKit;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;

import de.soderer.utilities.Utilities;

/**
 * A Swing combo with autocomplete support.
 *
 * <p>
 * Swing counterpart of {@code de.soderer.utilities.swt.DropDown}, with the
 * same behavior and the same feature set.
 * </p>
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
 * <li>ActionEvent only fires when a value was actually accepted</li>
 * <li>ValidationListener fires whenever the valid/invalid state changes</li>
 * <li>Drag&amp;drop reordering of entries in the popup (optional)</li>
 * <li>Delete key removes the selected entry from the popup (optional)</li>
 * <li>Placeholder/hint text while empty</li>
 * <li>ChangeEvents are fired whenever the entered text changes</li>
 * </ul>
 *
 * <p>
 * Implementation notes (differences to the SWT variant that are caused by the
 * toolkit, not by the behavior):
 * </p>
 * <ul>
 * <li>The input field is a single-line, non-wrapping {@link JTextPane} (the
 * Swing equivalent of SWT's StyledText), because a plain JTextField cannot
 * color individual characters.</li>
 * <li>All user edits are intercepted synchronously by a {@link DocumentFilter}
 * (the counterpart of SWT's VerifyListener + ModifyListener pair), so no
 * asyncExec-like deferral is needed.</li>
 * <li>The popup list is not focusable, so clicking into it never takes the
 * keyboard focus away from the input field. The deferred-hide workarounds of
 * the SWT variant are therefore not needed.</li>
 * <li>Item manipulation methods are named addItem()/removeItem()/
 * removeItemAt()/removeAllItems() (like JComboBox), because add()/remove()/
 * removeAll() are already used by java.awt.Container for child
 * components.</li>
 * </ul>
 */
public class DropDown extends JPanel {
	private static final long serialVersionUID = 4630391958126720583L;

	/**
	 * Search mode.
	 */
	public enum MatchMode {
		STARTS_WITH,
		CONTAINS
	}

	/**
	 * Action command of the ActionEvents fired when a value was accepted.
	 */
	public static final String ACTION_COMMAND_VALUE_ACCEPTED = "valueAccepted";

	/**
	 * Maximum pointer movement (in pixels) between mouse-down and mouse-up on a
	 * popup entry that still counts as a plain click rather than a drag&amp;drop
	 * reorder attempt.
	 */
	private static final int CLICK_MOVEMENT_THRESHOLD_PX = 4;

	/**
	 * Maximum height of the popup list's content area in pixels.
	 */
	private static final int MAX_POPUP_HEIGHT_PX = 300;

	/**
	 * Page size for PageUp/PageDown while no popup is shown.
	 */
	private static final int DEFAULT_PAGE_SIZE = 10;

	private static final int DROPDOWN_BUTTON_WIDTH = 20;

	private final JTextPane textPane;
	private final JScrollPane textScrollPane;
	private final BasicArrowButton dropdownButton;

	private Popup popup;
	private Rectangle popupBounds;
	private JList<String> popupList;
	private DefaultListModel<String> popupModel;
	private JScrollPane popupScrollPane;

	private final List<String> items = new ArrayList<>();

	/**
	 * Current matches.
	 */
	private List<String> matches = Collections.emptyList();

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
	 * Last text reported to the ChangeListeners.
	 */
	private String lastReportedText = "";

	/**
	 * Suppresses the DocumentFilter's user-input handling for our own
	 * programmatic document changes.
	 */
	private boolean updating;

	/**
	 * Start offset of a currently displayed, unselected gray suggestion tail (see
	 * renderText()), or -1 if none is shown. Lets the DocumentFilter
	 * transparently consume that tail on the next keystroke - as if it had been
	 * selected - without ever actually showing a text selection for it.
	 */
	private int suggestionTailStart = -1;

	/**
	 * Explicit caret offset to apply on the next render call, or -1 to fall back
	 * to the widget's current caret offset.
	 *
	 * <p>
	 * The DocumentFilter never lets a user edit through natively; it computes the
	 * resulting text itself and re-renders the whole document. So the caret
	 * position the edit would have produced natively is computed there as well
	 * and handed over via this field.
	 * </p>
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

	private Color suggestionColor = new Color(170, 170, 170);
	private Color invalidColor = new Color(220, 70, 70);
	private final Color insertionMarkerColor = new Color(0, 90, 220);

	/**
	 * Placeholder/hint text shown in light gray while the field is empty. Purely a
	 * paint-time overlay, never part of the document content.
	 */
	private String hintText;

	/**
	 * Preferred width of the input field in columns, or 0 to size it by the widest
	 * item (like JComboBox does).
	 */
	private int columns = 0;

	/**
	 * Whether entries can be reordered via drag&amp;drop in the popup.
	 */
	private boolean reorderable = false;

	/**
	 * Whether the currently selected entry in the popup can be removed from the
	 * item list via the Delete key.
	 */
	private boolean deletable = false;

	private final List<Consumer<List<String>>> itemsReorderedListeners = new CopyOnWriteArrayList<>();
	private final List<Consumer<String>> itemDeletedListeners = new CopyOnWriteArrayList<>();
	private final List<Consumer<Boolean>> validationListeners = new CopyOnWriteArrayList<>();

	private int dragSourceIndex = -1;
	private boolean dragInProgress = false;
	private int insertionIndex = -1;
	private Point mouseDownPoint;

	/**
	 * Closes the popup on a mouse press anywhere outside of it and outside of this
	 * combo. Registered globally on the Toolkit while this component is
	 * displayable, since a click can land on any component, not just ours.
	 */
	private final AWTEventListener outsideClickListener;
	private boolean outsideClickListenerRegistered = false;

	/**
	 * Closes the popup when the owning window is moved or resized, since a
	 * heavyweight popup would otherwise stay behind at its old screen position.
	 */
	private final ComponentListener ownerWindowListener;
	private Window ownerWindow;

	/**
	 * Creates a DropDown.
	 */
	public DropDown() {
		super(new BorderLayout());

		textPane = new JTextPane() {
			private static final long serialVersionUID = -2305262436826618545L;

			@Override
			public boolean getScrollableTracksViewportHeight() {
				return true;
			}

			@Override
			public Dimension getPreferredScrollableViewportSize() {
				return new Dimension(computePreferredTextWidth(this), getPreferredSize().height);
			}

			@Override
			protected void paintComponent(final Graphics g) {
				super.paintComponent(g);

				paintHint(this, g);
			}
		};

		// Must be set before installing the filter, because setEditorKit() replaces the document
		textPane.setEditorKit(new SingleLineEditorKit());
		((AbstractDocument) textPane.getDocument()).setDocumentFilter(new InputFilter());

		/*
		 * Tab is handled manually in handleKey() (accept suggestion, then move
		 * focus), so it must not be swallowed by the focus traversal machinery before
		 * our KeyListener sees it.
		 */
		textPane.setFocusTraversalKeysEnabled(false);

		/*
		 * Unbind Enter and Tab from the text pane's own actions (insert-break /
		 * insert-tab). Mapping to a non-existent action lets unconsumed key events
		 * fall through to WHEN_IN_FOCUSED_WINDOW bindings, e.g. a dialog's default
		 * button - like Enter in a JTextField does.
		 */
		final InputMap inputMap = textPane.getInputMap(JComponent.WHEN_FOCUSED);
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "none");
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0), "none");
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_DOWN_MASK), "none");

		textScrollPane = new JScrollPane(textPane, ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		add(textScrollPane, BorderLayout.CENTER);

		dropdownButton = new BasicArrowButton(SwingConstants.SOUTH) {
			private static final long serialVersionUID = 8914036436318547129L;

			@Override
			public Dimension getPreferredSize() {
				return new Dimension(DROPDOWN_BUTTON_WIDTH, super.getPreferredSize().height);
			}
		};

		/*
		 * Not focusable, so clicking it keeps the keyboard focus in the text pane,
		 * where the KeyListener driving popup navigation/deletion lives.
		 */
		dropdownButton.setFocusable(false);
		dropdownButton.addActionListener(e -> {
			showAllItems();

			textPane.requestFocusInWindow();
		});
		add(dropdownButton, BorderLayout.EAST);

		applyLookAndFeelDefaults();

		textPane.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(final KeyEvent e) {
				handleKey(e);
			}
		});

		/*
		 * Close the popup on focus loss. Covers switching to another application too
		 * (temporary focus loss of the owning window).
		 */
		textPane.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(final FocusEvent e) {
				hidePopup();
			}
		});

		/*
		 * Mouse click on the field: Open the popup if there are multiple matches.
		 */
		textPane.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(final MouseEvent e) {
				if (isEnabled() && matches.size() > 1) {
					showPopup();
				}
			}
		});

		outsideClickListener = event -> {
			if (popup == null || event.getID() != MouseEvent.MOUSE_PRESSED || !(event.getSource() instanceof Component)) {
				return;
			}

			final Component clickedComponent = (Component) event.getSource();

			if (SwingUtilities.isDescendingFrom(clickedComponent, popupScrollPane) || SwingUtilities.isDescendingFrom(clickedComponent, this)) {
				return;
			}

			hidePopup();
		};

		ownerWindowListener = new ComponentAdapter() {
			@Override
			public void componentMoved(final ComponentEvent e) {
				hidePopup();
			}

			@Override
			public void componentResized(final ComponentEvent e) {
				hidePopup();
			}
		};

		/*
		 * Intermediate containers (split panes, scroll panes, ...) moving this combo
		 * would leave the popup stranded as well.
		 */
		addHierarchyBoundsListener(new HierarchyBoundsAdapter() {
			@Override
			public void ancestorMoved(final HierarchyEvent e) {
				hidePopup();
			}

			@Override
			public void ancestorResized(final HierarchyEvent e) {
				hidePopup();
			}
		});
	}

	@Override
	public void addNotify() {
		super.addNotify();

		if (!outsideClickListenerRegistered) {
			try {
				Toolkit.getDefaultToolkit().addAWTEventListener(outsideClickListener, AWTEvent.MOUSE_EVENT_MASK);
				outsideClickListenerRegistered = true;
			} catch (@SuppressWarnings("unused") final SecurityException e) {
				// Outside click detection not available, focus loss still closes the popup
			}
		}

		ownerWindow = SwingUtilities.getWindowAncestor(this);
		if (ownerWindow != null) {
			ownerWindow.addComponentListener(ownerWindowListener);
		}
	}

	@Override
	public void removeNotify() {
		hidePopup();

		if (outsideClickListenerRegistered) {
			try {
				Toolkit.getDefaultToolkit().removeAWTEventListener(outsideClickListener);
			} catch (@SuppressWarnings("unused") final SecurityException e) {
				// Nothing to clean up
			}
			outsideClickListenerRegistered = false;
		}

		if (ownerWindow != null) {
			ownerWindow.removeComponentListener(ownerWindowListener);
			ownerWindow = null;
		}

		super.removeNotify();
	}

	@Override
	public void updateUI() {
		super.updateUI();

		// Called by JPanel's constructor before our fields are initialized
		if (textPane != null) {
			applyLookAndFeelDefaults();

			if (popupScrollPane != null) {
				// The popup content is not part of the component tree while hidden
				SwingUtilities.updateComponentTreeUI(popupScrollPane);
			}

			updateMatches(false);
		}
	}

	/**
	 * Sets the values of the suggestion list.
	 */
	public void setItems(final String... values) {
		setItems(values == null ? null : Arrays.asList(values));
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
	public void setItems(final List<String> values) {
		items.clear();

		if (values != null) {
			items.addAll(values);
		}

		selectedMatch = -1;

		updateMatches(false);

		revalidate();
	}

	/**
	 * Sets the values of the suggestion list and returns this instance for
	 * chaining.
	 */
	public DropDown withItems(final List<String> values) {
		setItems(values);

		return this;
	}

	/**
	 * Adds a value to the suggestion list.
	 */
	public void addItem(final String value) {
		Objects.requireNonNull(value, "value");

		items.add(value);

		updateMatches(false);

		revalidate();
	}

	/**
	 * Removes a value by its index.
	 */
	public void removeItemAt(final int index) {
		items.remove(index);

		updateMatches(false);

		revalidate();
	}

	/**
	 * Removes a value.
	 */
	public void removeItem(final String value) {
		items.remove(value);

		updateMatches(false);

		revalidate();
	}

	/**
	 * Removes all values.
	 */
	public void removeAllItems() {
		items.clear();
		matches = Collections.emptyList();
		selectedMatch = -1;

		hidePopup();

		revalidate();
	}

	/**
	 * Returns all suggestion values.
	 */
	public String[] getItems() {
		return items.toArray(new String[0]);
	}

	/**
	 * Returns the number of suggestion values.
	 */
	public int getItemCount() {
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
		Objects.requireNonNull(value, "value");

		pendingCaretOffset = -1;

		updating = true;
		try {
			userText = value;
			acceptedText = value;
			setValid(true);

			writeContent(value, 0, 0, getNormalColor(), getNormalColor());
			textPane.setCaretPosition(value.length());

			suggestionTailStart = -1;
		} finally {
			updating = false;
		}

		selectedMatch = -1;

		updateMatches(false);

		fireTextChangedIfNeeded();
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
		this.matchMode = Objects.requireNonNull(matchMode, "matchMode");

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
	 * list (i.e. the input text matches every item, or the popup was opened via
	 * the dropdown button / {@link #showAllItems()}).
	 * </p>
	 */
	public boolean isReorderable() {
		return reorderable;
	}

	/**
	 * Enables or disables drag&amp;drop reordering of entries in the popup.
	 */
	public void setReorderable(final boolean reorderable) {
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
	 * Propagates the enabled state to the text field and the dropdown button and
	 * grays out the whole text, like a disabled native combo.
	 */
	@Override
	public void setEnabled(final boolean enabled) {
		super.setEnabled(enabled);

		textPane.setEnabled(enabled);
		dropdownButton.setEnabled(enabled);

		applyBackground();

		if (enabled) {
			/*
			 * Re-render from the real current state, so content and coloring are correct
			 * even if something changed (e.g. via setText()/setItems()) while disabled.
			 */
			updateMatches(false);
		} else {
			hidePopup();

			/*
			 * Uniformly gray, deliberately not preserving the distinction between typed
			 * and suggested text - that only matters while the field is interactive.
			 */
			updating = true;
			try {
				final StyledDocument document = textPane.getStyledDocument();
				document.setCharacterAttributes(0, document.getLength(), createColorAttributes(suggestionColor), false);
			} finally {
				updating = false;
			}
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
	 * Sets a hint/placeholder text shown in light gray while the field is empty,
	 * like SWT's Text.setMessage(). Pass {@code null} or an empty string to remove
	 * it.
	 */
	public void setMessage(final String message) {
		hintText = message;

		revalidate();
		textPane.repaint();
	}

	/**
	 * Sets a hint/placeholder text and returns this instance for chaining.
	 */
	public DropDown withMessage(final String message) {
		setMessage(message);

		return this;
	}

	/**
	 * Returns the hint/placeholder text.
	 */
	public String getMessage() {
		return hintText;
	}

	/**
	 * Returns the preferred width of the input field in columns, or 0 if it is
	 * sized by the widest item.
	 */
	public int getColumns() {
		return columns;
	}

	/**
	 * Sets the preferred width of the input field in columns (like
	 * JTextField.setColumns()). 0 (default) sizes it by the widest item.
	 */
	public void setColumns(final int columns) {
		if (columns < 0) {
			throw new IllegalArgumentException("columns less than zero");
		}

		this.columns = columns;

		revalidate();
	}

	/**
	 * Sets the preferred width of the input field in columns and returns this
	 * instance for chaining.
	 */
	public DropDown withColumns(final int columnsValue) {
		setColumns(columnsValue);

		return this;
	}

	/**
	 * Returns whether the built-in dropdown "browse all" button is shown next to
	 * the text field.
	 */
	public boolean isShowDropdownButton() {
		return dropdownButton.isVisible();
	}

	/**
	 * Shows or hides the built-in dropdown "browse all" button next to the text
	 * field. Shown by default.
	 */
	public void setShowDropdownButton(final boolean show) {
		dropdownButton.setVisible(show);

		revalidate();
		repaint();
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
	 * Color of the gray suggestion parts, the hint text and the disabled text.
	 */
	public Color getSuggestionColor() {
		return suggestionColor;
	}

	public void setSuggestionColor(final Color suggestionColor) {
		this.suggestionColor = Objects.requireNonNull(suggestionColor, "suggestionColor");

		updateMatches(false);
	}

	public DropDown withSuggestionColor(final Color suggestionColorValue) {
		setSuggestionColor(suggestionColorValue);

		return this;
	}

	/**
	 * Color of invalid text (no match while custom values are not allowed).
	 */
	public Color getInvalidColor() {
		return invalidColor;
	}

	public void setInvalidColor(final Color invalidColor) {
		this.invalidColor = Objects.requireNonNull(invalidColor, "invalidColor");

		updateMatches(false);
	}

	public DropDown withInvalidColor(final Color invalidColorValue) {
		setInvalidColor(invalidColorValue);

		return this;
	}

	@Override
	public void setFont(final Font font) {
		super.setFont(font);

		// Called by JPanel's constructor before our fields are initialized
		if (textPane != null) {
			textPane.setFont(font);

			if (popupList != null) {
				popupList.setFont(font);
			}

			revalidate();
		}
	}

	@Override
	public void setToolTipText(final String text) {
		super.setToolTipText(text);

		if (textPane != null) {
			textPane.setToolTipText(text);
		}
	}

	/**
	 * Registers a listener that is notified with the new item order whenever the
	 * user reorders entries via drag&amp;drop in the popup.
	 */
	public void addItemsReorderedListener(final Consumer<List<String>> listener) {
		itemsReorderedListeners.add(Objects.requireNonNull(listener, "listener"));
	}

	public void removeItemsReorderedListener(final Consumer<List<String>> listener) {
		itemsReorderedListeners.remove(listener);
	}

	/**
	 * Registers a listener that is notified with the value of the entry the user
	 * just removed via the Delete key in the popup (see {@link #isDeletable()}).
	 */
	public void addItemDeletedListener(final Consumer<String> listener) {
		itemDeletedListeners.add(Objects.requireNonNull(listener, "listener"));
	}

	public void removeItemDeletedListener(final Consumer<String> listener) {
		itemDeletedListeners.remove(listener);
	}

	/**
	 * Registers a listener that is notified whenever the validity state (see
	 * {@link #isValidValue()}) actually changes, in either direction.
	 *
	 * <p>
	 * Unlike {@link #addActionListener(ActionListener)}, which only fires when a
	 * value was actually accepted, this fires immediately whenever the text
	 * switches to/from the invalid (red) display state - in particular when
	 * {@link #getText()} switches to/from returning {@code null} (only relevant
	 * while {@link #isAllowCustomValues()} is {@code false}).
	 * </p>
	 */
	public void addValidationListener(final Consumer<Boolean> listener) {
		validationListeners.add(Objects.requireNonNull(listener, "listener"));
	}

	public void removeValidationListener(final Consumer<Boolean> listener) {
		validationListeners.remove(listener);
	}

	/**
	 * Adds a listener notified when a value is accepted by the user (Enter, Tab,
	 * click in the popup, or automatic acceptance of a single match). Counterpart
	 * of the SWT variant's SelectionListener, following JComboBox's ActionEvent
	 * convention.
	 */
	public void addActionListener(final ActionListener listener) {
		listenerList.add(ActionListener.class, Objects.requireNonNull(listener, "listener"));
	}

	public void removeActionListener(final ActionListener listener) {
		listenerList.remove(ActionListener.class, listener);
	}

	/**
	 * Adds a listener notified whenever the entered text (see {@link #getText()})
	 * changes, whether by typing, by acceptance or programmatically.
	 */
	public void addChangeListener(final ChangeListener listener) {
		listenerList.add(ChangeListener.class, Objects.requireNonNull(listener, "listener"));
	}

	public void removeChangeListener(final ChangeListener listener) {
		listenerList.remove(ChangeListener.class, listener);
	}

	/**
	 * Shows the popup with the full, unfiltered item list, regardless of the
	 * current input text.
	 *
	 * <p>
	 * Used by the dropdown button, and the only way to trigger drag&amp;drop
	 * reordering while some text is entered, since reordering requires the popup
	 * to show every item.
	 * </p>
	 */
	public void showAllItems() {
		if (items.isEmpty()) {
			return;
		}

		matches = new ArrayList<>(items);
		selectedMatch = -1;

		showPopup();
	}

	/**
	 * Whether the popup list is currently shown.
	 */
	public boolean isPopupVisible() {
		return popup != null;
	}

	/**
	 * Selects the entire text.
	 */
	public void selectAll() {
		textPane.selectAll();
	}

	/**
	 * Returns the current text selection (x = start offset, y = end offset), like
	 * SWT's getSelection().
	 */
	public Point getSelection() {
		return new Point(textPane.getSelectionStart(), textPane.getSelectionEnd());
	}

	@Override
	public boolean requestFocusInWindow() {
		return textPane.requestFocusInWindow();
	}

	@Override
	public void requestFocus() {
		textPane.requestFocus();
	}

	/**
	 * Returns the internal JTextPane.
	 *
	 * <p>
	 * Can be useful when special Swing functionality is needed. Do not replace its
	 * document or editor kit, as this would remove the input handling.
	 * </p>
	 */
	public JTextPane getTextControl() {
		return textPane;
	}

	/**
	 * Handles an edit the user made to the document, intercepted by the
	 * DocumentFilter before it is applied.
	 *
	 * <p>
	 * The edit is never applied natively. Instead the resulting text is computed
	 * here and the document is re-rendered from it, so the gray suggestion parts
	 * never leak into "userText" accidentally.
	 * </p>
	 */
	private void handleUserEdit(final int offset, final int length, final String insertedText) {
		final String typed = removeLineBreaks(insertedText);
		if (length == 0 && typed.isEmpty()) {
			return;
		}

		final String displayed = getDisplayedText();
		final int tailStart = suggestionTailStart;
		suggestionTailStart = -1;

		if (tailStart >= 0 && length == 0 && offset == tailStart) {
			/*
			 * Typing right at the start of the gray suggestion tail: the tail is
			 * consumed as if it had been selected, the typed text is appended to the
			 * real input.
			 */
			userText = userText + typed;
			pendingCaretOffset = offset + typed.length();
		} else if (tailStart >= 0 && typed.isEmpty() && length == 1 && offset == tailStart - 1) {
			/*
			 * Backspace right at the boundary between the real input and the gray
			 * suggestion tail. The default deletion would remove a character from the
			 * DISPLAYED suggestion text (which may not even be the last character the
			 * user actually typed - see matchStart > 0 in renderText()), not from the
			 * real "userText". So shrink "userText" by one character instead.
			 */
			userText = userText.isEmpty() ? "" : userText.substring(0, userText.length() - 1);
			pendingCaretOffset = offset;
		} else {
			// Any other edit applies to the displayed text, exactly like a native edit would
			final int start = Math.max(0, Math.min(offset, displayed.length()));
			final int end = Math.max(start, Math.min(offset + length, displayed.length()));

			userText = displayed.substring(0, start) + typed + displayed.substring(end);
			pendingCaretOffset = start + typed.length();
		}

		processUserInput();
	}

	/**
	 * Processes an actual user input.
	 */
	private void processUserInput() {
		selectedMatch = -1;

		updateMatches(true);

		fireTextChangedIfNeeded();
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

		final List<String> result = new ArrayList<>();
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
		} else if (!allowCustomValues && matches.size() == 1 && userInitiated) {
			acceptValue(matches.get(0));
		} else {
			if (selectedMatch < 0 || selectedMatch >= matches.size()) {
				/*
				 * Prefer an exact match over the first (arbitrary, item-order-dependent)
				 * substring/prefix match - otherwise, under MatchMode.CONTAINS, a value that
				 * is itself a complete item but also a substring of an earlier item (e.g.
				 * "SIT-1" is contained in "PRESIT-1") would display that unrelated item.
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
			 * setEnabled(true), etc. - must stay purely visual.
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
		 * Consume the pending caret offset unconditionally (also for the empty-input
		 * case), so a stale value can never leak into a later render.
		 */
		final int caretBeforeUpdate = pendingCaretOffset >= 0 ? pendingCaretOffset : textPane.getCaretPosition();
		pendingCaretOffset = -1;

		final Color normalColor = getNormalColor();

		updating = true;
		try {
			if (Utilities.isEmpty(input)) {
				writeContent("", 0, 0, normalColor, normalColor);
				suggestionTailStart = -1;
			} else {
				String displayText = input;
				int matchStart = -1;
				int matchLength = 0;

				if (suggestion != null) {
					matchStart = normalize(suggestion).indexOf(normalize(input));
					displayText = suggestion;
					matchLength = input.length();
				}

				if (matchStart >= 0) {
					final int afterStart = Math.min(matchStart + matchLength, displayText.length());

					// Gray before and after the typed part, normal color for the typed part
					writeContent(displayText, matchStart, afterStart, suggestionColor, normalColor);

					textPane.setCaretPosition(afterStart);
					suggestionTailStart = afterStart;
				} else if (suggestion == null) {
					writeContent(displayText, 0, 0, normalColor, normalColor);

					textPane.setCaretPosition(Math.max(0, Math.min(caretBeforeUpdate, displayText.length())));
					suggestionTailStart = -1;
				} else {
					writeContent(displayText, 0, 0, normalColor, normalColor);

					textPane.setCaretPosition(displayText.length());
					suggestionTailStart = displayText.length();
				}
			}
		} finally {
			updating = false;
		}

		scheduleCaretVisible();
	}

	/**
	 * Renders an invalid text.
	 *
	 * <p>
	 * Keeps the caret where the user's edit put it, so editing in the middle of an
	 * invalid (red) text does not make the caret jump to the end.
	 * </p>
	 */
	private void renderInvalidText() {
		final int caretBeforeUpdate = pendingCaretOffset >= 0 ? pendingCaretOffset : userText.length();
		pendingCaretOffset = -1;

		updating = true;
		try {
			writeContent(userText, 0, 0, invalidColor, invalidColor);

			textPane.setCaretPosition(Math.max(0, Math.min(caretBeforeUpdate, userText.length())));

			suggestionTailStart = -1;
		} finally {
			updating = false;
		}

		scheduleCaretVisible();
	}

	/**
	 * Replaces the whole document content. Characters in [highlightStart,
	 * highlightEnd) get "insideColor", all others "outsideColor".
	 *
	 * <p>
	 * Must only be called while "updating" is set, so the DocumentFilter lets the
	 * change through. Safe to call from within the DocumentFilter itself, since
	 * AbstractDocument's write lock is reentrant for the owning thread.
	 * </p>
	 */
	private void writeContent(final String content, final int highlightStart, final int highlightEnd, final Color outsideColor, final Color insideColor) {
		final StyledDocument document = textPane.getStyledDocument();
		try {
			document.remove(0, document.getLength());
			document.insertString(0, content, createColorAttributes(outsideColor));

			if (highlightEnd > highlightStart && !Objects.equals(outsideColor, insideColor)) {
				document.setCharacterAttributes(highlightStart, highlightEnd - highlightStart, createColorAttributes(insideColor), false);
			}
		} catch (final BadLocationException e) {
			throw new IllegalStateException("Cannot render DropDown text", e);
		}
	}

	private static SimpleAttributeSet createColorAttributes(final Color color) {
		final SimpleAttributeSet attributes = new SimpleAttributeSet();
		if (color != null) {
			StyleConstants.setForeground(attributes, color);
		}
		return attributes;
	}

	private String getDisplayedText() {
		final StyledDocument document = textPane.getStyledDocument();
		try {
			return document.getText(0, document.getLength());
		} catch (final BadLocationException e) {
			throw new IllegalStateException("Cannot read DropDown text", e);
		}
	}

	private Color getNormalColor() {
		final Color foreground = textPane.getForeground();
		return foreground != null ? foreground : Color.BLACK;
	}

	/**
	 * Scrolls the (possibly wider than visible) text so the caret is visible.
	 * Deferred, because the text pane's new preferred width is only applied by the
	 * next layout pass.
	 */
	private void scheduleCaretVisible() {
		SwingUtilities.invokeLater(() -> {
			try {
				final Rectangle2D caretRectangle = textPane.modelToView2D(textPane.getCaretPosition());
				if (caretRectangle != null) {
					textPane.scrollRectToVisible(caretRectangle.getBounds());
				}
			} catch (@SuppressWarnings("unused") final BadLocationException e) {
				// Document changed meanwhile, next render will scroll again
			}
		});
	}

	private void handleKey(final KeyEvent e) {
		if (!isEnabled()) {
			return;
		}

		switch (e.getKeyCode()) {
			case KeyEvent.VK_DOWN:
			case KeyEvent.VK_KP_DOWN:
				if (matches.isEmpty() && userText.isEmpty() && !items.isEmpty()) {
					openFullListFromKeyboard(0);

					e.consume();
				} else if (!matches.isEmpty()) {
					selectNextMatch();

					e.consume();
				}
				break;
			case KeyEvent.VK_UP:
			case KeyEvent.VK_KP_UP:
				if (matches.isEmpty() && userText.isEmpty() && !items.isEmpty()) {
					openFullListFromKeyboard(items.size() - 1);

					e.consume();
				} else if (!matches.isEmpty()) {
					selectPreviousMatch();

					e.consume();
				}
				break;
			case KeyEvent.VK_PAGE_DOWN:
				if (!matches.isEmpty()) {
					selectPage(1);

					e.consume();
				}
				break;
			case KeyEvent.VK_PAGE_UP:
				if (!matches.isEmpty()) {
					selectPage(-1);

					e.consume();
				}
				break;
			case KeyEvent.VK_TAB:
				if (e.isControlDown() || e.isAltDown()) {
					break;
				}

				// Focus traversal keys are disabled on the text pane, so traversal is done manually here
				e.consume();

				if (!acceptCurrentSuggestion() && !allowCustomValues && !valid) {
					// No match: focus stays in the field
					break;
				}

				if (e.isShiftDown()) {
					textPane.transferFocusBackward();
				} else {
					textPane.transferFocus();
				}
				break;
			case KeyEvent.VK_ENTER:
				if (acceptCurrentSuggestion()) {
					e.consume();
				} else if (!allowCustomValues && !valid) {
					e.consume();
				}
				// Otherwise unconsumed: may trigger e.g. a dialog's default button
				break;
			case KeyEvent.VK_ESCAPE:
				if (isPopupVisible()) {
					hidePopup();

					// Consumed only if it closed the popup, so it does not also close a dialog
					e.consume();
				}
				break;
			case KeyEvent.VK_DELETE:
				if (deletable && isPopupVisibleWithSelection()) {
					deleteSelectedPopupEntry();

					e.consume();
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
		return isPopupVisible() && selectedMatch >= 0 && selectedMatch < matches.size();
	}

	/**
	 * Removes the currently selected popup entry from "items" and "matches",
	 * updates the popup list and selection accordingly, and notifies the
	 * itemDeletedListeners.
	 */
	private void deleteSelectedPopupEntry() {
		if (!isPopupVisibleWithSelection()) {
			return;
		}

		final int deletedIndex = selectedMatch;
		final String deletedItem = matches.get(deletedIndex);

		items.remove(deletedItem);
		matches.remove(deletedIndex);
		popupModel.remove(deletedIndex);

		if (matches.isEmpty()) {
			/*
			 * No items left to show a selection for - fully reset via setText("")
			 * rather than leaving the field showing the just-deleted entry's text.
			 */
			setText("");
		} else {
			selectedMatch = Math.min(deletedIndex, matches.size() - 1);

			renderText(userText, matches.get(selectedMatch));

			selectPopupItem();

			positionPopup();
		}

		revalidate();

		for (final Consumer<String> listener : itemDeletedListeners) {
			listener.accept(deletedItem);
		}
	}

	/**
	 * Opens the popup showing the full, unfiltered item list, for an arrow key
	 * press while no text has been typed yet.
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
	 * Jumps one page forward/backward. A page is the number of rows visible in the
	 * popup.
	 */
	private void selectPage(final int direction) {
		if (matches.isEmpty()) {
			return;
		}

		final int pageSize = isPopupVisible() ? Math.max(1, popupList.getVisibleRowCount()) : DEFAULT_PAGE_SIZE;

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

		pendingCaretOffset = -1;

		updating = true;
		try {
			userText = value;
			acceptedText = value;
			setValid(true);

			writeContent(value, 0, 0, getNormalColor(), getNormalColor());

			textPane.setCaretPosition(value.length());

			suggestionTailStart = -1;
		} finally {
			updating = false;
		}

		selectedMatch = -1;

		hidePopup();

		scheduleCaretVisible();

		fireTextChangedIfNeeded();

		// Action means: a value was actually accepted
		fireActionEvent();
	}

	/**
	 * Creates the popup content (list inside a scroll pane) on first use.
	 */
	private void ensurePopupContent() {
		if (popupList != null) {
			return;
		}

		popupModel = new DefaultListModel<>();

		popupList = new JList<>(popupModel) {
			private static final long serialVersionUID = 5170358651911519390L;

			@Override
			protected void paintComponent(final Graphics g) {
				super.paintComponent(g);

				paintInsertionMarker(this, g);
			}
		};
		popupList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		/*
		 * Not focusable, so clicking an entry keeps the keyboard focus in the text
		 * pane. This is what makes the popup work without the deferred-hide
		 * workarounds needed in the SWT variant.
		 */
		popupList.setFocusable(false);
		popupList.setFont(textPane.getFont());

		final PopupMouseHandler mouseHandler = new PopupMouseHandler();
		popupList.addMouseListener(mouseHandler);
		popupList.addMouseMotionListener(mouseHandler);

		popupScrollPane = new JScrollPane(popupList, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		popupScrollPane.setFocusable(false);
		popupScrollPane.getVerticalScrollBar().setFocusable(false);

		final Border popupBorder = UIManager.getBorder("PopupMenu.border");
		popupScrollPane.setBorder(popupBorder != null ? popupBorder : BorderFactory.createLineBorder(Color.GRAY));
	}

	/**
	 * Fills and shows the matches popup.
	 */
	private void showPopup() {
		if (matches.size() <= 1 || !isShowing()) {
			return;
		}

		ensurePopupContent();

		popupModel.clear();
		popupModel.addAll(matches);

		if (selectedMatch >= 0 && selectedMatch < popupModel.getSize()) {
			popupList.setSelectedIndex(selectedMatch);
		} else {
			popupList.clearSelection();
		}

		positionPopup();
	}

	/**
	 * Positions the popup below the input field (or above it, if there is not
	 * enough space on the screen below), and shows it.
	 *
	 * <p>
	 * A javax.swing.Popup cannot be moved or resized, so it is only recreated when
	 * its bounds actually change; otherwise the existing one just shows the
	 * updated list model.
	 * </p>
	 */
	private void positionPopup() {
		if (popupList == null || !isShowing()) {
			return;
		}

		final int count = popupModel.getSize();
		if (count == 0) {
			hidePopup();
			return;
		}

		final Rectangle firstCell = popupList.getCellBounds(0, 0);
		final int rowHeight = firstCell != null && firstCell.height > 0 ? firstCell.height : popupList.getFontMetrics(popupList.getFont()).getHeight();
		final int maxRows = Math.max(1, MAX_POPUP_HEIGHT_PX / rowHeight);
		popupList.setVisibleRowCount(Math.min(count, maxRows));

		popupScrollPane.setPreferredSize(null);
		final int height = popupScrollPane.getPreferredSize().height;
		final int width = getWidth();
		popupScrollPane.setPreferredSize(new Dimension(width, height));

		final Point location = new Point(0, getHeight());
		SwingUtilities.convertPointToScreen(location, this);

		final GraphicsConfiguration graphicsConfiguration = getGraphicsConfiguration();
		if (graphicsConfiguration != null) {
			final Rectangle screenBounds = graphicsConfiguration.getBounds();
			final Insets screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(graphicsConfiguration);
			final int screenBottom = screenBounds.y + screenBounds.height - screenInsets.bottom;
			final int screenTop = screenBounds.y + screenInsets.top;

			if (location.y + height > screenBottom) {
				final Point topLocation = new Point(0, 0);
				SwingUtilities.convertPointToScreen(topLocation, this);

				if (topLocation.y - height >= screenTop) {
					location.y = topLocation.y - height;
				}
			}
		}

		final Rectangle newBounds = new Rectangle(location.x, location.y, width, height);

		if (popup != null && newBounds.equals(popupBounds)) {
			popupScrollPane.revalidate();
			popupList.repaint();
		} else {
			if (popup != null) {
				popup.hide();
			}

			popup = PopupFactory.getSharedInstance().getPopup(this, popupScrollPane, location.x, location.y);
			popupBounds = newBounds;
			popup.show();
		}

		if (selectedMatch >= 0 && selectedMatch < count) {
			popupList.ensureIndexIsVisible(selectedMatch);
		}
	}

	/**
	 * Hides the popup.
	 */
	private void hidePopup() {
		if (popup != null) {
			popup.hide();
			popup = null;
			popupBounds = null;
		}

		dragInProgress = false;
		dragSourceIndex = -1;
		insertionIndex = -1;
	}

	/**
	 * Synchronizes the popup's selection.
	 */
	private void selectPopupItem() {
		if (popupList == null) {
			return;
		}

		if (selectedMatch >= 0 && selectedMatch < popupModel.getSize()) {
			popupList.setSelectedIndex(selectedMatch);

			popupList.ensureIndexIsVisible(selectedMatch);
		}
	}

	/**
	 * Moves the dragged entry ("dragSourceIndex") to "insertionIndex" within
	 * "items", called on mouse release at the end of a drag&amp;drop reorder
	 * gesture.
	 */
	private void finishReorder() {
		if (dragSourceIndex < 0 || insertionIndex < 0 || dragSourceIndex >= items.size()) {
			return;
		}

		if (insertionIndex != dragSourceIndex && insertionIndex != dragSourceIndex + 1) {
			final String movedItem = items.remove(dragSourceIndex);

			final int insertAt = insertionIndex > dragSourceIndex ? insertionIndex - 1 : insertionIndex;

			items.add(insertAt, movedItem);

			selectedMatch = insertAt;

			/*
			 * Refresh the popup to show the new order and keep it open - reordering only
			 * ever happens while browsing the full, unfiltered list, so "matches" is
			 * simply "items" again in the new order. Routing through updateMatches()
			 * instead would close the popup for an empty "userText".
			 */
			matches = new ArrayList<>(items);

			showPopup();

			final List<String> newOrder = new ArrayList<>(items);
			for (final Consumer<List<String>> listener : itemsReorderedListeners) {
				listener.accept(newOrder);
			}
		}
	}

	/**
	 * Translates a mouse position inside the popup list into the index a dragged
	 * entry would be inserted at (0 = before the first entry, itemCount = after the
	 * last entry), based on which half of the hovered row the pointer is in.
	 */
	private static int computeInsertionIndex(final JList<String> list, final Point point) {
		final int itemCount = list.getModel().getSize();
		if (itemCount <= 0) {
			return 0;
		}

		final int hoveredIndex = list.locationToIndex(point);
		if (hoveredIndex < 0) {
			return 0;
		}

		final Rectangle cell = list.getCellBounds(hoveredIndex, hoveredIndex);
		if (cell == null) {
			return hoveredIndex;
		}

		final int insertAt = point.y - cell.y < cell.height / 2 ? hoveredIndex : hoveredIndex + 1;

		return Math.max(0, Math.min(insertAt, itemCount));
	}

	/**
	 * Paints the thin marker bar showing where a dragged entry would be inserted.
	 */
	private void paintInsertionMarker(final JList<String> list, final Graphics g) {
		if (!dragInProgress || insertionIndex < 0) {
			return;
		}

		final int itemCount = list.getModel().getSize();
		if (itemCount == 0) {
			return;
		}

		final int markerY;
		if (insertionIndex < itemCount) {
			markerY = list.getCellBounds(insertionIndex, insertionIndex).y;
		} else {
			final Rectangle lastCell = list.getCellBounds(itemCount - 1, itemCount - 1);
			markerY = lastCell.y + lastCell.height;
		}

		g.setColor(insertionMarkerColor);
		g.fillRect(0, Math.max(0, Math.min(list.getHeight() - 2, markerY - 1)), list.getWidth(), 2);
	}

	/**
	 * Paints the hint text overlay while the field is empty.
	 */
	private void paintHint(final JTextPane pane, final Graphics g) {
		if (Utilities.isEmpty(hintText) || pane.getDocument().getLength() > 0) {
			return;
		}

		final Graphics2D g2 = (Graphics2D) g.create();
		try {
			g2.setColor(suggestionColor);
			g2.setFont(pane.getFont());

			final Insets insets = pane.getInsets();
			final FontMetrics fontMetrics = g2.getFontMetrics();

			BasicGraphicsUtils.drawString(pane, g2, hintText, insets.left, insets.top + fontMetrics.getAscent());
		} finally {
			g2.dispose();
		}
	}

	/**
	 * Preferred width of the visible text area: by columns if set, otherwise by
	 * the widest item or hint text (like JComboBox).
	 */
	private int computePreferredTextWidth(final JTextPane pane) {
		final FontMetrics fontMetrics = pane.getFontMetrics(pane.getFont());
		final Insets insets = pane.getInsets();

		int textWidth;
		if (columns > 0) {
			textWidth = columns * fontMetrics.charWidth('m');
		} else {
			textWidth = 5 * fontMetrics.charWidth('m');
			for (final String item : items) {
				if (item != null) {
					textWidth = Math.max(textWidth, fontMetrics.stringWidth(item));
				}
			}
			if (!Utilities.isEmpty(hintText)) {
				textWidth = Math.max(textWidth, fontMetrics.stringWidth(hintText));
			}
		}

		// Plus room for the caret at the end of the text
		return textWidth + insets.left + insets.right + 2;
	}

	/**
	 * Makes the embedded text pane look like a regular JTextField of the current
	 * look and feel.
	 */
	private void applyLookAndFeelDefaults() {
		Border border = UIManager.getBorder("TextField.border");
		if (border == null) {
			border = UIManager.getBorder("ScrollPane.border");
		}
		textScrollPane.setBorder(border);
		textScrollPane.setViewportBorder(null);

		textPane.setBorder(BorderFactory.createEmptyBorder(1, 2, 1, 2));

		final Font font = UIManager.getFont("TextField.font");
		if (font != null) {
			textPane.setFont(font);
		}

		textPane.setForeground(UIManager.getColor("TextField.foreground"));
		textPane.setCaretColor(UIManager.getColor("TextField.caretForeground"));
		textPane.setSelectionColor(UIManager.getColor("TextField.selectionBackground"));
		textPane.setSelectedTextColor(UIManager.getColor("TextField.selectionForeground"));
		textPane.setDisabledTextColor(UIManager.getColor("TextField.inactiveForeground"));

		applyBackground();
	}

	private void applyBackground() {
		Color background;
		if (isEnabled()) {
			background = UIManager.getColor("TextField.background");
		} else {
			background = UIManager.getColor("TextField.inactiveBackground");
			if (background == null) {
				background = UIManager.getColor("TextField.disabledBackground");
			}
			if (background == null) {
				background = UIManager.getColor("Panel.background");
			}
		}

		textPane.setBackground(background);
		textScrollPane.getViewport().setBackground(background);
	}

	/**
	 * Fires an ActionEvent to all ActionListeners.
	 *
	 * <p>
	 * The event is only fired when a value was actually accepted.
	 * </p>
	 */
	private void fireActionEvent() {
		final ActionListener[] listeners = listenerList.getListeners(ActionListener.class);
		if (listeners.length == 0) {
			return;
		}

		final ActionEvent event = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, ACTION_COMMAND_VALUE_ACCEPTED);
		for (final ActionListener listener : listeners) {
			listener.actionPerformed(event);
		}
	}

	/**
	 * Fires a ChangeEvent if the entered text differs from the last reported one.
	 */
	private void fireTextChangedIfNeeded() {
		if (Objects.equals(lastReportedText, userText)) {
			return;
		}

		lastReportedText = userText;

		final ChangeListener[] listeners = listenerList.getListeners(ChangeListener.class);
		if (listeners.length == 0) {
			return;
		}

		final ChangeEvent event = new ChangeEvent(this);
		for (final ChangeListener listener : listeners) {
			listener.stateChanged(event);
		}
	}

	/**
	 * Updates the "valid" field and notifies the validationListeners, but only if
	 * the validity state actually changed.
	 */
	private void setValid(final boolean newValid) {
		if (valid == newValid) {
			return;
		}

		valid = newValid;

		for (final Consumer<Boolean> listener : validationListeners) {
			listener.accept(valid);
		}
	}

	/**
	 * Returns the index of the match that equals normalizedSearch exactly (after
	 * normalize()), or -1 if none of the matches is an exact match.
	 */
	private int findExactMatchIndex(final List<String> candidateMatches, final String normalizedSearch) {
		for (int i = 0; i < candidateMatches.size(); i++) {
			if (normalize(candidateMatches.get(i)).equals(normalizedSearch)) {
				return i;
			}
		}
		return -1;
	}

	private static String removeLineBreaks(final String value) {
		if (value == null) {
			return "";
		}

		return value.replace("\r", "").replace("\n", "");
	}

	/**
	 * Routes every user edit of the document through handleUserEdit(), and lets
	 * our own rendering (while "updating" is set) pass through unchanged.
	 */
	private final class InputFilter extends DocumentFilter {
		@Override
		public void insertString(final FilterBypass filterBypass, final int offset, final String string, final AttributeSet attributes) throws BadLocationException {
			if (updating) {
				filterBypass.insertString(offset, string, attributes);
			} else {
				handleUserEdit(offset, 0, string);
			}
		}

		@Override
		public void remove(final FilterBypass filterBypass, final int offset, final int length) throws BadLocationException {
			if (updating) {
				filterBypass.remove(offset, length);
			} else {
				handleUserEdit(offset, length, "");
			}
		}

		@Override
		public void replace(final FilterBypass filterBypass, final int offset, final int length, final String text, final AttributeSet attributes) throws BadLocationException {
			if (updating) {
				filterBypass.replace(offset, length, text, attributes);
			} else {
				handleUserEdit(offset, length, text);
			}
		}
	}

	/**
	 * Mouse handling of the popup list: click to accept, hover to highlight, drag
	 * to reorder.
	 */
	private final class PopupMouseHandler extends MouseAdapter {
		@Override
		public void mousePressed(final MouseEvent e) {
			if (!SwingUtilities.isLeftMouseButton(e)) {
				return;
			}

			mouseDownPoint = e.getPoint();
			dragInProgress = false;
			insertionIndex = -1;

			final int index = popupList.locationToIndex(e.getPoint());
			dragSourceIndex = index;

			// Single press: preview the entry, but don't accept it yet
			if (index >= 0 && index < matches.size()) {
				selectedMatch = index;

				renderText(userText, matches.get(selectedMatch));
			}
		}

		@Override
		public void mouseReleased(final MouseEvent e) {
			if (!SwingUtilities.isLeftMouseButton(e)) {
				return;
			}

			if (dragInProgress) {
				finishReorder();

				dragInProgress = false;
				dragSourceIndex = -1;
				insertionIndex = -1;

				if (popupList != null) {
					popupList.repaint();
				}

				return;
			}

			dragSourceIndex = -1;

			final int index = popupList.getSelectedIndex();

			if (index >= 0 && index < matches.size()) {
				selectedMatch = index;

				acceptValue(matches.get(index));
			}
		}

		@Override
		public void mouseMoved(final MouseEvent e) {
			/*
			 * Passive hover (no button held): highlight the entry under the cursor and
			 * keep "selectedMatch" in sync, so Delete removes the hovered entry and Enter
			 * accepts it. Deliberately no live preview in the text field here.
			 */
			final int hoveredIndex = popupList.locationToIndex(e.getPoint());

			if (hoveredIndex >= 0 && hoveredIndex < matches.size() && popupList.getSelectedIndex() != hoveredIndex) {
				popupList.setSelectedIndex(hoveredIndex);

				selectedMatch = hoveredIndex;
			}
		}

		@Override
		public void mouseDragged(final MouseEvent e) {
			if (!reorderable || !SwingUtilities.isLeftMouseButton(e) || mouseDownPoint == null) {
				return;
			}

			if (dragSourceIndex < 0 || matches.size() != items.size()) {
				return;
			}

			if (!dragInProgress) {
				final int deltaX = Math.abs(e.getX() - mouseDownPoint.x);
				final int deltaY = Math.abs(e.getY() - mouseDownPoint.y);

				if (deltaX <= CLICK_MOVEMENT_THRESHOLD_PX && deltaY <= CLICK_MOVEMENT_THRESHOLD_PX) {
					return;
				}

				dragInProgress = true;
			}

			// JList's own drag handling moves the selection along with the pointer, keep the dragged entry selected
			if (popupList.getSelectedIndex() != dragSourceIndex) {
				popupList.setSelectedIndex(dragSourceIndex);
			}

			insertionIndex = computeInsertionIndex(popupList, e.getPoint());

			popupList.repaint();
		}
	}

	/**
	 * Editor kit producing a single, non-wrapping line, so the JTextPane behaves
	 * like a text field that scrolls horizontally inside its viewport.
	 */
	private static final class SingleLineEditorKit extends StyledEditorKit {
		private static final long serialVersionUID = -6420427931093766143L;

		private final ViewFactory viewFactory = element -> {
			final String kind = element.getName();
			if (AbstractDocument.ParagraphElementName.equals(kind)) {
				return new NoWrapParagraphView(element);
			} else if (AbstractDocument.SectionElementName.equals(kind)) {
				return new BoxView(element, View.Y_AXIS);
			} else if (StyleConstants.ComponentElementName.equals(kind)) {
				return new ComponentView(element);
			} else if (StyleConstants.IconElementName.equals(kind)) {
				return new IconView(element);
			} else {
				return new LabelView(element);
			}
		};

		@Override
		public ViewFactory getViewFactory() {
			return viewFactory;
		}
	}

	/**
	 * Paragraph view that lays out its content on one unlimited-width row.
	 */
	private static final class NoWrapParagraphView extends ParagraphView {
		NoWrapParagraphView(final Element element) {
			super(element);
		}

		@Override
		public void layout(final int width, final int height) {
			super.layout(Short.MAX_VALUE, height);
		}

		@Override
		public float getMinimumSpan(final int axis) {
			return super.getPreferredSpan(axis);
		}
	}
}

