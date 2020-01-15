package com.zing.zalo.zalosdk.pixel.impl;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.text.TextUtils;

import com.zing.zalo.zalosdk.core.helper.DeviceHelper;
import com.zing.zalo.zalosdk.core.log.Log;
import com.zing.zalo.zalosdk.pixel.Utils;
import com.zing.zalo.zalosdk.pixel.abstracts.IAdvertiserIdProvider;
import com.zing.zalo.zalosdk.pixel.abstracts.IGlobalIdDProvider;
import com.zing.zalo.zalosdk.pixel.abstracts.ILocationProvider;
import com.zing.zalo.zalosdk.pixel.abstracts.ILogUploader;
import com.zing.zalo.zalosdk.pixel.abstracts.ILogUploaderCallback;
import com.zing.zalo.zalosdk.pixel.abstracts.IStorage;
import com.zing.zalo.zalosdk.pixel.abstracts.IZPTracker;
import com.zing.zalo.zalosdk.pixel.model.Event;

import java.util.List;
import java.util.Map;

import static com.zing.zalo.zalosdk.pixel.ZPConstants.DISPATCH_INTERVAL;
import static com.zing.zalo.zalosdk.pixel.ZPConstants.LOG_TAG;
import static com.zing.zalo.zalosdk.pixel.ZPConstants.MAX_EVENT_SUBMIT;
import static com.zing.zalo.zalosdk.pixel.ZPConstants.STORE_INTERVAL;

public class TrackerImpl implements IZPTracker, Handler.Callback, ILogUploaderCallback {
    public static final int ACT_DISPATCH_EVENTS = 0x6000;
    public static final int ACT_PUSH_EVENT = 0x6001;
    public static final int ACT_REMOVE_EVENTS = 0x6002;
    public static final int ACT_STORE_EVENTS = 0x6003;
    public static final int ACT_LOAD_EVENTS = 0x6004;
    private final Context mContext;

    private ILogUploader mLogUploader;
    private IStorage mStorage;
    private IAdvertiserIdProvider mIAdsIdProvider;
    private IGlobalIdDProvider mIGlobalIdDProvider;
    private ILocationProvider mILocationProvider;
    private long mDispatchInterval;
    private long mStoreInterval;
    private HandlerThread mThread;
    private Handler mHandler;

    public TrackerImpl(Context context) {
        mDispatchInterval = DISPATCH_INTERVAL;
        mStoreInterval = STORE_INTERVAL;
        mContext = context;
    }

    public void setLogUploader(ILogUploader logUploader) {
        mLogUploader = logUploader;
    }

    public void setStorage(IStorage storage) {
        mStorage = storage;
    }

    public void setIAdsIdProvider(IAdvertiserIdProvider IAdsIdProvider) {
        mIAdsIdProvider = IAdsIdProvider;
    }

    public void setIGlobalIdDProvider(IGlobalIdDProvider IGlobalIdDProvider) {
        mIGlobalIdDProvider = IGlobalIdDProvider;
    }

    public void setILocationProvider(ILocationProvider ILocationProvider) {
        mILocationProvider = ILocationProvider;
    }

    public void setHandler(Handler handler) {
        mHandler = handler;
    }

    public void setDispatchInterval(long dispatchInterval) {
        mDispatchInterval = dispatchInterval;
        scheduleNextDispatch();
    }

    public void setStoreInterval(long storeInterval) {
        mStoreInterval = storeInterval;
        scheduleNextStore();
    }

    public void start() {
        Log.v(LOG_TAG, "Start tracker");
        if(mHandler == null) {
            mThread = new HandlerThread("zpt-event-tracker", HandlerThread.MIN_PRIORITY);
            mThread.start();
            mHandler = new Handler(mThread.getLooper(), this);
        }

        mHandler.sendEmptyMessage(ACT_LOAD_EVENTS);
        scheduleNextDispatch();
        scheduleNextStore();
    }

