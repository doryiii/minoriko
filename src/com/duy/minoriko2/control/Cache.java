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
 *
 * Derived from code by The Android Open Source Project
 * Modification is not made available under the Apache 2.0 license
 * The Apache 2.0 license is included for attribution purpose only.
 */

/**
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.duy.minoriko2.control;

import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentHashMap;

import android.os.Handler;

/**
 * Cache-related fields and methods.
 *
 * We use a hard and a soft cache. A soft reference cache is too aggressively cleared by the
 * Garbage Collector.
 */
public class Cache<T> {

    private final int HARD_CACHE_CAPACITY;
    private static final int DELAY_BEFORE_PURGE = 90 * 1000; // in milliseconds
    // Hard cache, with a fixed maximum capacity and a life duration
    private final HashMap<String, T> sHardCache;
    // Soft cache for bitmaps kicked out of hard cache
    private final ConcurrentHashMap<String, SoftReference<T>> sSoftCache;
    private final Handler purgeHandler = new Handler();
    // The runnable to clear the cache
    private final Runnable purger = new Runnable() {
        public void run() {
            clearCache();
        }
    };

    public Cache(int capacity) {
        this.HARD_CACHE_CAPACITY = capacity;
        sHardCache =
                new LinkedHashMap<String, T>
                        (HARD_CACHE_CAPACITY / 2, 0.75f, true) {
                    private static final long serialVersionUID =
                            -8857250249129151362L;

                    @Override
                    protected boolean removeEldestEntry(
                            LinkedHashMap.Entry<String, T> eldest) {
                        if (size() > HARD_CACHE_CAPACITY) {
                            // Entries push-out of hard reference cache are
                            // transferred to soft reference cache
                            sSoftCache.put(eldest.getKey(),
                                    new SoftReference<T>(eldest.getValue()));
                            return true;
                        } else
                            return false;
                    }
                };

        sSoftCache =
                new ConcurrentHashMap<String, SoftReference<T>>
                (HARD_CACHE_CAPACITY / 2);
    }

    /**
     * Adds this object to the cache.
     * @param bitmap The newly downloaded bitmap.
     */
    public void addToCache(String url, T obj) {
        if (obj != null) {
            synchronized (sHardCache) {
                sHardCache.put(url, obj);
            }
        }
    }
    /**
     * @param url The key of the object that will be retrieved from the cache.
     * @return The cached object or null if it was not found.
     */
    public T getFromCache(String url) {
        // First try the hard reference cache
        synchronized (sHardCache) {
            final T obj = sHardCache.get(url);
            if (obj != null) {
                // Bitmap found in hard cache
                // Move element to first position, so that it is removed last
                sHardCache.remove(url);
                sHardCache.put(url, obj);
                return obj;
            }
        }

        // Then try the soft reference cache
        SoftReference<T> reference = sSoftCache.get(url);
        if (reference != null) {
            final T obj = reference.get();
            if (obj != null) {
                // Bitmap found in soft cache
                return obj;
            } else {
                // Soft reference has been Garbage Collected
                sSoftCache.remove(url);
            }
        }

        return null;
    }

    /**
     * Clears the image cache used internally to improve performance.
     * Note that for memory efficiency reasons, the cache will automatically
     * be cleared after a certain inactivity delay.
     */
    public void clearCache() {
        sHardCache.clear();
        sSoftCache.clear();
    }

    /**
     * Allow a new delay before the automatic cache clear is done.
     */
    public void resetPurgeTimer() {
        purgeHandler.removeCallbacks(purger);
        purgeHandler.postDelayed(purger, DELAY_BEFORE_PURGE);
    }
}
