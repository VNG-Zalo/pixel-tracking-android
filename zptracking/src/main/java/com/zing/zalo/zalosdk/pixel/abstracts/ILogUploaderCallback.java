package com.zing.zalo.zalosdk.pixel.abstracts;

import com.zing.zalo.zalosdk.pixel.model.Event;

import java.util.List;

public interface ILogUploaderCallback {
    void onCompleted(List<Event> events, boolean result);
}
