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

package com.duy.minoriko2.widgets;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

public class NotifyImageView extends TimedImageView {
    ImageReadyListener imageReadyListener = null;

    // Constructors
    public NotifyImageView(Context context) {
        super(context);
    }
    public NotifyImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public NotifyImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * Set the ImageReadyListener
     * @param listener
     */
    public void setImageReadyListener(ImageReadyListener listener) {
        this.imageReadyListener = listener;
    }

    /**
     * When a Drawable is loaded, and it is a BitmapDrawable, notify the
     * ImageReadyListener
     */
    @Override
    public void setImageDrawable(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            if (((BitmapDrawable) drawable).getBitmap() != null) {
                super.setImageDrawable(drawable);
            } else {
                super.setImageDrawable(null);
            }
        } else {
            super.setImageDrawable(drawable);
        }

        if (drawable instanceof BitmapDrawable && imageReadyListener != null) {
            imageReadyListener.onImageLoaded(this);
        }
    }

    public interface ImageReadyListener {
        public void onImageLoaded(NotifyImageView imgView);
    }
}
