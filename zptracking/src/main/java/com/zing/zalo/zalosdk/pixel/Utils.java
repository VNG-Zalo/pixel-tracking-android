package com.zing.zalo.zalosdk.pixel;

import android.os.Bundle;

import com.zing.zalo.zalosdk.core.log.Log;
import com.zing.zalo.zalosdk.pixel.model.Event;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.zing.zalo.zalosdk.pixel.ZPConstants.LOG_TAG;

public class Utils {
    public static JSONArray toJSON(List<Event> events) {
        JSONArray array = new JSONArray();

        if(events == null) return array;
        for(Event event : events) {
            try {
                JSONObject obj = new JSONObject();
                obj.put("event", event.getName());
                obj.put("ts", event.getTimestamp());
                obj.put("params", event.getParams());
                array.put(obj);
            } catch(JSONException ex) {
                Log.w(LOG_TAG, "toJSON", ex);
            }

        }

        return array;
    }

    public static JSONObject toJSON(Map<String, Object> map, String keyPrefix) {
        JSONObject obj = new JSONObject();

        if(map == null) return obj;
        for(String key : map.keySet()) {
            Object val = map.get(key);
            try {
                if(keyPrefix == null) {
                    obj.put(key, wrap(val));
                } else {
                    obj.put(keyPrefix + key, wrap(val));
                }
            } catch (JSONException ex) {
                Log.w(LOG_TAG, "toJSON", ex);
            }
        }

        return obj;
    }

    public static JSONObject toJSON(Bundle bundle, String keyPrefix) {
        JSONObject obj = new JSONObject();

        if(bundle == null) return obj;
        for(String key : bundle.keySet()) {
            Object val = bundle.get(key);
            try {
                if(keyPrefix == null) {
                    obj.put(key, wrap(val));
                } else {
                    obj.put(keyPrefix + key, wrap(val));
                }
            } catch (JSONException ex) {
                Log.w(LOG_TAG, "toJSON", ex);
            }
        }

        return obj;
    }

    public static JSONObject toJSON(Bundle bundle) {
        return toJSON(bundle, null);
    }

    private static Object wrap(Object o) {
        if (o == null) {
            return JSONObject.NULL;
        }
        if (o instanceof JSONArray || o instanceof JSONObject) {
            return o;
        }
        if (o.equals(JSONObject.NULL)) {
            return o;
        }
        try {
            if (o instanceof Collection) {
                return new JSONArray((Collection) o);
            } else if (o.getClass().isArray()) {
                return toJSONArray(o);
            }
            if (o instanceof Map) {
                return new JSONObject((Map) o);
            }
            if (o instanceof Boolean ||
                    o instanceof Byte ||
                    o instanceof Character ||
                    o instanceof Double ||
                    o instanceof Float ||
                    o instanceof Integer ||
                    o instanceof Long ||
                    o instanceof Short ||
                    o instanceof String) {
                return o;
            }
            if (o.getClass().getPackage().getName().startsWith("java.")) {
                return o.toString();
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static JSONArray toJSONArray(Object array) throws JSONException {
        JSONArray result = new JSONArray();

        if(array == null) return result;
        if (!array.getClass().isArray()) {
            throw new JSONException("Not a primitive array: " + array.getClass());
        }
        final int length = Array.getLength(array);
        for (int i = 0; i < length; ++i) {
            result.put(wrap(Array.get(array, i)));
        }
        return result;
    }
}
