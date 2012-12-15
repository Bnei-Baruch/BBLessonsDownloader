package info.kabbalah.lessons.downloader;


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
		return Environment.getExternalStoragePublicDirectory(
				Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() +
				"/" + getMediaSubFolder();
	}

}