package ca.triangulationdevice.android.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ca.triangulationdevice.android.storage.PathStorage;

public class MemoryUserManager {

    private User user;
    private Map<User, List<PathStorage.Session>> sessions = new HashMap<>();
    private Map<String, User> users = new HashMap<>();

    public void logIn(User user) {
        this.user = user;
        this.add(user);
    }

    public void logOut() {
        this.user = null;
    }

    public void add(User user) {
        this.users.put(user.id, user);
        this.sessions.put(user, new ArrayList<PathStorage.Session>());
    }

    public List<User> getUsers() {
        return new ArrayList<>(this.users.values());
    }

    public User getUser(String id) {
        return this.users.get(id);
    }

    public User getCurrentUser() {
        return this.user;
    }

    public void addSessionForUser(User user, PathStorage.Session session) {
        this.sessions.get(user).add(session);
    }

    public List<PathStorage.Session> getSessionsForUser(User user) {
        return sessions.get(user);
    }
}
