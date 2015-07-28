package ca.triangulationdevice.android.model;

import android.content.Context;
import android.util.Log;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ca.triangulationdevice.android.storage.PathStorage;

public class MemoryUserManager implements UserManager {

    private User user;
    private Map<User, List<PathStorage.Session>> paths = new HashMap<>();
    private Map<String, User> users = new HashMap<>();

    public MemoryUserManager(Context context) throws UnknownHostException {

    }

    public void add(User user) {
        this.users.put(user.id, user);
    }

    public List<User> getUsers() {
        return new ArrayList<>(this.users.values());
    }

    public User getUser(String id) {
        return this.users.get(id);
    }

    public void setCurrentUser(User user) {
        this.user = user;
    }

    public User getCurrentUser(User user) {
        return this.user;
    }

    @Override
    public void addPathForUser(User user, PathStorage.Session session, PathStorage.Path path) {

    }

    @Override
    public void addSessionForUser(User user, PathStorage.Session session) {

    }

    public void addPathForUser(User user, PathStorage.Path path) {
        if (!this.paths.containsKey(user)) {
            this.paths.put(user, new ArrayList<PathStorage.Session>());
        }

//        this.paths.get(user).add(path);
    }

    public List<PathStorage.Session> getSessionsForUser(User user) {
        return paths.get(user);
    }
}
