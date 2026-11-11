package com.tuankiet.myapplication.ui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.tuankiet.myapplication.R;
import com.tuankiet.myapplication.data.OAuthManager;
import com.tuankiet.myapplication.data.SecurePreferenceManager;
import com.tuankiet.myapplication.databinding.FragmentSettingsBinding;
import com.tuankiet.myapplication.model.LLMConfig;
import com.tuankiet.myapplication.model.SocialAccount;
import com.tuankiet.myapplication.model.SocialPlatform;
import com.tuankiet.myapplication.repository.MockDataRepository;

import java.util.List;

public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;
    private SecurePreferenceManager prefManager;
    private MockDataRepository repository;
    private boolean updatingAccountUi = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        prefManager = SecurePreferenceManager.getInstance(requireContext());
        repository = MockDataRepository.getInstance(requireContext());

        loadSettings();
        setupListeners();
    }

    private void loadSettings() {
        // 1. Accounts
        refreshAccountDisplay();

        // 1.1 Threads Token
        String threadsToken = prefManager.getThreadsToken();
        if (threadsToken != null && !threadsToken.isEmpty()) {
            binding.etSettingsThreadsToken.setText(threadsToken);
            binding.tvSettingsThreadsDesc.setText(
                    "Đã lưu Token • Chưa xác minh kết nối"
            );
            binding.tvSettingsThreadsDesc.setTextColor(
                    getResources().getColor(R.color.text_secondary)
            );
        } else {
            binding.tvSettingsThreadsDesc.setText("Chưa cấu hình Token");
            binding.tvSettingsThreadsDesc.setTextColor(getResources().getColor(R.color.text_secondary));
        }

        // 2. LLM Config
        LLMConfig config = prefManager.getConfig();
        binding.switchDemoMode.setChecked(config.isDemoMode());

        if (LLMConfig.PROVIDER_OPENAI.equalsIgnoreCase(config.getProvider())) {
            binding.rbOpenAI.setChecked(true);
        } else if (LLMConfig.PROVIDER_OPENROUTER.equalsIgnoreCase(config.getProvider())) {
            binding.rbOpenRouter.setChecked(true);
        } else {
            binding.rbGemini.setChecked(true);
        }

        if (config.getApiKey() != null && !config.getApiKey().isEmpty()) {
            binding.etApiKey.setText(config.getApiKey());
        }

        // 3. Token metrics
        updateTokenMetrics(config);
    }

    private void updateTokenMetrics(LLMConfig config) {
        binding.tvTotalTokens.setText(String.format("%,d", config.getTotalTokensUsed()));
        binding.tvTotalCost.setText(String.format("$%.4f USD", config.getTotalCostUsd()));
    }

    private void setupListeners() {
        // Toggle Facebook
        binding.switchFbConnection.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (updatingAccountUi) return;

            if (isChecked) {
                OAuthManager.getInstance(requireContext()).launchOAuth(requireActivity(), SocialPlatform.FACEBOOK);
            } else {
                OAuthManager.getInstance(requireContext()).disconnectChannel(SocialPlatform.FACEBOOK);
                Toast.makeText(getContext(), "Đã ngắt kết nối Facebook Page.", Toast.LENGTH_SHORT).show();
            }
        });

        // Toggle LinkedIn
        binding.switchLiConnection.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (updatingAccountUi) return;

            if (isChecked) {
                OAuthManager.getInstance(requireContext()).launchOAuth(requireActivity(), SocialPlatform.LINKEDIN);
            } else {
                OAuthManager.getInstance(requireContext()).disconnectChannel(SocialPlatform.LINKEDIN);
                Toast.makeText(getContext(), "Đã ngắt kết nối LinkedIn.", Toast.LENGTH_SHORT).show();
            }
        });

        // Save Threads Token
        binding.btnSaveThreadsToken.setOnClickListener(v -> {
            String token = binding.etSettingsThreadsToken.getText() != null ?
                    binding.etSettingsThreadsToken.getText().toString().trim() : "";
            if (token.isEmpty()) {
                Toast.makeText(getContext(), "Vui lòng nhập Threads Token!", Toast.LENGTH_SHORT).show();
                return;
            }
            prefManager.saveThreadsToken(token);
            binding.tvSettingsThreadsDesc.setText(
                    "Đã lưu Token • Chưa xác minh kết nối"
            );
            binding.tvSettingsThreadsDesc.setTextColor(
                    getResources().getColor(R.color.text_secondary)
            );
            Toast.makeText(getContext(), "🎉 Đã lưu Threads Token thành công!", Toast.LENGTH_SHORT).show();
        });

        // Disconnect Threads
        binding.btnDisconnectThreads.setOnClickListener(v -> {
            prefManager.clearThreadsToken();

            binding.etSettingsThreadsToken.setText("");
            binding.tvSettingsThreadsDesc.setText("Chưa cấu hình Token");
            binding.tvSettingsThreadsDesc.setTextColor(
                    getResources().getColor(R.color.text_secondary)
            );

            Toast.makeText(
                    requireContext(),
                    "Đã xóa Threads Token trên thiết bị!",
                    Toast.LENGTH_SHORT
            ).show();
        });

        // Toggle Demo Mode
        binding.switchDemoMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefManager.setDemoMode(isChecked);
            refreshAccountDisplay();
            String status = isChecked ? "Đã BẬT Chế độ Demo (Không tốn token API)" : "Đã TẮT Chế độ Demo (Sử dụng API Key thực tế)";
            Toast.makeText(getContext(), status, Toast.LENGTH_SHORT).show();
        });

        // Save LLM Config
        binding.btnSaveApiKey.setOnClickListener(v -> {
            String provider = LLMConfig.PROVIDER_GEMINI;
            if (binding.rbOpenAI.isChecked()) {
                provider = LLMConfig.PROVIDER_OPENAI;
            } else if (binding.rbOpenRouter.isChecked()) {
                provider = LLMConfig.PROVIDER_OPENROUTER;
            }

            String apiKey = binding.etApiKey.getText() != null ?
                    binding.etApiKey.getText().toString().trim() : "";
            boolean isDemo = binding.switchDemoMode.isChecked();

            if (!apiKey.isEmpty() && !isDemo) {
                Toast.makeText(getContext(), "Đang kiểm tra kết nối API Key (" + provider + ")...", Toast.LENGTH_SHORT).show();
                String finalProvider = provider;
                new com.tuankiet.myapplication.agent.AIAgentOrchestrator(requireContext())
                        .validateApiKey(provider, apiKey, new com.tuankiet.myapplication.agent.AIAgentOrchestrator.ValidationCallback() {
                            @Override
                            public void onSuccess(String message) {
                                prefManager.saveConfig(finalProvider, apiKey, isDemo);
                                Toast.makeText(getContext(), "✅ " + message, Toast.LENGTH_LONG).show();
                            }

                            @Override
                            public void onError(String error) {
                                Toast.makeText(getContext(), "⚠️ " + error, Toast.LENGTH_LONG).show();
                            }
                        });
            } else {
                prefManager.saveConfig(provider, apiKey, isDemo);
                Toast.makeText(getContext(), "Đã lưu cấu hình (Chế độ Demo: " + (isDemo ? "BẬT" : "TẮT") + ")", Toast.LENGTH_SHORT).show();
            }
        });

        // Reset Tokens
        binding.btnResetTokens.setOnClickListener(v -> {
            prefManager.resetTokenStats();
            LLMConfig updated = prefManager.getConfig();
            updateTokenMetrics(updated);
            Toast.makeText(getContext(), "Đã đặt lại bộ đếm token về 0", Toast.LENGTH_SHORT).show();
        });
    }

    private void refreshAccountDisplay() {
        if (binding == null) return;

        boolean demoMode = prefManager.isDemoMode();
        List<SocialAccount> accounts = repository.getAccounts();

        updatingAccountUi = true;

        try {
            for (SocialAccount acc : accounts) {
                String description = acc.getAccountName()
                        + " (" + acc.getHandle() + ")";

                if (demoMode) {
                    description += " • TÀI KHOẢN DEMO";
                } else {
                    description += " • Chưa xác minh OAuth";
                }

                boolean displayedConnected = demoMode && acc.isConnected();

                if (acc.getPlatform() == SocialPlatform.FACEBOOK) {
                    binding.tvSettingsFbDesc.setText(description);
                    binding.switchFbConnection.setChecked(displayedConnected);
                } else if (acc.getPlatform() == SocialPlatform.LINKEDIN) {
                    binding.tvSettingsLiDesc.setText(description);
                    binding.switchLiConnection.setChecked(displayedConnected);
                }
            }
        } finally {
            updatingAccountUi = false;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

