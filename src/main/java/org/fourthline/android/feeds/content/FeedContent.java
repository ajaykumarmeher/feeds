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

package org.fourthline.android.feeds.content;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Intent;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import com.googlecode.sqb.query.Restrictions;
import com.googlecode.sqb.query.Table;
import com.googlecode.sqb.query.select.SelectQueryBuilder;
import com.googlecode.sqb.sql.JoinType;
import com.googlecode.sqb.sql.Order;
import com.googlecode.sqb.vendor.sql92.Sql92;
import org.fourthline.android.feeds.widget.FeedWidgetProvider;
import org.fourthline.android.feeds.database.PersistentEntity;
import org.fourthline.android.feeds.util.StringUtil;

import java.util.Arrays;
import java.util.logging.Logger;

public class FeedContent extends ContentProvider {

    final private static Logger log = Logger.getLogger(FeedContent.class.getName());

    public static final String AUTHORITY = FeedContent.class.getName().toLowerCase();

    protected static final int URI_FEEDCONFIGS = 1;
    protected static final int URI_FEEDCONFIG = 2;
    protected static final int URI_FEED = 3;
    protected static final int URI_FEEDS = 4;
    protected static final int URI_FEEDENTRIES = 5;
    protected static final int URI_FEEDENTRY = 6;
    protected static final int URI_FEEDENTRIES_FOR_FEEDCONFIG = 7;
    protected static final int URI_FEEDENTRY_FOR_LINK = 8;
    protected static final int URI_FEEDWIDGETS = 9;
    protected static final int URI_FEEDWIDGET = 10;
    protected static final int URI_FEEDWIDGET_FEED = 11;
    protected static final int URI_FEEDENTRIES_FOR_FEEDWIDGET = 12;
    protected static final int URI_FEEDCONFIGS_BY_URL = 13;

    protected static UriMatcher URI_MATCHER;

    static {
        FeedContent.URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
        FeedContent.URI_MATCHER.addURI(AUTHORITY, FeedConfig.ENTITY, URI_FEEDCONFIGS);
        FeedContent.URI_MATCHER.addURI(AUTHORITY, FeedConfig.ENTITY + "/#", URI_FEEDCONFIG);
        FeedContent.URI_MATCHER.addURI(AUTHORITY, Feed.ENTITY, URI_FEEDS);
        FeedContent.URI_MATCHER.addURI(AUTHORITY, Feed.ENTITY + "/#", URI_FEED);
        FeedContent.URI_MATCHER.addURI(AUTHORITY, FeedEntry.ENTITY, URI_FEEDENTRIES);
        FeedContent.URI_MATCHER.addURI(AUTHORITY, FeedEntry.ENTITY + "/#", URI_FEEDENTRY);
        FeedContent.URI_MATCHER.addURI(AUTHORITY, FeedEntry.ENTITY + "/" + FeedConfig.ENTITY + "/#", URI_FEEDENTRIES_FOR_FEEDCONFIG);
        FeedContent.URI_MATCHER.addURI(AUTHORITY, FeedEntry.ENTITY + "/" + FeedEntry.LINK.getName() + "/#", URI_FEEDENTRY_FOR_LINK);
        FeedContent.URI_MATCHER.addURI(AUTHORITY, FeedWidgetConfig.ENTITY, URI_FEEDWIDGETS);
        FeedContent.URI_MATCHER.addURI(AUTHORITY, FeedWidgetConfig.ENTITY + "/#", URI_FEEDWIDGET);
        FeedContent.URI_MATCHER.addURI(AUTHORITY, FeedWidgetFeed.ENTITY + "/#", URI_FEEDWIDGET_FEED);
        FeedContent.URI_MATCHER.addURI(AUTHORITY, FeedEntry.ENTITY + "/" + FeedWidgetConfig.ENTITY + "/#", URI_FEEDENTRIES_FOR_FEEDWIDGET);
        FeedContent.URI_MATCHER.addURI(AUTHORITY, FeedConfig.ENTITY + "/url/*", URI_FEEDCONFIGS_BY_URL);
    }

