package com.tuankiet.myapplication.ui.inbox;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.tuankiet.myapplication.R;
import com.tuankiet.myapplication.agent.CommentSafetyPolicy;
import com.tuankiet.myapplication.databinding.ItemCommentCardBinding;
import com.tuankiet.myapplication.model.CommentItem;
import com.tuankiet.myapplication.model.SocialPlatform;

import java.util.ArrayList;
import java.util.List;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    public interface OnCommentClickListener {
        void onCommentClick(CommentItem item);
    }

    private final List<CommentItem> allComments = new ArrayList<>();
    private final List<CommentItem> displayedComments = new ArrayList<>();
    private String currentChannelFilter = InboxFilterHelper.CHANNEL_ALL;
    private String currentPostFilter = InboxFilterHelper.POST_ALL;
    private String currentStatusFilter = InboxFilterHelper.STATUS_ALL;
    private OnCommentClickListener listener;

    public void setComments(List<CommentItem> items) {
        allComments.clear();
        if (items != null) {
            allComments.addAll(items);
        }
        rebuildFilteredList();
    }

    public void setOnCommentClickListener(OnCommentClickListener listener) {
        this.listener = listener;
    }

    public void applyFilter(String channelFilter) {
        applyFilters(channelFilter, currentPostFilter, currentStatusFilter);
    }

    public void applyFilters(String channelFilter, String postFilter, String statusFilter) {
        currentChannelFilter = channelFilter != null ? channelFilter : InboxFilterHelper.CHANNEL_ALL;
        currentPostFilter = postFilter != null ? postFilter : InboxFilterHelper.POST_ALL;
        currentStatusFilter = statusFilter != null ? statusFilter : InboxFilterHelper.STATUS_ALL;
        rebuildFilteredList();
    }

    private void rebuildFilteredList() {
        displayedComments.clear();
        displayedComments.addAll(
                InboxFilterHelper.filter(allComments, currentChannelFilter, currentPostFilter, currentStatusFilter)
        );
        notifyDataSetChanged();
    }

    public int getFilteredCount() {
        return displayedComments.size();
    }

    public int getRiskAlertCountInDisplayed() {
        int count = 0;
        for (CommentItem item : displayedComments) {
            if (CommentSafetyPolicy.shouldShowSafetyAlert(item)) {
                count++;
            }
        }
        return count;
    }

    public static void bindCategoryBadge(TextView badge, CommentItem item) {
        if (badge == null || item == null) return;
        String cat = item.getCategory();
        badge.setBackgroundResource(R.drawable.bg_soft_pill);

        if (CommentItem.CATEGORY_PRAISE.equalsIgnoreCase(cat)) {
            badge.setText("Khen ngợi");
            badge.setTextColor(Color.parseColor("#0D9488"));
            badge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#CCFBF1")));
        } else if (CommentItem.CATEGORY_QUESTION.equalsIgnoreCase(cat)) {
            String label = (item.isFaq() && !item.hasPersonalInfo() && !CommentSafetyPolicy.containsPersonalInfo(item.getContent()))
                    ? "Câu hỏi • FAQ"
                    : "Câu hỏi";
            badge.setText(label);
            badge.setTextColor(Color.parseColor("#0369A1"));
            badge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E0F2FE")));
        } else if (CommentItem.CATEGORY_SUGGESTION.equalsIgnoreCase(cat)) {
            badge.setText("Góp ý");
            badge.setTextColor(Color.parseColor("#6D28D9"));
            badge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#EDE9FE")));
        } else if (CommentItem.CATEGORY_NEGATIVE.equalsIgnoreCase(cat)) {
            badge.setText("Tiêu cực");
            badge.setTextColor(Color.parseColor("#E11D48"));
            badge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFE4E6")));
        } else {
            badge.setText("Spam");
            badge.setTextColor(Color.parseColor("#78716C"));
            badge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F3EFE7")));
        }
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCommentCardBinding binding = ItemCommentCardBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new CommentViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        holder.bind(displayedComments.get(position));
    }

    @Override
    public int getItemCount() {
        return displayedComments.size();
    }

    class CommentViewHolder extends RecyclerView.ViewHolder {
        private final ItemCommentCardBinding binding;

        CommentViewHolder(ItemCommentCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(CommentItem item) {
            if (item.getPlatform() == SocialPlatform.FACEBOOK) {
                binding.ivCommentPlatform.setImageResource(R.drawable.ic_facebook);
            } else {
                binding.ivCommentPlatform.setImageResource(R.drawable.ic_threads);
            }

            String postTitle = item.getPostTitle() != null ? item.getPostTitle() : "Bài viết";
            binding.tvPostContextTitle.setText("Bài: " + postTitle);
            binding.tvAuthorName.setText(item.getAuthorName());
            binding.tvAuthorAvatar.setText(item.getAuthorInitial());
            binding.tvCommentTime.setText("• " + (item.getTimeAgo() != null ? item.getTimeAgo() : "Vừa xong"));
            binding.tvCommentContent.setText(item.getContent());

            // 5-Category AI Badge styling
            bindCategoryBadge(binding.tvCategoryBadge, item);

            // Reply Status
            binding.tvReplyStatusBadge.setBackground(null);
            if (item.isAnswered()) {
                binding.tvReplyStatusBadge.setText(item.isAutoReplied() ? "Bot đã trả lời" : "Đã trả lời");
                binding.tvReplyStatusBadge.setTextColor(Color.parseColor("#0D9488"));
                binding.tvTapHint.setText("Xem phản hồi");
            } else {
                binding.tvReplyStatusBadge.setText("Chưa trả lời");
                binding.tvReplyStatusBadge.setTextColor(Color.parseColor("#D97706"));
                binding.tvTapHint.setText("Chạm để duyệt & trả lời");
            }

            // AI Confidence Meta
            int confidencePct = (int) Math.round(item.getAiConfidence() * 100.0);
            if (item.getAiConfidence() < CommentSafetyPolicy.MIN_AUTO_REPLY_CONFIDENCE) {
                binding.tvConfidenceMeta.setText("• AI chưa chắc (" + confidencePct + "%)");
                binding.tvConfidenceMeta.setTextColor(Color.parseColor("#D97706"));
            } else {
                binding.tvConfidenceMeta.setText("• AI " + confidencePct + "%");
                binding.tvConfidenceMeta.setTextColor(Color.parseColor("#8C837A"));
            }

            // Safety Warning Banner (Negative / Spam / PII / Low AI Confidence)
            if (CommentSafetyPolicy.shouldShowSafetyAlert(item)) {
                binding.layoutAlertWarning.setVisibility(View.VISIBLE);
                binding.tvAlertWarningText.setText(CommentSafetyPolicy.getSafetyBannerSummary(item));
            } else {
                binding.layoutAlertWarning.setVisibility(View.GONE);
            }

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCommentClick(item);
                }
            });
        }
    }
}
