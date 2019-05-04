package info.kabbalah.lessons.downloader;

import android.os.AsyncTask;
import android.util.Log;

import java.net.HttpURLConnection;
import java.net.URL;

class DownloadFilesTask extends AsyncTask<FileProcessor, Integer, Long> {
	private final MediaDownloaderService mediaDownloaderService;
	private boolean failed = false;
	private int contentLength;
	private String fileName;
	
	public DownloadFilesTask(MediaDownloaderService mediaDownloaderService) {
		this.mediaDownloaderService = mediaDownloaderService;
	}

	public void publishProgress(Integer i)
	{
		super.publishProgress(i);
	}

	@Override
	protected Long doInBackground(FileProcessor... finfos) {
        int count = finfos.length;
        long totalSize = 0;
		for (FileProcessor fp : finfos) {
			FileInfo fileInfo = fp.getFileInfo();
			try {
				HttpURLConnection connection = (HttpURLConnection) MediaDownloaderService
        								.getConnectionWithProxy(new URL(fileInfo.getUrl()));
				contentLength = connection.getContentLength();
        		if(contentLength > 0)
        		{
        			fileInfo.setFileSize(contentLength);
        			fileInfo.setLastModified(connection.getLastModified());
        			publishProgress(0);
        		}
        		else
        			publishProgress(-1);
        			
				FileDownloader fileDownloader = new FileDownloader();
				fileName = fileInfo.getName();
				long size = fileDownloader.downloadFile(fileInfo, this);
				fileInfo.setDownloadedSize(size);
				totalSize += size;
				mediaDownloaderService.downloadComplete(fileInfo);
			} catch (Exception e) {
				failed = true;
				mediaDownloaderService.downloadFailed(fileInfo);
				Log.e("DownloadFilesTask", "Download Error", e);
			}
        }
        return totalSize;
	}

	@Override
	protected void onProgressUpdate (Integer... values)
	{
		mediaDownloaderService.setProgressBar(contentLength,
				values[0] < 0 ? 0: values[0],
				values[0] < 0, fileName);
	}

// --Commented out by Inspection START (26/03/2015 14:00):
//	public boolean isFailed()
//	{
//		return failed;
//	}
// --Commented out by Inspection STOP (26/03/2015 14:00)

	protected void onPostExecute(Long result) {
		mediaDownloaderService.cleanUp();
	}
}
