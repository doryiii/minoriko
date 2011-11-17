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

package com.duy.minoriko2;

import java.util.ArrayList;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.duy.minoriko2.R;
import com.duy.minoriko2.control.BitmapDownloaderTask;
import com.duy.minoriko2.control.Helper;
import com.duy.minoriko2.control.ImgFetcher;
import com.duy.minoriko2.control.XMLCallback;
import com.duy.minoriko2.control.XMLDownloader;
import com.duy.minoriko2.model.Comment;
import com.duy.minoriko2.model.Note;
import com.duy.minoriko2.model.PostList;
import com.duy.minoriko2.widgets.DanboImageView;
import com.duy.minoriko2.widgets.TimedImageView;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

public class ImgViewActivity extends Activity implements
        DanboImageView.PrevNextHandler {
    // The main controls on the form
    DanboImageView imView0, imView1;
    TimedImageView tmpImv0, tmpImv1;
    ProgressBar imgProgressBar, downloadProgressBar;
    ViewSwitcher switcher;
    AlertDialog tagsDialog = null;
    AlertDialog commentsDialog = null;

    private int index, page;
    private String mode, url, post_query;
    //private int pool_id;
    boolean everything;
    private PostList posts;
    private ArrayList<Note> notes = null;
    private ArrayList<Comment> comments = null;
    private XMLDownloader notesDownloader = null;
    private XMLDownloader pageDownloader = null;
    private XMLDownloader commentDownloader = null;
    private boolean pageWaiting;
    private ImgViewActivity imgViewActivity = this;
    private CountDownTimer timer = null;

    private Animation slide_out_right;
    private Animation slide_in_left;
    private Animation slide_out_left;
    private Animation slide_in_right;
    private ImgFetcher imgFetcher = null;

    private boolean destroying;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        notes = new ArrayList<Note>();
        comments = new ArrayList<Comment>();

        setContentView(R.layout.imgview);
        imView0 = (DanboImageView) findViewById(R.id.imgview_imView0);
        imView0.setPrevNextHandler(this);
        imView1 = (DanboImageView) findViewById(R.id.imgview_imView1);
        imView1.setPrevNextHandler(this);

        switcher = (ViewSwitcher) findViewById(R.id.imgview_switcher);

        imgProgressBar = (ProgressBar) findViewById(
                R.id.imgview_imgProgressBar);
        downloadProgressBar = (ProgressBar) findViewById(
                R.id.imgview_downloadProgressBar);
        imgProgressBar.setVisibility(View.GONE);
        downloadProgressBar.setVisibility(View.GONE);

        slide_in_right = AnimationUtils.loadAnimation(
                this, R.anim.slide_in_right);
        slide_out_left = AnimationUtils.loadAnimation(
                this, R.anim.slide_out_left);
        slide_in_left = AnimationUtils.loadAnimation(
                this, R.anim.slide_in_left);
        slide_out_right = AnimationUtils.loadAnimation(
                this, R.anim.slide_out_right);

        Bundle extras = getIntent().getExtras();
        index = extras.getInt("index");
        url = extras.getString("listKey");
        page = extras.getInt("page");
        everything = extras.getBoolean("everything");
        mode = extras.getString("mode");
        if ("post".equals(mode)) {
            post_query = extras.getString("post_query");
        } else if ("pool".equals(mode)) {
            //pool_id = extras.getInt("pool_id");
        }

        posts = (PostList) ((MinorikoApplication)getApplication())
                .getObject(url);
        if (posts == null) { // if posts is already garbage-collected
            goBack();
            return;
        }

        tmpImv0 = new TimedImageView(this);
        tmpImv1 = new TimedImageView(this);

        if (index > 0) {
            ((MinorikoApplication) getApplication()).previewDownloader
                    .download(posts.get(index-1).preview_url, tmpImv0);
        }
        if (index < posts.size() - 1) {
            ((MinorikoApplication) getApplication()).previewDownloader
                    .download(posts.get(index+1).preview_url, tmpImv1);
        }

        destroying = false;
    }

    @Override
    public void onResume() {
        super.onResume();
        DanboImageView imView = (DanboImageView) switcher.getCurrentView();
        imView.setPost(posts.get(index));
        launchImgDownloader(imView);

        if (notes == null)
            notes = new ArrayList<Note>();
        if (notes.size() == 0)
            launchNotesDownloader(imView);

        if (comments == null)
            comments = new ArrayList<Comment>();
        if (comments.size() == 0)
            launchCommentDownloader();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        destroying = true;

        if (imView0 != null)
            imView0.setNotes(null);
        if (imView1 != null)
            imView1.setNotes(null);

        imView0 = null;
        imView1 = null;
        tmpImv0 = null;
        tmpImv1 = null;
        imgProgressBar = null;
        downloadProgressBar = null;
        switcher = null;
        if (tagsDialog != null) {
            tagsDialog.dismiss();
            tagsDialog = null;
        }

        commentsDialog = null;

        url = null;
        posts = null;
        notes = null;
        comments = null;
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        if (notesDownloader != null) {
            notesDownloader.cancel(true);
            notesDownloader = null;
        }
        if (pageDownloader != null) {
            pageDownloader.cancel(true);
            pageDownloader = null;
        }
        if (commentDownloader != null) {
            commentDownloader.cancel(true);
            commentDownloader = null;
        }
        if (imgFetcher != null) {
            imgFetcher.cancel(true);
            imgFetcher = null;
            Toast.makeText(this, "Image download cancelled",
                    Toast.LENGTH_SHORT);
        }

        imgViewActivity = null;

        slide_out_right = null;
        slide_in_left = null;
        slide_out_left = null;
        slide_in_right = null;

    }

    public void launchImgDownloader(DanboImageView imView) {
        imgProgressBar.setVisibility(View.GONE);
        ((MinorikoApplication) getApplication()).previewDownloader.download(
                posts.get(index).preview_url, imView.previewImgView);

        if (Helper.isSupported(posts.get(index).sample_url)) {
            ((MinorikoApplication) getApplication()).imgDownloader.download(
                    posts.get(index).sample_url,
                    imView, imgProgressBar);
        } else { // load the preview_url for those unsupported, such as flash
            ((MinorikoApplication) getApplication()).imgDownloader.download(
                    posts.get(index).preview_url,
                    imView, imgProgressBar);
        }
    }

    private void launchNotesDownloader(DanboImageView imView) {
        if (notesDownloader != null) {
            notesDownloader.cancel(true);
            notesDownloader = null;
        }
        if (timer != null)
            timer.cancel();
        notes.clear();

        if (posts.get(index).has_notes) {
            imView.notesDone = false;
            timer = new CountDownTimer(600, 601) {
                @Override
                public void onFinish() {
                    if (destroying)
                        return;

                    notesDownloader = new XMLDownloader(notesCallback, null,
                            Helper.getNotesUrl(ImgViewActivity.this,
                                    posts.getId(index)), true);
                    notesDownloader.execute();
                    timer = null;
                }
                @Override
                public void onTick(long millisUntilFinished) {}
            };
            timer.start();
        } else {
            imView.setNotes(notes);
            imView.notesDone = true;
        }
    }

    private void launchCommentDownloader() {
        if (commentDownloader != null) {
            commentDownloader.cancel(true);
        }
        comments.clear();

        if (posts.get(index).has_comments) {
            commentDownloader = new XMLDownloader(commentCallback, null,
                    Helper.getCommentsUrl(this, posts.getId(index)), true);
            commentDownloader.execute();
        } else {
            commentDownloader = null;
        }
    }

    private void goBack() {
        Intent intent = new Intent();
        intent.putExtra("index", index);
        intent.putExtra("page", page);
        intent.putExtra("listKey", url);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        goBack();
    }

    @Override
    public void prev() {
        if (index == 0) {
            goBack();
            return;
        }

        DanboImageView currView = (DanboImageView) switcher.getCurrentView();
        final DanboImageView nextView = (currView == imView0) ? imView1 : imView0;
        BitmapDownloaderTask.cancelDownload(currView);

        currView.setNotes(null);
        //currView.notesDone = false;
        switcher.setOutAnimation(slide_out_right);
        switcher.setInAnimation(slide_in_left);

        index--;
        nextView.setPost(posts.get(index));
        nextView.setNotes(null);
        launchImgDownloader(nextView);
        launchCommentDownloader();
        launchNotesDownloader(nextView);

        switcher.showNext();

        if (index > 0) {
            ((MinorikoApplication) getApplication()).previewDownloader
                    .download(posts.get(index-1).preview_url, tmpImv0);
        }
        if (index > 1) {
            ((MinorikoApplication) getApplication()).previewDownloader
                    .download(posts.get(index-2).preview_url, tmpImv1);
        }
    }

    @Override
    public void next() {
        if (index >= posts.size() - 1) {
            //goBack();
            return;
        }

        DanboImageView currView = (DanboImageView) switcher.getCurrentView();
        final DanboImageView nextView = (currView == imView0) ? imView1 : imView0;
        BitmapDownloaderTask.cancelDownload(currView);

        currView.setNotes(null);
        //currView.notesDone = false;
        switcher.setOutAnimation(slide_out_left);
        switcher.setInAnimation(slide_in_right);

        index++;
        nextView.setPost(posts.get(index));
        nextView.setNotes(null);
        launchImgDownloader(nextView);
        launchNotesDownloader(nextView);
        launchCommentDownloader();

        switcher.showNext();

        // if we're loading last post(s), load the next page
        if (index >= posts.size() - 2 && !pageWaiting && !everything) {
            if (!everything) {
                pageWaiting = true;
                if ("post".equals(mode)) {
                    pageDownloader = new XMLDownloader(nextPageCallback, null,
                            Helper.getPostListUrl(this, post_query, page+1));
                } else if ("pool".equals(mode)) {
                    pageDownloader = new XMLDownloader(nextPageCallback, null,
                            url + (page+1));
                    //TODO: omg refactor this when gelbooru pool support is out
                }
                pageDownloader.execute();
            }
        }

        // if we're not loading last post, load next image into cache
        if (index < posts.size() - 1) {
            ((MinorikoApplication) getApplication()).previewDownloader
                    .download(posts.get(index+1).preview_url, tmpImv0);
        }
        if (index < posts.size() - 2) {
            ((MinorikoApplication) getApplication()).previewDownloader
                    .download(posts.get(index+2).preview_url, tmpImv1);
        }
    }

    @Override
    public void click() {
        String url = posts.get(index).sample_url;
        if (!Helper.isSupported(url)) {
            Helper.launchBrowser(this, url);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.imgview, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.imgmenu_save:
            if (imgFetcher != null) // only 1 download at a time
                return true;

            downloadProgressBar.setVisibility(View.VISIBLE);
            imgFetcher = new ImgFetcher(this, downloadProgressBar,
                    new ImgFetcher.DownloadDoneCallback() {
                        @Override
                        public void done() {
                            imgFetcher = null;
                        }
            });
            imgFetcher.execute(posts.get(index).file_url);
            return true;

        case R.id.imgmenu_tags:
            ListView tagsList = new ListView(this);
            tagsList.setAdapter(new TagsPopupAdapter(this));
            tagsList.setOnItemClickListener(new ListView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> a, View v,
                        int i, long l) {
                    launchPostSearch((String) a.getItemAtPosition(i));
                }
            });

            if (tagsDialog != null) {
                tagsDialog.dismiss();
            }
            tagsDialog = (new AlertDialog.Builder(this)).create();
            tagsDialog.setOnCancelListener(new OnCancelListener() {
                @Override
                public void onCancel(DialogInterface arg0) {
                    tagsDialog = null;
                }
            });
            tagsDialog.setView(tagsList);
            tagsDialog.setTitle("Tags");
            tagsDialog.show();

            return true;

        case R.id.imgmenu_comments:
            if (commentDownloader != null) {
                Toast.makeText(this, "Comments are still being downloaded",
                        Toast.LENGTH_SHORT).show();
                return true;
            }
            if (comments.size() == 0) {
                Toast.makeText(this, "There is no comment for this post",
                        Toast.LENGTH_SHORT).show();
                return true;
            }
            ListView commentList = new ListView(this);
            commentList.setAdapter(new CommentsAdapter(this));
            commentList.setOnItemClickListener(new ListView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> arg0, View arg1,
                        int arg2, long arg3) {
                    commentsDialog.dismiss();
                }
            });

            if (commentsDialog != null) {
                commentsDialog.dismiss();
                commentsDialog = null;
            }
            commentsDialog = (new AlertDialog.Builder(this)).create();
            commentsDialog.setView(commentList);
            commentsDialog.setTitle("Comments");
            commentsDialog.show();

            return true;

        case R.id.imgmenu_openpage:
            if (destroying || posts == null || index > posts.size())
                return true;

            Helper.launchBrowser(this, Helper.getWebPostURL(
                    this, posts.get(index).id));
            return true;

        default:
            return super.onOptionsItemSelected(item);
        }
    }

    protected void launchPostSearch(String tag) {
//    	if (tagsDialog != null) {
//    		tagsDialog.dismiss();
//    		tagsDialog = null;
//    	}

        Intent intent = new Intent(this, PostListActivity.class);
        intent.setAction(Intent.ACTION_SEARCH);
        intent.putExtra(SearchManager.QUERY, tag);
        startActivity(intent);
    }


    private XMLCallback notesCallback = new XMLCallback() {
        @Override
        public void onFinished(Document doc) {
            // theoretically should never happen, but it does sometimes...
            if (notes == null) return;

            notes.clear();

            if (doc == null) {
                Toast.makeText(imgViewActivity, "Error downloading notes",
                        Toast.LENGTH_SHORT).show();
                ((DanboImageView) switcher.getCurrentView()).setNotes(null);
                return;
            }

            try {
                NodeList nodes = doc.getElementsByTagName("note");
                int nodesLen = nodes.getLength();
                for (int i=0; i<nodesLen; i++) {
                    Element element = (Element) nodes.item(i);
                    if ("false".equalsIgnoreCase(
                            element.getAttribute("is_active"))) {
                        continue;
                    }

                    notes.add(new Note(imgViewActivity, element));
                }
                ((DanboImageView) switcher.getCurrentView()).setNotes(notes);
            } catch (NumberFormatException e) {
                Toast.makeText(imgViewActivity, R.string.danbo_api_change_note,
                        Toast.LENGTH_LONG).show();
            } catch (NullPointerException e) {
                Toast.makeText(imgViewActivity, R.string.danbo_api_change_note,
                        Toast.LENGTH_LONG).show();
            }
            notesDownloader = null;
        }

        @Override
        public void onError(String errMsg) {
            Toast.makeText(imgViewActivity, R.string.danbo_api_change_note,
                    Toast.LENGTH_LONG).show();
            ((DanboImageView) switcher.getCurrentView()).setNotes(null);
            notes.clear();
            notesDownloader = null;
        }

        @Override
        public void onCancelled() {
//        	if (switcher != null)
//        		((DanboImageView) switcher.getCurrentView()).setNotes(null);
//        	if (notes != null)
//                notes.clear();
//            notesDownloader = null;
        }
    };

    private XMLCallback nextPageCallback = new XMLCallback() {
        @Override
        public void onFinished(Document doc) {
            if (!pageWaiting)
                throw new RuntimeException("returned while not waiting");

            try {
                NodeList nodes = null;
                if ("post".equals(mode)) {
                    nodes = doc.getElementsByTagName("post");
                } else if ("pool".equals(mode)) {
                    nodes = ((Element) doc.getElementsByTagName("posts")
                            .item(0)).getElementsByTagName("post");
                } else {
                    return;
                }

                everything = !posts.mergeListWithXml(nodes);
                page++;

            } catch (NullPointerException e) {
                onError("Bad return string from server");
            } catch (NumberFormatException e) {
                onError("Bad return string from server");
            }
            pageWaiting = false;
            pageDownloader = null;
        }

        @Override
        public void onError(String errMsg) {
            pageWaiting = false;
            pageDownloader = null;
        }

        @Override
        public void onCancelled() {
//            pageWaiting = false;
//            pageDownloader = null;
        }
    };

    private XMLCallback commentCallback = new XMLCallback() {
        @Override
        public void onFinished(Document doc) {
            // theoretically should never happen, but it does sometimes...
            if (comments == null) return;

            comments.clear();
            try {
                NodeList nodes = doc.getDocumentElement()
                        .getElementsByTagName("comment");

                if (Helper.isCommentsTimeSortBackward(ImgViewActivity.this)) {
                    for (int i=nodes.getLength()-1; i>=0; i--) {
                        Element e = (Element) nodes.item(i);
                        comments.add(new Comment(ImgViewActivity.this, e));
                    }
                } else {
                    for (int i=0; i<nodes.getLength(); i++) {
                        Element e = (Element) nodes.item(i);
                        comments.add(new Comment(ImgViewActivity.this, e));
                    }
                }

            } catch (NullPointerException e) {
                onError("Bad return string from server");
            } catch (NumberFormatException e) {
                onError("Bad return string from server");
            }
            commentDownloader = null;
        }

        @Override
        public void onError(String errMsg) {
            commentDownloader = null;
            if (comments != null)
                comments.clear();
        }

        @Override
        public void onCancelled() {
//            commentDownloader = null;
//            if (comments != null)
//            	comments.clear();
        }

    };

    private class TagsPopupAdapter extends BaseAdapter {
        private final Context context;

        public TagsPopupAdapter(Context context) {
            this.context = context;
        }

        @Override
        public int getCount() {
            if (destroying || posts == null ||
            	posts.get(index) == null || posts.get(index).tags == null)
                return 0;
            return posts.get(index).tags.length;
        }

        @Override
        public Object getItem(int position) {
            if (destroying || posts == null ||
                posts.get(index) == null || posts.get(index).tags == null)
                return null;
            return posts.get(index).tags[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            View row = (View) view;
            if (row == null) {
                LayoutInflater inflater = LayoutInflater.from(context);
                row = inflater.inflate(R.layout.tagpopuprow, parent, false);
            }
            TextView tv = (TextView) row.findViewById(R.id.tagpopuprow_text);

            tv.setText(posts.get(index).tags[position]);

            return row;
        }
    }

    private class CommentsAdapter extends BaseAdapter {
        private final Context context;

        public CommentsAdapter(Context context) {
            this.context = context;
        }

        @Override
        public int getCount() {
            if (destroying || comments == null)
            	return 0;
            return comments.size();
        }

        @Override
        public Object getItem(int i) {
            if (destroying || comments == null)
            	return null;
            return comments.get(i);
        }

        @Override
        public long getItemId(int i) {
            if (destroying || comments == null)
            	return 0;
            return comments.get(i).id;
        }

        @Override
        public boolean isEnabled(int position) {
            return false;
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public View getView(int i, View view, ViewGroup parent) {
            View row = view;
            if (row == null) {
                LayoutInflater inflater = LayoutInflater.from(context);
                row = inflater.inflate(R.layout.commentpopuprow, parent, false);
            }

            TextView creatorText = (TextView) row.findViewById(
                    R.id.commentpopuprow_creator);
            TextView timeText = (TextView) row.findViewById(
                    R.id.commentpopuprow_time);
            TextView bodyText = (TextView) row.findViewById(
                    R.id.commentpopuprow_body);

            if (i < getCount()) {
                Comment c = comments.get(i);
                creatorText.setText(c.creator);
                bodyText.setText(c.body);
                timeText.setText(c.time);
            }
            return row;
        }

    }
}
