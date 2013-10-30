package de.soderer.utilities.swt;

import java.util.Date;

public abstract class DualProgressWorker<T> extends Worker<T> {
	protected long subTodo = -1;
	protected Date lastSubItemProgressShow = new Date();
	protected Date lastSubItemTitleShow = new Date();
	
	public DualProgressWorker(ProgressShowingModalDialog<?> parent) {
		super(parent);
	}
	
	protected void showSubItemsDone(long subItemsDone) {
		if (!cancel && new Date().getTime() - lastSubItemProgressShow.getTime() > showProgressAfterMilliseconds) {
			if (parent instanceof DualProgressShowingModalDialog) {
				((DualProgressShowingModalDialog<?>) parent).showSubItemProgress(subTodo, subItemsDone);
			}
			lastSubItemProgressShow = new Date();
		}
	}
	
	protected void setSubItemTitle(String subItemTitle) {
		if (!cancel && new Date().getTime() - lastSubItemTitleShow.getTime() > showProgressAfterMilliseconds) {
			if (parent instanceof DualProgressShowingModalDialog) {
				((DualProgressShowingModalDialog<?>) parent).setSubItemTitle(subItemTitle);
			}
			lastSubItemTitleShow = new Date();
		}
	}
}
