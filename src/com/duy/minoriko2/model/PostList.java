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

package com.duy.minoriko2.model;

import java.util.ArrayList;
import java.util.HashMap;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.duy.minoriko2.control.Helper;

import android.content.Context;

public class PostList {
    private ArrayList<Post> posts = new ArrayList<Post>(Helper.POST_PER_PAGE);
    private HashMap<Integer, Integer> idMap = new HashMap<Integer, Integer>();
    private Context context;

    public PostList(Context context) {
        this.context = context;
    }

    public void add(Post p) {
        posts.add(p);
        idMap.put(p.id, posts.size() - 1);
    }

    public int size() {
        return posts.size();
    }
    public Post get(int position) {
        return posts.get(position);
    }
    public int getPositionById(int id) {
        try {
            return idMap.get(id);
        } catch (NullPointerException e) {
            return posts.size();
        }
    }
    public void empty() {
        posts.clear();
        idMap.clear();
    }

    /**
     * This method will find the post that has the same ID as the first post
     * in the text, and skips from there to the end of current list.
     * The reason for that is because posts in Danbooru are added frequently,
     * in the order of minutes, and often getting next page will have
     * duplicates from previous page. This will eliminate duplicates
     * Assumption: dups are continuous, no post is deleted in the meantime.
     *
     * @param xmlArray new post infos
     * @param posts
     * @return true if new post added, false if at the end already
     * (empty returned array)
     * @throws NumberFormatException, NullPointerException
     */
    public boolean mergeListWithXml(NodeList xmlArray)
            throws NumberFormatException, NullPointerException {
        if (xmlArray.getLength() == 0)
            return false;

        int i;

        // get the first post that is within rating
        int xmlArrayLen = xmlArray.getLength();
        for (i=0; i<xmlArrayLen; i++) {
            if (Helper.getRatingInt(
                    ((Element) xmlArray.item(i)).getAttribute("rating"))
                    <= Helper.getRating(context)) {
                break;
            }
        }

        // we filtered out everything; don't add anything but we're not done
        if (i == xmlArray.getLength())
            return true;

        int id = Integer.parseInt(
                ((Element) xmlArray.item(i)).getAttribute("id"));
        i = getPositionById(id);

        // TODO: This has a corner case: new posts are all either
        // explicit or duplicate; but the dups might not be explicit

        // (posts.length - i) will now be the number of items matched
        // thus will be the number of items to skip
        if ((posts.size() - i) >= xmlArray.getLength())
            return false;
        int rating = Helper.getRating(context);
        for (int j=(posts.size() - i); j<xmlArrayLen; j++) {
            Post p = new Post(context, (Element) xmlArray.item(j));
            if (p.rating <= rating) {
                add(p);
            }
        }

        return true;
    }

    public long getId(int position) {
        try {
            return posts.get(position).id;
        } catch (NullPointerException e) {
            return -1;
        }
    }
}

