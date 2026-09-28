package com.kasiefm;

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

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Details for a radio show, loaded from GET /api/shows/{id}. */
public class ShowDetailsFragment extends Fragment {
    private static final String ARG_SHOW_ID = "show_id";

    private long showId;
    private TextView status;
    private LinearLayout content;
    private TextView showName;
    private LinearLayout presenterSection;
    private TextView presenter;
    private LinearLayout aboutSection;
    private TextView description;
    private Call<RadioShow> showCall;
    private OnBackPressedCallback backCallback;

    public static ShowDetailsFragment newInstance(long showId) {
        ShowDetailsFragment fragment = new ShowDetailsFragment();
        Bundle arguments = new Bundle();
        arguments.putLong(ARG_SHOW_ID, showId);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container, @Nullable Bundle state) {
        return inflater.inflate(R.layout.fragment_show_details, container, false);
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        Bundle arguments = getArguments();
        showId = arguments == null ? 0L : arguments.getLong(ARG_SHOW_ID, 0L);
        status = view.findViewById(R.id.show_details_status);
        content = view.findViewById(R.id.show_details_content);
        showName = view.findViewById(R.id.show_details_name);
        presenterSection = view.findViewById(R.id.show_details_presenter_section);
        presenter = view.findViewById(R.id.show_details_presenter);
        aboutSection = view.findViewById(R.id.show_details_about_section);
        description = view.findViewById(R.id.show_details_description);
        view.findViewById(R.id.show_details_back).setOnClickListener(v -> returnToAllShows());
        backCallback = new OnBackPressedCallback(!isHidden()) {
            @Override public void handleOnBackPressed() {
                returnToAllShows();
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), backCallback);
        loadShow();
    }

    @Override public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (backCallback != null) backCallback.setEnabled(!hidden);
    }

    private void loadShow() {
        if (!isAdded() || getView() == null) return;
        if (showId <= 0L) {
            renderError("Show unavailable.");
            return;
        }
        if (showCall != null) showCall.cancel();
        renderLoading();
        showCall = RetrofitClient.getInstance().create(KasieApiService.class).getShow(showId);
        showCall.enqueue(new Callback<RadioShow>() {
            @Override public void onResponse(Call<RadioShow> call, Response<RadioShow> response) {
                if (!canRender(call)) return;
                showCall = null;
                if (!response.isSuccessful() || response.body() == null) {
                    renderError(response.code() == 404 ? "Show not found." : "Show unavailable. Tap to retry.");
                    return;
                }
                renderShow(response.body());
            }

            @Override public void onFailure(Call<RadioShow> call, Throwable error) {
                if (!canRender(call) || call.isCanceled()) return;
                showCall = null;
                renderError("Unable to load show. Tap to retry.");
            }
        });
    }

    private boolean canRender(Call<RadioShow> call) {
        return call == showCall && isAdded() && getView() != null && !isHidden();
    }

    private void renderLoading() {
        content.setVisibility(View.GONE);
        status.setText("Loading show...");
        status.setVisibility(View.VISIBLE);
        status.setOnClickListener(null);
    }

    private void renderShow(RadioShow show) {
        showName.setText(nonBlank(show.getName(), "Kasie FM programme"));
        String presenterText = joinAvailable(show.getPresenterName(), show.getPresenterNickname());
        presenterSection.setVisibility(presenterText.isEmpty() ? View.GONE : View.VISIBLE);
        presenter.setText(presenterText);
        String descriptionText = nonBlank(show.getDescription(), "");
        aboutSection.setVisibility(descriptionText.isEmpty() ? View.GONE : View.VISIBLE);
        description.setText(descriptionText);
        status.setVisibility(View.GONE);
        status.setOnClickListener(null);
        content.setVisibility(View.VISIBLE);
    }

    private void renderError(String message) {
        content.setVisibility(View.GONE);
        status.setText(message);
        status.setVisibility(View.VISIBLE);
        status.setOnClickListener(v -> loadShow());
    }

    private String joinAvailable(String name, String nickname) {
        String cleanName = nonBlank(name, "");
        String cleanNickname = nonBlank(nickname, "");
        if (cleanName.isEmpty()) return cleanNickname;
        if (cleanNickname.isEmpty()) return cleanName;
        return cleanName + " · " + cleanNickname;
    }

    private String nonBlank(String value, String fallback) {
        if (value == null) return fallback;
        String trimmed = value.trim();
        return trimmed.isEmpty() || "null".equalsIgnoreCase(trimmed) ? fallback : trimmed;
    }

    private void returnToAllShows() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).returnToAllShowsFromDetails();
        }
    }

    @Override public void onDestroyView() {
        if (showCall != null) showCall.cancel();
        showCall = null;
        status = null;
        content = null;
        showName = null;
        presenterSection = null;
        presenter = null;
        aboutSection = null;
        description = null;
        backCallback = null;
        super.onDestroyView();
    }
}
