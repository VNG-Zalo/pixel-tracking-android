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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import static com.zing.zalo.zalosdk.pixel.ZPConstants.DISPATCH_INTERVAL;
import static com.zing.zalo.zalosdk.pixel.ZPConstants.LOG_TAG;
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
    private Timer mDispatchTimer;
    private Timer mStoreTimer;

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
        cancelDispatchTimer();

        if(dispatchInterval <= 0) return;
        scheduleDispatchTimer();
    }

    public void setStoreInterval(long storeInterval) {
        mStoreInterval = storeInterval;
        cancelStoreTimer();

        if(storeInterval <=0) return;
        scheduleStoreTimer();
    }

    public void start() {
        Log.v(LOG_TAG, "Start tracker");
        if(mHandler == null) {
            mThread = new HandlerThread("zpt-event-tracker", HandlerThread.MIN_PRIORITY);
            mThread.start();
            mHandler = new Handler(mThread.getLooper(), this);
        }

        mHandler.sendEmptyMessage(ACT_LOAD_EVENTS);
        scheduleDispatchTimer();
        scheduleStoreTimer();
    }

    public void stop() {
        Log.v(LOG_TAG, "Stop tracker");
        cancelStoreTimer();
        cancelDispatchTimer();
        mHandler.sendEmptyMessage(ACT_STORE_EVENTS);
        if(mThread != null) {
            mThread.quitSafely();
        }
    }

    @Override
    public void track(String name, Map<String, Object> params) {
        Log.v(LOG_TAG, "track %s %s", name, params.toString());
        Event event = new Event(name, Utils.toJSON(params, "p"));
        Message msg = new Message();
        msg.what = ACT_PUSH_EVENT;
        msg.obj = event;
        mHandler.sendMessage(msg);
    }

    private void cancelDispatchTimer() {
        if(mDispatchTimer != null) {
            Log.v(LOG_TAG, "cancel dispatch timer");
            mDispatchTimer.cancel();
            mDispatchTimer = null;
        }
    }

    private void scheduleDispatchTimer() {
        cancelDispatchTimer();

        if(mDispatchInterval <= 0) {
            return;
        }

        Log.v(LOG_TAG,"schedule dispatch timer");
        try {
            mDispatchTimer = new Timer();
            mDispatchTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    mHandler.sendEmptyMessage(ACT_DISPATCH_EVENTS);
                }
            }, mDispatchInterval, mDispatchInterval);
        }
        catch(Exception ignored){};
    }

    private void cancelStoreTimer() {
        if(mStoreTimer != null) {
            Log.v(LOG_TAG, "cancel store timer");
            mStoreTimer.cancel();
            mStoreTimer = null;
        }
    }

    private void scheduleStoreTimer() {
        cancelStoreTimer();

        if(mStoreInterval <= 0) {
            return;
        }

        Log.v(LOG_TAG,"schedule store timer");
        try {
            mStoreTimer = new Timer();
            mStoreTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    mHandler.sendEmptyMessage(ACT_STORE_EVENTS);
                }
            }, mStoreInterval, mStoreInterval);
        }
        catch(Exception ignored){};
    }

    @Override
    public boolean handleMessage(Message message) {
        switch (message.what) {
            case ACT_DISPATCH_EVENTS:{
                if(mStorage.getEvents().size() == 0 ||
                        TextUtils.isEmpty(mIGlobalIdDProvider.globalId())) {
                    Log.v(LOG_TAG, "ACT_DISPATCH_EVENTS no submit: %d %b",
                            mStorage.getEvents().size(),
                            TextUtils.isEmpty(mIGlobalIdDProvider.globalId())
                    );
                    return false;
                }

                List<Event> events = new ArrayList<>(mStorage.getEvents());
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
                break;
            default:
                return false;
        }
        return true;
    }

    @Override
    public void onCompleted(List<Event> events, boolean result) {
        if(result) {
            Message msg = new Message();
            msg.what = ACT_REMOVE_EVENTS;
            msg.obj = events;
            mHandler.sendMessage(msg);
        }
    }
}
