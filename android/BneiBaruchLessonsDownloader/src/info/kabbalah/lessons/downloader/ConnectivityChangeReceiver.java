package info.kabbalah.lessons.downloader;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.util.List;

public class ConnectivityChangeReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context ctx, Intent intent) {
		Log.d("LessonDownloader", "Connection Change Received");
        ConnectivityManager cm = (ConnectivityManager)ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        PackageManager pm = ctx.getPackageManager();
        if(intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
		    NetworkInfo networkInfo = cm.getActiveNetworkInfo();
		    if(networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_WIFI && networkInfo.isConnected()) {
		        // Wifi is connected
		        Log.d("LessonDownloader", "Wifi is connected: " + String.valueOf(networkInfo));
		    }
		} else if(intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
		    NetworkInfo networkInfo = cm.getActiveNetworkInfo();
		    if(networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_WIFI && ! networkInfo.isConnected()) {
		        // Wifi is disconnected
		        Log.d("LessonDownloader", "Wifi is disconnected: " + String.valueOf(networkInfo));
		    }
		    if(networkInfo != null && networkInfo.isConnected() && networkInfo.getType() == ConnectivityManager.TYPE_WIFI)
		    {
                final Intent wifiOn = new Intent(
                        MediaDownloaderService.INFO_KABBALAH_LESSONS_DOWNLOADER_WIFI_ON);
                wifiOn.setClass(ctx, MediaDownloaderService.class);
                ctx.startService(wifiOn);
		    } else if(networkInfo != null && ! networkInfo.isConnected() && networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                final Intent wifiOff = new Intent(
                        MediaDownloaderService.INFO_KABBALAH_LESSONS_DOWNLOADER_WIFI_OFF);
                wifiOff.setClass(ctx, MediaDownloaderService.class);
                ctx.startService(wifiOff);
		    }
		}
	}

}
