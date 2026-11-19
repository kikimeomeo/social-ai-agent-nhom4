package com.tuankiet.myapplication.ui.logs;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.tuankiet.myapplication.data.DatabaseHelper;
import com.tuankiet.myapplication.databinding.DialogAgentLogsBinding;
import com.tuankiet.myapplication.model.AgentLog;

import java.util.List;
import java.util.Locale;

/**
 * BottomSheet hiển thị Nhật ký hoạt động AI Agent (F2 - Issue #23):
 * Tổng hợp số bước, token, chi phí ($) và danh sách log chi tiết từ SQLite.
 */
public class AgentLogsBottomSheet {

    public static void show(Context context, LayoutInflater inflater) {
        if (context == null || inflater == null) return;

        BottomSheetDialog dialog = new BottomSheetDialog(context);
        DialogAgentLogsBinding binding = DialogAgentLogsBinding.inflate(inflater);
        dialog.setContentView(binding.getRoot());

        AgentLogAdapter adapter = new AgentLogAdapter();
        binding.rvAgentLogs.setLayoutManager(new LinearLayoutManager(context));
        binding.rvAgentLogs.setAdapter(adapter);

        Runnable reloadLogs = () -> {
            List<AgentLog> logs = DatabaseHelper.getInstance(context).getAllLogs();
            adapter.setLogs(logs);

            int totalTokens = 0;
            double totalCost = 0.0;
            for (AgentLog log : logs) {
                totalTokens += log.getTokensUsed();
                totalCost += log.getCostEstimate();
            }

            binding.tvLogTotalCount.setText(logs.size() + " bước");
            binding.tvLogTotalTokens.setText(String.format(Locale.US, "%,d tok", totalTokens));
            binding.tvLogTotalCost.setText(String.format(Locale.US, "$%.4f", totalCost));

            boolean isEmpty = logs.isEmpty();
            binding.layoutEmptyLogs.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            binding.rvAgentLogs.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            binding.btnClearLogs.setEnabled(!isEmpty);
            binding.btnClearLogs.setAlpha(isEmpty ? 0.4f : 1.0f);
        };

        reloadLogs.run();

        binding.btnClearLogs.setOnClickListener(v -> new AlertDialog.Builder(context)
                .setTitle("Xoá toàn bộ Nhật ký AI?")
                .setMessage("Lịch sử Prompt, Kết quả, Token và Chi phí đã lưu sẽ bị xoá khỏi máy.")
                .setPositiveButton("Xoá tất cả", (d, which) -> {
                    DatabaseHelper.getInstance(context).clearAllLogs();
                    reloadLogs.run();
                    Toast.makeText(context, "Đã xoá toàn bộ Nhật ký AI", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Huỷ", null)
                .show());

        binding.btnCloseLogs.setOnClickListener(v -> dialog.dismiss());

        dialog.setOnShowListener(d -> {
            View parent = (View) binding.getRoot().getParent();
            if (parent != null) {
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(parent);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
                android.view.ViewGroup.LayoutParams lp = parent.getLayoutParams();
                if (lp != null) {
                    lp.width = android.view.ViewGroup.LayoutParams.MATCH_PARENT;
                    parent.setLayoutParams(lp);
                }
            }
        });

        dialog.show();
    }
}
