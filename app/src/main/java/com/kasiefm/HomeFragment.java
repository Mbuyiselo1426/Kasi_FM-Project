package com.kasiefm;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.kasiefm.api.KasieApiService;
import com.kasiefm.api.RetrofitClient;
import com.kasiefm.api.Show;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Home schedule UI. The API's isCurrent flag is the sole current-show authority. */
public class HomeFragment extends Fragment {
    private static final long REFRESH_GRACE_MILLIS = 8_000L;
    private TextView showTitle, showTime, currentStatus;
    private View currentDot, currentBadge, scheduleSectionTitle;
    private LinearLayout scheduleList;
    private Call<List<Show>> scheduleCall;
    private final Handler refreshHandler = new Handler(Looper.getMainLooper());
    private final Runnable refreshRunnable = this::loadSchedule;

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
        scheduleSectionTitle = view.findViewById(R.id.schedule_section_title);
        view.findViewById(R.id.full_schedule_action).setOnClickListener(v -> focusSchedule());
    }

    @Override public void onResume() { super.onResume(); if (!isHidden()) loadSchedule(); }
    @Override public void onPause() { stopRefreshing(); super.onPause(); }
    @Override public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (hidden) stopRefreshing(); else if (isResumed()) loadSchedule();
    }

    private void focusSchedule() {
        if (scheduleSectionTitle == null) return;
        scheduleSectionTitle.requestFocus();
        scheduleSectionTitle.post(() -> scheduleSectionTitle.requestRectangleOnScreen(
                new android.graphics.Rect(0, 0, scheduleSectionTitle.getWidth(), scheduleSectionTitle.getHeight()), true));
    }

    private void loadSchedule() {
        if (!isHomeVisible()) return;
        stopRefreshTimer();
        if (scheduleCall != null) scheduleCall.cancel();
        renderLoadingState();
        scheduleCall = RetrofitClient.getInstance().create(KasieApiService.class).getSchedule();
        scheduleCall.enqueue(new Callback<List<Show>>() {
            @Override public void onResponse(Call<List<Show>> call, Response<List<Show>> response) {
                if (!canRender(call)) return;
                scheduleCall = null;
                if (!response.isSuccessful() || response.body() == null) renderHttpErrorState();
                else renderSchedule(response.body());
            }
            @Override public void onFailure(Call<List<Show>> call, Throwable error) {
                if (!canRender(call) || call.isCanceled()) return;
                scheduleCall = null;
                renderNetworkErrorState();
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
            addScheduleMessage("No schedule available today"); return;
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

    private void scheduleNextRefresh(Show currentShow) {
        long delay = ScheduleSelector.millisUntilNextRefresh(currentShow.getEndTime(),
                System.currentTimeMillis(), REFRESH_GRACE_MILLIS);
        if (delay > 0L && isHomeVisible()) refreshHandler.postDelayed(refreshRunnable, delay);
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
        stopRefreshing(); showTitle = null; showTime = null; currentStatus = null; currentDot = null;
        currentBadge = null; scheduleList = null; scheduleSectionTitle = null; super.onDestroyView();
    }
}
