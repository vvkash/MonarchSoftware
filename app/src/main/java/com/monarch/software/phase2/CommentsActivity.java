package com.monarch.software.phase2;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.monarch.software.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CommentsActivity extends AppCompatActivity {

    private static final String STATE_REPLY_PARENT_POSITION =
            "state_reply_parent_position";
    private static final String STATE_COMMENT_DRAFT = "state_comment_draft";

    private static final class Comment {
        final SocialFeedContent.User user;
        final String text;
        final String timeAgo;
        int likeCount;
        boolean liked;
        final List<Comment> replies = new ArrayList<>();

        Comment(SocialFeedContent.User user, String text, String timeAgo, int likeCount) {
            this.user = user;
            this.text = text;
            this.timeAgo = timeAgo;
            this.likeCount = likeCount;
        }
    }

    private final List<Comment> comments = new ArrayList<>();

    private SocialTelemetry telemetry;
    private CommentsAdapter adapter;
    private RecyclerView recyclerView;
    private LinearLayoutManager layoutManager;
    private EditText commentInput;
    private Comment replyingTo;
    private int replyingToPosition = -1;
    private int postId;
    private String postHandle;
    private long screenOpenedAt;
    private long resumedAt;
    private int maxCommentDepth;
    private int lastDepthBucket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        configureSystemBars();
        setContentView(R.layout.activity_comments);

        Intent intent = getIntent();
        postId = intent.getIntExtra("postId", -1);
        postHandle = intent.getStringExtra("postHandle");
        String postCaption = intent.getStringExtra("postCaption");
        int postMediaResId = intent.getIntExtra(
                "postMediaResId", R.drawable.social_workspace);
        long sessionStart = intent.getLongExtra(
                InstaFeedActivity.EXTRA_SESSION_START, System.currentTimeMillis());

        screenOpenedAt = System.currentTimeMillis();
        telemetry = new SocialTelemetry(this, "comments", sessionStart);

        ImageView preview = findViewById(R.id.iv_post_preview);
        preview.setImageResource(postMediaResId);
        ((TextView) findViewById(R.id.tv_post_handle_preview)).setText(postHandle);
        ((TextView) findViewById(R.id.tv_post_caption_preview)).setText(postCaption);
        findViewById(R.id.post_preview).setOnClickListener(view -> log(
                "POST_PREVIEW_TAP", "post_preview", null, 0, 0));

        findViewById(R.id.btn_back).setOnClickListener(view -> {
            log("BACK_TAP", "back_button", null, 0, 0);
            finish();
        });

        generateComments(postId);
        recyclerView = findViewById(R.id.rv_comments);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new CommentsAdapter(comments, new CommentsAdapter.Listener() {
            @Override
            public void onReply(Comment comment, int parentPosition) {
                startReply(comment, parentPosition);
            }

            @Override
            public void onAction(String eventType, Comment comment,
                                 int parentPosition, int replyPosition) {
                String target = replyPosition >= 0 ? "reply" : "comment";
                String details = "parent_position=" + parentPosition
                        + ";reply_position=" + replyPosition
                        + ";author=" + comment.user.handle;
                log(eventType, target, details, parentPosition, 0);
            }
        });
        recyclerView.setAdapter(adapter);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                String state = newState == RecyclerView.SCROLL_STATE_DRAGGING
                        ? "dragging"
                        : newState == RecyclerView.SCROLL_STATE_SETTLING
                                ? "settling"
                                : "idle";
                log(
                        "COMMENT_SCROLL_STATE",
                        "comment_list",
                        "state=" + state,
                        Math.max(0, layoutManager.findFirstVisibleItemPosition()),
                        0);
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                trackCommentDepth(dy);
            }
        });

        commentInput = findViewById(R.id.et_comment_input);
        if (savedInstanceState != null) {
            int restoredParentPosition =
                    savedInstanceState.getInt(STATE_REPLY_PARENT_POSITION, -1);
            if (restoredParentPosition >= 0
                    && restoredParentPosition < comments.size()) {
                replyingToPosition = restoredParentPosition;
                replyingTo = comments.get(restoredParentPosition);
                commentInput.setHint("Reply to @" + replyingTo.user.handle + "…");
            }
            String draft = savedInstanceState.getString(STATE_COMMENT_DRAFT);
            if (draft != null) {
                commentInput.setText(draft);
                commentInput.setSelection(draft.length());
            }
        }
        commentInput.setOnFocusChangeListener((view, hasFocus) -> {
            if (hasFocus) log("COMMENT_INPUT_FOCUS", "comment_input", null, 0, 0);
        });
        findViewById(R.id.tv_post_btn).setOnClickListener(view -> submitComment());
        commentInput.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                submitComment();
                return true;
            }
            return false;
        });

        log(
                "SCREEN_OPEN",
                "comments",
                "generated_comment_count=" + comments.size(),
                0,
                0);
    }

    private void configureSystemBars() {
        Window window = getWindow();
        boolean darkMode = (getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        int flags = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.setStatusBarColor(
                    ContextCompat.getColor(this, R.color.social_surface));
            if (!darkMode) {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            }
        } else {
            window.setStatusBarColor(0xFF212121);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            window.setNavigationBarColor(
                    ContextCompat.getColor(this, R.color.social_surface));
            if (!darkMode) {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }
        } else {
            window.setNavigationBarColor(0xFF212121);
        }
        window.getDecorView().setSystemUiVisibility(flags);
    }

    private void startReply(Comment parent, int parentPosition) {
        replyingTo = parent;
        replyingToPosition = parentPosition;
        commentInput.setHint("Reply to @" + parent.user.handle + "…");
        commentInput.setText("@" + parent.user.handle + " ");
        commentInput.requestFocus();
        commentInput.setSelection(commentInput.getText().length());
        log(
                "REPLY_START",
                "comment",
                "parent_position=" + parentPosition + ";author=" + parent.user.handle,
                parentPosition,
                0);
        InputMethodManager keyboard =
                (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (keyboard != null) {
            keyboard.showSoftInput(commentInput, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void submitComment() {
        String text = commentInput.getText().toString().trim();
        if (text.isEmpty()) {
            log("COMMENT_SUBMIT_EMPTY", "comment_input", null, 0, 0);
            return;
        }

        SocialFeedContent.User currentUser = new SocialFeedContent.User(
                "You", "you", R.drawable.social_workspace_thumb, false);
        Comment comment = new Comment(currentUser, text, "now", 0);
        boolean isReply = replyingTo != null;
        int submittedParentPosition = isReply ? comments.indexOf(replyingTo) : -1;

        if (isReply) {
            replyingTo.replies.add(comment);
            if (submittedParentPosition >= 0) {
                adapter.notifyItemChanged(submittedParentPosition);
            }
        } else {
            comments.add(0, comment);
            adapter.notifyItemInserted(0);
            recyclerView.scrollToPosition(0);
        }

        log(
                isReply ? "REPLY_SUBMIT" : "COMMENT_SUBMIT",
                "comment_input",
                "text_length=" + text.length()
                        + ";parent_position=" + submittedParentPosition,
                Math.max(0, submittedParentPosition),
                0);

        replyingTo = null;
        replyingToPosition = -1;
        commentInput.setHint("Add a comment…");
        commentInput.setText("");
        InputMethodManager keyboard =
                (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (keyboard != null) {
            keyboard.hideSoftInputFromWindow(commentInput.getWindowToken(), 0);
        }
    }

    private void generateComments(int seedPostId) {
        Random random = new Random(seedPostId * 31L + 7);
        String[] times = {
            "2m", "7m", "14m", "28m", "46m", "1h", "2h", "4h", "7h", "1d", "2d"
        };
        int count = 7 + random.nextInt(8);

        for (int i = 0; i < count; i++) {
            SocialFeedContent.User user =
                    SocialFeedContent.USERS[random.nextInt(SocialFeedContent.USERS.length)];
            String text = SocialFeedContent.COMMENT_TEXTS[
                    random.nextInt(SocialFeedContent.COMMENT_TEXTS.length)];
            Comment comment = new Comment(
                    user,
                    text,
                    times[random.nextInt(times.length)],
                    random.nextInt(86));

            if (random.nextInt(10) < 4) {
                int replyCount = 1 + random.nextInt(2);
                for (int j = 0; j < replyCount; j++) {
                    SocialFeedContent.User replyUser =
                            SocialFeedContent.USERS[
                                    random.nextInt(SocialFeedContent.USERS.length)];
                    String replyText = SocialFeedContent.COMMENT_TEXTS[
                            random.nextInt(SocialFeedContent.COMMENT_TEXTS.length)];
                    comment.replies.add(new Comment(
                            replyUser,
                            replyText,
                            times[random.nextInt(times.length)],
                            random.nextInt(24)));
                }
            }
            comments.add(comment);
        }
    }

    private void trackCommentDepth(int dy) {
        int lastVisible = layoutManager.findLastVisibleItemPosition();
        if (lastVisible < 0 || comments.isEmpty()) return;
        int depth = Math.min(
                100, Math.round((lastVisible + 1) * 100f / comments.size()));
        maxCommentDepth = Math.max(maxCommentDepth, depth);
        int bucket = maxCommentDepth / 10 * 10;
        if (bucket <= lastDepthBucket) return;
        lastDepthBucket = bucket;
        log(
                "COMMENT_SCROLL_DEPTH",
                "comment_list",
                "percent=" + bucket + ";dy=" + dy,
                lastVisible,
                0);
    }

    private void log(String eventType, String target, String details,
                     int position, long duration) {
        telemetry.log(
                eventType,
                postId,
                postHandle,
                position,
                duration,
                target,
                details);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (telemetry != null) telemetry.recordTouch(event, "social_comments", 0);
        return super.dispatchTouchEvent(event);
    }

    @Override
    protected void onResume() {
        super.onResume();
        resumedAt = System.currentTimeMillis();
        if (telemetry != null) {
            log("SCREEN_RESUME", "comments", null, 0, 0);
        }
    }

    @Override
    protected void onPause() {
        if (telemetry != null) {
            log(
                    "SCREEN_PAUSE",
                    "comments",
                    "max_scroll_percent=" + maxCommentDepth,
                    maxCommentDepth,
                    Math.max(0, System.currentTimeMillis() - resumedAt));
        }
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt(
                STATE_REPLY_PARENT_POSITION,
                replyingTo == null ? -1 : comments.indexOf(replyingTo));
        if (commentInput != null) {
            outState.putString(
                    STATE_COMMENT_DRAFT,
                    commentInput.getText().toString());
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        if (telemetry != null) {
            log(
                    "SCREEN_CLOSE",
                    "comments",
                    "max_scroll_percent=" + maxCommentDepth,
                    maxCommentDepth,
                    System.currentTimeMillis() - screenOpenedAt);
            telemetry.close();
        }
        super.onDestroy();
    }

    private static final class CommentsAdapter
            extends RecyclerView.Adapter<CommentsAdapter.CommentViewHolder> {

        interface Listener {
            void onReply(Comment comment, int parentPosition);
            void onAction(String eventType, Comment comment,
                          int parentPosition, int replyPosition);
        }

        private final List<Comment> comments;
        private final Listener listener;

        CommentsAdapter(List<Comment> comments, Listener listener) {
            this.comments = comments;
            this.listener = listener;
        }

        @NonNull
        @Override
        public CommentViewHolder onCreateViewHolder(
                @NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_comment, parent, false);
            return new CommentViewHolder(view, listener);
        }

        @Override
        public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
            holder.bind(comments.get(position), position);
        }

        @Override
        public int getItemCount() {
            return comments.size();
        }

        private static final class CommentViewHolder extends RecyclerView.ViewHolder {
            private final ImageView avatar;
            private final TextView body;
            private final TextView time;
            private final TextView likeCount;
            private final TextView reply;
            private final ImageButton like;
            private final LinearLayout replies;
            private final Listener listener;

            CommentViewHolder(View itemView, Listener listener) {
                super(itemView);
                this.listener = listener;
                avatar = itemView.findViewById(R.id.iv_comment_avatar);
                body = itemView.findViewById(R.id.tv_comment_body);
                time = itemView.findViewById(R.id.tv_comment_time);
                likeCount = itemView.findViewById(R.id.tv_comment_like_count);
                reply = itemView.findViewById(R.id.tv_comment_reply);
                like = itemView.findViewById(R.id.btn_comment_like);
                replies = itemView.findViewById(R.id.ll_replies);
            }

            void bind(Comment comment, int parentPosition) {
                avatar.setImageResource(comment.user.avatarResId);
                avatar.setContentDescription(
                        comment.user.displayName + " profile image");
                body.setText(styledBody(comment.user.handle, comment.text));
                time.setText(comment.timeAgo);
                refreshLike(comment);

                like.setOnClickListener(view -> {
                    int currentPosition = getBindingAdapterPosition();
                    if (currentPosition == RecyclerView.NO_POSITION) return;
                    comment.liked = !comment.liked;
                    comment.likeCount =
                            Math.max(0, comment.likeCount + (comment.liked ? 1 : -1));
                    refreshLike(comment);
                    listener.onAction(
                            comment.liked ? "COMMENT_LIKE" : "COMMENT_UNLIKE",
                            comment,
                            currentPosition,
                            -1);
                });
                reply.setOnClickListener(view -> {
                    int currentPosition = getBindingAdapterPosition();
                    if (currentPosition != RecyclerView.NO_POSITION) {
                        listener.onReply(comment, currentPosition);
                    }
                });

                replies.removeAllViews();
                for (int replyPosition = 0;
                     replyPosition < comment.replies.size();
                     replyPosition++) {
                    Comment replyComment = comment.replies.get(replyPosition);
                    View replyView = LayoutInflater.from(itemView.getContext())
                            .inflate(R.layout.item_comment_reply, replies, false);
                    bindReply(
                            replyView,
                            replyComment,
                            parentPosition,
                            replyPosition);
                    replies.addView(replyView);
                }
            }

            private void refreshLike(Comment comment) {
                like.setImageResource(
                        comment.liked
                                ? R.drawable.ic_social_heart_filled
                                : R.drawable.ic_social_heart_outline);
                like.setContentDescription(
                        comment.liked ? "Unlike comment" : "Like comment");
                likeCount.setText(
                        comment.likeCount > 0
                                ? comment.likeCount + " likes"
                                : "");
            }

            private void bindReply(View view, Comment replyComment,
                                   int parentPosition, int replyPosition) {
                ImageView replyAvatar = view.findViewById(R.id.iv_reply_avatar);
                TextView replyBody = view.findViewById(R.id.tv_reply_body);
                TextView replyTime = view.findViewById(R.id.tv_reply_time);
                TextView replyLikeCount = view.findViewById(R.id.tv_reply_like_count);
                ImageButton replyLike = view.findViewById(R.id.btn_reply_like);

                replyAvatar.setImageResource(replyComment.user.avatarResId);
                replyAvatar.setContentDescription(
                        replyComment.user.displayName + " profile image");
                replyBody.setText(styledBody(
                        replyComment.user.handle, replyComment.text));
                replyTime.setText(replyComment.timeAgo);
                refreshReplyLike(replyLike, replyLikeCount, replyComment);

                replyLike.setOnClickListener(clicked -> {
                    int currentParentPosition = getBindingAdapterPosition();
                    if (currentParentPosition == RecyclerView.NO_POSITION) return;
                    replyComment.liked = !replyComment.liked;
                    replyComment.likeCount = Math.max(
                            0,
                            replyComment.likeCount + (replyComment.liked ? 1 : -1));
                    refreshReplyLike(replyLike, replyLikeCount, replyComment);
                    listener.onAction(
                            replyComment.liked ? "REPLY_LIKE" : "REPLY_UNLIKE",
                            replyComment,
                            currentParentPosition,
                            replyPosition);
                });
            }

            private void refreshReplyLike(ImageButton button, TextView count, Comment reply) {
                button.setImageResource(
                        reply.liked
                                ? R.drawable.ic_social_heart_filled
                                : R.drawable.ic_social_heart_outline);
                button.setContentDescription(
                        reply.liked ? "Unlike reply" : "Like reply");
                count.setText(reply.likeCount > 0 ? reply.likeCount + " likes" : "");
            }

            private static SpannableString styledBody(String handle, String text) {
                SpannableString value = new SpannableString(handle + "  " + text);
                value.setSpan(
                        new StyleSpan(Typeface.BOLD),
                        0,
                        handle.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                return value;
            }
        }
    }
}
