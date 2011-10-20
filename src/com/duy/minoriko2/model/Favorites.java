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

import com.duy.minoriko2.model.Server;
import com.duy.minoriko2.model.Favorite.Type;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class Favorites extends SQLiteOpenHelper {
    static final String dbName = "minorikoFav";
    static final String tableName = "favorites";
    static final String idColName = "id";
    static final String typeColName = "type";
    static final String serverColName = "server";
    static final String queryColName ="query";
    static final String displayColName ="display";
    static final String serverTypeColName = "stype";
    static final String[] allCols = { idColName, typeColName, serverColName,
                                    queryColName, displayColName,
                                    serverTypeColName};
    static final int version = 3;
    SQLiteDatabase mdb;

    public Favorites(Context context) {
        super(context, dbName, null, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String q = "CREATE TABLE " + tableName + " ( " +
                   idColName + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                   typeColName + " INTEGER NOT NULL, " +
                   serverColName + " STRING NOT NULL, " +
                   queryColName + " STRING, " +
                   serverTypeColName + " STRING, " +
                   displayColName + " STRING); ";
        db.execSQL(q);
        Favorite f = new Favorite(Type.POOL_VIEW,
                "http://hijiribe.donmai.us/",
                "1556", "Minoriko x Danbooru",
                Server.Type.DANBOORU);
        ContentValues cv = new ContentValues();
        cv.put(typeColName, f.type.name());
        cv.put(serverColName, f.server);
        cv.put(displayColName, f.display);
        cv.put(queryColName, f.query);
        cv.put(serverTypeColName, f.serverType.name());
        db.insert(tableName, null, cv);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + tableName);
        onCreate(db);
    }

    /**
     * Add a Favorite into the database
     * @param f the favorite to add
     * @return true if f is added to db, false if f already exist
     */
    public boolean add(Favorite f) {
        mdb = getWritableDatabase();
        Cursor c = mdb.query(tableName, allCols,
                typeColName + "='" + f.type.name() + "' AND " +
                serverColName + "='" + f.server.replace("'", "''") + "' AND " +
                displayColName + "='" + f.display.replace("'", "''") + "' AND " +
                serverTypeColName + "='" + f.serverType.name() + "' AND " +
                queryColName + "='" + f.query.replace("'", "''") + "'",
                null, null, null, serverColName);
        if (c.getCount() != 0) {
            return false; // already exists
        }

        ContentValues cv = new ContentValues();
        cv.put(typeColName, f.type.name());
        cv.put(serverColName, f.server);
        cv.put(displayColName, f.display);
        cv.put(queryColName, f.query);
        cv.put(serverTypeColName, f.serverType.name());
        mdb.insert(tableName, null, cv);
        return true;
    }

    public void delete(long id) {
        mdb = getWritableDatabase();
        mdb.delete(tableName, idColName + "=" + id, null);
    }

    @Override
    public synchronized void close() {
        if(mdb != null){
            mdb.close();
        }
        super.close();
    }

    /**
     * Get all the favorites in the database
     * @return an array of Favorite
     */
    public Favorite[] getAll() {
        mdb = getWritableDatabase();
        Cursor c = mdb.query(tableName, allCols, null, null,
                null, null, serverColName);

        Favorite[] fs = new Favorite[c.getCount()];
        c.moveToFirst();
        for (int i=0; i<fs.length; i++) {
            fs[i] = new Favorite(
                    c.getInt(c.getColumnIndex(idColName)),
                    Type.valueOf(c.getString(
                            c.getColumnIndex(typeColName))),
                    c.getString(c.getColumnIndex(serverColName)),
                    c.getString(c.getColumnIndex(queryColName)),
                    c.getString(c.getColumnIndex(displayColName)),
                    Server.Type.valueOf(c.getString(
                            c.getColumnIndex(serverTypeColName))));
            c.moveToNext();
        }

        return fs;
    }
}
