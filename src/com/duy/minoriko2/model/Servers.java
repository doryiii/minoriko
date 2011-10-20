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

import com.duy.minoriko2.model.Server.Type;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class Servers extends SQLiteOpenHelper {
    static final String dbName = "minorikoServers";
    static final String tableName = "servers";
    static final String idColName = "id";
    static final String typeColName = "type";
    static final String serverColName = "server";
    static final String[] allCols = { idColName, typeColName, serverColName};
    static final int version = 3;
    SQLiteDatabase mdb;


    public Servers(Context context) {
        super(context, dbName, null, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String q = "CREATE TABLE " + tableName + " ( " +
                   idColName + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                   typeColName + " STRING NOT NULL, " +
                   serverColName + " STRING NOT NULL); ";
        db.execSQL(q);

        ContentValues cv = new ContentValues();
        cv.put(typeColName, Type.DANBOORU.name());
        cv.put(serverColName, "http://hijiribe.donmai.us/");
        db.insert(tableName, null, cv);
        cv.clear();

        cv.put(typeColName, Type.DANBOORU.name());
        cv.put(serverColName, "http://danbooru.donmai.us/");
        db.insert(tableName, null, cv);
        cv.clear();

        cv.put(typeColName, Type.DANBOORU.name());
        cv.put(serverColName, "http://konachan.com/");
        db.insert(tableName, null, cv);
        cv.clear();

        cv.put(typeColName, Type.GELBOORU.name());
        cv.put(serverColName, "http://gelbooru.com/");
        db.insert(tableName, null, cv);
        cv.clear();

        cv.put(typeColName, Type.DANBOORU.name());
        cv.put(serverColName, "http://behoimi.org/");
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
    public boolean add(Type type, String server) {
        mdb = getWritableDatabase();
        Cursor c = mdb.query(tableName, allCols,
                serverColName + "='" + server.replace("'", "''") + "';",
                null, null, null, serverColName);
        if (c.getCount() != 0) {
            return false; // already exists
        }

        ContentValues cv = new ContentValues();
        cv.put(typeColName, type.name());
        cv.put(serverColName, server);
        mdb.insert(tableName, null, cv);
        return true;
    }

    public void delete(long id) {
        mdb = getReadableDatabase();
        mdb.delete(tableName, idColName + "=" + id, null);
    }

    @Override
    public synchronized void close() {
        if (mdb != null){
            mdb.close();
        }
        super.close();
    }


    /**
     * Get all the favorites in the database
     * @return an array of Favorite
     */
    public Server[] getAll() {
        mdb = getReadableDatabase();
        Cursor c = mdb.query(tableName, allCols, null, null,
                null, null, idColName);

        Server[] fs = new Server[c.getCount()];
        c.moveToFirst();
        for (int i=0; i<fs.length; i++) {
            fs[i] = new Server(
                    c.getInt(c.getColumnIndex(idColName)),
                    c.getString(c.getColumnIndex(serverColName)),
                    Type.valueOf(c.getString(
                            c.getColumnIndex(typeColName))));
            c.moveToNext();
        }

        return fs;
    }
}
