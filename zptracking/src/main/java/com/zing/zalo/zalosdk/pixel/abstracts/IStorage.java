package com.zing.zalo.zalosdk.pixel.abstracts;

import android.os.Bundle;

import com.zing.zalo.zalosdk.pixel.model.Event;

import java.util.List;

public interface IStorage {
    String getAppId();
    long getPixelId();
    Bundle getUserInfo();
    List<Event> getEvents();

    void addEvent(Event event);
    void removeEvents(List<Event> events);
    void loadEvents();
    void storeEvents();
}
