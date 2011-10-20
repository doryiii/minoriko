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

import org.w3c.dom.Element;

import com.duy.minoriko2.control.Helper;

import android.content.Context;

public class Post {
    public int w, h;//, sample_w, sample_h, preview_w, preview_h;
    public String sample_url, file_url, preview_url;//, source;
    public String[] tags;
    public int id;//, parent_id;
    //public boolean bitmapIsSample = true;
    public boolean has_notes, has_comments;
    public int rating;
    //Context context;

    public Post(Context context, Element element)
            throws NumberFormatException, NullPointerException {
        w = Integer.parseInt(element.getAttribute("width"));
        h = Integer.parseInt(element.getAttribute("height"));
        id = Integer.parseInt(element.getAttribute("id"));
        sample_url = Helper.urlCheck(context,
                element.getAttribute(("sample_url")));
        preview_url = Helper.urlCheck(context,
                element.getAttribute(("preview_url")));
        file_url = Helper.urlCheck(context,
                element.getAttribute(("file_url")));
        tags = element.getAttribute("tags").trim().split(" ");

        if (element.hasAttribute("has_notes")) {
            has_notes = Boolean.parseBoolean(
                    element.getAttribute("has_notes"));
        } else {
            has_notes = true;
        }
//    	if (element.hasAttribute("has_comments")) {
//    		has_comments = Boolean.parseBoolean(
//    				element.getAttribute("has_comments"));
//    	} else {
//    		has_comments = true;
//    	}
        has_comments = true; // danbooru bug; has_comment not always correct

        rating = Helper.getRatingInt(element.getAttribute("rating"));
    }

    public long getId() {
        return this.id;
    }
}
