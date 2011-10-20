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

import java.lang.ref.WeakReference;
import java.util.Hashtable;

import org.w3c.dom.Document;

import com.duy.minoriko2.control.Cache;
import com.duy.minoriko2.control.ImgDownloader;
import com.duy.minoriko2.control.ThumbnailDownloader;

import android.app.Application;
import android.graphics.Bitmap;

public class MinorikoApplication extends Application {
    public ThumbnailDownloader previewDownloader;
    public ImgDownloader imgDownloader;
    public static Cache<Document> domCache = new Cache<Document>(20);
    public static Cache<Bitmap> thumbCache = new Cache<Bitmap>(25);
    public static Cache<Bitmap> imgCache = new Cache<Bitmap>(1);
    private Hashtable<String, WeakReference<Object>> storage =
            new Hashtable<String, WeakReference<Object>>();

    @Override
    public void onCreate() {
        super.onCreate();
        previewDownloader = new ThumbnailDownloader(this, thumbCache);
        imgDownloader = new ImgDownloader(this, imgCache);
    }

    public Object getObject(String key) {
        WeakReference<Object> ref = storage.get(key);
        return (ref != null) ? ref.get() : null;
    }
    public void putObject(String key, Object value) {
        storage.put(key, new WeakReference<Object>(value));
    }
    public void delObject(String key) {
        storage.remove(key);
    }
}
