package com.monarch.software.phase2;

import com.monarch.software.R;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Random;

final class SocialFeedContent {

    static final class User {
        final String displayName;
        final String handle;
        final int avatarResId;
        final boolean verified;

        User(String displayName, String handle, int avatarResId, boolean verified) {
            this.displayName = displayName;
            this.handle = handle;
            this.avatarResId = avatarResId;
            this.verified = verified;
        }
    }

    private static final class PostTemplate {
        final int mediaResId;
        final String mediaDescription;
        final String caption;
        final int baseLikes;
        final int baseComments;
        final String location;

        PostTemplate(int mediaResId, String mediaDescription, String caption,
                     int baseLikes, int baseComments, String location) {
            this.mediaResId = mediaResId;
            this.mediaDescription = mediaDescription;
            this.caption = caption;
            this.baseLikes = baseLikes;
            this.baseComments = baseComments;
            this.location = location;
        }
    }

    static final class FeedPost {
        int postId;
        User user;
        int mediaResId;
        String mediaDescription;
        String caption;
        int likeCount;
        int commentCount;
        String timeAgo;
        String location;
        boolean liked;
        boolean saved;
        boolean captionExpanded;
        User commentUser;
        String commentText;
    }

    static final User[] USERS = {
        new User("Maya Chen", "maya.frames", R.drawable.social_flowers_thumb, true),
        new User("Noah Williams", "noahwanders", R.drawable.social_mountain_thumb, false),
        new User("Sofia Patel", "sofiaeats", R.drawable.social_salad_thumb, false),
        new User("Eli Brooks", "elibrooks.photo", R.drawable.social_city_thumb, true),
        new User("Leah Morgan", "leahathome", R.drawable.social_interior_thumb, false),
        new User("Mateo Rivera", "mateo.outside", R.drawable.social_redwoods_thumb, false),
        new User("Amara Davis", "amaradaily", R.drawable.social_sunrise_thumb, false),
        new User("Owen Kim", "owenmakes", R.drawable.social_workspace_thumb, false),
        new User("Nora Ali", "noraonfilm", R.drawable.social_street_thumb, true),
        new User("Theo Bennett", "theobakes", R.drawable.social_bread_thumb, false),
        new User("Isla Thompson", "isla.reads", R.drawable.social_books_thumb, false),
        new User("Lucas Grant", "lucas.coast", R.drawable.social_beach_thumb, false),
        new User("Avery Scott", "averybuilds", R.drawable.social_architecture_thumb, true),
        new User("Mila Carter", "milagrows", R.drawable.social_meadow_thumb, false),
        new User("Ezra Lewis", "ezra.afterdark", R.drawable.social_night_sky_thumb, false),
        new User("Layla Reed", "layla.table", R.drawable.social_restaurant_thumb, false),
        new User("Julian Price", "julianrides", R.drawable.social_bicycle_thumb, false),
        new User("Aria Foster", "aria.bythelake", R.drawable.social_lake_thumb, false),
        new User("Caleb Ross", "calebandmilo", R.drawable.social_dog_thumb, false),
        new User("Zoe Turner", "zoe.studio", R.drawable.social_home_thumb, true),
        new User("Miles Nguyen", "milesinthewoods", R.drawable.social_forest_thumb, false),
        new User("Nina Clarke", "ninasees", R.drawable.social_ocean_thumb, false),
        new User("Sam Cole", "samcoffeeclub", R.drawable.social_coffee_thumb, false)
    };

