package info.kabbalah.lessons.downloader;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

public class Downloader extends Activity
	implements OnSharedPreferenceChangeListener {
	
	public class MediaScannerClient implements MediaScannerConnectionClient {

		private final Downloader instance;

		public MediaScannerClient(Downloader downloader) {
			instance = downloader;
		}

		public void onMediaScannerConnected() {
			instance.onMediaScannerConnected();
		}

		public void onScanCompleted(String path, Uri uri) {
			instance.onScanCompleted(path, uri);
		}

	}

	private MediaDownloaderService mBoundService;

	private Handler mHandler = null;
	private boolean mIsBound = false;
	private boolean bMediaScannerReady = false;
	private FileProcessorArrayAdapter processedFiles = null;
	private final Map<String, FileProcessor> nameIndex = new HashMap<>();
    private Uri todayPlaylist = null;
	private MediaScannerConnection mediaScannerConnection;
	private final DownloaderPreferenceData data = new DownloaderPreferenceData();

	private final ServiceConnection mConnection = new ServiceConnection() {
	    public void onServiceConnected(ComponentName className, IBinder service) {
	        // This is called when the connection with the service has been
	        // established, giving us the service object we can use to
	        // interact with the service.  Because we have bound to a explicit
	        // service that we know is running in our own process, we can
	        // cast its IBinder to a concrete class and directly access it.
	        mBoundService = ((MediaDownloaderService.LocalBinder)service).getService();
	        mBoundService.setDownloader(Downloader.this);
//	        mBoundService.setFileList(processedFiles.getList());
	        
	        // Tell the user about this for our demo.
	        //Toast.makeText(Downloader.this, R.string.local_service_connected,
	        //        Toast.LENGTH_SHORT).show();
	        
	        showToday();
	    }

	    public void onServiceDisconnected(ComponentName className) {
	        // This is called when the connection with the service has been
	        // unexpectedly disconnected -- that is, its process crashed.
	        // Because it is running in our same process, we should never
	        // see this happen.
	        mBoundService.setDownloader(null);
	        mBoundService = null;
	        //Toast.makeText(Downloader.this, R.string.local_service_disconnected,
	        //        Toast.LENGTH_SHORT).show();
	    }
	};

	void doBindService() {
	    // Establish a connection with the service.  We use an explicit
	    // class name because we want a specific service implementation that
	    // we know will be running in our own process (and thus won't be
	    // supporting component replacement by other applications).
	    bindService(new Intent(Downloader.this, 
	    		MediaDownloaderService.class), mConnection, Context.BIND_AUTO_CREATE);
	    mIsBound = true;
	}

	void onScanCompleted(String path, Uri uri) {

		final FileInfo fileInfo = nameIndex.get(path).getFileInfo();

		fileInfo.setUri(uri);

		createPlaylistForTodayFiles();
		
		mHandler.post( new Runnable() {

			public void run() {
				if(nameIndex.containsKey(fileInfo.getLocalPath()))
				{
					for(int index = 0; index < processedFiles.getCount(); ++index)
					{
						FileProcessor item = processedFiles.getItem(index);
						if(item.getLocalPath().compareTo(fileInfo.getLocalPath()) == 0)
							processedFiles.remove(item);
					}
				}
				FileProcessor fileProcessor = new FileProcessor(fileInfo);
				processedFiles.add(fileProcessor);
				processedFiles.notifyDataSetChanged();
			}});

	}

	void onMediaScannerConnected() {
		bMediaScannerReady = true;
	}

	void doUnbindService() {
	    if (mIsBound) {
	        // Detach our existing connection.
	        unbindService(mConnection);
	        mIsBound = false;
	    }
	}

	@Override
	protected void onDestroy() {
	    super.onDestroy();
	    doUnbindService();
		mediaScannerConnection.disconnect();
    	PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);     
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState)
	{
		try {
	    	super.onRestoreInstanceState(savedInstanceState);
	//    	ArrayList<FileProcessor> files = savedInstanceState.getParcelableArrayList("processedFiles");
	//    	mBoundService.setFileList(files);
		} catch (Exception e) {
			Log.e(this.getClass().getName(), "Couldn't restore state", e);
		}
	}

    protected void onPause()
    {
    	super.onPause();
    }
    
    protected void onResume()
    {
    	super.onResume();
    }

    protected void onStop()
    {
    	super.onStop();
    }

    protected void onStart()
    {
    	super.onStart();
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        PreferenceManager.getDefaultSharedPreferences(this)
        			.registerOnSharedPreferenceChangeListener(this);
        getPreferencesData().readPreferences(this);

        ListView fileList = (ListView) findViewById(R.id.fileListId);
        
        fileList.setOnItemClickListener(new OnItemClickListener() {

            public void onItemClick(AdapterView<?> listView,
                                    View view, int position,
                                    long id) {
                FileProcessor fp = (FileProcessor) listView.getItemAtPosition(position);
                if (fp == null) return;
                FileInfo fileInfo = fp.getFileInfo();
                if (fileInfo == null) return;
                Intent intentToPlayMedia = new Intent(Intent.ACTION_DEFAULT);
                intentToPlayMedia.setDataAndType(
                        Uri.parse("file://" + fileInfo.getLocalPath()),
                        fileInfo.getMimeType());
                startActivity(intentToPlayMedia);
            }
        });
        
        mHandler = new Handler();
		mediaScannerConnection = new MediaScannerConnection(this, new MediaScannerClient(this));
		mediaScannerConnection.connect();

        doBindService();
        
        BootSetScheduleReceiver.renewAlarm(this);
        
        ArrayList<FileProcessor> files = /*null;
        
        if(savedInstanceState != null)
        {
        	try {
        		files = savedInstanceState.getParcelableArrayList("processedFiles");
        	} catch (Exception e) {}
    	}

        if(files == null)
    		files =*/ new ArrayList<>();
    	
		processedFiles = new FileProcessorArrayAdapter(Downloader.this, 
				R.id.fileListId, files);

		if(fileList != null)
        	fileList.setAdapter(this.processedFiles);
        
    }

    public void onPlayNowClick(View v)
    {
   	try {
        	if(todayPlaylist != null)
        	{
        		Intent intentToPlayMedia = new Intent(Intent.ACTION_DEFAULT);
                intentToPlayMedia.setDataAndType(todayPlaylist, MediaStore.Audio.Playlists.CONTENT_TYPE);
                startActivity(intentToPlayMedia);
        	}
    	} catch (Exception e) {
			Log.e("onPlayNowClick", "Cannot play playlist.", e);
		}
    }

    public void onCheckNowClick(View v)
    {
        if(mBoundService != null)
    	    mBoundService.checkNow();
    }

    public void onDonateClick(View v)
    {
    	Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.kabbalah.info/donations/"));
    	startActivity(browserIntent);
    }

    public void onCheckYesterdayClick(View v)
    {
    	mBoundService.checkYesterday();
    }

    public boolean onCreateOptionsMenu (Menu menu)
	{
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.menu, menu);
	    return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	    case R.id.checkNow:
	    	mBoundService.checkNow();
	    	return true;
	    case R.id.oneDay:
	    	showYesterday();
	    	return true;
	    case R.id.zeroDay:
	    	showToday();
	    	return true;
	    case R.id.preferences:
	    	showPreferences();
	    	return true;
	    default:
	        return super.onOptionsItemSelected(item);
	    }
	}

	private void showPreferences() {
		Intent prefIntent = new Intent(Downloader.this, DownloaderPreferences.class);
		startActivity(prefIntent);
	}

	private void showToday() {
		String folder = getFolderPath(Calendar.getInstance(), 0);

		processedFiles.clear();
		processedFiles.notifyDataSetChanged();

		ArrayList<FileProcessor> fileList = new ArrayList<>();
		mBoundService.loadFilesFromFolder(folder, fileList);
		for(FileProcessor fp : fileList)
		{
			rescanMedia(fp.getFileInfo());
		}
	}

	private void showYesterday() {
		String folder = getFolderPath(Calendar.getInstance(), -1);

		processedFiles.clear();
		processedFiles.notifyDataSetChanged();

		ArrayList<FileProcessor> fileList = new ArrayList<>();
		mBoundService.loadFilesFromFolder(folder, fileList);
		for(FileProcessor fp : fileList)
		{
			rescanMedia(fp.getFileInfo());
		}
	}

	private String getFolderPath(Calendar today, int diff) {
		today.add(Calendar.DAY_OF_YEAR, diff);
		return String.format(Locale.getDefault(), "%04d-%02d-%02d", today.get(Calendar.YEAR),
				today.get(Calendar.MONTH) + 1,
				today.get(Calendar.DAY_OF_MONTH));
	}

	public void downloadFailed(FileInfo fileInfo) {
	}

	public void downloadComplete(final FileInfo fileInfo) {
		rescanMedia(fileInfo);
	}
	
	Uri createPlaylist(String name)
	{
        ContentResolver resolver = getContentResolver();
        int id = idForplaylist(name);
        Uri uri;
        if (id >= 0) {
            uri = ContentUris.withAppendedId(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, id);
            MusicUtils.clearPlaylist(this, id);
        } else {
            ContentValues values = new ContentValues(1);
            values.put(MediaStore.Audio.Playlists.NAME, name);
            uri = resolver.insert(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, values);
        }
        return uri;
	}

	private int idForplaylist(String name) {
        Cursor c = MusicUtils.query(this, MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                new String[] { MediaStore.Audio.Playlists._ID },
                MediaStore.Audio.Playlists.NAME + "=?",
                new String[] { name },
                MediaStore.Audio.Playlists.NAME);
        int id = -1;
        if (c != null) {
            c.moveToFirst();
            if (!c.isAfterLast()) {
                id = c.getInt(0);
            }
            c.close();
        }
        return id;
	}

	boolean createPlaylistForTodayFiles() {
		return createTodayPlaylist() && createPlaylistForPastNDay(0);
	}

