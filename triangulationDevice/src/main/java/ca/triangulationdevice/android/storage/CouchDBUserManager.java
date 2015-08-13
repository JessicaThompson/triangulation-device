package ca.triangulationdevice.android.storage;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.Document;
import com.couchbase.lite.Emitter;
import com.couchbase.lite.Manager;
import com.couchbase.lite.Mapper;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryRow;
import com.couchbase.lite.UnsavedRevision;
import com.couchbase.lite.View;
import com.couchbase.lite.android.AndroidContext;
import com.couchbase.lite.auth.Authenticator;
import com.couchbase.lite.auth.BasicAuthenticator;
import com.couchbase.lite.replicator.Replication;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import ca.triangulationdevice.android.model.CouchObject;
import ca.triangulationdevice.android.model.Session;
import ca.triangulationdevice.android.model.User;

public class CouchDBUserManager {
    private static final String TAG = "CouchDBUserManager";
    private static final String DB_NAME = "triangulation_device";

    ObjectMapper mapper;

    private Database database = null;
    private View sessionsView;
    private View usersView;
    private User user;

    public CouchDBUserManager(Context context) {
        mapper = new ObjectMapper();

        try {
            // Connect to the database.
            Manager manager = new Manager(new AndroidContext(context), Manager.DEFAULT_OPTIONS);
            database = manager.getDatabase(DB_NAME);

            if (false) {
                database.delete();
                database = manager.getDatabase(DB_NAME);
            }

            // Set up the views we want.
            usersView = database.getView("users");
            usersView.setMap(new Mapper() {
                @Override
                public void map(Map<String, Object> document, Emitter emitter) {
                if (document.containsKey("type") && document.get("type").equals(User.class.getSimpleName())) {
                    emitter.emit(document.get("_id"), document);
                }
                }
            }, "1");

            sessionsView = database.getView("sessions");
            sessionsView.setMap(new Mapper() {
                @Override
                public void map(Map<String, Object> document, Emitter emitter) {
                    if (document.containsKey("type") && document.get("type").equals(Session.class.getSimpleName())) {
                        emitter.emit(document.get("_id"), document);
                    }
                }
            }, "1");

            URL url = new URL("http://triangulationdevice.iriscouch.com/" + DB_NAME);
            Replication push = database.createPushReplication(url);
            Replication pull = database.createPullReplication(url);
            pull.setContinuous(true);
            push.setContinuous(true);
            Authenticator auth = new BasicAuthenticator("androidapp", "testing123");
            push.setAuthenticator(auth);
            pull.setAuthenticator(auth);

            push.addChangeListener(new Replication.ChangeListener() {
                @Override
                public void changed(Replication.ChangeEvent event) {
                    // will be called back when the push replication status changes
                }
            });
            pull.addChangeListener(new Replication.ChangeListener() {
                @Override
                public void changed(Replication.ChangeEvent event) {
                    // will be called back when the pull replication status changes
                }
            });
            push.start();
            pull.start();
        } catch (Exception e) {
            Log.e(TAG, "Error getting database", e);
        }
    }

    public boolean logIn(String installation) throws CouchbaseLiteException {
        this.user = getUser(installation);
        return this.user != null;
    }

    public void logOut() {
        this.user = null;
    }

    public void add(User user) throws CouchbaseLiteException {
        add(user, usersView);
    }

    public void add(Session session) throws CouchbaseLiteException {
        add(session, sessionsView);
    }

    public User getCurrentUser() {
        return this.user;
    }

    public List<User> getUsers() throws CouchbaseLiteException {
        Query userQuery = usersView.createQuery();

        List<User> users = new ArrayList<>(usersView.getTotalRows());
        for (QueryRow row : userQuery.run()) {
            users.add(load(row, User.class));
        }

        return users;
    }

    public User getUser(String id) throws CouchbaseLiteException {
        Log.i(TAG, "Trying to get user " + id);
        Log.i(TAG, "All users: " + getUsers().toString());
        return load(getRowById(usersView, id), User.class);
    }

    public List<Session> getSessionsForUser(User user) throws CouchbaseLiteException {
        Query sessionsQuery = sessionsView.createQuery();

        List<Session> sessions = new ArrayList<>(usersView.getTotalRows());
        for (QueryRow row : sessionsQuery.run()) {
            Session session = load(row, Session.class);
            if (session.ownerId.equals(user.id)) {
                sessions.add(session);
            }
        }

        return sessions;
    }

    private <T extends CouchObject> void add(final T object, View view) throws CouchbaseLiteException {
        Log.d(TAG, "Adding " + object);

        Document document = getDocumentForId(view, object.id);
        if (document == null) {
            Log.e(TAG, "Couldn't get a document for ID " + object.id);
        } else {
            Log.d(TAG, "Got document " + object.id);
            final Map<String, Object> properties = mapper.convertValue(object, Map.class);
            properties.put("type", object.getClass().getSimpleName());
            document.update(new Document.DocumentUpdater() {
                @Override
                public boolean update(UnsavedRevision newRevision) {
                    newRevision.setUserProperties(properties);
                    Log.d(TAG, "Finished updating properties for " + newRevision.getDocument().getId());
                    return true;
                }
            });
        }
    }

    private Document getDocumentForId(View view, String id) throws CouchbaseLiteException {
        if (id != null) {
            QueryRow row = getRowById(view, id);
            if (row != null) {
                Log.d(TAG, "Found an existing document for row " + id);
                return row.getDocument();
            } else {
                Log.d(TAG, "Making a new document for row " + id);
                return database.getDocument(id);
            }
        } else {
            Log.d(TAG, "Making a new document.");
            return database.createDocument();
        }
    }

    private QueryRow getRowById(View view, String id) throws CouchbaseLiteException {
        List<Object> keys = Collections.<Object>singletonList(id);
        Query userQuery = view.createQuery();
        userQuery.setKeys(keys);
        userQuery.setLimit(1);

        Iterator<QueryRow> iterator = userQuery.run().iterator();
        if (iterator.hasNext()) {
            QueryRow next = iterator.next();
            Log.d(TAG, "Has a row: " + next);
            return next;
        } else {
            Log.d(TAG, "No row found for ID: " + id);
            return null;
        }
    }

    private <T extends CouchObject> T load(QueryRow row, Class<T> clazz) {
        if (row == null) return null;

        Log.d(TAG, row.getDocumentId() + " " + row.getDocument().getUserProperties().toString());
        Document doc = row.getDocument();
        T object = mapper.convertValue(doc.getProperties(), clazz);
        object.id = doc.getId();

        return object;
    }

    private void attachDrawable(Drawable drawable, Document doc) {
        try {
            Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
            int size = bitmap.getHeight() * bitmap.getRowBytes();
            ByteBuffer buffer = ByteBuffer.allocate(size);
            bitmap.copyPixelsToBuffer(buffer);
            InputStream stream = new ByteArrayInputStream(buffer.array());

            UnsavedRevision newRev = doc.getCurrentRevision().createRevision();
            newRev.setAttachment("photo.jpg", "image/jpeg", stream);
            newRev.save();
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }
    }
}