    protected FeedsDatabase database;

    @Override
    public boolean onCreate() {
        database = FeedsDatabase.getInstance(getContext());
        return true;
    }

    @Override
    public String getType(Uri uri) {
        log.fine("Get Type: " + uri);
        switch (URI_MATCHER.match(uri)) {
            case URI_FEEDCONFIGS:
            case URI_FEEDCONFIGS_BY_URL:
                return FeedConfig.MIME_DIR;
            case URI_FEEDCONFIG:
                return FeedConfig.MIME_ITEM;
            case URI_FEEDS:
                return Feed.MIME_DIR;
            case URI_FEED:
                return Feed.MIME_ITEM;
            case URI_FEEDENTRIES:
            case URI_FEEDENTRIES_FOR_FEEDCONFIG:
            case URI_FEEDENTRIES_FOR_FEEDWIDGET:
                return FeedEntry.MIME_DIR;
            case URI_FEEDENTRY:
            case URI_FEEDENTRY_FOR_LINK:
                return FeedEntry.MIME_ITEM;
            case URI_FEEDWIDGETS:
                return FeedWidgetConfig.MIME_DIR;
            case URI_FEEDWIDGET:
                return FeedWidgetConfig.MIME_ITEM;
            case URI_FEEDWIDGET_FEED:
                return FeedWidgetFeed.MIME_DIR;
        }
        throw new IllegalArgumentException("URI not supported by this ContentProvider: " + uri);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        log.fine("Querying URI: " + uri);

        if (URI_MATCHER.match(uri) == URI_FEEDWIDGET_FEED
           && Arrays.equals(FeedWidgetFeed.PROJECTION_FEED_SELECTION, projection)) {
            log.fine("TODO: Special query we can't do with the SQL builder..."); // TODO
            String sql =
               "select " +
                  "fc._ID as _id," +
                  "case when f.TITLE is null then fc.URL else f.TITLE end as " + FeedWidgetFeed.PROJECTION_FEED_SELECTION[0]+ "," +
                  "case when fwf._ID is null then 0 else 1 end as " + FeedWidgetFeed.PROJECTION_FEED_SELECTION[1] + " " +
                  "from FEEDCONFIG fc " +
                  "left outer join FEED f " +
                  "on fc._ID = f._ID " +
                  "left outer join FEEDWIDGETFEED fwf " +
                  "on fwf.FEED_CONFIG_ID = fc._ID and fwf.FEED_WIDGET_ID = ? " +
                  "order by fc._ID asc";
            selectionArgs = new String[]{uri.getLastPathSegment()};
            Cursor c = database.get().rawQuery(sql, selectionArgs);
            c.setNotificationUri(getContext().getContentResolver(), uri);
            return c;
        }

        SelectQueryBuilder query = new SelectQueryBuilder();

        switch (URI_MATCHER.match(uri)) {
            case URI_FEEDCONFIGS:
                query.select(FeedConfig.PROJECTION_WITH_FEED);
                query.setAliasPrefix(FeedConfig.COLUMNS, FeedConfig.ALIAS_PREFIX);
                query.setAliasPrefix(Feed.COLUMNS, Feed.ALIAS_PREFIX);
                query.setAlias(FeedConfig._ID, BaseColumns._ID); // Required for CursorAdapter
                query.from(FeedConfig.TABLE)
                   .join(Feed.TABLE, JoinType.LEFT, Restrictions.eq(FeedConfig._ID, Feed._ID));
                query.orderBy(FeedConfig._ID, Order.ASC);
                break;
            case URI_FEEDCONFIG:
            case URI_FEEDCONFIGS_BY_URL:
                query.select(FeedConfig.COLUMNS);
                query.setAliasPrefix(FeedConfig.COLUMNS, FeedConfig.ALIAS_PREFIX);
                query.setAlias(FeedConfig._ID, BaseColumns._ID); // Required for CursorAdapter
                query.from(FeedConfig.TABLE);
                break;
            case URI_FEEDENTRY:
            case URI_FEEDENTRIES:
            case URI_FEEDENTRIES_FOR_FEEDCONFIG:
            case URI_FEEDENTRY_FOR_LINK:
                query.select(FeedEntry.PROJECTION_WITH_FEED);
                query.setAliasPrefix(FeedEntry.COLUMNS, FeedEntry.ALIAS_PREFIX);
                query.setAliasPrefix(FeedConfig.COLUMNS, FeedConfig.ALIAS_PREFIX);
                query.setAliasPrefix(Feed.COLUMNS, Feed.ALIAS_PREFIX);
                query.setAlias(FeedEntry._ID, BaseColumns._ID); // Required for CursorAdapter
                query.from(Feed.TABLE)
                   .join(FeedEntry.TABLE, JoinType.INNER, Restrictions.eq(Feed._ID, FeedEntry.FEED_ID))
                   .join(FeedConfig.TABLE, JoinType.INNER, Restrictions.eq(Feed._ID, FeedConfig._ID));
                query.orderBy(FeedEntry.POLLED_DATE, Order.DESC);
                query.orderBy(FeedEntry.PUBLISHED_DATE, Order.DESC);
                query.orderBy(FeedEntry.UPDATED_DATE, Order.DESC);
                break;
            case URI_FEEDWIDGETS:
            case URI_FEEDWIDGET:
                query.select(FeedWidgetConfig.COLUMNS);
                query.setAliasPrefix(FeedWidgetConfig.COLUMNS, FeedWidgetConfig.ALIAS_PREFIX);
                query.setAlias(FeedWidgetConfig._ID, BaseColumns._ID); // Required for CursorAdapter
                query.from(FeedWidgetConfig.TABLE);
                break;
            case URI_FEEDENTRIES_FOR_FEEDWIDGET:
                query.select(FeedEntry.PROJECTION_WITH_FEED);
                query.setAliasPrefix(FeedEntry.COLUMNS, FeedEntry.ALIAS_PREFIX);
                query.setAliasPrefix(FeedConfig.COLUMNS, FeedConfig.ALIAS_PREFIX);
                query.setAliasPrefix(Feed.COLUMNS, Feed.ALIAS_PREFIX);
                query.setAlias(FeedEntry._ID, BaseColumns._ID); // Required for CursorAdapter
                query.from(Feed.TABLE)
                   .join(FeedEntry.TABLE, JoinType.INNER, Restrictions.eq(Feed._ID, FeedEntry.FEED_ID))
                   .join(FeedConfig.TABLE, JoinType.INNER, Restrictions.eq(Feed._ID, FeedConfig._ID))
                   .join(FeedWidgetFeed.TABLE, JoinType.INNER, Restrictions.eq(Feed._ID, FeedWidgetFeed.FEED_CONFIG_ID));
                query.orderBy(FeedEntry.POLLED_DATE, Order.DESC);
                query.orderBy(FeedEntry.PUBLISHED_DATE, Order.DESC);
                query.orderBy(FeedEntry.UPDATED_DATE, Order.DESC);
                break;
            case URI_FEEDWIDGET_FEED:
                query.select(FeedWidgetFeed.COLUMNS);
                query.setAliasPrefix(FeedWidgetFeed.COLUMNS, FeedWidgetFeed.ALIAS_PREFIX);
                query.setAlias(FeedWidgetFeed._ID, BaseColumns._ID); // Required for CursorAdapter
                query.from(FeedWidgetFeed.TABLE);
                query.orderBy(FeedWidgetFeed._ID, Order.ASC);
                break;
            default:
                throw new IllegalArgumentException("URI not supported by this ContentProvider: " + uri);
        }

        // TODO OMFG
        String[] existingArgs = selectionArgs;
        selectionArgs = new String[0];
        switch (URI_MATCHER.match(uri)) {
            case URI_FEEDCONFIG:
                query.where(Restrictions.eq(FeedConfig._ID, "?"));
                selectionArgs = new String[]{uri.getLastPathSegment()};
                break;
            case URI_FEEDCONFIGS_BY_URL:
                query.where(Restrictions.eq(FeedConfig.URL, "?"));
                selectionArgs = new String[]{uri.getLastPathSegment()};
                break;
            case URI_FEEDENTRY:
                query.where(Restrictions.eq(FeedEntry._ID, "?"));
                selectionArgs = new String[]{uri.getLastPathSegment()};
                break;
            case URI_FEEDENTRIES_FOR_FEEDCONFIG:
                query.where(Restrictions.eq(Feed._ID, "?"));
                selectionArgs = new String[]{uri.getLastPathSegment()};
                break;
            case URI_FEEDENTRY_FOR_LINK:
                query.where(Restrictions.eq(FeedEntry.LINK, "?"));
                selectionArgs = new String[]{uri.getLastPathSegment()};
                break;
            case URI_FEEDWIDGET:
                query.where(Restrictions.eq(FeedWidgetConfig._ID, "?"));
                selectionArgs = new String[]{uri.getLastPathSegment()};
                break;
            case URI_FEEDENTRIES_FOR_FEEDWIDGET:
                query.where(Restrictions.eq(FeedWidgetFeed.FEED_WIDGET_ID, "?"));
                selectionArgs = new String[]{uri.getLastPathSegment()};
                break;
            case URI_FEEDWIDGET_FEED:
                query.where(Restrictions.eq(FeedWidgetFeed.FEED_WIDGET_ID, "?"));
                selectionArgs = new String[]{uri.getLastPathSegment()};
                break;
        }

        String sql = new Sql92().serialize(query);
        if (PersistentEntity.RESTRICTION_LIMIT_OFFSET.equals(selection)) {
            sql = sql + " limit ? offset ?";
            selectionArgs = StringUtil.concatAll(selectionArgs, existingArgs);
        }
        log.fine("Executing SQL query: " + sql);
        log.fine("With arguments: " + Arrays.toString(selectionArgs));
        Cursor c = database.get().rawQuery(sql, selectionArgs);
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        log.fine("Inserting values for URI: " + uri);
        Uri newUri = null;
        database.get().beginTransaction();
        try {
            switch (URI_MATCHER.match(uri)) {
                case URI_FEEDCONFIGS:
                    newUri = insert(FeedConfig.TABLE, FeedConfig.CONTENT_URI, values);
                    break;
                case URI_FEEDS:
                    newUri = insert(Feed.TABLE, Feed.CONTENT_URI, values);
                    break;
                case URI_FEEDENTRIES:
                    newUri = insert(FeedEntry.TABLE, FeedEntry.CONTENT_URI, values);
                    break;
                default:
                    throw new IllegalArgumentException("URI not supported by this ContentProvider: " + uri);
                case URI_FEEDWIDGETS:
                    newUri = insert(FeedWidgetConfig.TABLE, FeedWidgetConfig.CONTENT_URI, values);
                    break;
                case URI_FEEDWIDGET_FEED:
                    newUri = insert(FeedWidgetFeed.TABLE, FeedWidgetFeed.CONTENT_URI, values);
                    break;
            }
            database.get().setTransactionSuccessful();
            getContext().getContentResolver().notifyChange(uri, null);
        } finally {
            database.get().endTransaction();
        }

        log.fine("Sending FeedWidget UPDATE_ALL broadcast");
        Intent update = new Intent();
        update.setAction(FeedWidgetProvider.ACTION_FEEDWIDGET_UPDATE_ALL);
        getContext().sendBroadcast(update);

        return newUri;
    }

