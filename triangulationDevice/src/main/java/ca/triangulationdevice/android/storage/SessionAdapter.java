package ca.triangulationdevice.android.storage;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

import ca.triangulationdevice.android.R;

public class SessionAdapter extends BaseAdapter {

    private final List<PathStorage.Session> paths;
    private final LayoutInflater inflater;

    public SessionAdapter(Context context, List<PathStorage.Session> paths) {
        this.paths = paths;
        inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return paths.size();
    }

    @Override
    public Object getItem(int position) {
        return paths.get(position);
    }

    @Override
    public long getItemId(int position) {
        return paths.get(position).id;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.path_list_item, parent, false);
        }

        TextView pathNameView = (TextView) convertView.findViewById(R.id.path_name);
        TextView lengthView = (TextView) convertView.findViewById(R.id.path_length);
        TextView locationView = (TextView) convertView.findViewById(R.id.path_location);

        PathStorage.Session session = paths.get(position);
        pathNameView.setText(session.title);

        return convertView;
    }
}
