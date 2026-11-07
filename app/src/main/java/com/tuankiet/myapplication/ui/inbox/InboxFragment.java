package com.tuankiet.myapplication.ui.inbox;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.tuankiet.myapplication.R;
import com.tuankiet.myapplication.data.SecurePreferenceManager;
import com.tuankiet.myapplication.databinding.DialogReplyCommentBinding;
import com.tuankiet.myapplication.databinding.FragmentInboxBinding;
import com.tuankiet.myapplication.model.CommentItem;
import com.tuankiet.myapplication.repository.MockDataRepository;
import com.tuankiet.myapplication.util.RealSocialPublisher;

import java.util.List;

public class InboxFragment extends Fragment {

    private FragmentInboxBinding binding;
    private CommentAdapter adapter;
    private MockDataRepository repository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentInboxBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = MockDataRepository.getInstance(requireContext());

        setupRecyclerView();
        setupFilters();
        setupSwipeRefresh();
        setupAutoReplyBot();
        loadComments();
    }

    private void setupAutoReplyBot() {
        binding.btnRunControlledAutoReply.setOnClickListener(v -> {
            List<CommentItem> comments = repository.getComments();
            int autoRepliedCount = 0;
            int skippedRiskCount = 0;
            String threadsToken = SecurePreferenceManager.getInstance(requireContext()).getThreadsToken();

            for (CommentItem c : comments) {
                if (!c.isAnswered()) {
                    boolean isRisk = CommentItem.CATEGORY_NEGATIVE.equalsIgnoreCase(c.getCategory())
                            || CommentItem.CATEGORY_SPAM.equalsIgnoreCase(c.getCategory())
                            || c.isNegativeFlagged();

                    if (!isRisk) {
                        String reply = c.getAiSuggestedReply() != null ? c.getAiSuggestedReply() :
                                "Cảm ơn bạn đã quan tâm và để lại tương tác trên kênh!";
                        repository.replyToComment(c.getId(), reply);
                        c.setAnswered(true);
                        c.setReplyText(reply);
                        autoRepliedCount++;

                        // Nếu là bình luận thật từ Threads (có gắn ID:), bắn reply thật lên Threads API!
                        if (c.getTimeAgo() != null && c.getTimeAgo().contains("ID:") && !threadsToken.isEmpty()) {
                            String replyToId = c.getTimeAgo().substring(c.getTimeAgo().indexOf("ID:") + 3).trim();
                            RealSocialPublisher.replyToThreadsCommentViaApi(threadsToken, replyToId, reply, new RealSocialPublisher.ApiPublishCallback() {
                                @Override
                                public void onSuccess(String postId, String platformName) {
                                    if (getContext() != null) {
                                        Toast.makeText(getContext(), "✅ Bot đã trả lời thật lên Threads cho @" + c.getAuthorName(), Toast.LENGTH_SHORT).show();
                                    }
                                }

                                @Override
                                public void onError(String errorMessage) {
                                    if (getContext() != null) {
                                        Toast.makeText(getContext(), "⚠️ Lỗi gửi reply Threads: " + errorMessage, Toast.LENGTH_LONG).show();
                                    }
                                }
                            });
                        }
                    } else {
                        skippedRiskCount++;
                    }
                }
            }

            adapter.notifyDataSetChanged();

            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("🤖 Kết Quả Bot Tự Động Phản Hồi (F6)")
                    .setMessage("Tuân thủ nghiêm ngặt quy định F6 [B] Tự động trả lời có kiểm soát:\n\n"
                            + "✅ Đã tự động trả lời: " + autoRepliedCount + " bình luận an toàn (thuộc nhóm Khen ngợi & Câu hỏi thường gặp).\n\n"
                            + "🛡️ Đã chặn tự động và bỏ qua: " + skippedRiskCount + " bình luận (thuộc nhóm Tiêu cực / Spam / Rủi ro thương hiệu) để buộc con người duyệt thủ công!")
                    .setPositiveButton("Xác nhận", null)
                    .show();
        });
    }

    private void setupSwipeRefresh() {
        binding.swipeRefreshLayout.setColorSchemeColors(
                getResources().getColor(R.color.primary),
                getResources().getColor(R.color.status_blue)
        );

        binding.swipeRefreshLayout.setOnRefreshListener(() -> {
            String threadsToken = SecurePreferenceManager.getInstance(requireContext()).getThreadsToken();
            if (!threadsToken.isEmpty()) {
                // Có cấu hình Threads Token -> Quét bình luận thật từ bài viết trên Threads!
                RealSocialPublisher.fetchRealThreadsComments(requireContext(), threadsToken, (newCommentsCount, statusMessage) -> {
                    if (binding == null) return;
                    loadComments();
                    binding.swipeRefreshLayout.setRefreshing(false);
                    Toast.makeText(getContext(), statusMessage, Toast.LENGTH_LONG).show();
                });
            } else {
                binding.swipeRefreshLayout.postDelayed(() -> {
                    if (binding == null) return;
                    // Simulate new incoming comment from network when no Threads Token is configured
                    com.tuankiet.myapplication.model.CommentItem newComment = new com.tuankiet.myapplication.model.CommentItem(
                            0,
                            com.tuankiet.myapplication.model.SocialPlatform.LINKEDIN,
                            "5 Xu Hướng Trí Tuệ Nhân Tạo Năm 2026",
                            "Phan Thanh Tùng",
                            "T",
                            "Tính năng tự động hóa nội dung và trả lời an toàn này rất thiết thực cho doanh nghiệp!",
                            "Vừa xong",
                            com.tuankiet.myapplication.model.CommentItem.CATEGORY_PRAISE,
                            false,
                            null,
                            "Cảm ơn bạn Tùng đã dành lời khen! SocialAI luôn hướng đến trải nghiệm an toàn và hiệu quả nhất.",
                            false,
                            null
                    );

                    com.tuankiet.myapplication.data.DatabaseHelper.getInstance(requireContext()).insertComment(newComment);
                    com.tuankiet.myapplication.util.NotificationHelper.showCommentNotification(requireContext(), newComment);

                    loadComments();
                    binding.swipeRefreshLayout.setRefreshing(false);
                    Toast.makeText(getContext(), "🔔 Đã đồng bộ và nhận 1 bình luận mới từ LinkedIn!", Toast.LENGTH_SHORT).show();
                }, 800);
            }
        });
    }

    private void setupRecyclerView() {
        adapter = new CommentAdapter();
        binding.rvComments.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvComments.setAdapter(adapter);

        adapter.setOnCommentClickListener(this::openReplyBottomSheet);
    }

    private void setupFilters() {
        binding.chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.contains(R.id.chipFilterFacebook)) {
                adapter.applyFilter("FACEBOOK");
            } else if (checkedIds.contains(R.id.chipFilterLinkedIn)) {
                adapter.applyFilter("LINKEDIN");
            } else if (checkedIds.contains(R.id.chipFilterThreads)) { // Bổ sung xử lý lọc Threads
                adapter.applyFilter("THREADS");
            } else {
                adapter.applyFilter("ALL");
            }
        });
    }

    private void loadComments() {
        List<CommentItem> comments = repository.getComments();
        adapter.setComments(comments);
    }

    private void openReplyBottomSheet(CommentItem item) {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        DialogReplyCommentBinding dialogBinding = DialogReplyCommentBinding.inflate(getLayoutInflater());
        dialog.setContentView(dialogBinding.getRoot());

        dialogBinding.tvDialogPostTitle.setText("Về bài: " + item.getPostTitle());
        dialogBinding.tvDialogAuthorName.setText(item.getAuthorName() + " (" + item.getTimeAgo() + ")");
        dialogBinding.tvDialogCommentText.setText(item.getContent());
        dialogBinding.tvDialogCategoryBadge.setText(item.getCategory());

        // Safety rules checking
        boolean isNegative = CommentItem.CATEGORY_NEGATIVE.equalsIgnoreCase(item.getCategory())
                || CommentItem.CATEGORY_SPAM.equalsIgnoreCase(item.getCategory())
                || item.isNegativeFlagged();

        if (isNegative) {
            // Flagged: Show Alert Card, Hide AI Auto-suggest
            dialogBinding.cardSafetyAlert.setVisibility(View.VISIBLE);
            dialogBinding.tvSafetyAlertDetail.setText(
                    item.getRiskWarning() != null ? item.getRiskWarning() :
                            "CẢNH BÁO AN TOÀN AI: Bình luận này có dấu hiệu tiêu cực hoặc rủi ro uy tín thương hiệu. " +
                                    "Tính năng tự động điền gợi ý đã bị khóa. Vui lòng tự soạn phản hồi cẩn trọng.");
            dialogBinding.cardAiSuggestedReply.setVisibility(View.GONE);
            dialogBinding.etReplyInput.setHint("Nhập câu trả lời thủ công của bạn sau khi cân nhắc...");
        } else {
            // Safe: Show AI Suggestion
            dialogBinding.cardSafetyAlert.setVisibility(View.GONE);
            dialogBinding.cardAiSuggestedReply.setVisibility(View.VISIBLE);
            String suggested = item.getAiSuggestedReply() != null ? item.getAiSuggestedReply() :
                    "Cảm ơn bạn đã quan tâm và phản hồi bài viết!";
            dialogBinding.tvAiSuggestedText.setText(suggested);

            dialogBinding.btnApplySuggestion.setOnClickListener(v -> {
                dialogBinding.etReplyInput.setText(suggested);
            });
        }

        if (item.isAnswered() && item.getReplyText() != null) {
            dialogBinding.etReplyInput.setText(item.getReplyText());
        }

        dialogBinding.btnDismissDialog.setOnClickListener(v -> dialog.dismiss());

        dialogBinding.btnSendReply.setOnClickListener(v -> {
            String reply = dialogBinding.etReplyInput.getText() != null ?
                    dialogBinding.etReplyInput.getText().toString().trim() : "";
            if (reply.isEmpty()) {
                Toast.makeText(getContext(), "Vui lòng nhập nội dung câu trả lời!", Toast.LENGTH_SHORT).show();
                return;
            }

            repository.replyToComment(item.getId(), reply);
            item.setAnswered(true);
            item.setReplyText(reply);
            adapter.notifyDataSetChanged();
            dialog.dismiss();

            // Kiểm tra xem có phải comment thật từ Threads không để bắn API trả lời thật
            String threadsToken = SecurePreferenceManager.getInstance(requireContext()).getThreadsToken();
            if (item.getTimeAgo() != null && item.getTimeAgo().contains("ID:") && !threadsToken.isEmpty()) {
                String replyToId = item.getTimeAgo().substring(item.getTimeAgo().indexOf("ID:") + 3).trim();
                Toast.makeText(getContext(), "⏳ Đang gửi câu trả lời trực tiếp lên Threads...", Toast.LENGTH_SHORT).show();
                RealSocialPublisher.replyToThreadsCommentViaApi(threadsToken, replyToId, reply, new RealSocialPublisher.ApiPublishCallback() {
                    @Override
                    public void onSuccess(String postId, String platformName) {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "🎉 Đã trả lời thật trên Threads tới @" + item.getAuthorName() + " (ID: " + postId + ")!", Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onError(String errorMessage) {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "⚠️ Lỗi gửi reply lên Threads: " + errorMessage, Toast.LENGTH_LONG).show();
                        }
                    }
                });
            } else {
                Toast.makeText(getContext(), "🎉 Đã gửi phản hồi thành công đến " + item.getAuthorName() + "!", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadComments();
        RealSocialPublisher.setOnCommentsUpdatedListener(() -> {
            if (binding != null && isAdded()) {
                loadComments();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        RealSocialPublisher.setOnCommentsUpdatedListener(null);
        binding = null;
    }
}
