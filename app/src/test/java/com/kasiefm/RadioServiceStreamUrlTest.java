package com.kasiefm;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Focused JVM coverage for RadioService's stream-resolution gate. */
public class RadioServiceStreamUrlTest {

    @Test
    public void successfulResolution_makesUrlAvailableWithoutQueuingPlayback() {
        RadioService.StreamUrlResolutionState state = new RadioService.StreamUrlResolutionState();

        assertTrue(state.beginResolution());
        assertTrue(RadioService.isValidStreamUrl("https://radio.example.com/live.mp3"));
        assertFalse(state.completeResolution("https://radio.example.com/live.mp3"));

        assertTrue(state.hasResolvedUrl());
        assertFalse(state.hasFailed());
    }

    @Test
    public void playBeforeResolution_completesStartsWhenUrlArrives() {
        RadioService.StreamUrlResolutionState state = new RadioService.StreamUrlResolutionState();

        assertTrue(state.beginResolution());
        assertFalse(state.requestPlayback()); // Request is already in flight.

        assertTrue(state.completeResolution("https://radio.example.com/live.mp3"));
        assertTrue(state.hasResolvedUrl());
    }

    @Test
    public void apiFailure_leavesNoUrlAvailableForPreparation() {
        RadioService.StreamUrlResolutionState state = new RadioService.StreamUrlResolutionState();

        assertTrue(state.beginResolution());
        state.failResolution();

        assertFalse(state.hasResolvedUrl());
        assertNull(state.getResolvedUrl());
        assertTrue(state.hasFailed());
    }

    @Test
    public void emptyOrBlankUrl_isRejected() {
        assertFalse(RadioService.isValidStreamUrl(null));
        assertFalse(RadioService.isValidStreamUrl(""));
        assertFalse(RadioService.isValidStreamUrl("   "));
        assertFalse(RadioService.isValidStreamUrl("not a URL"));
    }

    @Test
    public void retryAfterResolutionFailure_startsANewRequestAndHonoursPlay() {
        RadioService.StreamUrlResolutionState state = new RadioService.StreamUrlResolutionState();

        assertTrue(state.beginResolution());
        state.failResolution();

        assertTrue(state.requestPlayback());
        assertTrue(state.beginResolution());
        assertTrue(state.completeResolution("https://radio.example.com/live.mp3"));
    }
}
