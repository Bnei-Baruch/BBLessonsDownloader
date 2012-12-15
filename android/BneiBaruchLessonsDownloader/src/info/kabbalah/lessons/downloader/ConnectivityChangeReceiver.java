package info.kabbalah.lessons.downloader;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

public class ConnectivityChangeReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context ctx, Intent intent) {
		if(intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
		    NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
		    if(networkInfo.isConnected()) {
		        // Wifi is connected
		        Log.d("LessonDownloader", "Wifi is connected: " + String.valueOf(networkInfo));
		    }
		} else if(intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
		    NetworkInfo networkInfo = intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
		    if(networkInfo.getType() == ConnectivityManager.TYPE_WIFI && ! networkInfo.isConnected()) {
		        // Wifi is disconnected
		        Log.d("LessonDownloader", "Wifi is disconnected: " + String.valueOf(networkInfo));
		    }
		    if(networkInfo.isConnected())
		    {
				Intent srvc = new Intent(
						 MediaDownloaderService.INFO_KABBALAH_LESSONS_DOWNLOADER_CHECK_FILES);
				ctx.startService(srvc);
		    }
		}
	}

}
