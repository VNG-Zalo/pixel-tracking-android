package com.zing.zalo.zalosdk.pixel;

import android.content.Context;
import android.os.Bundle;

import androidx.test.core.app.ApplicationProvider;

import com.zing.zalo.zalosdk.core.helper.DeviceHelper;
import com.zing.zalo.zalosdk.core.http.HttpClientFactory;
import com.zing.zalo.zalosdk.core.http.HttpClientRequest;
import com.zing.zalo.zalosdk.pixel.abstracts.ILogUploaderCallback;
import com.zing.zalo.zalosdk.pixel.impl.LogUploader;
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
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.skyscreamer.jsonassert.JSONAssert;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static com.zing.zalo.zalosdk.core.helper.Utils.gzipDecompress;
import static com.zing.zalo.zalosdk.pixel.impl.LogUploader.API_URL;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class LogUploaderTest {
    @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
    @Mock HttpClientFactory mHttpClientFactory;
    @Mock HttpClientRequest mRequest;
    @Captor ArgumentCaptor<byte[]> mBodyCaptor;

    private LogUploader mSut;
    private List<Event> mEvents;
    private String appId;
    private long pixelId;
    private String globalId;
    private String adsId;
    private Bundle userInfo;
    private String packageName;
    private String connectionType;
    private String location;
    private String mnc;

    @Before
    public void setup() throws JSONException {
        Context context = ApplicationProvider.getApplicationContext();
        mSut = new LogUploader(mHttpClientFactory);
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
        mnc = "40025";
    }

    @After
    public void teardown() {
        mSut = null;
    }

    @Test
    public void uploadSuccess() throws JSONException, IOException {
        String url = API_URL + "?id=" + pixelId;
        when(mHttpClientFactory.newRequest(HttpClientRequest.Type.POST, url)).thenReturn(mRequest);
        mRequest.liveResponseCode = 200;
        when(mRequest.getResponseCode()).thenReturn(200);
        mSut.setMobileNetworkCode(mnc);
        mSut.upload(mEvents, appId, pixelId, globalId, adsId, userInfo, packageName, connectionType,
                location, new ILogUploaderCallback() {
            @Override
            public void onCompleted(List<Event> events, boolean result) {
                assertThat(result).isTrue();
                assertThat(events).isEqualTo(mEvents);
            }
        });

        verify(mRequest, times(1)).addHeader("Content-Encoding", "gzip");
        verify(mRequest, times(1)).addHeader("Content-Type", "application/json");
        verify(mRequest, times(1)).setBody(mBodyCaptor.capture());
        byte[] body = mBodyCaptor.getValue();
        verify(mRequest, times(1)).addHeader("Content-Length",
                String.valueOf(body.length));

        String str = gzipDecompress(body);
        JSONObject json = new JSONObject(str);
        assertThat(json.getString("app_id")).isEqualTo(appId);
        assertThat(json.getString("ads_id")).isEqualTo(adsId);
        assertThat(json.getString("vid")).isEqualTo(globalId);
        assertThat(json.getString("pkg")).isEqualTo(packageName);
        assertThat(json.getString("model")).isEqualTo(DeviceHelper.getModel());
        assertThat(json.getString("brd")).isEqualTo(DeviceHelper.getBrand());
        assertThat(json.getString("pl")).isEqualTo("2002");
        assertThat(json.getString("net")).isEqualTo(connectionType);
        assertThat(json.getString("osv")).isEqualTo(DeviceHelper.getOSVersion());
        assertThat(json.getString("sdkv")).isEqualTo(BuildConfig.VERSION_NAME);
        assertThat(json.getString("loc")).isEqualTo(location);
        assertThat(json.getString("mnc")).isEqualTo(mnc);

        JSONAssert.assertEquals(json.getJSONObject("user_info"), Utils.toJSON(userInfo), false);
        JSONAssert.assertEquals(json.getJSONArray("events"), Utils.toJSON(mEvents), false);
    }

    @Test
    public void uploadNullParams() throws JSONException, IOException {
        String url = API_URL + "?id=" + pixelId;
        when(mHttpClientFactory.newRequest(HttpClientRequest.Type.POST, url)).thenReturn(mRequest);
        mSut.upload(mEvents, null, pixelId, null, null, null, packageName,
                connectionType, location, null);

        verify(mRequest, times(1)).setBody(mBodyCaptor.capture());
        byte[] body = mBodyCaptor.getValue();

        String str = gzipDecompress(body);
        JSONObject json = new JSONObject(str);
        assertThat(json.has("app_id")).isFalse();
        assertThat(json.has("ads_id")).isFalse();
        assertThat(json.has("global_id")).isFalse();

        JSONAssert.assertEquals(json.getJSONObject("user_info"), new JSONObject(), false);
        JSONAssert.assertEquals(json.getJSONArray("events"), Utils.toJSON(mEvents), false);
    }

    @Test
    public void uploadFail() {
        String url = API_URL + "?id=" + pixelId;
        when(mHttpClientFactory.newRequest(HttpClientRequest.Type.POST, url)).thenReturn(mRequest);
        mRequest.liveResponseCode = 0;
        when(mRequest.getResponseCode()).thenReturn(0);

        mSut.upload(mEvents, appId, pixelId, globalId, adsId, userInfo, packageName, connectionType, location,
                new ILogUploaderCallback() {
            @Override
            public void onCompleted(List<Event> events, boolean result) {
                assertThat(result).isFalse();
            }
        });
    }
}
