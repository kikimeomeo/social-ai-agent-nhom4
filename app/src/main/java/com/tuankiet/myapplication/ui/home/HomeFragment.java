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
import com.tuankiet.myapplication.ui.preview.PreviewDialogFragment;

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

        binding.tvViewAll.setOnClickListener(v -> {
            loadData();
            Toast.makeText(getContext(), "Đã cập nhật dữ liệu mới nhất!", Toast.LENGTH_SHORT).show();
        });
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
            SocialPlatform initialPlatform = (post.isTargetThreads() && !post.isTargetFacebook())
                    ? SocialPlatform.THREADS
                    : SocialPlatform.FACEBOOK;
            PreviewDialogFragment dialog = PreviewDialogFragment.newInstance(post, initialPlatform);
            dialog.setOnPreviewActionListener(new PreviewDialogFragment.OnPreviewActionListener() {
                @Override
                public void onPostUpdated(Post updatedPost) {
                    if (!isAdded() || updatedPost == null) return;
                    repository.updatePost(updatedPost);
                    loadData();
                }

                @Override
                public void onApprovedForPublish(Post approvedPost, SocialPlatform activePlatform) {
                    if (!isAdded() || approvedPost == null) return;
                    approvedPost.setStatus(Post.STATUS_PUBLISHED);
                    approvedPost.setUserApproved(true);
                    repository.updatePost(approvedPost);
                    loadData();
                    Toast.makeText(getContext(), "✅ Đã duyệt & cập nhật bài viết (" + activePlatform.getShortName() + ")!", Toast.LENGTH_SHORT).show();
                }
            });
            dialog.show(getParentFragmentManager(), "HomePreviewDialog");
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
            } else if (acc.getPlatform() == SocialPlatform.LINKEDIN) {
                binding.tvLiProfileName.setText("@aitech.vn");
                binding.tvLiFollowers.setText(acc.getFollowerCount() + " kết nối");
                binding.tvLiStatus.setText(acc.isConnected() ? "● Đã kết nối" : "○ Chưa kết nối");
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

