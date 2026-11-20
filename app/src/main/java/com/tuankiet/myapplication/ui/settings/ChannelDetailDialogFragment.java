package com.tuankiet.myapplication.ui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.tuankiet.myapplication.R;
import com.tuankiet.myapplication.data.DatabaseHelper;
import com.tuankiet.myapplication.data.OAuthManager;
import com.tuankiet.myapplication.databinding.DialogChannelDetailBinding;
import com.tuankiet.myapplication.model.SocialAccount;
import com.tuankiet.myapplication.model.SocialPlatform;

import java.util.List;

public class ChannelDetailDialogFragment extends DialogFragment {

    private static final String ARG_PLATFORM = "platform_arg";
    private DialogChannelDetailBinding binding;
    private SocialPlatform platform = SocialPlatform.FACEBOOK;
    private Runnable onDismissCallback;

    public static ChannelDetailDialogFragment newInstance(SocialPlatform platform) {
        ChannelDetailDialogFragment fragment = new ChannelDetailDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PLATFORM, platform.name());
        fragment.setArguments(args);
        return fragment;
    }

    public void setOnDismissCallback(Runnable callback) {
        this.onDismissCallback = callback;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, com.google.android.material.R.style.Theme_Material3_DayNight_Dialog_Alert);
        if (getArguments() != null) {
            String platStr = getArguments().getString(ARG_PLATFORM, SocialPlatform.FACEBOOK.name());
            try {
                platform = SocialPlatform.valueOf(platStr);
            } catch (Exception ignored) {
                platform = SocialPlatform.FACEBOOK;
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DialogChannelDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadAccountData();
        setupListeners();
    }

    private void loadAccountData() {
        if (binding == null || getContext() == null) return;

        DatabaseHelper db = DatabaseHelper.getInstance(requireContext());
        List<SocialAccount> accounts = db.getAllAccounts();
        SocialAccount target = null;
        for (SocialAccount acc : accounts) {
            if (acc.getPlatform() == platform) {
                target = acc;
                break;
            }
        }

        if (platform == SocialPlatform.FACEBOOK) {
            binding.ivDetailPlatformIcon.setImageResource(R.drawable.ic_facebook);
            binding.tvDetailAccountName.setText(target != null ? target.getAccountName() : "AI Tech Vietnam");
            binding.tvDetailHandle.setText(target != null ? target.getHandle() : "@aitechvn.official");
            binding.tvCapPublish.setText("✅ Xuất bản bài viết Facebook Page tự động qua Meta Graph API v19.0");
            binding.tvCapComments.setText("✅ Quét & Đồng bộ bình luận theo thời gian thực (pages_read_engagement)");
            binding.tvCapReplies.setText("✅ AI Agent tự động phản hồi có kiểm soát (pages_manage_posts)");
            binding.tvCapAnalytics.setText("✅ Theo dõi số liệu Reach, Likes, Shares và Biểu đồ tăng trưởng (F7)");
        } else {
            binding.ivDetailPlatformIcon.setImageResource(R.drawable.ic_threads);
            binding.tvDetailAccountName.setText(target != null ? target.getAccountName() : "AI Tech VN");
            binding.tvDetailHandle.setText(target != null ? target.getHandle() : "@aitech.vn");
            binding.tvCapPublish.setText("✅ Xuất bản bài viết Threads Container 2 bước chính thức (Official API v1.0)");
            binding.tvCapComments.setText("✅ Quét replies tự động mỗi 12 giây (threads_read_replies)");
            binding.tvCapReplies.setText("✅ AI Agent tự động trả lời có kiểm soát & chặn tiêu cực (F6)");
            binding.tvCapAnalytics.setText("✅ Lấy permalink bài viết gốc và đo lường tương tác");
        }

        boolean isConnected = target != null && target.isConnected();
        binding.tvDetailStatusBadge.setText(isConnected ? "● Đã kết nối" : "○ Chưa kết nối");
        binding.tvDetailStatusBadge.setTextColor(getResources().getColor(isConnected ? R.color.status_green : R.color.text_muted));
        binding.tvDetailStatusBadge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                isConnected ? 0xFFE6F4EA : 0xFFF1F5F9
        ));

        String statusMsg = (target != null && target.getStatusMessage() != null)
                ? target.getStatusMessage()
                : (isConnected ? "Đang kết nối qua OAuth 2.0 Token" : "Chưa cấu hình kết nối");
        binding.tvDetailStatusMessage.setText(statusMsg);
    }

    private void setupListeners() {
        binding.btnDetailRefreshToken.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Đang gửi yêu cầu làm mới Token...", Toast.LENGTH_SHORT).show();
            OAuthManager.getInstance(requireContext()).checkAndRefreshTokenIfNeeded(platform, new OAuthManager.OAuthCallback() {
                @Override
                public void onSuccess(SocialPlatform p, String message) {
                    if (isAdded()) {
                        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
                        loadAccountData();
                    }
                }

                @Override
                public void onError(String errorMessage) {
                    if (isAdded()) {
                        Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
                        loadAccountData();
                    }
                }
            });
        });

        binding.btnDetailOAuth.setOnClickListener(v -> {
            dismiss();
            OAuthManager.getInstance(requireContext()).launchOAuth(requireActivity(), platform);
        });

        binding.btnDetailDisconnect.setOnClickListener(v -> {
            OAuthManager.getInstance(requireContext()).disconnectChannel(platform);
            Toast.makeText(getContext(), "Đã ngắt kết nối và xoá token của " + platform.getDisplayName(), Toast.LENGTH_SHORT).show();
            loadAccountData();
        });

        binding.btnDetailClose.setOnClickListener(v -> dismiss());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        if (onDismissCallback != null) {
            onDismissCallback.run();
        }
    }
}
