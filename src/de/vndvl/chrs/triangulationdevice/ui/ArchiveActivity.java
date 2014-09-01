package de.vndvl.chrs.triangulationdevice.ui;

import java.text.DateFormat;
import java.util.List;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import de.vndvl.chrs.triangulationdevice.R;
import de.vndvl.chrs.triangulationdevice.storage.PathStorage;
import de.vndvl.chrs.triangulationdevice.storage.PathStorage.Session;
import de.vndvl.chrs.triangulationdevice.ui.partial.TriangulationListActivity;

/**
 * An {@link Activity} which shows an archive of path events to play back.
 */
public class ArchiveActivity extends TriangulationListActivity {
    private final PathStorage storage = new PathStorage(this);
    private List<Session> sessions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.archive);

        // Load our archives from storage.
        this.storage.open();
        this.sessions = this.storage.loadSessions();
        final SessionAdapter adapter = new SessionAdapter(this, this.sessions);
        setListAdapter(adapter);

        // Update the count view and register an observer.
        adapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                updateTitle(ArchiveActivity.this.sessions);
            }
        });
        updateTitle(this.sessions);

        // Set the up action.
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    private void updateTitle(List<Session> sessions) {
        if (sessions.size() > 0) {
            getActionBar().setTitle(getResources().getQuantityString(R.plurals.n_sessions, sessions.size()));
        } else {
            getActionBar().setTitle(getResources().getString(R.string.archive));
        }
    }

    @Override
    protected void onDestroy() {
        this.storage.close();
        super.onDestroy();
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Intent playerIntent = new Intent(this, PlayerActivity.class);
        playerIntent.setAction(Intent.ACTION_VIEW);
        playerIntent.putExtra(PlayerActivity.SESSION_EXTRA, position);
        startActivity(playerIntent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean displayed = super.onCreateOptionsMenu(menu);
        menu.findItem(R.id.menu_archive).setVisible(false);
        return displayed;
    }

    public class SessionAdapter extends ArrayAdapter<Session> {
        private final DateFormat timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT);

        public SessionAdapter(Context context, List<Session> users) {
            super(context, R.layout.archive_row, users);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Session session = getItem(position);

            // Check if an existing view is being reused, otherwise inflate the
            // view
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.archive_row, parent, false);
            }

            // Lookup view for data population
            TextView titleView = (TextView) convertView.findViewById(R.id.title);
            TextView timeView = (TextView) convertView.findViewById(R.id.absolute_time);
            TextView timeDiffView = (TextView) convertView.findViewById(R.id.relative_time);

            // Populate the data into the template view using the data object
            if (session.title != null && !session.title.equals("")) {
                titleView.setText(session.title);
            } else {
                titleView.setText(getResources().getString(R.string.no_title));
            }
            timeView.setText(this.timeFormat.format(session.saved));
            timeDiffView.setText(DateUtils.getRelativeTimeSpanString(session.saved.getTime()));

            // Return the completed view to render on screen
            return convertView;
        }
    }
}
