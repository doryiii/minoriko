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
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.duy.minoriko2.R;
import com.duy.minoriko2.control.Helper;
import com.duy.minoriko2.control.XMLDownloader;
import com.duy.minoriko2.model.Favorite;
import com.duy.minoriko2.model.Favorites;
import com.duy.minoriko2.model.Pool;
import com.duy.minoriko2.model.Post;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

public class PoolViewActivity extends ImgGridActivity {
    private String urlPrefix;
    private Pool pool = null;
    private int poolID;
    private String poolName;
    private boolean loadingAll = false;
    private ProgressDialog dialog = null;
    private XMLDownloader pageLoader = null;
    private boolean destroying;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        urlPrefix = Helper.getServerRoot(this) + "/pool/show.xml?";
        init(getIntent());
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        init(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (pageLoader != null) {
            pageLoader.cancel(true);
            pageLoader = null;
        }
        pool = null;
        posts = null;
        urlPrefix = null;
        poolName = null;
        if (dialog != null) {
            dialog.dismiss();
            dialog = null;
        }
        destroying = true;
    }

    private void init(Intent intent) {
        poolID = Integer.parseInt(intent.getStringExtra("poolID"));
        setTitle("Loading pool " + poolID);
        page = 0;
        loadingAll = false;
        dialog = null;
        if (pageLoader != null)
            pageLoader.cancel(true);
        pageLoader = null;
        pool = null;
        launchDownload(1);
        ((MinorikoApplication) getApplication()).putObject(getListKey(), posts);
        destroying = false;
    }

    @Override
    public void openPost(int i) {
        Post current_post = (Post) grid.getItemAtPosition(i);
        String url = current_post.sample_url;

        if (Helper.isSupported(url)) {
            Intent intent = new Intent(this, ImgViewActivity.class);
            intent.setAction(Intent.ACTION_DEFAULT);
            intent.putExtra("listKey", getListKey());
            intent.putExtra("index", i);
            intent.putExtra("page", page);
            intent.putExtra("everything",
                    ((GridAdapter) grid.getAdapter()).everything);
            intent.putExtra("mode", "pool");
            intent.putExtra("pool_id", pool.id);
            startActivityForResult(intent, Helper.IMGVIEW_REQCODE);
        } else {
            Helper.launchBrowser(this, url);
        }
    }

    private void launchDownload(int toPage) {
        if (destroying)
            return;
        //Log.v(Helper.TAG, "PoolViewPageStarting " + toPage);
        getProgressBar().setMax(1);
        getProgressBar().setProgress(0);
        getProgressBar().setVisibility(View.VISIBLE);
        pageLoader = new XMLDownloader(getGridAdapter(), getProgressBar(),
                urlPrefix + "id=" + poolID + "&page=" + toPage);
        pageLoader.execute();
    }

    @Override
    public String getListKey() {
        return urlPrefix + "id=" + poolID + "&page=";
    }

    @Override
    public void retryDownload() {
        pageLoader = null;
        launchDownload(page + 1);
    }

    @Override
    public void stopTrying() {
        pageLoader = null;
    }

    @Override
    public void onLastItemViewed() {
        launchDownload(page + 1);
    }

    @Override
    public NodeList getXMLArray(Document doc) throws NullPointerException {
        return ((Element) doc.getDocumentElement()
                .getElementsByTagName("posts").item(0))
                .getElementsByTagName("post");
    }

    @Override
    public void onNewPageLoaded(Document doc) {
        if (destroying)
            return;

        pageLoader = null;

        if (pool == null) {
            try {
                pool = new Pool(doc.getDocumentElement());
                poolName = pool.name;
                setTitle(poolName);
                if (dialog != null) {
                    dialog.setMax(pool.post_count);
                    dialog.setProgress(posts.size());
                    // dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                }
            } catch (NumberFormatException e) {
                // do nothing; load on next try
            } catch (NullPointerException e) {
            }
        }

        page++;

        if (loadingAll) {
            if (((GridAdapter) grid.getAdapter()).everything) {
                if (dialog != null) {
                    dialog.dismiss();
                    dialog = null;
                }
                grid.smoothScrollToPosition(grid.getCount() - 1);
                //
            } else {
                if (dialog != null) {
                    dialog.setProgress(posts.size());
                }
                getGridAdapter().waiting = true;
                onLastItemViewed();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.poolview, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.poolview_menu_fav:
            if (pool != null) {
                Favorites favDB = new Favorites(this);
                Favorite f = new Favorite(Favorite.Type.POOL_VIEW,
                        Helper.getServerRoot(this), String.valueOf(poolID),
                        poolName, Helper.getServerType(this));
                if (favDB.add(f)) {
                    Toast.makeText(this, "Added to favorite",
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Favorite already exist",
                            Toast.LENGTH_SHORT).show();
                }
                favDB.close();
            } else {
                Toast.makeText(this, "Please wait until pool is loaded",
                        Toast.LENGTH_SHORT).show();
            }
            return true;

        case R.id.poolview_menu_refresh:
            MinorikoApplication.domCache.clearCache();
            onNewIntent(getIntent());
            return true;

        case R.id.poolview_menu_all:
            if (!loadingAll) {
                loadingAll = true;
                dialog = new ProgressDialog(this);
                // if (pool == null) {
                // dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                // } else {
                dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                dialog.setMax(pool == null ? 1 : pool.post_count);
                // }
                dialog.setTitle("Minoriko");
                dialog.setMessage("Loading all posts");
                dialog.setCancelable(true);
                dialog.setOnCancelListener(new OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface arg0) {
                        loadingAll = false;
                        if (pageLoader != null) {
                            pageLoader.cancel(true);
                            getGridAdapter().waiting = false;
                        }
                        dialog = null;
                    }
                });
                dialog.show();
                dialog.setProgress(posts.size());
                //
                // // can be better than a spinloop waiting for the pool to be
                // ready
                if (!getGridAdapter().waiting) {
                    getGridAdapter().waiting = true;
                    onLastItemViewed();
                } else {
                    // do nothing; when the download returned it will
                    // automatically continue, since loadingAll = true;
                }
            }
            return true;

        case R.id.poolview_menu_openpage:
            if (destroying || pool == null)
                return true;

            Helper.launchBrowser(this, Helper.getWebPoolURL(this, pool.id));
            return true;

        default:
            return super.onOptionsItemSelected(item);
        }
    }

}
