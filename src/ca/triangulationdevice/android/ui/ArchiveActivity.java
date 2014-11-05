package ca.triangulationdevice.android.ui;

import java.text.DateFormat;
import java.util.List;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import ca.triangulationdevice.android.R;
import ca.triangulationdevice.android.storage.PathStorage;
import ca.triangulationdevice.android.storage.PathStorage.DeleteOneSessionTask;
import ca.triangulationdevice.android.storage.PathStorage.SendToServerTask;
import ca.triangulationdevice.android.storage.PathStorage.Session;
import ca.triangulationdevice.android.ui.partial.TriangulationListActivity;

/**
 * An {@link Activity} which shows an archive of path events to play back.
 */
public class ArchiveActivity extends TriangulationListActivity {
    private final PathStorage storage = new PathStorage(this);
    private List<Session> sessions;

    // Tasks and progress dialogs.
    private ProgressDialog sendingDialog;
    protected ProgressDialog deletingDialog;
    private SendToServerTask sendToServerTask;
    protected DeleteOneSessionTask deletingTask;

    DialogInterface.OnClickListener sendConfirmListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                ArchiveActivity.this.sendingDialog = ProgressDialog.show(ArchiveActivity.this, "Sending to server...", "", true);
                ArchiveActivity.this.sendToServerTask.execute();
                ArchiveActivity.this.sendToServerTask = ArchiveActivity.this.storage.new SendToServerTask() {
                    @Override
                    protected void onPostExecute(Void result) {
                        ArchiveActivity.this.sendingDialog.dismiss();
                    }
                };
                break;

            case DialogInterface.BUTTON_NEGATIVE:
                // No button clicked. Do nothing.
                break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.archive);

        // Allow long-clicks to open a context menu in our ListView.
        registerForContextMenu(this.getListView());

        this.sendToServerTask = this.storage.new SendToServerTask() {
            @Override
            protected void onPostExecute(Void result) {
                ArchiveActivity.this.sendingDialog.dismiss();
            }
        };

        this.deletingTask = this.storage.new DeleteOneSessionTask() {
            @Override
            protected void onPostExecute(Void result) {
                ArchiveActivity.this.deletingDialog.dismiss();
            }
        };

        // Load our archives from storage.
        this.storage.open();
        this.storage.new LoadSessionsTask() {
            @Override
            protected void onPostExecute(List<Session> result) {
                ArchiveActivity.this.sessions = result;
                final SessionAdapter adapter = new SessionAdapter(ArchiveActivity.this, ArchiveActivity.this.sessions);
                ArchiveActivity.this.setListAdapter(adapter);

                // Update the count view and register an observer.
                adapter.registerDataSetObserver(new DataSetObserver() {
                    @Override
                    public void onChanged() {
                        updateTitle(ArchiveActivity.this.sessions);
                    }
                });
                updateTitle(ArchiveActivity.this.sessions);
            }
        }.execute();

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

    // Not calling this until we implement a playback system.
    // @Override
    // protected void onListItemClick(ListView l, View v, int position, long id)
    // {
    // Intent playerIntent = new Intent(this, PlayerActivity.class);
    // playerIntent.setAction(Intent.ACTION_VIEW);
    // playerIntent.putExtra(PlayerActivity.SESSION_EXTRA, position);
    // startActivity(playerIntent);
    // }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean displayed = super.onCreateOptionsMenu(menu);
        menu.findItem(R.id.menu_archive).setVisible(false);
        MenuItem sendSessionsItem = menu.add(R.string.send_sessions);
        sendSessionsItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                AlertDialog.Builder builder = new AlertDialog.Builder(ArchiveActivity.this);
                builder.setMessage(R.string.send_confirm_message)
                        .setPositiveButton(R.string.yes, ArchiveActivity.this.sendConfirmListener)
                        .setNegativeButton(R.string.no, ArchiveActivity.this.sendConfirmListener)
                        .show();
                return true;
            }
        });
        return displayed;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        // Get the info on which item was selected
        final AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        final ArrayAdapter<Session> adapter = (ArrayAdapter<Session>) getListAdapter();

        // Retrieve the item that was clicked on
        final Session session = adapter.getItem(info.position);

        if (v.getId() == android.R.id.list) {
            menu.setHeaderTitle(R.string.session);
            String[] menuItems = getResources().getStringArray(R.array.archive_session_menu);
            for (int i = 0; i < menuItems.length; i++) {
                MenuItem newItem = menu.add(Menu.NONE, i, i, menuItems[i]);
                newItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                        case 0:
                            ArchiveActivity.this.deletingDialog = ProgressDialog.show(ArchiveActivity.this, "Deleting session \"" + session.title + "\"...", "", true);
                            ArchiveActivity.this.deletingTask.execute((long) session.id);
                            ArchiveActivity.this.sessions.remove(info.position);
                            adapter.notifyDataSetChanged();
                            ArchiveActivity.this.deletingTask = ArchiveActivity.this.storage.new DeleteOneSessionTask() {
                                @Override
                                protected void onPostExecute(Void result) {
                                    ArchiveActivity.this.deletingDialog.dismiss();
                                }
                            };
                        }
                        return false;
                    }
                });
            }
        }
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
