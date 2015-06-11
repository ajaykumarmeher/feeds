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

import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import com.googlecode.sqb.query.Column;
import com.googlecode.sqb.query.DataType;
import com.googlecode.sqb.query.Projection;
import com.googlecode.sqb.query.References;
import com.googlecode.sqb.query.Table;
import org.fourthline.android.feeds.Constants;
import org.fourthline.android.feeds.database.PersistentEntity;

public class FeedConfig extends PersistentEntity {

    public static final String ENTITY = getEntityName(FeedConfig.class);

    public static final String MIME_ITEM = MIME_ITEM_PREFIX + Constants.MIME_PREFIX + ENTITY;
    public static final String MIME_DIR = MIME_DIR_PREFIX + Constants.MIME_PREFIX + ENTITY;

    public static final Uri CONTENT_URI =
       new Uri.Builder().scheme(Constants.SCHEME_CONTENT)
          .authority(FeedContent.AUTHORITY)
          .appendPath(ENTITY)
          .build();

    public static final Uri CONTENT_URI_BY_URL =
       new Uri.Builder().scheme(Constants.SCHEME_CONTENT)
          .authority(FeedContent.AUTHORITY)
          .appendPath(ENTITY)
          .appendPath("url")
          .build();

    public enum EntryPrefix {
        NONE,
        AUTHOR,
        TIMESTAMP,
        FEED_TITLE
    }

    public enum PreviewLength {
        NONE,
        LESS,
        REGULAR,
        MORE,
        ALL
    }

    public static final Table TABLE = References.table(getTableName(FeedConfig.class));
    public static final String ALIAS_PREFIX = "fc";
    public static final Column<Long> _ID = References.columnPK(TABLE);
    public static final Column<String> URL = References.column(TABLE, "URL");
    public static final Column<Integer> REFRESH_INTERVAL = References.column(TABLE, "REFRESH_INTERVAL", DataType.INTEGER);
    public static final Column<Enum<PreviewLength>> PREVIEW_LENGTH = References.column(TABLE, "PREVIEW_LENGTH", new DataType.Enum(PreviewLength.class));
    public static final Column<Enum<EntryPrefix>> ENTRY_PREFIX = References.column(TABLE, "ENTRY_PREFIX", new DataType.Enum(EntryPrefix.class));
    public static final Column<Integer> TEXT_COLOR = References.column(TABLE, "TEXT_COLOR", DataType.INTEGER);
    public static final Column<Long> LAST_REFRESH = References.column(TABLE, "LAST_REFRESH", DataType.LONG);
    public static final Column<String> LAST_REFRESH_ETAG = References.column(TABLE, "LAST_REFRESH_ETAG");
    public static final Column<Integer> MAX_AGE_DAYS = References.column(TABLE, "MAX_AGE_DAYS", DataType.INTEGER);
    public static final Column<Boolean> NOTIFY_NEW = References.column(TABLE, "NOTIFY_NEW", DataType.BOOLEAN);

    public static final Column[] COLUMNS = new Column[]{
       _ID, URL, REFRESH_INTERVAL, PREVIEW_LENGTH, ENTRY_PREFIX, TEXT_COLOR, LAST_REFRESH, LAST_REFRESH_ETAG, MAX_AGE_DAYS, NOTIFY_NEW
    };

    public static final Projection[] PROJECTION_WITH_FEED = new Projection[]{
       _ID, URL, REFRESH_INTERVAL, PREVIEW_LENGTH, ENTRY_PREFIX, TEXT_COLOR, LAST_REFRESH, LAST_REFRESH_ETAG, MAX_AGE_DAYS, NOTIFY_NEW, Feed._ID, Feed.TITLE, Feed.DESCRIPTION
    };

    public static final int DEFAULT_REFRESH_INTERVAL = 1800000; // 30 mins
    public static final PreviewLength DEFAULT_PREVIEW_LENGTH = PreviewLength.REGULAR;
    public static final long DEFAULT_LAST_REFRESH = 0;
    public static final String DEFAULT_LAST_REFRESH_ETAG = "";
    public static final int DEFAULT_MAX_AGE_DAYS = 14;
    public static final boolean DEFAULT_NOTIFY_NEW = true;
    public static final EntryPrefix DEFAULT_ENTRY_PREFIX = FeedConfig.EntryPrefix.NONE;
    public static final int DEFAULT_TEXT_COLOR = Color.WHITE;

