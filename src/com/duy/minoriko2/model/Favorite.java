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

public class Favorite {
    public enum Type {
        POOL_SEARCH, POST_SEARCH, TAG_SEARCH, POOL_VIEW
    }

    public final int id;
    public final Type type; // pool search, post search, tag search
    public final String server;
    public final String query;
    public final String display;
    public final Server.Type serverType;

    public Favorite(Type type, String server, String query, String display,
            Server.Type serverType) {
        this.id = -1;
        this.type = type;
        this.server = server;
        this.query = query;
        this.display = display;
        this.serverType = serverType;
    }

    public Favorite(int id, Type type, String server, String query,
            String display, Server.Type serverType) {
        this.id = id;
        this.type = type;
        this.server = server;
        this.query = query;
        this.display = display;
        this.serverType = serverType;
    }
}
