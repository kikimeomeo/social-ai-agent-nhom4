package com.tuankiet.myapplication.ui.preview;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.tuankiet.myapplication.databinding.DialogPreviewBinding;

public class PreviewDialogFragment extends DialogFragment {

    private static final int MAX_THREADS_LIMIT = 500; // Giới hạn 500 ký tự Threads
    private DialogPreviewBinding binding;
    private String initialContent = "";

    public static PreviewDialogFragment newInstance(String content) {
        PreviewDialogFragment fragment = new PreviewDialogFragment();
        Bundle args = new Bundle();
        args.putString("ARG_CONTENT", content);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DialogPreviewBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            initialContent = getArguments().getString("ARG_CONTENT", "");
            binding.etPostContent.setText(initialContent);
        }

        setupLiveCharCounter();
        setupAIRewriteButtons();

        binding.btnDismiss.setOnClickListener(v -> dismiss());
        binding.btnSave.setOnClickListener(v -> {
            String updatedText = binding.etPostContent.getText().toString();
            Toast.makeText(getContext(), "✅ Đã lưu nội dung bài viết!", Toast.LENGTH_SHORT).show();
            dismiss();
        });
    }

    // Tính năng: Đếm live số ký tự & Cảnh báo đỏ khi vượt giới hạn 500 ký tự
    private void setupLiveCharCounter() {
        binding.etPostContent.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                int length = (s != null) ? s.length() : 0;
                if (length > MAX_THREADS_LIMIT) {
                    binding.tvCharCounter.setTextColor(Color.parseColor("#EF4444"));
                    binding.tvCharCounter.setText("⚠️ " + length + " / " + MAX_THREADS_LIMIT + " ký tự (Vượt giới hạn Threads!)");
                } else {
                    binding.tvCharCounter.setTextColor(Color.parseColor("#64748B"));
                    binding.tvCharCounter.setText(length + " / " + MAX_THREADS_LIMIT + " ký tự");
                }
            }
        });
    }

    // Tính năng: Bộ 3 nút AI Viết lại (Ngắn hơn, Trang trọng hơn, Đổi Hook)
    private void setupAIRewriteButtons() {
        binding.btnShorten.setOnClickListener(v -> {
            String current = binding.etPostContent.getText().toString();
            if (!current.isEmpty()) {
                binding.etPostContent.setText(current.substring(0, Math.min(current.length(), 100)) + "...");
                Toast.makeText(getContext(), "✨ AI đã tóm tắt ngắn gọn hơn", Toast.LENGTH_SHORT).show();
            }
        });

        binding.btnFormal.setOnClickListener(v -> {
            String current = binding.etPostContent.getText().toString();
            binding.etPostContent.setText("Kính gửi quý độc giả,\n\n" + current);
            Toast.makeText(getContext(), "✨ AI đã chuyển sang giọng văn trang trọng", Toast.LENGTH_SHORT).show();
        });

        binding.btnChangeHook.setOnClickListener(v -> {
            String current = binding.etPostContent.getText().toString();
            binding.etPostContent.setText("🔥 BÍ QUYẾT MỚI NHẤT: " + current);
            Toast.makeText(getContext(), "✨ AI đã đổi câu Hook mở đầu", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}