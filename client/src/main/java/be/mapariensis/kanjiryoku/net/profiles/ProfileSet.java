package be.mapariensis.kanjiryoku.net.profiles;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractListModel;

import org.apache.commons.collections4.list.TreeList;
import org.json.JSONObject;

import be.mapariensis.kanjiryoku.config.ConfigManager;
import be.mapariensis.kanjiryoku.net.exceptions.BadConfigurationException;
import be.mapariensis.kanjiryoku.util.IProperties;

public class ProfileSet extends AbstractListModel<String> {

    public static final class Profile {
        public final String host, username;
        public final int port;
        public final boolean useSsl;

        protected Profile(String host, int port, String username, boolean useSsl) {
            if (port <= 0)
                throw new IllegalArgumentException(
                        "Port number should be positive.");
            this.host = host;
            this.username = username;
            this.port = port;
            this.useSsl = useSsl;
        }
    }

    private final Map<String, Profile> profiles = new HashMap<>();
    private final List<String> profileNames = new TreeList<>();

    public ProfileSet() {

    }

    public ProfileSet(IProperties p) throws BadConfigurationException {
        for (String key : p.keySet()) {
            Object thing = p.get(key);
            if (thing instanceof IProperties) {
                IProperties pprop = (IProperties) thing;
                String host = pprop.getRequired("host", String.class);
                int port = pprop.getRequired("port", Integer.class);
                String username = pprop.getRequired("username", String.class);
                boolean useSsl = pprop.getTyped("useSsl", Boolean.class,
                        ConfigManager.SSL_DEFAULT);
                profiles.put(key, new Profile(host, port, username, useSsl));
            }
        }
        profileNames.addAll(profiles.keySet());
    }

    public void replaceProfile(String oldProfile, String newProfileName,
            String host, int port, String username, boolean useSsl) {
        profiles.remove(oldProfile);
        profileNames.remove(oldProfile);
        putProfile(newProfileName, host, port, username, useSsl);
    }

    public void putProfile(String newProfileName, String host, int port,
            String username, boolean useSsl) {
        if (!profiles.containsKey(newProfileName))
            profileNames.add(newProfileName);
        profiles.put(newProfileName, new Profile(host, port, username, useSsl));
        fireContentsChanged(this, 0, profileNames.size());
    }

    public void removeProfile(String profileName) {
        if (profileName == null)
            return;
        profiles.remove(profileName);
        profileNames.remove(profileName);
        fireContentsChanged(this, 0, profileNames.size());
    }

    @Override
    public int getSize() {
        return profileNames.size();
    }

    @Override
    public String getElementAt(int index) {
        return profileNames.get(index);
    }

    public Profile get(String profileName) {
        return profiles.get(profileName);
    }

    public JSONObject toJSON() {
        JSONObject res = new JSONObject();
        for (Map.Entry<String, Profile> e : profiles.entrySet()) {
            JSONObject profile = new JSONObject();
            profile.put("host", e.getValue().host);
            profile.put("port", e.getValue().port);
            profile.put("username", e.getValue().username);
            profile.put("useSsl", e.getValue().useSsl);
            res.put(e.getKey(), profile);
        }
        return res;
    }

}
