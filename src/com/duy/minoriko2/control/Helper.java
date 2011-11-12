/**
 * Copyright (c) 2011 Duy Truong <hduyudh@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.duy.minoriko2.control;

import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

import com.duy.minoriko2.model.Server;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;

public class Helper {
    public static final String TAG = "Minoriko";

    public static final int POST_PER_PAGE = (Build.VERSION.SDK_INT <= Build.VERSION_CODES.GINGERBREAD_MR1) ? 17 : 100;
    public static final int POOL_PER_PAGE = 20;
    public static final int TAG_PER_PAGE = 50;

    public static final int IMGVIEW_REQCODE = 1;

    public static final int RATING_SAFE = 1;
    public static final int RATING_QUESTIONNABLE = 2;
    public static final int RATING_EXPLICIT = 3;

    public static final int MAX_SIZE_FOR_RAM = 500000;

    /**
     * Prepend "http://" and append "/" to url as needed
     * TODO: HTTPS support
     * @param url
     * @return url, prefixed with http:// and ended with /
     */
    public static String baseUrlCheck(String url) {
        return (url.startsWith("http://") ? "" : "http://") +
                url + (url.endsWith("/") ? "" : "/");
    }

    public static String urlCheck(Context context, String url) {
        if (url.startsWith("http://")) {
            return url;
        } else {
            return getServerRoot(context) + url;
        }
    }

    public static String getServerRoot(Context context) {
        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString("danbo_servers_list",
                "http://hijiribe.donmai.us/");
    }
    public static Server.Type getServerType(Context context) {
        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(context);
        return Server.Type.valueOf(
                prefs.getString("danbo_servers_type", "DANBOORU"));
    }

    public static void setServerRoot(Context context, String url,
            Server.Type type) {
        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("danbo_servers_list", url);
        editor.putString("danbo_servers_type", type.name());
        editor.commit();
    }

    /* Functions related to tags url construction */
    private static String getTagsUrlPrefix(Context context) {
        switch (getServerType(context)) {
        case GELBOORU:
            return getServerRoot(context) +
                    "/index.php?page=dapi&s=tag&q=index&";

        case DANBOORU:
            return getServerRoot(context) +
                    "/tag/index.xml?";

        default:
            return "";
        }
    }
    public static String getTagsUrl(Context context, String query) {
        switch (getServerType(context)) {
        case GELBOORU:
            return getTagsUrlPrefix(context) +
                    "limit=" + (query.length() >= 3 ? "0" : "100") +
                    "&order=" + (query.length() >= 3 ? "count" : "") +
                    "&name_pattern=" + URLEncoder.encode(query);

        case DANBOORU:
            return getTagsUrlPrefix(context) + "limit=100&order=count" +
                    "&name=*" + URLEncoder.encode(query) + "*";

        default:
            return "";
        }
    }
    public static boolean isTagsSortDesc(Context context, String query) {
        return !(getServerType(context) == Server.Type.GELBOORU &&
                query.length() >= 3);
    }

    /* Functions related to pool list url construction */
    public static String getPoolsUrl(Context context, String query, int page) {
        String serverRoot = getServerRoot(context);
        switch (getServerType(context)) {
        case GELBOORU:
            return "";

        case DANBOORU:
            return serverRoot + "/pool/index.xml?" +
                    "query=" + URLEncoder.encode(query) +
                    "&page=" + page;

        default:
            return "";
        }
    }
    public static boolean isPoolsWorking(Context context) {
        switch (getServerType(context)) {
        case GELBOORU:
            return false;

        case DANBOORU:
            return true;

        default:
            return false;
        }
    }

    /* Functions related to post list url construction */
    public static String getPostListUrl(Context context,
            String query, int page) {
        String serverRoot = getServerRoot(context);

        switch (getServerType(context)) {
        case GELBOORU:
            return serverRoot + "/index.php?page=dapi&s=post&q=index&" +
                    "tags=" + URLEncoder.encode(query) +
                    "&limit=" + POST_PER_PAGE +
                    "&pid=" + (page == -1 ? "" : String.valueOf(page-1));
            // gelbooru number pages from 0, not 1 like danbooru

        case DANBOORU:
            return serverRoot + "/post/index.xml?" +
                    "tags=" + URLEncoder.encode(query) +
                    "&limit=" + POST_PER_PAGE +
                    "&page=" + (page == -1 ? "" : String.valueOf(page));

        default:
            return "";
        }
    }

    /* Functions related to comments */
    public static String getCommentsUrl(Context context, long post_id) {
        String serverRoot = getServerRoot(context);

        switch (getServerType(context)) {
        case GELBOORU:
            return serverRoot + "/index.php?page=dapi&s=comment&q=index&" +
                    "post_id=" + post_id;

        case DANBOORU:
            return serverRoot + "/comment/index.xml?post_id=" + post_id;

        default:
            return "";
        }
    }
    public static boolean isCommentsTimeSortBackward(Context context) {
        return !(getServerType(context) == Server.Type.GELBOORU);
    }
    public static String getCommentTime(Context context, String tStr) {
        SimpleDateFormat parser;
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd",
                Locale.US);

        try {
            parser = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
            return formatter.format(parser.parse(tStr));
        } catch (ParseException e) {}

        try {
            parser = new SimpleDateFormat("EEE MMM dd HH:mm:ss Z yyyy",
                    Locale.US);
            return formatter.format(parser.parse(tStr));
        } catch (ParseException e) {}

        return "";
    }

    /* Functions related to notes */
    public static String getNotesUrl(Context context, long post_id) {
        String serverRoot = getServerRoot(context);

        switch (getServerType(context)) {
        case GELBOORU:
            return serverRoot +
                    "/index.php?page=dapi&s=note&q=index&post_id=" + post_id;

        case DANBOORU:
            return serverRoot + "/note/index.xml?"+ "post_id=" + post_id;

        default:
            return "";
        }
    }

    /* Functions related to ratings */
    public static int getRating(Context context) {
        return getRatingInt(
                PreferenceManager.getDefaultSharedPreferences(context)
                .getString("danbo_filter", "s"));
    }
    public static String getRatingString(Context context) {
        switch (getRating(context)) {
        case RATING_SAFE:
            return "Safe";
        case RATING_QUESTIONNABLE:
            return "Questionnable";
        case RATING_EXPLICIT:
            return "Explicit";
        default:
            return "";
        }
    }
    public static String getRatingString(String ratingChar) {
        if ("s".equals(ratingChar)) {
            return "Safe";
        } else if ("q".equals(ratingChar)) {
            return "Questionnable";
        } else if ("e".equals(ratingChar)) {
            return "Explicit";
        } else {
            return "";
        }
    }
    public static int getRatingInt(String ratingChar) {
        if ("s".equals(ratingChar)) {
            return Helper.RATING_SAFE;
        } else if ("q".equals(ratingChar)) {
            return Helper.RATING_QUESTIONNABLE;
        } else if ("e".equals(ratingChar)) {
            return Helper.RATING_EXPLICIT;
        } else {
            return Helper.RATING_SAFE;
        }
    }

    /**
     * Check to see if the url is supported by the image view.
     * Currently, supported file types are jpg, jpeg, png, gif, and bmp
     * @param url
     * @return true/false
     */
    public static boolean isSupported(String url) {
        return url.endsWith("jpg") || url.endsWith("jpeg") ||
                url.endsWith("png") || url.endsWith("gif") ||
                url.endsWith("bmp");
    }

    /**
     * Get a human-readable String representing file size
     * @param bytes
     * @param si true if using SI, false if using binary
     * @return
     */
    public static String humanReadableByteCount(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "i" : "");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    /**
     * Launch the url in a browser activity
     * @param url
     */
    public static void launchBrowser(Activity act, String url) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.addCategory(Intent.CATEGORY_BROWSABLE);
        intent.setData(Uri.parse(url));
        act.startActivity(intent);
    }

    /**
     * Get the website URL for a post
     * @param post_id the id of the post
     */
    public static String getWebPostURL(Context context, int post_id) {
        String serverRoot = getServerRoot(context);

        switch(getServerType(context)) {
        case GELBOORU:
            return serverRoot + "index.php?page=post&s=view&id=" + post_id;
        case DANBOORU:
            return serverRoot + "post/show/" + post_id;
        default:
            return "";
        }
    }

    /**
     * Get the website URL for a pool
     * @param post_id the id of the post
     */
    public static String getWebPoolURL(Context context, int pool_id) {
        String serverRoot = getServerRoot(context);

        switch(getServerType(context)) {
        case GELBOORU:
            return serverRoot + "index.php?page=pool&s=show&id=" + pool_id;
        case DANBOORU:
            return serverRoot + "pool/show/" + pool_id;
        default:
            return "";
        }
    }

    /**
     * Convert a string from danbooru markup style to Html
     * @param text
     * @return text, with all known danbooru tags converted to html
     */
    public static String convertDanbooruToHtml(String text) {
        return text.trim()
                .replace("\n\n", "\n")
                .replace("<b>", "[b]").replace("</b>", "[/b]")
                .replace("<i>", "[i]").replace("</i>", "[/i]")
                .replace("<s>", "[s]").replace("</s>", "[/s]")
                .replace("<", "&lt;").replace(">", "&gt;")
                .replace("\n", "<br />")
                .replace("[i]", "<i>").replace("[/i]", "</i>")
                .replace("[b]", "<b>").replace("[/b]", "</b>")
                .replace("[s]", "<s>").replace("[/s]", "</s>");
    }
}
