package com.zing.zalo.zalosdk.pixel.model;

import android.app.ActivityManager;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Process;

public class EventSQLiteHelper extends SQLiteOpenHelper {
    static final String TABLE_EVENT   	  		 = "zp_events";
    static final String COLUMN_TIME  			 = "time";
    static final String COLUMN_NAME  	 		 = "name";
    static final String COLUMN_PARAMS  	 		 = "params";

    private static final String DATABASE_NAME = "zp_events";
    private static final int DATABASE_VERSION = 1;

    // Database creation sql statement
    private static final String DATABASE_CREATE = "create table "
            + TABLE_EVENT + "("
            + COLUMN_TIME	+ " integer not null, "
            + COLUMN_NAME + " text not null, "
            + COLUMN_PARAMS	+ " text not null);";

    EventSQLiteHelper(Context context, String key) {
        super(context, getCurrentProcessName(context) + "." + DATABASE_NAME + key + ".db", null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(DATABASE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_EVENT);
        onCreate(db);
    }

    private static String getCurrentProcessName(Context context) {
        try {
            ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            int myPid = Process.myPid();
            for (ActivityManager.RunningAppProcessInfo processInfo : manager.getRunningAppProcesses()) {
                if(processInfo.pid == myPid) {
                    return processInfo.processName;
                }
            }

            return context.getPackageName();
        }
        catch (Exception ex) {
            return "default";
        }
    }
}
