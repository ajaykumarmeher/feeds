/*
 * Copyright (C) 2011 4th Line GmbH, Switzerland
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

import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.app.NotificationManager;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import com.googlecode.sqb.query.DataType;
import org.fourthline.android.feeds.content.FeedConfig;
import org.fourthline.android.feeds.content.FeedEntry;
import org.fourthline.android.feeds.filechooser.FileChooserActivity;
import org.fourthline.android.feeds.imexport.Exporter;
import org.fourthline.android.feeds.imexport.Importer;
import org.fourthline.android.feeds.model.FeedEntryDetail;
import org.fourthline.android.feeds.model.FeedEntryDetails;
import org.fourthline.android.feeds.model.FeedReader;
import org.fourthline.android.feeds.refresh.FeedRefreshService;
import org.fourthline.android.feeds.refresh.OnStartupReceiver;
import org.fourthline.android.feeds.widget.OkCancelDialog;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class FeedEntryListActivity extends AbstractFeedReaderActivity implements
   OnFeedConfigSelectedListener,
   OnFeedEntrySelectionListener {

    final private static Logger log = Logger.getLogger(FeedEntryListActivity.class.getName());

    protected final static int REQUEST_FEEDCONFIG_PREFERENCES = 0;
    protected final static int REQUEST_IMPORT = 1;
    protected final static int REQUEST_EXPORT = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        log.fine("On create");
        setContentView(R.layout.feedreader_layout);
        getActionBar().setDisplayShowTitleEnabled(false);

        if (savedInstanceState == null) {
            log.fine("Sending startup broadcast action: " + OnStartupReceiver.ACTION_STARTUP);
            Intent startupIntent = new Intent();
            startupIntent.setAction(OnStartupReceiver.ACTION_STARTUP);
            sendBroadcast(startupIntent);
        }

        setCurrentState(savedInstanceState);
    }

    @Override
    protected void setCurrentState(Bundle savedInstanceState) {
        super.setCurrentState(savedInstanceState);
        showFeedEntryList();
        showSidebar();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.feed_entry_list_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.getItem(0).setVisible(feedReader.feedId != -1);
        menu.getItem(1).setVisible(feedReader.haveEntries);
        menu.getItem(3).getSubMenu().getItem(0).setVisible(!isSidebarViewVisible());
        menu.getItem(3).getSubMenu().getItem(2).setVisible(feedReader.feedId != -1);
        menu.getItem(3).getSubMenu().getItem(3).setVisible(feedReader.feedId != -1);
        menu.getItem(3).getSubMenu().getItem(4).setVisible(feedReader.feedId != -1);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case R.id.menu_show_all:
                showAllFeedConfigs();
                return true;
            case R.id.menu_mark_read:
                markRead();
                return true;
            case R.id.menu_refresh:
                startFeedRefreshService(feedReader.feedId);
                return true;
            case R.id.menu_feed_select:
                showFeedList();
                return true;
            case R.id.menu_feed_add:
                addFeedConfig();
                return true;
            case R.id.menu_feed_edit:
                editFeedConfig();
                return true;
            case R.id.menu_feed_delete:
                deleteFeedConfig();
                return true;
            case R.id.menu_feed_copy:
                copyFeedConfigUrl();
                return true;
            case R.id.menu_import:
                intent = new Intent(this, FileChooserActivity.class);
                startActivityForResult(intent, REQUEST_IMPORT);
                return true;
            case R.id.menu_export:
                intent = new Intent(this, FileChooserActivity.class);
                intent.putExtra(FileChooserActivity.EXTRA_SELECT_DIRECTORY, true);
                startActivityForResult(intent, REQUEST_EXPORT);
                return true;
            case R.id.menu_settings:
                intent = new Intent(this, FeedsPreferenceActivity.class);
                startActivity(intent);
                return true;
            case R.id.menu_about:
                Toast.makeText(
                    this,
                    "Feeds is Free Software. " +
                        "See https://github.com/4thline/feeds for more information.",
                    Toast.LENGTH_LONG
                ).show();
                return true;

        }
        return false;
    }

    @Override
    public void onFeedEntriesLoaded(boolean haveEntries) {
        log.fine("On feed entries loaded, having entries: " + haveEntries);
        feedReader.onFeedEntriesLoaded(haveEntries);
        invalidateOptionsMenu();
    }

    @Override
    public void onFeedConfigSelected(int position, long id, String url) {
        log.fine("On feed config selected: " + position);
        feedReader.onFeedConfigSelected(position, id, url);
        showFeedEntryList();
        invalidateOptionsMenu();
    }

    @Override
    public void onFeedEntrySelected(int position, FeedEntryDetail[] feedEntryDetails) {
        log.fine("On feed entry selected, starting feed entry detail activity");
        feedReader.feedEntryDetails = new FeedEntryDetails(position, feedEntryDetails);
        Intent intent = new Intent(this, FeedEntryDetailsActivity.class);
        intent.putExtra(FeedReader.EXTRA_FEEDREADER, feedReader);
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_FEEDCONFIG_PREFERENCES:
                log.fine("Preference activity returned");
                if (resultCode == FeedConfigPreferenceActivity.RESULT_CONFIG_CHANGED) {
                    // Either the preference activity gives us the feed ID, or we use the currently selected feed
                    long refreshFeedId =
                        data.getExtras().getLong(FeedConfigPreferenceActivity.EXTRA_FEED_CONFIG_ID, feedReader.feedId);
                    log.fine("Preferences changed, triggering refresh of feed: " + refreshFeedId);
                    startFeedRefreshService(refreshFeedId);
                    // TODO: We should also select the edited/added feed but it's not that easy because
                    // of the mess with position state in FeedListFragment
                }
                break;
            case REQUEST_IMPORT:
                if (resultCode == RESULT_OK) {
                    final Uri uri = data.getData();
                    Importer importer = new Importer();
                    boolean feedsImported = importer.importOPML(this, uri);
                    if (feedsImported)
                        startFeedRefreshService(-1);
                }
                break;
            case REQUEST_EXPORT:
                if (resultCode == RESULT_OK) {
                    final Uri uri = data.getData();
                    Exporter exporter = new Exporter();
                    exporter.exportOPML(this, uri);
                }
                break;
        }
    }

    protected void showAllFeedConfigs() {
        log.fine("Showing all feed configs");
        feedReader.onFeedConfigDeselected();
        showSidebar();
        showFeedEntryList();
        invalidateOptionsMenu();
    }

    protected void showFeedEntryList() {
        getFragmentManager().popBackStack(); // If the feed list has been show before, pop it
        log.fine("Replacing main fragment with entry list: " + feedReader);
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace(R.id.main, new FeedEntryListFragment());
        ft.commit();
    }

    protected void showFeedList() {
        log.fine("Replacing main fragment with feed list: " + feedReader);
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace(R.id.main, new FeedListFragment());
        ft.addToBackStack(null);
        ft.commit();
    }

    protected void showSidebar() {
        if (isSidebarViewVisible()) {
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            log.fine("Replacing sidebar fragment with feed list: " + feedReader);
            ft.replace(R.id.sidebar, new FeedListFragment());
            ft.commit();
        }
    }

    protected boolean isSidebarViewVisible() {
        View sidebarView = findViewById(R.id.sidebar);
        return sidebarView != null && sidebarView.getVisibility() == View.VISIBLE;
    }

    protected void startFeedRefreshService(long feedId) {
        log.fine("Starting feed refresh service for feed: " + feedId);
        Intent intent = new Intent(this, FeedRefreshService.class);
        intent.putExtra(FeedRefreshService.EXTRA_FORCE_REFRESH, true);
        if (feedId != -1) {
            intent.putExtra(FeedRefreshService.EXTRA_FEEDCONFIG_IDS, new long[]{feedId});
        }
        startService(intent);
    }

    protected void markRead() {
        log.fine("Marking read feed config: " + feedReader.feedId);
        new AsyncTask<Long, Void, Long[]>() {
            @Override
            protected Long[] doInBackground(Long... params) {
                // Mark all feed entries as read, update database
                ContentValues cv = new ContentValues();
                DataType.write(cv, FeedEntry.IS_READ, true);
                getContentResolver().update(
                   FeedEntry.CONTENT_URI,
                   cv,
                   params[0] != -1 ? FeedEntry.FEED_ID.getName() : null,
                   params[0] != -1 ? new String[]{Long.toString(params[0])} : null
                );
                // Return the identifiers of all feed configs that were affected
                if (params[0] != -1) {
                    return new Long[]{params[0]};
                }
                // Unfortunately, we have to query here if we want all feed config ids
                Cursor c = null;
                try {
                    List<Long> ids = new ArrayList<Long>();
                    c = getContentResolver().query(FeedConfig.CONTENT_URI, null, null, null, null);
                    while (c.moveToNext()) {
                        ids.add(DataType.read(c, FeedConfig._ID));
                    }
                    return ids.toArray(new Long[ids.size()]);
                } finally {
                    if (c != null) c.close();
                }
            }

            @Override
            protected void onPostExecute(Long[] feedConfigIds) {
                invalidateOptionsMenu();
                for (Long feedConfigId : feedConfigIds) {
                    Uri uri = Uri.withAppendedPath(FeedConfig.CONTENT_URI, String.valueOf(feedConfigId));
                    getContentResolver().notifyChange(uri, null);
                }
                ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE))
                   .cancel(FeedRefreshService.NOTIFICATION_NEW_ENTRIES);
            }
        }.execute(feedReader.feedId);
    }

    protected void addFeedConfig() {
        log.fine("Add new feed config, starting preference activity");
        Intent intent = new Intent(this, FeedConfigPreferenceActivity.class);
        startActivityForResult(intent, REQUEST_FEEDCONFIG_PREFERENCES);
    }

    protected void editFeedConfig() {
        if (feedReader.feedId == -1)
            return;
        log.fine("Edit feed config, starting preference activity");
        Intent intent = new Intent(this, FeedConfigPreferenceActivity.class);
        intent.putExtra(
           FeedConfigPreferenceActivity.EXTRA_FEED_CONFIG_ID,
           feedReader.feedId
        );
        startActivityForResult(intent, REQUEST_FEEDCONFIG_PREFERENCES);
    }

    protected void deleteFeedConfig() {
        if (feedReader.feedId == -1)
            return;
        final AlertDialog deleteConfirmation =
           OkCancelDialog.newInstance(
              this,
              R.string.feed_delete_confirmation,
              R.string.dialog_ok, R.string.dialog_cancel,
              new DialogInterface.OnClickListener() {
                  @Override
                  public void onClick(DialogInterface dialogInterface, int i) {
                      log.fine("Deleting feed config: " + feedReader.feedId);
                      getContentResolver().delete(
                         Uri.withAppendedPath(FeedConfig.CONTENT_URI, Long.toString(feedReader.feedId)),
                         null, null
                      );
                      onFeedConfigSelected(-1, -1, null);
                  }
              }
           );
        deleteConfirmation.show();
    }

    protected void copyFeedConfigUrl() {
        if (feedReader.feedId != -1) {
            log.fine("Copying feed config URL to clipboard: " + feedReader.feedUrl);
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            clipboard.setText(feedReader.feedUrl);
            Toast.makeText(this, R.string.feed_url_copied_to_clipboard, Toast.LENGTH_SHORT).show();
        }
    }

}