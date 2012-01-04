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
import com.duy.minoriko2.control.Helper;
import com.duy.minoriko2.control.XMLCallback;
import com.duy.minoriko2.control.XMLDownloader;
import com.duy.minoriko2.model.Tag;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView;

public class TagListActivity extends Activity {
    private ListView list;
    private ProgressBar progressBar;
    private ArrayList<Tag> tags;
    private String query;
    private final TagListActivity itself = this;
    private XMLDownloader tagsDownloader;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.taglist);
        list = (ListView) findViewById(R.id.taglist_list);
        progressBar = (ProgressBar) findViewById(R.id.taglist_progressBar);
        tags = new ArrayList<Tag>(Helper.TAG_PER_PAGE);

        list.setAdapter(new TagListAdapter(this));
        list.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> a, View v, int i, long id) {
                openTag(i);
            }
        });

        Intent intent = getIntent();
        init(intent);
    }

    @Override
    public void onNewIntent(Intent intent) {
        tags.clear();
        init(intent);
    }

    private void init(Intent intent) {
        String intentAction = intent.getAction();

        query = "";
        if (intentAction.equals(Intent.ACTION_DEFAULT)) {
            query = "";
            setTitle("Tags");
            ((TagListAdapter) list.getAdapter()).waiting = true;
            launchDownload();
        } else if (intentAction.equals(Intent.ACTION_SEARCH)) {
            query = intent.getStringExtra(SearchManager.QUERY);
            setTitle(query);
            ((TagListAdapter) list.getAdapter()).waiting = true;
            launchDownload();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        list = null;
        progressBar = null;
        tags = null;
        query = null;
        if (tagsDownloader != null) {
            tagsDownloader.cancel(true);
            tagsDownloader = null;
        }
    }

    protected void openTag(int i) {
        Intent intent = new Intent(this, PostListActivity.class);
        intent.setAction(Intent.ACTION_SEARCH);
        intent.putExtra(SearchManager.QUERY,
                ((Tag) list.getItemAtPosition(i)).name);
        startActivity(intent);
    }

    public void launchDownload() {
        if (tagsDownloader != null)
            return;

        progressBar.setVisibility(View.VISIBLE);
        progressBar.setProgress(0);
        tagsDownloader = new XMLDownloader(
                (TagListAdapter) list.getAdapter(), progressBar,
                Helper.getTagsUrl(this, query));
        tagsDownloader.execute();
    }

    public void setTags(Document doc)
            throws NumberFormatException, NullPointerException {
        NodeList nodes = doc.getDocumentElement().getElementsByTagName("tag");

        if (Helper.isTagsSortDesc(this, query)) {
            for (int i=0; i<nodes.getLength(); i++) {
                tags.add(new Tag((Element) nodes.item(i)));
            }
        } else {
            for (int i=nodes.getLength()-1; i>=0; i--) {
                tags.add(new Tag((Element) nodes.item(i)));
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.taglist, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.tagslist_menu_search:
            onSearchRequested();
            return true;

        default:
            return super.onOptionsItemSelected(item);
        }
    }

    class TagListAdapter extends BaseAdapter implements XMLCallback {
        private final TagListActivity context;
        boolean everything;
        boolean retried;
        // this state var ensures there can be only 1 download fired at a time
        boolean waiting;

        public TagListAdapter(TagListActivity context) {
            super();
            this.context = context;
        }

        public void reset() {
            everything = false;
            retried = false;
            waiting = false;
        }

        @Override
        public void onFinished(Document doc) {
            tagsDownloader = null;

            if (doc == null) { // server failed
                Toast.makeText(itself, "Error downloading tags list",
                        Toast.LENGTH_SHORT);
                onError("Error downloading tags list");
            }

            try {
                setTags(doc);
                notifyDataSetChanged();
                retried = false;
                waiting = false;
                tagsDownloader = null;
            } catch (NumberFormatException e) {
                onError("Bad return string from server");
            } catch (NullPointerException e) {
                onError("Bad return string from server");
            }
        }

        @Override
        public void onError(String errMsg) {
            if (!retried) {
                waiting = true;
                tagsDownloader = null;
                launchDownload();
                retried = true;
            } else {
                tagsDownloader = null;
                everything = true;
            }
        }

        @Override
        public void onCancelled() {
            //waiting = false;
            retried = false;
        }

        @Override
        public int getCount() {
            if (tags == null)
                return 0;
            return tags.size();
        }

        @Override
        public Object getItem(int position) {
            return tags.get(position);
        }

        @Override
        public long getItemId(int position) {
            return tags.get(position).id;
        }

        @Override
        public View getView(int position, View v, ViewGroup parent) {
            View row = v;
            if (row == null) {
                  LayoutInflater inflater = LayoutInflater.from(this.context);
                  row = inflater.inflate(R.layout.taglistrow, parent, false);
            }
            TextView leftText = (TextView)
                    row.findViewById(R.id.taglistrow_lefttext);
            TextView rightText = (TextView)
                    row.findViewById(R.id.taglistrow_righttext);

            Tag tag = tags.get(position);
            leftText.setText(tag.name.replace('_', ' '));
            rightText.setText(" " + String.valueOf(tag.count));

            Resources res = getResources();
            switch (tag.type) {
            case 0: //general, blue
                leftText.setTextColor(res.getColor(R.color.tag_general));
                break;
            case 1: //artist, red
                leftText.setTextColor(res.getColor(R.color.tag_artist));
                break;
            //case 2: dunno
            case 3: //copyright, purple
                leftText.setTextColor(res.getColor(R.color.tag_copyright));
                break;
            case 4: //character, green
                leftText.setTextColor(res.getColor(R.color.tag_character));
                break;
            default: //make it blue
                leftText.setTextColor(res.getColor(R.color.tag_general));
                break;
            }

            return row;
        }
    }
}
