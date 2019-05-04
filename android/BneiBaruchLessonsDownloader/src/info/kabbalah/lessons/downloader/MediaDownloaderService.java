package info.kabbalah.lessons.downloader;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import info.kabbalah.lessons.downloader.R.string;


public class MediaDownloaderService	extends android.app.Service {
	public static final String INFO_KABBALAH_LESSONS_DOWNLOADER_CHECK_FILES = "info.kabbalah.lessons.downloader.CheckFiles";
	public static final String INFO_KABBALAH_LESSONS_DOWNLOADER_WIFI_ON = "info.kabbalah.lessons.downloader.Network.WiFi.On";
	public static final String INFO_KABBALAH_LESSONS_DOWNLOADER_WIFI_OFF = "info.kabbalah.lessons.downloader.Network.WiFi.Off";
	private static boolean proxyEnabled;
	private static String proxyHost;
	private static int proxyPort;
    public final DownloaderPreferenceData data = new DownloaderPreferenceData();
    // This is the object that receives interactions from clients.  See
    // RemoteService for a more complete example.
    private final IBinder mBinder = new LocalBinder();
    private final ArrayList<FileProcessor> filesToDownload = new ArrayList<>();
    private final ArrayList<FileProcessor> processedFiles = new ArrayList<>();
    private NotificationManager mNM;
    private WifiLock wifilock;
    private DownloadFilesTask task = null;
    private long time;
    private Downloader downloader = null;
    private int PLAY_NOTIFICATION_ID = string.local_service_started + 123;
    private WakeLock powerlock;
    private boolean bWifiConnected;

	public static URLConnection getConnectionWithProxy(URL url)
            throws IOException {
        if (proxyEnabled) {
            Proxy p = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(
                    InetAddress.getByName(proxyHost), proxyPort));
			return url.openConnection(p);
        } else
			return url.openConnection();
    }

    @Override
    public void onCreate() {
        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        data.readPreferences(this);

		WifiManager wifimanager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifilock = wifimanager.createWifiLock(WifiManager.WIFI_MODE_FULL, "LessonDownloader");
        
        PowerManager powermanager = (PowerManager) getSystemService(POWER_SERVICE);
		powerlock = powermanager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "LessonDownloader:DownloadWake");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
    	if(intent == null) return START_NOT_STICKY;
        Log.i("LocalService", "Received start id " + startId + ": " + intent);
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
       	this.bWifiConnected = isWiFiConnected(getApplicationContext());
        data.readPreferences(this);
        long hours = Calendar.getInstance().getTimeInMillis() / 1000 / 60 / 60;
