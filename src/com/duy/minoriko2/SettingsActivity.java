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

package com.duy.minoriko2;

import java.io.File;

import com.duy.minoriko2.R;
import com.duy.minoriko2.control.Helper;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;

public class SettingsActivity extends PreferenceActivity {
    private ListPreference filterPref;
    private Preference clearCachePref;
    private ProgressDialog dialog;
    private ClearCacheTask clearCacheTask = null;
    private CacheSizeTask cacheSizeTask = null;
    private boolean destroying;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setTitle("Minoriko Settings");
        addPreferencesFromResource(R.xml.preferences);
        filterPref = (ListPreference) findPreference("danbo_filter");
        clearCachePref = (Preference) findPreference("clear_cache");
        dialog = new ProgressDialog(this);
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.setTitle("Clearing cache");
        dialog.setMessage("Deleting...");
        dialog.setProgress(0);
        dialog.setCancelable(true);
        dialog.setOnCancelListener(new OnCancelListener() {
            @Override
            public void onCancel(DialogInterface arg0) {
                if (clearCacheTask != null) {
                    clearCacheTask.cancel(false);
                    clearCacheTask = null;
                }
                if (cacheSizeTask == null) {
                    cacheSizeTask = new CacheSizeTask();
                    cacheSizeTask.execute();
                } else {
                    cacheSizeTask.cancel(true);
                    cacheSizeTask = new CacheSizeTask();
                    cacheSizeTask.execute();
                }
            }
        });

        filterPref.setSummary(Helper.getRatingString(this));
        filterPref.setOnPreferenceChangeListener(
                new OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference p,
                            Object value) {
                        if (destroying)
                            return true;

                        filterPref.setSummary(
                                Helper.getRatingString((String) value));
                        return true;
                    }

        });

        cacheSizeTask = new CacheSizeTask();
        cacheSizeTask.execute();
        clearCachePref.setOnPreferenceClickListener(
                new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference arg0) {
                if (destroying)
                    return true;

                // clear SD cache
                if (clearCacheTask == null) {
                    clearCacheTask = new ClearCacheTask();
                    dialog.show();
                    clearCacheTask.execute();
                }

                // clear mem cache
                MinorikoApplication.domCache.clearCache();
                MinorikoApplication app =
                        (MinorikoApplication) getApplication();
                app.imgDownloader.cache.clearCache();
                app.previewDownloader.cache.clearCache();
                return true;
            }
        });

        setResult(RESULT_OK);
        destroying = false;
    }

    @Override
    public void onDestroy() {
        destroying = true;
        super.onDestroy();
        filterPref = null;
        clearCachePref = null;
        if (dialog != null) {
            dialog.dismiss();
            dialog = null;
        }
        if (clearCacheTask != null) {
            clearCacheTask.cancel(true);
            clearCacheTask = null;
        }
        if (cacheSizeTask != null) {
            cacheSizeTask.cancel(true);
            cacheSizeTask = null;
        }
    }

    class ClearCacheTask extends AsyncTask<Void, Integer, Void> {
        int total = 1;

        @Override
        protected Void doInBackground(Void... args) {
            File dir = SettingsActivity.this.getExternalCacheDir();
            if (dir == null) // no card
                return null;

            if (dir.isDirectory()) {
                File[] children = dir.listFiles();
                total = children.length;
                for (int i = 0; i < total; i++) {
                    if (isCancelled())
                        return null;
                    publishProgress(i);

                    if (children[i].isFile())
                        children[i].delete();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            dialog.dismiss();
            clearCacheTask = null;
            (new CacheSizeTask()).execute();
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            dialog.setProgress(progress[0]);
            dialog.setMax(total);
        }
    }

    class CacheSizeTask extends AsyncTask<Void, Integer, Integer> {
        @Override
        protected Integer doInBackground(Void... args) {
            int result = 0;
            File dir = SettingsActivity.this.getExternalCacheDir();
            if (dir == null) // no card
                return -1;

            File[] fileList = dir.listFiles();

            for(int i = 0; i < fileList.length; i++) {
                if(fileList[i].isFile()) {
                    result += fileList[i].length();
                }
            }
            return result;
        }

        @Override
        protected void onPostExecute(Integer result) {
            if (result < 0) {
                clearCachePref.setSummary("No SD card present");
                clearCachePref.setEnabled(false);
            } else {
                clearCachePref.setSummary("Cache uses " +
                        Helper.humanReadableByteCount(result, false));
            }
        }
    }
}
