package ca.triangulationdevice.android.model;

import java.util.List;

import ca.triangulationdevice.android.storage.PathStorage;

public interface UserManager {

    public void add(User user);

    public List<User> getUsers();

    public User getUser(String id);

    public void setCurrentUser(User user);

    public User getCurrentUser(User user);

    public void addPathForUser(User user, PathStorage.Session session, PathStorage.Path path);

    public void addSessionForUser(User user, PathStorage.Session session);

    public List<PathStorage.Session> getSessionsForUser(User user);
}
