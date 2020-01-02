package com.zing.zalo.zalosdk.pixel.abstracts;

import android.os.Bundle;

import com.zing.zalo.zalosdk.pixel.model.Event;

import java.util.List;

public interface ILogUploader {
    void upload(List<Event> events, String appId, long pixelId, String globalId, String adsId,
                Bundle userInfo, String packageName, String connectionType, String location, ILogUploaderCallback callback);
}
