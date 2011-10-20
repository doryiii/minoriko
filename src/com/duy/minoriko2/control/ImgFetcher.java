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

package com.duy.minoriko2.control;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

public class ImgFetcher extends AsyncTask<String, Integer, String> {
    WeakReference<ProgressBar> progressBarRef;
    Context appContext;

    private HttpURLConnection conn;
    private InputStream stream; //to read
    BufferedInputStream bstream;
    private String error;

    private int fileSize;
    private int downloaded; // number of bytes downloaded

    private static final int MAX_BUFFER_SIZE = 4096; //1kb
    private byte[] buffer;

    private final DownloadDoneCallback callback;

    public ImgFetcher(Context c, ProgressBar pBar, DownloadDoneCallback cb) {
        progressBarRef = new WeakReference<ProgressBar>(pBar);
        appContext = c.getApplicationContext();
        conn = null;
        fileSize = 0;
        downloaded = 0;
        buffer = new byte[MAX_BUFFER_SIZE];
        callback = cb;
    }

    @Override
    protected String doInBackground(String... addr) {
        try {
            URL url = new URL(addr[0]);

            File SDCardRoot = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS);
            if (!SDCardRoot.exists() && !SDCardRoot.mkdir()) {
                throw new IOException();
            }

            String fileName = URLDecoder.decode(url.getFile());
            fileName = fileName.substring(fileName.lastIndexOf('/') + 1);
            File file = new File(SDCardRoot, fileName);
            FileOutputStream fileOutput = new FileOutputStream(file);
            int nRead;

            File cache = new File(BitmapDownloaderTask.getFileName(
                    appContext, addr[0]));

            if (cache.exists()) {
                InputStream cacheInput = new FileInputStream(cache);
                while ((nRead = cacheInput.read(buffer, 0, MAX_BUFFER_SIZE))
                        > 0) {
                    fileOutput.write(buffer, 0, nRead);
                    downloaded += nRead;
                    publishProgress(downloaded);
                }
                fileOutput.close();
                cacheInput.close();
                return SDCardRoot + System.getProperty("file.separator") +
                        fileName;

            } else {
                conn = (HttpURLConnection) url.openConnection();
                conn.addRequestProperty("Referer",
                        Helper.getServerRoot(appContext));
                fileSize = conn.getContentLength();
                conn.connect();

                stream = conn.getInputStream();
                bstream = new BufferedInputStream(stream);

                while ((nRead = bstream.read(buffer, 0, MAX_BUFFER_SIZE))
                        != -1) {
                    if (isCancelled()) {
                        fileOutput.close();
                        file.delete();
                        bstream.close();
                        return null;
                    }
                    fileOutput .write(buffer, 0, nRead);
                    downloaded += nRead;
                    publishProgress(downloaded);
                }
                fileOutput.close();
                bstream.close();
                return SDCardRoot + System.getProperty("file.separator") +
                        fileName;
            }
        } catch (MalformedURLException e) {
            synchronized (this) {
                error = "Bad URL reported by danbooru site";
            }
        } catch (IOException e) {
            synchronized (this) {
                error = "Failed to download file to SD card";
            }
        } finally {
//            if (bstream != null) {
//                bstream.close();
//            }
        }

        return null;
    }

    @Override
    protected void onProgressUpdate(Integer... changed) {
        if (progressBarRef != null) {
            ProgressBar progressBar = progressBarRef.get();
            if (progressBar != null) {
                progressBar.setMax(fileSize);
                progressBar.setProgress(changed[0]);
            }
        }
    }

    @Override
    protected void onPostExecute(String loc) {
        if (progressBarRef != null) {
            ProgressBar progressBar = progressBarRef.get();
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
        }

        if (loc == null) {
            Toast.makeText(appContext, error,
                    Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(appContext, "Saved file " + loc,
                    Toast.LENGTH_SHORT).show();
        }

        if (callback != null) {
            callback.done();
        }
    }

    @Override
    protected void onCancelled() {
        if (progressBarRef != null) {
            ProgressBar progressBar = progressBarRef.get();
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
        }
        Toast.makeText(appContext, "Cancelled",
                Toast.LENGTH_SHORT).show();
    }

    public interface DownloadDoneCallback {
        public void done();
    }
}
