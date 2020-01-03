package com.zing.zalo.zalosdk.pixel.impl;

import android.os.AsyncTask;
import android.os.Bundle;

import com.zing.zalo.zalosdk.pixel.BuildConfig;
import com.zing.zalo.zalosdk.core.helper.DeviceHelper;
import com.zing.zalo.zalosdk.core.http.HttpClientFactory;
import com.zing.zalo.zalosdk.core.http.HttpClientRequest;
import com.zing.zalo.zalosdk.core.log.Log;
import com.zing.zalo.zalosdk.pixel.Utils;
import com.zing.zalo.zalosdk.pixel.abstracts.ILogUploader;
import com.zing.zalo.zalosdk.pixel.abstracts.ILogUploaderCallback;
import com.zing.zalo.zalosdk.pixel.model.Event;

import org.json.JSONObject;

import java.util.List;

import static com.zing.zalo.zalosdk.core.helper.Utils.gzipCompress;
import static com.zing.zalo.zalosdk.pixel.ZPConstants.LOG_TAG;

public class LogUploader implements ILogUploader {
    public static final String API_URL = "https://px.za.zalo.me/m/tr";
    private HttpClientFactory mHttpClientFactory;
    private String mMobileNetworkCode;

    public LogUploader(HttpClientFactory httpClientFactory) {
        mHttpClientFactory = httpClientFactory;
    }

    @Override
    public void upload(List<Event> events, String appId, long pixelId, String globalId, String adsId,
                       Bundle userInfo, String packageName, String connectionType, String location, ILogUploaderCallback callback) {
        if(events.size() == 0) {
            Log.v(LOG_TAG, "No events to submit");
            return;
        }

        Task task = new Task(mHttpClientFactory, events, appId, pixelId, globalId, adsId, userInfo,
                packageName, connectionType, mMobileNetworkCode, location, callback);
        task.execute();

    }

    public void setMobileNetworkCode(String mobileNetworkCode) {
        mMobileNetworkCode = mobileNetworkCode;
    }

    private static class Task extends AsyncTask<Void, Void, Boolean> {
        String location;
        List<Event> events;
        String appId;
        long pixelId;
        String globalId;
        String adsId;
        Bundle userInfo;
        String packageName;
        String connectionType;
        ILogUploaderCallback callback;
        HttpClientFactory mHttpClientFactory;
        String mobileNetworkCode;

        Task(HttpClientFactory httpClientFactory, List<Event> events, String appId, long pixelId,
             String globalId, String adsId, Bundle userInfo, String packageName, String connectionType, String mobileNetworkCode, String location, ILogUploaderCallback callback) {
            this.mHttpClientFactory = httpClientFactory;
            this.events = events;
            this.appId = appId;
            this.pixelId = pixelId;
            this.globalId = globalId;
            this.adsId = adsId;
            this.userInfo = userInfo;
            this.callback = callback;
            this.packageName = packageName;
            this.connectionType = connectionType;
            this.location = location;
            this.mobileNetworkCode = mobileNetworkCode;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                JSONObject json = new JSONObject();
                json.put("pkg", packageName);
                json.put("app_id", appId);
                json.put("vid", globalId);
                json.put("ads_id", adsId);
                json.put("model", DeviceHelper.getModel());
                json.put("brd", DeviceHelper.getBrand());
                json.put("pl", "2002");
                json.put("net", connectionType);
                json.put("osv", DeviceHelper.getOSVersion());
                json.put("sdkv", BuildConfig.VERSION_NAME);
                json.put("loc", location);
                json.put("mnc", mobileNetworkCode);
                json.put("events", Utils.toJSON(events));
                json.put("user_info", Utils.toJSON(userInfo));

                String src = json.toString();
                byte[] body = gzipCompress(src);

                String url = API_URL + "?id=" + pixelId;
                HttpClientRequest req = mHttpClientFactory.newRequest(HttpClientRequest.Type.POST, url);
                req.addHeader("Content-Encoding", "gzip");
                req.addHeader("Content-Type", "application/json");
                req.addHeader("Content-Length", String.valueOf(body.length));
                req.setBody(body);

                Log.v(LOG_TAG, src);

                req.getResponse();
                int code = req.liveResponseCode;
                if(code != 200) {
                    Log.w(LOG_TAG, "Upload err with status code: %d, skip retry!", code);
//                    return true;
                }

                return code > 0;
            } catch (Exception e) {
                Log.w(LOG_TAG, "Upload err: %s", e);
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            if(callback != null) {
                callback.onCompleted(events, result);
            }
        }
    }

}
