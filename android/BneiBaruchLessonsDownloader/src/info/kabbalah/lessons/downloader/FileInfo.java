package info.kabbalah.lessons.downloader;

import java.io.File;

import org.w3c.dom.Node;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

public class FileInfo  implements Parcelable {
	private String name;
	private String date;
	private String url;
	private String localPath;
	private long fileSize;
	private long downloadedSize;
	private Uri uri;
	private boolean existed;
	private long lastModified;

	public FileInfo(Node file) {
		name = file.getAttributes().getNamedItem("Name").getNodeValue();
		date = file.getAttributes().getNamedItem("Date").getNodeValue();
		url = file.getAttributes().getNamedItem("Url").getNodeValue();
	}

	public FileInfo(Parcel in) {
		name = in.readString();
		date = in.readString();
		url = in.readString();
		localPath = in.readString();
		downloadedSize = in.readLong();
		fileSize = in.readLong();
	}

	public FileInfo(File file, String folder) {
		name = file.getName();
		localPath = file.getAbsolutePath();
		downloadedSize = file.length();
		fileSize = 0;
		date = folder;
		url = "";
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public String getDate() {
		return date;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getUrl() {
		return url;
	}

	public String getLocalPath() {
		return localPath;
	}

	public void setLocalPath(String localPath) {
		this.localPath = localPath;
	}

	public int describeContents() {
		return 0;
	}

	public void writeToParcel(Parcel out, int arg1) {
		out.writeString(name);
		out.writeString(date);
		out.writeString(url);
		out.writeString(localPath);
		out.writeLong(downloadedSize);
		out.writeLong(fileSize);
	}
	
    public static final Parcelable.Creator<FileInfo> CREATOR = new Parcelable.Creator<FileInfo>() {
    	public FileInfo createFromParcel(Parcel in) {
    		return new FileInfo(in);
    	}

		public FileInfo[] newArray(int size) {
		    return new FileInfo[size];
		}
    };

	public void setDownloadedSize(long downloadFile) {
		downloadedSize = downloadFile;		
	}

	public long getDownloadedSize() {
		return downloadedSize;		
	}

	public void setFileSize(long fileSize) {
		this.fileSize = fileSize;
	}

	public long getFileSize() {
		return fileSize;
	}

	public boolean isDownloaded() {
		return downloadedSize > 0 && downloadedSize == fileSize;
	}

	public String getMimeType() {
		return this.name.contains(".mp3") ? "audio/*" : "video/*";
	}

	public void setUri(Uri uri) {
		this.uri = uri;
	}

	public Uri getUri() {
		return uri;
	}

	public void setExisted(boolean b) {
		existed = b;
	}

	public boolean getExisted() {
		return existed;
	}

	public void setLastModified(long lastModified) {
		this.lastModified = lastModified; 
	}

	public long getLastModified() {
		return lastModified;
	}
}