// --Commented out by Inspection START (26/03/2015 14:00):
//	public boolean createPlaylistForYesterdayFiles() {
//		return createPlaylistForPastNDay(1);
//	}
// --Commented out by Inspection STOP (26/03/2015 14:00)

	synchronized boolean createPlaylistForPastNDay(int N) {
		String folderPath = getFolderPath(Calendar.getInstance(), -N);
		String like = "%" + folderPath + "%lesson%";
		long[] songs = MusicUtils.getSongIdLikeName(this, like);
		if(songs.length > 0)
		{
			String playlistName = FileSystemUtilities.folderName + "_" + folderPath;
			this.todayPlaylist = createPlaylist(playlistName);
			int plId = idForplaylist(playlistName);			
			MusicUtils.addToPlaylist(this, songs, plId);
			return true;
		} else {
			return false;
		}
	}

	synchronized boolean createTodayPlaylist() {
		String folderPath = getFolderPath(Calendar.getInstance(), 0);
		String like = "%" + folderPath + "%lesson%";
		long[] songs = MusicUtils.getSongIdLikeName(this, like);
		if(songs.length > 0)
		{
			String playlistName = "TodayMorningLessons";
			createPlaylist(playlistName);
			int plId = idForplaylist(playlistName);			
			MusicUtils.addToPlaylist(this, songs, plId);
			return true;
		} else {
			return false;
		}
	}

	void rescanMedia(FileInfo fi) {
		nameIndex.put(fi.getLocalPath(), new FileProcessor(fi));
		if(bMediaScannerReady && mediaScannerConnection.isConnected())
		{
			mediaScannerConnection.scanFile(fi.getLocalPath(), fi.getMimeType());
		}
	}

	public void finishedDownload() {
		createPlaylistForTodayFiles();
	}

	public void onSharedPreferenceChanged(SharedPreferences arg0, String arg1) {
		getPreferencesData().onSharedPreferenceChanged(arg0, arg1);
		if(mBoundService != null)
		{
			mBoundService.reloadPreferences(arg0, arg1);
		}
	}

	DownloaderPreferenceData getPreferencesData() {
		return data;
	}

}