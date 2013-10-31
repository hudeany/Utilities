package de.soderer.utilities.swt;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.DeviceData;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Path;
import org.eclipse.swt.graphics.Pattern;
import org.eclipse.swt.graphics.Region;
import org.eclipse.swt.graphics.TextLayout;
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import de.soderer.utilities.collection.CaseInsensitiveMap;

public class SwtUtilities {
	public static final int BOTTOM_RIGHT_ENTER_KEY = 16777296;
	
	public static CaseInsensitiveMap<Integer> getSwtHandlesStatus(Display display) {
		DeviceData swtDeviceData = display.getDeviceData();
		
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

		for (Object object : swtDeviceData.objects) {
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
		
		CaseInsensitiveMap<Integer> result = new CaseInsensitiveMap<Integer>();
		
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
	
	public static void expandTree(Tree tree) {
		for (TreeItem subItem : tree.getItems()) {
			expandTree(subItem);
		}
	}
	
	public static void expandTree(TreeItem item) {
		item.setExpanded(true);
		for (TreeItem subItem : item.getItems()) {
			expandTree(subItem);
		}
	}
}
