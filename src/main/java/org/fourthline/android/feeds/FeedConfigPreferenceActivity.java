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

package org.fourthline.android.feeds;

import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.widget.Toast;
import org.fourthline.android.feeds.R;
import org.fourthline.android.feeds.content.FeedConfig;
import org.fourthline.android.feeds.widget.ColorPickerDialog;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;

/*
TODO: http://code.google.com/p/color-picker-view/
 */
public class FeedConfigPreferenceActivity extends PreferenceActivity
   implements SharedPreferences.OnSharedPreferenceChangeListener {

    final private static Logger log = Logger.getLogger(FeedConfigPreferenceActivity.class.getName());

    public static final String EXTRA_FEED_CONFIG_ID = "feed_config_id";
    public static final int RESULT_CONFIG_CHANGED = 1;

    protected String KEY_URL;
    protected String KEY_PREVIEW_LENGTH;
    protected String KEY_PREFIX;
    protected String KEY_REFRESH_INTERVAL;
    protected String KEY_NOTIFY_NEW;
    protected String KEY_MAX_AGE_DAYS;
    protected String KEY_TEXT_COLOR;

    protected long feedConfigId = -1;
    protected boolean isConfigChanged;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        KEY_URL = getString(R.string.key_feed_url);
        KEY_PREFIX = getString(R.string.key_feed_prefix);
        KEY_PREVIEW_LENGTH = getString(R.string.key_feed_preview_length);
        KEY_REFRESH_INTERVAL = getString(R.string.key_feed_refresh_interval);
        KEY_NOTIFY_NEW = getString(R.string.key_feed_notify_new);
        KEY_MAX_AGE_DAYS = getString(R.string.key_feed_max_age_days);
        KEY_TEXT_COLOR = getString(R.string.key_feed_text_color);

        if (getIntent().getExtras() != null)
            feedConfigId = getIntent().getExtras().getLong(EXTRA_FEED_CONFIG_ID, -1);
        log.fine("Working on feed config: " + feedConfigId);

        addPreferencesFromResource(R.xml.feedconfig_preferences);

        findPreference(KEY_TEXT_COLOR).setOnPreferenceClickListener(
           new Preference.OnPreferenceClickListener() {
               public boolean onPreferenceClick(Preference preference) {
                   showTextColorPickerDialog();
                   return true;
               }
           });

    }

    @Override
    protected void onResume() {
        super.onResume();
        log.fine("Resuming feed config activity");
        loadFeedConfig(getPreferences());
        getPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        log.fine("Pausing feed config activity");
        getPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
        log.fine("Feed config changed, triggered by new value of: " + key);
        storeFeedConfig(preferences);
    }

    @Override
    public void onBackPressed() {
        if (isConfigChanged) {
            if (feedConfigId != -1) {
                Intent data = new Intent();
                data.putExtra(EXTRA_FEED_CONFIG_ID, feedConfigId);
                setResult(RESULT_CONFIG_CHANGED, data);
            } else {
                setResult(RESULT_CONFIG_CHANGED);
            }
        }
        super.onBackPressed();
    }

    protected SharedPreferences getPreferences() {
        return getPreferenceScreen().getSharedPreferences();
    }

    protected void loadFeedConfig(SharedPreferences preferences) {
        if (feedConfigId != -1) {
            log.fine("Loading feed config: " + feedConfigId);
            Cursor c = null;
            try {
                Uri uri = Uri.withAppendedPath(FeedConfig.CONTENT_URI, Long.toString(feedConfigId));
                c = getContentResolver().query(uri, null, null, null, null);
                if (c.moveToFirst()) {
                    log.fine("Feed config loaded, setting editor values: " + feedConfigId);
                    setValues(getPreferenceScreen(), new FeedConfig(c));
                } else {
                    log.fine("Starting fresh, no feed config found in database for id: " + feedConfigId);
                    setValues(getPreferenceScreen(), getDefaultValues());
                }
            } finally {
                if (c != null) c.close();
            }
        } else {
            log.fine("Starting fresh with default values, new config");
            setValues(getPreferenceScreen(), getDefaultValues());
        }
    }

    protected void storeFeedConfig(SharedPreferences preferences) {
        if (!isValidURL(preferences)) {
            log.fine("No valid URL entered, not saving feed config in database: " + feedConfigId);
            return;
        }

        ContentValues values = getValues(preferences).INSTANCE.getEntityValues();
        if (feedConfigId != -1) {
            log.fine("Edited existing config, updating database: " + feedConfigId);
            int count = getContentResolver().update(
               Uri.withAppendedPath(FeedConfig.CONTENT_URI, Long.toString(feedConfigId)),
               values, null, null
            );
            log.fine("After update, affected rows: " + count);
        } else {
            log.fine("Inserting new feed config into database...");
            Uri uri = getContentResolver().insert(FeedConfig.CONTENT_URI, values);
            feedConfigId = Long.valueOf(uri.getLastPathSegment());
            log.fine("After insertion, have feed config: " + feedConfigId);
        }

        isConfigChanged = true;
    }

    protected boolean isValidURL(SharedPreferences preferences) {
        String url = preferences.getString(KEY_URL, "NULL");
        try {
            log.fine("User entered valid URL? " + url);
            new URL(url);
            return true;
        } catch (MalformedURLException ex) {
            Toast.makeText(this, R.string.msg_enter_valid_url, Toast.LENGTH_SHORT).show();
        }
        return false;
    }

    protected FeedConfig getValues(SharedPreferences preferences) {
        FeedConfig config;
        if (feedConfigId != -1) {
            config = new FeedConfig(
               preferences.getString(KEY_URL, ""),
               Integer.valueOf(preferences.getString(KEY_REFRESH_INTERVAL, Integer.toString(FeedConfig.DEFAULT_REFRESH_INTERVAL))),
               FeedConfig.PreviewLength.valueOf(preferences.getString(KEY_PREVIEW_LENGTH, FeedConfig.DEFAULT_PREVIEW_LENGTH.name())),
               FeedConfig.EntryPrefix.valueOf(preferences.getString(KEY_PREFIX, FeedConfig.DEFAULT_ENTRY_PREFIX.name())),
               preferences.getInt(KEY_TEXT_COLOR, FeedConfig.DEFAULT_TEXT_COLOR),
               Integer.valueOf(preferences.getString(KEY_MAX_AGE_DAYS, Integer.toString(FeedConfig.DEFAULT_MAX_AGE_DAYS))),
               preferences.getBoolean(KEY_NOTIFY_NEW, FeedConfig.DEFAULT_NOTIFY_NEW)
            );
        } else {
            config = new FeedConfig(
               null,
               preferences.getString(KEY_URL, ""),
               Integer.valueOf(preferences.getString(KEY_REFRESH_INTERVAL, Integer.toString(FeedConfig.DEFAULT_REFRESH_INTERVAL))),
               FeedConfig.PreviewLength.valueOf(preferences.getString(KEY_PREVIEW_LENGTH, FeedConfig.DEFAULT_PREVIEW_LENGTH.name())),
               FeedConfig.EntryPrefix.valueOf(preferences.getString(KEY_PREFIX, FeedConfig.EntryPrefix.NONE.name())),
               preferences.getInt(KEY_TEXT_COLOR, FeedConfig.DEFAULT_TEXT_COLOR),
               FeedConfig.DEFAULT_LAST_REFRESH,
                FeedConfig.DEFAULT_LAST_REFRESH_ETAG,
               Integer.valueOf(preferences.getString(KEY_MAX_AGE_DAYS, Integer.toString(FeedConfig.DEFAULT_MAX_AGE_DAYS))),
               preferences.getBoolean(KEY_NOTIFY_NEW, FeedConfig.DEFAULT_NOTIFY_NEW)
            );
        }
        log.fine("Got values: " + config);
        return config;
    }

    protected FeedConfig getDefaultValues() {
        return new FeedConfig(
           null,
           "http://",
           FeedConfig.DEFAULT_REFRESH_INTERVAL,
           FeedConfig.DEFAULT_PREVIEW_LENGTH,
           FeedConfig.DEFAULT_ENTRY_PREFIX,
           FeedConfig.DEFAULT_TEXT_COLOR,
           FeedConfig.DEFAULT_LAST_REFRESH,
            FeedConfig.DEFAULT_LAST_REFRESH_ETAG,
           FeedConfig.DEFAULT_MAX_AGE_DAYS,
           FeedConfig.DEFAULT_NOTIFY_NEW
        );
    }

    protected void setValues(PreferenceScreen screen, FeedConfig config) {
        log.fine("Setting values: " + config);

        // TODO: Don't forget, this shit doesn't write through to the UI and you can't do anything about it!
        SharedPreferences.Editor editor = screen.getEditor();

        editor.putString(KEY_URL, config.getValue(FeedConfig.URL));
        ((EditTextPreference) screen.findPreference(KEY_URL)).setText(config.getValue(FeedConfig.URL));

        editor.putString(KEY_REFRESH_INTERVAL, Long.toString(config.getValue(FeedConfig.REFRESH_INTERVAL)));
        ((ListPreference) screen.findPreference(KEY_REFRESH_INTERVAL)).setValue(Integer.toString(config.getValue(FeedConfig.REFRESH_INTERVAL)));

        editor.putBoolean(KEY_NOTIFY_NEW, config.getValue(FeedConfig.NOTIFY_NEW));
        ((CheckBoxPreference) screen.findPreference(KEY_NOTIFY_NEW)).setChecked(config.getValue(FeedConfig.NOTIFY_NEW));

        editor.putString(KEY_MAX_AGE_DAYS, Long.toString(config.getValue(FeedConfig.MAX_AGE_DAYS)));
        ((ListPreference) screen.findPreference(KEY_MAX_AGE_DAYS)).setValue(Integer.toString(config.getValue(FeedConfig.MAX_AGE_DAYS)));

        editor.putString(KEY_PREFIX, config.getValueAsString(FeedConfig.ENTRY_PREFIX));
        ((ListPreference) screen.findPreference(KEY_PREFIX)).setValue(config.getValueAsString(FeedConfig.ENTRY_PREFIX));

        editor.putString(KEY_PREVIEW_LENGTH, config.getValueAsString(FeedConfig.PREVIEW_LENGTH));
        ((ListPreference) screen.findPreference(KEY_PREVIEW_LENGTH)).setValue(config.getValueAsString(FeedConfig.PREVIEW_LENGTH));

        editor.putInt(KEY_TEXT_COLOR, config.getValue(FeedConfig.TEXT_COLOR));

        editor.apply();
    }

    protected void showTextColorPickerDialog() {
        final SharedPreferences preferences = getPreferences();

        int color = preferences.getInt(KEY_TEXT_COLOR, FeedConfig.DEFAULT_TEXT_COLOR);

        final ColorPickerDialog dialog =
           new ColorPickerDialog(this, color, getString(R.string.pick_text_color));

        dialog.setAlphaSliderVisible(true);

        dialog.setButton(getString(R.string.done), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface d, int which) {
                SharedPreferences.Editor editor = preferences.edit();
                editor.putInt(KEY_TEXT_COLOR, dialog.getColor());
                editor.commit();
            }
        });
        dialog.show();
    }

}
