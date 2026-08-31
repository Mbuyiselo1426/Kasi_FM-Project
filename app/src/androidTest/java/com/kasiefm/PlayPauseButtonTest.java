package com.kasiefm;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isClickable;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
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
 * Checks RadioService's actual playWhenReady state after a tap rather than
 * the UI labels, which only update synchronously on the pause() path and so
 * can't tell a swallowed tap from a real one on the play() path.
 *
 * Deliberately only exercises the PAUSED -> play() direction, not a
 * play() -> pause() round trip: tapping play kicks off real ExoPlayer
 * buffering against the live stream, which intentionally disables the
 * button (see MainActivity#updateUiForState's BUFFERING case) - and Espresso
 * waits for the main thread to fully quiesce after perform(click()), which
 * can include that disable happening before a second click ever lands. That
 * makes a second immediate tap indistinguishable from a real regression.
 * Testing the play() direction alone is sufficient to catch a click getting
 * swallowed before it reaches the OnClickListener.
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
    public void tappingButton_startsPlayback() {
        // Idling resource has already waited for RadioService to bind, so the
        // button should be sitting in its initial, interactive PAUSED state.
        onView(withId(R.id.playPauseButton))
                .check(matches(isDisplayed()))
                .check(matches(isEnabled()))
                .check(matches(isClickable()));

        activityRule.getScenario().onActivity(activity ->
                assertFalse("expected not playing before any taps", activity.isPlayingForTesting()));

        // Tapping play now may either start playback immediately (if the
        // stream URL was already resolved) or move into BUFFERING while the
        // URL fetch completes. Either outcome proves the click reached the
        // OnClickListener. Remaining PAUSED would mean the tap was ignored.
        onView(withId(R.id.playPauseButton)).perform(click());

        activityRule.getScenario().onActivity(activity -> {
            String state = activity.getPlaybackStateForTesting();
            assertNotNull("expected a bound playback state after tapping play", state);
            assertTrue(
                    "tap should have triggered play(), expected BUFFERING or LIVE but was " + state,
                    "BUFFERING".equals(state) || "LIVE".equals(state)
            );
        });
    }
}
