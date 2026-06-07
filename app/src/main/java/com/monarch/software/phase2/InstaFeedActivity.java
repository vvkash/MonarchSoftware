package com.monarch.software.phase2;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.monarch.software.R;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class InstaFeedActivity extends AppCompatActivity {

    // ─────────────────────────────────────────────────────────────────────────
    // 40 fake users: {displayName, handle, avatarEmoji, avatarColor}
    // ─────────────────────────────────────────────────────────────────────────
    static final Object[][] USERS = {
        {"Alex Rivera",    "alex.captures",   "🌿", 0xFF4CAF50},
        {"Jordan Lee",     "jordanlee_",      "✨", 0xFFFF9800},
        {"Morgan Chase",   "m.chase.photo",   "🌸", 0xFFE91E63},
        {"Taylor Kim",     "taylor.creates",  "🎨", 0xFF9C27B0},
        {"Casey Patel",    "caseypatel",      "🌊", 0xFF2196F3},
        {"Riley Santos",   "riley.snaps",     "🔥", 0xFFF44336},
        {"Avery Chen",     "avery.vibes",     "🌙", 0xFF3F51B5},
        {"Quinn Davis",    "quinndavis",      "🍃", 0xFF009688},
        {"Blake Monroe",   "blakemonroe",     "⚡", 0xFFFFC107},
        {"Drew Martinez",  "drewm.lens",      "🎭", 0xFF795548},
        {"Sage Thompson",  "sage.t",          "🌺", 0xFFFF5722},
        {"Reese Johnson",  "reese_j",         "🎵", 0xFF607D8B},
        {"Skylar Brown",   "skylar.brown",    "🌈", 0xFF00BCD4},
        {"Parker Wilson",  "parkerwilson",    "🦋", 0xFF8BC34A},
        {"Peyton Garcia",  "peyton.garcia",   "🌻", 0xFFFFEB3B},
        {"Logan Adams",    "logan.adams",     "🏔", 0xFF455A64},
        {"Harley Clark",   "harley.clark",    "🎸", 0xFFAD1457},
        {"Rowan Wright",   "rowan.wright",    "🌴", 0xFF00796B},
        {"Finley Scott",   "finley.s",        "🎯", 0xFF1565C0},
        {"River Campbell", "rivercampbell",   "🌊", 0xFF0288D1},
        {"Indie Nguyen",   "indie.n",         "🦚", 0xFF558B2F},
        {"Zara Mitchell",  "zaramitchell",    "💫", 0xFFAB47BC},
        {"Eli Roberts",    "eli.snaps",       "🏙", 0xFF37474F},
        {"Nova Turner",    "nova.t",          "🌠", 0xFF4527A0},
        {"Luca Phillips",  "lucaphillips",    "🍕", 0xFFBF360C},
        {"Mia Evans",      "mia.creates",     "🌷", 0xFFC2185B},
        {"Theo Walker",    "theo.walker",     "🎬", 0xFF2E7D32},
        {"Ivy Harris",     "ivy.harris",      "🍀", 0xFF33691E},
        {"Ash King",       "ash.king",        "🌑", 0xFF424242},
        {"Jax Young",      "jaxyoung",        "🏄", 0xFF01579B},
        {"Cali Allen",     "cali.allen",      "🌅", 0xFFE64A19},
        {"Rio Hall",       "rio.hall",        "🎪", 0xFF6A1B9A},
        {"Zion Baker",     "zion.baker",      "⚽", 0xFF1B5E20},
        {"Ember Cox",      "ember.cox",       "🔮", 0xFF4A148C},
        {"Storm Nelson",   "storm.n",         "⛈", 0xFF37474F},
        {"Luna Carter",    "luna.carter",     "🌙", 0xFF311B92},
        {"Felix Morris",   "felix.m",         "🦉", 0xFF4E342E},
        {"Noel Rogers",    "noelrogers",      "🎄", 0xFF1A237E},
        {"Sable Reed",     "sable.reed",      "🐈", 0xFF5D4037},
        {"Cove James",     "cove.james",      "🏝", 0xFF006064}
    };

    // ─────────────────────────────────────────────────────────────────────────
    // 40 post templates: {imageEmoji, imageColor, caption, baseLikes, baseComments, location}
    // ─────────────────────────────────────────────────────────────────────────
    static final Object[][] POST_TEMPLATES = {
        {"🏛", 0xFFE8EAF6, "Some moments just stop time ✨ #architecture #travel #history",        4823, 127, "Washington, D.C."},
        {"🌲", 0xFFE8F5E9, "Lost in the woods and loving every second 🌿 #nature #pnw #hiking",    6714, 203, "Olympic National Park"},
        {"🍔", 0xFFFFEBEE, "Weekend calories don't count, right? 😅 #foodie #cheeseburger #nyc",   3291,  88, "Brooklyn, New York"},
        {"🐕", 0xFFFFF3E0, "He's a good boy and he knows it 🐾 #dogsofinstagram #goldenretriever", 12045, 445, "Portland, Oregon"},
        {"🌅", 0xFFFFE0B2, "5am is worth it every single time 🌊 #sunrise #beach #goldenhour",     8932, 267, "Malibu, California"},
        {"🌆", 0xFFE3F2FD, "The city never sleeps 🏙 #citylife #nightphotography #urban",          5567, 149, "Manhattan, New York"},
        {"🥗", 0xFFE8F5E9, "Eating the rainbow 🌈 #healthyeating #mealprep #vegan",               2876,  76, "Los Angeles, California"},
        {"🥾", 0xFFEFEBE9, "Every summit is worth the climb 🏔 #hiking #mountains #adventure",    7234, 198, "Rocky Mountain NP"},
        {"⚡", 0xFFE8EAF6, "The future is electric ⚡ #ev #sustainable #cleanenergy #tesla",       4102,  93, "Palo Alto, California"},
        {"☕", 0xFFEFEBE9, "Monday mornings hit different with the perfect cup ☕ #coffeelover",    9823, 312, "Seattle, Washington"},
        {"🏔", 0xFFECEFF1, "High altitude, higher perspective 🗻 #mountains #snow #ski #alps",    11203, 387, "Aspen, Colorado"},
        {"🖼", 0xFFFCE4EC, "Art is just creativity refusing to stay quiet 🎨 #art #museum #nyc",   3654, 101, "MoMA, New York"},
        {"🍜", 0xFFFFF8E1, "Street food is the soul of every city 🍜 #foodtravel #ramen #tokyo",   6891, 231, "Tokyo, Japan"},
        {"🚴", 0xFFE8F5E9, "Two wheels, zero excuses 🚵 #mountainbiking #cycling #moab",          4521, 134, "Moab, Utah"},
        {"🐘", 0xFFF1F8E9, "Big heart, even bigger ears 🐘 #wildlife #safari #nature #kenya",    15678, 523, "Amboseli, Kenya"},
        {"🌿", 0xFFE8F5E9, "Growing things, growing people 🌱 #rooftopgarden #urban #chicago",    3289,  87, "Chicago, Illinois"},
        {"🚗", 0xFFFFEBEE, "They don't make them like this anymore 🏎 #classiccar #vintage",       5892, 167, "Pebble Beach, California"},
        {"🏠", 0xFFE3F2FD, "Where the land meets the sea 🌊 #lighthouse #coastal #acadia",        8123, 245, "Acadia National Park"},
        {"🎸", 0xFFEDE7F6, "Playing until the neighbors complain 🎶 #music #guitar #austin",      4456, 128, "Austin, Texas"},
        {"🌺", 0xFFFCE4EC, "Nature's confetti 🌸 #flowers #spring #bloom #garden #portland",      7654, 219, "Portland, Oregon"},
        {"🏖", 0xFFE0F7FA, "Saltwater heals everything 🌊 #beach #summer #ocean #tulum",         10234, 356, "Tulum, Mexico"},
        {"🎭", 0xFFF3E5F5, "Life is a stage, might as well perform ✨ #theatre #arts #broadway",   3876,  98, "Broadway, New York"},
        {"🦅", 0xFFE8EAF6, "Freedom looks like this 🦅 #wildlife #birdwatching #grandteton",      9012, 278, "Grand Teton, Wyoming"},
        {"🍦", 0xFFFFF8E1, "Summer in a cone 🍦 #icecream #summer #dessert #sanfrancisco",        6234, 187, "San Francisco, CA"},
        {"🏋", 0xFFEFEBE9, "Progress over perfection every single day 💪 #gym #fitness #grind",   5432, 145, "Venice Beach, California"},
        {"🌌", 0xFFE8EAF6, "We are made of star stuff 🌟 #astrophotography #milkyway #stars",    18923, 634, "Death Valley, California"},
        {"🎪", 0xFFFCE4EC, "Life is too short for boring weekends 🎡 #carnival #lights #fun",     4123, 112, "Brooklyn, New York"},
        {"🦊", 0xFFFFE0B2, "Found a friend on the trail 🦊 #wildlife #fox #yellowstone",         14567, 489, "Yellowstone, Wyoming"},
        {"🏄", 0xFFE0F7FA, "Chasing waves and good vibes only 🌊 #surfing #hawaii #ocean",        8934, 267, "Oahu, Hawaii"},
        {"🌃", 0xFFE8EAF6, "When the city sparkles 🌉 #cityscape #longexposure #sfbay",           6789, 198, "San Francisco, CA"},
        {"🍰", 0xFFFCE4EC, "Because every day deserves a little sweetness 🎂 #baking #paris",     5234, 156, "Paris, France"},
        {"🎿", 0xFFE3F2FD, "Powder days are the best days ⛷ #skiing #snow #whistler #canada",    7891, 234, "Whistler, Canada"},
        {"🌊", 0xFFE0F7FA, "The ocean is calling and I must go 🐚 #ocean #bigsur #travel",        9321, 298, "Big Sur, California"},
        {"🏕", 0xFFE8F5E9, "Disconnected from WiFi, connected to the world 🌲 #camping #yosemite",12034, 401, "Yosemite, California"},
        {"🎨", 0xFFF3E5F5, "Every canvas starts with a blank page 🖌 #art #creative #studio",     3456,  89, "Brooklyn, New York"},
        {"🦋", 0xFFF1F8E9, "Transformation is always worth it 🦋 #butterfly #nature #costarica", 11234, 367, "Costa Rica"},
        {"🍣", 0xFFE8F5E9, "Omakase dreams 🍣 #sushi #foodie #japanese #tokyo #omakase",          7654, 213, "Tokyo, Japan"},
        {"🏰", 0xFFE8EAF6, "Fairytales are real, you just have to find them 🏰 #castle #europe", 13421, 445, "Edinburgh, Scotland"},
        {"🌋", 0xFFFFEBEE, "The earth is alive and incredible 🌋 #volcano #hawaii #geology",     16789, 567, "Hawaii Volcanoes NP"},
        {"🎵", 0xFFF3E5F5, "Music is the language everyone speaks 🎵 #festival #coachella #live", 8234, 256, "Coachella Valley, CA"}
    };

    // Comment snippets for post previews
    static final String[] COMMENT_TEXTS = {
        "absolutely stunning 🔥", "this is everything 😍", "goals!!!",
        "why am I not here 😩", "incredible shot!", "pure magic ✨",
        "obsessed with this 🙌", "love love love ❤", "how is this real??",
        "this made my day 🙏", "stunning as always!", "I need to go here",
        "you're so talented!", "can't stop looking at this", "speechless 😭",
        "the vibe!! 💯", "wow wow wow", "send location!!",
        "this is my new wallpaper", "perfection 🤩"
    };

    // ─────────────────────────────────────────────────────────────────────────
    // Data model
    // ─────────────────────────────────────────────────────────────────────────
    static class FeedPost {
        int    postId;
        String displayName, handle;
        String avatarEmoji;
        int    avatarColor;
        String imageEmoji;
        int    imageColor;
        String caption;
        int    likeCount;
        int    commentCount;
        String timeAgo;
        String location;
        boolean liked;
        String  commentUser;
        String  commentText;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Touch / DB
    // ─────────────────────────────────────────────────────────────────────────
    private TouchDbHelper dbHelper;
    private SQLiteDatabase touchDb;
    private VelocityTracker velocityTracker;
    private int  gestureId      = 0;
    private int  moveEventCount = 0;
    private long sessionStart;

    // ─────────────────────────────────────────────────────────────────────────
    // Feed state
    // ─────────────────────────────────────────────────────────────────────────
    private List<FeedPost>       posts;
    private RecyclerView         recyclerView;
    private LinearLayoutManager  layoutManager;
    private final HashMap<Integer, Long> postViewStartTimes = new HashMap<>();

    // ─────────────────────────────────────────────────────────────────────────
    // onCreate
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_insta_feed);

        dbHelper     = new TouchDbHelper(this);
        touchDb      = dbHelper.getWritableDatabase();
        sessionStart = System.currentTimeMillis();

        posts = buildDailyFeed();

        recyclerView  = findViewById(R.id.recycler_insta);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(false);

        InstaAdapter adapter = new InstaAdapter(
                posts,
                (post, pos) -> logFeedEvent(post.liked ? "LIKE" : "UNLIKE", post, pos, 0),
                (post, pos) -> logFeedEvent("DOUBLE_TAP", post, pos, 0),
                (type, post, pos) -> logFeedEvent(type, post, pos, 0)
        );
        recyclerView.setAdapter(adapter);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                trackVisiblePosts();
            }
        });

        recyclerView.post(this::trackVisiblePosts);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Build today's 30-post feed (deterministic per calendar day)
    // ─────────────────────────────────────────────────────────────────────────
    private List<FeedPost> buildDailyFeed() {
        Calendar cal  = Calendar.getInstance();
        long     seed = cal.get(Calendar.YEAR) * 1000L + cal.get(Calendar.DAY_OF_YEAR);
        Random   rng  = new Random(seed);

        // Shuffle post template indices, take first 30
        List<Integer> postIdxs = new ArrayList<>();
        for (int i = 0; i < POST_TEMPLATES.length; i++) postIdxs.add(i);
        Collections.shuffle(postIdxs, rng);
        List<Integer> selected = postIdxs.subList(0, Math.min(30, postIdxs.size()));

        // Shuffle user indices
        List<Integer> userIdxs = new ArrayList<>();
        for (int i = 0; i < USERS.length; i++) userIdxs.add(i);
        Collections.shuffle(userIdxs, rng);

        String[] timeOptions = {
            "1h", "2h", "3h", "4h", "5h", "6h", "8h", "10h", "12h", "15h", "18h", "22h", "1d", "2d"
        };

        List<FeedPost> result = new ArrayList<>();
        for (int i = 0; i < selected.size(); i++) {
            int pidx = selected.get(i);
            int uidx = userIdxs.get(i % userIdxs.size());

            Object[] pt = POST_TEMPLATES[pidx];
            Object[] ut = USERS[uidx];

            FeedPost fp = new FeedPost();
            fp.postId       = pidx;
            fp.displayName  = (String) ut[0];
            fp.handle       = (String) ut[1];
            fp.avatarEmoji  = (String) ut[2];
            fp.avatarColor  = (int)    ut[3];
            fp.imageEmoji   = (String) pt[0];
            fp.imageColor   = (int)    pt[1];
            fp.caption      = (String) pt[2];
            fp.likeCount    = Math.max(10, (int) pt[3] + rng.nextInt(500) - 250);
            fp.commentCount = Math.max(1,  (int) pt[4] + rng.nextInt(50)  - 25);
            fp.location     = (String) pt[5];
            fp.timeAgo      = timeOptions[rng.nextInt(timeOptions.length)];
            fp.liked        = false;

            int commentUidx = userIdxs.get((i + 1) % userIdxs.size());
            fp.commentUser  = (String) USERS[commentUidx][1];
            fp.commentText  = COMMENT_TEXTS[rng.nextInt(COMMENT_TEXTS.length)];

            result.add(fp);
        }
        return result;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Post visibility tracking (logs view duration when posts leave screen)
    // ─────────────────────────────────────────────────────────────────────────
    private void trackVisiblePosts() {
        int first = layoutManager.findFirstVisibleItemPosition();
        int last  = layoutManager.findLastVisibleItemPosition();
        if (first < 0) return;

        // Clamp: adapter position 0 = stories row, posts start at 1
        int firstPost = Math.max(first, 1);
        long now = System.currentTimeMillis();

        // Posts that scrolled off — log their view duration
        List<Integer> toRemove = new ArrayList<>();
        for (int pos : postViewStartTimes.keySet()) {
            if (pos < firstPost || pos > last) {
                long duration = now - postViewStartTimes.get(pos);
                int idx = pos - 1;
                if (idx >= 0 && idx < posts.size()) {
                    logFeedEvent("POST_VIEW", posts.get(idx), pos, duration);
                }
                toRemove.add(pos);
            }
        }
        for (int pos : toRemove) postViewStartTimes.remove(pos);

        // Posts that just entered the viewport
        for (int pos = firstPost; pos <= last; pos++) {
            if (!postViewStartTimes.containsKey(pos)) {
                postViewStartTimes.put(pos, now);
                int idx = pos - 1;
                if (idx >= 0 && idx < posts.size()) {
                    logFeedEvent("POST_ENTER", posts.get(idx), pos, 0);
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Engagement event → feed_events table
    // ─────────────────────────────────────────────────────────────────────────
    private void logFeedEvent(final String eventType, final FeedPost post,
                              final int scrollPos, final long viewDurationMs) {
        final long   ts      = System.currentTimeMillis();
        final int    postId  = post.postId;
        final String handle  = post.handle;
        final long   sess    = sessionStart;

        new Thread(() -> {
            ContentValues cv = new ContentValues();
            cv.put(TouchDbHelper.COL_FEED_EVENT_TYPE,    eventType);
            cv.put(TouchDbHelper.COL_FEED_POST_ID,       postId);
            cv.put(TouchDbHelper.COL_FEED_USERNAME,      handle);
            cv.put(TouchDbHelper.COL_FEED_SCROLL_POS,    scrollPos);
            cv.put(TouchDbHelper.COL_FEED_VIEW_DURATION, viewDurationMs);
            cv.put(TouchDbHelper.COL_TIMESTAMP_MS,       ts);
            cv.put(TouchDbHelper.COL_SESSION_TIMESTAMP,  sess);
            touchDb.insert(TouchDbHelper.FEED_TABLE, null, cv);
        }).start();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Raw touch logging for scroll biometrics
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                gestureId++;
                moveEventCount = 0;
                if (velocityTracker == null) velocityTracker = VelocityTracker.obtain();
                else velocityTracker.clear();
                velocityTracker.addMovement(event);
                saveTouchEvent(event, "DOWN", 0f, 0f);
                break;
            case MotionEvent.ACTION_MOVE:
                if (velocityTracker != null) velocityTracker.addMovement(event);
                moveEventCount++;
                if (moveEventCount % 3 == 0) saveTouchEvent(event, "MOVE", 0f, 0f);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (velocityTracker != null) {
                    velocityTracker.addMovement(event);
                    velocityTracker.computeCurrentVelocity(1000);
                    float vx = velocityTracker.getXVelocity();
                    float vy = velocityTracker.getYVelocity();
                    saveTouchEvent(event,
                        action == MotionEvent.ACTION_UP ? "UP" : "CANCEL", vx, vy);
                    velocityTracker.recycle();
                    velocityTracker = null;
                }
                break;
        }
        return super.dispatchTouchEvent(event);
    }

    private void saveTouchEvent(final MotionEvent e, final String type,
                                final float vx, final float vy) {
        final float x = e.getX(), y = e.getY(), rawX = e.getRawX(), rawY = e.getRawY();
        final float pressure = e.getPressure(), size = e.getSize();
        final long  ts  = System.currentTimeMillis();
        final int   gid = gestureId;
        final long  sess = sessionStart;

        new Thread(() -> {
            ContentValues cv = new ContentValues();
            cv.put(TouchDbHelper.COL_TASK_TYPE,         "insta_feed");
            cv.put(TouchDbHelper.COL_TRIAL_NUM,         0);
            cv.put(TouchDbHelper.COL_GESTURE_ID,        gid);
            cv.put(TouchDbHelper.COL_EVENT_TYPE,        type);
            cv.put(TouchDbHelper.COL_X,                 x);
            cv.put(TouchDbHelper.COL_Y,                 y);
            cv.put(TouchDbHelper.COL_RAW_X,             rawX);
            cv.put(TouchDbHelper.COL_RAW_Y,             rawY);
            cv.put(TouchDbHelper.COL_PRESSURE,          pressure);
            cv.put(TouchDbHelper.COL_TOUCH_SIZE,        size);
            cv.put(TouchDbHelper.COL_TIMESTAMP_MS,      ts);
            cv.put(TouchDbHelper.COL_VELOCITY_X,        vx);
            cv.put(TouchDbHelper.COL_VELOCITY_Y,        vy);
            cv.put(TouchDbHelper.COL_SESSION_TIMESTAMP, sess);
            touchDb.insert(TouchDbHelper.TABLE, null, cv);
        }).start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        long now = System.currentTimeMillis();
        for (int pos : postViewStartTimes.keySet()) {
            int idx = pos - 1;
            if (idx >= 0 && idx < posts.size()) {
                logFeedEvent("POST_VIEW", posts.get(idx), pos, now - postViewStartTimes.get(pos));
            }
        }
        postViewStartTimes.clear();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (touchDb != null) touchDb.close();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Callback interfaces
    // ─────────────────────────────────────────────────────────────────────────
    interface OnLikeCallback       { void onLike(FeedPost post, int pos); }
    interface OnDoubleTapCallback  { void onDoubleTap(FeedPost post, int pos); }
    interface OnEngagementCallback { void onEvent(String type, FeedPost post, int pos); }

    // ─────────────────────────────────────────────────────────────────────────
    // RecyclerView Adapter
    // ─────────────────────────────────────────────────────────────────────────
    static class InstaAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        static final int TYPE_STORIES = 0;
        static final int TYPE_POST    = 1;

        private final List<FeedPost>       posts;
        private final OnLikeCallback       onLike;
        private final OnDoubleTapCallback  onDoubleTap;
        private final OnEngagementCallback onEngagement;

        InstaAdapter(List<FeedPost> posts, OnLikeCallback onLike,
                     OnDoubleTapCallback onDoubleTap, OnEngagementCallback onEngagement) {
            this.posts       = posts;
            this.onLike      = onLike;
            this.onDoubleTap = onDoubleTap;
            this.onEngagement = onEngagement;
        }

        @Override public int getItemViewType(int position) {
            return position == 0 ? TYPE_STORIES : TYPE_POST;
        }

        @Override public int getItemCount() { return posts.size() + 1; } // +1 for stories

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inf = LayoutInflater.from(parent.getContext());
            if (viewType == TYPE_STORIES) {
                return new StoriesVH(inf.inflate(R.layout.item_stories_row, parent, false));
            }
            return new PostVH(inf.inflate(R.layout.item_insta_post, parent, false), onDoubleTap);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof StoriesVH) {
                ((StoriesVH) holder).bind(posts);
            } else {
                int idx = position - 1; // offset past stories row
                ((PostVH) holder).bind(posts.get(idx), idx, onLike, onEngagement);
            }
        }

        // ── Stories row ──────────────────────────────────────────────────────
        static class StoriesVH extends RecyclerView.ViewHolder {
            LinearLayout container;
            StoriesVH(View v) {
                super(v);
                container = v.findViewById(R.id.stories_container);
            }

            void bind(List<FeedPost> posts) {
                container.removeAllViews();
                android.content.Context ctx = itemView.getContext();

                addCircle(ctx, "➕", 0xFF12111A, "Your Story", null);

                int count = Math.min(13, posts.size());
                for (int i = 0; i < count; i++) {
                    FeedPost p = posts.get(i);
                    String shortHandle = p.handle.length() > 9 ? p.handle.substring(0, 8) + "…" : p.handle;
                    addCircle(ctx, p.avatarEmoji, p.avatarColor, shortHandle, p);
                }
            }

            private void showStoryDialog(android.content.Context ctx, FeedPost post) {
                android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(ctx);

                android.widget.LinearLayout root = new android.widget.LinearLayout(ctx);
                root.setOrientation(android.widget.LinearLayout.VERTICAL);
                root.setBackgroundColor(0xFF12111A);
                root.setGravity(android.view.Gravity.CENTER);
                root.setPadding(0, dp(ctx, 48), 0, dp(ctx, 48));

                // Progress bar at top
                android.widget.LinearLayout progressRow = new android.widget.LinearLayout(ctx);
                progressRow.setOrientation(android.widget.LinearLayout.HORIZONTAL);
                progressRow.setPadding(dp(ctx, 16), 0, dp(ctx, 16), dp(ctx, 24));
                progressRow.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.WRAP_CONTENT));

                for (int i = 0; i < 3; i++) {
                    android.view.View seg = new android.view.View(ctx);
                    android.widget.LinearLayout.LayoutParams lp =
                            new android.widget.LinearLayout.LayoutParams(0, dp(ctx, 2), 1f);
                    lp.setMarginEnd(dp(ctx, 4));
                    seg.setLayoutParams(lp);
                    seg.setBackgroundColor(i == 0 ? 0xFFD4A843 : 0x55FFFFFF);
                    progressRow.addView(seg);
                }
                root.addView(progressRow);

                // Avatar circle
                android.widget.FrameLayout avatarFrame = new android.widget.FrameLayout(ctx);
                int avSize = dp(ctx, 80);
                android.widget.FrameLayout.LayoutParams avfp =
                        new android.widget.FrameLayout.LayoutParams(avSize, avSize);
                avfp.gravity = android.view.Gravity.CENTER_HORIZONTAL;
                avatarFrame.setLayoutParams(avfp);

                android.widget.TextView avText = new android.widget.TextView(ctx);
                avText.setLayoutParams(new android.widget.FrameLayout.LayoutParams(avSize, avSize));
                avText.setText(post.avatarEmoji);
                avText.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 32);
                avText.setGravity(android.view.Gravity.CENTER);
                android.graphics.drawable.GradientDrawable avBg = new android.graphics.drawable.GradientDrawable();
                avBg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
                avBg.setColor(post.avatarColor);
                avBg.setStroke(dp(ctx, 3), 0xFFD4A843);
                avText.setBackground(avBg);
                avatarFrame.addView(avText);

                android.widget.LinearLayout.LayoutParams avContLp =
                        new android.widget.LinearLayout.LayoutParams(
                                android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                                android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
                avContLp.gravity = android.view.Gravity.CENTER_HORIZONTAL;
                avatarFrame.setLayoutParams(avContLp);
                root.addView(avatarFrame);

                // Handle
                android.widget.TextView handleTv = new android.widget.TextView(ctx);
                android.widget.LinearLayout.LayoutParams hlp =
                        new android.widget.LinearLayout.LayoutParams(
                                android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                                android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
                hlp.gravity = android.view.Gravity.CENTER_HORIZONTAL;
                hlp.topMargin = dp(ctx, 10);
                handleTv.setLayoutParams(hlp);
                handleTv.setText("@" + post.handle);
                handleTv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 15);
                handleTv.setTextColor(0xFFFFFFFF);
                handleTv.setTypeface(null, android.graphics.Typeface.BOLD);
                root.addView(handleTv);

                // Story content card
                android.widget.FrameLayout storyCard = new android.widget.FrameLayout(ctx);
                android.widget.LinearLayout.LayoutParams sclp =
                        new android.widget.LinearLayout.LayoutParams(
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT, dp(ctx, 280));
                sclp.topMargin = dp(ctx, 20);
                sclp.leftMargin = dp(ctx, 20);
                sclp.rightMargin = dp(ctx, 20);
                storyCard.setLayoutParams(sclp);

                android.view.View cardBg = new android.view.View(ctx);
                cardBg.setLayoutParams(new android.widget.FrameLayout.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT));
                cardBg.setBackgroundColor(post.imageColor);
                storyCard.addView(cardBg);

                android.widget.TextView storyEmoji = new android.widget.TextView(ctx);
                storyEmoji.setLayoutParams(new android.widget.FrameLayout.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT));
                storyEmoji.setText(post.imageEmoji);
                storyEmoji.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 72);
                storyEmoji.setGravity(android.view.Gravity.CENTER);
                storyCard.addView(storyEmoji);
                root.addView(storyCard);

                // Caption
                android.widget.TextView capTv = new android.widget.TextView(ctx);
                android.widget.LinearLayout.LayoutParams caplp =
                        new android.widget.LinearLayout.LayoutParams(
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
                caplp.topMargin = dp(ctx, 16);
                caplp.leftMargin = dp(ctx, 24);
                caplp.rightMargin = dp(ctx, 24);
                capTv.setLayoutParams(caplp);
                capTv.setText(post.caption);
                capTv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 13);
                capTv.setTextColor(0xFFBDB08A);
                capTv.setMaxLines(2);
                capTv.setEllipsize(android.text.TextUtils.TruncateAt.END);
                root.addView(capTv);

                builder.setView(root);
                builder.setPositiveButton("Close", null);

                android.app.AlertDialog dialog = builder.create();
                if (dialog.getWindow() != null) {
                    dialog.getWindow().setBackgroundDrawable(
                            new android.graphics.drawable.ColorDrawable(0xFF12111A));
                }
                dialog.show();
                // Style the close button gold
                dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
                      .setTextColor(0xFFD4A843);
            }

            private void addCircle(android.content.Context ctx,
                                   String emoji, int color, String label, FeedPost post) {
                LinearLayout item = new LinearLayout(ctx);
                item.setOrientation(LinearLayout.VERTICAL);
                item.setGravity(Gravity.CENTER_HORIZONTAL);
                item.setPadding(dp(ctx, 8), dp(ctx, 10), dp(ctx, 8), dp(ctx, 10));

                // Story ring (thin gradient-like border)
                FrameLayout ring = new FrameLayout(ctx);
                int ringSize = dp(ctx, 70);
                ring.setLayoutParams(new FrameLayout.LayoutParams(ringSize, ringSize));
                GradientDrawable ringBg = new GradientDrawable();
                ringBg.setShape(GradientDrawable.OVAL);
                ringBg.setStroke(dp(ctx, 2), 0xFFD4A843); // Monarch gold ring
                ringBg.setColor(0x00000000);
                ring.setBackground(ringBg);

                // Avatar circle inside ring
                TextView avatar = new TextView(ctx);
                int avSize = dp(ctx, 60);
                FrameLayout.LayoutParams avp = new FrameLayout.LayoutParams(avSize, avSize);
                avp.gravity = Gravity.CENTER;
                avatar.setLayoutParams(avp);
                avatar.setText(emoji);
                avatar.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 24);
                avatar.setGravity(Gravity.CENTER);
                GradientDrawable avBg = new GradientDrawable();
                avBg.setShape(GradientDrawable.OVAL);
                avBg.setColor(color);
                avatar.setBackground(avBg);
                ring.addView(avatar);

                // Username label
                TextView lbl = new TextView(ctx);
                lbl.setText(label);
                lbl.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 10);
                lbl.setTextColor(0xFFBDB08A);
                lbl.setMaxLines(1);
                lbl.setPadding(0, dp(ctx, 5), 0, 0);

                item.addView(ring);
                item.addView(lbl);

                // Tap to view story
                if (post != null) {
                    final FeedPost fp = post;
                    item.setOnClickListener(v -> showStoryDialog(ctx, fp));
                }

                container.addView(item);
            }

            static int dp(android.content.Context ctx, int dp) {
                return Math.round(dp * ctx.getResources().getDisplayMetrics().density);
            }
        }

        // ── Post item ─────────────────────────────────────────────────────────
        static class PostVH extends RecyclerView.ViewHolder {
            FrameLayout flAvatar, flImage;
            TextView    tvAvatarEmoji, tvUsername, tvLocation;
            View        viewImageBg;
            TextView    tvImageEmoji, tvHeartOverlay;
            TextView    tvLikeBtn, tvCommentBtn, tvShareBtn, tvBookmarkBtn;
            TextView    tvLikeCount, tvCaption, tvViewComments, tvCommentPreview, tvTimestamp;

            FeedPost boundPost;
            int      boundIdx;
            GestureDetector gestureDetector;

            PostVH(View v, OnDoubleTapCallback onDblTap) {
                super(v);
                flAvatar         = v.findViewById(R.id.fl_avatar);
                tvAvatarEmoji    = v.findViewById(R.id.tv_avatar_emoji);
                tvUsername       = v.findViewById(R.id.tv_username);
                tvLocation       = v.findViewById(R.id.tv_location);
                flImage          = v.findViewById(R.id.fl_image);
                viewImageBg      = v.findViewById(R.id.view_image_bg);
                tvImageEmoji     = v.findViewById(R.id.tv_image_emoji);
                tvHeartOverlay   = v.findViewById(R.id.tv_heart_overlay);
                tvLikeBtn        = v.findViewById(R.id.tv_like_btn);
                tvCommentBtn     = v.findViewById(R.id.tv_comment_btn);
                tvShareBtn       = v.findViewById(R.id.tv_share_btn);
                tvBookmarkBtn    = v.findViewById(R.id.tv_bookmark_btn);
                tvLikeCount      = v.findViewById(R.id.tv_like_count);
                tvCaption        = v.findViewById(R.id.tv_caption);
                tvViewComments   = v.findViewById(R.id.tv_view_comments);
                tvCommentPreview = v.findViewById(R.id.tv_comment_preview);
                tvTimestamp      = v.findViewById(R.id.tv_timestamp);

                // Double-tap gesture on the image
                gestureDetector = new GestureDetector(v.getContext(),
                        new GestureDetector.SimpleOnGestureListener() {
                    @Override public boolean onDown(MotionEvent e) { return true; }

                    @Override
                    public boolean onDoubleTap(MotionEvent e) {
                        if (boundPost == null) return true;
                        if (!boundPost.liked) {
                            boundPost.liked = true;
                            boundPost.likeCount++;
                            tvLikeBtn.setText("\u2764"); // ❤ (filled heart, red)
                            tvLikeBtn.setTextColor(0xFFEE2244);
                            refreshLikeCount();
                        }
                        animateHeart();
                        if (onDblTap != null) onDblTap.onDoubleTap(boundPost, boundIdx);
                        return true;
                    }
                });

                flImage.setOnTouchListener((view, event) -> {
                    gestureDetector.onTouchEvent(event);
                    return false; // let the event propagate for scrolling
                });
            }

            void bind(FeedPost post, int idx,
                      OnLikeCallback onLike, OnEngagementCallback onEngagement) {
                boundPost = post;
                boundIdx  = idx;

                // Avatar
                GradientDrawable avBg = new GradientDrawable();
                avBg.setShape(GradientDrawable.OVAL);
                avBg.setColor(post.avatarColor);
                // Story ring stroke (Monarch gold)
                avBg.setStroke(3, 0xFFD4A843);
                flAvatar.setBackground(avBg);
                tvAvatarEmoji.setText(post.avatarEmoji);

                // Header text
                tvUsername.setText(post.handle);
                tvLocation.setText(post.location);

                // Image
                viewImageBg.setBackgroundColor(post.imageColor);
                tvImageEmoji.setText(post.imageEmoji);

                // Like button state
                if (post.liked) {
                    tvLikeBtn.setText("\u2764");
                    tvLikeBtn.setTextColor(0xFFEE2244);
                } else {
                    tvLikeBtn.setText("\u2661"); // ♡ outline heart
                    tvLikeBtn.setTextColor(0xFFDDDDDD);
                }
                refreshLikeCount();

                // Caption with bold username prefix
                SpannableString cap = new SpannableString(post.handle + "  " + post.caption);
                cap.setSpan(new StyleSpan(Typeface.BOLD), 0, post.handle.length(),
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                tvCaption.setText(cap);

                // Comments
                tvViewComments.setText("View all " + fmt(post.commentCount) + " comments");

                // Comment preview with bold commenter handle
                SpannableString cmt = new SpannableString(post.commentUser + "  " + post.commentText);
                cmt.setSpan(new StyleSpan(Typeface.BOLD), 0, post.commentUser.length(),
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                tvCommentPreview.setText(cmt);

                // Timestamp
                tvTimestamp.setText(post.timeAgo.toUpperCase(Locale.US) + " AGO");

                // Reset heart overlay state
                tvHeartOverlay.setVisibility(View.GONE);
                tvHeartOverlay.setAlpha(1f);
                tvHeartOverlay.setScaleX(1f);
                tvHeartOverlay.setScaleY(1f);

                // ── Click listeners ──
                tvLikeBtn.setOnClickListener(v -> {
                    post.liked = !post.liked;
                    post.likeCount = Math.max(0, post.likeCount + (post.liked ? 1 : -1));
                    if (post.liked) {
                        tvLikeBtn.setText("\u2764");
                        tvLikeBtn.setTextColor(0xFFEE2244);
                    } else {
                        tvLikeBtn.setText("\u2661");
                        tvLikeBtn.setTextColor(0xFFDDDDDD);
                    }
                    refreshLikeCount();
                    if (onLike != null) onLike.onLike(post, idx);
                });

                tvCommentBtn.setOnClickListener(v -> {
                    if (onEngagement != null) onEngagement.onEvent("COMMENT_TAP", post, idx);
                    openComments(v.getContext(), post);
                });

                tvViewComments.setOnClickListener(v -> openComments(v.getContext(), post));

                tvShareBtn.setOnClickListener(v -> {
                    if (onEngagement != null) onEngagement.onEvent("SHARE_TAP", post, idx);
                });

                flAvatar.setOnClickListener(v -> {
                    if (onEngagement != null) onEngagement.onEvent("PROFILE_TAP", post, idx);
                });
                tvUsername.setOnClickListener(v -> {
                    if (onEngagement != null) onEngagement.onEvent("PROFILE_TAP", post, idx);
                });

                tvBookmarkBtn.setOnClickListener(v -> {
                    if (onEngagement != null) onEngagement.onEvent("BOOKMARK_TAP", post, idx);
                });
            }

            static void openComments(android.content.Context ctx, FeedPost post) {
                android.content.Intent intent = new android.content.Intent(ctx, CommentsActivity.class);
                intent.putExtra("postId",         post.postId);
                intent.putExtra("postHandle",     post.handle);
                intent.putExtra("postCaption",    post.caption);
                intent.putExtra("postImageEmoji", post.imageEmoji);
                intent.putExtra("postImageColor", post.imageColor);
                intent.putExtra("postAvatarEmoji",post.avatarEmoji);
                intent.putExtra("postAvatarColor",post.avatarColor);
                ctx.startActivity(intent);
            }

            void refreshLikeCount() {
                if (boundPost != null)
                    tvLikeCount.setText(fmt(boundPost.likeCount) + " likes");
            }

            void animateHeart() {
                tvHeartOverlay.setVisibility(View.VISIBLE);
                tvHeartOverlay.setAlpha(1f);
                tvHeartOverlay.setScaleX(0f);
                tvHeartOverlay.setScaleY(0f);
                tvHeartOverlay.animate()
                    .scaleX(1.25f).scaleY(1.25f).setDuration(180)
                    .withEndAction(() ->
                        tvHeartOverlay.animate()
                            .scaleX(1.0f).scaleY(1.0f).setDuration(100)
                            .withEndAction(() ->
                                tvHeartOverlay.animate()
                                    .alpha(0f).setStartDelay(550).setDuration(350)
                                    .withEndAction(() -> tvHeartOverlay.setVisibility(View.GONE))
                                    .start())
                            .start())
                    .start();
            }

            static String fmt(int n) {
                if (n >= 1_000_000) return String.format(Locale.US, "%.1fM", n / 1_000_000f);
                if (n >= 1_000)     return String.format(Locale.US, "%.1fK", n / 1_000f);
                return String.valueOf(n);
            }
        }
    }
}
