package com.monarch.software.phase2;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.monarch.software.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CommentsActivity extends AppCompatActivity {

    // ─────────────────────────────────────────────────────────────────
    // Data model
    // ─────────────────────────────────────────────────────────────────
    static class Comment {
        String handle, avatarEmoji, text, timeAgo;
        int    avatarColor, likeCount;
        boolean liked;
        final List<Comment> replies = new ArrayList<>();

        Comment(String handle, String avatarEmoji, int avatarColor,
                String text, String timeAgo, int likeCount) {
            this.handle      = handle;
            this.avatarEmoji = avatarEmoji;
            this.avatarColor = avatarColor;
            this.text        = text;
            this.timeAgo     = timeAgo;
            this.likeCount   = likeCount;
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // State
    // ─────────────────────────────────────────────────────────────────
    private final List<Comment> comments = new ArrayList<>();
    private CommentsAdapter adapter;
    private RecyclerView rv;
    private EditText etComment;
    private Comment replyingTo = null;  // non-null → input is replying to this comment

    // ─────────────────────────────────────────────────────────────────
    // onCreate
    // ─────────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comments);

        Intent intent = getIntent();
        int    postId         = intent.getIntExtra("postId", 0);
        String postHandle     = intent.getStringExtra("postHandle");
        String postCaption    = intent.getStringExtra("postCaption");
        String postImageEmoji = intent.getStringExtra("postImageEmoji");
        int    postImageColor = intent.getIntExtra("postImageColor", 0xFF222233);

        // Back button
        findViewById(R.id.tv_back).setOnClickListener(v -> finish());

        // Post preview strip
        View     postBg      = findViewById(R.id.view_post_color_preview);
        TextView tvEmoji     = findViewById(R.id.tv_post_emoji_preview);
        TextView tvHdl       = findViewById(R.id.tv_post_handle_preview);
        TextView tvCap       = findViewById(R.id.tv_post_caption_preview);
        if (postBg  != null) postBg.setBackgroundColor(postImageColor);
        if (tvEmoji != null) tvEmoji.setText(postImageEmoji);
        if (tvHdl   != null) tvHdl.setText(postHandle);
        if (tvCap   != null) tvCap.setText(postCaption);

        // Generate seeded comments
        generateComments(postId);

        // RecyclerView
        rv = findViewById(R.id.rv_comments);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CommentsAdapter(comments, this::startReply);
        rv.setAdapter(adapter);

        // Bottom input bar
        etComment = findViewById(R.id.et_comment_input);
        TextView tvPost = findViewById(R.id.tv_post_btn);

        tvPost.setOnClickListener(v -> submitComment());
        etComment.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) { submitComment(); return true; }
            return false;
        });
    }

    // ─────────────────────────────────────────────────────────────────
    // Reply flow
    // ─────────────────────────────────────────────────────────────────
    private void startReply(Comment parent) {
        replyingTo = parent;
        etComment.setHint("Reply to @" + parent.handle + "…");
        etComment.setText("@" + parent.handle + " ");
        etComment.requestFocus();
        etComment.setSelection(etComment.getText().length());
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) imm.showSoftInput(etComment, InputMethodManager.SHOW_IMPLICIT);
    }

    private void submitComment() {
        String text = etComment.getText().toString().trim();
        if (text.isEmpty()) return;

        Comment c = new Comment("you", "😊", 0xFF9C27B0, text, "now", 0);

        if (replyingTo != null) {
            replyingTo.replies.add(c);
            int idx = comments.indexOf(replyingTo);
            if (idx >= 0) adapter.notifyItemChanged(idx);
            replyingTo = null;
            etComment.setHint("Add a comment…");
        } else {
            comments.add(0, c);
            adapter.notifyItemInserted(0);
            rv.scrollToPosition(0);
        }

        etComment.setText("");
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(etComment.getWindowToken(), 0);
    }

    // ─────────────────────────────────────────────────────────────────
    // Comment generation (seeded per post so they're consistent)
    // ─────────────────────────────────────────────────────────────────
    private void generateComments(int postId) {
        Random rng = new Random(postId * 31L + 7);
        String[] times = {"1m", "5m", "12m", "23m", "1h", "2h", "3h", "5h", "8h", "1d", "2d"};
        int count = 5 + rng.nextInt(11); // 5-15 comments

        for (int i = 0; i < count; i++) {
            Object[] u    = InstaFeedActivity.USERS[rng.nextInt(InstaFeedActivity.USERS.length)];
            String   text = InstaFeedActivity.COMMENT_TEXTS[rng.nextInt(InstaFeedActivity.COMMENT_TEXTS.length)];
            Comment  c    = new Comment(
                    (String) u[1], (String) u[2], (int) u[3],
                    text, times[rng.nextInt(times.length)], rng.nextInt(200));

            // 40% chance of replies (1-3)
            if (rng.nextInt(10) < 4) {
                int rc = 1 + rng.nextInt(3);
                for (int j = 0; j < rc; j++) {
                    Object[] ru = InstaFeedActivity.USERS[rng.nextInt(InstaFeedActivity.USERS.length)];
                    String   rt = InstaFeedActivity.COMMENT_TEXTS[rng.nextInt(InstaFeedActivity.COMMENT_TEXTS.length)];
                    c.replies.add(new Comment(
                            (String) ru[1], (String) ru[2], (int) ru[3],
                            rt, times[rng.nextInt(times.length)], rng.nextInt(50)));
                }
            }
            comments.add(c);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Adapter
    // ─────────────────────────────────────────────────────────────────
    interface OnReplyCallback { void onReply(Comment comment); }

    static class CommentsAdapter extends RecyclerView.Adapter<CommentsAdapter.CommentVH> {

        private final List<Comment>    items;
        private final OnReplyCallback  replyCallback;

        CommentsAdapter(List<Comment> items, OnReplyCallback cb) {
            this.items         = items;
            this.replyCallback = cb;
        }

        @NonNull @Override
        public CommentVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_comment, parent, false);
            return new CommentVH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull CommentVH h, int pos) {
            h.bind(items.get(pos), replyCallback);
        }

        @Override public int getItemCount() { return items.size(); }

        // ── ViewHolder ──────────────────────────────────────────────
        static class CommentVH extends RecyclerView.ViewHolder {
            TextView    tvAvatar, tvHandle, tvText, tvTime, tvLike, tvLikeCount, tvReply;
            LinearLayout llReplies;

            CommentVH(View v) {
                super(v);
                tvAvatar    = v.findViewById(R.id.tv_comment_avatar);
                tvHandle    = v.findViewById(R.id.tv_comment_handle);
                tvText      = v.findViewById(R.id.tv_comment_text);
                tvTime      = v.findViewById(R.id.tv_comment_time);
                tvLike      = v.findViewById(R.id.tv_comment_like);
                tvLikeCount = v.findViewById(R.id.tv_comment_like_count);
                tvReply     = v.findViewById(R.id.tv_comment_reply);
                llReplies   = v.findViewById(R.id.ll_replies);
            }

            void bind(Comment c, OnReplyCallback replyCallback) {
                // Avatar
                GradientDrawable bg = new GradientDrawable();
                bg.setShape(GradientDrawable.OVAL);
                bg.setColor(c.avatarColor);
                tvAvatar.setBackground(bg);
                tvAvatar.setText(c.avatarEmoji);

                tvHandle.setText(c.handle);
                tvText.setText(c.text);
                tvTime.setText(c.timeAgo);

                // Like
                refreshLike(c);
                tvLike.setOnClickListener(v -> {
                    c.liked = !c.liked;
                    c.likeCount = Math.max(0, c.likeCount + (c.liked ? 1 : -1));
                    refreshLike(c);
                });

                // Reply
                tvReply.setOnClickListener(v -> { if (replyCallback != null) replyCallback.onReply(c); });

                // Replies
                llReplies.removeAllViews();
                for (Comment reply : c.replies) {
                    View rv = LayoutInflater.from(itemView.getContext())
                            .inflate(R.layout.item_comment_reply, llReplies, false);
                    bindReply(rv, reply);
                    llReplies.addView(rv);
                }
            }

            void refreshLike(Comment c) {
                tvLike.setText(c.liked ? "❤" : "♡");
                tvLike.setTextColor(c.liked ? 0xFFEE2244 : 0xFF8A8A9A);
                tvLikeCount.setText(c.likeCount > 0 ? String.valueOf(c.likeCount) : "");
            }

            void bindReply(View v, Comment r) {
                TextView tvAv   = v.findViewById(R.id.tv_reply_avatar);
                TextView tvHdl  = v.findViewById(R.id.tv_reply_handle);
                TextView tvTxt  = v.findViewById(R.id.tv_reply_text);
                TextView tvTm   = v.findViewById(R.id.tv_reply_time);
                TextView tvLk   = v.findViewById(R.id.tv_reply_like);
                TextView tvLkCt = v.findViewById(R.id.tv_reply_like_count);

                GradientDrawable bg = new GradientDrawable();
                bg.setShape(GradientDrawable.OVAL);
                bg.setColor(r.avatarColor);
                tvAv.setBackground(bg);
                tvAv.setText(r.avatarEmoji);
                tvHdl.setText(r.handle);
                tvTxt.setText(r.text);
                tvTm.setText(r.timeAgo);

                refreshReplyLike(tvLk, tvLkCt, r);
                tvLk.setOnClickListener(lv -> {
                    r.liked = !r.liked;
                    r.likeCount = Math.max(0, r.likeCount + (r.liked ? 1 : -1));
                    refreshReplyLike(tvLk, tvLkCt, r);
                });
            }

            void refreshReplyLike(TextView tvLk, TextView tvLkCt, Comment r) {
                tvLk.setText(r.liked ? "❤" : "♡");
                tvLk.setTextColor(r.liked ? 0xFFEE2244 : 0xFF8A8A9A);
                tvLkCt.setText(r.likeCount > 0 ? String.valueOf(r.likeCount) : "");
            }
        }
    }
}
