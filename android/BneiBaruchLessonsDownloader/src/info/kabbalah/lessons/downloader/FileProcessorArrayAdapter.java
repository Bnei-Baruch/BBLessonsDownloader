package info.kabbalah.lessons.downloader;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

class FileProcessorArrayAdapter extends ArrayAdapter<FileProcessor> {

	private final ArrayList<FileProcessor> items;
	private Map<TextView, FileInfo> view2file;
	
	public FileProcessorArrayAdapter(Context context, int textViewResourceId, ArrayList<FileProcessor> arrayList) {
		super(context, textViewResourceId, arrayList);
		
		this.items = arrayList;
		view2file = new HashMap<TextView, FileInfo>();
	}
	
	public View getView(int position, View convertView, ViewGroup parent) {
		View v = convertView;
       if (v == null) {
           LayoutInflater vi = (LayoutInflater)super.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
           v = vi.inflate(R.layout.file_li, null);
       }
       FileProcessor o = this.items.get(position);
       if (o != null) {
		   TextView tt = v.findViewById(R.id.fileLabel);
		   if (tt != null) {
			   tt.setText(o.getFileInfo().getName());
			   view2file.put(tt, o.getFileInfo());
		   }
       }
       return v;
	}

	public FileInfo getFileInfo(View v) {
		return view2file.get(v);
	}

	public boolean isEnabled(int position) {
		return true;
	}

// --Commented out by Inspection START (26/03/2015 14:01):
//	public ArrayList<FileProcessor> getList() {
//		return items;
//	}
// --Commented out by Inspection STOP (26/03/2015 14:01)

	public boolean hasStableIds()
	{
		return false;
	}

}
