package com.kasiefm;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isClickable;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.test.espresso.IdlingRegistry;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Regression test for the top-card play/pause button being non-interactive.
 * Checks RadioService's actual playWhenReady state after each tap rather than
 * the UI labels - the labels only update synchronously on the pause() path,
 * so they can't tell a swallowed tap from a real one on the play() path.
 */
@RunWith(AndroidJUnit4.class)
public class PlayPauseButtonTest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Before
    public void registerIdlingResource() {
        IdlingRegistry.getInstance().register(MainActivity.getIdlingResource());
    }

    @After
    public void unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(MainActivity.getIdlingResource());
    }

    @Test
    public void tappingButton_isReceivedAndTogglesPlaybackState() {
        // Idling resource has already waited for RadioService to bind, so the
        // button should be sitting in its initial, interactive PAUSED state.
        onView(withId(R.id.playPauseButton))
                .check(matches(isDisplayed()))
                .check(matches(isEnabled()))
                .check(matches(isClickable()));

        activityRule.getScenario().onActivity(activity ->
                assertFalse("expected not playing before any taps", activity.isPlayingForTesting()));

        // First tap: not playing -> play(). Synchronously flips playWhenReady,
        // independent of whether the stream has actually started buffering.
        onView(withId(R.id.playPauseButton)).perform(click());

        activityRule.getScenario().onActivity(activity ->
                assertTrue("first tap should have called play()", activity.isPlayingForTesting()));

        // Second tap: now playing -> pause(). If either tap had been swallowed
        // before reaching the button (e.g. by something drawn on top of it),
        // this would still read as playing (or never-playing) instead.
        onView(withId(R.id.playPauseButton)).perform(click());

        activityRule.getScenario().onActivity(activity ->
                assertFalse("second tap should have called pause()", activity.isPlayingForTesting()));
    }
}
