# pixel-tracking-android
Pixel Tracking module for Android

## Install
Add to build.gradle
```groovy
implementation "com.zing.zalo.zalosdk:pixel:+"
```
Add to AndroidManifest.xml
```xml
<uses-permission android:name="android.permission.INTERNET" />
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
