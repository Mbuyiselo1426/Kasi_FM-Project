package com.kasiefm;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageButton;
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
import com.kasiefm.api.FeaturedContent;
import com.kasiefm.api.KasieApiService;
import com.kasiefm.api.RadioShow;
import com.kasiefm.api.RetrofitClient;
import com.kasiefm.api.Show;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Home schedule UI. The API's isCurrent flag is the sole current-show authority. */
public class HomeFragment extends Fragment {
    private static final int REQUEST_NOTIFICATION_PERMISSION = 401;
    private static final long REFRESH_GRACE_MILLIS = 8_000L;
    private static final long REFRESH_RETRY_MILLIS = 60_000L;
    private static final int SHOW_PREVIEW_COUNT = 4;
    private static final int FEATURED_PREVIEW_COUNT = 3;
    private TextView showTitle, showTime, currentStatus;
    private TextView fullScheduleAction;
    private View currentDot, currentBadge, scheduleContainer;
    private LinearLayout scheduleList;
    private LinearLayout showsList;
    private TextView showsStatus;
    private TextView featuredStatus;
    private View featuredScroll;
    private LinearLayout featuredList;
    private Call<List<Show>> scheduleCall;
    private Call<List<RadioShow>> showsCall;
    private Call<List<FeaturedContent>> featuredCall;
    private boolean hasScheduleResponse;
    private Show pendingReminderShow;
    private ImageButton pendingReminderButton;
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
        view.findViewById(R.id.view_all_shows_action).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).openAllShows();
            }
        });
        loadShows();
        featuredStatus = view.findViewById(R.id.featured_status);
        featuredScroll = view.findViewById(R.id.featured_scroll);
        featuredList = view.findViewById(R.id.featured_list);
        loadFeatured();
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

    private void loadFeatured() {
        if (!isAdded() || getView() == null) return;
        if (featuredCall != null) featuredCall.cancel();
        renderFeaturedLoading();
        featuredCall = RetrofitClient.getInstance().create(KasieApiService.class).getFeatured();
        featuredCall.enqueue(new Callback<List<FeaturedContent>>() {
            @Override public void onResponse(Call<List<FeaturedContent>> call,
                                             Response<List<FeaturedContent>> response) {
                if (!canRenderFeatured(call)) return;
                featuredCall = null;
                if (!response.isSuccessful() || response.body() == null) {
                    renderFeaturedError();
                    return;
                }
                renderFeatured(response.body());
            }

            @Override public void onFailure(Call<List<FeaturedContent>> call, Throwable error) {
                if (!canRenderFeatured(call) || call.isCanceled()) return;
                featuredCall = null;
                renderFeaturedError();
            }
        });
    }

    private boolean canRenderFeatured(Call<List<FeaturedContent>> call) {
        return call == featuredCall && isAdded() && getView() != null;
    }

    private void renderFeaturedLoading() {
        featuredList.removeAllViews();
        featuredScroll.setVisibility(View.GONE);
        featuredStatus.setText("Loading featured content...");
        featuredStatus.setVisibility(View.VISIBLE);
    }

    private void renderFeatured(List<FeaturedContent> content) {
        featuredList.removeAllViews();
        int rendered = 0;
        for (FeaturedContent item : content) {
            if (item == null) continue;
            addFeaturedCard(item);
            if (++rendered == FEATURED_PREVIEW_COUNT) break;
        }
        if (rendered == 0) {
            renderFeaturedEmpty();
            return;
        }
        featuredStatus.setVisibility(View.GONE);
        featuredScroll.setVisibility(View.VISIBLE);
    }

    private void renderFeaturedEmpty() {
        featuredList.removeAllViews();
        featuredScroll.setVisibility(View.GONE);
        featuredStatus.setText("No featured content available right now.");
        featuredStatus.setVisibility(View.VISIBLE);
    }

    private void renderFeaturedError() {
        featuredList.removeAllViews();
        featuredScroll.setVisibility(View.GONE);
        featuredStatus.setText("Featured content is unavailable right now.");
        featuredStatus.setVisibility(View.VISIBLE);
    }

    private void addFeaturedCard(FeaturedContent content) {
        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_dark_card);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(dp(220), dp(320));
        cardParams.setMarginEnd(dp(12));

        FrameLayout imageArea = new FrameLayout(requireContext());
        imageArea.setBackgroundResource(R.drawable.bg_art_tile);
        TextView fallback = new TextView(requireContext());
        fallback.setText("KASIE FM\nFEATURED");
        fallback.setGravity(android.view.Gravity.CENTER);
        fallback.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        fallback.setTextColor(requireContext().getColor(R.color.kasie_accent));
        fallback.setTextSize(11);
        fallback.setLetterSpacing(0.08f);
        fallback.setTypeface(android.graphics.Typeface.MONOSPACE);
        imageArea.addView(fallback, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        ImageView image = new ImageView(requireContext());
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setContentDescription(nonBlank(content.getTitle(), "Featured content"));
        image.setVisibility(View.GONE);
        imageArea.addView(image, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        card.addView(imageArea, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(124)));

        LinearLayout details = new LinearLayout(requireContext());
        details.setOrientation(LinearLayout.VERTICAL);
        details.setPadding(dp(14), dp(12), dp(14), dp(14));

        TextView type = new TextView(requireContext());
        type.setText(readableFeaturedType(content.getType()));
        type.setTextColor(requireContext().getColor(R.color.kasie_accent));
        type.setTextSize(10);
        type.setLetterSpacing(0.1f);
        type.setTypeface(android.graphics.Typeface.MONOSPACE);
        details.addView(type);

        TextView title = new TextView(requireContext());
        title.setText(nonBlank(content.getTitle(), "Featured content"));
        title.setTextAppearance(R.style.TextAppearance_Kasie_CardTitle);
        title.setMaxLines(2);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        titleParams.topMargin = dp(6);
        details.addView(title, titleParams);

        String summaryText = nonBlank(content.getSummary(), "");
        if (!summaryText.isEmpty()) {
            TextView summary = new TextView(requireContext());
            summary.setText(summaryText);
            summary.setTextColor(requireContext().getColor(R.color.kasie_text_muted));
            summary.setTextSize(12);
            summary.setLineSpacing(0f, 1.2f);
            summary.setMaxLines(3);
            summary.setEllipsize(android.text.TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams summaryParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            summaryParams.topMargin = dp(8);
            details.addView(summary, summaryParams);
        }

        String publishedDate = formatFeaturedDate(content.getPublishedAt());
        if (!publishedDate.isEmpty()) {
            TextView date = new TextView(requireContext());
            date.setText(publishedDate);
            date.setTextColor(requireContext().getColor(R.color.kasie_text_muted));
            date.setTextSize(11);
            date.setTypeface(android.graphics.Typeface.MONOSPACE);
            LinearLayout.LayoutParams dateParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dateParams.topMargin = dp(12);
            details.addView(date, dateParams);
        }

        String externalUrl = nonBlank(content.getExternalUrl(), "");
        if (!externalUrl.isEmpty()) {
            TextView action = new TextView(requireContext());
            action.setText("READ MORE →");
            action.setTextColor(requireContext().getColor(R.color.kasie_accent));
            action.setTextSize(11);
            action.setTypeface(android.graphics.Typeface.MONOSPACE);
            action.setClickable(true);
            action.setContentDescription("Read more: " + nonBlank(content.getTitle(), "Featured content"));
            action.setOnClickListener(v -> openFeaturedLink(externalUrl));
            LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            actionParams.topMargin = dp(12);
            details.addView(action, actionParams);
        }

        card.addView(details, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        loadFeaturedImage(content.getImageUrl(), image, fallback);
        featuredList.addView(card, cardParams);
    }

    private void loadFeaturedImage(String imageUrl, ImageView image, TextView fallback) {
        String url = nonBlank(imageUrl, "");
        if (url.isEmpty()) return;
        // A GONE target is not measured, so Glide cannot determine the size of this
        // dynamically-created artwork view. It has no drawable yet, leaving the
        // fallback visible underneath until the request succeeds or fails.
        image.setVisibility(View.VISIBLE);
        Glide.with(this)
                .load(url)
                .centerCrop()
                .listener(new RequestListener<Drawable>() {
                    @Override public boolean onLoadFailed(@Nullable GlideException error, Object model,
                            Target<Drawable> target, boolean isFirstResource) {
                        image.setVisibility(View.GONE);
                        fallback.setVisibility(View.VISIBLE);
                        return false;
                    }

                    @Override public boolean onResourceReady(Drawable resource, Object model,
                            Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        fallback.setVisibility(View.GONE);
                        image.setVisibility(View.VISIBLE);
                        return false;
                    }
                })
                .into(image);
    }

    static String readableFeaturedType(String type) {
        String normalizedType = nonBlank(type, "").toUpperCase(Locale.US);
        switch (normalizedType) {
            case "COMMUNITY_NEWS": return "Community News";
            case "ANNOUNCEMENT": return "Announcement";
            case "EVENT": return "Event";
            case "INTERVIEW": return "Interview";
            case "FEATURED_SHOW": return "Featured Show";
            default: return "Featured";
        }
    }

    private void openFeaturedLink(String externalUrl) {
        Uri uri;
        try {
            uri = Uri.parse(externalUrl);
        } catch (Exception error) {
            showFeaturedLinkError();
            return;
        }
        String scheme = uri.getScheme();
        if (scheme == null || uri.getHost() == null
                || !("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))) {
            showFeaturedLinkError();
            return;
        }
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (ActivityNotFoundException error) {
            if (isAdded()) Toast.makeText(requireContext(), "No app is available to open this link.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void showFeaturedLinkError() {
        if (isAdded()) Toast.makeText(requireContext(), "This link is unavailable.",
                Toast.LENGTH_SHORT).show();
    }

    static String formatFeaturedDate(String value) {
        String dateValue = nonBlank(value, "");
        if (dateValue.length() < 10) return "";
        try {
            Date date = new SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    .parse(dateValue.substring(0, 10));
            return date == null ? "" : new SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                    .format(date);
        } catch (ParseException error) {
            return "";
        }
    }

    static String nonBlank(String value, String fallback) {
        if (value == null || value.trim().isEmpty() || "null".equalsIgnoreCase(value.trim())) return fallback;
        return value.trim();
    }

    private void addShowCard(RadioShow show) {
        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_dark_card);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(dp(180), dp(140));
        cardParams.setMarginEnd(dp(12));

        TextView label = new TextView(requireContext());
        label.setText("PROGRAMME");
        label.setTextColor(requireContext().getColor(R.color.kasie_accent));
        label.setTextSize(10);
        label.setLetterSpacing(0.1f);
        label.setTypeface(android.graphics.Typeface.MONOSPACE);
        card.addView(label);

        TextView name = new TextView(requireContext());
        name.setText(ShowPreviewFormatter.showName(show));
        name.setTextAppearance(R.style.TextAppearance_Kasie_RowTitle);
        name.setMaxLines(2);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        nameParams.topMargin = dp(8);
        card.addView(name, nameParams);

        String presenter = ShowPreviewFormatter.presenterText(show);
        if (!presenter.isEmpty()) {
            TextView presenterView = new TextView(requireContext());
            presenterView.setText("PRESENTER · " + presenter);
            presenterView.setTextColor(Color.LTGRAY);
            presenterView.setTextSize(12);
            presenterView.setMaxLines(1);
            presenterView.setEllipsize(android.text.TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams presenterParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            presenterParams.topMargin = dp(8);
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
        LinearLayout row = new LinearLayout(requireContext());
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dp(15), dp(8), dp(8), dp(8));
        if (show.isCurrent()) row.setBackgroundResource(R.drawable.bg_schedule_now);

        TextView scheduleText = new TextView(requireContext());
        scheduleText.setText(formatTime(show) + "\n" + safeName(show));
        scheduleText.setTextColor(Color.WHITE);
        scheduleText.setTextSize(14);
        row.addView(scheduleText, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        ImageButton reminderButton = new ImageButton(requireContext());
        reminderButton.setMinimumWidth(dp(48));
        reminderButton.setMinimumHeight(dp(48));
        reminderButton.setContentDescription("Set reminder for " + safeName(show));
        reminderButton.setBackgroundColor(Color.TRANSPARENT);
        row.addView(reminderButton, new LinearLayout.LayoutParams(dp(48), dp(48)));
        renderReminderBell(reminderButton, show);
        reminderButton.setOnClickListener(v -> onReminderClicked(show, reminderButton));

        scheduleList.addView(row);
    }

    private void onReminderClicked(Show show, ImageButton button) {
        ShowReminderScheduler scheduler = new ShowReminderScheduler(requireContext());
        if (scheduler.isReminderSet(show)) {
            scheduler.cancel(show);
            renderReminderBell(button, show);
            Toast.makeText(requireContext(), "Show reminder cancelled", Toast.LENGTH_SHORT).show();
            return;
        }
        if (ShowReminderTime.calculateTriggerMillis(show, System.currentTimeMillis()) < 0L) {
            Toast.makeText(requireContext(), "This airing can no longer be reminded.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && requireContext().checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            pendingReminderShow = show;
            pendingReminderButton = button;
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    REQUEST_NOTIFICATION_PERMISSION);
            return;
        }
        scheduleReminder(show, button);
    }

    private void scheduleReminder(Show show, ImageButton button) {
        ShowReminderScheduler scheduler = new ShowReminderScheduler(requireContext());
        if (!scheduler.schedule(show, System.currentTimeMillis())) {
            Toast.makeText(requireContext(), "This airing can no longer be reminded.", Toast.LENGTH_SHORT).show();
            return;
        }
        renderReminderBell(button, show);
        Toast.makeText(requireContext(), "Show reminder set for 5 minutes before", Toast.LENGTH_SHORT).show();
    }

    private void renderReminderBell(ImageButton button, Show show) {
        boolean set = new ShowReminderScheduler(requireContext()).isReminderSet(show);
        button.setImageResource(set ? R.drawable.ic_reminder_set : R.drawable.ic_reminder_bell);
        button.setColorFilter(requireContext().getColor(set ? R.color.kasie_accent : R.color.kasie_cyan));
        button.setContentDescription((set ? "Cancel reminder for " : "Set reminder for ")
                + safeName(show));
    }

    @Override public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_NOTIFICATION_PERMISSION) return;
        boolean granted = grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED;
        if (granted && pendingReminderShow != null && pendingReminderButton != null) {
            scheduleReminder(pendingReminderShow, pendingReminderButton);
        } else if (!granted && isAdded()) {
            Toast.makeText(requireContext(), "Notification permission is needed for show reminders.",
                    Toast.LENGTH_SHORT).show();
        }
        pendingReminderShow = null;
        pendingReminderButton = null;
    }
    @Override public void onDestroyView() {
        stopRefreshing();
        if (showsCall != null) showsCall.cancel();
        showsCall = null;
        if (featuredCall != null) featuredCall.cancel();
        featuredCall = null;
        showTitle = null; showTime = null; currentStatus = null; currentDot = null;
        currentBadge = null; scheduleList = null; scheduleContainer = null; fullScheduleAction = null;
        showsList = null; showsStatus = null;
        featuredStatus = null; featuredScroll = null; featuredList = null;
        pendingReminderShow = null; pendingReminderButton = null;
        hasScheduleResponse = false;
        super.onDestroyView();
    }
}
