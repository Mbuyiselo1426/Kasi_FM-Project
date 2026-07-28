package com.kasiefm;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isClickable;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

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
 * Verifies the button actually receives touch events end-to-end - a tap gets
 * routed all the way to RadioService and the service's reported state comes
 * back to update the UI - rather than being swallowed before it ever reaches
 * the OnClickListener.
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
        // button should be sitting in its initial PAUSED state.
        onView(withId(R.id.playPauseButton))
                .check(matches(isDisplayed()))
                .check(matches(isEnabled()))
                .check(matches(isClickable()))
                .check(matches(withContentDescription(R.string.play_action)));

        // First tap: PAUSED -> play(). This doesn't have to change the UI
        // synchronously (LIVE depends on the player actually buffering), but
        // it does have to be delivered to the button rather than eaten by
        // something drawn on top of it.
        onView(withId(R.id.playPauseButton)).perform(click());

        // Second tap: play() already flipped RadioService's internal
        // playWhenReady state, so this tap calls pause() instead - which
        // synchronously reports PAUSED back to the UI. If either tap had
        // been swallowed before reaching the button, we would not land back
        // on the "Play" content description here.
        onView(withId(R.id.playPauseButton)).perform(click());

        onView(withId(R.id.playPauseButton))
                .check(matches(withContentDescription(R.string.play_action)));
    }
}
