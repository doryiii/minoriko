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

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;


import com.duy.minoriko2.R;
import com.duy.minoriko2.control.Helper;
import com.duy.minoriko2.control.XMLCallback;
import com.duy.minoriko2.model.PostList;
import com.duy.minoriko2.widgets.ImageGrid;
import com.duy.minoriko2.widgets.TimedImageView;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;

public abstract class ImgGridActivity extends Activity {
    ImageGrid grid;
    PostList posts;
    ProgressBar progressBar;
    int page;
    MinorikoApplication app;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.imggrid);
        grid = (ImageGrid) findViewById(R.id.imggrid_imgGrid);
        progressBar = (ProgressBar) findViewById(R.id.imggrid_progressBar);
        app = (MinorikoApplication) getApplication();

        posts = new PostList(getApplicationContext());

        grid.setAdapter(new GridAdapter(this));
        grid.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> a, View v, int i, long id) {
                openPost(i);
            }
        });

    }

    @Override
    public void onNewIntent(Intent intent) {
        posts = new PostList(getApplicationContext());
        ((GridAdapter) grid.getAdapter()).reset();
    }

    public GridAdapter getGridAdapter() {
        return (GridAdapter) grid.getAdapter();
    }
    public ProgressBar getProgressBar() {
        return progressBar;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        posts = null;
        progressBar = null;
        grid = null;
    }

    /**
     * Called when an item in the image grid is clicked on
     * @param i the index of the clicked post
     */
    public abstract void openPost(int i);
    /**
     * Called when the ImgViewActivity returned. Scroll the grid to the
     * last viewed post
     */
    @Override
    public void onActivityResult(int reqCode, int resCode, Intent intent) {
        if (resCode == RESULT_OK && reqCode == Helper.IMGVIEW_REQCODE) {
            String listKey = intent.getStringExtra("listKey");
            if (getListKey().equals(listKey)) {
                int wasIndex = intent.getIntExtra("index", posts.size() - 1);
                grid.setSelection(wasIndex);
                page = intent.getIntExtra("page", page);
            }
        }
    }

    /**
     * Get the key to the posts list, which should uniquely identifies
     * a Post[] list.
     * @return the string that the post list in the activity holds
     */
    public abstract String getListKey();

    /**
     * Should retry the download with current parameters.
     */
    public abstract void retryDownload();

    public abstract void stopTrying();

    /**
     * Activates when the last item is viewed. Generally to get more posts.
     */
    public abstract void onLastItemViewed();

    /**
     * Adds the newly downloaded posts list into current one, taking care of
     * duplicates
     * @param doc The Document DOM
     * @return true if there is more, false if at last post.
     */
    public abstract NodeList getXMLArray(Document doc)
            throws NullPointerException;

    /**
     * A callback when the page is completed loading, so that the client
     * can refresh, do processing, load more pages, etc.
     */
    public abstract void onNewPageLoaded(Document doc);

    /** This class implements the Adapter for the image grid */
    class GridAdapter extends BaseAdapter implements XMLCallback {
        private final ImgGridActivity context;
        public boolean everything;
        private boolean retried;
        // this state var ensures there can be only 1 download fired at a time
        public boolean waiting;

        public GridAdapter(ImgGridActivity context) {
            super();
            this.context = context;
            reset();
        }

        public void reset() {
            everything = false;
            retried = false;
            waiting = true; // no firing download until first download arrives
        }

        @Override
        public void onFinished(Document doc) {
            try {
                //Log.v(Helper.TAG, "onFinished" + doc.whatever);
                if (!waiting) //uh-oh, there's a bug
                    throw new RuntimeException("Finished but not waiting");
                waiting = false;
                everything = !posts.mergeListWithXml(getXMLArray(doc));
                notifyDataSetChanged();
                retried = false;
                onNewPageLoaded(doc);
            } catch (NullPointerException e) {
                onError("Bad return string from server");
            } catch (NumberFormatException e) {
                onError("Bad return string from server");
            } catch (RuntimeException e) {}; // OMG
        }

        @Override
        public void onError(String errMsg) {
            if (!retried) {
                waiting = true;
                retryDownload();
                retried = true;
            } else {
                everything = true;
                stopTrying();
            }
        }

        @Override
        public void onCancelled() {
            //waiting = false;
            retried = false;
        }

        @Override
        public int getCount() {
            if (posts == null)
                return 0;
            return posts.size();
        }

        @Override
        public Object getItem(int position) {
            if (posts == null || position >= getCount())
                return null;
            return posts.get(position);
        }

        @Override
        public long getItemId(int position) {
            if (posts == null || position >= getCount())
                return -1;
            return posts.getId(position);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TimedImageView cell = (TimedImageView) convertView;

            if (cell == null) {
                cell = new TimedImageView(this.context);
                cell.setLayoutParams(new GridView.LayoutParams(
                        LayoutParams.MATCH_PARENT, 130));
                cell.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            //	cell.setScaleType(ImageView.ScaleType.CENTER_CROP);
            }

            if (posts == null || position >= getCount()) {
                return cell;
            }

            ((MinorikoApplication) context.getApplication())
                    .previewDownloader.download(posts.get(position)
                            .preview_url, cell);

            if (position == (getCount() - 1)) {
                if (!waiting && !everything) {
                    waiting = true;
                    onLastItemViewed();
                }
            }

            return cell;
        }
    }
}
