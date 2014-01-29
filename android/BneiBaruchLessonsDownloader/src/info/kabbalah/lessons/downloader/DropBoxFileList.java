package info.kabbalah.lessons.downloader;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.util.ByteArrayBuffer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import android.os.AsyncTask;
import android.util.Log;

public class DropBoxFileList {
	final static String uri = "http://mylibrary.kbb1.com/api/morning_lessons.json?lang=%2$s";
	//final static String uri = "http://dl.dropbox.com/u/3074981/%s.txt";
	static List<FileInfo> fileList = null;
	private static MediaDownloaderService caller;

	public static void startDownLoadFileList(int offset, MediaDownloaderService mediaDownloaderService) {

		caller = mediaDownloaderService;
		startDownloadFileList(offset);
	}
	
	public synchronized static void pushFileList(String fileListJson, String dateFilter) {
		try {	
			caller.pushFileList(parseFileListJson(fileListJson, dateFilter));
		} catch (Exception e) {
			Log.d("DropBoxFileListDownloader", e.toString());
		}
	}

	private static void startDownloadFileList(int offset) {
		Calendar today = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tel_Aviv"));
		today.add(Calendar.DAY_OF_MONTH, offset);

		String title = String
				.format(Locale.US, "%04d-%02d-%02d", today.get(Calendar.YEAR),
						today.get(Calendar.MONTH) + 1,
						today.get(Calendar.DAY_OF_MONTH));
		String sUrl = String.format(uri, title, caller.data.selectedLanguage.toUpperCase(Locale.US));
		URL url = null;
		try {
			url = new URL(sUrl);
			final File storagePublicDirectory = new File(FileSystemUtilities.getLocalPath());
			if( ! storagePublicDirectory.exists())
				storagePublicDirectory.mkdir();
			File localFileList = new File( storagePublicDirectory.getAbsolutePath() + File.separatorChar + title + caller.data.selectedLanguage + ".txt");
			startGetUrlTextContent(url, localFileList, title);
		} catch (MalformedURLException e) {
		}
	}

	private static void startGetUrlTextContent(final URL url, final File localFileList, final String dateFilter) {
		
		class GetFileListAT extends AsyncTask<URL, Void, String> {
			@Override
			protected String doInBackground(URL... params) {
				/* Open a connection to that URL. */
				try {
					HttpURLConnection ucon = MediaDownloaderService.getConnectionWithProxy(url);
					try {
						if(localFileList.exists() && localFileList.canRead())
						{
							ucon.setIfModifiedSince(localFileList.lastModified());
						}
						
						ucon.setChunkedStreamingMode(4096);
				
						/*
						 * Define InputStreams to read from the URLConnection.
						 */
				
						InputStream is = ucon.getInputStream();
			/*			if (!url.getHost().equals(ucon.getURL().getHost())) {
							// Redirected ! Kick the user out to the browser to sign on?
							ShowBrowserLogOn(ucon);
						}*/
						if(is == null && ! localFileList.exists())
						{
							return null;
						}
						long lastModified = ucon.getLastModified();
						byte[] content = null;
						if(lastModified != 0 && lastModified > localFileList.lastModified()
								|| lastModified == 0)
						{
							content = readToString(is);
							if(content.length <= 0 && ! localFileList.exists())
							{
								return null;
							}
	
							if(localFileList.length() != content.length)
							{
								OutputStream os = new FileOutputStream(localFileList);
								try {
									os.write(content);
								} finally {
									os.close();
								}
							}
	
							return new String(content);
						} else if(localFileList.exists()){
							byte[] buffer = new byte[(int) localFileList.length()];
							FileInputStream file = new FileInputStream(localFileList);
							try {
								file.read(buffer);
							} finally {
								if(file != null)
									file.close();
							}
							return new String(buffer);
						}
					} catch (Exception e) {
						Log.d("DropBoxFileList", "Cannot download file list", e);
					} finally {
						ucon.disconnect();
					}
				} catch (IOException e)
				{
					return null;
				}
				return null;
			}
			protected void onPostExecute(String ret)
			{
				if(ret != null)
					pushFileList(ret, dateFilter);
			}
		}
		
		new GetFileListAT().execute(url);
		
	}

	private static byte[] readToString(InputStream is) throws IOException {
		BufferedInputStream bis = new BufferedInputStream(is);
		try {
			/*
			 * Read bytes to the Buffer until there is nothing more to read(-1).
			 */
	
			ByteArrayBuffer baf = new ByteArrayBuffer(5000);
			int current = 0;
			byte buf[] = new byte[1024];
			while ((current = bis.read(buf)) != -1) {
				baf.append(buf, 0, current);
			}
	
			return baf.toByteArray();
		} finally {
			bis.close();
			is.close();
		}
	}

    private static List<FileInfo> parseFileListJson(String flieListJson, String dateFilter) throws JSONException {
        JSONObject json= new JSONObject(flieListJson);
        fileList = new ArrayList<FileInfo>();
        if(json.has("morning_lessons")) {
            JSONArray jlist= json.getJSONArray("morning_lessons");
            for(int i= 0; i < jlist.length(); i++) {
                JSONArray jdates= jlist.getJSONObject(i).getJSONArray("dates");
                for(int j= 0; j < jdates.length(); j++) {
                    JSONObject jdate = jdates.getJSONObject(j);
                    String date=jdate.getString("date");
                    if(!date.equals(dateFilter)) continue;
                    JSONArray jfiles= jdate.getJSONArray("files");
                    for(int k= 0; k < jfiles.length(); k++) {
                        fileList.add(new FileInfo(date, jfiles.getJSONObject(k)));
                    }
                }
            }
        }
        return fileList;
    }
	private static List<FileInfo> parseFileListXml(String flieListXml)
			throws ParserConfigurationException, SAXException, IOException {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

		DocumentBuilder db = dbf.newDocumentBuilder();

		Document doc = db.parse(
				new InputSource(new StringReader(
						flieListXml
				)));

		NodeList files = doc.getElementsByTagName("File");
		if(files.getLength() == 0)
		{
			Log.d("DropBoxFileListDownloader", "No files found.");
			return null;
		}
		fileList = new ArrayList<FileInfo>();
		for (int i = 0; i < files.getLength(); ++i) {
			Node file = files.item(i);
			fileList.add(new FileInfo(file));
		}
		return fileList;
	}

	public static void deleteFileListsOlderThanNDays(int nRemoveFiles) {
		final Calendar date = Calendar.getInstance();
		final Pattern datepattern = Pattern.compile("[0-9]{8}.txt"); 
		date.add(Calendar.DAY_OF_YEAR, -nRemoveFiles);

		final File dir = new File(FileSystemUtilities.getLocalPath());
		
		if(dir != null)
		{
			dir.mkdirs();
			
			final File[] toDelete = dir.listFiles(new FilenameFilter() {
				
				public boolean accept(File dir, String filename) {
					if(datepattern.matcher(filename).matches())
					{
						try {
							String year = filename.substring(0, 4);
							String month = filename.substring(4, 6);
							String day = filename.substring(6, 8);
							int iYear = Integer.parseInt(year);
							int iMonth = Integer.parseInt(month) - 1;
							int iDay = Integer.parseInt(day);
							Calendar cal = new GregorianCalendar();
							cal.setTime(new Date(iYear - 1900, iMonth, iDay));
							
							return date.before(cal);
						} catch (Exception e)
						{
							return false;
						}
					} else
						return false;
				}
			});
			if(toDelete != null)
				for(final File f : toDelete)
				{
					if(f.exists())
						f.delete();
				}
		}
	}

}
