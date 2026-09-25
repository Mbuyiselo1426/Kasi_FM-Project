package com.kasiefm;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.kasiefm.api.KasieApiService;
import com.kasiefm.api.RadioShow;
import com.kasiefm.api.RetrofitClient;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Text-first listing for the complete, server-ordered GET /api/shows response. */
public class AllShowsFragment extends Fragment {
    private LinearLayout showsList;
    private TextView status;
    private Call<List<RadioShow>> showsCall;
    private OnBackPressedCallback backCallback;

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container, @Nullable Bundle state) {
        return inflater.inflate(R.layout.fragment_all_shows, container, false);
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        showsList = view.findViewById(R.id.all_shows_list);
        status = view.findViewById(R.id.all_shows_status);
        view.findViewById(R.id.all_shows_back).setOnClickListener(v -> returnHome());
        backCallback = new OnBackPressedCallback(!isHidden()) {
            @Override public void handleOnBackPressed() {
                returnHome();
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), backCallback);
        loadShows();
    }

    @Override public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (backCallback != null) backCallback.setEnabled(!hidden);
    }

    private void returnHome() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).returnHomeFromAllShows();
        }
    }

    private void loadShows() {
        if (!isAdded() || getView() == null) return;
        if (showsCall != null) showsCall.cancel();
        renderLoading();
        showsCall = RetrofitClient.getInstance().create(KasieApiService.class).getShows();
        showsCall.enqueue(new Callback<List<RadioShow>>() {
            @Override public void onResponse(Call<List<RadioShow>> call,
                    Response<List<RadioShow>> response) {
                if (!canRender(call)) return;
                showsCall = null;
                if (!response.isSuccessful() || response.body() == null) {
                    renderError();
                    return;
                }
                renderShows(response.body());
            }

            @Override public void onFailure(Call<List<RadioShow>> call, Throwable error) {
                if (!canRender(call) || call.isCanceled()) return;
                showsCall = null;
                renderError();
            }
        });
    }

    private boolean canRender(Call<List<RadioShow>> call) {
        return call == showsCall && isAdded() && getView() != null;
    }

    private void renderLoading() {
        showsList.removeAllViews();
        status.setText("Loading shows...");
        status.setVisibility(View.VISIBLE);
        status.setOnClickListener(null);
    }

    private void renderShows(List<RadioShow> shows) {
        showsList.removeAllViews();
        int rendered = 0;
        // Iterate directly: the backend defines the display order.
        for (RadioShow show : shows) {
            if (show == null) continue;
            addShowCard(show);
            rendered++;
        }
        if (rendered == 0) {
            status.setText("No shows available right now.");
            status.setVisibility(View.VISIBLE);
        } else {
            status.setVisibility(View.GONE);
        }
        status.setOnClickListener(null);
    }

    private void renderError() {
        showsList.removeAllViews();
        status.setText("Unable to load shows. Tap to retry.");
        status.setVisibility(View.VISIBLE);
        status.setOnClickListener(v -> loadShows());
    }

    private void addShowCard(RadioShow show) {
        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_dark_card);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));

        TextView label = new TextView(requireContext());
        label.setText("PROGRAMME");
        label.setTextColor(requireContext().getColor(R.color.kasie_accent));
        label.setTextSize(10);
        label.setLetterSpacing(0.1f);
        label.setTypeface(android.graphics.Typeface.MONOSPACE);
        card.addView(label);

        TextView name = new TextView(requireContext());
        name.setText(ShowPreviewFormatter.showName(show));
        name.setTextAppearance(R.style.TextAppearance_Kasie_CardTitle);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        nameParams.topMargin = dp(7);
        card.addView(name, nameParams);

        String presenter = ShowPreviewFormatter.presenterText(show);
        if (!presenter.isEmpty()) {
            TextView presenterView = new TextView(requireContext());
            presenterView.setText("PRESENTER\n" + presenter);
            presenterView.setTextColor(Color.LTGRAY);
            presenterView.setTextSize(13);
            presenterView.setLineSpacing(0f, 1.2f);
            LinearLayout.LayoutParams presenterParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            presenterParams.topMargin = dp(10);
            card.addView(presenterView, presenterParams);
        }

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.bottomMargin = dp(12);
        showsList.addView(card, cardParams);
    }

    private int dp(int value) {
        return Math.round(value * requireContext().getResources().getDisplayMetrics().density);
    }

    @Override public void onDestroyView() {
        if (showsCall != null) showsCall.cancel();
        showsCall = null;
        showsList = null;
        status = null;
        backCallback = null;
        super.onDestroyView();
    }
}
