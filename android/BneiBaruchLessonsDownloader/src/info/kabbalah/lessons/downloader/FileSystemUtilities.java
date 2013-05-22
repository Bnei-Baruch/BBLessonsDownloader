package info.kabbalah.lessons.downloader;


import java.io.File;

import android.os.Environment;

public class FileSystemUtilities {

	public static String folderName = "MorningLessons";
	public static boolean resumeDownloads = false;

	public static String getMediaSubFolder() {
		return folderName;
	}

	public FileSystemUtilities() {
		super();
	}

	public static String getDefaultLocalPath() {
		String localFSPath = null;
		localFSPath = getLocalPath();
		return localFSPath + File.separator + getMediaSubFolder();
	}

	public static String getLocalPath() {
		String localFSPath;
		if(Environment.getExternalStorageState() != null &&
				! Environment.getExternalStorageState().equalsIgnoreCase(android.os.Environment.MEDIA_REMOVED)) {
			localFSPath = Environment.getExternalStoragePublicDirectory(
					Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();			
		} else {
			localFSPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getAbsolutePath();
		}
		return localFSPath;
	}

}