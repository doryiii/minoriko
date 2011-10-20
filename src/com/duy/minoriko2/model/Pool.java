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

public class Pool {
    public final int id;
    public final String artist;
    public final String name;
    public final int post_count;
    public final String desc;

    public Pool(Element node)
            throws NumberFormatException, NullPointerException {
        //this.context = context;
        this.id = Integer.parseInt(node.getAttribute("id"));
        this.post_count = Integer.parseInt(node.getAttribute("post_count"));

        String fullName = node.getAttribute("name").replace('_', ' ');
        if (fullName.endsWith(")")) {
            int nOpen = 0, i;
            int close = fullName.length();
            for (i=fullName.length()-1; i>=0; i--) {
                if (fullName.charAt(i) == ')') {
                    nOpen++;
                } else if (fullName.charAt(i) == '(') {
                    if (nOpen > 0) {
                        nOpen--;
                        if (nOpen == 0) {
                            close = i;
                            break;
                        }
                    }
                }
            }
            if (close < fullName.length()) {
                this.name = fullName.substring(0, close);
                this.artist = fullName.substring(close+1, fullName.length() - 1);
            } else {
                this.name = fullName;
                this.artist = "";
            }
        } else {
            this.name = fullName;
            this.artist = "";
        }

        String desc = "";
        try {
        	desc = Helper.convertDanbooruToHtml(
        			node.getElementsByTagName("description")
        			.item(0).getTextContent());
        } catch (Exception e) {}

        this.desc = desc;
    }
}
