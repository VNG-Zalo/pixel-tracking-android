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
import static com.zing.zalo.zalosdk.pixel.ZPConstants.DISPATCH_INTERVAL;
import static com.zing.zalo.zalosdk.pixel.ZPConstants.MAX_EVENT_SUBMIT;
import static com.zing.zalo.zalosdk.pixel.ZPConstants.STORE_INTERVAL;
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
    @Captor ArgumentCaptor<Message> mMessageCaptor;
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

        verify(mLogUploader).upload(eq(mEvents), eq(appId), eq(pixelId), eq(globalId),
                eq(adsId), eq(userInfo), eq(packageName), eq(connectionType), eq(location), mCallbackCaptor.capture());
        mCallbackCaptor.getValue().onCompleted(mEvents, true);
        verify(mStorage).removeEvents(mEvents);
        verify(mStorage).storeEvents();
        verify(mHandler).sendMessageDelayed(
                mMessageCaptor.capture(), eq(DISPATCH_INTERVAL));
        Message value = mMessageCaptor.getValue();
        assertThat(value.what).isEqualTo(TrackerImpl.ACT_DISPATCH_EVENTS);

        verify(mHandler).sendMessageDelayed(
                mMessageCaptor.capture(), eq(STORE_INTERVAL));
        value = mMessageCaptor.getValue();
        assertThat(value.what).isEqualTo(TrackerImpl.ACT_STORE_EVENTS);
    }

    @Test
    public void dispatchEventsManyTimesSuccess() {
        mEvents = new ArrayList<>();
        for(int i=1; i<100; i++) {
            Event e = new Event("event " + i, new JSONObject());
            mEvents.add(e);
        }

        List<Event> events1 = mEvents.subList(0, MAX_EVENT_SUBMIT);
        List<Event> events2 = mEvents.subList(MAX_EVENT_SUBMIT, mEvents.size());

        mockStorageValidData();
        when(mStorage.getEvents(eq(MAX_EVENT_SUBMIT)))
                .thenReturn(events1)
                .thenReturn(events2);

        Message msg = new Message();
        msg.what = TrackerImpl.ACT_DISPATCH_EVENTS;
        mSut.handleMessage(msg);

        verify(mLogUploader).upload(eq(events1), eq(appId), eq(pixelId), eq(globalId),
                eq(adsId), eq(userInfo), eq(packageName), eq(connectionType), eq(location), mCallbackCaptor.capture());
        mCallbackCaptor.getValue().onCompleted(events1, true);
        verify(mStorage).removeEvents(events1);

        verify(mLogUploader).upload(eq(events2), eq(appId), eq(pixelId), eq(globalId),
                eq(adsId), eq(userInfo), eq(packageName), eq(connectionType), eq(location), mCallbackCaptor.capture());
        mCallbackCaptor.getValue().onCompleted(events2, true);
        verify(mStorage).removeEvents(events2);

        verify(mStorage, times(2)).storeEvents();
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
        when(mStorage.getEvents(any(Integer.class))).thenReturn(mEvents);
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

        when(mHandler.obtainMessage(any(Integer.class))).then(new Answer<Message>() {

            @Override
            public Message answer(InvocationOnMock invocation) throws Throwable {
                int what = (int) invocation.getArguments()[0];
                Message msg = new Message();
                msg.what = what;
                return msg;
            }
        });
    }
    //test submit 50 1 lan
}
