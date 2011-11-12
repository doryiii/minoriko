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

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.duy.minoriko2.R;
import com.duy.minoriko2.control.Helper;
import com.duy.minoriko2.control.XMLDownloader;
import com.duy.minoriko2.model.Favorite;
import com.duy.minoriko2.model.Favorites;
import com.duy.minoriko2.model.Post;
import com.duy.minoriko2.model.Tag;
import com.duy.minoriko2.widgets.MySearchBox;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.LinearLayout;
import android.widget.MultiAutoCompleteTextView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

public class PostListActivity extends ImgGridActivity {
    private String query;
    private XMLDownloader postsDownloader;
    private boolean destroying;
    private LinearLayout searchBar;
    private LinearLayout dummyView;
    private MySearchBox searchTxt;
    private Button searchBtn;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        searchBar = (LinearLayout) findViewById(R.id.imggrid_searchbar);
        searchTxt = (MySearchBox) findViewById(R.id.imggrid_searchtxt);
        searchTxt.setTokenizer(new MultiAutoCompleteTextView.Tokenizer() {
            @Override
            public int findTokenEnd(CharSequence text, int cursor) {
                int i = cursor;
                int len = text.length();
                while (i<len) {
                    if (text.charAt(i) == ' ') {
                        return i;
                    } else {
                        i++;
                    }
                }
                return len;
            }

            @Override
            public int findTokenStart(CharSequence text, int cursor) {
                int i = cursor;
                while (i>0 && text.charAt(i-1) != ' ') { i--; }
                return i;
            }

            @Override
            public CharSequence terminateToken(CharSequence text) {
                if (text.charAt(text.length() - 1) == ' ') {
                    return text;
                } else {
                    return text + " ";
                }
            }
        });
        searchBtn = (Button) findViewById(R.id.imggrid_searchbtn);
        dummyView = (LinearLayout) findViewById(R.id.imggrid_dummy);

