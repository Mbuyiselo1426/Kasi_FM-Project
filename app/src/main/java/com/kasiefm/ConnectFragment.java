package com.kasiefm;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.kasiefm.model.StudioMessage;
import com.kasiefm.api.KasieApiService;
import com.kasiefm.api.MessageRequest;
import com.kasiefm.api.MessageResponse;
import com.kasiefm.api.RetrofitClient;

import java.util.UUID;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Community form for listener messages. The primary action persists the
 * selected category and fields through POST /api/messages; SMS and calling
 * remain the existing alternate contact actions.
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
    private TextInputLayout messageInputLayout;
    private MaterialButton sendButton;
    private TextView liveStatus;
    private TextView liveShowTitle;
    private TextView liveShowTime;
    private MaterialButton livePlayButton;
    private TextView messageStudioTab;
    private TextView groupChatsTab;
    private View messageStudioIndicator;
    private View groupChatsIndicator;
    private View messageStudioContainer;
    private View groupChatsContainer;
    private Call<MessageResponse> messageCall;

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
        messageInputLayout = view.findViewById(R.id.message_input_layout);
        liveStatus = view.findViewById(R.id.community_live_status);
        liveShowTitle = view.findViewById(R.id.community_live_show_title);
        liveShowTime = view.findViewById(R.id.community_live_show_time);
        livePlayButton = view.findViewById(R.id.community_live_play_button);
        messageStudioTab = view.findViewById(R.id.message_studio_tab);
        groupChatsTab = view.findViewById(R.id.group_chats_tab);
        messageStudioIndicator = view.findViewById(R.id.message_studio_indicator);
        groupChatsIndicator = view.findViewById(R.id.group_chats_indicator);
        messageStudioContainer = view.findViewById(R.id.message_studio_container);
        groupChatsContainer = view.findViewById(R.id.group_chats_container);

        sendButton = view.findViewById(R.id.send_whatsapp_button);
        MaterialButton smsButton = view.findViewById(R.id.sms_button);
        MaterialButton callButton = view.findViewById(R.id.call_button);

        requestTypeGroup.setOnCheckedStateChangeListener((group, checkedIds) ->
                updateSongFieldsVisibility());
        updateSongFieldsVisibility(); // Song Request is checked by default in the layout

        sendButton.setOnClickListener(v -> {
            StudioMessage msg = buildValidatedMessage();
            if (msg != null) {
                submitMessage(msg);
            }
        });

        smsButton.setOnClickListener(v -> {
            StudioMessage msg = buildValidatedMessage();
            if (msg != null) {
                sendViaSms(msg);
            }
        });

        callButton.setOnClickListener(v -> callStudio());

        livePlayButton.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).toggleLiveRadioFromCommunity();
            }
        });
        messageStudioTab.setOnClickListener(v -> selectCommunityTab(true));
        groupChatsTab.setOnClickListener(v -> selectCommunityTab(false));
        View.OnClickListener comingSoon = v -> Toast.makeText(requireContext(),
                "Group chats coming soon", Toast.LENGTH_SHORT).show();
        view.findViewById(R.id.live_show_chat_card).setOnClickListener(comingSoon);
        view.findViewById(R.id.community_lounge_card).setOnClickListener(comingSoon);
        view.findViewById(R.id.local_events_card).setOnClickListener(comingSoon);
        selectCommunityTab(true);
        syncLiveMiniPlayer();
    }

    @Override public void onResume() {
        super.onResume();
        syncLiveMiniPlayer();
    }

    @Override public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) syncLiveMiniPlayer();
    }

    /** Receives display state from MainActivity; never owns a player. */
    public void renderLiveMiniPlayer(String title, String time, @Nullable String playbackState) {
        if (liveShowTitle == null) return;
        liveShowTitle.setText(TextUtils.isEmpty(title) ? "Kasie FM Live" : title);
        liveShowTime.setText(time == null ? "" : time);
        liveShowTime.setVisibility(TextUtils.isEmpty(time) ? View.GONE : View.VISIBLE);
        String state = playbackState == null ? "PAUSED" : playbackState;
        if ("LIVE".equals(state)) {
            liveStatus.setText("● LIVE");
            livePlayButton.setEnabled(true);
            livePlayButton.setText("Ⅱ");
            livePlayButton.setContentDescription("Pause live radio");
        } else if ("BUFFERING".equals(state)) {
            liveStatus.setText("● BUFFERING");
            livePlayButton.setEnabled(false);
            livePlayButton.setText("…");
            livePlayButton.setContentDescription("Live radio is buffering");
        } else if ("ERROR".equals(state)) {
            liveStatus.setText("● OFFLINE");
            livePlayButton.setEnabled(true);
            livePlayButton.setText("▶");
            livePlayButton.setContentDescription("Retry live radio");
        } else {
            liveStatus.setText("● PAUSED");
            livePlayButton.setEnabled(true);
            livePlayButton.setText("▶");
            livePlayButton.setContentDescription("Play live radio");
        }
    }

    private void syncLiveMiniPlayer() {
        if (getActivity() instanceof MainActivity) {
            MainActivity activity = (MainActivity) getActivity();
            renderLiveMiniPlayer(activity.getCommunityShowTitle(), activity.getCommunityShowTime(),
                    activity.getPlaybackStateForTesting());
        }
    }

    private void selectCommunityTab(boolean messageStudioSelected) {
        messageStudioContainer.setVisibility(messageStudioSelected ? View.VISIBLE : View.GONE);
        groupChatsContainer.setVisibility(messageStudioSelected ? View.GONE : View.VISIBLE);
        messageStudioTab.setTextColor(ContextCompat.getColor(requireContext(),
                messageStudioSelected ? R.color.kasie_accent : R.color.kasie_text_muted));
        groupChatsTab.setTextColor(ContextCompat.getColor(requireContext(),
                messageStudioSelected ? R.color.kasie_text_muted : R.color.kasie_accent));
        messageStudioIndicator.setBackgroundColor(ContextCompat.getColor(requireContext(),
                messageStudioSelected ? R.color.kasie_accent : R.color.kasie_outline));
        groupChatsIndicator.setBackgroundColor(ContextCompat.getColor(requireContext(),
                messageStudioSelected ? R.color.kasie_outline : R.color.kasie_accent));
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
        messageInputLayout.setError(null);

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

        if (TextUtils.isEmpty(message)) {
            messageInputLayout.setError("Message is required");
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

    private void submitMessage(StudioMessage message) {
        sendButton.setEnabled(false);
        sendButton.setText("SENDING...");
        MessageRequest request = new MessageRequest(
                message.getListenerName(), message.getMessageType().name(), message.getMessage(),
                message.getSongTitle(), message.getArtist());
        messageCall = RetrofitClient.getInstance().create(KasieApiService.class).createMessage(request);
        messageCall.enqueue(new Callback<MessageResponse>() {
            @Override public void onResponse(Call<MessageResponse> call, Response<MessageResponse> response) {
                if (!isAdded() || call != messageCall) return;
                messageCall = null;
                restoreSendButton();
                if (response.code() == 201) {
                    clearSubmittedForm();
                    Toast.makeText(requireContext(), "Message sent to the studio", Toast.LENGTH_SHORT).show();
                } else {
                    String error = response.code() == 400 ? "Please check your message and try again"
                            : "Unable to send message. Try again.";
                    Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
                }
            }

            @Override public void onFailure(Call<MessageResponse> call, Throwable error) {
                if (!isAdded() || call != messageCall || call.isCanceled()) return;
                messageCall = null;
                restoreSendButton();
                Toast.makeText(requireContext(), "Unable to send message. Check your connection.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void restoreSendButton() {
        sendButton.setEnabled(true);
        sendButton.setText("SEND MESSAGE");
    }

    private void clearSubmittedForm() {
        nameInput.setText("");
        songTitleInput.setText("");
        artistInput.setText("");
        messageInput.setText("");
        requestTypeGroup.check(R.id.chip_song_request);
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

    @Override public void onDestroyView() {
        if (messageCall != null) messageCall.cancel();
        messageCall = null;
        super.onDestroyView();
    }
}
