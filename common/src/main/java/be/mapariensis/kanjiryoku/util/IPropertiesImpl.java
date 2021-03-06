package be.mapariensis.kanjiryoku.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.AbstractList;
import java.util.Set;

import org.json.*;

import be.mapariensis.kanjiryoku.net.exceptions.BadConfigurationException;

/**
 * Provides an additional layer of abstraction between the JSON library and the
 * application.
 * 
 * @author Matthias Valvekens
 * @version 1.1
 */
public class IPropertiesImpl implements IProperties {
    private final Object LOCK = new Object();

    // don't implement Map, this class doesn't deal with put/remove/etc.
    private static class JSONArrayWrapper extends AbstractList<Object> {
        final JSONArray arr;

        JSONArrayWrapper(JSONArray arr) {
            this.arr = arr;
        }

        @Override
        public Object get(int arg0) {
            Object o = arr.get(arg0);
            if (o instanceof JSONArray)
                return new JSONArrayWrapper((JSONArray) o);
            else if (o instanceof JSONObject)
                return new IPropertiesImpl((JSONObject) o);
            else
                return o;
        }

        @Override
        public int size() {
            return arr.length();
        }

    }

    private JSONObject json;

    private static JSONObject parse(String json)
            throws BadConfigurationException {
        try {
            return new JSONObject(json);
        } catch (JSONException ex) {
            throw new BadConfigurationException(
                    "Failed to parse configuration", ex);
        }
    }

    public void swapBackend(String json) throws BadConfigurationException {
        synchronized (LOCK) {
            this.json = parse(json);
        }
    }

    public void swapBackend(JSONObject json) {
        synchronized (LOCK) {
            this.json = json;
        }
    }

    /**
     * Construct a IPropertiesImpl object from the given JSON string.
     * 
     * @param json
     *            A valid JSON string.
     */
    public IPropertiesImpl(String json) throws BadConfigurationException {
        this(parse(json));
    }

    /**
     * Construct a IPropertiesImpl object directly from a JSON object.
     * 
     * @param json
     */
    public IPropertiesImpl(JSONObject json) {
        if (json == null)
            throw new NullPointerException(
                    "Can't initialize the JSON backend as null.");
        this.json = json;
    }

    /**
     * Fetch a property by name, or return <code>null</code> if not found.
     * JSONObject and JSONArray objects are silently converted to
     * IPropertiesImpl and List objects, respectively.
     * 
     * @param key
     *            The property name.
     */
    @Override
    public Object get(String key) {
        return get(key, null);
    }

    @Override
    public Object get(String key, Object defaultVal) {
        try {
            Object thing;
            synchronized (LOCK) {
                thing = json.get(key);
            }
            if (thing instanceof JSONObject)
                return new IPropertiesImpl((JSONObject) thing);
            else if (thing instanceof JSONArray)
                return new JSONArrayWrapper((JSONArray) thing);
            else
                return thing;
        } catch (JSONException e) {
            return defaultVal;
        }
    }

    /**
     * Verify whether the property with the given name is defined.
     * 
     * @param key
     *            A property name.
     */
    @Override
    public boolean containsKey(String key) {
        synchronized (LOCK) {
            return json.has(key);
        }
    }

    /**
     * Return the key set associated with this IPropertiesImpl object.
     */
    @Override
    @SuppressWarnings("unchecked")
    public Set<String> keySet() {
        synchronized (LOCK) {
            return json.keySet();
        }
    }

    /**
     * Fetch a mandatory parameter by name and type.
     * 
     * @param key
     *            The parameter name.
     * @param type
     *            The parameter's type.
     * @throws BadConfigurationException
     *             Thrown if the key in question was not found, or if its
     *             corresponding value does not match the given type.
     */
    @Override
    public <T> T getRequired(String key, Class<T> type)
            throws BadConfigurationException {
        Object item;
        synchronized (LOCK) {
            item = this.get(key);
        }
        if (item == null)
            throw new BadConfigurationException(
                    "Missing mandatory parameter \"" + key + "\".");
        if (!isInstance(type, item))
            throw new BadConfigurationException("Parameter " + key
                    + " is of type " + item.getClass() + ", but expected "
                    + type);
        return cast(type, item);
    }

    /**
     * @see getRequired(String, Class<T>)
     * @note This method simply returns <code>null</code> when a parameter is
     *       not found, instead of throwing an exception.
     */
    @Override
    public <T> T getTyped(String key, Class<T> type)
            throws BadConfigurationException {
        return getTyped(key, type, null);
    }

    @Override
    public <T> T getTyped(String key, Class<T> type, T defaultVal)
            throws BadConfigurationException {
        Object item;
        synchronized (LOCK) {
            item = this.get(key);
        }
        if (item == null)
            return defaultVal;
        if (!isInstance(type, item))
            throw new BadConfigurationException("Parameter " + key
                    + " is of type " + item.getClass() + ", but expected "
                    + type);
        return cast(type, item);
    }

    @Override
    public <T> T getSafely(String key, Class<T> type, T defaultVal) {
        Object item;
        synchronized (LOCK) {
            item = this.get(key);
        }
        if (item == null || !isInstance(type, item))
            return defaultVal;
        return cast(type, item);
    }

    @Override
    public String toString() {
        return json.toString();
    }

    private static boolean isInstance(Class<?> type, Object o) {
        if (type.isInstance(o))
            return true;
        if (type == Double.class && (o instanceof Number))
            return true;
        if (type == Long.class && (o instanceof Integer))
            return true;
        return false;
    }

    private static <T> T cast(Class<T> type, Object o) {
        if (type.isInstance(o))
            return type.cast(o);
        if (type == Double.class && (o instanceof Number))
            return type.cast(((Number) o).doubleValue());
        if (type == Long.class && (o instanceof Integer))
            return type.cast(((Integer) o).longValue());
        else
            throw new ClassCastException();
    }

    @Override
    public long getTimeMillis(String key, long defaultVal)
            throws BadConfigurationException {
        Object item;
        synchronized (LOCK) {
            item = this.get(key);
        }
        if (item == null)
            return defaultVal;
        return parseTime(item);
    }

    public static long parseTime(Object o) throws BadConfigurationException {
        if (o instanceof Long)
            return ((Long) o).longValue();
        if (o instanceof Integer)
            return ((Integer) o).longValue();
        if (o instanceof String) {
            try {
                return (new SimpleDateFormat("yyyy-MM-dd")).parse((String) o)
                        .getTime();
            } catch (ParseException e) {
                throw new BadConfigurationException("Could not parse time.", e);
            }
        } else
            throw new BadConfigurationException("Could not parse time.");
    }

    @Override
    public long getTimeMillis(String key) throws BadConfigurationException {
        Object item;
        synchronized (LOCK) {
            item = this.get(key);
        }
        if (item == null)
            throw new BadConfigurationException(
                    "Missing mandatory parameter \"" + key + "\".");
        return parseTime(item);
    }

}
