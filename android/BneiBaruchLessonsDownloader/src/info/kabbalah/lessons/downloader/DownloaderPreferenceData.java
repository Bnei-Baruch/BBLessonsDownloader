package info.kabbalah.lessons.downloader;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class DownloaderPreferenceData {
	public String selectedLanguage;
	public boolean bMp3Low;
	public boolean bMp3High;
	public boolean bMp4;
	public int nRemoveFiles;
	public int checkSchedule;
	public boolean checkWithCellular;
	public boolean proxyEnabled;
	public String proxyHost;
	public int proxyPort;

	public DownloaderPreferenceData() {
	}

	public void readPreferences(Context downloader) {
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(downloader);
		readPreferencesFrom(pref);
	}

	private void readPreferencesFrom(SharedPreferences pref) {
		FileSystemUtilities.resumeDownloads = pref.getBoolean("resume_failed_downloads", false);
		FileSystemUtilities.folderName = pref.getString("local_folder_name", FileSystemUtilities.folderName);
		checkSchedule = Integer.parseInt(pref.getString("dl_check_schedule", "0"));
		checkWithCellular = pref.getBoolean("check_with_cellular", false);
		selectedLanguage = pref.getString("language", "heb");
		bMp3Low = pref.getBoolean("mp3_low", true);
		bMp3High = false; //pref.getBoolean("mp3_high", false);
		bMp4 = pref.getBoolean("mp4", false);
		nRemoveFiles = Integer.parseInt(pref.getString("remove_old_files_list", "0"));
		proxyEnabled = pref.getBoolean("proxy_enabled", false);
		proxyHost = pref.getString("proxy_host_name", "");
		proxyPort = Integer.parseInt(pref.getString("proxy_port_number", "80"));
	}

	public void onSharedPreferenceChanged(SharedPreferences pref, String key) {
		readPreferencesFrom(pref);
	}
}