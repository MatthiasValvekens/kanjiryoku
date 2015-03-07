package be.mapariensis.kanjiryoku.net.profiles;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractListModel;

import org.apache.commons.collections4.list.TreeList;
import org.json.JSONObject;

import be.mapariensis.kanjiryoku.net.exceptions.BadConfigurationException;
import be.mapariensis.kanjiryoku.util.IProperties;

public class ProfileSet extends AbstractListModel<String> {

	public static final class Profile {
		public final String host, username;
		public final int port;

		protected Profile(String host, int port, String username) {
			if (port <= 0)
				throw new IllegalArgumentException(
						"Port number should be positive.");
			this.host = host;
			this.username = username;
			this.port = port;
		}
	}

	private final Map<String, Profile> profiles = new HashMap<String, Profile>();
	private final List<String> profileNames = new TreeList<String>();

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
				profiles.put(key, new Profile(host, port, username));
			}
		}
		profileNames.addAll(profiles.keySet());
	}

	public void addProfile(String profileName, String host, int port,
			String username) {
		profiles.put(profileName, new Profile(host, port, username));
		profileNames.add(profileName);
		fireContentsChanged(this, 0, profileNames.size());
	}

	public void removeProfile(String profileName) {
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
			res.put(e.getKey(), profile);
		}
		return res;
	}

}
