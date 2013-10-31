package de.soderer.utilities.swt;

import java.util.Date;

public abstract class DualProgressWorker<T> extends Worker<T> {
	protected long subTodo = -1;
	protected Date lastSubItemProgressShow = new Date();
	
	public DualProgressWorker(ProgressShowingModalDialog<?> parent) {
		super(parent);
	}
	
	protected void showSubItemsDone(long subItemsDone) {
		if (!cancel && (new Date().getTime() - lastSubItemProgressShow.getTime() > showProgressAfterMilliseconds || subItemsDone == 0)) {
			if (parent instanceof DualProgressShowingModalDialog) {
				((DualProgressShowingModalDialog<?>) parent).showSubItemProgress(subTodo, subItemsDone);
			}
			lastSubItemProgressShow = new Date();
		}
	}
	
	protected void setSubItemTitle(String subItemTitle) {
		if (!cancel) {
			if (parent instanceof DualProgressShowingModalDialog) {
				((DualProgressShowingModalDialog<?>) parent).setSubItemTitle(subItemTitle);
			}
		}
	}
}
