package ca.triangulationdevice.android.model;

import java.util.HashMap;

public class UserManager extends HashMap<String, User> {
    public void add(User user) {
        this.put(user.id, user);
    }
}
