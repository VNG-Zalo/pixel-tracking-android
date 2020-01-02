package com.zing.zalo.zalosdk.pixel.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import com.zing.zalo.zalosdk.core.log.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static com.zing.zalo.zalosdk.pixel.ZPConstants.EVENT_EXPIRE_TIME;
import static com.zing.zalo.zalosdk.pixel.ZPConstants.LOG_TAG;

public class EventDataSource {
    // Database fields
    private SQLiteDatabase database;
    private EventSQLiteHelper dbHelper;

    public EventDataSource(Context context, String key) {
        dbHelper = new EventSQLiteHelper(context, key);
    }

    private void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }

    private void close() {
        dbHelper.close();
    }

    public void addAllEvents(List<Event> events) {
        for (Event event : events) {
            addEvent(event);
        }
    }

    public void removeAllEvents(List<Event> events) {
        for (Event event : events) {
            deleteEvent(event.getTimestamp());
        }
    }

    private void addEvent(Event event) {
        try {
            open();
            ContentValues values = new ContentValues();
            values.put(EventSQLiteHelper.COLUMN_TIME, event.getTimestamp());
            values.put(EventSQLiteHelper.COLUMN_NAME, event.getName());
            values.put(EventSQLiteHelper.COLUMN_PARAMS, event.getParams().toString());
            long id = database.insert(EventSQLiteHelper.TABLE_EVENT, null, values);
            if (id != -1) {
            }
        } catch(Exception ex) {
            if (ex.getMessage() == null)
                ex.printStackTrace();
            else
                Log.e(getClass().getSimpleName(), ex.getMessage());
        } finally {
            close();
        }
    }

    public void clearAllEvents() {
        try {
            open();
            database.delete(EventSQLiteHelper.TABLE_EVENT, null, null);
        } catch(Exception ex) {
            if (ex.getMessage() == null)
                ex.printStackTrace();
            else
                Log.e(getClass().getSimpleName(), ex.getMessage());
        } finally {
            close();
        }
    }

    public void deleteEvent(long startTime) {
        try {
            open();
            database.delete(EventSQLiteHelper.TABLE_EVENT,
                    EventSQLiteHelper.COLUMN_TIME + "=?",
                    new String[] { ""+startTime });
        } catch(Exception ex) {
            if (ex.getMessage() == null)
                ex.printStackTrace();
            else
                Log.e(getClass().getSimpleName(), ex.getMessage());
        } finally {
            close();
        }

    }

    public List<Event> getListEvent() {
        Cursor cursor = null;
        List<Event> sessions = new ArrayList<Event>();
        try {
            open();
            cursor = database.query(EventSQLiteHelper.TABLE_EVENT, null, null, null,
                    null, null, null);

            if (cursor!= null) {
                cursor.moveToFirst();
                while (!cursor.isAfterLast()) {
                    long timestamp = Long.parseLong(cursor.getString(0));
                    String name= cursor.getString(1);
                    JSONObject params;
                    try {
                        params = new JSONObject(cursor.getString(2));
                    } catch (JSONException e) {
                        Log.w(LOG_TAG, "");
                        params = new JSONObject();
                    }
                    Event session = new Event(name, params, timestamp);
                    sessions.add(session);
                    cursor.moveToNext();
                }
            }
            return sessions;

        }catch(Exception e) {
            return sessions;
        }
        finally{
            if (cursor != null && !cursor.isClosed()) cursor.close();
            close();
        }
    }

    public void clearOldEvents(){
        try {
            open();
            int num = database.delete(EventSQLiteHelper.TABLE_EVENT,
                    EventSQLiteHelper.COLUMN_TIME + "<?",
                    new String[] { String.valueOf(System.currentTimeMillis() - EVENT_EXPIRE_TIME)});
            Log.v(LOG_TAG, "Removed " + num + " old events!");
        } catch(Exception ex) {
            Log.e(LOG_TAG, ex.getMessage());
        } finally {
            close();
        }
    }
}