    private static final PostTemplate[] POSTS = {
        new PostTemplate(R.drawable.social_sunrise,
                "Warm sunrise over a quiet field",
                "Up before the alarms and completely worth it. The whole field changed color for about three minutes.",
                1842, 64, "Sonoma County, California"),
        new PostTemplate(R.drawable.social_mountain,
                "Snow-covered mountain range beneath soft clouds",
                "The trail was still frozen at the top, but the view made us forget the cold.",
                3215, 91, "Banff, Alberta"),
        new PostTemplate(R.drawable.social_coffee,
                "Fresh coffee on a wooden cafe table",
                "A slow pour, a window seat, and nowhere to be for the next hour.",
                728, 28, "Seattle, Washington"),
        new PostTemplate(R.drawable.social_beach,
                "Clear turquoise water meeting a white sand beach",
                "No schedule today. Just salt water, a paperback, and the long way home.",
                4921, 137, "Tulum, Mexico"),
        new PostTemplate(R.drawable.social_street,
                "Busy downtown street framed by tall buildings",
                "The city right before rush hour: loud, bright, and somehow perfectly still in one frame.",
                2106, 52, "Chicago, Illinois"),
        new PostTemplate(R.drawable.social_forest,
                "Sunlight filtering through a dense green forest",
                "Took the path without a sign and found the quietest part of the forest.",
                2630, 77, "Olympic Peninsula"),
        new PostTemplate(R.drawable.social_home,
                "Modern white home with clean geometric lines",
                "Material study: warm wood, white concrete, and a courtyard that catches the afternoon light.",
                1187, 34, "Austin, Texas"),
        new PostTemplate(R.drawable.social_flowers,
                "Small colorful flowers in soft natural light",
                "The corner shop had the first spring stems in today, so these came home with me.",
                946, 31, "Portland, Oregon"),
        new PostTemplate(R.drawable.social_salad,
                "Colorful seasonal salad served in a ceramic bowl",
                "Lunch built from whatever looked best at the market this morning. The citrus dressing is staying in rotation.",
                1564, 46, "Los Angeles, California"),
        new PostTemplate(R.drawable.social_restaurant,
                "Warmly lit dining room with neatly set tables",
                "A neighborhood table that gets every little detail right—from the music to the last plate.",
                883, 22, "Brooklyn, New York"),
        new PostTemplate(R.drawable.social_ocean,
                "Deep blue ocean water with a textured surface",
                "The water looked almost metallic after the storm passed.",
                3754, 104, "Big Sur, California"),
        new PostTemplate(R.drawable.social_architecture,
                "Contemporary concrete building against a clear sky",
                "Lines, shadow, repetition. I walked around this block twice waiting for the light to land here.",
                2431, 58, "Mexico City, Mexico"),
        new PostTemplate(R.drawable.social_lake,
                "Still mountain lake surrounded by evergreen trees",
                "Coffee tasted better from this dock. That is the entire review.",
                5310, 149, "Lake Louise, Alberta"),
        new PostTemplate(R.drawable.social_meadow,
                "Wide green meadow beneath an open sky",
                "A little wind, no signal, and more space than we knew what to do with.",
                1372, 39, "Willamette Valley, Oregon"),
        new PostTemplate(R.drawable.social_redwoods,
                "Tall redwood trees viewed from the forest floor",
                "Everything gets quieter under trees this old.",
                2998, 83, "Humboldt County, California"),
        new PostTemplate(R.drawable.social_interior,
                "Bright living room with neutral furniture and plants",
                "Moved the reading chair into the morning light and now this is everyone’s favorite corner.",
                675, 19, "Minneapolis, Minnesota"),
        new PostTemplate(R.drawable.social_workspace,
                "Minimal desk workspace beside a sunlit window",
                "Reset the studio before starting the next project. Clear desk, clear head—or at least that’s the plan.",
                812, 27, "Toronto, Ontario"),
        new PostTemplate(R.drawable.social_city,
                "Dense city skyline stretching toward the horizon",
                "Twenty floors up and the streets turn into patterns.",
                3450, 96, "New York, New York"),
        new PostTemplate(R.drawable.social_night_sky,
                "Star-filled night sky above a dark landscape",
                "We let our eyes adjust, turned every light off, and the sky did the rest.",
                6184, 202, "Death Valley, California"),
        new PostTemplate(R.drawable.social_dog,
                "Golden retriever sitting outdoors",
                "Milo heard the word ‘walk’ from two rooms away.",
                4572, 173, "Boulder, Colorado"),
        new PostTemplate(R.drawable.social_bread,
                "Freshly baked loaves cooling on a bakery counter",
                "The first batch was out before sunrise. Nothing in the kitchen smells better.",
                1291, 44, "Montreal, Quebec"),
        new PostTemplate(R.drawable.social_books,
                "Tall library shelves filled with books",
                "Came in for one title. Left when they announced closing.",
                1038, 36, "Boston, Massachusetts"),
        new PostTemplate(R.drawable.social_bicycle,
                "Classic bicycle parked on a quiet street",
                "The best way to learn a city is still two wheels and no fixed route.",
                1677, 51, "Copenhagen, Denmark")
    };

    private static final int[] POST_USER_INDEXES = {
        6, 1, 22, 11, 8, 20, 19, 0, 2, 15, 21, 12,
        17, 13, 5, 4, 7, 3, 14, 18, 9, 10, 16
    };

    static final String[] COMMENT_TEXTS = {
        "The light in this is so good.",
        "Adding this to the weekend list.",
        "That last detail makes the whole frame.",
        "This feels so peaceful.",
        "Okay, I need the full story behind this.",
        "The colors are unreal.",
        "You always find the best corners.",
        "Saving this for later.",
        "This made my morning.",
        "Such a beautiful shot.",
        "I was just there last month!",
        "The composition is perfect.",
        "Now I want to take the long way home.",
        "This is exactly the kind of day I need.",
        "Where exactly was this taken?",
        "The texture in this frame is everything.",
        "Instant favorite.",
        "I can almost hear this photo.",
        "Please share the recipe.",
        "Worth the early start for sure."
    };

    private SocialFeedContent() {}

    static List<FeedPost> buildDailyFeed() {
        Calendar calendar = Calendar.getInstance();
        long seed = calendar.get(Calendar.YEAR) * 1000L
                + calendar.get(Calendar.DAY_OF_YEAR);
        Random random = new Random(seed);

        List<Integer> postIndexes = new ArrayList<>();
        for (int i = 0; i < POSTS.length; i++) postIndexes.add(i);
        Collections.shuffle(postIndexes, random);

        List<Integer> userIndexes = new ArrayList<>();
        for (int i = 0; i < USERS.length; i++) userIndexes.add(i);
        Collections.shuffle(userIndexes, random);

        String[] timeOptions = {
            "18m", "32m", "1h", "2h", "3h", "4h", "5h", "7h", "9h", "12h", "1d", "2d"
        };

        int count = Math.min(20, postIndexes.size());
        List<FeedPost> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            int templateIndex = postIndexes.get(i);
            PostTemplate template = POSTS[templateIndex];
            User user = USERS[POST_USER_INDEXES[templateIndex]];

            FeedPost post = new FeedPost();
            post.postId = templateIndex;
            post.user = user;
            post.mediaResId = template.mediaResId;
            post.mediaDescription = template.mediaDescription;
            post.caption = template.caption;
            post.likeCount = Math.max(12, template.baseLikes + random.nextInt(240) - 120);
            post.commentCount = Math.max(2, template.baseComments + random.nextInt(20) - 10);
            post.location = template.location;
            post.timeAgo = timeOptions[random.nextInt(timeOptions.length)];
            post.commentUser = USERS[userIndexes.get((i + 3) % userIndexes.size())];
            post.commentText = COMMENT_TEXTS[random.nextInt(COMMENT_TEXTS.length)];
            result.add(post);
        }
        return result;
    }
}
