package info.kabbalah.lessons.downloader;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.Calendar;

public class BootSetScheduleReceiver extends BroadcastReceiver {

	public static void renewAlarm(Context ctx) {
		AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);

		// am start -a info.kabbalah.lessons.downloader.CheckFiles
		PendingIntent operation = buildSyncIntent(ctx);

		am.cancel(operation);

		Calendar date = Calendar.getInstance();
		date.set(date.get(Calendar.YEAR), date.get(Calendar.MONTH), date.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
		am.setRepeating(AlarmManager.RTC_WAKEUP,
				date.getTimeInMillis(),
//				 10 * 1000,
				60 * 60 * 1000,
				operation);
	}

	private static PendingIntent buildSyncIntent(Context context) {
		Intent intent = new Intent(
				 MediaDownloaderService.INFO_KABBALAH_LESSONS_DOWNLOADER_CHECK_FILES);

        return PendingIntent.getService(context, 0, intent, 0);
	}

	@Override
	public void onReceive(Context ctx, Intent intent) {
		renewAlarm(ctx);
	}

}
