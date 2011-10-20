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

import android.os.CountDownTimer;

import com.duy.minoriko2.control.BitmapDownloaderTask;

public abstract class ImgCountDownTimer extends CountDownTimer {
    public final BitmapDownloaderTask task;

    public ImgCountDownTimer(long millisInFuture, long countDownInterval) {
        super(millisInFuture, countDownInterval);
        this.task = null;
    }
    public ImgCountDownTimer(long millisInFuture, long countDownInterval,
            BitmapDownloaderTask task) {
        super(millisInFuture, countDownInterval);
        this.task = task;
    }
}
