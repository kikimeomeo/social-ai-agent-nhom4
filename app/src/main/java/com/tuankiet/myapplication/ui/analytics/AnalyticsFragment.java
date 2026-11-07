package com.tuankiet.myapplication.ui.analytics;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.tuankiet.myapplication.databinding.FragmentAnalyticsBinding;
import com.tuankiet.myapplication.model.AnalyticsData;
import com.tuankiet.myapplication.repository.MockDataRepository;

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
        AnalyticsData data = repository.getAnalyticsData();

        // 1. Chart Data
        if (data.getWeeklyTimeline() != null) {
            binding.customChartView.setData(data.getWeeklyTimeline());
        }

        // 2. Comparison Table
        binding.tvFbReach.setText(String.format("%,d", data.getFacebookReach()));
        binding.tvFbLikes.setText(String.format("%,d", data.getFacebookLikes()));
        binding.tvFbComments.setText(String.format("%,d", data.getFacebookComments()));
        binding.tvFbRate.setText(String.format("%.2f%%", data.getFacebookEngagementRate()));

        binding.tvLiReach.setText(String.format("%,d", data.getLinkedinReach()));
        binding.tvLiLikes.setText(String.format("%,d", data.getLinkedinLikes()));
        binding.tvLiComments.setText(String.format("%,d", data.getLinkedinComments()));
        binding.tvLiRate.setText(String.format("%.2f%%", data.getLinkedinEngagementRate()));

        // 3. AI Weekly Summary Report
        binding.tvTopPost.setText(data.getTopPerformingPost());
        binding.tvWeeklySummary.setText(data.getWeeklyAiSummary());
        binding.tvNextWeekAdvice.setText("💡 " + data.getNextWeekAdvice());
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

