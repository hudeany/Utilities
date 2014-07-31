package de.soderer.utilities.swt;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.io.IOUtils;

import de.soderer.utilities.UserError;

public class FileDownloadWorker extends Worker<Boolean> {
	private static long SHOW_PROGRESS_AFTER_MILLISECONDS = 500;

	private String downloadUrl = null;
	private long fileSize = -1;
	private String destinationFilePath = null;
	private Boolean result = false;
	
	private Exception error = null;
	private boolean cancel = false;
	private boolean isDone = false;
	
	public FileDownloadWorker(ProgressShowingModalDialog<?> parent, String downloadUrl, String destinationFilePath) {
		super(parent);
		this.destinationFilePath = destinationFilePath;
		this.downloadUrl = downloadUrl;
	}
	
	@Override
	public boolean cancel(boolean arg0) {
		cancel = true;
		return cancel;
	}

	@Override
	public Boolean get() throws InterruptedException, ExecutionException {
		if (error != null) {
			throw new ExecutionException(error);
		} else {
			return result;
		}
	}

	@Override
	public Boolean get(long arg0, TimeUnit arg1) throws InterruptedException, ExecutionException, TimeoutException {
		return get();
	}

	@Override
	public boolean isCancelled() {
		return cancel;
	}

	@Override
	public boolean isDone() {
		return isDone;
	}

	@Override
	public void run() {
		long position = 0L;
		Date lastProgressShow = new Date();
		try {
			BufferedInputStream bufferedInputStream = null;
			FileOutputStream fileOutputStream = null;
			try {
				parent.showProgress(fileSize, 0L);
				URLConnection urlConnection = new URL(downloadUrl).openConnection();
		    	fileSize = urlConnection.getContentLength();
				InputStream inputStream = urlConnection.getInputStream();
				parent.showProgress(fileSize, 0L);
				bufferedInputStream = new BufferedInputStream(inputStream);
				fileOutputStream = new FileOutputStream(destinationFilePath);
				
				byte[] buffer = new byte[4096];
				int readLength;
				while ((readLength = bufferedInputStream.read(buffer)) != -1) {
					if (cancel) {
						break;
					} else {
						fileOutputStream.write(buffer, 0, readLength);
						position += readLength;
						
						if (!cancel && new Date().getTime() - lastProgressShow.getTime() > SHOW_PROGRESS_AFTER_MILLISECONDS) {
							parent.showProgress(fileSize, position);
							lastProgressShow = new Date();
						}
					}
				}
			} catch (Exception e) {
				if (e.getMessage().toLowerCase().contains("server returned http response code: 401")) {
					throw new UserError("error.userNotAuthenticatedOrNotAuthorized", UserError.Reason.UnauthenticatedOrUnauthorized);
				} else {
					throw new Exception("Cannot download file", e);
				}
			} finally {
				IOUtils.closeQuietly(fileOutputStream);
				IOUtils.closeQuietly(bufferedInputStream);
			}
			
			if (cancel) {
				new File(destinationFilePath).delete();
			} else {
				parent.showProgress(fileSize, fileSize);
				result = true;
			}
		} catch (Exception e) {
			error = e;
			result = false;
		}
		
		parent.showDone();
		
		isDone = true;
	}
}
