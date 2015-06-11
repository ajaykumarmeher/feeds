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

package org.fourthline.android.feeds.refresh;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.preference.PreferenceManager;
import com.googlecode.sqb.query.DataType;
import com.sun.syndication.feed.atom.Content;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;
import org.fourthline.android.feeds.R;
import org.fourthline.android.feeds.FeedEntryListActivity;
import org.fourthline.android.feeds.content.Feed;
import org.fourthline.android.feeds.content.FeedConfig;
import org.fourthline.android.feeds.content.FeedEntry;
import org.seamless.util.Exceptions;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FeedRefreshService extends IntentService {

    final private static Logger log = Logger.getLogger(FeedRefreshService.class.getName());

    public static final String EXTRA_FORCE_REFRESH = "FORCE_REFRESH";
    public static final String EXTRA_FEEDCONFIG_IDS = "FEEDCONFIG_IDS";
    public static final int NOTIFICATION_WORK_IN_PROGRESS = 1;
    public static final int NOTIFICATION_NEW_ENTRIES = 2;

    // This is used to serialize feed refresh
    protected static final AtomicBoolean IN_PROGRESS = new AtomicBoolean(false);

    public FeedRefreshService() {
        super(FeedRefreshService.class.getSimpleName());
    }

    public class FeedRefresh {
        long id;
        String url;
        long lastRefresh;
        String lastRefreshEtag;
        int maxAgeDays;
        boolean notifyNew;
        RefreshAction action;
    }

    public class FeedEntryRefresh {
        FeedEntry entry;
        RefreshAction action;
    }

    enum RefreshAction {
        INSERT,
        UPDATE,
        DELETE
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (IN_PROGRESS.get()) {
            log.fine("Refresh already in progress, ignoring intent");
            return;
        }
        IN_PROGRESS.set(true);

        try {
            log.fine("On handle intent, finding feed configs that require refresh now");

            Object service = getSystemService(Context.CONNECTIVITY_SERVICE);
            if (service == null) {
                log.fine("No connectivity service available, skipping refresh checking...");
                return;
            }

            if (service instanceof ConnectivityManager) {
                ConnectivityManager cm = (ConnectivityManager) service;
                try {
                    if (cm.getActiveNetworkInfo() == null || !cm.getActiveNetworkInfo().isConnected()) {
                        log.fine("No network connection or background data connections disabled, skipping refresh checking...");
                        return;
                    }
                } catch (NullPointerException ex) {
                    log.warning("Caught and ignored NPE in android.NetworkInfo#isConnected(), you have a broken device.");
                    return;
                }
            } else {
                log.severe("Connectivity service is not a ConnectivityManager, your Android is buggy.");
                return;
            }

            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            if (preferences.getBoolean(getString(R.string.key_feedreader_mastersync), true)) {
                if (!ContentResolver.getMasterSyncAutomatically()) {
                    log.fine("Master account sync setting is disabled, skipping refresh checking...");
                    return;
                }
            }

            boolean forceRefresh = false;
            if (intent.getExtras() != null && intent.getExtras().getBoolean(EXTRA_FORCE_REFRESH)) {
                log.fine("Forcing refresh for feed configs");
                forceRefresh = true;
            }

            if (!preferences.getBoolean(getString(R.string.key_feedreader_sync), true) && !forceRefresh) {
                log.fine("Feed updates disabled, and refresh not forced, skipping refresh checking...");
                return;
            }

            List<Long> feedConfigIds = new ArrayList<Long>();
            if (intent.getExtras() != null && intent.getExtras().containsKey(EXTRA_FEEDCONFIG_IDS)) {
                long[] ids = intent.getExtras().getLongArray(EXTRA_FEEDCONFIG_IDS);
                log.fine("Only refresh feed configs: " + Arrays.toString(ids));
                for (long id : ids) {
                    feedConfigIds.add(id);
                }
            }

            refreshFeeds(forceRefresh, feedConfigIds);

        } finally {
            IN_PROGRESS.set(false);
        }
    }

    protected void refreshFeeds(boolean forceRefresh, List<Long> feedConfigIds) {

        List<FeedRefresh> feedRefreshList = new ArrayList<FeedRefresh>();
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(
               FeedConfig.CONTENT_URI, null, null, null, null
            );
            while (cursor.moveToNext()) {
                long feedConfigId = DataType.read(cursor, FeedConfig._ID);

                // Skip feed configs we don't want to refresh
                if (feedConfigIds.size() > 0) {
                    if (!feedConfigIds.contains(feedConfigId)) {
                        log.fine("Skipping feed config: " + feedConfigId);
                        continue;
                    }
                }

                if (forceRefresh || isFeedRequiringRefresh(cursor)) {
                    FeedRefresh feedRefresh = new FeedRefresh();
                    feedRefresh.id = feedConfigId;
                    feedRefresh.lastRefresh = DataType.read(cursor, FeedConfig.LAST_REFRESH, FeedConfig.ALIAS_PREFIX);
                    feedRefresh.lastRefreshEtag = DataType.read(cursor, FeedConfig.LAST_REFRESH_ETAG, FeedConfig.ALIAS_PREFIX);
                    feedRefresh.maxAgeDays = DataType.read(cursor, FeedConfig.MAX_AGE_DAYS, FeedConfig.ALIAS_PREFIX);
                    feedRefresh.url = DataType.read(cursor, FeedConfig.URL, FeedConfig.ALIAS_PREFIX);
                    feedRefresh.notifyNew = DataType.read(cursor, FeedConfig.NOTIFY_NEW, FeedConfig.ALIAS_PREFIX);

                    // Outer joined feed ID
                    Long feedId = DataType.read(cursor, Feed._ID, Feed.ALIAS_PREFIX);
                    feedRefresh.action = feedId != null ? RefreshAction.UPDATE : RefreshAction.INSERT;

                    log.fine("Feed config requires refresh: " + feedRefresh.url);
                    feedRefreshList.add(feedRefresh);
                }
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }

        if (feedRefreshList.isEmpty()) {
            log.fine("No feed configs require refresh now");
        } else {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

            stopNotifyNewEntries();

            if (preferences.getBoolean(getString(R.string.key_feedreader_refresh_status), false))
                startNotifyWorkInProgress();

            boolean haveNewEntries = false;

            // All entries discovered now share the same creation time, nicer sorting later
            // when we mix entries from different feeds in the same list
            long currentTime = System.currentTimeMillis();

            try {
                for (FeedRefresh refresh : feedRefreshList) {
                    boolean feedHasNewEntries = refreshFeed(refresh, currentTime);
                    if (!haveNewEntries && feedHasNewEntries && refresh.notifyNew)
                        haveNewEntries = true;
                }
            } finally {
                stopNotifyWorkInProgress();
            }

            if (haveNewEntries && preferences.getBoolean(getString(R.string.key_feedreader_notification), true)) {
                startNotifyNewEntries();
            }
        }
    }

    protected boolean refreshFeed(FeedRefresh feedRefresh, long currentTime) {

        boolean haveNewEntries = false;
        String etag = FeedConfig.DEFAULT_LAST_REFRESH_ETAG;

        try {
            if (!isFeedDataStale(feedRefresh.url, feedRefresh.lastRefresh, feedRefresh.lastRefreshEtag)) {
                log.fine("Feed data is not stale, skipping refresh: " + feedRefresh.url);
                return haveNewEntries;
            }
            log.fine("Feed data is stale, fetching with GET: " + feedRefresh.url);
            URL feedSource = new URL(feedRefresh.url);
            URLConnection urlConnection = feedSource.openConnection();
            urlConnection.setConnectTimeout(10000);
            urlConnection.setReadTimeout(10000);

            // This connects to the server!
            etag = urlConnection.getHeaderField("ETag");
            if (etag == null)
                etag = FeedConfig.DEFAULT_LAST_REFRESH_ETAG;

            SyndFeedInput input = new SyndFeedInput();
            SyndFeed syndFeed = input.build(new XmlReader(urlConnection));
            log.fine("Got feed data, converting...");

            if (syndFeed.getLink() == null) {
                log.warning("Feed doesn't even have a link, skipping: " + syndFeed);
                return haveNewEntries;
            }

            Feed feed = createFeed(feedRefresh.id, syndFeed);
            log.fine("Have feed: " + feed.getValue(Feed.LINK));

            if (feedRefresh.action.equals(RefreshAction.UPDATE)) {
                log.fine("Updating existing feed in database with values: " + feed.INSTANCE.getEntityValues());

                // Skip title, only inserted
                // TODO: Title editing
                // feed.INSTANCE.getEntityValues().remove(Feed.TITLE.getName());

                getContentResolver().update(
                   Uri.withAppendedPath(Feed.CONTENT_URI, Long.toString(feedRefresh.id)),
                   feed.INSTANCE.getEntityValues(), null, null
                );
            } else if (feedRefresh.action.equals(RefreshAction.INSERT)) {
                log.fine("Inserting new feed into database for feed config: " + feedRefresh.id);
                getContentResolver().insert(
                   Feed.CONTENT_URI, feed.INSTANCE.getEntityValues()
                );
            }

            List<FeedEntry> existingEntries = getExistingFeedEntries(feedRefresh.id);
            log.fine("Existing feed entries: " + existingEntries.size());

            List<FeedEntry> newFeedEntries = new ArrayList<FeedEntry>();
            for (Object o : syndFeed.getEntries()) {
                SyndEntry syndEntry = (SyndEntry) o;
                if (syndEntry.getLink() == null) {
                    log.warning("Feed entry doesn't even have a link, skipping: " + syndEntry);
                    continue;
                }
                FeedEntry fe = createFeedEntry(feedRefresh.id, syndEntry, currentTime);
                newFeedEntries.add(fe);
            }
            log.fine("New feed entries: " + newFeedEntries.size());

            haveNewEntries = refreshFeedEntries(feedRefresh, existingEntries, newFeedEntries);

            log.fine("Completed feed config refresh: " + feedRefresh.url);

        } catch (SocketTimeoutException timeoutEx) {
            log.warning("Timeout connecting to feed: " + feedRefresh.url + ", " + timeoutEx);
        } catch (MalformedURLException urlEx) {
            log.warning("URL is not valid: " + feedRefresh.url + ", " + urlEx);
        } catch (IOException ioEx) {
            log.warning("Could not connect to feed: " + feedRefresh.url + ", " + ioEx);
        } catch (FeedException fex) {
            log.warning("Could not marshall feed data: " + feedRefresh.url + ", " + fex);
            log.log(Level.WARNING, "Cause: " + Exceptions.unwrap(fex));
        } catch (Exception ex) {
            log.log(Level.WARNING, "Error refreshing feed: " + feedRefresh.url + " - " + ex, ex);
        }

        log.fine("Setting last refresh timestamp/etag of feed config: " + feedRefresh.id);
        FeedConfig updatedFeedConfig = new FeedConfig(feedRefresh.id, System.currentTimeMillis(), etag);
        getContentResolver().update(
           Uri.withAppendedPath(FeedConfig.CONTENT_URI, Long.toString(feedRefresh.id)),
           updatedFeedConfig.INSTANCE.getEntityValues(),
           null, null
        );

        return haveNewEntries;
    }

    protected boolean refreshFeedEntries(FeedRefresh feedRefresh, List<FeedEntry> existingEntries, List<FeedEntry> newFeedEntries) {
        List<FeedEntryRefresh> feedEntryRefreshList = new ArrayList<FeedEntryRefresh>();

        boolean haveNewEntries = false;

        Iterator<FeedEntry> it = existingEntries.iterator();
        while (it.hasNext()) {
            FeedEntry fe = it.next();
            // Remove expired entries but not if they are in the current feed
            if (fe.isExpired(feedRefresh.maxAgeDays)) {
                String existingLink = fe.getValue(FeedEntry.LINK);

                boolean isStillPresent = false;
                for (FeedEntry newEntry : newFeedEntries) {
                    String newLink = newEntry.getValue(FeedEntry.LINK);
                    if (newLink.equals(existingLink)) {
                        isStillPresent = true;
                        break;
                    }
                }
                if (!isStillPresent) {
                    log.fine("Existing feed entry is expired, deleting: " + fe.getId());
                    FeedEntryRefresh feRefresh = new FeedEntryRefresh();
                    feRefresh.entry = fe;
                    feRefresh.action = RefreshAction.DELETE;
                    feedEntryRefreshList.add(feRefresh);
                    it.remove();
                }
            }
        }

        for (FeedEntry fe : newFeedEntries) {
            boolean haveExisting = false;

            // We match entries by comparing their link, assuming this is the best key in RSS/atom
            String newLink = fe.getValue(FeedEntry.LINK);
            for (FeedEntry existingEntry : existingEntries) {
                String existingLink = existingEntry.getValue(FeedEntry.LINK);

                if (existingLink.equals(newLink)) {
                    long existingUpdatedDate = existingEntry.getValue(FeedEntry.UPDATED_DATE);
                    long newUpdatedDate = fe.getValue(FeedEntry.UPDATED_DATE);
                    if (newUpdatedDate > existingUpdatedDate) {
                        log.fine("New feed data has newer update timestamp, updating existing data");
                        FeedEntryRefresh feRefresh = new FeedEntryRefresh();
                        fe.INSTANCE.getEntityValues().put(FeedEntry._ID.getName(), existingEntry.getId());

                        // Keep the old polled and published dates, so we don't change the display order if an
                        // entry was updated!
                        fe.INSTANCE.getEntityValues().put(
                            FeedEntry.POLLED_DATE.getName(),
                            existingEntry.getValue(FeedEntry.POLLED_DATE)
                        );
                        fe.INSTANCE.getEntityValues().put(
                            FeedEntry.PUBLISHED_DATE.getName(),
                            existingEntry.getValue(FeedEntry.PUBLISHED_DATE)
                        );

                        feRefresh.entry = fe;
                        feRefresh.action = RefreshAction.UPDATE;
                        feedEntryRefreshList.add(feRefresh);
                    }
                    haveExisting = true;
                    break;
                }
            }

            if (!haveExisting) {
                log.fine("Feed entry is new, inserting data: " + newLink);
                haveNewEntries = true;
                FeedEntryRefresh feRefresh = new FeedEntryRefresh();
                feRefresh.entry = fe;
                feRefresh.action = RefreshAction.INSERT;
                feedEntryRefreshList.add(feRefresh);
            }
        }

        log.fine("Feed entry refresh items: " + feedEntryRefreshList.size());
        for (FeedEntryRefresh feedEntryRefresh : feedEntryRefreshList) {
            refreshFeedEntry(feedEntryRefresh);
        }

        return haveNewEntries;
    }

    protected void refreshFeedEntry(FeedEntryRefresh feedEntryRefresh) {
        if (feedEntryRefresh.action.equals(RefreshAction.DELETE)) {
            // TODO: Bulk delete this!
            log.fine("Deleting expired feed entry in database: " + feedEntryRefresh.entry.getId());
            getContentResolver().delete(
               Uri.withAppendedPath(FeedEntry.CONTENT_URI, Long.toString(feedEntryRefresh.entry.getId())),
               null, null
            );
        } else if (feedEntryRefresh.action.equals(RefreshAction.UPDATE)) {
            log.fine("Updating existing feed entry in database: " + feedEntryRefresh.entry.getId());
            getContentResolver().update(
               Uri.withAppendedPath(FeedEntry.CONTENT_URI, Long.toString(feedEntryRefresh.entry.getId())),
               feedEntryRefresh.entry.INSTANCE.getEntityValues(),
               null, null
            );
        } else if (feedEntryRefresh.action.equals(RefreshAction.INSERT)) {
            log.fine("Inserting new feed entry into database");
            getContentResolver().insert(
               FeedEntry.CONTENT_URI, feedEntryRefresh.entry.INSTANCE.getEntityValues()
            );
        }
    }

    protected boolean isFeedRequiringRefresh(Cursor c) {
        long lastRefresh = DataType.read(c, FeedConfig.LAST_REFRESH, FeedConfig.ALIAS_PREFIX);
        if (lastRefresh == FeedConfig.DEFAULT_LAST_REFRESH) {
            return true; // Has never been refreshed
        }
        long dueTime = lastRefresh + DataType.read(c, FeedConfig.REFRESH_INTERVAL, FeedConfig.ALIAS_PREFIX);
        return dueTime <= System.currentTimeMillis();
    }

    protected Feed createFeed(long id, SyndFeed syndFeed) {
        // TODO: author
        return new Feed(
           id,
           0,
           syndFeed.getLink(),
           syndFeed.getTitle() != null ? syndFeed.getTitle() : Feed.DEFAULT_TITLE,
           syndFeed.getDescription() != null ? syndFeed.getDescription() : Feed.DEFAULT_DESCRIPTION,
           syndFeed.getPublishedDate() != null ? syndFeed.getPublishedDate().getTime() : Feed.DEFAULT_DATE
        );
    }

    protected FeedEntry createFeedEntry(long id, SyndEntry syndEntry, long currentTime) {

        // We can never be sure what the Rome crap is doing internally... it is definitely not
        // masking Atom types with MIME types, so we need to check for that and correct.
        String descriptionType = FeedEntry.DEFAULT_DESCRIPTION_TYPE;
        if (syndEntry.getDescription() != null && syndEntry.getDescription().getType() != null) {
            descriptionType = syndEntry.getDescription().getType();
        }
        if (descriptionType.equals(Content.HTML)) {
            descriptionType = "text/html";
        } else if (descriptionType.equals(Content.TEXT)) {
            descriptionType = "text/plain";
        } else if (descriptionType.equals(Content.XHTML)) {
            descriptionType = "application/xhtml+xml";
        }

        return new FeedEntry(
           null,
           id,
           syndEntry.getLink(),
           syndEntry.getTitle() != null && syndEntry.getTitle().length() > 0 ? syndEntry.getTitle() : FeedEntry.DEFAULT_TITLE,
           syndEntry.getAuthor() != null && syndEntry.getAuthor().length() > 0 ? syndEntry.getAuthor() : FeedEntry.DEFAULT_AUTHOR,
           currentTime,
           syndEntry.getPublishedDate() != null ? syndEntry.getPublishedDate().getTime() : FeedEntry.DEFAULT_DATE,
           syndEntry.getUpdatedDate() != null ? syndEntry.getUpdatedDate().getTime() : FeedEntry.DEFAULT_DATE,
           descriptionType,
           syndEntry.getDescription() != null && syndEntry.getDescription().getValue() != null ? syndEntry.getDescription().getValue() : FeedEntry.DEFAULT_DESCRIPTION_VALUE,
           false
        );
    }

    protected List<FeedEntry> getExistingFeedEntries(long feedConfigId) {
        List<FeedEntry> existingEntries = new ArrayList<FeedEntry>();
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(
               Uri.withAppendedPath(FeedEntry.CONTENT_URI_FOR_FEEDCONFIG, Long.toString(feedConfigId)),
               null, null, null, null
            );
            while (cursor.moveToNext()) {
                existingEntries.add(new FeedEntry(cursor));
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return existingEntries;
    }

    protected void startNotifyWorkInProgress() {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        Notification notification = new Notification(
           R.drawable.notify_feed_work, getString(R.string.refreshing_feeds), 0);

        notification.flags |= Notification.FLAG_AUTO_CANCEL;

        notification.setLatestEventInfo(
           this,
           getString(R.string.refreshing_feeds),
           getString(R.string.refreshing_feeds_summary),
           null
        );

        manager.notify(NOTIFICATION_WORK_IN_PROGRESS, notification);
    }

    protected void stopNotifyWorkInProgress() {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(NOTIFICATION_WORK_IN_PROGRESS);
    }

    protected void startNotifyNewEntries() {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        Notification notification = new Notification(
           R.drawable.notify_feed, getString(R.string.new_feed_entries), System.currentTimeMillis());

        notification.flags |= Notification.FLAG_AUTO_CANCEL;

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (!preferences.getBoolean(getString(R.string.key_feedreader_notification_silent), true)) {
            notification.defaults |= Notification.DEFAULT_ALL;
        }

        Intent intent = new Intent(this, FeedEntryListActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        notification.setLatestEventInfo(
           this,
           getString(R.string.new_feed_entries),
           getString(R.string.new_feed_entries_summary),
           pendingIntent
        );

        manager.notify(NOTIFICATION_NEW_ENTRIES, notification);
    }

    protected void stopNotifyNewEntries() {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(NOTIFICATION_NEW_ENTRIES);
    }

    protected boolean isFeedDataStale(String url, long lastRefresh, String lastRefreshEtag) {
        log.fine("Checking feed data staleness with HEAD request: " + url);
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestMethod("HEAD");

             // TODO: Android bug http://code.google.com/p/android/issues/detail?id=24672
            connection.setRequestProperty("Accept-Encoding", "");

            int responseCode = connection.getResponseCode();

            if (responseCode != 200) {
                log.fine("HEAD request for staleness check of '" + url + "' failed: " + responseCode);
                return true;
            }
            long lastModified = connection.getLastModified();
            String etag = connection.getHeaderField("ETag");
            if (lastModified > 0) {
                // Date based validation
                return lastModified > lastRefresh;
            } else if (etag != null) {
                // Content based validation with ETag
                return !etag.equals(lastRefreshEtag);
            }
            return true;
        } catch (Exception ex) {
            log.log(Level.FINE, "Error checking feed staleness of: " + url, ex);
            return true;
        }
    }

}
