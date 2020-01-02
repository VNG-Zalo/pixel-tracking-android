package com.zing.zalo.zalosdk.pixel.abstracts;

import java.util.Map;

public interface IZPTracker {
    void track(String name, Map<String, Object> params);
}
