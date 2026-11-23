package com.tuankiet.myapplication.ui.analytics;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.tuankiet.myapplication.data.DatabaseHelper;
import com.tuankiet.myapplication.databinding.FragmentAnalyticsBinding;
import com.tuankiet.myapplication.model.AnalyticsData;
import com.tuankiet.myapplication.repository.MockDataRepository;
import com.tuankiet.myapplication.util.NetworkUtils;

import java.util.List;

public class AnalyticsFragment extends Fragment {

    private FragmentAnalyticsBinding binding;
    private MockDataRepository repository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAnalyticsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = MockDataRepository.getInstance(requireContext());
        loadAnalytics();
    }

    private void loadAnalytics() {
        boolean isOffline = !NetworkUtils.isNetworkAvailable(requireContext());
        if (isOffline && isAdded()) {
            Toast.makeText(getContext(), "📴 Chế độ Offline: Đang hiển thị dữ liệu lịch sử từ SQLite cục bộ", Toast.LENGTH_SHORT).show();
        }

        AnalyticsData data = repository.getAnalyticsData();

        // Persist snapshot to SQLite history
        DatabaseHelper.getInstance(requireContext()).insertAnalyticsSnapshot(
                "ALL", data.getTotalReach(), data.getTotalLikes(), data.getTotalComments(), 0, 1540
        );

        // 0. Overview KPI Cards
        binding.tvTotalReach.setText(String.format("%,d", data.getTotalReach()));
        binding.tvTotalLikes.setText(String.format("%,d", data.getTotalLikes()));
        binding.tvTotalComments.setText(String.format("%,d", data.getTotalComments()));
        binding.tvAvgRate.setText(String.format("%.2f%%", data.getAverageEngagementRate()));

        // 1. Chart Data from SQLite / repository
        List<AnalyticsData.DayMetric> history = DatabaseHelper.getInstance(requireContext()).getAnalyticsHistory();
        if (history != null && !history.isEmpty()) {
            binding.customChartView.setData(history);
        } else if (data.getWeeklyTimeline() != null) {
            binding.customChartView.setData(data.getWeeklyTimeline());
        }

        // 2. Comparison Table
        binding.tvFbReach.setText(String.format("%,d", data.getFacebookReach()));
        binding.tvFbLikes.setText(String.format("%,d", data.getFacebookLikes()));
        binding.tvFbComments.setText(String.format("%,d", data.getFacebookComments()));
        binding.tvFbRate.setText(String.format("%.2f%%", data.getFacebookEngagementRate()));

        binding.tvLiReach.setText(String.format("%,d", data.getThreadsReach()));
        binding.tvLiLikes.setText(String.format("%,d", data.getThreadsLikes()));
        binding.tvLiComments.setText(String.format("%,d", data.getThreadsComments()));
        binding.tvLiRate.setText(String.format("%.2f%%", data.getThreadsEngagementRate()));

        // 3. AI Weekly Summary Report
        binding.tvTopPost.setText(data.getTopPerformingPost());
        binding.tvWeeklySummary.setText(data.getWeeklyAiSummary());
        String advice = data.getNextWeekAdvice() != null
                ? data.getNextWeekAdvice().replace("LinkedIn", "Threads")
                : "";
        binding.tvNextWeekAdvice.setText(advice);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadAnalytics();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