    protected Uri insert(Table table, Uri baseUri, ContentValues values) {
        long id = database.get().insert(table.getName(), null, values);
        return Uri.withAppendedPath(baseUri, Long.toString(id));
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        log.fine("Updating values for URI: " + uri);
        int count;
        database.get().beginTransaction();
        try {
            switch (URI_MATCHER.match(uri)) {
                case URI_FEEDCONFIG:
                    count = database.get().update(
                       FeedConfig.TABLE.getName(),
                       values,
                       FeedConfig._ID + "=?",
                       new String[]{uri.getLastPathSegment()}
                    );
                    break;
                case URI_FEED:
                    count = database.get().update(
                       Feed.TABLE.getName(),
                       values,
                       Feed._ID + "=?",
                       new String[]{uri.getLastPathSegment()}
                    );
                    break;
                case URI_FEEDENTRIES:
                    if (FeedEntry.FEED_ID.getName().equals(selection)) {
                        // Update all feed entries of feed id
                        count = database.get().update(
                           FeedEntry.TABLE.getName(),
                           values,
                           FeedEntry.FEED_ID + "=?",
                           selectionArgs
                        );
                    } else {
                        // Update all feed entries
                        count = database.get().update(
                           FeedEntry.TABLE.getName(),
                           values, null, null
                        );
                    }
                    break;
                case URI_FEEDENTRY:
                    // Update a single feed entry
                    count = database.get().update(
                       FeedEntry.TABLE.getName(),
                       values,
                       FeedEntry._ID + "=?",
                       new String[]{uri.getLastPathSegment()}
                    );
                    break;
                case URI_FEEDWIDGET:
                    count = database.get().update(
                       FeedWidgetConfig.TABLE.getName(),
                       values,
                       FeedWidgetConfig._ID + "=?",
                       new String[]{uri.getLastPathSegment()}
                    );
                    break;
                default:
                    throw new IllegalArgumentException("URI not supported by this ContentProvider: " + uri);
            }

            database.get().setTransactionSuccessful();
            getContext().getContentResolver().notifyChange(uri, null);
        } finally {
            database.get().endTransaction();
        }

        log.fine("Sending FeedWidget UPDATE_ALL broadcast");
        Intent update = new Intent();
        update.setAction(FeedWidgetProvider.ACTION_FEEDWIDGET_UPDATE_ALL);
        getContext().sendBroadcast(update);

        return count;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        log.fine("Deleting values for URI: " + uri);
        int count;
        database.get().beginTransaction();
        try {

            switch (URI_MATCHER.match(uri)) {
                case URI_FEEDCONFIG:
                    count = database.get().delete(
                       FeedConfig.TABLE.getName(),
                       FeedConfig._ID + "=?",
                       new String[]{uri.getLastPathSegment()}
                    );
                    break;
                case URI_FEEDENTRY:
                    count = database.get().delete(
                       FeedEntry.TABLE.getName(),
                       FeedEntry._ID + "=?",
                       new String[]{uri.getLastPathSegment()}
                    );
                    break;
                case URI_FEEDWIDGET:
                    count = database.get().delete(
                       FeedWidgetConfig.TABLE.getName(),
                       FeedWidgetConfig._ID + "=?",
                       new String[]{uri.getLastPathSegment()}
                    );
                    break;
                case URI_FEEDWIDGET_FEED:
                    count = database.get().delete(
                       FeedWidgetFeed.TABLE.getName(),
                       FeedWidgetFeed.FEED_WIDGET_ID + "=?",
                       new String[]{uri.getLastPathSegment()}
                    );
                    break;
                default:
                    throw new IllegalArgumentException("URI not supported by this ContentProvider: " + uri);
            }

            database.get().setTransactionSuccessful();
            getContext().getContentResolver().notifyChange(uri, null);
        } finally {
            database.get().endTransaction();
        }

        log.fine("Sending FeedWidget UPDATE_ALL broadcast");
        Intent update = new Intent();
        update.setAction(FeedWidgetProvider.ACTION_FEEDWIDGET_UPDATE_ALL);
        getContext().sendBroadcast(update);

        return count;
    }

}