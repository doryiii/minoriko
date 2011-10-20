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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.os.Environment;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.duy.minoriko2.control.DownloadedDrawable;

/**
 * The actual AsyncTask that will asynchronously download the image.
 */
public class BitmapDownloaderTask extends AsyncTask<Void, Integer, Bitmap> {
    public String url;
    final WeakReference<ProgressBar> progressBarReference;
    final WeakReference<ImageView> imageViewReference;
    private final DownloaderCallback downloader;
    // private final ImageCache cache;
    private int fileSize = Integer.MAX_VALUE;
    private final Context context;
    // private static final Random random = new Random();
    private final boolean useDiskTmp;

    public BitmapDownloaderTask(Context context, DownloaderCallback downloader,
            ImageView imageView, ProgressBar pb, boolean useDiskTmp,
            String url) {
        this.context = context;
        imageViewReference = new WeakReference<ImageView>(imageView);
        progressBarReference = new WeakReference<ProgressBar>(pb);
        // this.cache = cache;
        this.downloader = downloader;
        this.useDiskTmp = useDiskTmp;
        this.url = url;
    }

    /**
     * Actual download method.
     */
    @Override
    protected Bitmap doInBackground(Void... params) {
        Bitmap b = readFromCache(url);
        if (b != null) {
            return b;
        } else {
            return downloadBitmap(url);
        }
    }

    /**
     * update a progressBar if it exists
     */
    @Override
    protected void onProgressUpdate(Integer... progress) {
        if (progressBarReference != null) {
            ProgressBar progressBar = progressBarReference.get();
            if (progressBar != null) {
                progressBar.setMax(fileSize);
                progressBar.setProgress(progress[0]);
            }
        }
    }

    /**
     * Once the image is downloaded, associates it to the imageView
     */
    @Override
    protected void onPostExecute(Bitmap bitmap) {
        if (isCancelled()) {
            bitmap = null;
        }
        downloader.onPostExecute(url, this, imageViewReference,
                progressBarReference, bitmap);
    }

    public static String getFileName(Context context, String url) {
        File dir = context.getExternalCacheDir();
        if (dir == null) { // no SD card
            return "";
        }
        return dir.getPath() + "/" + Integer.toHexString(url.hashCode());
    }

    Bitmap readFromCache(String url) {
        String fileName = getFileName(context, url);
        try {
            return BitmapFactory.decodeFile(fileName);
        } catch (OutOfMemoryError e) {
            System.gc();
            return null;
        }
    }

    Bitmap downloadBitmap(String url) {
        final int IO_BUFFER_SIZE = 4 * 1024;

        // AndroidHttpClient is not allowed to be used from the main thread
        final HttpClient client = AndroidHttpClient.newInstance("Android");
        final HttpGet getRequest = new HttpGet(url);
        getRequest.addHeader("Referer", Helper.getPostListUrl(context, "", 1));
        File file = null;

        try {
            HttpResponse response = client.execute(getRequest);
            final int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK) {
                // Log.w("ImageDownloader", "Error " + statusCode +
                // " while retrieving bitmap from " + url);
                return null;
            }

            final HttpEntity entity = response.getEntity();
            if (entity != null) {
                InputStream inputStream = null;
                try {
                    fileSize = (int) entity.getContentLength();
                    inputStream = entity.getContent();

                    int nRead;
                    byte[] data = new byte[IO_BUFFER_SIZE];
                    int downloaded = 0;
                    if (isCancelled()) {
                        getRequest.abort();
                        return null;
                    }

                    boolean hasSD = Environment.getExternalStorageState()
                            .equals(Environment.MEDIA_MOUNTED);
                    if (useDiskTmp && hasSD) {
                        // String fileName =
                        // Long.toHexString(random.nextLong());
                        // file = new File(context.getExternalCacheDir(),
                        // fileName);
                        file = new File(getFileName(context, url) + ".tmp");
                        file.deleteOnExit();
                        if (!file.createNewFile())
                            return null;
                        FileOutputStream out = new FileOutputStream(file);
                        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                            if (isCancelled()) {
                                getRequest.abort();
                                out.close();
                                file.delete();
                                return null;
                            }
                            out.write(data, 0, nRead);
                            downloaded += nRead;
                            publishProgress(downloaded);
                        }
                        out.close();
                        Bitmap b = BitmapFactory.decodeFile(file.getPath());
                        File finalFile = new File(getFileName(context, url));
                        file.renameTo(finalFile);
                        finalFile.deleteOnExit();
                        // file.delete();
                        return b;

                    } else { // not use disk cache for bitmap generation
                        ByteArrayOutputStream arrayBuilder = new ByteArrayOutputStream();

                        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                            if (isCancelled()) {
                                getRequest.abort();
                                return null;
                            }
                            arrayBuilder.write(data, 0, nRead);
                            downloaded += nRead;
                            publishProgress(downloaded);
                        }

                        arrayBuilder.flush();

                        data = arrayBuilder.toByteArray();
                        if (hasSD) { // cache to SD
                            FileOutputStream out = new FileOutputStream(
                                    getFileName(context, url));
                            out.write(data);
                            out.close();
                        }

                        return BitmapFactory.decodeByteArray(data, 0,
                                data.length);
                    }
                } finally {
                    if (inputStream != null) {
                        inputStream.close();
                    }
                    entity.consumeContent();
                    // if (file != null)
                    // file.delete();
                }
            }
        } catch (IOException e) {
            getRequest.abort();
        } catch (IllegalStateException e) {
            getRequest.abort();
        } catch (Exception e) {
            getRequest.abort();
        } catch (OutOfMemoryError e) {
            // Toast.makeText(context, "Image too big. Go back to main menu " +
            // "and try again.", Toast.LENGTH_SHORT);
            System.gc();
        } finally {
            if ((client instanceof AndroidHttpClient)) {
                ((AndroidHttpClient) client).close();
                client.getConnectionManager().shutdown();
            }
            if (file != null)
                file.delete();
        }
        return null;
    }

    public static void cancelDownload(ImageView imageView) {
        BitmapDownloaderTask bitmapDownloaderTask = getBitmapDownloaderTask(imageView);
        if (bitmapDownloaderTask != null) {
            bitmapDownloaderTask.cancel(false);
            imageView.setImageDrawable(new ColorDrawable(Color.DKGRAY));
        }
    }

    /**
     * Returns true if the current download has been canceled or if there was no
     * download in progress on this image view. Returns false if the download in
     * progress deals with the same url. The download is not stopped in that
     * case.
     */
    public static boolean cancelPotentialDownload(String url,
            ImageView imageView) {
        BitmapDownloaderTask bitmapDownloaderTask = getBitmapDownloaderTask(imageView);

        if (bitmapDownloaderTask != null) {
            String bitmapUrl = bitmapDownloaderTask.url;
            if ((bitmapUrl == null) || (!bitmapUrl.equals(url))) {
                bitmapDownloaderTask.cancel(true);
            } else {
                // The same URL is already being downloaded.
                return false;
            }
        }
        return true;
    }

    /**
     * @param imageView
     *            Any imageView
     * @return Retrieve the currently active download task (if any) associated
     *         with this imageView. null if there is no such task.
     */
    public static BitmapDownloaderTask getBitmapDownloaderTask(
            ImageView imageView) {
        if (imageView != null) {
            Drawable drawable = imageView.getDrawable();
            if (drawable instanceof DownloadedDrawable) {
                DownloadedDrawable downloadedDrawable = (DownloadedDrawable) drawable;
                return downloadedDrawable.getBitmapDownloaderTask();
            }
        }
        return null;
    }
}
