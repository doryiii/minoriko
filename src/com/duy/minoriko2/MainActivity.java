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

import java.io.File;

import com.duy.minoriko2.R;
import com.duy.minoriko2.control.Helper;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Process;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.text.format.Time;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class MainActivity extends Activity {

    @Override
    public void finish() {
        if(IntentActionData.isActive())
        {
            if(IntentActionData.isSaved())
            {
                Intent i = new Intent();
                i.putExtra(MediaStore.EXTRA_OUTPUT, IntentActionData.getFiles());
                setResult(RESULT_OK, i);
            }
            else
                setResult(RESULT_CANCELED);
            IntentActionData.clearData();
        }
        super.finish();
    }

    ListView mainList;
    private static final int POOL = 0, POST = 1, TAGS = 2, SERVERS = 3,
            FAVS = 4, SETTING = 5;
    private static final int N_ITEMS = 6;

    // TODO: Use property service for Login, push "&login=" to Acts
    // TODO: search suggestion.
    // TODO: continuous view.
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        setTitle(Helper.getServerRoot(this));

        mainList = (ListView) findViewById(R.id.mainList);
        mainList.setAdapter(new MainAdapter(this));

        mainList.setOnItemClickListener(new ListView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> a, View v, int i, long l) {
                switch (i) {
                case POOL:
                    openPools();
                    break;
                case POST:
                    openPosts();
                    break;
                case TAGS:
                    openTags();
                    break;
                case FAVS:
                    openFav();
                    break;
                case SERVERS:
                    openServerList();
                    break;
                case SETTING:
                    openSettings();
                    break;
                default: // huh?
                    break;
                }
            }
        });

        if (PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean("is_auto_purge", true)) {
            new CleanCache().execute();
        }

        Intent i = getIntent();
        if(i != null && i.getAction().compareTo(Intent.ACTION_PICK) == 0)
            IntentActionData.setData(i.getStringExtra(MediaStore.EXTRA_OUTPUT));
    }

    protected void openPools() {
        Intent intent = new Intent(this,
                PoolListActivity.class);
        intent.setAction(Intent.ACTION_DEFAULT);
        intent.putExtra("page", 1);
        startActivity(intent);
    }

    protected void openPosts() {
        Intent intent = new Intent(this,
                PostListActivity.class);
        intent.setAction(Intent.ACTION_DEFAULT);
        startActivity(intent);
    }

    protected void openTags() {
        Intent intent = new Intent(this,
                TagListActivity.class);
        intent.setAction(Intent.ACTION_DEFAULT);
        intent.putExtra("page", 1);
        startActivity(intent);
    }

    protected void openServerList() {
        Intent intent = new Intent(this,
                ServerListActivity.class);
        intent.setAction(Intent.ACTION_DEFAULT);
        startActivity(intent);
    }

    protected void openFav() {
        Intent intent = new Intent(this,
                FavoriteActivity.class);
        intent.setAction(Intent.ACTION_DEFAULT);
        startActivity(intent);
    }

    protected void openSettings() {
        Intent intent = new Intent(this,
                SettingsActivity.class);
        startActivity(intent);
    }

    @Override
    public boolean onSearchRequested() {
        Intent intent = new Intent(this,
                PostListActivity.class);
        intent.setAction(Intent.ACTION_RUN);
        startActivity(intent);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        setTitle(Helper.getServerRoot(this));
        ((MainAdapter) mainList.getAdapter()).notifyDataSetChanged();
    }

    public class MainAdapter extends BaseAdapter {
        Context context;

        public MainAdapter(Context context) {
            this.context = context;
        }

        @Override
        public View getView(int i, View v, ViewGroup parent) {
            View row = v;
            if (row == null) {
                LayoutInflater inflater = LayoutInflater.from(this.context);
                row = inflater.inflate(R.layout.mainlistrow, parent, false);
            }

            ImageView img = (ImageView)
                    row.findViewById(R.id.mainlistrow_img);
            TextView topText = (TextView)
                    row.findViewById(R.id.mainlistrow_toptext);
            TextView bottomText = (TextView)
                    row.findViewById(R.id.mainlistrow_bottomtext);

            topText.setTextColor(Color.WHITE);
            switch (i) {
            case POOL:
                img.setImageResource(R.drawable.ic_menu_pools);
                topText.setText("Pools");
                if (isEnabled(0)) {
                    bottomText.setText(
                            "Images organized by series or category");
                } else {
                    bottomText.setText("Not supported");
                    topText.setTextColor(Color.GRAY);
                }
                break;
            case POST:
                img.setImageResource(R.drawable.ic_menu_posts);
                topText.setText("Posts");
                bottomText.setText("Images organized by tags");
                break;
            case TAGS:
                img.setImageResource(R.drawable.ic_menu_tags);
                topText.setText("Tags");
                bottomText.setText("List and search for tags");
                break;
            case FAVS:
                img.setImageResource(R.drawable.ic_menu_fav);
                topText.setText("Favorites");
                bottomText.setText("Your saved searches");
                break;
            case SERVERS:
                img.setImageResource(R.drawable.ic_menu_home);
                topText.setText("Server");
                bottomText.setText("Select 'booru server");
                break;
            case SETTING:
                img.setImageResource(R.drawable.ic_menu_preferences);
                topText.setText("Settings");
                bottomText.setText("Set server, filter, clear cache");
                break;
            default:
                break;
            }

            return row;
        }

        @Override
        public boolean isEnabled(int position) {
            if (position == POOL) { // pools
                return Helper.isPoolsWorking(MainActivity.this);
            } else {
                return true;
            }
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public int getCount() {
            return N_ITEMS;
        }

        @Override
        public Object getItem(int i) {
            return null;
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }
    }

    private class CleanCache extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try {
                Process.setThreadPriority(Process.THREAD_PRIORITY_LOWEST);
            } catch (Exception e) { }


            File dir = MainActivity.this.getExternalCacheDir();
            if (dir == null)
                return null;

            if (dir.isDirectory()) {
                File[] children = dir.listFiles();

//                Arrays.sort(children, new Comparator<File>() {
//					@Override
//					public int compare(File f1, File f2) {
//						if (f1.lastModified() > f2.lastModified())
//							return -1;
//						else if (f1.lastModified() < f2.lastModified())
//							return +1;
//						else
//							return 0;
//					}
//                });

                int total = children.length;
                Time now = new Time();
                now.setToNow();

                for (int i=0; i<total; i++) {
                    if (children[i].isFile() && children[i].lastModified()
                            < (now.toMillis(false) - 86400000)) {
                        // delete files older than 1 day
                        children[i].delete();
                    }
                }
            }
            return null;
        }
    }
}
