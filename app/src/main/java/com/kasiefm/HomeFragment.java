package com.kasiefm;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.graphics.Color;
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

/**
 * Home tab content: the "CURRENT SHOW" card, populated from the live
 * schedule API. Deliberately separate from the player card / Now Playing
 * card in activity_main.xml - those stay Activity-level and are driven by
 * RadioService's playback state, not by which bottom-nav tab is selected.
 * This fragment used to be inline in MainActivity, sharing (and colliding
 * with) the same views the tab-switcher used - it doesn't anymore.
 */
public class HomeFragment extends Fragment {

    private TextView showTitle;
    private TextView showTime;
    private LinearLayout scheduleList;
    private Call<List<Show>> scheduleCall;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        showTitle = view.findViewById(R.id.show_title);
        showTime = view.findViewById(R.id.show_time);
        scheduleList = view.findViewById(R.id.schedule_list);
        loadSchedule();
    }

    private void loadSchedule() {
        showTitle.setText("Loading schedule...");
        showTime.setText("");
        scheduleList.removeAllViews();
        addScheduleMessage("Loading schedule...");

        KasieApiService api = RetrofitClient.getInstance().create(KasieApiService.class);

        scheduleCall = api.getSchedule();
        scheduleCall.enqueue(new Callback<List<Show>>() {
            @Override
            public void onResponse(Call<List<Show>> call, Response<List<Show>> response) {
                if (!isAdded() || getView() == null) {
                    return; // fragment detached (e.g. user switched tabs) before response arrived
                }
                if (response.isSuccessful() && response.body() != null) {
                    List<Show> schedule = response.body();
                    scheduleList.removeAllViews();
                    if (schedule.isEmpty()) {
                        showTitle.setText("No schedule available today");
                        showTime.setText("");
                        addScheduleMessage("No schedule available today");
                        return;
                    }
                    Show current = ScheduleSelector.findCurrentShow(schedule);
                    if (current == null) {
                        showTitle.setText("Schedule unavailable");
                        showTime.setText("No current show right now");
                    } else {
                        showTitle.setText(current.getName() == null ? "Current show" : current.getName());
                        showTime.setText(formatTime(current));
                    }
                    for (Show show : ScheduleSelector.rowsInResponseOrder(schedule)) {
                        addScheduleRow(show);
                    }
                } else {
                    showTitle.setText("Schedule unavailable");
                    showTime.setText("");
                    scheduleList.removeAllViews();
                    addScheduleMessage("Schedule unavailable");
                }
            }

            @Override
            public void onFailure(Call<List<Show>> call, Throwable t) {
                if (!isAdded() || getView() == null) {
                    return;
                }
                showTitle.setText("Unable to load schedule");
                showTime.setText("Check your connection");
                scheduleList.removeAllViews();
                addScheduleMessage("Unable to load schedule\nCheck your connection");
            }
        });
    }

    private String formatTime(Show show) {
        String start = show.getStartTime() == null ? "" : show.getStartTime();
        String end = show.getEndTime() == null ? "" : show.getEndTime();
        return start + " - " + end;
    }

    private void addScheduleMessage(String message) {
        TextView text = new TextView(requireContext());
        text.setText(message);
        text.setTextColor(Color.LTGRAY);
        text.setPadding(15, 15, 15, 15);
        scheduleList.addView(text);
    }

    private void addScheduleRow(Show show) {
        if (show == null) {
            return;
        }
        TextView row = new TextView(requireContext());
        row.setText(formatTime(show) + "\n" + (show.getName() == null ? "Unnamed show" : show.getName()));
        row.setTextColor(Color.WHITE);
        row.setTextSize(14);
        row.setPadding(15, 15, 15, 15);
        if (show.isCurrent()) row.setBackgroundResource(R.drawable.bg_schedule_now);
        scheduleList.addView(row);
    }

    @Override
    public void onDestroyView() {
        if (scheduleCall != null) scheduleCall.cancel();
        scheduleCall = null;
        showTitle = null;
        showTime = null;
        scheduleList = null;
        super.onDestroyView();
    }
}
