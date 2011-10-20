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

public class Tag {
    public final int id;
    public final String name;
    public final int type;
    public final int count;

    public static final int GENERAL = 0;
    public static final int ARTIST = 0;
    public static final int COPYRIGHT = 0;
    public static final int CHARACTER = 0;

    public Tag(Element element)
            throws NullPointerException, NumberFormatException{
        this.id = Integer.parseInt(element.getAttribute("id"));
        this.name = element.getAttribute("name");
        this.type = Integer.parseInt(element.getAttribute("type"));
        this.count = Integer.parseInt(element.getAttribute("count"));
    }

    public Tag(int id, String name, int type, int count) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.count = count;
    }
}
