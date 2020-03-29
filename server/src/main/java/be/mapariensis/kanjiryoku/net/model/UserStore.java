package be.mapariensis.kanjiryoku.net.model;

import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import be.mapariensis.kanjiryoku.net.exceptions.UserManagementException;

public final class UserStore implements Iterable<User> {
    private final Map<SocketChannel, User> userConnMap = new ConcurrentHashMap<>();
    private final Map<String, User> userNameMap = new ConcurrentHashMap<>();
    private final Object LOCK = new Object();

    public void addUser(User u) throws UserManagementException {
        synchronized (LOCK) {
            User other;
            if ((other = userConnMap.get(u.channel)) != null)
                throw new UserManagementException(
                        String.format(
                                "Connection already bound to user \"%s\"",
                                other.handle));
            if (userNameMap.containsKey(u.handle))
                throw new UserManagementException("User name taken.");
            userConnMap.put(u.channel, u);
            userNameMap.put(u.handle, u);
        }
    }

    public void removeUser(User u) {
        if (u == null)
            return;
        synchronized (LOCK) {
            userConnMap.remove(u.channel);
            userNameMap.remove(u.handle);
        }
    }

    public User requireUser(String name) throws UserManagementException {
        User u = getUser(name);
        if (u == null)
            throw new UserManagementException(String.format("No user named %s",
                    name));
        return u;
    }

    public User getUser(String name) {
        return userNameMap.get(name);
    }

    public User getUser(SocketChannel peer) {
        return userConnMap.get(peer);
    }


    @Override
    public Iterator<User> iterator() {
        LinkedList<User> list;
        synchronized (LOCK) {
            list = new LinkedList<>(userNameMap.values());
        }
        return list.iterator();
    }

}
