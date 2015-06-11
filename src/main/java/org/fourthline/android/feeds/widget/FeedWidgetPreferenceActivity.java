/*
 * Copyright (C) 2012 4th Line GmbH, Switzerland
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.fourthline.android.feeds.widget;

import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.provider.BaseColumns;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import org.fourthline.android.feeds.R;
import org.fourthline.android.feeds.FeedConfigPreferenceActivity;
import org.fourthline.android.feeds.content.FeedWidgetConfig;
import org.fourthline.android.feeds.content.FeedWidgetFeed;

import java.util.logging.Logger;

public class FeedWidgetPreferenceActivity extends PreferenceActivity
   implements SharedPreferences.OnSharedPreferenceChangeListener {

    final private static Logger log = Logger.getLogger(FeedWidgetPreferenceActivity.class.getName());

    protected final static int REQUEST_CREATE_FEED = 0;

    protected String KEY_TOOLBAR;
    protected String KEY_BACKGROUND_COLOR;
    protected String KEY_TEXT_SIZE;
    protected String KEY_SELECT_FEEDS;

    protected long feedWidgetConfigId = -1;
    protected FeedWidgetConfig config;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        log.fine("On create");

        KEY_TOOLBAR = getString(R.string.key_feedwidget_toolbar);
        KEY_BACKGROUND_COLOR = getString(R.string.key_feedwidget_background_color);
        KEY_TEXT_SIZE = getString(R.string.key_feedwidget_text_size);
        KEY_SELECT_FEEDS = getString(R.string.key_feedwidget_feeds);

        addPreferencesFromResource(R.xml.feedwidgetconfig_preferences);

        findPreference(KEY_SELECT_FEEDS).setOnPreferenceClickListener(
           new Preference.OnPreferenceClickListener() {
               public boolean onPreferenceClick(Preference preference) {
                   showFeedSelectionDialog();
                   return true;
               }
           });

        findPreference(KEY_BACKGROUND_COLOR).setOnPreferenceClickListener(
           new Preference.OnPreferenceClickListener() {
               public boolean onPreferenceClick(Preference preference) {
                   showBackgroundColorPickerDialog();
                   return true;
               }
           });

        onIntentUpdated();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        onIntentUpdated();
    }

    @Override
    protected void onResume() {
        super.onResume();
        log.fine("Resuming feed widget config activity");
        loadFeedWidgetConfig(getPreferences());
        getPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        checkFeedSelection();
        log.fine("Pausing feed widget config activity");
        getPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
        log.fine("Feed widget config changed, triggered by new value of: " + key);
        storeFeedWidgetConfig(preferences, true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CREATE_FEED:
                log.fine("Result from feed creation activity, let's see if user created a feed");
                selectAllFeedsAndStore();
                break;
        }
    }

    protected void onIntentUpdated() {
        log.fine("On intent updated: " + getIntent().getData());
        if (getIntent().getExtras() != null) {
            feedWidgetConfigId =
               getIntent().getExtras().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }
        log.fine("Working on feed widget: " + feedWidgetConfigId);
    }

    protected SharedPreferences getPreferences() {
        return getPreferenceScreen().getSharedPreferences();
    }

    protected void loadFeedWidgetConfig(SharedPreferences preferences) {
        log.fine("Loading feed widget config: " + feedWidgetConfigId);
        FeedWidgetConfig config = null;
        Cursor c = null;
        try {
            Uri uri = Uri.withAppendedPath(FeedWidgetConfig.CONTENT_URI, Long.toString(feedWidgetConfigId));
            c = getContentResolver().query(uri, null, null, null, null);
            if (c.moveToFirst()) {
                config = new FeedWidgetConfig(c);
            }
        } finally {
            if (c != null) c.close();
        }

        if (config != null) {
            log.fine("Feed widget config loaded, setting editor values: " + feedWidgetConfigId);
            setValues(getPreferenceScreen(), config);
        } else {
            log.fine("Starting fresh, no feed widget config found in database for id: " + feedWidgetConfigId);
            setValues(getPreferenceScreen(), getDefaultValues());
            log.fine("Storing new feed widget config immediately: " + feedWidgetConfigId);
            storeFeedWidgetConfig(preferences, false);
        }
    }

    protected void storeFeedWidgetConfig(SharedPreferences preferences, boolean isUpdate) {
        ContentValues values = getValues(preferences).INSTANCE.getEntityValues();
        if (isUpdate) {
            log.fine("Edited existing config, updating database: " + feedWidgetConfigId);
            int count = getContentResolver().update(
               Uri.withAppendedPath(FeedWidgetConfig.CONTENT_URI, Long.toString(feedWidgetConfigId)),
               values, null, null
            );
            log.fine("After update, affected rows: " + count);
        } else {
            log.fine("Inserting new feed widget config into database...");
            getContentResolver().insert(FeedWidgetConfig.CONTENT_URI, values);
            log.fine("After insertion, have feed config: " + feedWidgetConfigId);
        }
        applyConfigOnWidget();
    }

    protected FeedWidgetConfig getValues(SharedPreferences preferences) {
        FeedWidgetConfig config;
        config = new FeedWidgetConfig(
           feedWidgetConfigId,
           preferences.getBoolean(KEY_TOOLBAR, false),
           preferences.getInt(KEY_BACKGROUND_COLOR, FeedWidgetConfig.DEFAULT_BACKGROUND_COLOR),
           FeedWidgetConfig.TextSize.valueOf(preferences.getString(KEY_TEXT_SIZE, FeedWidgetConfig.DEFAULT_TEXT_SIZE.name()))
        );
        log.fine("Got values: " + config);
        return config;
    }

    protected FeedWidgetConfig getDefaultValues() {
        return new FeedWidgetConfig(
           feedWidgetConfigId,
           FeedWidgetConfig.DEFAULT_TOOLBAR,
           FeedWidgetConfig.DEFAULT_BACKGROUND_COLOR,
           FeedWidgetConfig.DEFAULT_TEXT_SIZE
        );
    }

    protected void setValues(PreferenceScreen screen, FeedWidgetConfig config) {
        log.fine("Setting values: " + config);

        // TODO: Don't forget, this shit doesn't write through to the UI and you can't do anything about it!
        SharedPreferences.Editor editor = screen.getEditor();

        editor.putBoolean(KEY_TOOLBAR, config.getValue(FeedWidgetConfig.TOOLBAR));
        ((CheckBoxPreference) screen.findPreference(KEY_TOOLBAR)).setChecked(config.getValue(FeedWidgetConfig.TOOLBAR));

        editor.putInt(KEY_BACKGROUND_COLOR, config.getValue(FeedWidgetConfig.BACKGROUND_COLOR));

        editor.putString(KEY_TEXT_SIZE, config.getValueAsString(FeedWidgetConfig.TEXT_SIZE));
        ((ListPreference) screen.findPreference(KEY_TEXT_SIZE)).setValue(config.getValueAsString(FeedWidgetConfig.TEXT_SIZE));


        editor.apply();
    }

    protected void applyConfigOnWidget() {
        log.fine("Sending update broadcast for widget: " + feedWidgetConfigId);
        int[] widgetIds = new int[]{new Long(feedWidgetConfigId).intValue()};
        Intent update = new Intent();
        update.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds);
        update.setAction(FeedWidgetProvider.ACTION_FEEDWIDGET_UPDATE);
        sendBroadcast(update);
    }

    /* ##################################################### */

    // 200 lines of code for a simple multi-select list in a dialog... unbelievable
    // TODO http://code.google.com/p/android/issues/detail?id=2998

    class FeedOption implements CharSequence {
        long feedConfigId;
        String label;

        FeedOption(long feedConfigId, String label) {
            this.feedConfigId = feedConfigId;
            this.label = label;
        }

        @Override
        public int length() {
            return label.length();
        }

        @Override
        public char charAt(int index) {
            return label.charAt(index);
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return label.subSequence(start, end);
        }

        @Override
        public String toString() {
            return label;
        }
    }

    FeedOption[] feedSelectionOptions;
    boolean[] feedSelection;

    protected void selectAllFeedsAndStore() {
        queryFeedSelectionOptions();
        if (feedSelectionOptions != null) {
            log.fine("Selecting all feeds and storing immediately");
            for (int i = 0; i < feedSelection.length; i++) {
                feedSelection[i] = true;
            }
            storeFeedSelection();
        } else {
            log.fine("No feeds to select and store");
        }
    }

    protected void showFeedSelectionDialog() {

        queryFeedSelectionOptions();
        if (feedSelectionOptions == null) {
            log.fine("No feed selection options, starting feed preference activity");
            Intent intent = new Intent(this, FeedConfigPreferenceActivity.class);
            startActivityForResult(intent, REQUEST_CREATE_FEED);
            return;
        }

        // Rule 1 for Google interns: Android framework has to cause as much pain to developers as possible!
        final AlertDialog dialog = new AlertDialog.Builder(this)
           .setTitle(R.string.select_feeds)
           .setMultiChoiceItems(feedSelectionOptions, feedSelection, new FeedSelectionMultiChoiceListener())
           .create();
        // This way the button clicks don't dismiss the dialog
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface d) {
                Button b = dialog.getButton(AlertDialog.BUTTON2);
                b.setOnClickListener(new FeedSelectionDoneListener(dialog));
                b = dialog.getButton(AlertDialog.BUTTON3);
                b.setOnClickListener(new FeedSelectionSelectListener(dialog.getListView(), false));
                b = dialog.getButton(AlertDialog.BUTTON1);
                b.setOnClickListener(new FeedSelectionSelectListener(dialog.getListView(), true));
            }
        });
        // Yes, this really is the order of buttons from left to right. Good with numbers, eh Google interns?
        dialog.setButton(DialogInterface.BUTTON2, getString(R.string.done), new EmptyClickListener());
        dialog.setButton(DialogInterface.BUTTON3, getString(R.string.none), new EmptyClickListener());
        dialog.setButton(DialogInterface.BUTTON1, getString(R.string.all), new EmptyClickListener());
        dialog.show();
    }

    protected void queryFeedSelectionOptions() {
        Cursor c = null;
        try {
            Uri uri = Uri.withAppendedPath(FeedWidgetFeed.CONTENT_URI, Long.toString(feedWidgetConfigId));
            c = getContentResolver().query(uri, FeedWidgetFeed.PROJECTION_FEED_SELECTION, null, null, null);
            if (c.getCount() == 0) {
                feedSelectionOptions = null;
                feedSelection = null;
                return;
            }
            feedSelectionOptions = new FeedOption[c.getCount()];
            feedSelection = new boolean[c.getCount()];
            int i = 0;
            while (c.moveToNext()) {
                feedSelectionOptions[i] = new FeedOption(
                   c.getLong(c.getColumnIndex(BaseColumns._ID)),
                   c.getString(c.getColumnIndex(FeedWidgetFeed.PROJECTION_FEED_SELECTION[0]))
                );
                feedSelection[i] =
                   c.getInt(c.getColumnIndex(FeedWidgetFeed.PROJECTION_FEED_SELECTION[1])) == 1;
                i++;
            }
        } finally {
            if (c != null) c.close();
        }
    }

    protected void storeFeedSelection() {
        // Delete all then insert rows for selected only
        Uri uri = Uri.withAppendedPath(FeedWidgetFeed.CONTENT_URI, Long.toString(feedWidgetConfigId));
        getContentResolver().delete(uri, null, null);
        for (int i = 0; i < feedSelection.length; i++) {
            if (feedSelection[i]) {
                FeedWidgetFeed fwf =
                   new FeedWidgetFeed(null, feedWidgetConfigId, feedSelectionOptions[i].feedConfigId);
                log.fine("Storing feed selection: " + fwf);
                getContentResolver().insert(uri, fwf.INSTANCE.getEntityValues());
            }
        }
        applyConfigOnWidget();
    }

    protected void checkFeedSelection() {
        if (feedSelectionOptions != null && feedSelectionOptions.length > 0) {
            boolean isOneSelected = false;
            for (boolean b : feedSelection) {
                if (b) {
                    isOneSelected = true;
                    break;
                }
            }
            if (!isOneSelected)
                Toast.makeText(this, R.string.no_feed_selected, Toast.LENGTH_SHORT).show();
        }
    }

    class FeedSelectionDoneListener implements View.OnClickListener {

        AlertDialog dialog;

        FeedSelectionDoneListener(AlertDialog dialog) {
            this.dialog = dialog;
        }

        @Override
        public void onClick(View v) {
            log.fine("DONE clicked in feed selection dialog");
            dialog.dismiss();
            storeFeedSelection();
        }
    }

    class EmptyClickListener implements DialogInterface.OnClickListener {
        @Override
        public void onClick(DialogInterface dialog, int which) {
        }
    }

    class FeedSelectionSelectListener implements View.OnClickListener {

        ListView lv;
        boolean isChecked;

        FeedSelectionSelectListener(ListView lv, boolean checked) {
            this.lv = lv;
            isChecked = checked;
        }

        @Override
        public void onClick(View v) {
            for (int i = 0; i < lv.getCount(); i++) {
                lv.setItemChecked(i, isChecked);
                feedSelection[i] = isChecked;
            }
        }
    }

    class FeedSelectionMultiChoiceListener implements DialogInterface.OnMultiChoiceClickListener {
        @Override
        public void onClick(DialogInterface dialog, int which, boolean isChecked) {
            feedSelection[which] = isChecked;
        }
    }

    /* ##################################################### */

    protected void showBackgroundColorPickerDialog() {
        final SharedPreferences preferences = getPreferences();

        int color = preferences.getInt(KEY_BACKGROUND_COLOR, FeedWidgetConfig.DEFAULT_BACKGROUND_COLOR);

        final ColorPickerDialog dialog = new ColorPickerDialog(this, color, getString(R.string.pick_background_color));

        dialog.setAlphaSliderVisible(true);

        dialog.setButton(getString(R.string.done), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface d, int which) {
                SharedPreferences.Editor editor = preferences.edit();
                editor.putInt(KEY_BACKGROUND_COLOR, dialog.getColor());
                editor.commit();
            }
        });
        dialog.show();
    }

}
