package ca.triangulationdevice.android.model;

import android.content.Context;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

import ca.triangulationdevice.android.R;
import ca.triangulationdevice.android.model.Session;

public class SessionAdapter extends BaseAdapter {

    private final List<Session> paths;
    private final LayoutInflater inflater;

    public SessionAdapter(Context context, List<Session> paths) {
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
        return 0l;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.path_list_item, parent, false);
        }

        TextView pathNameView = (TextView) convertView.findViewById(R.id.path_name);
        TextView lengthView = (TextView) convertView.findViewById(R.id.path_length);
        TextView locationView = (TextView) convertView.findViewById(R.id.path_location);

        Session session = paths.get(position);
        pathNameView.setText(session.title);
        locationView.setText(session.location);
        lengthView.setText(session.duration());

        return convertView;
    }
}
