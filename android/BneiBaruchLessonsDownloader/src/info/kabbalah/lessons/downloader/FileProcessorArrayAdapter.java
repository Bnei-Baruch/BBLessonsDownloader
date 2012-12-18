package info.kabbalah.lessons.downloader;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class FileProcessorArrayAdapter extends ArrayAdapter<FileProcessor> {

	private ArrayList<FileProcessor> items;
	
	public FileProcessorArrayAdapter(Context context, int textViewResourceId, ArrayList<FileProcessor> arrayList) {
		super(context, textViewResourceId, arrayList);
		
		this.items = arrayList;
	}
	
	public View getView(int position, View convertView, ViewGroup parent) {
		View v = convertView;
       if (v == null) {
           LayoutInflater vi = (LayoutInflater)super.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
           v = vi.inflate(R.layout.file_li, null);
       }
       FileProcessor o = this.items.get(position);
       if (o != null) {
               TextView tt = (TextView) v.findViewById(R.id.fileLabel);
               if (tt != null) {
                     tt.setText(o.getFileInfo().getName());                            
               }
       }
       return v;
	}

	public boolean isEnabled(int position) {
		return true;
	}

	public ArrayList<FileProcessor> getList() {
		return items;
	}
	
	public boolean hasStableIds()
	{
		return false;
	}

}