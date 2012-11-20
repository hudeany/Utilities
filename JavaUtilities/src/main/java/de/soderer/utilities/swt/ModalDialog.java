package de.soderer.utilities.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public abstract class ModalDialog<T> extends Dialog {
	private Shell shell;
	private T returnValue;
	
	public ModalDialog(Shell parentShell) {
		super(new Shell(parentShell, SWT.TITLE | SWT.BORDER | SWT.APPLICATION_MODAL));
	}
	
	public ModalDialog(Shell parentShell, int style) {   
		super(new Shell(parentShell, style | SWT.TITLE | SWT.BORDER | SWT.APPLICATION_MODAL));
	}
	
	protected Shell getShell() {
		return shell;
	}
	
	public T open() {
		shell = getParent();
		try {
			createComponents(shell);
		} catch (Exception e) {
			throw new RuntimeException("Cannot create dialog components", e);
		}
		
		setPosition(shell);
	    
	    shell.open();
	    Display display = getParent().getDisplay();
	    while (!shell.isDisposed()) {
	    	if (!display.readAndDispatch())
	    		display.sleep();
	    }
	    
	    return returnValue;
	}
	
	protected void setReturnValue(T value) {
		returnValue = value;
	}
	
	protected void setPosition(Shell shell) {
		// Sets the position centered of parent
		shell.setLocation(
				shell.getParent().getLocation().x + shell.getParent().getSize().x / 2 - shell.getSize().x / 2,
				shell.getParent().getLocation().y + shell.getParent().getSize().y / 2 - shell.getSize().y / 2);
	}
	
	protected abstract void createComponents(Shell parentShell) throws Exception;
}
