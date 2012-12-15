package info.kabbalah.lessons.downloader;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

public class MusicUtils {
    private final static long [] sEmptyList = new long[0];

    public static long [] getSongListForCursor(Cursor cursor) {
        if (cursor == null) {
            return sEmptyList;
        }
        int len = cursor.getCount();
        long [] list = new long[len];
        cursor.moveToFirst();
        int colidx = -1;
        try {
            colidx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Playlists.Members.AUDIO_ID);
        } catch (IllegalArgumentException ex) {
            colidx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
        }
        for (int i = 0; i < len; i++) {
            list[i] = cursor.getLong(colidx);
            cursor.moveToNext();
        }
        return list;
    }

    public static long [] getSongListForArtist(Context context, long id) {
        final String[] ccols = new String[] { MediaStore.Audio.Media._ID };
        String where = MediaStore.Audio.Media.ARTIST_ID + "=" + id + " AND " + 
        MediaStore.Audio.Media.IS_MUSIC + "=1";
        Cursor cursor = query(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                ccols, where, null,
                MediaStore.Audio.Media.ALBUM_KEY + ","  + MediaStore.Audio.Media.TRACK);
        
        if (cursor != null) {
            long [] list = getSongListForCursor(cursor);
            cursor.close();
            return list;
        }
        return sEmptyList;
    }
	
    public static long [] getSongIdLikeName(Context context, String name) {
        final String[] ccols = new String[] { MediaStore.Audio.Media._ID };
        String where = MediaStore.Audio.Media.TITLE + " LIKE ? AND " + 
        MediaStore.Audio.Media.IS_MUSIC + "=1";
        Cursor cursor = query(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                ccols, where, new String[] {name},
                MediaStore.Audio.Media.ALBUM_KEY + ","  + MediaStore.Audio.Media.TRACK);
        
        if (cursor != null) {
            long [] list = getSongListForCursor(cursor);
            cursor.close();
            return list;
        }
        return sEmptyList;
    }
	
    private static ContentValues[] sContentValuesCache = null;

    /**
     * @param ids The source array containing all the ids to be added to the playlist
     * @param offset Where in the 'ids' array we start reading
     * @param len How many items to copy during this pass
     * @param base The play order offset to use for this pass
     */
    private static void makeInsertItems(long[] ids, int offset, int len, int base) {
        // adjust 'len' if would extend beyond the end of the source array
        if (offset + len > ids.length) {
            len = ids.length - offset;
        }
        // allocate the ContentValues array, or reallocate if it is the wrong size
        if (sContentValuesCache == null || sContentValuesCache.length != len) {
            sContentValuesCache = new ContentValues[len];
        }
        // fill in the ContentValues array with the right values for this pass
        for (int i = 0; i < len; i++) {
            if (sContentValuesCache[i] == null) {
                sContentValuesCache[i] = new ContentValues();
            }

            sContentValuesCache[i].put(MediaStore.Audio.Playlists.Members.PLAY_ORDER, base + offset + i);
            sContentValuesCache[i].put(MediaStore.Audio.Playlists.Members.AUDIO_ID, ids[offset + i]);
        }
    }

    public static void addToPlaylist(Context context, long [] ids, long playlistid) {
        if (ids == null) {
            // this shouldn't happen (the menuitems shouldn't be visible
            // unless the selected item represents something playable
            Log.e("MusicUtils", "ListSelection null");
        } else {
            int size = ids.length;
            ContentResolver resolver = context.getContentResolver();
            // need to determine the number of items currently in the playlist,
            // so the play_order field can be maintained.
            String[] cols = new String[] {
                    "count(*)"
            };
            Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external", playlistid);
            Cursor cur = resolver.query(uri, cols, null, null, null);
            cur.moveToFirst();
            int base = cur.getInt(0);
            cur.close();
//            int numinserted = 0;
            for (int i = 0; i < size; i += 1000) {
                makeInsertItems(ids, i, 1000, base);
//                numinserted += resolver.bulkInsert(uri, sContentValuesCache);
                resolver.bulkInsert(uri, sContentValuesCache);
            }
//            String message = context.getResources().getQuantityString(
//                    R.plurals.NNNtrackstoplaylist, numinserted, numinserted);
//            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
            //mLastPlaylistSelected = playlistid;
        }
    }

	public static void clearPlaylist(Context context, int plid) {
        Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external", plid);
        context.getContentResolver().delete(uri, null, null);
        return;
	}
	
	public static void cleanOldPlaylists(Context ctx, String folderName, int delta)
	{
		Calendar date = Calendar.getInstance();
		date.add(Calendar.DAY_OF_MONTH, -delta);
		Cursor c = query(ctx, MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
					new String[] {MediaStore.Audio.Playlists._ID, MediaStore.Audio.Playlists.NAME },
	                MediaStore.Audio.Playlists.NAME + " LIKE ? " /*+ " AND " + MediaStore.Audio.Playlists.DATE_MODIFIED + " < ? "*/,
	                new String[] { folderName + "_%"/*, Long.toString(date.getTime().getTime() / 1000)*/ },
	                MediaStore.Audio.Playlists.NAME);

		String listToDelete = "";
		if(c != null && c.moveToFirst())
		{
			boolean first = true;
			while(c.moveToNext())
			{
				String name = c.getString(1);
				int id = c.getInt(0);
				Log.d("cleanOldPlaylists", String.format("ID - %d, Name - %s", new Object[] {id, name}));
				String[] d = name.split("_")[1].split("-");
				GregorianCalendar pld = new GregorianCalendar(Integer.parseInt(d[0]), Integer.parseInt(d[1]) - 1, Integer.parseInt(d[2]));
				pld.setTime(new Date(Integer.parseInt(d[0]) - 1900, Integer.parseInt(d[1]) - 1, Integer.parseInt(d[2])));
				if(pld.before(date))
				{
					Log.d("cleanOldPlaylists", String.format("To delete - %s", new Object[] {name}));
					if(!first)
						listToDelete += ", ";
					else
						first = false;

					listToDelete += String.format("%d", id);
				}
			}
			c.close();
		}
		if(listToDelete != null && listToDelete.length() > 0)
		{
			ContentResolver resolver = ctx.getContentResolver();
			Integer num = resolver.delete(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
			        MediaStore.Audio.Playlists._ID + " IN (" + listToDelete + ")",
			        null);
			Log.d("cleanOldPlaylists", String.format("Removed %d playlists", new Object[] {num}));
		}
	}

    public static Cursor query(Context context, Uri uri, String[] projection,
            String selection, String[] selectionArgs, String sortOrder, int limit) {
        try {
            ContentResolver resolver = context.getContentResolver();
            if (resolver == null) {
                return null;
            }
            if (limit > 0) {
                uri = uri.buildUpon().appendQueryParameter("limit", "" + limit).build();
            }
            return resolver.query(uri, projection, selection, selectionArgs, sortOrder);
         } catch (UnsupportedOperationException ex) {
            return null;
        }
        
    }

    public static Cursor query(Context context, Uri uri, String[] projection,
            String selection, String[] selectionArgs, String sortOrder) {
        return query(context, uri, projection, selection, selectionArgs, sortOrder, 0);
    }

    public static boolean isMediaScannerScanning(Context context) {
        boolean result = false;
        Cursor cursor = query(context, MediaStore.getMediaScannerUri(), 
                new String [] { MediaStore.MEDIA_SCANNER_VOLUME }, null, null, null);
        if (cursor != null) {
            if (cursor.getCount() == 1) {
                cursor.moveToFirst();
                result = "external".equals(cursor.getString(0));
            }
            cursor.close(); 
        } 

        return result;
    }
}
