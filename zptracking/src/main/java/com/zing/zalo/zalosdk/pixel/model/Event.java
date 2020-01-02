package com.zing.zalo.zalosdk.pixel.model;

import org.json.JSONObject;

import java.util.Locale;

public class Event {
    private String name;
    private JSONObject params;
    private long timestamp;

    public Event(String name, JSONObject params) {
        this(name, params, System.currentTimeMillis());
    }

    public Event(String name, JSONObject params, long timestamp) {
        this.name = name;
        this.params = params;
        this.timestamp = timestamp;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public JSONObject getParams() {
        return params;
    }

    public void setParams(JSONObject params) {
        this.params = params;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return String.format(Locale.getDefault(), "[n:%s] [ts:%d] [p:%s]", name, timestamp, params.toString());
    }
}
