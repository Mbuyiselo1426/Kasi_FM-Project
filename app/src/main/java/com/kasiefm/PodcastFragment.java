package com.kasiefm;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.Player;
import com.kasiefm.api.KasieApiService;
import com.kasiefm.api.PodcastEpisode;
import com.kasiefm.api.RetrofitClient;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Catch Up tab: fetches podcast metadata and owns only on-screen podcast playback. */
public class PodcastFragment extends Fragment {
    private TextView status;
    private LinearLayout list;
    private Call<List<PodcastEpisode>> podcastsCall;
    private ExoPlayer player;
    private PodcastEpisode selectedEpisode;
    private Button selectedPlayButton;

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container, @Nullable Bundle state) {
        return inflater.inflate(R.layout.fragment_podcasts, container, false);
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        status = view.findViewById(R.id.podcast_status);
        list = view.findViewById(R.id.podcast_list);
        loadPodcasts();
    }

    private void loadPodcasts() {
        status.setVisibility(View.VISIBLE);
        status.setText("Loading podcasts...");
        list.removeAllViews();
        podcastsCall = RetrofitClient.getInstance().create(KasieApiService.class).getPodcasts();
        podcastsCall.enqueue(new Callback<List<PodcastEpisode>>() {
            @Override public void onResponse(@NonNull Call<List<PodcastEpisode>> call,
                    @NonNull Response<List<PodcastEpisode>> response) {
                if (!canRender(call)) return;
                podcastsCall = null;
                if (!response.isSuccessful() || response.body() == null) {
                    renderError();
                    return;
                }
                renderEpisodes(response.body());
            }

            @Override public void onFailure(@NonNull Call<List<PodcastEpisode>> call,
                    @NonNull Throwable error) {
                if (!canRender(call) || call.isCanceled()) return;
                podcastsCall = null;
                renderError();
            }
        });
    }

    private boolean canRender(Call<List<PodcastEpisode>> call) {
        return call == podcastsCall && isAdded() && getView() != null;
    }

    private void renderEpisodes(List<PodcastEpisode> episodes) {
        list.removeAllViews();
        if (episodes.isEmpty()) {
            status.setVisibility(View.VISIBLE);
            status.setText("No podcasts available right now.");
            return;
        }
        status.setVisibility(View.GONE);
        for (PodcastEpisode episode : episodes) {
            addEpisode(episode);
        }
    }

    private void renderError() {
        list.removeAllViews();
        status.setVisibility(View.VISIBLE);
        status.setText("Podcasts are unavailable right now.");
    }

    private void addEpisode(PodcastEpisode episode) {
        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_dark_card);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.bottomMargin = dp(10);
        list.addView(card, cardParams);

        TextView label = new TextView(requireContext());
        label.setText("PODCAST");
        label.setTextColor(ContextCompat.getColor(requireContext(), R.color.kasie_accent));
        label.setTextSize(11);
        label.setTypeface(android.graphics.Typeface.MONOSPACE);
        label.setLetterSpacing(0.08f);
        card.addView(label);

        TextView title = new TextView(requireContext());
        title.setText(nonEmpty(episode.getTitle(), "Untitled episode"));
        title.setTextColor(ContextCompat.getColor(requireContext(), R.color.kasie_text_primary));
        title.setTextSize(17);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setMaxLines(2);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        titleParams.topMargin = dp(6);
        card.addView(title, titleParams);

        TextView showName = new TextView(requireContext());
        showName.setText(nonEmpty(episode.getShowName(), "Kasie FM"));
        showName.setTextColor(ContextCompat.getColor(requireContext(), R.color.kasie_text_secondary));
        showName.setTextSize(14);
        showName.setMaxLines(1);
        showName.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams showParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        showParams.topMargin = dp(5);
        card.addView(showName, showParams);

        String description = blankToEmpty(episode.getDescription());
        if (!description.isEmpty()) {
            TextView descriptionView = new TextView(requireContext());
            descriptionView.setText(description);
            descriptionView.setTextColor(ContextCompat.getColor(requireContext(), R.color.kasie_text_muted));
            descriptionView.setTextSize(13);
            descriptionView.setMaxLines(3);
            descriptionView.setEllipsize(android.text.TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams descriptionParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            descriptionParams.topMargin = dp(10);
            card.addView(descriptionView, descriptionParams);
        }

        String publishedAt = blankToEmpty(episode.getPublishedAt());
        if (!publishedAt.isEmpty()) {
            TextView date = new TextView(requireContext());
            date.setText("PUBLISHED  •  " + publishedAt);
            date.setTextColor(ContextCompat.getColor(requireContext(), R.color.kasie_text_muted));
            date.setTextSize(11);
            date.setTypeface(android.graphics.Typeface.MONOSPACE);
            date.setMaxLines(1);
            date.setEllipsize(android.text.TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams dateParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dateParams.topMargin = dp(10);
            card.addView(date, dateParams);
        }

        Button playButton = new Button(requireContext());
        playButton.setAllCaps(false);
        playButton.setText("▶  PLAY EPISODE");
        playButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.kasie_text_on_accent));
        playButton.setBackgroundResource(R.drawable.bg_onair_pill);
        playButton.setContentDescription("Play " + nonEmpty(episode.getTitle(), "podcast episode"));
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(dp(168), dp(44));
        buttonParams.topMargin = dp(12);
        buttonParams.gravity = Gravity.START;
        card.addView(playButton, buttonParams);

        if (!hasPlayableUrl(episode)) {
            playButton.setEnabled(false);
            playButton.setText("Unavailable");
        } else {
            playButton.setOnClickListener(v -> toggleEpisode(episode, playButton));
        }
    }

    private void toggleEpisode(PodcastEpisode episode, Button button) {
        if (episode == selectedEpisode && player != null) {
            if (player.getPlayWhenReady()) pausePlayback(); else startSelectedEpisode();
            return;
        }
        resetSelectedButton();
        selectedEpisode = episode;
        selectedPlayButton = button;
        ((MainActivity) requireActivity()).pauseLiveRadioForPodcast();
        ensurePlayer();
        player.setMediaItem(MediaItem.fromUri(episode.getAudioUrl().trim()));
        player.prepare();
        startSelectedEpisode();
    }

    private void startSelectedEpisode() {
        if (player == null || selectedEpisode == null) return;
        ((MainActivity) requireActivity()).pauseLiveRadioForPodcast();
        if (player.getPlaybackState() == Player.STATE_IDLE) {
            player.setMediaItem(MediaItem.fromUri(selectedEpisode.getAudioUrl().trim()));
            player.prepare();
        } else if (player.getPlaybackState() == Player.STATE_ENDED) {
            player.seekTo(0);
        }
        player.play();
        if (selectedPlayButton != null) {
            if (player.getPlaybackState() == Player.STATE_READY) {
                showPauseState();
                status.setVisibility(View.GONE);
            } else {
                showLoadingState();
            }
        }
    }

    /** Called by MainActivity immediately before live radio begins. */
    public void pausePlayback() {
        if (player != null && player.getPlayWhenReady()) player.pause();
        if (selectedPlayButton != null && selectedEpisode != null) {
            showPlayState();
        }
        if (selectedEpisode != null && status != null) {
            status.setVisibility(View.VISIBLE);
            status.setText("Episode paused");
        }
    }

    private void ensurePlayer() {
        if (player != null) return;
        player = new ExoPlayer.Builder(requireContext()).build();
        player.addListener(new Player.Listener() {
            @Override public void onPlaybackStateChanged(int playbackState) {
                if (status == null) return;
                if (playbackState == Player.STATE_BUFFERING) {
                    status.setVisibility(View.VISIBLE);
                    status.setText("Buffering episode...");
                    if (selectedPlayButton != null && selectedEpisode != null) showBufferingState();
                } else if (playbackState == Player.STATE_READY && player.getPlayWhenReady()) {
                    status.setVisibility(View.GONE);
                    showPauseState();
                } else if (playbackState == Player.STATE_ENDED) {
                    resetSelectedButton();
                    status.setVisibility(View.VISIBLE);
                    status.setText("Episode finished");
                }
            }

            @Override public void onPlayerError(@NonNull PlaybackException error) {
                Log.e("KasiePodcast", "Playback failed: code=" + error.getErrorCodeName()
                        + ", message=" + error.getMessage()
                        + ", title=" + (selectedEpisode == null ? "" : selectedEpisode.getTitle())
                        + ", audioUrl=" + (selectedEpisode == null ? "" : selectedEpisode.getAudioUrl()),
                        error);
                resetSelectedButton();
                if (status != null) {
                    status.setVisibility(View.VISIBLE);
                    status.setText("Unable to play this episode. Please try again.");
                }
            }
        });
    }

    private void resetSelectedButton() {
        if (selectedPlayButton != null && selectedEpisode != null) {
            showPlayState();
        }
    }

    private void showPlayState() {
        selectedPlayButton.setText("▶  PLAY EPISODE");
        selectedPlayButton.setContentDescription("Play "
                + nonEmpty(selectedEpisode.getTitle(), "podcast episode"));
    }

    private void showLoadingState() {
        selectedPlayButton.setText("LOADING...");
        selectedPlayButton.setContentDescription("Loading "
                + nonEmpty(selectedEpisode.getTitle(), "podcast episode"));
        status.setVisibility(View.VISIBLE);
        status.setText("Loading episode...");
    }

    private void showPauseState() {
        selectedPlayButton.setText("Ⅱ  PAUSE");
        selectedPlayButton.setContentDescription("Pause "
                + nonEmpty(selectedEpisode.getTitle(), "podcast episode"));
    }

    private void showBufferingState() {
        selectedPlayButton.setText("BUFFERING...");
        selectedPlayButton.setContentDescription("Buffering "
                + nonEmpty(selectedEpisode.getTitle(), "podcast episode"));
    }

    private boolean hasPlayableUrl(PodcastEpisode episode) {
        String url = episode.getAudioUrl();
        if (url == null || url.trim().isEmpty()) return false;
        Uri uri = Uri.parse(url.trim());
        return ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                && uri.getHost() != null;
    }

    private String nonEmpty(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    private String blankToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    @Override public void onDestroyView() {
        if (podcastsCall != null) podcastsCall.cancel();
        podcastsCall = null;
        if (player != null) {
            player.release();
            player = null;
        }
        selectedEpisode = null;
        selectedPlayButton = null;
        status = null;
        list = null;
        super.onDestroyView();
    }
}
