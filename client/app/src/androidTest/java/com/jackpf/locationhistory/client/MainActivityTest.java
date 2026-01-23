package com.jackpf.locationhistory.client;

import static org.junit.Assert.assertEquals;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class MainActivityTest {

    @Test
    public void activityLaunches() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity ->
                    assertEquals(MainActivity.class, activity.getClass())
            );
        }
    }

    @Test
    public void activityReachesResumedState() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            assertEquals(Lifecycle.State.RESUMED, scenario.getState());
        }
    }

//    @Test
//    public void activityReachesResumedStatef() {
//        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
//            assertEquals(Lifecycle.State.RESUMED, scenario.getState());
//            assertEquals(1, 2);
//        }
//    }
}
