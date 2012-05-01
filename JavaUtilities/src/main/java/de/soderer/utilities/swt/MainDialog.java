package de.soderer.utilities.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public abstract class MainDialog extends Shell {
	private Display display;
	
	public MainDialog(Display display) throws Exception {
		super(display, SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MIN | SWT.MAX);
		
		this.display = display;
		
		createWidgets();
	}
	
	public Display getDisplay() {
		return display;
	}
	
	public void run() {
		open();
		Display display = getDisplay();
		while (!this.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}
	
	@Override
	protected void checkSubclass() {
	}
	
	public abstract void createWidgets() throws Exception;
}
