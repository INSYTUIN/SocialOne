package com.example.socialonetwo;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class QuickAccessFragment extends Fragment {

    public interface QuickAccessListener {
        void onLaunchAppOrWeb(String packageName, String webUrl);
    }

    private QuickAccessListener listener;

    public void setListener(QuickAccessListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_quick_access, container, false);
        setupQuickAccessMessages(view);
        return view;
    }

    private void setupQuickAccessMessages(View view) {
        View link1 = view.findViewById(R.id.cardLinkedIn);
        View link2 = view.findViewById(R.id.cardTwitter);
        View link3 = view.findViewById(R.id.cardFacebook);
        View link4 = view.findViewById(R.id.cardInstagram);
        View link5 = view.findViewById(R.id.cardGmail);
        View link6 = view.findViewById(R.id.cardWhatsApp);
        View link7 = view.findViewById(R.id.cardTelegram);
        View link8 = view.findViewById(R.id.cardDiscord);
        View link9 = view.findViewById(R.id.cardReddit);
        View link10 = view.findViewById(R.id.cardTikTok);
        View link11 = view.findViewById(R.id.cardPinterest);
        View link12 = view.findViewById(R.id.cardSnapchat);

        UIUtils.setClickAnimation(getContext(), link1);
        UIUtils.setClickAnimation(getContext(), link2);
        UIUtils.setClickAnimation(getContext(), link3);
        UIUtils.setClickAnimation(getContext(), link4);
        UIUtils.setClickAnimation(getContext(), link5);
        UIUtils.setClickAnimation(getContext(), link6);
        UIUtils.setClickAnimation(getContext(), link7);
        UIUtils.setClickAnimation(getContext(), link8);
        UIUtils.setClickAnimation(getContext(), link9);
        UIUtils.setClickAnimation(getContext(), link10);
        UIUtils.setClickAnimation(getContext(), link11);
        UIUtils.setClickAnimation(getContext(), link12);

        link1.setOnClickListener(v -> launchAppOrWeb("com.linkedin.android", "https://www.linkedin.com/messaging/"));
        link2.setOnClickListener(v -> launchAppOrWeb("com.twitter.android", "https://twitter.com/messages"));
        link3.setOnClickListener(v -> launchAppOrWeb("com.facebook.orca", "https://www.facebook.com/messages/"));
        link4.setOnClickListener(v -> launchAppOrWeb("com.instagram.android", "https://www.instagram.com/direct/inbox/"));
        link5.setOnClickListener(v -> launchAppOrWeb("com.google.android.gm", "https://mail.google.com/mail/u/0/#inbox"));
        link6.setOnClickListener(v -> launchAppOrWeb("com.whatsapp", "https://web.whatsapp.com/"));
        link7.setOnClickListener(v -> launchAppOrWeb("org.telegram.messenger", "https://web.telegram.org/"));
        link8.setOnClickListener(v -> launchAppOrWeb("com.discord", "https://discord.com/channels/@me"));
        link9.setOnClickListener(v -> launchAppOrWeb("com.reddit.frontpage", "https://www.reddit.com/chat"));
        link10.setOnClickListener(v -> launchAppOrWeb("com.zhiliaoapp.musically", "https://www.tiktok.com/messages"));
        link11.setOnClickListener(v -> launchAppOrWeb("com.pinterest", "https://www.pinterest.com/notifications/"));
        link12.setOnClickListener(v -> launchAppOrWeb("com.snapchat.android", "https://web.snapchat.com/"));
    }

    private void launchAppOrWeb(String packageName, String webUrl) {
        if (listener != null) {
            listener.onLaunchAppOrWeb(packageName, webUrl);
        }
    }
}
