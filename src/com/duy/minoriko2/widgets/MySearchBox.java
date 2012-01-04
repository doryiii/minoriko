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
import android.util.AttributeSet;
import android.widget.MultiAutoCompleteTextView;

import com.duy.minoriko2.PostListActivity.TagsAdapter;

public class MySearchBox extends MultiAutoCompleteTextView {

    public MySearchBox(Context context) {
        super(context);
    }
    public MySearchBox(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public MySearchBox(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void performFiltering(CharSequence text, int keyCode) {
        ((TagsAdapter) getAdapter()).clear();
        ((TagsAdapter) getAdapter()).notifyDataSetInvalidated();
        this.clearListSelection();
        //Log.e(Helper.TAG, "Text.performFiltering " + text);
        super.performFiltering(text, keyCode);
    }
}
