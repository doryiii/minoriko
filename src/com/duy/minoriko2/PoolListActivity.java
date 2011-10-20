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

import com.duy.minoriko2.R;
import com.duy.minoriko2.control.Helper;
import com.duy.minoriko2.control.XMLCallback;
import com.duy.minoriko2.control.XMLDownloader;
import com.duy.minoriko2.model.Favorite;
import com.duy.minoriko2.model.Favorites;
import com.duy.minoriko2.model.Pool;
import com.duy.minoriko2.model.PoolList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class PoolListActivity extends Activity {
    ListView list;
    PoolList pools;
    ProgressBar progressBar;
    //String urlPrefix;
    String query;
    XMLDownloader poolListDownloader;
    int page;
    private boolean destroying;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.poollist);
        list = (ListView) findViewById(R.id.poollist_list);
        progressBar = (ProgressBar) findViewById(R.id.poollist_progressBar);

        pools = new PoolList();

        Intent intent = getIntent();
        list.setAdapter(new PoolListAdapter());
        list.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> a, View v,
            		int position, long id) {
                openPool(position);
            }
        });
        list.setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> a, View v,
					int position, long id) {
				if (destroying || position > pools.size())
					return true;

				View dialogView = View.inflate(PoolListActivity.this,
						R.layout.pool_desc_popup, null);
				TextView tv = (TextView) dialogView
						.findViewById(R.id.pooldescpopup_tv);
				tv.setText(Html.fromHtml(pools.get(position).desc));

				Dialog dialog = (new AlertDialog.Builder(
						PoolListActivity.this))
	                .setView(dialogView)
                    .setTitle(pools.get(position).name)
                    .create();
	            dialog.show();
				return true;
			}
        });

        init(intent);
    }

    @Override
    public void onNewIntent(Intent intent) {
        pools = new PoolList();
        setIntent(intent);
        init(intent);
    }

    public void init(Intent intent) {
        String intentAction = intent.getAction();
        //urlPrefix = Helper.getServerRoot(this) + "/pool/index.xml?";

        if (poolListDownloader != null)
            poolListDownloader.cancel(true);
        poolListDownloader = null;
        getPoolListAdapter().reset();

        if (intentAction.equals(Intent.ACTION_DEFAULT)) {
            query = "";
            setTitle("Pools - " + Helper.getServerRoot(this));
            launchSearch();
        } else if (intentAction.equals(Intent.ACTION_SEARCH)) {
            query = intent.getStringExtra(SearchManager.QUERY);
            setTitle(query + " - " + Helper.getServerRoot(this));
            launchSearch();
        }

        destroying = false;
    }

    private PoolListAdapter getPoolListAdapter() {
        return ((PoolListAdapter) list.getAdapter());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        list = null;
        pools = null;
        progressBar = null;
        //urlPrefix = null;
        query = null;
        if (poolListDownloader != null) {
            poolListDownloader.cancel(true);
            poolListDownloader = null;
        }
        destroying = true;
    }

    private void launchSearch() {
    	if (destroying)
    		return;
        this.page = 0;
        getPoolListAdapter().waiting = true;
        launchDownload(1);
    }

    protected void openPool(int i) {
        Intent intent = new Intent(this, PoolViewActivity.class);
        Pool pool = (Pool) list.getItemAtPosition(i);
        intent.putExtra("poolID", String.valueOf(pool.id));
        this.startActivity(intent);
    }

    private void launchDownload(int doPage) {
    	if (destroying)
    		return;
        if (poolListDownloader != null)
            return;

        progressBar.setVisibility(View.VISIBLE);
        progressBar.setProgress(0);
        poolListDownloader = new XMLDownloader(
                getPoolListAdapter(), progressBar,
                Helper.getPoolsUrl(this, query, doPage),
                true);
        poolListDownloader.execute();
    }

    public class PoolListAdapter extends BaseAdapter
            implements XMLCallback {
        boolean everything;
        boolean retried;
        // this state var ensures there can be only 1 download fired at a time
        boolean waiting;

        public void reset() {
            everything = false;
            retried = false;
            waiting = false;
        }

        @Override
        public void onFinished(Document doc) {
        	if (destroying)
        		return;

            try {
                if (!waiting) //uh-oh, there's a bug
                    throw new RuntimeException("Finished but not waiting");
                everything = !pools.mergeListWithXml(
                        doc.getDocumentElement().getElementsByTagName("pool"));
                notifyDataSetChanged();
                retried = false;
                waiting = false;
                page++;
            } catch (NumberFormatException e) {
                onError("Bad return string from server");
            } catch (NullPointerException e) {
                onError("Bad return string from server");
            } catch (RuntimeException e) {}; // OMG
            poolListDownloader = null;
        }

        @Override
        public void onError(String errMsg) {
        	if (destroying)
        		return;

            if (!retried) {
                waiting = true;
                launchDownload(page + 1);
                retried = true;
            } else {
                everything = true;
            }
            poolListDownloader = null;
        }

        @Override
        public void onCancelled() {
            //waiting = false;
            retried = false;
            poolListDownloader = null;
        }

        @Override
        public int getCount() {
            if (pools == null)
                return 0;
            return pools.size();
        }

        @Override
        public Object getItem(int position) {
            return pools.get(position);
        }

        @Override
        public long getItemId(int position) {
            return pools.getId(position);
        }

        @Override
        public View getView(int position, View v, ViewGroup parent) {
            View row = v;
            if (row == null) {
                LayoutInflater inflater = LayoutInflater.from(
                        PoolListActivity.this);
                row = inflater.inflate(R.layout.poollistrow, parent, false);
            }

            TextView topText = (TextView)
                    row.findViewById(R.id.poollistrow_toptext);
            TextView bottomText = (TextView)
                    row.findViewById(R.id.poollistrow_bottomtext);

            topText.setText(pools.get(position).name);
            bottomText.setText(pools.get(position).artist);
            if ("".equals(bottomText.getText()))
                bottomText.setVisibility(View.GONE);
            else
                bottomText.setVisibility(View.VISIBLE);

            if (position == (getCount() - 1)) {
                if (!waiting && !everything) {
                    waiting = true;
                    launchDownload(page + 1);
                }
            }

            return row;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.poollist, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.poollist_menu_search:
            onSearchRequested();
            return true;

        case R.id.poollist_menu_fav:
            String display = query;
            if ("".equals(query)) {
                display = "(Pools)";
            }

            Favorites favDB = new Favorites(this);
            Favorite f = new Favorite (
                    Favorite.Type.POOL_SEARCH,
                    Helper.getServerRoot(this),
                    query, display,
                    Helper.getServerType(this));
            if (favDB.add(f)) {
                Toast.makeText(this, "Added to favorite",
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Favorite already exist",
                        Toast.LENGTH_SHORT).show();
            }
            favDB.close();
            return true;

        case R.id.poollist_menu_refresh:
            MinorikoApplication.domCache.clearCache();
            onNewIntent(getIntent());
            return true;

        default:
            return super.onOptionsItemSelected(item);
        }
    }

}
