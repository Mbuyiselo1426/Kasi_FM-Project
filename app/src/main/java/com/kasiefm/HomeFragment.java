package com.kasiefm;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.kasiefm.api.KasieApiService;
import com.kasiefm.api.RadioShow;
import com.kasiefm.api.RetrofitClient;
import com.kasiefm.api.Show;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Home schedule UI. The API's isCurrent flag is the sole current-show authority. */
public class HomeFragment extends Fragment {
    private static final String TAG = "HomeFragment";
    private static final String ASIMDUMISE_NAME = "Asimdumise";
    private static final String ASIMDUMISE_CLOUDINARY_URL =
            "https://res.cloudinary.com/dtbpwk3j/image/upload/v1789736933/"
                    + "kasiefm/shows/asidmumise.webp";
    private static final long REFRESH_GRACE_MILLIS = 8_000L;
    private static final long REFRESH_RETRY_MILLIS = 60_000L;
    private static final int SHOW_PREVIEW_COUNT = 4;
    private TextView showTitle, showTime, currentStatus;
    private TextView fullScheduleAction;
    private View currentDot, currentBadge, scheduleContainer;
    private LinearLayout scheduleList;
    private LinearLayout showsList;
    private TextView showsStatus;
    private Call<List<Show>> scheduleCall;
    private Call<List<RadioShow>> showsCall;
    private boolean hasScheduleResponse;
    private final ScheduleExpansionState scheduleExpansionState = new ScheduleExpansionState();
    private final Handler refreshHandler = new Handler(Looper.getMainLooper());
    private final Runnable refreshRunnable = () -> loadSchedule(false);

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container, @Nullable Bundle state) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        showTitle = view.findViewById(R.id.show_title);
        showTime = view.findViewById(R.id.show_time);
        currentStatus = view.findViewById(R.id.current_show_status);
        currentDot = view.findViewById(R.id.current_show_dot);
        currentBadge = view.findViewById(R.id.current_show_on_air_badge);
        scheduleList = view.findViewById(R.id.schedule_list);
        scheduleContainer = view.findViewById(R.id.schedule_container);
        fullScheduleAction = view.findViewById(R.id.full_schedule_action);
        fullScheduleAction.setOnClickListener(v -> toggleSchedule());
        renderScheduleExpansion();
        showsList = view.findViewById(R.id.shows_list);
        showsStatus = view.findViewById(R.id.shows_status);
        loadShows();
    }

    @Override public void onResume() {
        super.onResume();
        if (!isHidden()) loadSchedule(!hasScheduleResponse);
    }
    @Override public void onPause() { stopRefreshing(); super.onPause(); }
    @Override public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (hidden) stopRefreshing(); else if (isResumed()) loadSchedule(!hasScheduleResponse);
    }

    private void toggleSchedule() {
        scheduleExpansionState.toggle();
        renderScheduleExpansion();
    }

    private void renderScheduleExpansion() {
        boolean expanded = scheduleExpansionState.isExpanded();
        scheduleContainer.setVisibility(expanded ? View.VISIBLE : View.GONE);
        fullScheduleAction.setText(scheduleExpansionState.getActionText());
    }

    /** Uses the same request/render path for initial and automatic refreshes. */
    private void loadSchedule(boolean showLoadingState) {
        if (!isHomeVisible()) return;
        stopRefreshTimer();
        if (scheduleCall != null) scheduleCall.cancel();
        if (showLoadingState) renderLoadingState();
        scheduleCall = RetrofitClient.getInstance().create(KasieApiService.class).getSchedule();
        scheduleCall.enqueue(new Callback<List<Show>>() {
            @Override public void onResponse(Call<List<Show>> call, Response<List<Show>> response) {
                if (!canRender(call)) return;
                scheduleCall = null;
                if (!response.isSuccessful() || response.body() == null) {
                    if (showLoadingState) renderHttpErrorState();
                    scheduleRetry();
                } else {
                    hasScheduleResponse = true;
                    renderSchedule(response.body());
                }
            }
            @Override public void onFailure(Call<List<Show>> call, Throwable error) {
                if (!canRender(call) || call.isCanceled()) return;
                scheduleCall = null;
                if (showLoadingState) renderNetworkErrorState();
                scheduleRetry();
            }
        });
    }

    private boolean canRender(Call<List<Show>> call) { return call == scheduleCall && isHomeVisible(); }
    private boolean isHomeVisible() { return isAdded() && getView() != null && !isHidden() && isResumed(); }

    private void renderLoadingState() {
        updateCurrentShowCard(null, "Loading schedule...", "");
        updateHero(null, "Loading schedule...", "");
        scheduleList.removeAllViews(); addScheduleMessage("Loading schedule...");
    }

    private void renderSchedule(List<Show> schedule) {
        scheduleList.removeAllViews();
        if (schedule.isEmpty()) {
            updateCurrentShowCard(null, "No schedule available today", "");
            updateHero(null, "No schedule available today", "");
            addScheduleMessage("No schedule available today");
            scheduleRetry();
            return;
        }
        Show currentShow = ScheduleSelector.findCurrentShow(schedule);
        if (currentShow == null) {
            updateCurrentShowCard(null, "Schedule unavailable", "No current show right now");
            updateHero(null, "Schedule unavailable", "No current show right now");
        } else {
            updateCurrentShowCard(currentShow, "", ""); updateHero(currentShow, "", "");
        }
        for (Show show : ScheduleSelector.rowsInResponseOrder(schedule)) addScheduleRow(show);
        if (currentShow != null) scheduleNextRefresh(currentShow);
        else scheduleRetry();
    }

    private void updateHero(@Nullable Show show, String title, String time) {
        if (getActivity() instanceof MainActivity)
            ((MainActivity) getActivity()).renderScheduleHero(show, title, time);
    }

    private void updateCurrentShowCard(@Nullable Show show, String title, String time) {
        boolean onAir = show != null;
        currentStatus.setVisibility(onAir ? View.VISIBLE : View.GONE);
        currentDot.setVisibility(onAir ? View.VISIBLE : View.GONE);
        currentBadge.setVisibility(onAir ? View.VISIBLE : View.GONE);
        showTitle.setText(onAir ? safeName(show) : title);
        showTime.setText(onAir ? formatTime(show) : time);
    }

    private void renderHttpErrorState() {
        updateCurrentShowCard(null, "Schedule unavailable", ""); updateHero(null, "Schedule unavailable", "");
        scheduleList.removeAllViews(); addScheduleMessage("Schedule unavailable");
    }
    private void renderNetworkErrorState() {
        updateCurrentShowCard(null, "Unable to load schedule", "Check your connection");
        updateHero(null, "Unable to load schedule", "Check your connection");
        scheduleList.removeAllViews(); addScheduleMessage("Unable to load schedule\nCheck your connection");
    }

    private void loadShows() {
        if (!isAdded() || getView() == null) return;
        if (showsCall != null) showsCall.cancel();
        renderShowsLoading();
        showsCall = RetrofitClient.getInstance().create(KasieApiService.class).getShows();
        showsCall.enqueue(new Callback<List<RadioShow>>() {
            @Override public void onResponse(Call<List<RadioShow>> call,
                                             Response<List<RadioShow>> response) {
                if (!canRenderShows(call)) return;
                showsCall = null;
                if (!response.isSuccessful() || response.body() == null) {
                    renderShowsError();
                    return;
                }
                renderShows(response.body());
            }

            @Override public void onFailure(Call<List<RadioShow>> call, Throwable error) {
                if (!canRenderShows(call) || call.isCanceled()) return;
                showsCall = null;
                renderShowsError();
            }
        });
    }

    private boolean canRenderShows(Call<List<RadioShow>> call) {
        return call == showsCall && isAdded() && getView() != null;
    }

    private void renderShowsLoading() {
        showsList.removeAllViews();
        showsStatus.setText("Loading shows...");
        showsStatus.setVisibility(View.VISIBLE);
        showsStatus.setOnClickListener(null);
    }

    private void renderShows(List<RadioShow> shows) {
        showsList.removeAllViews();
        int rendered = 0;
        for (RadioShow show : shows) {
            if (show == null) continue;
            addShowCard(show);
            if (++rendered == SHOW_PREVIEW_COUNT) break;
        }
        if (rendered == 0) {
            showsStatus.setText("No shows available right now.");
            showsStatus.setVisibility(View.VISIBLE);
        } else {
            showsStatus.setVisibility(View.GONE);
        }
        showsStatus.setOnClickListener(null);
    }

    private void renderShowsError() {
        showsList.removeAllViews();
        showsStatus.setText("Unable to load shows. Tap to retry.");
        showsStatus.setVisibility(View.VISIBLE);
        showsStatus.setOnClickListener(v -> loadShows());
    }

    private void addShowCard(RadioShow show) {
        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_dark_card);
        card.setPadding(dp(10), dp(10), dp(10), dp(12));
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(dp(180),
                ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.setMarginEnd(dp(12));

        FrameLayout artworkFrame = new FrameLayout(requireContext());
        artworkFrame.setBackgroundResource(R.drawable.bg_art_tile);
        ImageView fallbackArtwork = new ImageView(requireContext());
        fallbackArtwork.setImageResource(R.drawable.ic_mic);
        artworkFrame.addView(fallbackArtwork, new FrameLayout.LayoutParams(dp(30), dp(30),
                android.view.Gravity.CENTER));
        ImageView artwork = new ImageView(requireContext());
        artwork.setScaleType(ImageView.ScaleType.CENTER_CROP);
        artwork.setContentDescription(ShowPreviewFormatter.showName(show) + " artwork");
        artwork.setVisibility(View.GONE);
        artworkFrame.addView(artwork, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(108)));
        card.addView(artworkFrame, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(108)));

        String imageUrl = show.getImageUrl() == null ? null : show.getImageUrl().trim();
        String showName = ShowPreviewFormatter.showName(show);
        boolean isAsimdumise = ASIMDUMISE_NAME.equalsIgnoreCase(showName.trim());
        if (isAsimdumise) {
            if (imageUrl == null || imageUrl.isEmpty()) {
                Toast.makeText(artwork.getContext(), "ASIMDUMISE: URL MISSING", Toast.LENGTH_LONG)
                        .show();
            } else if (ASIMDUMISE_CLOUDINARY_URL.equals(imageUrl)) {
                Toast.makeText(artwork.getContext(), "ASIMDUMISE: CLOUDINARY URL RECEIVED",
                        Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(artwork.getContext(), "ASIMDUMISE: OLD URL RECEIVED",
                        Toast.LENGTH_LONG).show();
            }
        }
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Log.d(TAG, "Loading show artwork: show=" + showName + ", url=" + imageUrl);
            Glide.with(this)
                    .load(imageUrl)
                    .centerCrop()
                    .placeholder(new ColorDrawable(Color.TRANSPARENT))
                    .error(new ColorDrawable(Color.TRANSPARENT))
                    .listener(new RequestListener<Drawable>() {
                        @Override public boolean onLoadFailed(@Nullable GlideException error,
                                Object model, Target<Drawable> target, boolean isFirstResource) {
                            artwork.setVisibility(View.GONE);
                            fallbackArtwork.setVisibility(View.VISIBLE);
                            Log.e(TAG, "Show artwork failed: show=" + showName + ", url=" + imageUrl,
                                    error);
                            if (isAsimdumise) {
                                Toast.makeText(artwork.getContext(), "ASIMDUMISE ARTWORK FAILED",
                                        Toast.LENGTH_LONG).show();
                            }
                            return false;
                        }

                        @Override public boolean onResourceReady(Drawable resource, Object model,
                                Target<Drawable> target, DataSource dataSource,
                                boolean isFirstResource) {
                            fallbackArtwork.setVisibility(View.GONE);
                            artwork.setVisibility(View.VISIBLE);
                            Log.d(TAG, "Show artwork loaded: show=" + showName + ", url="
                                    + imageUrl + ", source=" + dataSource);
                            if (isAsimdumise) {
                                Toast.makeText(artwork.getContext(), "ASIMDUMISE ARTWORK LOADED",
                                        Toast.LENGTH_LONG).show();
                            }
                            return false;
                        }
                    })
                    .into(artwork);
        } else {
            Log.w(TAG, "Show artwork URL missing: show=" + ShowPreviewFormatter.showName(show));
        }

        TextView name = new TextView(requireContext());
        name.setText(ShowPreviewFormatter.showName(show));
        name.setTextAppearance(R.style.TextAppearance_Kasie_RowTitle);
        name.setMaxLines(2);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        nameParams.topMargin = dp(10);
        card.addView(name, nameParams);

        String presenter = ShowPreviewFormatter.presenterText(show);
        if (!presenter.isEmpty()) {
            TextView presenterView = new TextView(requireContext());
            presenterView.setText(presenter);
            presenterView.setTextColor(Color.LTGRAY);
            presenterView.setTextSize(12);
            LinearLayout.LayoutParams presenterParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            presenterParams.topMargin = dp(4);
            card.addView(presenterView, presenterParams);
        }
        showsList.addView(card, cardParams);
    }

    private int dp(int value) {
        return Math.round(value * requireContext().getResources().getDisplayMetrics().density);
    }

    private void scheduleNextRefresh(Show currentShow) {
        long delay = ScheduleSelector.millisUntilNextRefresh(currentShow.getEndTime(),
                System.currentTimeMillis(), REFRESH_GRACE_MILLIS);
        scheduleRefresh(delay);
    }
    private void scheduleRetry() {
        scheduleRefresh(REFRESH_RETRY_MILLIS);
    }
    private void scheduleRefresh(long delayMillis) {
        stopRefreshTimer();
        if (delayMillis > 0L && isHomeVisible()) {
            refreshHandler.postDelayed(refreshRunnable, delayMillis);
        }
    }
    private void stopRefreshTimer() { refreshHandler.removeCallbacks(refreshRunnable); }
    private void stopRefreshing() {
        stopRefreshTimer(); if (scheduleCall != null) scheduleCall.cancel(); scheduleCall = null;
    }
    private String safeName(Show show) { return show.getName() == null ? "Current show" : show.getName(); }
    private String formatTime(Show show) {
        String start = show.getStartTime() == null ? "" : show.getStartTime();
        String end = show.getEndTime() == null ? "" : show.getEndTime(); return start + " - " + end;
    }
    private void addScheduleMessage(String message) {
        TextView text = new TextView(requireContext()); text.setText(message); text.setTextColor(Color.LTGRAY);
        text.setPadding(15, 15, 15, 15); scheduleList.addView(text);
    }
    private void addScheduleRow(Show show) {
        if (show == null) return;
        TextView row = new TextView(requireContext());
        row.setText(formatTime(show) + "\n" + safeName(show)); row.setTextColor(Color.WHITE);
        row.setTextSize(14); row.setPadding(15, 15, 15, 15);
        if (show.isCurrent()) row.setBackgroundResource(R.drawable.bg_schedule_now);
        scheduleList.addView(row);
    }
    @Override public void onDestroyView() {
        stopRefreshing();
        if (showsCall != null) showsCall.cancel();
        showsCall = null;
        showTitle = null; showTime = null; currentStatus = null; currentDot = null;
        currentBadge = null; scheduleList = null; scheduleContainer = null; fullScheduleAction = null;
        showsList = null; showsStatus = null;
        hasScheduleResponse = false;
        super.onDestroyView();
    }
}
