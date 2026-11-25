package com.tuankiet.myapplication.ui.settings;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.fragment.app.Fragment;

import com.tuankiet.myapplication.R;
import com.tuankiet.myapplication.agent.AIAgentOrchestrator;
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

        setupSpinners();
        loadSettings();
        setupListeners();
        // Tam khoa ket noi OAuth khi chua cau hinh
        binding.switchFbConnection.setEnabled(false);
        binding.switchLiConnection.setEnabled(false);
    }

    private void setupSpinners() {
        String[] models = new String[] {
            "gemini-3.1-flash-lite",
            "gemini-2.5-flash-lite",
            "gemini-2.0-flash",
            "gemini-1.5-flash",
            "gpt-4o",
            "gpt-4o-mini",
            "anthropic/claude-3.5-sonnet",
            "deepseek/deepseek-chat"
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            requireContext(), android.R.layout.simple_spinner_item, models
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spnModelPost.setAdapter(adapter);
        binding.spnModelComment.setAdapter(adapter);
        binding.spnModelImage.setAdapter(adapter);
    }

    private void setSpinnerSelection(Spinner spinner, String value) {
        if (value == null) return;
        SpinnerAdapter adapter = spinner.getAdapter();
        for (int i = 0; i < adapter.getCount(); i++) {
            if (value.equals(adapter.getItem(i))) {
                spinner.setSelection(i);
                break;
            }
        }
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
        updateApiKeyInputState(config.isDemoMode());

        setSpinnerSelection(binding.spnModelPost, config.getPostGenerationModel());
        setSpinnerSelection(binding.spnModelComment, config.getCommentClassificationModel());
        setSpinnerSelection(binding.spnModelImage, config.getImagePromptModel());
        String currentLocale = AppCompatDelegate.getApplicationLocales().toLanguageTags();
        if (currentLocale.contains("en")) {
            binding.rbEn.setChecked(true);
        } else {
            binding.rbVi.setChecked(true);
        }

        if (LLMConfig.PROVIDER_OPENAI.equalsIgnoreCase(config.getProvider())) {
            binding.rbOpenAI.setChecked(true);
        } else if (LLMConfig.PROVIDER_OPENROUTER.equalsIgnoreCase(config.getProvider())) {
            binding.rbOpenRouter.setChecked(true);
        } else {
            binding.rbGemini.setChecked(true);
        }

        if (config.getApiKey() != null && !config.getApiKey().isEmpty()) {
            binding.etApiKey.setText(config.getApiKey());
            binding.tvApiKeyStatus.setVisibility(View.VISIBLE);
            binding.tvApiKeyStatus.setText("Trạng thái Key: Đã lưu API Key mã hóa (" + config.getProvider() + ")");
            binding.tvApiKeyStatus.setTextColor(getResources().getColor(R.color.status_green));
        } else {
            binding.tvApiKeyStatus.setVisibility(View.VISIBLE);
            binding.tvApiKeyStatus.setText("Trạng thái Key: Chưa cấu hình API Key");
            binding.tvApiKeyStatus.setTextColor(getResources().getColor(R.color.text_secondary));
        }

        updateHelperLinkText();

        // 3. Token metrics
        updateTokenMetrics(config);
    }

    private void updateApiKeyInputState(boolean isDemo) {
        binding.etApiKey.setEnabled(!isDemo);
        binding.btnValidateApiKey.setEnabled(!isDemo);
        binding.tilApiKey.setAlpha(isDemo ? 0.5f : 1.0f);
        binding.btnValidateApiKey.setAlpha(isDemo ? 0.5f : 1.0f);
    }

    private void updateHelperLinkText() {
        if (binding.rbOpenAI.isChecked()) {
            binding.tvGetApiKeyLink.setText("🔗 Chưa có Key? Lấy OpenAI API Key tại platform.openai.com ↗");
        } else if (binding.rbOpenRouter.isChecked()) {
            binding.tvGetApiKeyLink.setText("🔗 Chưa có Key? Lấy OpenRouter Key tại openrouter.ai ↗");
        } else {
            binding.tvGetApiKeyLink.setText("🔗 Chưa có Key? Lấy Google Gemini API Key miễn phí tại Google AI Studio ↗");
        }
    }

    private String getSelectedProvider() {
        if (binding.rbOpenAI.isChecked()) return LLMConfig.PROVIDER_OPENAI;
        if (binding.rbOpenRouter.isChecked()) return LLMConfig.PROVIDER_OPENROUTER;
        return LLMConfig.PROVIDER_GEMINI;
    }

    private void updateTokenMetrics(LLMConfig config) {
        binding.tvTotalTokens.setText(String.format("%,d tokens", config.getTotalTokensUsed()));
        binding.tvTotalCost.setText(String.format("$%.4f USD", config.getTotalCostUsd()));
        binding.tvTodayTokens.setText(String.format("%,d tokens ($%.4f)", prefManager.getTodayTokens(), prefManager.getTodayCost()));
        binding.tvMonthTokens.setText(String.format("%,d tokens ($%.4f)", prefManager.getThisMonthTokens(), prefManager.getThisMonthCost()));
    }

    private void setupListeners() {
        // Language Listener
        binding.rgLanguage.setOnCheckedChangeListener((group, checkedId) -> {
            String langTag = (checkedId == R.id.rbEn) ? "en" : "vi";
            AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags(langTag)
            );
        });

        // Provider Selection Listener
        binding.rgProvider.setOnCheckedChangeListener((group, checkedId) -> updateHelperLinkText());

        // Helper Link Click Listener (Open Browser to Get Key)
        binding.tvGetApiKeyLink.setOnClickListener(v -> {
            String url = "https://aistudio.google.com/app/apikey";
            if (binding.rbOpenAI.isChecked()) {
                url = "https://platform.openai.com/api-keys";
            } else if (binding.rbOpenRouter.isChecked()) {
                url = "https://openrouter.ai/keys";
            }
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(getContext(), "Không thể mở trình duyệt!", Toast.LENGTH_SHORT).show();
            }
        });

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
            updateApiKeyInputState(isChecked);
            refreshAccountDisplay();
            String status = isChecked ? "Đã BẬT Chế độ Demo (Không tốn token API)" : "Đã TẮT Chế độ Demo (Sử dụng API Key thực tế)";
            Toast.makeText(getContext(), status, Toast.LENGTH_SHORT).show();
        });

        // Independent "Kiểm Tra Key" Button
        binding.btnValidateApiKey.setOnClickListener(v -> {
            String provider = getSelectedProvider();
            String apiKey = binding.etApiKey.getText() != null ?
                    binding.etApiKey.getText().toString().trim() : "";

            if (apiKey.isEmpty()) {
                Toast.makeText(getContext(), "Vui lòng nhập API Key để kiểm tra!", Toast.LENGTH_SHORT).show();
                return;
            }

            binding.tvApiKeyStatus.setVisibility(View.VISIBLE);
            binding.tvApiKeyStatus.setText("🔄 Đang gửi request kiểm tra kết nối với " + provider + "...");
            binding.tvApiKeyStatus.setTextColor(getResources().getColor(R.color.primary));

            new AIAgentOrchestrator(requireContext()).validateApiKey(provider, apiKey, new AIAgentOrchestrator.ValidationCallback() {
                @Override
                public void onSuccess(String message) {
                    if (isAdded()) {
                        binding.tvApiKeyStatus.setText("✅ " + message);
                        binding.tvApiKeyStatus.setTextColor(getResources().getColor(R.color.status_green));
                        Toast.makeText(getContext(), "✅ Key hợp lệ!", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onError(String error) {
                    if (isAdded()) {
                        binding.tvApiKeyStatus.setText("⚠️ " + error);
                        binding.tvApiKeyStatus.setTextColor(getResources().getColor(R.color.status_red));
                        Toast.makeText(getContext(), "⚠️ Lỗi kết nối Key!", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        });

        // Save LLM Config Button
        binding.btnSaveApiKey.setOnClickListener(v -> {
            String provider = getSelectedProvider();
            String apiKey = binding.etApiKey.getText() != null ?
                    binding.etApiKey.getText().toString().trim() : "";
            boolean isDemo = binding.switchDemoMode.isChecked();

            String modelPost = binding.spnModelPost.getSelectedItem() != null ? binding.spnModelPost.getSelectedItem().toString() : "gemini-3.1-flash-lite";
            String modelComment = binding.spnModelComment.getSelectedItem() != null ? binding.spnModelComment.getSelectedItem().toString() : "gemini-2.5-flash-lite";
            String modelImage = binding.spnModelImage.getSelectedItem() != null ? binding.spnModelImage.getSelectedItem().toString() : "gemini-2.0-flash";

            prefManager.saveConfig(provider, apiKey, isDemo, modelPost, modelComment, modelImage);
            binding.tvApiKeyStatus.setVisibility(View.VISIBLE);
            if (isDemo) {
                binding.tvApiKeyStatus.setText("Trạng thái Key: Đang hoạt động ở Chế độ Demo (Giả lập)");
                binding.tvApiKeyStatus.setTextColor(getResources().getColor(R.color.primary));
            } else {
                binding.tvApiKeyStatus.setText("Trạng thái Key: Đã lưu API Key mã hóa (" + provider + ")");
                binding.tvApiKeyStatus.setTextColor(getResources().getColor(R.color.status_green));
            }
            Toast.makeText(getContext(), "💾 Đã lưu cấu hình LLM thành công!", Toast.LENGTH_SHORT).show();
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
                    binding.switchFbConnection.setEnabled(false);
                } else if (acc.getPlatform() == SocialPlatform.LINKEDIN) {
                    binding.tvSettingsLiDesc.setText(description);
                    binding.switchLiConnection.setChecked(displayedConnected);
                    binding.switchLiConnection.setEnabled(false);
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

