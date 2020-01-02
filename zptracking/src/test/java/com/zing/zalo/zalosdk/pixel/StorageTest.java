package com.zing.zalo.zalosdk.pixel;


import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.zing.zalo.zalosdk.pixel.impl.Storage;
import com.zing.zalo.zalosdk.pixel.model.Event;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

@RunWith(RobolectricTestRunner.class)
public class StorageTest {
    @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();


    private Storage mSut;
    private Context mContext;
    private List<Event> mEvents;

    @Before
    public void setup() throws JSONException {
        mContext = ApplicationProvider.getApplicationContext();
        mSut = new Storage(mContext, 123L);
        mEvents = new ArrayList<Event>() {{
            add(new Event("e1", new JSONObject("{ 'a': 'b' }")));
            add(new Event("e2", new JSONObject("{ 'a': 'b', 'd': 1 }")));
            add(new Event("e3", new JSONObject()));
        }};
    }

    @After
    public void teardown() {
        mSut.clear();
    }

    @Test
    public void addAndRemoveEvents() {
        assertThat(mSut.getEvents().size()).isEqualTo(0);

        mSut.addEvent(mEvents.get(0));
        mSut.addEvent(mEvents.get(1));
        mSut.addEvent(mEvents.get(2));

        assertThat(mSut.getEvents().size()).isEqualTo(3);

        mSut.removeEvents(mEvents);

        assertThat(mSut.getEvents().size()).isEqualTo(0);
    }

    @Test
    public void saveAndLoadEvents() {
        assertThat(mSut.getEvents().size()).isEqualTo(0);

        mSut.addEvent(mEvents.get(0));
        mSut.addEvent(mEvents.get(1));
        mSut.addEvent(mEvents.get(2));

        mSut.storeEvents();

        mSut = new Storage(mContext, 123L);
        mSut.loadEvents();
        assertThat(mSut.getEvents().size()).isEqualTo(3);
    }
}
