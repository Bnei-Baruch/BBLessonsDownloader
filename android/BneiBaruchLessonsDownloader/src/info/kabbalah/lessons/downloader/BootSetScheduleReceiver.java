package info.kabbalah.lessons.downloader;

import java.util.Calendar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootSetScheduleReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context ctx, Intent intent) {
		renewAlarm(ctx);
	}

	public static void renewAlarm(Context ctx) {
		AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
		 // am start -a info.kabbalah.lessons.downloader.CheckFiles
		PendingIntent operation = buildSyncIntent(ctx);
		
		am.cancel(operation);
		 
		am.setRepeating(AlarmManager.RTC_WAKEUP, 
				 Calendar.getInstance().getTimeInMillis(),
				 3 * 60 * 60 * 1000,
				 operation);
	}

	public static PendingIntent buildSyncIntent(Context context) {
		Intent intent = new Intent(
				 MediaDownloaderService.INFO_KABBALAH_LESSONS_DOWNLOADER_CHECK_FILES);
		 
		PendingIntent operation = PendingIntent.getService(context, 0, intent, 0);
		return operation;
	}

}
