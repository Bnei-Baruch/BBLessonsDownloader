package info.kabbalah.lessons.downloader;


import android.os.Environment;

import java.io.File;

class FileSystemUtilities {

	public static String folderName = "MorningLessons";
	public static boolean resumeDownloads = false;

	public static String getMediaSubFolder() {
		return folderName;
	}

// --Commented out by Inspection START (26/03/2015 14:01):
//	FileSystemUtilities() {
//		super();
//	}
// --Commented out by Inspection STOP (26/03/2015 14:01)

	public static String getDefaultLocalPath() {
		String localFSPath = null;
		localFSPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getAbsolutePath();
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