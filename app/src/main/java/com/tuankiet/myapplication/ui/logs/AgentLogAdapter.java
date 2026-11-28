package com.tuankiet.myapplication.ui.logs;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.tuankiet.myapplication.R;
import com.tuankiet.myapplication.model.AgentLog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Adapter hiển thị danh sách nhật ký AI Agent (F2 - Issue #23),
 * hỗ trợ bấm vào từng thẻ để xem/ẩn chi tiết Prompt và Kết quả trả về.
 */
public class AgentLogAdapter extends RecyclerView.Adapter<AgentLogAdapter.LogViewHolder> {

    private final List<AgentLog> logs = new ArrayList<>();
    private final Set<Long> expandedIds = new HashSet<>();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm • dd/MM", Locale.getDefault());

    public void setLogs(List<AgentLog> newLogs) {
        logs.clear();
        if (newLogs != null) {
            logs.addAll(newLogs);
        }
        expandedIds.clear();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_agent_log, parent, false);
        return new LogViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
        AgentLog log = logs.get(position);
        long itemKey = log.getId() > 0 ? log.getId() : position;

        String role = (log.getAgentRole() != null && !log.getAgentRole().trim().isEmpty())
                ? log.getAgentRole() : "AI Agent";
        holder.tvLogAgentRole.setText(role);

        String step = log.getStepName() != null ? log.getStepName() : "AI Step";
        holder.tvLogStepName.setText(step);

        String output = log.getOutputSnippet() != null ? log.getOutputSnippet() : "";
        boolean isError = step.contains("❌") || step.contains("[LỖI]")
                || output.startsWith("❌") || output.startsWith("[LỖI]");

        if (isError) {
            holder.tvLogStatus.setText("Lỗi");
            holder.tvLogStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.status_red));
            holder.tvLogStatus.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.status_red_bg)));
        } else {
            holder.tvLogStatus.setText("Thành công");
            holder.tvLogStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.status_green));
            holder.tvLogStatus.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.status_green_bg)));
        }

        holder.tvLogTime.setText(timeFormat.format(new Date(log.getTimestamp())));
        holder.tvLogMetrics.setText(String.format(Locale.US, "%,d tok • $%.4f • %dms",
                log.getTokensUsed(), log.getCostEstimate(), log.getDurationMs()));

        String prompt = (log.getPromptUsed() != null && !log.getPromptUsed().trim().isEmpty())
                ? log.getPromptUsed() : "(Không có prompt)";
        holder.tvLogPrompt.setText(prompt);
        holder.tvLogOutput.setText(!output.trim().isEmpty() ? output : "(Không có kết quả)");

        String model = (log.getModelName() != null && !log.getModelName().trim().isEmpty())
                ? log.getModelName() : "gemini-3.1-flash-lite";
        holder.tvLogModel.setText(model);
        holder.tvLogModelDetail.setText("Mô hình AI: " + model);
        holder.tvLogCostRateDetail.setText(String.format(Locale.US,
                "Tiêu thụ: %,d tokens • Chi phí ước tính: $%.4f USD",
                log.getTokensUsed(), log.getCostEstimate()));

        boolean isExpanded = expandedIds.contains(itemKey);
        holder.layoutLogDetail.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
        holder.tvLogExpandHint.setText(isExpanded ? "Thu gọn ▲" : "Chi tiết ▼");

        holder.itemView.setOnClickListener(v -> {
            if (expandedIds.contains(itemKey)) {
                expandedIds.remove(itemKey);
            } else {
                expandedIds.add(itemKey);
            }
            int adapterPos = holder.getAdapterPosition();
            if (adapterPos != RecyclerView.NO_POSITION) {
                notifyItemChanged(adapterPos);
            }
        });
    }

    @Override
    public int getItemCount() {
        return logs.size();
    }

    static class LogViewHolder extends RecyclerView.ViewHolder {
        final TextView tvLogAgentRole;
        final TextView tvLogStatus;
        final TextView tvLogTime;
        final TextView tvLogStepName;
        final TextView tvLogMetrics;
        final TextView tvLogModel;
        final TextView tvLogExpandHint;
        final LinearLayout layoutLogDetail;
        final TextView tvLogModelDetail;
        final TextView tvLogCostRateDetail;
        final TextView tvLogPrompt;
        final TextView tvLogOutput;

        LogViewHolder(@NonNull View itemView) {
            super(itemView);
            tvLogAgentRole = itemView.findViewById(R.id.tvLogAgentRole);
            tvLogStatus = itemView.findViewById(R.id.tvLogStatus);
            tvLogModel = itemView.findViewById(R.id.tvLogModel);
            tvLogTime = itemView.findViewById(R.id.tvLogTime);
            tvLogStepName = itemView.findViewById(R.id.tvLogStepName);
            tvLogMetrics = itemView.findViewById(R.id.tvLogMetrics);
            tvLogExpandHint = itemView.findViewById(R.id.tvLogExpandHint);
            layoutLogDetail = itemView.findViewById(R.id.layoutLogDetail);
            tvLogModelDetail = itemView.findViewById(R.id.tvLogModelDetail);
            tvLogCostRateDetail = itemView.findViewById(R.id.tvLogCostRateDetail);
            tvLogPrompt = itemView.findViewById(R.id.tvLogPrompt);
            tvLogOutput = itemView.findViewById(R.id.tvLogOutput);
        }
    }
}
