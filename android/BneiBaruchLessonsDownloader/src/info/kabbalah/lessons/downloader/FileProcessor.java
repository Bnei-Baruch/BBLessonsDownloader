package info.kabbalah.lessons.downloader;

import android.os.Parcel;
import android.os.Parcelable;

public class FileProcessor  implements Parcelable {
	FileInfo info = null;

	public FileProcessor(FileInfo info) {
		super();
		this.info = info;		
	}
	public FileProcessor(Parcel source) {
		super();
		info = source.readParcelable(null);		
	}
	public boolean complete() {
		return info.isDownloaded();
	}
	public FileInfo getFileInfo() {
		return info;
	}
	public String getLocalPath() {
		return info.getLocalPath();
	}
	public int describeContents() {
		return 0;
	}
	public void writeToParcel(Parcel arg0, int arg1) {
		arg0.writeParcelable(info, 0);		
	}
	
	public static final Parcelable.Creator<FileProcessor> CREATOR = new Parcelable.Creator<FileProcessor>()
	{

		public FileProcessor createFromParcel(Parcel source) {
			return new FileProcessor(source);
		}

		public FileProcessor[] newArray(int size) {
			return new FileProcessor[size];
		}

	};
}