    public void stop() {
        Log.v(LOG_TAG, "Stop tracker");
        mHandler.removeMessages(ACT_DISPATCH_EVENTS);
        mHandler.removeMessages(ACT_STORE_EVENTS);
        mHandler.sendEmptyMessage(ACT_STORE_EVENTS);
        if(mThread != null) {
            mThread.quitSafely();
        }
    }

    @Override
    public void track(String name, Map<String, Object> params) {
        Log.v(LOG_TAG, "track %s %s", name, params.toString());
        Event event = new Event(name, Utils.toJSON(params, "p"));
        Message msg = mHandler.obtainMessage(ACT_PUSH_EVENT);
        msg.what = ACT_PUSH_EVENT;
        msg.obj = event;
        mHandler.sendMessage(msg);
    }

    @Override
    public boolean handleMessage(Message message) {
        switch (message.what) {
            case ACT_DISPATCH_EVENTS:{
                List<Event> events = mStorage.getEvents(MAX_EVENT_SUBMIT);
                if(events.size() == 0 ||
                        TextUtils.isEmpty(mIGlobalIdDProvider.globalId())) {
                    Log.v(LOG_TAG, "no submit: %d %b",
                            events.size(),
                            TextUtils.isEmpty(mIGlobalIdDProvider.globalId())
                    );
                    scheduleNextDispatch();
                    return false;
                }

                Log.v(LOG_TAG, "Dispatch %s events", events.size());
                mLogUploader.upload(events, mStorage.getAppId(), mStorage.getPixelId(),
                        mIGlobalIdDProvider.globalId(), mIAdsIdProvider.getAdsId(), mStorage.getUserInfo(),
                        mContext.getPackageName(), DeviceHelper.getConnectionType(mContext),
                        mILocationProvider.getLocation(),this);
            }

                break;
            case ACT_REMOVE_EVENTS:
                mStorage.removeEvents((List<Event>) message.obj);
                break;
            case ACT_PUSH_EVENT:
                mStorage.addEvent((Event) message.obj);
                break;
            case ACT_LOAD_EVENTS:
                mStorage.loadEvents();
                break;
            case ACT_STORE_EVENTS:
                mStorage.storeEvents();
                scheduleNextStore();
                break;
            default:
                return false;
        }
        return true;
    }

    @Override
    public void onCompleted(List<Event> events, boolean result) {
        Message msg;
        if(result) {
            mHandler.removeMessages(ACT_STORE_EVENTS);

            msg = mHandler.obtainMessage(ACT_REMOVE_EVENTS);
            msg.what = ACT_REMOVE_EVENTS;
            msg.obj = events;
            mHandler.sendMessage(msg);

            msg = mHandler.obtainMessage(ACT_STORE_EVENTS);
            msg.what = ACT_STORE_EVENTS;
            mHandler.sendMessage(msg);

            if(events.size() >= MAX_EVENT_SUBMIT) {
                mHandler.removeMessages(ACT_DISPATCH_EVENTS);
                msg = mHandler.obtainMessage(ACT_DISPATCH_EVENTS);
                msg.what = ACT_DISPATCH_EVENTS;
                mHandler.sendMessage(msg);
                return;
            }
        }

        scheduleNextDispatch();
    }

    private void scheduleNextDispatch() {
        mHandler.removeMessages(ACT_DISPATCH_EVENTS);
        if(mDispatchInterval > 0) {
            Message msg = mHandler.obtainMessage(ACT_DISPATCH_EVENTS);
            msg.what = ACT_DISPATCH_EVENTS;
            mHandler.sendMessageDelayed(msg, mDispatchInterval);
        }
    }

    private void scheduleNextStore() {
        mHandler.removeMessages(ACT_STORE_EVENTS);
        if(mStoreInterval > 0) {
            Message msg = mHandler.obtainMessage(ACT_STORE_EVENTS);
            msg.what = ACT_STORE_EVENTS;
            mHandler.sendMessageDelayed(msg, mStoreInterval);
        }
    }
}
