package info.kabbalah.lessons.downloader;

import java.net.HttpURLConnection;
import java.net.URL;

import android.os.AsyncTask;
import android.util.Log;

public class DownloadFilesTask extends AsyncTask<FileProcessor, Integer, Long> {
	boolean failed = false;
	MediaDownloaderService mediaDownloaderService;
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
        for (int i = 0; i < count; i++) {
            FileInfo fileInfo = finfos[i].getFileInfo();
			try {
        		HttpURLConnection connection = MediaDownloaderService
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

	public boolean isFailed()
	{
		return failed;
	}
	
	protected void onPostExecute(Long result) {
		mediaDownloaderService.cleanUp();
	}
}
