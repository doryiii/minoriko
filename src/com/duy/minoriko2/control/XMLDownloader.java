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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.lang.ref.WeakReference;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import com.duy.minoriko2.MinorikoApplication;


import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.os.Build;
import android.view.View;
import android.widget.ProgressBar;

public class XMLDownloader extends AsyncTask<Void, Integer, Document> {
    private final WeakReference<XMLCallback> xmlCallbackRef;
    private final WeakReference<ProgressBar> progressBarRef;
    private int fileSize = Integer.MAX_VALUE;
    private final int IO_BUFFER_SIZE = 512;
    private final String url;
    private final boolean convert_endline;

    public XMLDownloader(XMLCallback callback, String url) {
        super();
        xmlCallbackRef = new WeakReference<XMLCallback>(callback);
        progressBarRef = null;
        this.url = url;
        this.convert_endline = false;
    }
    public XMLDownloader(XMLCallback callback, ProgressBar pb, String url) {
        super();
        xmlCallbackRef = new WeakReference<XMLCallback>(callback);
        progressBarRef = new WeakReference<ProgressBar>(pb);
        this.url = url;
        this.convert_endline = false;
    }
    public XMLDownloader(XMLCallback callback, String url,
            boolean convert_endline) {
        super();
        xmlCallbackRef = new WeakReference<XMLCallback>(callback);
        progressBarRef = null;
        this.url = url;
        this.convert_endline = convert_endline;
    }
    public XMLDownloader(XMLCallback callback, ProgressBar pb, String url,
            boolean convert_endline) {
        super();
        xmlCallbackRef = new WeakReference<XMLCallback>(callback);
        progressBarRef = new WeakReference<ProgressBar>(pb);
        this.url = url;
        this.convert_endline = convert_endline;
    }

    @Override
    protected Document doInBackground(Void... none) {
        //Log.e(Helper.TAG, this.toString() + " " + url);
        Document doc = (Document) MinorikoApplication.domCache
                .getFromCache(url);
        if (doc != null) {
            return doc;
        }

        fileSize = 1;
        publishProgress(0);

        // AndroidHttpClient is not allowed to be used from the main thread
        final HttpClient client = AndroidHttpClient.newInstance("Android");
        final HttpGet getRequest = new HttpGet(url);

        try {
            HttpResponse response = client.execute(getRequest);
            final int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK) {
                //Log.w("ImageDownloader", "Error " + statusCode +
                //        " while retrieving " + url);
                return null;
            }

            final HttpEntity entity = response.getEntity();
            if (entity != null) {
                InputStream inputStream = null;
                try {
                    fileSize = (int) entity.getContentLength();
                    inputStream = entity.getContent();

                    DocumentBuilderFactory factory =
                            DocumentBuilderFactory.newInstance();
                    factory.setIgnoringComments(true);
                    DocumentBuilder builder = factory.newDocumentBuilder();

                    if (convert_endline && Build.VERSION.SDK_INT
                            <= Build.VERSION_CODES.GINGERBREAD_MR1) {
                        // Read a buffer at a time
                        ByteArrayOutputStream arrayBuilder =
                                new ByteArrayOutputStream();
                        int nRead;
                        int downloaded = 0;
                        byte[] data = new byte[IO_BUFFER_SIZE];
                        if (isCancelled()) {
                            client.getConnectionManager().shutdown();
                            entity.consumeContent();
                            return null;
                        }
                        while ((nRead = inputStream.read(data, 0, data.length))
                                != -1) {
                            if (isCancelled()) {
                                client.getConnectionManager().shutdown();
                                entity.consumeContent();
                                return null;
                            }
                            arrayBuilder.write(data, 0, nRead);
                            downloaded += nRead;
                            publishProgress(downloaded);
                        }
                        data = arrayBuilder.toByteArray();

                        // Convert to XML
                        if (isCancelled()) {
                            client.getConnectionManager().shutdown();
                            entity.consumeContent();
                            return null;
                        }

                        String str = new String(data);
                        str = str
                                //.replace("\r\n", "\n")
                                //.replace("\r", "\n")
                                .replace("\n", "&#10;");
                        if (isCancelled())
                            return null;
                        doc = builder.parse(new InputSource(new StringReader(
                                str)));
                        if (isCancelled())
                            return null;
                        return doc;
                    } else {
                        if (isCancelled()) {
                            client.getConnectionManager().shutdown();
                            entity.consumeContent();
                            return null;
                        }
                        doc = builder.parse(inputStream);
                        if (isCancelled()) {
                            client.getConnectionManager().shutdown();
                            entity.consumeContent();
                            return null;
                        }
                        return doc;
                    }
                } finally {
                    if (inputStream != null) {
                        inputStream.close();
                    }
                    entity.consumeContent();
                }
            }
        } catch (IOException e) {
            getRequest.abort();
            //Log.w(LOG_TAG, "I/O error while retrieving bitmap from " + url, e);
        } catch (IllegalStateException e) {
            getRequest.abort();
            //Log.w(LOG_TAG, "Incorrect URL: " + url);
        } catch (Exception e) {
            getRequest.abort();
            //Log.w(LOG_TAG, "Error while retrieving bitmap from " + url, e);
        } finally {
            if ((client instanceof AndroidHttpClient)) {
                ((AndroidHttpClient) client).close();
            }
        }
        return null;
    }

    @Override
    protected void onPostExecute(Document doc) {
        if (isCancelled()) {
            //Log.i(Helper.TAG, this.toString() + " " + url);
            return;
        }

        //Log.w(Helper.TAG, this.toString() + " " + url);
        MinorikoApplication.domCache.addToCache(url, doc);
        if (xmlCallbackRef != null) {
            XMLCallback xmlCallback = xmlCallbackRef.get();
            if (xmlCallback != null) {
                xmlCallback.onFinished(doc);
            }
        }
        if (progressBarRef != null) {
            ProgressBar pb = progressBarRef.get();
            if (pb != null) {
                pb.setVisibility(View.GONE);
            }
        }
    }

    @Override
    protected void onProgressUpdate(Integer... progress) {
        if (progressBarRef != null) {
            ProgressBar pb = progressBarRef.get();
            if (pb != null) {
                pb.setProgress(progress[0]);
                pb.setMax(fileSize);
            }
        }
    }

    @Override
    protected void onCancelled() {
        if (xmlCallbackRef != null) {
            XMLCallback xmlCallback = xmlCallbackRef.get();
            xmlCallback.onCancelled();
        }
        if (progressBarRef != null) {
            ProgressBar pb = progressBarRef.get();
            if (pb != null) {
                pb.setVisibility(View.GONE);
            }
        }
    }
}
