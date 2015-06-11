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

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import com.googlecode.sqb.query.Column;
import com.googlecode.sqb.query.DataType;
import com.googlecode.sqb.query.ForeignKey;
import com.googlecode.sqb.query.References;
import com.googlecode.sqb.query.Table;
import org.fourthline.android.feeds.Constants;
import org.fourthline.android.feeds.database.PersistentEntity;

public final class Feed extends PersistentEntity {

    public static final String ENTITY = getEntityName(Feed.class);

    public static final String MIME_ITEM = MIME_ITEM_PREFIX + Constants.MIME_PREFIX + ENTITY;
    public static final String MIME_DIR = MIME_DIR_PREFIX + Constants.MIME_PREFIX + ENTITY;

    public static final Uri CONTENT_URI =
       new Uri.Builder().scheme(Constants.SCHEME_CONTENT)
          .authority(FeedContent.AUTHORITY)
          .appendPath(ENTITY).build();

    public static final Table TABLE = References.table(getTableName(Feed.class));
    public static final String ALIAS_PREFIX = "f";
    public static final Column<Long> _ID = References.columnPK(TABLE, new ForeignKey(FeedConfig._ID, ForeignKey.Action.CASCADE));
    public static final Column<Integer> CATEGORY = References.column(TABLE, "CATEGORY", DataType.INTEGER);
    public static final Column<String> LINK = References.column(TABLE, "LINK");
    public static final Column<String> TITLE = References.column(TABLE, "TITLE");
    public static final Column<String> DESCRIPTION = References.column(TABLE, "DESCRIPTION");
    public static final Column<Long> PUBLISHED_DATE = References.column(TABLE, "PUBLISHED_DATE", DataType.LONG);

    public static final Column[] COLUMNS = new Column[]{
       _ID, CATEGORY, LINK, TITLE, DESCRIPTION, PUBLISHED_DATE
    };

    public static final String DEFAULT_TITLE = "NO TITLE";
    public static final String DEFAULT_DESCRIPTION = "NO DESCRIPTION";
    public static final long DEFAULT_DATE = 0;

    public Feed(Long id, int category, String link, String title, String description, long publishedDate) {
        super(id);
        ContentValues values = INSTANCE.getEntityValues();
        DataType.write(values, CATEGORY, category);
        DataType.write(values, LINK, link);
        DataType.write(values, LINK, link);
        DataType.write(values, TITLE, title);
        DataType.write(values, DESCRIPTION, description);
        DataType.write(values, PUBLISHED_DATE, publishedDate);
    }

    public static String getFeedTitleOrUrl(Cursor cursor) {
        String title = DataType.read(cursor, Feed.TITLE, Feed.ALIAS_PREFIX);
        return title != null
           ? title
           : DataType.read(cursor, FeedConfig.URL, FeedConfig.ALIAS_PREFIX);
    }

    public static Feed[] TESTDATA(FeedConfig[] feedConfigs) {
        long now = System.currentTimeMillis();
        return new Feed[]{
/*
           new Feed(feedConfigs[0].getId(), 0, "http://foo", "bar", "baz", now),
*/
/*
           new Feed(feedConfigs[0].getId(), 0, "http://4thline.org/articles/atom", "4th Line Articles with a really long title that should be enough to fill the UI", "Some description...", now),
           new Feed(feedConfigs[1].getId(), 0, "http://blog.fefe.de/rss.xml", "Fefes Blog", "Verschwörungen und Obskures aus aller Welt", now)
*/
        };
    }


}