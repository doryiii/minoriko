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

public class PoolList {
    private ArrayList<Pool> pools;
    private HashMap<Integer, Integer> idMap;

    public PoolList() {
        pools = new ArrayList<Pool>(Helper.POOL_PER_PAGE);
        idMap = new HashMap<Integer, Integer>(Helper.POOL_PER_PAGE);
    }

    public void add(Pool p) {
        pools.add(p);
        idMap.put(p.id, pools.size() - 1);
    }

    public int size() {
        return pools.size();
    }

    public Pool get(int position) {
        return pools.get(position);
    }

    public int getPositionById(int id) {
        try {
            return idMap.get(id);
        } catch (NullPointerException e) {
            return pools.size();
        }
    }

    public long getId(int position) {
        try {
            return pools.get(position).id;
        } catch (NullPointerException e) {
            return -1;
        }
    }

    /**
     * This method will find the pool that has the same ID as the first pool
     * in the text, and skips from there to the end of current list.
     * The reason for that is because pools in Danbooru are updatedfrequently,
     * in the order of minutes, and often getting next page will have
     * duplicates from previous page. This will eliminate duplicates
     * Assumption: dups are continuous, no pool is deleted in the meantime.
     *
     * @param NodeList new pool infos
     * @return true if new pool added, false if at the end already
     * (empty returned array)
     * @throws NumberFormatException, NullPointerException
     */
    public boolean mergeListWithXml(NodeList xmlArray)
            throws NumberFormatException, NullPointerException {

        if (xmlArray.getLength() == 0)
            return false;

        int id = Integer.parseInt(
                ((Element) xmlArray.item(0)).getAttribute("id"));
        int i = getPositionById(id);

        // (pools.length - i) will now be the number of items matched
        // thus will be the number of items to skip
        if ((pools.size() - i) >= xmlArray.getLength())
            return false; //TODO: Better to launch next page download
        for (int j=(pools.size() - i); j<xmlArray.getLength(); j++) {
            add(new Pool((Element) xmlArray.item(j)));
        }

        return true;
    }
}
