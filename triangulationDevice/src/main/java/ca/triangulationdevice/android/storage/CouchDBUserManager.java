package ca.triangulationdevice.android.storage;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Location;
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
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import ca.triangulationdevice.android.model.Session;
import ca.triangulationdevice.android.model.User;

public class CouchDBUserManager {
    private static final String TAG = "CouchDBUserManager";
    private static final String DB_NAME = "triangulation_device";

    private Manager manager = null;
    ObjectMapper mapper = new ObjectMapper();

    private Database database = null;
    private View sessionsView;
    private View usersView;
    private User user;

    public CouchDBUserManager(Context context) {
        try {
            // Connect to the database.
            manager = new Manager(new AndroidContext(context), Manager.DEFAULT_OPTIONS);
            database = manager.getDatabase(DB_NAME);

            // Set up the views we want.
            usersView = database.getView("users");
            usersView.setMap(new Mapper() {
                @Override
                public void map(Map<String, Object> document, Emitter emitter) {
                    if (document.get("type").equals("user")) {
                        emitter.emit(document.get("_id"), document);
                    }
                }
            }, "1");

            sessionsView = database.getView("sessions");
            sessionsView.setMap(new Mapper() {
                @Override
                public void map(Map<String, Object> document, Emitter emitter) {
                    if (document.get("type").equals("session")) {
                        emitter.emit(document.get("_id"), document);
                    }
                }
            }, "1");

//            URL url = new URL("http://triangulationdevice.iriscouch.com/");
//            Replication push = database.createPushReplication(url);
//            Replication pull = database.createPullReplication(url);
//            pull.setContinuous(true);
//            push.setContinuous(true);
//            Authenticator auth = new BasicAuthenticator("chris@vandevel.de", "orZXJfQChlGnkTyCrl7J0W59ltHayuAxJgCETBLezdH0Yl8oJK6sv4DlCw7hA4IW");
//            push.setAuthenticator(auth);
//            pull.setAuthenticator(auth);
//
//            push.addChangeListener(new Replication.ChangeListener() {
//                @Override
//                public void changed(Replication.ChangeEvent event) {
//                    // will be called back when the push replication status changes
//                }
//            });
//            pull.addChangeListener(new Replication.ChangeListener() {
//                @Override
//                public void changed(Replication.ChangeEvent event) {
//                    // will be called back when the pull replication status changes
//                }
//            });
//            push.start();
//            pull.start();
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
        Document document = getDocumentForId(usersView, user.id);
        Map<String, Object> properties = mapper.convertValue(user, Map.class);
        document.putProperties(properties);
    }

    public List<User> getUsers() throws CouchbaseLiteException {
        Query userQuery = usersView.createQuery();

        List<User> users = new ArrayList<>(usersView.getTotalRows());
        for (QueryRow row : userQuery.run()) {
            users.add(loadUser(row));
        }

        return users;
    }

    private Document getDocumentForId(View view, String id) throws CouchbaseLiteException {
        if (id != null) {
            QueryRow row = getRowById(view, id);
            if (row != null) {
                return row.getDocument();
            } else {
                return database.getDocument(id);
            }
        } else {
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
            return iterator.next();
        } else {
            return null;
        }
    }

    public User getUser(String id) throws CouchbaseLiteException {
        return loadUser(getRowById(usersView, id));
    }

    public User getCurrentUser() {
        return this.user;
    }

    public void addSession(Session session) throws CouchbaseLiteException {
        Document document = getDocumentForId(sessionsView, session.id);
        Map<String, Object> properties = mapper.convertValue(session, Map.class);
        document.putProperties(properties);
    }

    public List<Session> getSessionsForUser(User user) throws CouchbaseLiteException {
        Query sessionsQuery = sessionsView.createQuery();

        List<Session> sessions = new ArrayList<>(usersView.getTotalRows());
        for (QueryRow row : sessionsQuery.run()) {
            Session session = loadSession(row);
            if (session.ownerId.equals(user.id)) {
                sessions.add(session);
            }
        }

        return sessions;
    }

    private Session loadSession(QueryRow row) {
        if (row == null) return null;

        Document userDoc = row.getDocument();
        Session session = mapper.convertValue(userDoc.getProperties(), Session.class);

        return session;
    }

    private User loadUser(QueryRow row) {
        if (row == null) return null;

        Document userDoc = row.getDocument();
        User user = new User(row.getDocumentId());

        user.name = (String) userDoc.getProperty("name");
        user.description = (String) userDoc.getProperty("description");
        user.location = (String) userDoc.getProperty("location");
        user.email = (String) userDoc.getProperty("email");
        user.online = Boolean.valueOf((String) userDoc.getProperty("online"));

        user.myLocation = new Location("saved");
        user.myLocation.setLongitude(Double.valueOf((String) userDoc.getProperty("longitude")));
        user.myLocation.setLatitude(Double.valueOf((String) userDoc.getProperty("latitude")));

        return user;
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
