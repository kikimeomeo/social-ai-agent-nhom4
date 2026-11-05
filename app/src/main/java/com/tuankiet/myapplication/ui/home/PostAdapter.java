package com.tuankiet.myapplication.ui.home;

import android.graphics.Color;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.tuankiet.myapplication.databinding.ItemPostCardBinding;
import com.tuankiet.myapplication.model.Post;

import java.util.ArrayList;
import java.util.List;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    public interface OnPostClickListener {
        void onPostClick(Post post);
    }

    private final List<Post> posts = new ArrayList<>();
    private OnPostClickListener listener;

    public void setPosts(List<Post> newPosts) {
        posts.clear();
        if (newPosts != null) {
            posts.addAll(newPosts);
        }
        notifyDataSetChanged();
    }

    public void setOnPostClickListener(OnPostClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPostCardBinding binding = ItemPostCardBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new PostViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        holder.bind(posts.get(position));
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    class PostViewHolder extends RecyclerView.ViewHolder {
        private final ItemPostCardBinding binding;

        PostViewHolder(ItemPostCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Post post) {
            binding.tvTopicTitle.setText(post.getTopic());
            binding.tvToneBadge.setText(post.getTone() != null ? post.getTone() : "Chuyên gia");

            String snippet = !post.getContentFacebook().isEmpty() ?
                    post.getContentFacebook() : post.getContentLinkedIn();
            binding.tvContentSnippet.setText(snippet.replace("\n", " "));

            binding.ivFbIcon.setVisibility(post.isTargetFacebook() ? View.VISIBLE : View.GONE);
            binding.ivLiIcon.setVisibility(post.isTargetLinkedIn() ? View.VISIBLE : View.GONE);

            // Status Badge
            String status = post.getStatus();
            if (Post.STATUS_PUBLISHED.equals(status)) {
                binding.tvStatusBadge.setText("Đã Đăng");
                binding.tvStatusBadge.setTextColor(Color.parseColor("#10B981"));
                binding.tvStatusBadge.setBackgroundColor(Color.parseColor("#ECFDF5"));
            } else if (Post.STATUS_DRAFT.equals(status)) {
                binding.tvStatusBadge.setText("Bản Nháp");
                binding.tvStatusBadge.setTextColor(Color.parseColor("#F59E0B"));
                binding.tvStatusBadge.setBackgroundColor(Color.parseColor("#FFFBEB"));
            } else {
                binding.tvStatusBadge.setText("Thất Bại");
                binding.tvStatusBadge.setTextColor(Color.parseColor("#EF4444"));
                binding.tvStatusBadge.setBackgroundColor(Color.parseColor("#FEF2F2"));
            }

            if (post.getReachCount() > 0) {
                binding.tvMetrics.setText("👁 " + post.getReachCount() + "  •  👍 " + post.getLikeCount() + "  •  💬 " + post.getCommentCount());
            } else {
                binding.tvMetrics.setText("Chưa phát hành số liệu");
            }

            CharSequence timeStr = DateUtils.getRelativeTimeSpanString(
                    post.getCreatedAt(), System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS);
            binding.tvDate.setText(timeStr);

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPostClick(post);
                }
            });
        }
    }
}

