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

import org.w3c.dom.Element;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Picture;
import android.view.View;
import android.webkit.WebView;

public class Note {
    public int x, y, w, h;
    public String txt;
    //String DANBO_URL;
    public WebView wv;
    //private Context context;
    public Picture pic;
    //public static final String TAG = Helper.TAG;

    public Note(Context context, Element element)
            throws NumberFormatException, NullPointerException {
        x = Integer.parseInt(element.getAttribute("x"));
        y = Integer.parseInt(element.getAttribute("y"));
        w = Integer.parseInt(element.getAttribute("width"));
        h = Integer.parseInt(element.getAttribute("height"));
        txt = element.getAttribute("body").trim()
                //.replace("\r\n", "<br>")
                .replace("\n", "<br>")
                //.replace("\r", "<br>")
                .replace("<tn>", "<p class='tn'>")
                .replace("</tn>", "</p>");

        //context = ctx;
        wv = new WebView(context.getApplicationContext());
        drawNote();
    }

    public void drawNote() {
        wv.measure(0, 0);
        wv.layout(0, 0, 0, 0);
        wv.clearView();
        wv.setVisibility(View.VISIBLE);
        //wv.setWebChromeClient(new WebChromeClient() {});
//        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.GINGERBREAD_MR1) {
//	        wv.setPictureListener(new PictureListener() {
//	            @Override
//	            public void onNewPicture(WebView arg0, Picture arg1) {
//	                pic = arg1;//wv.capturePicture();
//	                if (pic == null ||
//	                    pic.getWidth() == 0 ||
//	                    pic.getHeight() == 0) {
//	                    pic = null;
//	                    // Log.e(TAG, "note pic generator failed");
//	                    return;
//	                }
//	                wv.setPictureListener(null);
//	                wv = null;
//	                System.gc();
//	                //Log.v(TAG, "Note pic generated: " + txt);
//	            }
//	        });
//        } else { //honeycomb+, with PictureListener deprecated
//        	wv.setWebViewClient(new WebViewClient() {
//        		@Override
//        		public void onPageFinished(final WebView view, String url) {
//        			view.postDelayed(new Runnable() {
//						@Override
//						public void run() {
//		        			pic = view.capturePicture();
//		        			wv.setWebViewClient(null);
//		        			wv = null;
//						}
//        			}, 750);
//        		}
//        	});
//        }

        wv.setBackgroundColor(Color.WHITE);
        wv.getSettings().setBuiltInZoomControls(false);
        wv.getSettings().setLoadWithOverviewMode(true);
        wv.getSettings().setUseWideViewPort(false);
        wv.getSettings().setJavaScriptEnabled(false);

        int width;
        if (this.txt.length() > 200) {
            width = 400;
        } else if (this.txt.length() > 50) {
            width = 300;
        } else {
            width = 250;
        }

        // Use loadDataWithBaseURL instead of loadData so that
        // WebView can render Unicode and correctly handle %&'?
        wv.loadDataWithBaseURL(null,
            "<html>" +
                "<head>" +
                    "<style type='text/css'>" +
                        "p.tn { font-size:0.8em; color:gray; }" +
                    "</style>" +
                "</head>" +
                "<body style='margin: 0px; width: " + width + "px;'>" +
                    "<div style='font-family: verdana,sans-serif; " +
                        "font-size: 130%; background: #FFE; " +
                        "border:1px solid black; min-height: 10px; " +
                        "margin: 0px; overflow: auto; height: auto; " +
                        "padding: 2px; float:left;'>" +
                            this.txt +
                    "</div>" +
                "</body>" +
            "</html>",
            "text/html", "utf-8", null);

//	    Log.v(TAG, "Note pic loading: " +
//	    		txt);
    }

    public void destroy() {
        //Log.e("Minoriko", "Destroying webviews");
        if (wv != null) {
            wv.destroy();
            wv = null;
        }
        pic = null;
    }
}
