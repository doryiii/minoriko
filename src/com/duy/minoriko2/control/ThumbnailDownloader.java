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

import java.lang.ref.WeakReference;
import java.util.concurrent.RejectedExecutionException;

import com.duy.minoriko2.widgets.ImgCountDownTimer;
import com.duy.minoriko2.widgets.TimedImageView;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

/**
 * This helper class download images from the Internet and binds those with the provided TimedImageView.
 *
 * <p>It requires the INTERNET permission, which should be added to your application's manifest
 * file.</p>
 *
 * A local cache of downloaded images is maintained internally to improve performance.
 */
public class ThumbnailDownloader implements DownloaderCallback {
    private final Context context;
    public final Cache<Bitmap> cache;

    public ThumbnailDownloader(Context context, Cache<Bitmap> cache) {
        super();
        this.context = context;
        this.cache = cache;
    }

    /**
     * Download the specified image from the Internet and binds it to the provided TimedImageView. The
     * binding is immediate if the image is found in the cache and will be done asynchronously
     * otherwise. A null bitmap will be associated to the TimedImageView if an error occurs.
     *
     * @param url The URL of the image to download.
     * @param imageView The TimedImageView to bind the downloaded image to.
     * @param progressBar The progress bar to update progress to. Can be null.
     * @param checker an object to check whether a thread should be launched.
     */
    public void download(String url, TimedImageView imageView, ProgressBar progressBar) {
        cache.resetPurgeTimer();
        Bitmap bitmap = cache.getFromCache(url);


        if (bitmap == null) {
            forceDownload(url, imageView, progressBar);
        } else {
            cancelPotentialDownload(url, imageView);
            imageView.setImageBitmap(bitmap);
        }
    }

    public void download(String url, TimedImageView imageView) {
        download(url, imageView, null);
    }

    /**
     * Same as download but the image is always downloaded and the cache is not used.
     * Kept private at the moment as its interest is not clear.
     */
    private void forceDownload(String url, TimedImageView imageView, ProgressBar progressBar) {
        // State sanity: url is guaranteed to never be null in DownloadedDrawable and cache keys.
        if (url == null) {
            imageView.setImageDrawable(null);
            return;
        }

        if (cancelPotentialDownload(url, imageView)) {
            final BitmapDownloaderTask task = new BitmapDownloaderTask(
                    context, this, imageView, progressBar, false, url);
            DownloadedDrawable downloadedDrawable = new DownloadedDrawable(task);
            imageView.setImageDrawable(downloadedDrawable);

            if (progressBar != null) {
                progressBar.setVisibility(View.VISIBLE);
            }

            if (imageView.timer != null) {
                imageView.timer.cancel();
            }
//            Log.e(Helper.TAG,
//            		Thread.currentThread().getId() + " - " +
//            		"DLD: " + imageView.toString() + " - " + url.hashCode());

            imageView.timer = new ImgCountDownTimer(700, 701, task) {
                @Override
                public void onFinish() {
                    //Log.e(Helper.TAG, "TMR: " + this.task.toString() + " " + this.url.hashCode());
                    try {
                        this.task.execute();
                    } catch (RejectedExecutionException e) {
                        this.start();
                    }
                }
                @Override
                public void onTick(long millisUntilFinished) {}
            };
            imageView.timer.start();
        }
    }

    public static void cancelDownload(TimedImageView imageView) {
        BitmapDownloaderTask bitmapDownloaderTask = getBitmapDownloaderTask(imageView);
        if (bitmapDownloaderTask != null) {
            bitmapDownloaderTask.cancel(false);
       }
    }

    /**
     * Returns true if the current download has been canceled or if there was no download in
     * progress on this image view.
     * Returns false if the download in progress deals with the same url. The download is not
     * stopped in that case.
     */
    private static boolean cancelPotentialDownload(String url, TimedImageView imageView) {
        BitmapDownloaderTask bitmapDownloaderTask = getBitmapDownloaderTask(imageView);
//        Log.w(Helper.TAG,  Thread.currentThread().getId() + " - " +
//        		(imageView != null ? imageView.toString() : "null") + " - " +
//        		(bitmapDownloaderTask != null ? bitmapDownloaderTask.toString() : "null") + " - " +
//        		(url != null ? Integer.toHexString(url.hashCode()) : "null"));

        if (bitmapDownloaderTask != null) {
            String bitmapUrl = bitmapDownloaderTask.url;
            if ((bitmapUrl == null) || (!bitmapUrl.equals(url))) {
//            	Log.w(Helper.TAG, "Cancelling: " + Integer.toHexString(url.hashCode()));
                bitmapDownloaderTask.cancel(true);
            } else {
                // The same URL is already being downloaded.
//            	Log.w(Helper.TAG, "Ignoring: " + Integer.toHexString(url.hashCode()));
                return false;
            }
        }
        return true;
    }

    /**
     * @param imageView Any imageView
     * @return Retrieve the currently active download task (if any) associated with this imageView.
     * null if there is no such task.
     */
    private static BitmapDownloaderTask getBitmapDownloaderTask(TimedImageView imageView) {
//    	Log.i(Helper.TAG, Thread.currentThread().getId() + " - " +
//    			(imageView != null ? imageView.toString() : "null") + " - " +
//    			(imageView.getDrawable() != null ? imageView.getDrawable().toString() : "null"));
        if (imageView != null) {
            Drawable drawable = imageView.getDrawable();
            if (drawable instanceof DownloadedDrawable) {
                DownloadedDrawable downloadedDrawable = (DownloadedDrawable)drawable;
                return downloadedDrawable.getBitmapDownloaderTask();
            }
        }
        return null;
    }

    @Override
    public void onPostExecute(String url, BitmapDownloaderTask task,
            WeakReference<ImageView> imageViewReference,
            WeakReference<ProgressBar> progressBarReference, Bitmap bitmap) {
        cache.addToCache(url, bitmap);

        if (imageViewReference != null) {
            ImageView imageView = imageViewReference.get();
            BitmapDownloaderTask bitmapDownloaderTask = BitmapDownloaderTask
                    .getBitmapDownloaderTask(imageView);
            // Change bitmap only if this process is still associated with it
            if (task == bitmapDownloaderTask) {
                imageView.setImageBitmap(bitmap);
            }
        }

        if (progressBarReference != null) {
            ProgressBar progressBar = progressBarReference.get();
            if (progressBar != null) {
                progressBar.setVisibility(ProgressBar.GONE);
            }
        }
    }
}
