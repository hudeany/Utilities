package de.soderer.utilities.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import de.soderer.utilities.LangResources;

public abstract class ModalDialog<T> extends Dialog {
	private Point location = null;
	private T returnValue;
	
	public ModalDialog(Shell parentShell, String title) {
		super(new Shell(parentShell, SWT.TITLE | SWT.BORDER | SWT.APPLICATION_MODAL));
		getParent().setText(title);
	}
	
	public ModalDialog(Shell parentShell, int style, String title) {   
		super(new Shell(parentShell, style | SWT.TITLE | SWT.BORDER | SWT.APPLICATION_MODAL));
		getParent().setText(title);
	}
	
	public T open() {
		try {
			createComponents(getParent());
		} catch (Exception e) {
			throw new RuntimeException("Cannot create dialog components", e);
		}
		
		setPosition(getParent());
	    
	    getParent().open();
	    Display display = getParent().getDisplay();
	    while (!getParent().isDisposed()) {
	    	if (!display.readAndDispatch())
	    		display.sleep();
	    }
	    
	    if (returnValue == null) {
	    	return getDefaultReturnValue();
	    } else {
	    	return returnValue;
	    }
	}
	
	public void hide() {
		getParent().setVisible(false);
	}
	
	public void show() {
		getParent().setVisible(true);
	}
	
	public void setLocation(Point value) {
		location = value;
	}
	
	public boolean isDisposed() {
		return getParent().isDisposed();
	}
	
	protected void setReturnValue(T value) {
		returnValue = value;
	}
	
	protected void setPosition(Shell shell) {
		int xPosition;
		int yPosition;
		if (location == null) {
			// Sets the position centered of parent
			xPosition = shell.getParent().getLocation().x + shell.getParent().getSize().x / 2 - shell.getSize().x / 2;
			yPosition = shell.getParent().getLocation().y + shell.getParent().getSize().y / 2 - shell.getSize().y / 2;
		} else {
			xPosition = location.x;
			yPosition = location.y;
		}
		xPosition = Math.max(0, xPosition);
		yPosition = Math.max(0, yPosition);
		shell.setLocation(xPosition, yPosition);
	}
	
	protected T getDefaultReturnValue() {
		return null;
	}

	protected void showMessage(String title, String text) {
		new QuestionDialog(getParent(), title, text, LangResources.get("ok")).open();
	}
	
	protected boolean showQuestion(String text, String... buttonTextProperties) {
		if (buttonTextProperties == null) {
			return new QuestionDialog(getParent(), getText(), text, LangResources.get("ok"), LangResources.get("cancel")).open() == 0;
		} else {
			String[] buttonTexts = new String[buttonTextProperties.length];
			for (int i = 0; i < buttonTextProperties.length; i++) {
				buttonTexts[i] = LangResources.get(buttonTextProperties[i]);
			}
			return new QuestionDialog(getParent(), getText(), text, buttonTexts).open() == 0;
		}
	}
	
	protected abstract void createComponents(Shell parentShell) throws Exception;
}
