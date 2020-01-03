# pixel-tracking-android
Pixel Tracking module for Android

## Requirement 
- Android v18+
- Zalo SDK core lib v2.4.1030+
- Permission "android.permission.INTERNET"

## Install
- Add to build.gradle
```groovy
implementation "com.zing.zalo.zalosdk:pixel:+"
```

## Usage
```java
long pixelId = 1;
String event = "buy";

Map<String, Object> params = new HashMap();
params.put("brand", "apple");
params.put("model", "iphone5");

Tracker tracker = Tracker.newInstance(context, pixelId);
tracker.track(event, params);
```