    public FeedConfig(Long id, String url, int refreshInterval,
                      Enum<PreviewLength> previewLength, Enum<EntryPrefix> entryPrefix,
                      int textColor, long lastRefresh, String lastRefreshEtag, int maxAgeDays, boolean notifyNew) {
        DataType.writeAll(
           INSTANCE.getEntityValues(),
           COLUMNS,
           id, url, refreshInterval, previewLength, entryPrefix, textColor, lastRefresh, lastRefreshEtag, maxAgeDays, notifyNew
        );
    }

    public FeedConfig(String url, int refreshInterval,
                      Enum<PreviewLength> previewLength, Enum<EntryPrefix> entryPrefix,
                      int textColor, int maxAgeDays, boolean notifyNew) {
        DataType.write(INSTANCE.getEntityValues(), URL, url);
        DataType.write(INSTANCE.getEntityValues(), PREVIEW_LENGTH, previewLength);
        DataType.write(INSTANCE.getEntityValues(), REFRESH_INTERVAL, refreshInterval);
        DataType.write(INSTANCE.getEntityValues(), ENTRY_PREFIX, entryPrefix);
        DataType.write(INSTANCE.getEntityValues(), TEXT_COLOR, textColor);
        DataType.write(INSTANCE.getEntityValues(), MAX_AGE_DAYS, maxAgeDays);
        DataType.write(INSTANCE.getEntityValues(), NOTIFY_NEW, notifyNew);
    }

    public FeedConfig(Long id, long lastRefresh, String lastRefreshEtag) {
        super(id);
        DataType.write(INSTANCE.getEntityValues(), LAST_REFRESH, lastRefresh);
        DataType.write(INSTANCE.getEntityValues(), LAST_REFRESH_ETAG, lastRefreshEtag);
    }

    public FeedConfig(Cursor c) {
        this(
           DataType.read(c, _ID),
           DataType.read(c, URL, ALIAS_PREFIX),
           DataType.read(c, REFRESH_INTERVAL, ALIAS_PREFIX),
           DataType.read(c, PREVIEW_LENGTH, ALIAS_PREFIX),
           DataType.read(c, ENTRY_PREFIX, ALIAS_PREFIX),
           DataType.read(c, TEXT_COLOR, ALIAS_PREFIX),
           DataType.read(c, LAST_REFRESH, ALIAS_PREFIX),
           DataType.read(c, LAST_REFRESH_ETAG, ALIAS_PREFIX),
           DataType.read(c, MAX_AGE_DAYS, ALIAS_PREFIX),
           DataType.read(c, NOTIFY_NEW, ALIAS_PREFIX)
        );
    }

