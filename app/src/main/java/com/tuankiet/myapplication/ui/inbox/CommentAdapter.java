package com.tuankiet.myapplication.ui.inbox;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.tuankiet.myapplication.R;
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
    private String currentFilter = "ALL";
    private OnCommentClickListener listener;

    public void setComments(List<CommentItem> items) {
        allComments.clear();
        if (items != null) {
            allComments.addAll(items);
        }
        applyFilter(currentFilter);
    }

    public void setOnCommentClickListener(OnCommentClickListener listener) {
        this.listener = listener;
    }

    public void applyFilter(String filter) {
        currentFilter = filter;
        displayedComments.clear();

        for (CommentItem item : allComments) {
            if ("ALL".equalsIgnoreCase(filter)) {
                if (item.getPlatform() != SocialPlatform.LINKEDIN) {
                    displayedComments.add(item);
                }
            } else if ("FACEBOOK".equalsIgnoreCase(filter) && item.getPlatform() == SocialPlatform.FACEBOOK) {
                displayedComments.add(item);
            } else if ("LINKEDIN".equalsIgnoreCase(filter) && item.getPlatform() == SocialPlatform.LINKEDIN) {
                displayedComments.add(item);
            } else if ("THREADS".equalsIgnoreCase(filter) && item.getPlatform() == SocialPlatform.THREADS) { // Bổ sung Threads
                displayedComments.add(item);
            }
        }
        notifyDataSetChanged();
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
            } else if (item.getPlatform() == SocialPlatform.LINKEDIN) {
                binding.ivCommentPlatform.setImageResource(R.drawable.ic_linkedin);
            } else if (item.getPlatform() == SocialPlatform.THREADS) {
                binding.ivCommentPlatform.setImageResource(R.drawable.ic_threads);
            }

            binding.tvPostContextTitle.setText("Bài: " + item.getPostTitle());
            binding.tvAuthorName.setText(item.getAuthorName());
            binding.tvAuthorAvatar.setText(item.getAuthorInitial());
            binding.tvCommentTime.setText(item.getTimeAgo());
            binding.tvCommentContent.setText(item.getContent());

            // AI Category Badge styling (rounded pill)
            String cat = item.getCategory();
            binding.tvCategoryBadge.setBackgroundResource(R.drawable.bg_soft_pill);
            if (CommentItem.CATEGORY_PRAISE.equalsIgnoreCase(cat)) {
                binding.tvCategoryBadge.setText("Khen ngợi");
                binding.tvCategoryBadge.setTextColor(Color.parseColor("#0D9488"));
                binding.tvCategoryBadge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#CCFBF1")));
            } else if (CommentItem.CATEGORY_QUESTION.equalsIgnoreCase(cat)) {
                binding.tvCategoryBadge.setText("Câu hỏi");
                binding.tvCategoryBadge.setTextColor(Color.parseColor("#0369A1"));
                binding.tvCategoryBadge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#EBF7FD")));
            } else if (CommentItem.CATEGORY_NEGATIVE.equalsIgnoreCase(cat)) {
                binding.tvCategoryBadge.setText("Tiêu cực");
                binding.tvCategoryBadge.setTextColor(Color.parseColor("#E11D48"));
                binding.tvCategoryBadge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FFE4E6")));
            } else {
                binding.tvCategoryBadge.setText("Spam");
                binding.tvCategoryBadge.setTextColor(Color.parseColor("#78716C"));
                binding.tvCategoryBadge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#F3EFE7")));
            }

            // Reply status (clean text color without yellow highlight box)
            binding.tvReplyStatusBadge.setBackground(null);
            if (item.isAnswered()) {
                binding.tvReplyStatusBadge.setText("Đã trả lời");
                binding.tvReplyStatusBadge.setTextColor(Color.parseColor("#0D9488"));
            } else {
                binding.tvReplyStatusBadge.setText("Chờ xử lý");
                binding.tvReplyStatusBadge.setTextColor(Color.parseColor("#D97706"));
            }

            // Negative warning banner
            if (item.isNegativeFlagged()) {
                binding.layoutAlertWarning.setVisibility(View.VISIBLE);
                binding.tvAlertWarningText.setText("Cảnh báo AI: Sắc thái tiêu cực • Khóa tự động trả lời");
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
