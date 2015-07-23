package ca.triangulationdevice.android.model;

//import org.hive2hive.core.api.H2HNode;
//import org.hive2hive.core.api.configs.FileConfiguration;
//import org.hive2hive.core.api.configs.NetworkConfiguration;
//import org.hive2hive.core.api.interfaces.IFileConfiguration;
//import org.hive2hive.core.api.interfaces.IH2HNode;
//import org.hive2hive.core.api.interfaces.INetworkConfiguration;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;

public class UserManager extends HashMap<String, User> {
    public UserManager() throws UnknownHostException {
//        INetworkConfiguration netConfig = NetworkConfiguration.create("first");
//        IFileConfiguration fileConfig = FileConfiguration.createDefault();
//
//        IH2HNode peerNode = H2HNode.createNode(fileConfig);
//        peerNode.connect(netConfig);
    }

    public void add(User user) {
        this.put(user.id, user);
    }
}
