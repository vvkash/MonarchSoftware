package com.monarch.software.phase2;

import android.content.Context;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.monarch.software.R;

import java.util.List;
import java.util.Locale;

final class SocialFeedAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    interface Listener {
        void onAction(String eventType, SocialFeedContent.FeedPost post,
                      int feedPosition, String target, String details);
        void onStorySelected(SocialFeedContent.FeedPost post, int feedPosition);
        void onCommentsRequested(SocialFeedContent.FeedPost post,
                                 int feedPosition, String target);
        void onShareRequested(SocialFeedContent.FeedPost post, int feedPosition);
        void onProfileRequested(SocialFeedContent.FeedPost post, int feedPosition);
        void onPostOptionsRequested(View anchor, SocialFeedContent.FeedPost post,
                                    int feedPosition);
    }

    private static final int TYPE_STORIES = 0;
    private static final int TYPE_POST = 1;

    private final List<SocialFeedContent.FeedPost> posts;
    private final Listener listener;

    SocialFeedAdapter(List<SocialFeedContent.FeedPost> posts, Listener listener) {
        this.posts = posts;
        this.listener = listener;
        setHasStableIds(true);
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? TYPE_STORIES : TYPE_POST;
    }

    @Override
    public long getItemId(int position) {
        return position == 0 ? Long.MIN_VALUE : posts.get(position - 1).postId;
    }

    @Override
    public int getItemCount() {
        return posts.size() + 1;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_STORIES) {
            return new StoriesViewHolder(
                    inflater.inflate(R.layout.item_stories_row, parent, false), listener);
        }
        return new PostViewHolder(
                inflater.inflate(R.layout.item_insta_post, parent, false), listener);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof StoriesViewHolder) {
            ((StoriesViewHolder) holder).bind(posts);
            return;
        }
        ((PostViewHolder) holder).bind(posts.get(position - 1), position - 1);
    }

    private static final class StoriesViewHolder extends RecyclerView.ViewHolder {
        private final LinearLayout container;
        private final Listener listener;

        StoriesViewHolder(View itemView, Listener listener) {
            super(itemView);
            this.container = itemView.findViewById(R.id.stories_container);
            this.listener = listener;
        }

        void bind(List<SocialFeedContent.FeedPost> posts) {
            Context context = itemView.getContext();
            container.removeAllViews();
            addStory(
                    context,
                    null,
                    -1,
                    "Your story",
                    R.drawable.social_workspace_thumb,
                    true);

            int storyCount = Math.min(12, posts.size());
            for (int i = 0; i < storyCount; i++) {
                SocialFeedContent.FeedPost post = posts.get(i);
                String handle = post.user.handle;
                String label = handle.length() > 10
                        ? handle.substring(0, 9) + "…"
                        : handle;
                addStory(context, post, i, label, post.user.avatarResId, false);
            }
        }

        private void addStory(Context context, SocialFeedContent.FeedPost post,
                              int position, String label, int avatarResId, boolean yourStory) {
            LinearLayout item = new LinearLayout(context);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER_HORIZONTAL);
            item.setPadding(dp(context, 7), dp(context, 10), dp(context, 7), dp(context, 9));
            item.setBackgroundResource(android.R.drawable.list_selector_background);

            FrameLayout ring = new FrameLayout(context);
            LinearLayout.LayoutParams ringParams =
                    new LinearLayout.LayoutParams(dp(context, 68), dp(context, 68));
            ring.setLayoutParams(ringParams);
            ring.setPadding(dp(context, yourStory ? 2 : 3),
                    dp(context, yourStory ? 2 : 3),
                    dp(context, yourStory ? 2 : 3),
                    dp(context, yourStory ? 2 : 3));
            ring.setBackgroundResource(
                    yourStory ? R.drawable.social_avatar_mask : R.drawable.social_story_ring);

            ImageView avatar = new ImageView(context);
            FrameLayout.LayoutParams avatarParams = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            avatar.setLayoutParams(avatarParams);
            avatar.setBackgroundResource(R.drawable.social_avatar_mask);
            avatar.setClipToOutline(true);
            avatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
            avatar.setImageResource(avatarResId);
            avatar.setContentDescription(label + " story");
            ring.addView(avatar);

            if (yourStory) {
                TextView plus = new TextView(context);
                FrameLayout.LayoutParams plusParams =
                        new FrameLayout.LayoutParams(dp(context, 22), dp(context, 22));
                plusParams.gravity = Gravity.END | Gravity.BOTTOM;
                plus.setLayoutParams(plusParams);
                plus.setBackgroundResource(R.drawable.social_verified_badge);
                plus.setGravity(Gravity.CENTER);
                plus.setText("+");
                plus.setTextColor(0xFFFFFFFF);
                plus.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
                plus.setTypeface(Typeface.DEFAULT_BOLD);
                ring.addView(plus);
            }

            TextView labelView = new TextView(context);
            labelView.setText(label);
            labelView.setTextColor(ContextCompat.getColor(context, R.color.social_text_secondary));
            labelView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
            labelView.setMaxLines(1);
            labelView.setGravity(Gravity.CENTER);
            labelView.setPadding(0, dp(context, 5), 0, 0);

            item.addView(ring);
            item.addView(labelView);
            item.setOnClickListener(view -> {
                if (post == null) {
                    listener.onAction(
                            "YOUR_STORY_TAP", null, -1, "your_story", null);
                } else {
                    listener.onStorySelected(post, position);
                }
            });
            container.addView(item);
        }
    }

    private static final class PostViewHolder extends RecyclerView.ViewHolder {
        private final FrameLayout avatarFrame;
        private final ImageView avatar;
        private final TextView username;
        private final TextView verified;
        private final TextView location;
        private final ImageButton more;
        private final View mediaFrame;
        private final ImageView media;
        private final ImageView heartOverlay;
        private final ImageButton like;
        private final ImageButton comment;
        private final ImageButton share;
        private final ImageButton bookmark;
        private final TextView likeCount;
        private final TextView caption;
        private final TextView viewComments;
        private final TextView commentPreview;
        private final TextView timestamp;
        private final Listener listener;
        private final GestureDetector gestureDetector;

        private SocialFeedContent.FeedPost boundPost;
        private int boundPosition;

        PostViewHolder(View itemView, Listener listener) {
            super(itemView);
            this.listener = listener;
            avatarFrame = itemView.findViewById(R.id.fl_avatar);
            avatar = itemView.findViewById(R.id.iv_avatar);
            username = itemView.findViewById(R.id.tv_username);
            verified = itemView.findViewById(R.id.tv_verified);
            location = itemView.findViewById(R.id.tv_location);
            more = itemView.findViewById(R.id.btn_more);
            mediaFrame = itemView.findViewById(R.id.fl_image);
            media = itemView.findViewById(R.id.iv_post_media);
            heartOverlay = itemView.findViewById(R.id.iv_heart_overlay);
            like = itemView.findViewById(R.id.btn_like);
            comment = itemView.findViewById(R.id.btn_comment);
            share = itemView.findViewById(R.id.btn_share);
            bookmark = itemView.findViewById(R.id.btn_bookmark);
            likeCount = itemView.findViewById(R.id.tv_like_count);
            caption = itemView.findViewById(R.id.tv_caption);
            viewComments = itemView.findViewById(R.id.tv_view_comments);
            commentPreview = itemView.findViewById(R.id.tv_comment_preview);
            timestamp = itemView.findViewById(R.id.tv_timestamp);

            gestureDetector = new GestureDetector(
                    itemView.getContext(),
                    new GestureDetector.SimpleOnGestureListener() {
                        @Override
                        public boolean onDown(MotionEvent event) {
                            return true;
                        }

                        @Override
                        public boolean onSingleTapConfirmed(MotionEvent event) {
                            emit("MEDIA_TAP", "media", null);
                            return true;
                        }

                        @Override
                        public void onLongPress(MotionEvent event) {
                            emit("MEDIA_LONG_PRESS", "media", null);
                        }

                        @Override
                        public boolean onDoubleTap(MotionEvent event) {
                            if (boundPost == null) return true;
                            boolean newlyLiked = !boundPost.liked;
                            if (newlyLiked) {
                                boundPost.liked = true;
                                boundPost.likeCount++;
                                refreshLike();
                                refreshLikeCount();
                            }
                            animateHeart();
                            emit(
                                    "DOUBLE_TAP",
                                    "media",
                                    "liked_now=" + boundPost.liked
                                            + ";new_like=" + newlyLiked);
                            return true;
                        }
                    });

            mediaFrame.setOnTouchListener((view, event) -> {
                gestureDetector.onTouchEvent(event);
                return true;
            });
        }

        void bind(SocialFeedContent.FeedPost post, int position) {
            boundPost = post;
            boundPosition = position;

            avatar.setImageResource(post.user.avatarResId);
            avatar.setContentDescription(post.user.displayName + " profile image");
            username.setText(post.user.handle);
            verified.setVisibility(post.user.verified ? View.VISIBLE : View.GONE);
            location.setText(post.location);

            media.setImageResource(post.mediaResId);
            media.setContentDescription(post.mediaDescription);
            refreshLike();
            refreshBookmark();
            refreshLikeCount();
            refreshCaption();

            viewComments.setText(
                    "View all " + formatCount(post.commentCount) + " comments");

            SpannableString preview = new SpannableString(
                    post.commentUser.handle + "  " + post.commentText);
            preview.setSpan(
                    new StyleSpan(Typeface.BOLD),
                    0,
                    post.commentUser.handle.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            commentPreview.setText(preview);
            timestamp.setText(post.timeAgo.toUpperCase(Locale.US));

            heartOverlay.setVisibility(View.GONE);
            heartOverlay.setAlpha(0f);
            heartOverlay.setScaleX(1f);
            heartOverlay.setScaleY(1f);

            like.setOnClickListener(view -> {
                post.liked = !post.liked;
                post.likeCount = Math.max(0, post.likeCount + (post.liked ? 1 : -1));
                refreshLike();
                refreshLikeCount();
                emit(post.liked ? "LIKE" : "UNLIKE", "like_button", null);
            });

            comment.setOnClickListener(view ->
                    listener.onCommentsRequested(post, position, "comment_button"));
            viewComments.setOnClickListener(view ->
                    listener.onCommentsRequested(post, position, "view_all_comments"));
            commentPreview.setOnClickListener(view ->
                    listener.onCommentsRequested(post, position, "comment_preview"));
            share.setOnClickListener(view ->
                    listener.onShareRequested(post, position));

            bookmark.setOnClickListener(view -> {
                post.saved = !post.saved;
                refreshBookmark();
                emit(post.saved ? "SAVE" : "UNSAVE", "bookmark_button", null);
            });

            View.OnClickListener profileClick =
                    view -> listener.onProfileRequested(post, position);
            avatarFrame.setOnClickListener(profileClick);
            username.setOnClickListener(profileClick);

            more.setOnClickListener(view ->
                    listener.onPostOptionsRequested(view, post, position));

            caption.setOnClickListener(view -> {
                post.captionExpanded = !post.captionExpanded;
                refreshCaption();
                emit(
                        post.captionExpanded ? "CAPTION_EXPAND" : "CAPTION_COLLAPSE",
                        "caption",
                        null);
            });
        }

        private void emit(String eventType, String target, String details) {
            if (boundPost != null) {
                listener.onAction(
                        eventType, boundPost, boundPosition, target, details);
            }
        }

        private void refreshLike() {
            like.setImageResource(
                    boundPost != null && boundPost.liked
                            ? R.drawable.ic_social_heart_filled
                            : R.drawable.ic_social_heart_outline);
            like.setContentDescription(
                    boundPost != null && boundPost.liked ? "Unlike" : "Like");
        }

        private void refreshBookmark() {
            bookmark.setImageResource(
                    boundPost != null && boundPost.saved
                            ? R.drawable.ic_social_bookmark_filled
                            : R.drawable.ic_social_bookmark_outline);
            bookmark.setContentDescription(
                    boundPost != null && boundPost.saved ? "Remove saved post" : "Save post");
        }

        private void refreshLikeCount() {
            if (boundPost != null) {
                likeCount.setText(formatCount(boundPost.likeCount) + " likes");
            }
        }

        private void refreshCaption() {
            if (boundPost == null) return;
            SpannableString value = new SpannableString(
                    boundPost.user.handle + "  " + boundPost.caption);
            value.setSpan(
                    new StyleSpan(Typeface.BOLD),
                    0,
                    boundPost.user.handle.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            caption.setText(value);
            caption.setMaxLines(boundPost.captionExpanded ? Integer.MAX_VALUE : 2);
            caption.setEllipsize(
                    boundPost.captionExpanded ? null : android.text.TextUtils.TruncateAt.END);
        }

        private void animateHeart() {
            heartOverlay.setVisibility(View.VISIBLE);
            heartOverlay.setAlpha(0.96f);
            heartOverlay.setScaleX(0.25f);
            heartOverlay.setScaleY(0.25f);
            heartOverlay.animate()
                    .scaleX(1.15f)
                    .scaleY(1.15f)
                    .setDuration(180)
                    .withEndAction(() -> heartOverlay.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(90)
                            .withEndAction(() -> heartOverlay.animate()
                                    .alpha(0f)
                                    .setStartDelay(420)
                                    .setDuration(260)
                                    .withEndAction(() ->
                                            heartOverlay.setVisibility(View.GONE))
                                    .start())
                            .start())
                    .start();
        }
    }

    static String formatCount(int value) {
        if (value >= 1_000_000) {
            return String.format(Locale.US, "%.1fM", value / 1_000_000f);
        }
        if (value >= 10_000) {
            return String.format(Locale.US, "%.1fK", value / 1_000f);
        }
        return String.format(Locale.US, "%,d", value);
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
