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
import com.duy.minoriko2.control.StrikeTagHandler;

import android.content.Context;
import android.text.Html;
import android.text.Spanned;

public class Comment {
    public final int id;
    public final String creator;
    public final Spanned body;
    public final String time;
    private static final StrikeTagHandler tagHandler = new StrikeTagHandler();
//
//    public Comment(Context context, int id, String creator,
//    		String body, String time) {
//        this.id = id;
//        this.creator = creator;
//        this.body = new SpannedString(body.trim());
//        this.time = Helper.getCommentTime(context, time);
//    }

    public Comment(Context context, Element e) {
        this.id = Integer.parseInt(e.getAttribute("id"));
        this.creator = e.getAttribute("creator");
        this.body = Html.fromHtml(
        		Helper.convertDanbooruToHtml(e.getAttribute("body")),
                null, tagHandler);
        this.time = Helper.getCommentTime(context,
                e.getAttribute("created_at"));
    }
}
