package com.zing.zalo.zalosdk.pixel;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import androidx.test.core.app.ApplicationProvider;

import com.google.common.collect.Range;
import com.zing.zalo.zalosdk.core.helper.DeviceHelper;
import com.zing.zalo.zalosdk.pixel.abstracts.IAdvertiserIdProvider;
import com.zing.zalo.zalosdk.pixel.abstracts.IGlobalIdDProvider;
import com.zing.zalo.zalosdk.pixel.abstracts.ILocationProvider;
import com.zing.zalo.zalosdk.pixel.abstracts.ILogUploader;
import com.zing.zalo.zalosdk.pixel.abstracts.ILogUploaderCallback;
import com.zing.zalo.zalosdk.pixel.abstracts.IStorage;
import com.zing.zalo.zalosdk.pixel.impl.TrackerImpl;
import com.zing.zalo.zalosdk.pixel.model.Event;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class TrackerTest {
    @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
    @Mock ILogUploader mLogUploader;
    @Mock IStorage mStorage;
    @Mock IGlobalIdDProvider mGlobalIdDProvider;
    @Mock IAdvertiserIdProvider mIAdvertiserIdProvider;
    @Mock ILocationProvider mILocationProvider;
    @Mock Handler mHandler;
    @Captor ArgumentCaptor<Event> mEventCaptor;
    @Captor ArgumentCaptor<ILogUploaderCallback> mCallbackCaptor;
    private TrackerImpl mSut;

    private List<Event> mEvents;
    private String appId;
    private long pixelId;
    private String globalId;
    private String adsId;
    private Bundle userInfo;
    private String packageName;
    private String connectionType;
    private String location;

    @Before
    public void setup() throws JSONException {
        Context context = ApplicationProvider.getApplicationContext();
        mSut = new TrackerImpl(context);
        mSut.setLogUploader(mLogUploader);
        mSut.setStorage(mStorage);
        mSut.setIGlobalIdDProvider(mGlobalIdDProvider);
        mSut.setIAdsIdProvider(mIAdvertiserIdProvider);
        mSut.setILocationProvider(mILocationProvider);
        mSut.setHandler(mHandler);
        mockHandlerMessage();

        mEvents = new ArrayList<Event>() {{
            add(new Event("e1", new JSONObject("{ 'a': 'b' }")));
            add(new Event("e2", new JSONObject("{ 'a': 'b', 'd': 1 }")));
            add(new Event("e3", new JSONObject()));
        }};
        appId = "123";
        globalId = "456";
        adsId = "789";
        pixelId = 123L;
        userInfo = new Bundle();
        userInfo.putInt("gender", 1);
        userInfo.putString("name", "abc");
        packageName = context.getPackageName();
        connectionType = DeviceHelper.getConnectionType(context);
        location = "0.0:0.0";
    }

    @After
    public void teardown() {
        mSut = null;
    }

    @Test
    public void loadEventOnStart() {
        mSut.start();
        verify(mStorage, times(1)).loadEvents();
    }

    @Test
    public void trackEvent() throws JSONException {
        Map<String, Object> bundle = new HashMap<>();
        bundle.put("a", "b");
        bundle.put("b", 1);
        mSut.track("e1", bundle);

        verify(mStorage, times(1)).addEvent(mEventCaptor.capture());
        Event event = mEventCaptor.getValue();
        assertThat(event.getName()).isEqualTo("e1");
        long now = System.currentTimeMillis();
        assertThat(event.getTimestamp()).isIn(Range.closed(now-1000, now+1000));
        assertThat(event.getParams().getString("pa")).isEqualTo("b");
        assertThat(event.getParams().getInt("pb")).isEqualTo(1);
    }

    @Test
    public void storeEvents() {
        Message msg = new Message();
        msg.what = TrackerImpl.ACT_STORE_EVENTS;
        mSut.handleMessage(msg);

        verify(mStorage, times(1)).storeEvents();
    }

    @Test
    public void loadEvents() {
        Message msg = new Message();
        msg.what = TrackerImpl.ACT_LOAD_EVENTS;
        mSut.handleMessage(msg);

        verify(mStorage, times(1)).loadEvents();
    }

    @Test
    public void dispatchEventsSuccess() {
        mockStorageValidData();
        Message msg = new Message();
        msg.what = TrackerImpl.ACT_DISPATCH_EVENTS;
        mSut.handleMessage(msg);

        verify(mLogUploader, times(1)).upload(eq(mEvents), eq(appId), eq(pixelId), eq(globalId),
                eq(adsId), eq(userInfo), eq(packageName), eq(connectionType), eq(location), mCallbackCaptor.capture());
        mCallbackCaptor.getValue().onCompleted(mEvents, true);
        verify(mStorage, times(1)).removeEvents(mEvents);
    }

    @Test
    public void dispatchEventsFail() {
        mockStorageValidData();
        Message msg = new Message();
        msg.what = TrackerImpl.ACT_DISPATCH_EVENTS;
        mSut.handleMessage(msg);

        verify(mLogUploader, times(1)).upload(eq(mEvents), eq(appId), eq(pixelId), eq(globalId),
                eq(adsId), eq(userInfo), eq(packageName), eq(connectionType), eq(location), mCallbackCaptor.capture());
        mCallbackCaptor.getValue().onCompleted(mEvents, false);
        verify(mStorage, times(0)).removeEvents(mEvents);
    }

    private void mockStorageValidData() {
        when(mStorage.getEvents()).thenReturn(mEvents);
        when(mStorage.getAppId()).thenReturn(appId);
        when(mStorage.getPixelId()).thenReturn(pixelId);
        when(mStorage.getUserInfo()).thenReturn(userInfo);
        when(mGlobalIdDProvider.globalId()).thenReturn(globalId);
        when(mIAdvertiserIdProvider.getAdsId()).thenReturn(adsId);
        when(mILocationProvider.getLocation()).thenReturn(location);
    }

    private void mockHandlerMessage() {
        int[] actions = new int[] {
                TrackerImpl.ACT_PUSH_EVENT,
                TrackerImpl.ACT_REMOVE_EVENTS,
        };

        for(int action: actions) {
            Message message = new Message();
            message.what = action;
            when(mHandler.obtainMessage(action)).thenReturn(message);
        }

        when(mHandler.sendMessage(any(Message.class))).then(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                Message msg = (Message) invocation.getArguments()[0];
                mSut.handleMessage(msg);
                return null;
            }
        });

        when(mHandler.sendEmptyMessage(any(Integer.class))).then(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                int what = (int) invocation.getArguments()[0];
                Message msg = new Message();
                msg.what = what;
                mSut.handleMessage(msg);
                return null;
            }
        });
    }
}
