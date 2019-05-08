package info.kabbalah.lessons.downloader;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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

import javax.net.ssl.HttpsURLConnection;

//import org.apache.http.util.ByteArrayBuffer;

class DropBoxFileList {
    // TODO: set https://archive.kbb1.com/backend/content_units?page_no=1&page_size=10&content_type=LESSON_PART&language=en
    //  private final static String uri = "https://www.kabbalahmedia.info/morning_lesson/%2$s";
//    private final static String uri = "https://kabbalahmedia.info/backend/content_units?page_no=1&page_size=10&content_type=LESSON_PART&language=%2$s&start=%1$s&end=%1$s";
    private final static String uri = "https://kabbalahmedia.info/feeds/morning_lesson?DLANG=%3$s&DF=%2$s&DAYS=%1$d";
//    private final static String uri = "http://mylibrary.kbb1.com/api/morning_lessons.json?lang=%2$s";
	//final static String uri = "http://dl.dropbox.com/u/3074981/%s.txt";
	private static List<FileInfo> fileList = null;
	private static MediaDownloaderService caller;

    public static void startDownLoadFileList(int offset, String fileFormat, MediaDownloaderService mediaDownloaderService) {

		caller = mediaDownloaderService;
        startDownloadFileList(offset, fileFormat);
	}
	
	private synchronized static void pushFileList(String fileListJson, String dateFilter) {
        try {
            //caller.pushFileList(parseFileListJson(fileListJson, dateFilter));
            caller.pushFileList(parseFileListJsonFromArchive(fileListJson, dateFilter));
		} catch (Exception e) {
            Log.d("DropBoxFLDownloader", e.toString());
		}
	}

    private static void startDownloadFileList(int offset, String fileFormat) {
		Calendar today = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tel_Aviv"));
		today.add(Calendar.DAY_OF_MONTH, offset);

		String title = String
				.format(Locale.US, "%04d-%02d-%02d", today.get(Calendar.YEAR),
						today.get(Calendar.MONTH) + 1,
						today.get(Calendar.DAY_OF_MONTH));
        String sUrl = String.format(uri, offset == -1 ? 1 : 0, fileFormat, caller.data.selectedLanguage.toUpperCase(Locale.US));
        URL url;
        try {
            url = new URL(sUrl);
			final File storagePublicDirectory = new File(FileSystemUtilities.getLocalPath());
			if( ! storagePublicDirectory.exists())
				storagePublicDirectory.mkdir();
			File localFileList = new File( storagePublicDirectory.getAbsolutePath() + File.separatorChar + title + caller.data.selectedLanguage + ".txt");
			startGetUrlTextContent(url, localFileList, title);
		} catch (MalformedURLException e) {
            Log.d("startDownloadFileList", "The URL is wrong", e);
		}
	}

	private static void startGetUrlTextContent(final URL url, final File localFileList, final String dateFilter) {
		
		class GetFileListAT extends AsyncTask<URL, Void, String> {
			@Override
			protected String doInBackground(URL... params) {
				/* Open a connection to that URL. */
				try {
                    HttpsURLConnection ucon = (HttpsURLConnection) MediaDownloaderService.getConnectionWithProxy(url);
					try {
						if(localFileList.exists() && localFileList.canRead())
						{
							ucon.setIfModifiedSince(localFileList.lastModified());
						}
						
						ucon.setChunkedStreamingMode(4096);
                        ucon.setRequestProperty("Accept", "application/json");

						/*
						 * Define InputStreams to read from the URLConnection.
						 */
				
						InputStream is = ucon.getInputStream();
						if(is == null && ! localFileList.exists())
						{
							return null;
						}
						long lastModified = ucon.getLastModified();
                        byte[] content;
                        if (lastModified == 0 || lastModified > localFileList.lastModified())
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
            byte buf[] = new byte[1024];
            byte result[] = null;
            int current;
            int total = 0;
            int pos;
            while ((current = bis.read(buf)) != -1) {
                pos = total;
                total += current;
                byte[] save = result;
                result = new byte[total];
                if (save != null)
                    System.arraycopy(save, 0, result, 0, save.length);
                System.arraycopy(buf, 0, result, pos, current);
            }

            return result;
        } finally {
            bis.close();
            is.close();
		}
	}

    private static List<FileInfo> parseFileListJsonFromArchive(String fileListJson, String dateFilter) throws JSONException {
        fileList = new ArrayList<FileInfo>();
        JSONArray jlist = new JSONArray(fileListJson);

        for (int i = 0; i < jlist.length(); i++) {
            JSONObject cunit = jlist.getJSONObject(i);
            if (cunit.has("Files")) {
                JSONArray flist = cunit.getJSONArray("Files");
                for (int j = 0; j < flist.length(); ++j) {
                    fileList.add(new FileInfo(dateFilter, flist.getJSONObject(j)));
                }
            }
        }
        return fileList;
    }

    private static List<FileInfo> parseFileListJson(String fileListJson, String dateFilter) throws JSONException {
        JSONObject json = new JSONObject(fileListJson);
        fileList = new ArrayList<FileInfo>();
            try {
                JSONObject jlist = json.getJSONObject("morning_lessons");
                for (int i = 0; i < jlist.length(); i++) {
                    if (jlist.has("dates")) {
                        JSONArray jdates = jlist.getJSONArray("dates");
                        for (int j = 0; j < jdates.length(); j++) {
                            JSONObject jdate = jdates.getJSONObject(j);
                            String date = jdate.getString("date");
                            if (!date.equals(dateFilter)) continue;
                            JSONArray jfiles = jdate.getJSONArray("files");
                            for (int k = 0; k < jfiles.length(); k++) {
                                fileList.add(new FileInfo(date, jfiles.getJSONObject(k)));
                            }
                        }
                    }
                }
            } catch (JSONException je) {
                Log.d("DropBoxFileList", "Cannot parse file list" + je.getMessage(), je);
            }
        return fileList;
    }

    public static void deleteFileListsOlderThanNDays(int nRemoveFiles) {
        final Calendar date = Calendar.getInstance();
        final Pattern datepattern = Pattern.compile("\\d{4}\\-\\d{2}\\-\\d{2}.{3}.txt");
        date.add(Calendar.DAY_OF_YEAR, -nRemoveFiles);

        final File dir = new File(FileSystemUtilities.getLocalPath());

        if (dir != null) {
            dir.mkdirs();

            final File[] toDelete = dir.listFiles(new FilenameFilter() {

                public boolean accept(File dir, String filename) {
                    if (datepattern.matcher(filename).matches()) {
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
                        } catch (Exception e) {
                            return false;
                        }
                    } else
                        return false;
                }
            });
            if (toDelete != null)
                for (final File f : toDelete) {
                    if (f.exists())
                        f.delete();
                }
        }
    }

}
