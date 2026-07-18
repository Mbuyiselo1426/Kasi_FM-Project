package com.kasiefm;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/**
 * Reusable fragment for tabs that just show static label/title/subtitle
 * content (Featured, Highlights) - avoids two near-duplicate fragment
 * classes for content that only differs by three strings. If either of
 * these tabs grows real behaviour later, split it out into its own
 * fragment at that point rather than overloading this one further.
 */
public class SimpleContentFragment extends Fragment {

    private static final String ARG_LABEL = "label";
    private static final String ARG_TITLE = "title";
    private static final String ARG_SUBTITLE = "subtitle";

    public static SimpleContentFragment newInstance(String label, String title, String subtitle) {
        SimpleContentFragment fragment = new SimpleContentFragment();
        Bundle args = new Bundle();
        args.putString(ARG_LABEL, label);
        args.putString(ARG_TITLE, title);
        args.putString(ARG_SUBTITLE, subtitle);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_simple_content, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle args = getArguments();
        if (args == null) {
            return;
        }
        ((TextView) view.findViewById(R.id.label)).setText(args.getString(ARG_LABEL));
        ((TextView) view.findViewById(R.id.title)).setText(args.getString(ARG_TITLE));
        ((TextView) view.findViewById(R.id.subtitle)).setText(args.getString(ARG_SUBTITLE));
    }
}
