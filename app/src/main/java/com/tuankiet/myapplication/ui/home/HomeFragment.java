package com.tuankiet.myapplication.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.tuankiet.myapplication.MainActivity;
import com.tuankiet.myapplication.R;
import com.tuankiet.myapplication.databinding.FragmentHomeBinding;
import com.tuankiet.myapplication.model.AnalyticsData;
import com.tuankiet.myapplication.model.Post;
import com.tuankiet.myapplication.model.SocialAccount;
import com.tuankiet.myapplication.model.SocialPlatform;
import com.tuankiet.myapplication.repository.MockDataRepository;
import com.tuankiet.myapplication.data.SecurePreferenceManager;

import java.util.List;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private PostAdapter adapter;
    private MockDataRepository repository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repository = MockDataRepository.getInstance(requireContext());
        setupRecyclerView();
        loadData();

        binding.fabCreatePost.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToTab(R.id.nav_create);
            }
        });

        binding.qaCreate.setOnClickListener(v -> navigateTo(R.id.nav_create));
        binding.qaInbox.setOnClickListener(v -> navigateTo(R.id.nav_inbox));
        binding.qaAnalytics.setOnClickListener(v -> navigateTo(R.id.nav_analytics));
        binding.qaSettings.setOnClickListener(v -> navigateTo(R.id.nav_settings));
        binding.btnHomeBell.setOnClickListener(v -> navigateTo(R.id.nav_inbox));
        binding.bannerPendingComments.setOnClickListener(v -> navigateTo(R.id.nav_inbox));
        binding.tvManageChannels.setOnClickListener(v -> navigateTo(R.id.nav_settings));

        binding.cardFbChannel.setOnClickListener(v -> openChannelDetail(SocialPlatform.FACEBOOK));
        binding.cardThreadsChannel.setOnClickListener(v -> openChannelDetail(SocialPlatform.THREADS));

        binding.tvViewAll.setOnClickListener(v -> {
            loadData();
            Toast.makeText(getContext(), "Đã cập nhật dữ liệu mới nhất!", Toast.LENGTH_SHORT).show();
        });
    }

    private void openChannelDetail(SocialPlatform platform) {
        com.tuankiet.myapplication.ui.settings.ChannelDetailDialogFragment dialog =
                com.tuankiet.myapplication.ui.settings.ChannelDetailDialogFragment.newInstance(platform);
        dialog.setOnDismissCallback(this::loadData);
        dialog.show(getParentFragmentManager(), "ChannelDetailDialog_" + platform.name());
    }

    private void navigateTo(int tabId) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).navigateToTab(tabId);
        }
    }

    private void setupRecyclerView() {
        adapter = new PostAdapter();
        binding.rvRecentPosts.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvRecentPosts.setAdapter(adapter);

        adapter.setOnPostClickListener(post -> {
            StringBuilder msg = new StringBuilder();
            msg.append("Chủ đề: ").append(post.getTopic()).append("\n");
            msg.append("Trạng thái: ").append(post.getStatus()).append("\n");

            if (post.getIdempotencyKey() != null && !post.getIdempotencyKey().isEmpty()) {
                msg.append("Mã chống trùng lặp (Idempotency): ").append(post.getIdempotencyKey()).append("\n");
            }
            if (post.getRetryCount() > 0) {
                msg.append("Số lần retry: ").append(post.getRetryCount()).append("\n");
            }
            if (post.getPublishError() != null && !post.getPublishError().isEmpty()) {
                msg.append("\n⚠️ Chi tiết lỗi: ").append(post.getPublishError()).append("\n");
            }

            String targetUrl = post.getThreadsPostUrl();
            if (targetUrl == null || targetUrl.isEmpty()) {
                targetUrl = post.getFbPostUrl();
            }

            if (targetUrl != null && !targetUrl.isEmpty()) {
                msg.append("\n🔗 Đường dẫn gốc: ").append(targetUrl);
            }

            androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("📋 Thông Tin Bài Viết")
                    .setMessage(msg.toString());

            final String finalUrl = targetUrl;
            if (finalUrl != null && (finalUrl.startsWith("http://") || finalUrl.startsWith("https://"))) {
                builder.setPositiveButton("Mở bài viết gốc", (dialog, which) -> {
                    try {
                        startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(finalUrl)));
                    } catch (Exception ignored) {}
                });
                builder.setNegativeButton("Đóng", null);
            } else {
                builder.setPositiveButton("Đóng", null);
            }
            builder.show();
        });
    }

    public void loadData() {
        if (binding == null) return;

        boolean demoMode = SecurePreferenceManager
                .getInstance(requireContext())
                .isDemoMode();

        binding.tvHomeModeBadge.setVisibility(
                demoMode ? View.VISIBLE : View.GONE
        );

        // Accounts info
        List<SocialAccount> accounts = repository.getAccounts();
        for (SocialAccount acc : accounts) {
            if (acc.getPlatform() == SocialPlatform.FACEBOOK) {
                String fbName = acc.getAccountName() != null ? acc.getAccountName().replace("Vietnam", "VN") : "AI Tech VN";
                binding.tvFbPageName.setText(fbName);
                binding.tvFbFollowers.setText(acc.getFollowerCount() + " người theo dõi");
                binding.tvFbStatus.setText(acc.isConnected() ? "● Đã kết nối" : "○ Chưa kết nối");
                binding.tvFbStatus.setTextColor(getResources().getColor(acc.isConnected() ? R.color.status_green : R.color.text_muted));
            } else if (acc.getPlatform() == SocialPlatform.THREADS || acc.getPlatform() == SocialPlatform.LINKEDIN) {
                String thName = acc.getHandle() != null ? acc.getHandle() : "@aitech.vn";
                binding.tvLiProfileName.setText(thName);
                binding.tvLiFollowers.setText(acc.getFollowerCount() + " người theo dõi");
                binding.tvLiStatus.setText(acc.isConnected() ? "● Đã kết nối" : "○ Chưa kết nối");
                binding.tvLiStatus.setTextColor(getResources().getColor(acc.isConnected() ? R.color.status_green : R.color.text_muted));
            }
        }

        // KPIs
        AnalyticsData analytics = repository.getAnalyticsData();
        binding.tvTotalPostsCount.setText(String.valueOf(analytics.getTotalPostsCreated()));
        binding.tvWeeklyEngagement.setText(String.format("%,d", analytics.getWeeklyEngagement()));
        binding.tvPendingComments.setText(String.valueOf(analytics.getPendingComments()));

        // Posts
        List<Post> posts = repository.getPosts();
        adapter.setPosts(posts);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