//	        long hours = Calendar.getInstance().getTimeInMillis() / 1000 / 10;
        if(data.checkSchedule != 0 && hours % data.checkSchedule == 0)
        {
            // Check WiFi status
            if(data.checkWithCellular || this.bWifiConnected) {
                checkAll();
            }
        }
        return START_STICKY;
     }

    private boolean isWiFiConnected(Context ctx) {
        ConnectivityManager cm = (ConnectivityManager)ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        return ni != null && ni.isConnected() && ni.getType() == ConnectivityManager.TYPE_WIFI;
    }

    private void checkAll() {
    	retrieveProxySettings();
    	checkNow();
		checkYesterday();
	}

	@Override
    public void onDestroy() {
        // Cancel the persistent notification.
        int NOTIFICATION = string.local_service_started;
        mNM.cancel(NOTIFICATION);
        // Tell the user we stopped.
        //Toast.makeText(this, R.string.local_service_stopped, Toast.LENGTH_SHORT).show();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    /**
     * Show a notification while this service is running.
     * @param intentToPlayVideo - what to start to play
     * @param fileInfo - file information
     */
    private void showNotification(FileInfo fileInfo, Intent intentToPlayVideo) {
        // In this sample, we'll use the same text for the ticker and the expanded notification
        CharSequence text = getText(intentToPlayVideo == null ? string.file_failed : string.file_downloaded);

        mNM.notify(PLAY_NOTIFICATION_ID++,
                new Notification.Builder(this)
                        .setSmallIcon(R.drawable.nicon)
                        .setAutoCancel(true)
                        .setContentIntent(PendingIntent.getActivity(this, 0, intentToPlayVideo, 0))
                        .setContentTitle(text)
                        .setContentText(fileInfo.getName())
                        .build());

    }

	void startDownload() {
        if(task == null)
        {
            sendDownloadNotification(100, 0, true, "");
    		
			wifilock.acquire();
			powerlock.acquire();
			task = new DownloadFilesTask(this);
	
			task.execute(filesToDownload.toArray(new FileProcessor[filesToDownload.size()]));
			filesToDownload.clear();
        }
	}

	private void sendDownloadNotification(int max, int cur, boolean indeterminate, String fileName) {
		CharSequence text = getText(string.local_service_started);

        mNM.notify(string.DownloadingFile, new Notification.Builder(this)
                .setSmallIcon(R.drawable.nicon)
                .setAutoCancel(true)
                .setOngoing(true)
                .setProgress(max, cur, indeterminate)
                .setContentTitle("Downloading")
                .setContentText(fileName)
                .setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, Downloader.class), 0))
                .build());
	}

	public void setProgressBar(int max, int progress, boolean indeterminate, String fileName) {
		if(progress == max || time + 1000 < System.currentTimeMillis())
		{
			sendDownloadNotification(max, progress, indeterminate, fileName);
			time = System.currentTimeMillis();
		}
	}

	public void downloadFailed(FileInfo fileInfo) {
        //showNotification(fileInfo, null);

        if(getDownloader() != null)
        	getDownloader().downloadFailed(fileInfo);
	}

	public void downloadComplete(FileInfo fileInfo) {
        if(getDownloader() != null)
        	getDownloader().downloadComplete(fileInfo);

        if(!fileInfo.getExisted())
        {
			Intent intentToPlayVideo = new Intent(Intent.ACTION_DEFAULT);
	        intentToPlayVideo.setDataAndType(Uri.parse("file://" + fileInfo.getLocalPath()),
	        		fileInfo.getLocalPath().contains(".mp3") ? "audio/*" : "video/*");
	        
	        showNotification(fileInfo, intentToPlayVideo);
        }
	}

	public void cleanUp() {
        if(getDownloader() != null)
        	getDownloader().finishedDownload();
		mNM.cancel(string.DownloadingFile);
		wifilock.release();
		powerlock.release();
		task = null;
	}

    Downloader getDownloader() {
        return downloader;
    }

	public void setDownloader(Downloader downloader) {
		this.downloader = downloader;
	}
	
	private void downloadFileList(int offset) {
		DropBoxFileList.startDownLoadFileList(offset, this);
	}
	
	public void pushFileList(List<FileInfo> fileList) {
    	if(fileList == null)
    	{
    		showFileNotFoundToast();
    		return;
    	}
    	boolean bFileAdded = false;
    	for(FileInfo info : fileList)
    	{
    		String name = info.getName();
			if(data.selectedLanguage.equalsIgnoreCase(name.substring(0, 3))
    				&& ! included(name, processedFiles)
    				&& (
    					   data.bMp3Low && name.contains(".mp3") && ! name.contains("96k.mp3")
    					|| data.bMp3High && name.contains("96k.mp3")
    					|| data.bMp4 && name.contains(".mp4")
    					)
    			)
    		{
				filesToDownload.add(new FileProcessor(info));
				bFileAdded = true;
    		}
    	}
		if(bFileAdded)
		{
			startDownload();
		} else
			showFileNotFoundToast();
	}

	private boolean included(String name,
			ArrayList<FileProcessor> processedFiles) {
		for(int i = 0; i < processedFiles.size(); ++i)
		{
			FileInfo fileInfo = processedFiles.get(i)
					.getFileInfo();
			if(fileInfo.getName().equalsIgnoreCase(name))
			{
				if(fileInfo.getFileSize() > 0)
					return true;
				else {
					processedFiles.remove(i);
					return false;
				}
			}
		}
		return false;
	}
	
	public void loadFilesFromFolder(String folder, ArrayList<FileProcessor> files) {
		File dir = new File(FileSystemUtilities.getDefaultLocalPath() 
			+ File.separator 
			+ folder); 
		if(dir.exists() && dir.isDirectory())
		{
			for(File file : dir.listFiles())
			{
				files.add(new FileProcessor(new FileInfo(file, folder)));
			}
		}
	}

	private void checkAndDeleteOldFilesAndMusic() {
		if(data.nRemoveFiles != 0)
		{
			MusicUtils.cleanOldPlaylists(this, 
					FileSystemUtilities.getMediaSubFolder(), data.nRemoveFiles);
			
			deleteFilesOlderThanNdays(data.nRemoveFiles, 
					FileSystemUtilities.getDefaultLocalPath());
			
			DropBoxFileList.deleteFileListsOlderThanNDays(data.nRemoveFiles);
		}
	}

	public void checkNow()
	{
		checkAndDeleteOldFilesAndMusic();
		checkFilesForOffset(0);
	}

	public void checkYesterday() {
		checkAndDeleteOldFilesAndMusic();
		checkFilesForOffset(-1);
	}

	private void checkFilesForOffset(int offset) {
		downloadFileList(offset);
	}
	
	private void showFileNotFoundToast() {
		Toast.makeText(this, string.NoFileFound, Toast.LENGTH_LONG).show();
	}

	private void deleteFilesOlderThanNdays(int daysBack, String dirWay) {
	
		File directory = new File(dirWay);
		if(directory.exists()){
	
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.DAY_OF_MONTH, -daysBack);
			long purgeTime = cal.getTimeInMillis(); 
			if(removeFiles(directory, purgeTime))
			{
				sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, 
						Uri.parse("file://" 
								+ Environment.getExternalStorageDirectory())));
			}
		} else {
			Log.w("deleteFilesOlderThan..", "Files were not deleted, directory " + dirWay + " does'nt exist!");
			if(! directory.mkdirs())
                Log.e("deleteFilesOlderThan...", "Cannot create directory: " + directory);
		}
	}

	private boolean removeFiles(File directory, long purgeTime) {
		boolean filesRemoved = false;
		File[] listFiles = directory.listFiles();
		if(listFiles == null) return false;
		for(File listFile : listFiles) {
			if(listFile.isDirectory())
			{
				filesRemoved = removeFiles(listFile, purgeTime) || filesRemoved;
				if(! listFile.delete())
                    Log.e("removeFiles", "Unable to delete directory: " + listFile);
			}
			if(listFile.lastModified() < purgeTime) {
				if(!listFile.delete()) {
					Log.e("deleteFilesOlderThan...", "Unable to delete file: " + listFile);
				} else {
					filesRemoved = true;
				}
			}
		}
		return filesRemoved;
	}

	public void reloadPreferences(SharedPreferences arg0, String arg1) {
        data.onSharedPreferenceChanged(arg0, arg1);
        retrieveProxySettings();
	}
	
	private void retrieveProxySettings() {
		proxyEnabled = data.proxyEnabled;
		proxyHost = data.proxyHost;
		proxyPort = data.proxyPort;
	}

    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    public class LocalBinder extends Binder {
        public MediaDownloaderService getService() {
            return MediaDownloaderService.this;
        }
    }

}
