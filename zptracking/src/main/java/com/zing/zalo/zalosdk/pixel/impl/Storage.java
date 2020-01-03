package com.zing.zalo.zalosdk.pixel.impl;

import android.content.Context;
import android.os.Bundle;

import com.zing.zalo.zalosdk.core.log.Log;
import com.zing.zalo.zalosdk.pixel.abstracts.IStorage;
import com.zing.zalo.zalosdk.pixel.model.Event;
import com.zing.zalo.zalosdk.pixel.model.EventDataSource;

import java.util.ArrayList;
import java.util.List;

import static com.zing.zalo.zalosdk.pixel.ZPConstants.LOG_TAG;
import static com.zing.zalo.zalosdk.pixel.ZPConstants.MAX_EVENT_STORED;

/**
 * This class is not thread safe
 */
public class Storage implements IStorage {
    private Bundle mUserInfo;
    private String mAppId;
    private long mPixelId;
    private List<Event> mEvents;
    private List<Event> mPendingWriteEvents;
    private List<Event> mPendingDeleteEvents;
    private int mMaxEventStored;

    private EventDataSource mEventDataSource;

    public Storage(Context context, long pixelId) {
        mMaxEventStored = MAX_EVENT_STORED;
        mEvents = new ArrayList<>();
        mPendingWriteEvents = new ArrayList<>();
        mPendingDeleteEvents = new ArrayList<>();
        mEventDataSource = new EventDataSource(context, String.valueOf(pixelId));
        mUserInfo = new Bundle();
        mAppId = "";
        mPixelId = pixelId;
    }

    public void setMaxEventStored(int maxEventStored) {
        mMaxEventStored = maxEventStored;
    }

    @Override
    public Bundle getUserInfo() {
        return mUserInfo;
    }

    public void setUserInfo(Bundle userInfo) {
        this.mUserInfo = userInfo;
    }

    @Override
    public String getAppId() {
        return mAppId;
    }

    public void setAppId(String appId) {
        mAppId = appId;
    }

    @Override
    public long getPixelId() {
        return mPixelId;
    }

    @Override
    public List<Event> getEvents() {
        return mEvents;
    }

    @Override
    public void addEvent(Event event) {
        Log.v(LOG_TAG, "Add event: %s", event);
        mEvents.add(event);
        mPendingWriteEvents.add(event);
        int overflow = mEvents.size() - mMaxEventStored;
        if(overflow > 0) {
            Event[] res = new Event[overflow];
            res = mEvents.subList(0, overflow).toArray(res);
            for(Event e : res) {
                mEvents.remove(e);
                mPendingDeleteEvents.add(e);
            }
        }
    }

    @Override
    public void removeEvents(List<Event> events) {
        Log.v(LOG_TAG, "Remove %d events", events.size());
        mPendingDeleteEvents.addAll(events);
        mEvents.removeAll(events);
    }

    @Override
    public void loadEvents() {
        mEvents.clear();
        mPendingWriteEvents.clear();
        mPendingDeleteEvents.clear();
        mEventDataSource.clearOldEvents();
        mEvents.addAll(mEventDataSource.getListEvent());
        Log.v(LOG_TAG, "Loaded %d events", mEvents.size());
    }

    @Override
    public void storeEvents() {
        mEventDataSource.addAllEvents(mPendingWriteEvents);
        mPendingWriteEvents.clear();

        mEventDataSource.removeAllEvents(mPendingDeleteEvents);
        mPendingDeleteEvents.clear();
        Log.v(LOG_TAG, "Stored %d events", mEvents.size());
    }

    public void clear() {
        mEvents.clear();
        mEventDataSource.clearAllEvents();
        mPendingDeleteEvents.clear();
        mPendingWriteEvents.clear();
    }
}
