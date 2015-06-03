package info.kabbalah.lessons.downloader;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;

class FileDownloader extends FileSystemUtilities {
	private DownloadFilesTask downloadFilesTask = null;

	public long downloadFile(FileInfo fileInfo, DownloadFilesTask downloadFilesTask)
		throws IOException, URISyntaxException {
		String url = fileInfo.getUrl();
		String localPath = getDefaultLocalPath() + "/" + fileInfo.getDate() + "/" + fileInfo.getName();
		
		File dir = new File(getDefaultLocalPath() + "/" + fileInfo.getDate());
		if(! dir.exists() && ! dir.mkdirs())
			return -1;
		
		File file = new File(localPath);
		if(file.exists() && 
			(fileInfo.getLastModified() != 0 
					&& file.lastModified() < fileInfo.getLastModified()
					|| fileInfo.getLastModified() == 0 )
					&& fileInfo.getFileSize() != file.length())
		{
			file.delete();
			fileInfo.setDownloadedSize(0);
		} else if(file.exists())
		{
			fileInfo.setDownloadedSize(file.length());
		}
		
		fileInfo.setLocalPath(localPath);
		this.downloadFilesTask = downloadFilesTask;
		long size = getFileFromURL(url, localPath, fileInfo.getDownloadedSize());
		if(size == -2)
		{
			fileInfo.setExisted(true);
			return fileInfo.getDownloadedSize();
		} else
			return size;
	}

	long getFileFromURL(String url, String localPath, long downloadedSize) throws
            IOException {
		/* Open a connection to that URL. */
		HttpURLConnection inp = MediaDownloaderService.getConnectionWithProxy(new URL(url));
		
		inp.setChunkedStreamingMode(4096);
		
		if( ! FileSystemUtilities.resumeDownloads) downloadedSize = 0; 
		
		File file = new File(localPath);
		if(downloadedSize > 0)
		{
			inp.setRequestProperty("Range", "bytes=" + downloadedSize + "-");
		}
		/*
		 * Define InputStreams to read from the URLConnection.
		 */
		InputStream is = inp.getInputStream();
		
		String range = inp.getHeaderField("Content-Range");
		
		Log.d("getFileFromURL", "Range: " + (range == null ? "<null>" : range) );

		BufferedInputStream bis = new BufferedInputStream(is, 4096);
		
		if(file.exists() && file.canWrite() && file.length() == inp.getContentLength())
		{
			return -2;
		}
		
		if(! file.exists() && ! file.createNewFile()) return -1;
				
		int size = (int) downloadedSize;
		int count = 0;
		BufferedOutputStream os = new BufferedOutputStream(
				new FileOutputStream(file, downloadedSize > 0));
		try {
			do {
				byte[] buf = new byte[4096];
				count =	bis.read(buf);
				if(count > 0)
				{
					os.write(buf, 0, count);
					size += count;
					if(downloadFilesTask != null)
					{
						if(downloadFilesTask.isCancelled())
						{
							return size;
						}
						downloadFilesTask.publishProgress(size);
					}
				}
			} while(count > 0);
		} finally {
			os.close();
		}
		return size;
	}
}
