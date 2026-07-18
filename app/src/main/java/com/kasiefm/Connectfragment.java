package com.kasiefm;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.kasiefm.model.StudioMessage;

import java.util.UUID;

/**
 * Phase 1 of the CONNECT module: listener submits a request (song, prayer,
 * shout-out, announcement, other) and sends it via WhatsApp, SMS, or a
 * direct call - no backend involved yet. The StudioMessage model is built
 * either way, so the shape is already correct for Phase 2's
 * POST /api/stations/{stationId}/messages once that exists.
 *
 * TODO(kasie-fm): STUDIO_PHONE_NUMBER below is a placeholder. Do not
 * replace it with Kasie FM's real number until the station has been
 * contacted and has agreed to this feature - see journey log, "Kasie FM
 * reality check". Wiring a real, unsuspecting business's number into a
 * feature that texts/calls/WhatsApps them is exactly the kind of thing
 * that needs their sign-off first, not just their public branding.
 */
public class ConnectFragment extends Fragment {

    // TODO(kasie-fm): placeholder - replace only once Kasie FM has agreed to this feature.
    private static final String STUDIO_PHONE_NUMBER = "+27000000000";

    private ChipGroup requestTypeGroup;
    private TextInputLayout nameInputLayout;
    private TextInputEditText nameInput;
    private View songFieldsContainer;
    private TextInputLayout songTitleInputLayout;
    private TextInputEditText songTitleInput;
    private TextInputEditText artistInput;
    private TextInputEditText messageInput;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_connect, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        requestTypeGroup = view.findViewById(R.id.request_type_group);
        nameInputLayout = view.findViewById(R.id.name_input_layout);
        nameInput = view.findViewById(R.id.name_input);
        songFieldsContainer = view.findViewById(R.id.song_fields_container);
        songTitleInputLayout = view.findViewById(R.id.song_title_input_layout);
        songTitleInput = view.findViewById(R.id.song_title_input);
        artistInput = view.findViewById(R.id.artist_input);
        messageInput = view.findViewById(R.id.message_input);

        MaterialButton whatsappButton = view.findViewById(R.id.send_whatsapp_button);
        MaterialButton smsButton = view.findViewById(R.id.sms_button);
        MaterialButton callButton = view.findViewById(R.id.call_button);

        requestTypeGroup.setOnCheckedStateChangeListener((group, checkedIds) ->
                updateSongFieldsVisibility());
        updateSongFieldsVisibility(); // Song Request is checked by default in the layout

        whatsappButton.setOnClickListener(v -> {
            StudioMessage msg = buildValidatedMessage();
            if (msg != null) {
                sendViaWhatsApp(msg);
            }
        });

        smsButton.setOnClickListener(v -> {
            StudioMessage msg = buildValidatedMessage();
            if (msg != null) {
                sendViaSms(msg);
            }
        });

        callButton.setOnClickListener(v -> callStudio());
    }

    private boolean isSongRequestSelected() {
        int checkedId = requestTypeGroup.getCheckedChipId();
        return checkedId == R.id.chip_song_request;
    }

    private void updateSongFieldsVisibility() {
        songFieldsContainer.setVisibility(isSongRequestSelected() ? View.VISIBLE : View.GONE);
    }

    private StudioMessage.MessageType selectedMessageType() {
        int checkedId = requestTypeGroup.getCheckedChipId();
        if (checkedId == R.id.chip_song_request) return StudioMessage.MessageType.SONG_REQUEST;
        if (checkedId == R.id.chip_birthday) return StudioMessage.MessageType.BIRTHDAY_SHOUTOUT;
        if (checkedId == R.id.chip_prayer) return StudioMessage.MessageType.PRAYER_REQUEST;
        if (checkedId == R.id.chip_announcement) return StudioMessage.MessageType.COMMUNITY_ANNOUNCEMENT;
        return StudioMessage.MessageType.OTHER;
    }

    /**
     * Validates the form and returns a StudioMessage if valid, or shows
     * Material error states on the offending fields and returns null.
     */
    @Nullable
    private StudioMessage buildValidatedMessage() {
        nameInputLayout.setError(null);
        songTitleInputLayout.setError(null);

        String name = trimmed(nameInput);
        String songTitle = trimmed(songTitleInput);
        String artist = trimmed(artistInput);
        String message = trimmed(messageInput);

        boolean valid = true;

        if (TextUtils.isEmpty(name)) {
            nameInputLayout.setError("Name is required");
            valid = false;
        }

        StudioMessage.MessageType type = selectedMessageType();
        if (type == StudioMessage.MessageType.SONG_REQUEST && TextUtils.isEmpty(songTitle)) {
            songTitleInputLayout.setError("Song title is required for a song request");
            valid = false;
        }

        if (message.length() > 500) {
            // Belt-and-braces: maxLength=500 on the field already prevents this in practice.
            message = message.substring(0, 500);
        }

        if (!valid) {
            return null;
        }

        return new StudioMessage(
                UUID.randomUUID().toString(),
                StudioMessage.DEFAULT_STATION_ID,
                name,
                type,
                type == StudioMessage.MessageType.SONG_REQUEST ? songTitle : null,
                type == StudioMessage.MessageType.SONG_REQUEST ? artist : null,
                message,
                System.currentTimeMillis()
        );
    }

    private String trimmed(TextInputEditText field) {
        return field.getText() == null ? "" : field.getText().toString().trim();
    }

    private String composeText(StudioMessage msg) {
        StringBuilder sb = new StringBuilder();
        sb.append(msg.getMessageTypeLabel()).append(" - Kasie FM\n");
        sb.append("From: ").append(msg.getListenerName()).append("\n");
        if (msg.getSongTitle() != null && !msg.getSongTitle().isEmpty()) {
            sb.append("Song: ").append(msg.getSongTitle());
            if (msg.getArtist() != null && !msg.getArtist().isEmpty()) {
                sb.append(" - ").append(msg.getArtist());
            }
            sb.append("\n");
        }
        if (msg.getMessage() != null && !msg.getMessage().isEmpty()) {
            sb.append(msg.getMessage());
        }
        return sb.toString();
    }

    /**
     * Uses https://wa.me - a plain web link, not the whatsapp:// scheme - so
     * it opens correctly whether or not WhatsApp is installed (falls back to
     * a browser prompting to install/open it), per the spec.
     */
    private void sendViaWhatsApp(StudioMessage msg) {
        String encodedText = Uri.encode(composeText(msg));
        String url = "https://wa.me/?text=" + encodedText;
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
    }

    private void sendViaSms(StudioMessage msg) {
        Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + STUDIO_PHONE_NUMBER));
        intent.putExtra("sms_body", composeText(msg));
        startActivity(intent);
    }

    /**
     * ACTION_DIAL, not ACTION_CALL - opens the dialer with the number
     * pre-filled but requires the user to press call themselves, which
     * means no CALL_PHONE permission is ever needed. Per spec.
     */
    private void callStudio() {
        Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + STUDIO_PHONE_NUMBER));
        startActivity(intent);
    }
}
