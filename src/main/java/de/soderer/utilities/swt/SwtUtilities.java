package de.soderer.utilities.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.DeviceData;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Path;
import org.eclipse.swt.graphics.Pattern;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Region;
import org.eclipse.swt.graphics.TextLayout;
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import de.soderer.utilities.collection.CaseInsensitiveMap;

public class SwtUtilities {
	public static final int BOTTOM_RIGHT_ENTER_KEY = 16777296;

	public static CaseInsensitiveMap<Integer> getSwtHandlesStatus(final Display display) {
		final DeviceData swtDeviceData = display.getDeviceData();

		int colors = 0;
		int cursors = 0;
		int fonts = 0;
		int gcs = 0;
		int images = 0;
		int paths = 0;
		int patterns = 0;
		int regions = 0;
		int textLayouts = 0;
		int transforms = 0;
		int others = 0;

		for (final Object object : swtDeviceData.objects) {
			if (object instanceof Color) {
				colors++;
			} else if (object instanceof Cursor) {
				cursors++;
			} else if (object instanceof Font) {
				fonts++;
			} else if (object instanceof GC) {
				gcs++;
			} else if (object instanceof Image) {
				images++;
			} else if (object instanceof Path) {
				paths++;
			} else if (object instanceof Pattern) {
				patterns++;
			} else if (object instanceof Region) {
				regions++;
			} else if (object instanceof TextLayout) {
				textLayouts++;
			} else if (object instanceof Transform) {
				transforms++;
			} else {
				others++;
			}
		}

		final CaseInsensitiveMap<Integer> result = new CaseInsensitiveMap<>();

		result.put("Colors", colors);
		result.put("Cursors", cursors);
		result.put("Fonts", fonts);
		result.put("GCs", gcs);
		result.put("Images", images);
		result.put("Paths", paths);
		result.put("Patterns", patterns);
		result.put("Regions", regions);
		result.put("TextLayouts", textLayouts);
		result.put("Transforms", transforms);
		result.put("Others", others);

		return result;
	}

	public static void expandTree(final Tree tree, final boolean expandValue) {
		for (final TreeItem subItem : tree.getItems()) {
			expandTree(subItem, expandValue);
		}
	}

	public static void expandTree(final TreeItem item, final boolean expandValue) {
		item.setExpanded(expandValue);
		for (final TreeItem subItem : item.getItems()) {
			expandTree(subItem, expandValue);
		}
	}

	public static String getAbsolutePath(final TreeItem selectedFolderItem) {
		String path = selectedFolderItem.getText();
		TreeItem parentTreeItem = selectedFolderItem.getParentItem();
		while (parentTreeItem != null) {
			path = parentTreeItem.getText() + (parentTreeItem.getText().endsWith("/") ? "" : "/") + path;
			parentTreeItem = parentTreeItem.getParentItem();
		}
		return path;
	}

	public static GridLayout createNoMarginGridLayout(final int columns, final boolean equalWidth) {
		final GridLayout gridLayout = new GridLayout(columns, equalWidth);
		gridLayout.marginWidth = 0;
		gridLayout.marginHeight = 0;
		return gridLayout;
	}

	public static GridLayout createSmallMarginGridLayout(final int columns, final boolean equalWidth) {
		final GridLayout gridLayout = new GridLayout(columns, equalWidth);
		gridLayout.marginWidth = 5;
		gridLayout.marginHeight = 5;
		return gridLayout;
	}

	public static Shell createCenteredShellIfNeeded(final Shell shell) {
		Shell newShell = shell;
		if (newShell == null) {
			newShell = new Shell();
			final Monitor primary = newShell.getDisplay().getPrimaryMonitor ();
			final Rectangle bounds = primary.getBounds ();
			final Rectangle rect = newShell.getBounds ();
			final int x = bounds.x + (bounds.width - rect.width) / 2;
			final int y = bounds.y + (bounds.height - rect.height) / 2;
			newShell.setLocation (x, y);
		}
		return newShell;
	}

	public static void addAutoCompleteFeature(final Combo combo) {
		combo.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(final KeyEvent keyEvent) {
				final Combo cmb = ((Combo) keyEvent.getSource());
				setClosestMatch(cmb);
			}

			@Override
			public void keyPressed(final KeyEvent keyEvent) {
				if (keyEvent.keyCode == SWT.BS) {
					final Combo comboBox = ((Combo) keyEvent.getSource());
					final Point pt = comboBox.getSelection();
					comboBox.setSelection(new Point(Math.max(0, pt.x - 1), pt.y));
				} else if (keyEvent.keyCode == SWT.CR) {
					final Combo comboBox = ((Combo) keyEvent.getSource());
					comboBox.notifyListeners(SWT.Selection, new Event());
				}
			}

			private void setClosestMatch(final Combo comboBox) {
				final String str = comboBox.getText();
				final String[] cItems = comboBox.getItems();
				int index = -1;
				for (int i = 0; i < cItems.length; i++) {
					if (cItems[i].toLowerCase().startsWith(str.toLowerCase())) {
						index = i;
						break;
					}
				}

				if (index != -1) {
					final Point pt = comboBox.getSelection();
					comboBox.select(index);
					comboBox.setText(cItems[index]);
					comboBox.setSelection(new Point(pt.x, cItems[index].length()));
				} else {
					comboBox.setText("");
				}
			}
		});
	}
}

