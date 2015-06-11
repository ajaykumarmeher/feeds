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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import org.fourthline.android.feeds.database.DDL;

import java.util.logging.Logger;

public class FeedsDatabase extends SQLiteOpenHelper {

    final private static Logger log = Logger.getLogger(FeedsDatabase.class.getName());

    public static final boolean TESTMODE = false;
    public static final boolean TESTMODE_INSERT_DATA = false;

    protected static FeedsDatabase INSTANCE;

    public static FeedsDatabase getInstance(Context context) {
        if (INSTANCE == null)
            INSTANCE = new FeedsDatabase(context);
        return INSTANCE;
    }

    public static final String DATABASE_NAME = "feeds";
    public static final int DATABASE_VERSION = 2;

    private FeedsDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public SQLiteDatabase get() {
        // Readable, writable, it's all the same connection with serial access
        return getWritableDatabase();
    }

    @Override
    public void onOpen(SQLiteDatabase database) {
        super.onOpen(database);

        if (TESTMODE)
            insertTestData(database);

        log.fine("Enabling foreign keys");
        database.execSQL("PRAGMA foreign_keys=ON");
    }

    @Override
    public void onCreate(SQLiteDatabase database) {

        log.fine("On create database, executing DDL");
        database.execSQL("drop table if exists " + FeedWidgetFeed.TABLE);
        database.execSQL("drop table if exists " + FeedWidgetConfig.TABLE);
        database.execSQL("drop table if exists " + FeedEntry.TABLE);
        database.execSQL("drop table if exists " + Feed.TABLE);
        database.execSQL("drop table if exists " + FeedConfig.TABLE);

        String ddl = DDL.createTable(FeedConfig.TABLE, FeedConfig.COLUMNS);
        log.fine("DDL: " + ddl);
        database.execSQL(ddl);

        ddl = DDL.createTable(Feed.TABLE, Feed.COLUMNS);
        log.fine("DDL: " + ddl);
        database.execSQL(ddl);

        ddl = DDL.createTable(FeedEntry.TABLE, FeedEntry.COLUMNS);
        log.fine("DDL: " + ddl);
        database.execSQL(ddl);

        ddl = DDL.createTable(FeedWidgetConfig.TABLE, FeedWidgetConfig.COLUMNS);
        log.fine("DDL: " + ddl);
        database.execSQL(ddl);

        ddl = DDL.createTable(FeedWidgetFeed.TABLE, FeedWidgetFeed.COLUMNS);
        log.fine("DDL: " + ddl);
        database.execSQL(ddl);
    }

    @Override
    public void close() {
        super.close();
        log.fine("On close database");
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        if (oldVersion == 1 && newVersion == 2) {
            database.execSQL(
                "alter table " + FeedConfig.TABLE.getName()
                    + " add column " + FeedConfig.LAST_REFRESH_ETAG.getName() + " string not null default ''"
            );
        }
    }

    protected void insertTestData(SQLiteDatabase db) {
        try {
            db.beginTransaction();

            log.fine("Cleaning database");
            db.execSQL("delete from " + FeedWidgetFeed.TABLE);
            db.execSQL("delete from " + FeedWidgetConfig.TABLE);
            db.execSQL("delete from " + FeedEntry.TABLE);
            db.execSQL("delete from " + Feed.TABLE);
            db.execSQL("delete from " + FeedConfig.TABLE);

            if (!TESTMODE_INSERT_DATA) {
                db.setTransactionSuccessful();
                return;
            }

            log.fine("Inserting test data");
            FeedConfig[] feedConfigs = FeedConfig.TESTDATA();
            for (FeedConfig feedConfig : feedConfigs) {
                feedConfig.setId(db.insert(FeedConfig.TABLE.getName(), null, feedConfig.INSTANCE.getEntityValues()));
            }

            Feed[] feeds = Feed.TESTDATA(feedConfigs);
            for (Feed feed : feeds) {
                feed.setId(db.insert(Feed.TABLE.getName(), null, feed.INSTANCE.getEntityValues()));
            }

            FeedEntry[] feedEntries = FeedEntry.TESTDATA(feeds);
            for (FeedEntry feedEntry : feedEntries) {
                feedEntry.setId(db.insert(FeedEntry.TABLE.getName(), null, feedEntry.INSTANCE.getEntityValues()));
            }

            FeedWidgetConfig[] feedWidgetConfigs = FeedWidgetConfig.TESTDATA();
            for (FeedWidgetConfig feedWidgetConfig : feedWidgetConfigs) {
                feedWidgetConfig.setId(db.insert(FeedWidgetConfig.TABLE.getName(), null, feedWidgetConfig.INSTANCE.getEntityValues()));
            }

            FeedWidgetFeed[] feedWidgetFeeds = FeedWidgetFeed.TESTDATA(feedWidgetConfigs, feedConfigs);
            for (FeedWidgetFeed feedWidgetFeed : feedWidgetFeeds) {
                feedWidgetFeed.setId(db.insert(FeedWidgetFeed.TABLE.getName(), null, feedWidgetFeed.INSTANCE.getEntityValues()));
            }

            db.setTransactionSuccessful();
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
        } finally {
            db.endTransaction();
        }
    }

}