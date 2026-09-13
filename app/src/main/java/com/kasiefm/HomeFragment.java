package com.kasiefm;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
        loadSchedule();
    }

    private void loadSchedule() {
        showTitle.setText("Loading schedule...");
        showTime.setText("");

        KasieApiService api = RetrofitClient.getInstance().create(KasieApiService.class);

        api.getSchedule().enqueue(new Callback<List<Show>>() {
            @Override
            public void onResponse(Call<List<Show>> call, Response<List<Show>> response) {
                if (!isAdded()) {
                    return; // fragment detached (e.g. user switched tabs) before response arrived
                }
                if (response.isSuccessful() && response.body() != null) {
                    Show current = ScheduleSelector.findCurrentShow(response.body());
                    if (current == null) {
                        showTitle.setText("No current show");
                        showTime.setText("No scheduled show right now");
                        return;
                    }
                    showTitle.setText(current.getName());
                    showTime.setText(current.getStartTime() + " - " + current.getEndTime());
                } else {
                    showTitle.setText("Schedule unavailable");
                    showTime.setText("");
                }
            }

            @Override
            public void onFailure(Call<List<Show>> call, Throwable t) {
                if (!isAdded()) {
                    return;
                }
                showTitle.setText("Unable to load schedule");
                showTime.setText("Check your connection");
            }
        });
    }
}