    public static FeedConfig[] TESTDATA() {
        long now = System.currentTimeMillis();
        return new FeedConfig[]{
            new FeedConfig(null, "http://blog.fefe.de/rss.xml", 900000, PreviewLength.NONE, EntryPrefix.NONE, DEFAULT_TEXT_COLOR, DEFAULT_LAST_REFRESH, DEFAULT_LAST_REFRESH_ETAG, DEFAULT_MAX_AGE_DAYS, DEFAULT_NOTIFY_NEW),
/*
           new FeedConfig(null, "http://home.cbauer.name/tmp/test/atom", 900000, PreviewLength.REGULAR, EntryPrefix.AUTHOR, Color.LTGRAY, now, DEFAULT_MAX_AGE_DAYS, DEFAULT_NOTIFY_NEW),
           new FeedConfig(null, "http://rss.cnn.com/rss/edition.rss", 900000, PreviewLength.ALL, EntryPrefix.NONE, DEFAULT_TEXT_COLOR, DEFAULT_LAST_REFRESH, DEFAULT_LAST_REFRESH_ETAG, DEFAULT_MAX_AGE_DAYS, DEFAULT_NOTIFY_NEW),
           new FeedConfig(null, "http://www.reddit.com/r/roomporn.rss", 900000, PreviewLength.LESS, EntryPrefix.NONE, DEFAULT_TEXT_COLOR, DEFAULT_LAST_REFRESH, DEFAULT_LAST_REFRESH_ETAG, DEFAULT_MAX_AGE_DAYS, DEFAULT_NOTIFY_NEW),
*/
/*
           new FeedConfig(null, "http://www.reddit.com/.rss", 900000, PreviewLength.REGULAR, EntryPrefix.NONE, DEFAULT_TEXT_COLOR, DEFAULT_LAST_REFRESH, DEFAULT_MAX_AGE_DAYS, DEFAULT_NOTIFY_NEW),
           new FeedConfig(null, "http://www.engadget.com/rss.xml", 900000, PreviewLength.REGULAR, EntryPrefix.NONE, DEFAULT_TEXT_COLOR, DEFAULT_LAST_REFRESH, DEFAULT_MAX_AGE_DAYS, DEFAULT_NOTIFY_NEW),
           new FeedConfig(null, "http://blog.fefe.de/rss.xml?html", 900000, PreviewLength.NONE, EntryPrefix.NONE, DEFAULT_TEXT_COLOR, DEFAULT_LAST_REFRESH, DEFAULT_MAX_AGE_DAYS, DEFAULT_NOTIFY_NEW),
*/
/*
           new FeedConfig(null, "http://1", 900000, EntryPrefix.FEED_TITLE, Color.WHITE,  now, DEFAULT_MAX_AGE_DAYS),
           new FeedConfig(null, "http://2", 900000, EntryPrefix.FEED_TITLE, Color.WHITE,  now, DEFAULT_MAX_AGE_DAYS),
           new FeedConfig(null, "http://3", 900000, EntryPrefix.FEED_TITLE, Color.WHITE,  now, DEFAULT_MAX_AGE_DAYS),
           new FeedConfig(null, "http://4", 900000, EntryPrefix.FEED_TITLE, Color.WHITE,  now, DEFAULT_MAX_AGE_DAYS),
           new FeedConfig(null, "http://5", 900000, EntryPrefix.FEED_TITLE, Color.WHITE,  now, DEFAULT_MAX_AGE_DAYS),
           new FeedConfig(null, "http://6", 900000, EntryPrefix.FEED_TITLE, Color.WHITE,  now, DEFAULT_MAX_AGE_DAYS),
           new FeedConfig(null, "http://7", 900000, EntryPrefix.FEED_TITLE, Color.WHITE,  now, DEFAULT_MAX_AGE_DAYS),
           new FeedConfig(null, "http://8", 900000, EntryPrefix.FEED_TITLE, Color.WHITE,  now, DEFAULT_MAX_AGE_DAYS),
           new FeedConfig(null, "http://9", 900000, EntryPrefix.FEED_TITLE, Color.WHITE,  now, DEFAULT_MAX_AGE_DAYS),
           new FeedConfig(null, "http://10", 900000, EntryPrefix.FEED_TITLE, Color.WHITE,  now, DEFAULT_MAX_AGE_DAYS),
           new FeedConfig(null, "http://11", 900000, EntryPrefix.FEED_TITLE, Color.WHITE,  now, DEFAULT_MAX_AGE_DAYS),
           new FeedConfig(null, "http://12", 900000, EntryPrefix.FEED_TITLE, Color.WHITE,  now, DEFAULT_MAX_AGE_DAYS),
           new FeedConfig(null, "http://13", 900000, EntryPrefix.FEED_TITLE, Color.WHITE,  now, DEFAULT_MAX_AGE_DAYS),
           new FeedConfig(null, "http://14", 900000, EntryPrefix.FEED_TITLE, Color.WHITE,  now, DEFAULT_MAX_AGE_DAYS),
           new FeedConfig(null, "http://15", 900000, EntryPrefix.FEED_TITLE, Color.WHITE,  now, DEFAULT_MAX_AGE_DAYS),
           new FeedConfig(null, "http://16", 900000, EntryPrefix.FEED_TITLE, Color.WHITE,  now, DEFAULT_MAX_AGE_DAYS),
           new FeedConfig(null, "http://17", 900000, EntryPrefix.FEED_TITLE, Color.WHITE,  now, DEFAULT_MAX_AGE_DAYS),
           new FeedConfig(null, "http://18", 900000, EntryPrefix.FEED_TITLE, Color.WHITE,  now, DEFAULT_MAX_AGE_DAYS),
           new FeedConfig(null, "http://19", 900000, EntryPrefix.FEED_TITLE, Color.WHITE,  now, DEFAULT_MAX_AGE_DAYS),
           new FeedConfig(null, "http://20", 900000, EntryPrefix.FEED_TITLE, Color.WHITE,  now, DEFAULT_MAX_AGE_DAYS),
*/
        };
    }
}