        searchTxt.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId,
                    KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    onSearchBtnClicked();
                    return true;
                }
                return false;
            }
        });
        searchTxt.setAdapter(new TagsAdapter());

        searchBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                onSearchBtnClicked();
            }
        });

        init(getIntent());
    }

    private void onSearchBtnClicked() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEARCH);
        intent.putExtra(SearchManager.QUERY, searchTxt.getText().toString());

        onNewIntent(intent);
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
        if (postsDownloader != null) {
            postsDownloader.cancel(true);
            postsDownloader = null;
        }
        query = null;
        posts = null;
        destroying = true;
    }

    private void init(Intent intent) {
        String intentAction = intent.getAction();

        if (postsDownloader != null) {
            //Log.v(Helper.TAG, "PostList Double Launch, cancelling");
            postsDownloader.cancel(true);
        }
        postsDownloader = null;
        page = 0;

        if (intentAction.equals(Intent.ACTION_DEFAULT)) {
            query = "";
            setTitle(Helper.getServerRoot(this));
            searchTxt.setText("");
            dummyView.requestFocus();
            InputMethodManager imm = (InputMethodManager)
                    getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(searchTxt.getWindowToken(), 0);
        } else if (intentAction.equals(Intent.ACTION_SEARCH)) {
            query = intent.getStringExtra(SearchManager.QUERY);
            setTitle(query + " - " + Helper.getServerRoot(this));
            searchTxt.setText(query);
            dummyView.requestFocus();
            InputMethodManager imm = (InputMethodManager)
                    getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(searchTxt.getWindowToken(), 0);
        } else if (intentAction.equals(Intent.ACTION_RUN)) {
            query = "";
            setTitle(Helper.getServerRoot(this));
            onSearchRequested();
        }

        searchBar.setVisibility(View.VISIBLE);
        launchSearch();
        app.putObject(getListKey(), posts);
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
            intent.putExtra("mode", "post");
            intent.putExtra("post_query", query);
            startActivityForResult(intent, Helper.IMGVIEW_REQCODE);
        } else {
            Helper.launchBrowser(this, url);
        }
    }

    @Override
    public boolean onSearchRequested() {
        searchTxt.requestFocus();
        //searchTxt.requestFocusFromTouch();
        int physKBstate = getResources().getConfiguration().hardKeyboardHidden;
        if (!(physKBstate == Configuration.HARDKEYBOARDHIDDEN_NO)) {
            InputMethodManager imm = (InputMethodManager)
                    getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(searchTxt, InputMethodManager.SHOW_FORCED);
        }
        return true;
    }

    private void launchSearch() {
        if (postsDownloader != null) {
            postsDownloader.cancel(true);
        }
        postsDownloader = null;

        getGridAdapter().waiting = true;
        launchDownload(1);
    }

    private void launchDownload(int doPage) {
        if (destroying)
            return;

        if (postsDownloader != null)
            return;

        //Log.v(Helper.TAG, "PostList Starting " + doPage);
        getProgressBar().setVisibility(View.VISIBLE);
        getProgressBar().setProgress(0);
        postsDownloader = new XMLDownloader(
                getGridAdapter(), getProgressBar(),
                Helper.getPostListUrl(this, query, doPage));
        postsDownloader.execute();
    }

    /**
     * This should only be called by openPost(i)
     * Main purpose is that it serves a key for the subsequent activities
     * to retrieve objects from Application context.
     */
    @Override
    public String getListKey() {
        return Helper.getPostListUrl(this, query, -1);
    }

    @Override
    public void retryDownload() {
        postsDownloader = null;
        launchDownload(page + 1);
    }

    @Override
    public void stopTrying() {
        postsDownloader = null;
    }

    @Override
    public void onLastItemViewed() {
        launchDownload(page + 1);
    }

    @Override
    public void onNewPageLoaded(Document doc) {
        page++;
        postsDownloader = null;
    }

    @Override
    public NodeList getXMLArray(Document doc) throws NullPointerException {
        return ((Element) doc.getDocumentElement())
                .getElementsByTagName("post");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.postlist, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

        case R.id.postlist_menu_fav:
            String display = query;
            if ("".equals(query)) {
                display = "(Posts)";
            }
            Favorites favDB = new Favorites(this);
            Favorite f = new Favorite (
                    Favorite.Type.POST_SEARCH,
                    Helper.getServerRoot(this),
                    query, display, Helper.getServerType(this));
            if (favDB.add(f)) {
                Toast.makeText(this, "Added to favorite",
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Favorite already exist",
                        Toast.LENGTH_SHORT).show();
            }

            favDB.close();
            return true;

        case R.id.postlist_menu_refresh:
            MinorikoApplication.domCache.clearCache();
            onNewIntent(getIntent());
            return true;

        default:
            return super.onOptionsItemSelected(item);
        }
    }


    public class TagsAdapter extends BaseAdapter implements Filterable {
        private ArrayList<Tag> tags = new ArrayList<Tag>();

        public void clear() {
            synchronized (tags) {
                tags.clear();
            }
        }

        @Override
        public int getCount() {
            return tags.size();
        }

        @Override
        public String getItem(int index) {
            return tags.get(index).name;
        }

        @Override
        public long getItemId(int index) {
            return tags.get(index).id;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            if (row == null) {
                  LayoutInflater inflater = LayoutInflater.from(
                          PostListActivity.this);
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

        @Override
        public Filter getFilter() {
            Filter myFilter = new Filter() {
                private FilterResults res = null;

                @Override
                protected FilterResults performFiltering(CharSequence query) {
                    FilterResults filterResults = new FilterResults();
                    res = filterResults; // only write to res, so no locking

                    try {
                        ArrayList<Tag> tags;
                        if (query != null &&
                            query.length() >= searchTxt.getThreshold()) {
//			            	Log.e(Helper.TAG, "Filter.performFiltering " +
//			            			query + " res=" + res.toString());
                            tags = downloadTags(query.toString());
                        } else {
                            tags = new ArrayList<Tag>();
                        }

                        filterResults.values = tags;
                        filterResults.count = tags.size();
                        return filterResults;

                    } catch (IOException e) {
                        return null;
                    } catch (SAXException e) {
                        return null;
                    }
                }

                @SuppressWarnings("unchecked")
                @Override
                protected void publishResults(
                        CharSequence contraint, FilterResults results) {
                    if (res != results) {
                        tags.clear();
                        notifyDataSetInvalidated();
                        return;
                    }
//	            	Log.e(Helper.TAG, "Filter.complete " +
//	            			"res=" + res.toString());

                    if(results != null && results.count > 0) {
                        tags.clear();
                        tags = (ArrayList<Tag>) results.values;
                        notifyDataSetChanged();
                    }
                    else {
                        notifyDataSetInvalidated();
                    }
                }
            };
            return myFilter;
        }

        public ArrayList<Tag> downloadTags(String query)
                throws IOException, SAXException {
            URL url = new URL(Helper.getTagsUrl(PostListActivity.this, query));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.addRequestProperty("Referer",
                    Helper.getServerRoot(PostListActivity.this));
            conn.connect();
            InputStream istream = conn.getInputStream();
            DocumentBuilderFactory factory =
                    DocumentBuilderFactory.newInstance();
            factory.setIgnoringComments(true);
            DocumentBuilder builder;
            try {
                builder = factory.newDocumentBuilder();
            } catch (ParserConfigurationException e) { return null; }

            NodeList nodes = builder.parse(istream).getDocumentElement()
                    .getElementsByTagName("tag");

            ArrayList<Tag> tags = new ArrayList<Tag>(nodes.getLength());
            if (Helper.isTagsSortDesc(PostListActivity.this, query)) {
                for (int i=0; i<nodes.getLength(); i++) {
                    tags.add(new Tag((Element) nodes.item(i)));
                }
            } else {
                for (int i=nodes.getLength()-1; i>=0; i--) {
                    tags.add(new Tag((Element) nodes.item(i)));
                }
            }

            return tags;
        }
    }
}
