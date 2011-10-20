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

import java.lang.ref.WeakReference;

import android.graphics.Bitmap;
import android.widget.ImageView;
import android.widget.ProgressBar;

public interface DownloaderCallback {
    void onPostExecute(String url, BitmapDownloaderTask bitmapDownloaderTask,
            WeakReference<ImageView> imageViewReference,
            WeakReference<ProgressBar> progressBarReference, Bitmap bitmap);
}
