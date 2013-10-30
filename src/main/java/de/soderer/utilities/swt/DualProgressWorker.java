package de.soderer.utilities.swt;

import java.util.Date;

public abstract class DualProgressWorker<T> extends Worker<T> {
	protected long subTodo = -1;
	
	public DualProgressWorker(ProgressShowingModalDialog<?> parent) {
		super(parent);
	}
	
	protected void showSubItemsDone(long subItemsDone) {
		if (!cancel && new Date().getTime() - lastProgressShow.getTime() > showProgressAfterMilliseconds) {
			if (parent instanceof DualProgressShowingModalDialog) {
				((DualProgressShowingModalDialog<?>) parent).showSubItemProgress(subTodo, subItemsDone);
			}
			lastProgressShow = new Date();
		}
	}
	
	protected void setSubItemTitle(String subItemTitle) {
		if (!cancel && new Date().getTime() - lastProgressShow.getTime() > showProgressAfterMilliseconds) {
			if (parent instanceof DualProgressShowingModalDialog) {
				((DualProgressShowingModalDialog<?>) parent).setSubItemTitle(subItemTitle);
			}
			lastProgressShow = new Date();
		}
	}
}
