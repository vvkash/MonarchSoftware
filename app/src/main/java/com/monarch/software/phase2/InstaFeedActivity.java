package com.monarch.software.phase2;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.monarch.software.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class InstaFeedActivity extends AppCompatActivity {

    static final String EXTRA_SESSION_START = "social_session_start";

    private static final String STATE_SESSION_START = "state_session_start";
    private static final float IMPRESSION_THRESHOLD = 0.5f;

    private static final class Impression {
        final long startedAt;
        float peakVisiblePercent;

        Impression(long startedAt, float visiblePercent) {
            this.startedAt = startedAt;
            this.peakVisiblePercent = visiblePercent;
        }
    }

    private final class TrackedStoryDialog extends Dialog {
        TrackedStoryDialog() {
            super(InstaFeedActivity.this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        }

        @Override
        public boolean dispatchTouchEvent(@NonNull MotionEvent event) {
            if (telemetry != null) {
                telemetry.recordTouch(event, "social_story", 0);
            }
            return super.dispatchTouchEvent(event);
        }
    }

    private final class TrackedAlertDialog extends AlertDialog {
        TrackedAlertDialog() {
            super(InstaFeedActivity.this);
        }

        @Override
        public boolean dispatchTouchEvent(@NonNull MotionEvent event) {
            if (telemetry != null) {
                telemetry.recordTouch(event, "social_overlay", 0);
            }
            return super.dispatchTouchEvent(event);
        }
    }

    private interface OptionListener {
        void onOption(int index, String label);
    }

    private final Map<Integer, Impression> activeImpressions = new HashMap<>();
    private final Rect visibleRect = new Rect();

    private List<SocialFeedContent.FeedPost> posts;
    private RecyclerView recyclerView;
    private LinearLayoutManager layoutManager;
    private SocialTelemetry telemetry;
    private long sessionStart;
    private long screenOpenedAt;
    private long resumedAt;
    private int maxScrollDepth;
    private int lastLoggedDepthBucket;
    private Dialog storyDialog;
    private AlertDialog activeAlertDialog;
    private boolean screenResumed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        configureSystemBars();
        setContentView(R.layout.activity_insta_feed);

        sessionStart = savedInstanceState == null
                ? System.currentTimeMillis()
                : savedInstanceState.getLong(
                        STATE_SESSION_START, System.currentTimeMillis());
        screenOpenedAt = System.currentTimeMillis();
        telemetry = new SocialTelemetry(this, "feed", sessionStart);

        posts = SocialFeedContent.buildDailyFeed();
        recyclerView = findViewById(R.id.recycler_insta);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(null);
        recyclerView.setAdapter(new SocialFeedAdapter(posts, createFeedListener()));
        recyclerView.addOnScrollListener(createScrollListener());

        configureChrome();
        telemetry.log(
                "SCREEN_OPEN",
                -1,
                null,
                0,
                0,
                "feed",
                "post_count=" + posts.size() + ";media=local_licensed");
        recyclerView.post(this::trackVisiblePosts);
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

    private SocialFeedAdapter.Listener createFeedListener() {
        return new SocialFeedAdapter.Listener() {
            @Override
            public void onAction(String eventType, SocialFeedContent.FeedPost post,
                                 int feedPosition, String target, String details) {
                logPostEvent(
                        eventType, post, feedPosition, 0, target, details, null);
                if ("YOUR_STORY_TAP".equals(eventType)) {
                    showInformationalDialog(
                            "Create a story",
                            "Story creation is unavailable in this research feed.",
                            "your_story");
                }
            }

            @Override
            public void onStorySelected(SocialFeedContent.FeedPost post, int feedPosition) {
                showStory(post, feedPosition);
            }

            @Override
            public void onCommentsRequested(SocialFeedContent.FeedPost post,
                                            int feedPosition, String target) {
                openComments(post, feedPosition, target);
            }

            @Override
            public void onShareRequested(SocialFeedContent.FeedPost post, int feedPosition) {
                sharePost(post, feedPosition);
            }

            @Override
            public void onProfileRequested(SocialFeedContent.FeedPost post, int feedPosition) {
                showProfile(post, feedPosition);
            }

            @Override
            public void onPostOptionsRequested(View anchor,
                                               SocialFeedContent.FeedPost post,
                                               int feedPosition) {
                showPostOptions(anchor, post, feedPosition);
            }
        };
    }

    private RecyclerView.OnScrollListener createScrollListener() {
        return new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                trackVisiblePosts();
                trackScrollDepth(dy);
            }

            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                String value;
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    value = "dragging";
                } else if (newState == RecyclerView.SCROLL_STATE_SETTLING) {
                    value = "settling";
                } else {
                    value = "idle";
                }
                telemetry.log(
                        "SCROLL_STATE",
                        -1,
                        null,
                        Math.max(0, layoutManager.findFirstVisibleItemPosition() - 1),
                        0,
                        "feed",
                        "state=" + value);
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    trackVisiblePosts();
                }
            }
        };
    }

    private void configureChrome() {
        View.OnClickListener createClick = view -> {
            telemetry.log(
                    "NAV_TAP", -1, null, 0, 0, "create", "source=toolbar_or_nav");
            showInformationalDialog(
                    "Create a post",
                    "Post creation is unavailable in this research feed.",
                    "create");
        };
        findViewById(R.id.btn_create).setOnClickListener(createClick);
        findViewById(R.id.nav_create).setOnClickListener(createClick);

        findViewById(R.id.btn_notifications).setOnClickListener(view -> {
            telemetry.log(
                    "NAV_TAP", -1, null, 0, 0, "activity", "source=toolbar");
            showMessageDialog(
                    "Activity",
                    "maya.frames liked your photo.\n\n"
                            + "theobakes started following you.\n\n"
                            + "noraonfilm mentioned you in a comment.",
                    () -> telemetry.log(
                            "DIALOG_CLOSE", -1, null, 0, 0, "activity", null));
        });

        findViewById(R.id.btn_messages).setOnClickListener(view -> {
            telemetry.log(
                    "NAV_TAP", -1, null, 0, 0, "messages", "source=toolbar");
            showInformationalDialog(
                    "Messages",
                    "Direct messages are hidden for this study session.",
                    "messages");
        });

        findViewById(R.id.nav_home).setOnClickListener(view -> {
            telemetry.log(
                    "NAV_TAP", -1, null, 0, 0, "home", "source=bottom_nav");
            recyclerView.smoothScrollToPosition(0);
        });
        findViewById(R.id.nav_search).setOnClickListener(view -> {
            telemetry.log(
                    "NAV_TAP", -1, null, 0, 0, "search", "source=bottom_nav");
            showInformationalDialog(
                    "Search",
                    "Search is not included in this study session.",
                    "search");
        });
        findViewById(R.id.nav_video).setOnClickListener(view -> {
            telemetry.log(
                    "NAV_TAP", -1, null, 0, 0, "video", "source=bottom_nav");
            showInformationalDialog(
                    "Video",
                    "Short-form video is not included in this study session.",
                    "video");
        });
        findViewById(R.id.nav_profile).setOnClickListener(view -> {
            telemetry.log(
                    "NAV_TAP", -1, null, 0, 0, "own_profile", "source=bottom_nav");
            showMessageDialog(
                    "Your profile",
                    "@you\n\n24 posts   318 followers   204 following",
                    () -> telemetry.log(
                            "PROFILE_CLOSE", -1, "you", 0, 0, "own_profile", null));
        });
    }

    private void showInformationalDialog(String title, String message, String target) {
        telemetry.log("DIALOG_OPEN", -1, null, 0, 0, target, null);
        showMessageDialog(
                title,
                message,
                () -> telemetry.log(
                        "DIALOG_CLOSE", -1, null, 0, 0, target, null));
    }

    private void showMessageDialog(String title, String message, Runnable onDismiss) {
        TrackedAlertDialog dialog = new TrackedAlertDialog();
        dialog.setTitle(title);
        dialog.setMessage(message);
        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "Close", (closed, which) -> {
        });
        showManagedDialog(dialog, onDismiss);
    }

    private void showOptionsDialog(String title, String[] options,
                                   OptionListener optionListener, Runnable onDismiss) {
        TrackedAlertDialog dialog = new TrackedAlertDialog();
        dialog.setTitle(title);

        LinearLayout optionList = new LinearLayout(this);
        optionList.setOrientation(LinearLayout.VERTICAL);
        int horizontalPadding = dp(20);
        optionList.setPadding(0, dp(8), 0, dp(8));
        for (int index = 0; index < options.length; index++) {
            String option = options[index];
            TextView row = new TextView(this);
            row.setText(option);
            row.setTextColor(getColorCompat(R.color.social_text));
            row.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(horizontalPadding, dp(14), horizontalPadding, dp(14));
            row.setBackgroundResource(android.R.drawable.list_selector_background);
            int selectedIndex = index;
            row.setOnClickListener(view -> {
                optionListener.onOption(selectedIndex, option);
                dialog.dismiss();
            });
            optionList.addView(
                    row,
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT));
        }
        dialog.setView(optionList);
        showManagedDialog(dialog, onDismiss);
    }

    private void showManagedDialog(TrackedAlertDialog dialog, Runnable onDismiss) {
        if (activeAlertDialog != null && activeAlertDialog.isShowing()) {
            activeAlertDialog.dismiss();
        }
        flushImpressions();
        dialog.setOnDismissListener(dismissed -> {
            if (activeAlertDialog == dialog) activeAlertDialog = null;
            if (onDismiss != null) onDismiss.run();
            if (screenResumed
                    && activeAlertDialog == null
                    && storyDialog == null
                    && recyclerView != null) {
                recyclerView.post(this::trackVisiblePosts);
            }
        });
        activeAlertDialog = dialog;
        dialog.show();
    }

    private void showProfile(SocialFeedContent.FeedPost post, int position) {
        logPostEvent(
                "PROFILE_OPEN", post, position, 0, "post_header", null, null);
        String verified = post.user.verified ? "  •  Verified" : "";
        showMessageDialog(
                post.user.displayName,
                "@" + post.user.handle + verified
                        + "\n\n" + (42 + post.postId) + " posts"
                        + "   " + SocialFeedAdapter.formatCount(824 + post.postId * 73)
                        + " followers"
                        + "\n\nPhotography, places, and everyday notes.",
                () -> logPostEvent(
                        "PROFILE_CLOSE",
                        post,
                        position,
                        0,
                        "profile_dialog",
                        null,
                        null));
    }

    private void showPostOptions(View anchor, SocialFeedContent.FeedPost post, int position) {
        logPostEvent(
                "POST_MENU_OPEN", post, position, 0, "more_button", null, null);
        String[] options = {
            "Why you’re seeing this post",
            "Not interested",
            "About this account",
            "Report"
        };
        showOptionsDialog(
                "Post options",
                options,
                (index, label) -> {
                    String[] actions = {
                        "why_this_post", "not_interested", "about_account", "report"
                    };
                    logPostEvent(
                            "POST_MENU_SELECT",
                            post,
                            position,
                            0,
                            "post_menu",
                            "action=" + actions[index],
                            null);
                    Toast.makeText(this, label, Toast.LENGTH_SHORT).show();
                },
                () -> logPostEvent(
                        "POST_MENU_CLOSE",
                        post,
                        position,
                        0,
                        "post_menu",
                        null,
                        null));
    }

    private void openComments(SocialFeedContent.FeedPost post, int position, String target) {
        logPostEvent(
                "COMMENT_OPEN", post, position, 0, target, null, null);
        Intent intent = new Intent(this, CommentsActivity.class);
        intent.putExtra("postId", post.postId);
        intent.putExtra("postHandle", post.user.handle);
        intent.putExtra("postCaption", post.caption);
        intent.putExtra("postMediaResId", post.mediaResId);
        intent.putExtra("postAvatarResId", post.user.avatarResId);
        intent.putExtra(EXTRA_SESSION_START, sessionStart);
        startActivity(intent);
    }

    private void sharePost(SocialFeedContent.FeedPost post, int position) {
        logPostEvent(
                "SHARE_OPEN", post, position, 0, "share_button", null, null);
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(
                Intent.EXTRA_TEXT,
                "A post from @" + post.user.handle + " on Monarch:\n\n" + post.caption);
        startActivity(Intent.createChooser(send, "Share post"));
    }

    private void showStory(SocialFeedContent.FeedPost post, int position) {
        if (storyDialog != null) storyDialog.dismiss();
        flushImpressions();

        long storyOpenedAt = System.currentTimeMillis();
        logPostEvent(
                "STORY_OPEN", post, position, 0, "story_ring", null, null);

        Dialog dialog = new TrackedStoryDialog();
        dialog.setContentView(R.layout.dialog_story);
        storyDialog = dialog;

        ImageView media = dialog.findViewById(R.id.iv_story_media);
        ImageView avatar = dialog.findViewById(R.id.iv_story_avatar);
        TextView handle = dialog.findViewById(R.id.tv_story_handle);
        TextView time = dialog.findViewById(R.id.tv_story_time);
        TextView caption = dialog.findViewById(R.id.tv_story_caption);
        EditText reply = dialog.findViewById(R.id.et_story_reply);
        ImageButton like = dialog.findViewById(R.id.btn_story_like);

        media.setImageResource(post.mediaResId);
        media.setContentDescription(post.mediaDescription);
        avatar.setImageResource(post.user.avatarResId);
        handle.setText(post.user.handle);
        time.setText(post.timeAgo);
        caption.setText(post.caption);

        media.setOnClickListener(view -> logPostEvent(
                "STORY_MEDIA_TAP", post, position, 0, "story_media", null, null));
        dialog.findViewById(R.id.btn_story_close).setOnClickListener(view -> {
            logPostEvent(
                    "STORY_CLOSE_TAP", post, position, 0, "close_button", null, null);
            dialog.dismiss();
        });
        dialog.findViewById(R.id.btn_story_more).setOnClickListener(view -> {
            logPostEvent(
                    "STORY_MENU_OPEN", post, position, 0, "story_more", null, null);
            showOptionsDialog(
                    "Story options",
                    new String[]{"Mute", "About this account", "Report"},
                    (index, label) -> logPostEvent(
                            "STORY_MENU_SELECT",
                            post,
                            position,
                            0,
                            "story_menu",
                            "action=" + label,
                            null),
                    () -> logPostEvent(
                            "STORY_MENU_CLOSE",
                            post,
                            position,
                            0,
                            "story_menu",
                            null,
                            null));
        });

        boolean[] storyLiked = {false};
        like.setOnClickListener(view -> {
            storyLiked[0] = !storyLiked[0];
            like.setImageResource(
                    storyLiked[0]
                            ? R.drawable.ic_social_heart_filled
                            : R.drawable.ic_social_heart_light);
            logPostEvent(
                    storyLiked[0] ? "STORY_LIKE" : "STORY_UNLIKE",
                    post,
                    position,
                    0,
                    "story_like",
                    null,
                    null);
        });

        reply.setOnFocusChangeListener((view, hasFocus) -> {
            if (hasFocus) {
                logPostEvent(
                        "STORY_REPLY_FOCUS",
                        post,
                        position,
                        0,
                        "story_reply",
                        null,
                        null);
            }
        });
        View.OnClickListener submitReply = view ->
                submitStoryReply(reply, post, position);
        dialog.findViewById(R.id.btn_story_send).setOnClickListener(submitReply);
        reply.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                submitStoryReply(reply, post, position);
                return true;
            }
            return false;
        });

        dialog.setOnDismissListener(dismissed -> {
            logPostEvent(
                    "STORY_VIEW",
                    post,
                    position,
                    System.currentTimeMillis() - storyOpenedAt,
                    "story",
                    "liked=" + storyLiked[0],
                    100f);
            if (storyDialog == dialog) storyDialog = null;
            if (screenResumed && recyclerView != null) {
                recyclerView.post(this::trackVisiblePosts);
            }
        });
        dialog.show();

        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT);
            window.setStatusBarColor(Color.BLACK);
            window.setNavigationBarColor(Color.BLACK);
            window.getDecorView().setSystemUiVisibility(0);
        }
    }

    private void submitStoryReply(EditText reply, SocialFeedContent.FeedPost post, int position) {
        String text = reply.getText().toString().trim();
        if (text.isEmpty()) {
            logPostEvent(
                    "STORY_REPLY_EMPTY",
                    post,
                    position,
                    0,
                    "story_reply",
                    null,
                    null);
            return;
        }
        logPostEvent(
                "STORY_REPLY_SEND",
                post,
                position,
                0,
                "story_reply",
                "text_length=" + text.length(),
                null);
        reply.setText("");
        Toast.makeText(this, "Message sent", Toast.LENGTH_SHORT).show();
    }

    private void trackVisiblePosts() {
        if (recyclerView == null || posts == null || isFeedObscured()) return;
        long now = System.currentTimeMillis();
        Set<Integer> currentlyVisible = new HashSet<>();

        for (int childIndex = 0; childIndex < recyclerView.getChildCount(); childIndex++) {
            View child = recyclerView.getChildAt(childIndex);
            int adapterPosition = recyclerView.getChildAdapterPosition(child);
            if (adapterPosition <= 0) continue;

            int feedPosition = adapterPosition - 1;
            if (feedPosition >= posts.size()) continue;
            float visiblePercent = visiblePercent(child);
            if (visiblePercent < IMPRESSION_THRESHOLD * 100f) continue;

            currentlyVisible.add(feedPosition);
            Impression impression = activeImpressions.get(feedPosition);
            if (impression == null) {
                impression = new Impression(now, visiblePercent);
                activeImpressions.put(feedPosition, impression);
                logPostEvent(
                        "POST_ENTER",
                        posts.get(feedPosition),
                        feedPosition,
                        0,
                        "post",
                        "threshold_percent=50",
                        visiblePercent);
            } else {
                impression.peakVisiblePercent =
                        Math.max(impression.peakVisiblePercent, visiblePercent);
            }
        }

        List<Integer> completed = new ArrayList<>();
        for (Map.Entry<Integer, Impression> entry : activeImpressions.entrySet()) {
            if (currentlyVisible.contains(entry.getKey())) continue;
            int feedPosition = entry.getKey();
            Impression impression = entry.getValue();
            logPostEvent(
                    "POST_VIEW",
                    posts.get(feedPosition),
                    feedPosition,
                    now - impression.startedAt,
                    "post",
                    "threshold_percent=50",
                    impression.peakVisiblePercent);
            completed.add(feedPosition);
        }
        for (int feedPosition : completed) {
            activeImpressions.remove(feedPosition);
        }
    }

    private float visiblePercent(View child) {
        if (!child.getLocalVisibleRect(visibleRect)) return 0f;
        long visibleArea = (long) visibleRect.width() * visibleRect.height();
        long totalArea = (long) child.getWidth() * child.getHeight();
        if (totalArea <= 0) return 0f;
        return Math.min(100f, visibleArea * 100f / totalArea);
    }

    private boolean isFeedObscured() {
        return (storyDialog != null && storyDialog.isShowing())
                || (activeAlertDialog != null && activeAlertDialog.isShowing());
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @SuppressWarnings("deprecation")
    private int getColorCompat(int colorResId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return getColor(colorResId);
        }
        return getResources().getColor(colorResId);
    }

    private void trackScrollDepth(int dy) {
        int lastVisible = layoutManager.findLastVisibleItemPosition() - 1;
        if (lastVisible < 0 || posts.isEmpty()) return;
        int depth = Math.min(100, Math.round((lastVisible + 1) * 100f / posts.size()));
        maxScrollDepth = Math.max(maxScrollDepth, depth);
        int bucket = maxScrollDepth / 5 * 5;
        if (bucket <= lastLoggedDepthBucket) return;
        lastLoggedDepthBucket = bucket;
        telemetry.log(
                "SCROLL_DEPTH",
                -1,
                null,
                lastVisible,
                0,
                "feed",
                "percent=" + bucket + ";dy=" + dy);
    }

    private void flushImpressions() {
        long now = System.currentTimeMillis();
        for (Map.Entry<Integer, Impression> entry : activeImpressions.entrySet()) {
            int feedPosition = entry.getKey();
            Impression impression = entry.getValue();
            logPostEvent(
                    "POST_VIEW",
                    posts.get(feedPosition),
                    feedPosition,
                    now - impression.startedAt,
                    "post",
                    "reason=screen_pause;threshold_percent=50",
                    impression.peakVisiblePercent);
        }
        activeImpressions.clear();
    }

    private void logPostEvent(String eventType, SocialFeedContent.FeedPost post,
                              int position, long duration, String target,
                              String details, Float visiblePercent) {
        telemetry.log(
                eventType,
                post == null ? -1 : post.postId,
                post == null ? null : post.user.handle,
                position,
                duration,
                target,
                details,
                visiblePercent);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (telemetry != null) telemetry.recordTouch(event, "social_feed", 0);
        return super.dispatchTouchEvent(event);
    }

    @Override
    protected void onResume() {
        super.onResume();
        screenResumed = true;
        resumedAt = System.currentTimeMillis();
        if (telemetry != null) {
            telemetry.log(
                    "SCREEN_RESUME", -1, null, 0, 0, "feed", null);
            if (recyclerView != null) recyclerView.post(this::trackVisiblePosts);
        }
    }

    @Override
    protected void onPause() {
        screenResumed = false;
        if (storyDialog != null && storyDialog.isShowing()) {
            storyDialog.dismiss();
        }
        if (activeAlertDialog != null && activeAlertDialog.isShowing()) {
            activeAlertDialog.dismiss();
        }
        if (telemetry != null) {
            flushImpressions();
            telemetry.log(
                    "SCREEN_PAUSE",
                    -1,
                    null,
                    maxScrollDepth,
                    Math.max(0, System.currentTimeMillis() - resumedAt),
                    "feed",
                    "max_scroll_percent=" + maxScrollDepth);
        }
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putLong(STATE_SESSION_START, sessionStart);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        if (telemetry != null) {
            telemetry.log(
                    "SCREEN_CLOSE",
                    -1,
                    null,
                    maxScrollDepth,
                    System.currentTimeMillis() - screenOpenedAt,
                    "feed",
                    "max_scroll_percent=" + maxScrollDepth);
            telemetry.close();
        }
        super.onDestroy();
    }
}
