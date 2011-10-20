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

import com.duy.minoriko2.R;
import com.duy.minoriko2.control.Helper;
import com.duy.minoriko2.model.Favorite;
import com.duy.minoriko2.model.Favorites;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class FavoriteActivity extends Activity {
    Favorites favDB;
    ListView favList;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setTitle("Minoriko - Favorites");
        favDB = new Favorites(this);
        favList = new ListView(this);
        favList.setAdapter(new FavoriteAdapter(this));
        setContentView(favList);
        registerForContextMenu(favList);
        favList.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> a, View v, int i, long id) {
                itemClicked(i);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        ((FavoriteAdapter) favList.getAdapter()).setFavs(favDB.getAll());
    }

    protected void itemClicked(int i) {
        Favorite f = (Favorite) favList.getAdapter().getItem(i);
        Helper.setServerRoot(this, f.server, f.serverType);

        Intent intent;
        switch (f.type){
        case POOL_SEARCH:
            intent = new Intent(this, PoolListActivity.class);
            if ("".equals(f.query)) {
                intent.setAction(Intent.ACTION_DEFAULT);
            } else {
                intent.setAction(Intent.ACTION_SEARCH);
                intent.putExtra(SearchManager.QUERY, f.query);
            }
            break;
        case POST_SEARCH:
            intent = new Intent(this, PostListActivity.class);
            if ("".equals(f.query)) {
                intent.setAction(Intent.ACTION_DEFAULT);
            } else {
                intent.setAction(Intent.ACTION_SEARCH);
                intent.putExtra(SearchManager.QUERY, f.query);
            }
            break;
        case TAG_SEARCH:
            intent = new Intent(this, TagListActivity.class);
            intent.setAction(Intent.ACTION_SEARCH);
            intent.putExtra(SearchManager.QUERY, f.query);
            break;
        case POOL_VIEW:
            intent = new Intent(this, PoolViewActivity.class);
            intent.putExtra("poolID", f.query);
            break;
        default: // never happens
            intent = new Intent();
        }
        startActivity(intent);
    }

    @Override
    public void onCreateContextMenu (ContextMenu menu, View v,
            ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.favcontext, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo)
                item.getMenuInfo();
        switch (item.getItemId()) {
          case R.id.favcontext_del:
              favDB.delete(info.id);
              ((FavoriteAdapter) favList.getAdapter()).setFavs(favDB.getAll());
              return true;

          default:
              return super.onContextItemSelected(item);
          }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        favDB.close();
        favDB = null;
        favList = null;
    }

    public class FavoriteAdapter extends BaseAdapter {
        Favorite[] fs = null;
        Context context;

        public FavoriteAdapter(Context context) {
            this.context = context;
        }

        public void setFavs(Favorite[] fs) {
            this.fs = fs;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            if (fs == null)
                return 0;
            else
                return fs.length;
        }

        @Override
        public Object getItem(int i) {
            if (fs == null || i < 0 || i >= fs.length)
                return null;
            else
                return fs[i];
        }

        @Override
        public long getItemId(int i) {
            if (fs == null || i < 0 || i >= fs.length)
                return -1;
            else
                return fs[i].id;
        }

        @Override
        public View getView(int i, View v, ViewGroup parent) {
            View row = v;
            if (row == null) {
                LayoutInflater inflater = LayoutInflater.from(context);
                row = inflater.inflate(R.layout.favlistrow, parent, false);
            }

            ImageView img = (ImageView)
                    row.findViewById(R.id.favlistrow_img);
            TextView topText = (TextView)
                    row.findViewById(R.id.favlistrow_toptext);
            TextView bottomText = (TextView)
                    row.findViewById(R.id.favlistrow_bottomtext);

            switch (fs[i].type) {
            case POOL_SEARCH:
                img.setImageResource(R.drawable.ic_menu_poolsearch);
                break;
            case POST_SEARCH:
                img.setImageResource(R.drawable.ic_menu_posts);
                break;
            case TAG_SEARCH:
                img.setImageResource(R.drawable.ic_menu_tags);
                break;
            case POOL_VIEW:
                img.setImageResource(R.drawable.ic_menu_pools);
                break;
            }

            topText.setText(fs[i].display);
            bottomText.setText(fs[i].server);

            return row;
        }

    }
}
