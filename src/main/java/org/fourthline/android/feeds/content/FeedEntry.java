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
import com.googlecode.sqb.query.Projection;
import com.googlecode.sqb.query.References;
import com.googlecode.sqb.query.Table;
import org.fourthline.android.feeds.Constants;
import org.fourthline.android.feeds.database.PersistentEntity;
import org.fourthline.android.feeds.util.StringUtil;

public class FeedEntry extends PersistentEntity {

    public static final String ENTITY = getEntityName(FeedEntry.class);

    public static final String MIME_ITEM = MIME_ITEM_PREFIX + Constants.MIME_PREFIX + ENTITY;
    public static final String MIME_DIR = MIME_DIR_PREFIX + Constants.MIME_PREFIX + ENTITY;

    public static final Uri CONTENT_URI =
       new Uri.Builder().scheme(Constants.SCHEME_CONTENT)
          .authority(FeedContent.AUTHORITY)
          .appendPath(ENTITY).build();

    public static final Table TABLE = References.table(getTableName(FeedEntry.class));
    public static final String ALIAS_PREFIX = "fe";
    public static final Column<Long> _ID = References.columnPK(TABLE);
    public static final Column<Long> FEED_ID =
       References.column(TABLE, "FEED_ID", DataType.LONG, new ForeignKey(Feed._ID, ForeignKey.Action.CASCADE));
    public static final Column<String> LINK = References.column(TABLE, "LINK");
    public static final Column<String> TITLE = References.column(TABLE, "TITLE");
    public static final Column<String> AUTHOR = References.column(TABLE, "AUTHOR");
    public static final Column<Long> POLLED_DATE = References.column(TABLE, "POLLED_DATE", DataType.LONG);
    public static final Column<Long> PUBLISHED_DATE = References.column(TABLE, "PUBLISHED_DATE", DataType.LONG);
    public static final Column<Long> UPDATED_DATE = References.column(TABLE, "UPDATED_DATE", DataType.LONG);
    public static final Column<String> DESCRIPTION_TYPE = References.column(TABLE, "DESCRIPTION_TYPE");
    public static final Column<String> DESCRIPTION_VALUE = References.column(TABLE, "DESCRIPTION_VALUE");
    public static final Column<Boolean> IS_READ = References.column(TABLE, "IS_READ", DataType.BOOLEAN);

    public static final Column[] COLUMNS = new Column[]{
       _ID, FEED_ID, LINK, TITLE, AUTHOR, POLLED_DATE, PUBLISHED_DATE, UPDATED_DATE, DESCRIPTION_TYPE, DESCRIPTION_VALUE, IS_READ
    };

    public static final Projection[] PROJECTION_WITH_FEED = new Projection[]{
       _ID, FEED_ID, LINK, TITLE, AUTHOR, POLLED_DATE, PUBLISHED_DATE, UPDATED_DATE, DESCRIPTION_TYPE, DESCRIPTION_VALUE, IS_READ,
       FeedConfig._ID, FeedConfig.URL, FeedConfig.PREVIEW_LENGTH, FeedConfig.TEXT_COLOR, FeedConfig.ENTRY_PREFIX, Feed.TITLE
    };

    /* TODO: No aggregation/grouping in query builder
    public static final String COUNT_FEEDENTRY_ALIAS = ALIAS_PREFIX + "count";
    public static final Projection COUNT_FEEDENTRY = new StringProjection("count("+ALIAS_PREFIX+_ID+")", COUNT_FEEDENTRY_ALIAS);
    */

    public static final Uri CONTENT_URI_FOR_FEEDCONFIG =
       new Uri.Builder().scheme(Constants.SCHEME_CONTENT)
          .authority(FeedContent.AUTHORITY)
          .appendPath(ENTITY)
          .appendPath(FeedConfig.ENTITY)
          .build();

    public static final Uri CONTENT_URI_FOR_FEEDWIDGET =
       new Uri.Builder().scheme(Constants.SCHEME_CONTENT)
          .authority(FeedContent.AUTHORITY)
          .appendPath(ENTITY)
          .appendPath(FeedWidgetConfig.ENTITY)
          .build();

    // Default values
    public static final String DEFAULT_TITLE = "NO TITLE";
    public static final String DEFAULT_AUTHOR = "NO AUTHOR";
    public static final long DEFAULT_DATE = 0;
    public static final String DEFAULT_DESCRIPTION_TYPE = "text/plain";
    public static final String DEFAULT_DESCRIPTION_VALUE = "NO DESCRIPTION";

    public FeedEntry(Long id, long feedId, String link, String title, String author,
                     long polledDate, long publishedDate, long updatedDate,
                     String descriptionType, String descriptionValue, boolean isRead) {
        super(id);
        ContentValues values = INSTANCE.getEntityValues();
        DataType.write(values, FEED_ID, feedId);
        DataType.write(values, LINK, link);
        DataType.write(values, TITLE, title);
        DataType.write(values, AUTHOR, author);
        DataType.write(values, POLLED_DATE, polledDate);
        DataType.write(values, PUBLISHED_DATE, publishedDate);
        DataType.write(values, UPDATED_DATE, updatedDate);
        DataType.write(values, DESCRIPTION_TYPE, descriptionType);
        DataType.write(values, DESCRIPTION_VALUE, descriptionValue);
        DataType.write(values, IS_READ, isRead);
    }

    public FeedEntry(long id, boolean isRead) {
        super(id);
        ContentValues values = INSTANCE.getEntityValues();
        DataType.write(values, IS_READ, isRead);
    }

    public FeedEntry(Cursor c) {
        this(
            DataType.read(c, _ID),
            DataType.read(c, FEED_ID, ALIAS_PREFIX),
            DataType.read(c, LINK, ALIAS_PREFIX),
            DataType.read(c, TITLE, ALIAS_PREFIX),
            DataType.read(c, AUTHOR, ALIAS_PREFIX),
            DataType.read(c, POLLED_DATE, ALIAS_PREFIX),
            DataType.read(c, PUBLISHED_DATE, ALIAS_PREFIX),
            DataType.read(c, UPDATED_DATE, ALIAS_PREFIX),
            DataType.read(c, DESCRIPTION_TYPE, ALIAS_PREFIX),
            DataType.read(c, DESCRIPTION_VALUE, ALIAS_PREFIX),
            DataType.read(c, IS_READ, ALIAS_PREFIX)
        );
    }

    public static String getDescriptionAsText(Cursor cursor) {
        return getDescriptionAsText(cursor, -1);
    }

    public static String getDescriptionAsText(Cursor cursor, int length) {
        String description = getDescription(cursor);
        if (isDescriptionHTML(cursor)) {
            description = StringUtil.removeHtml(description);
        }
        description = StringUtil.replaceControlChars(description);
        description = StringUtil.replaceMultipleWhitespace(description);
        description = description.trim();
        if (length != -1)
            description = StringUtil.truncateOnWordBoundary(description, length, "...");

        return description;
    }

    public static String getDescriptionAsHTML(Cursor cursor) {
        String description = getDescription(cursor);

        StringBuilder sb = new StringBuilder();
        sb.append("<html><head><style>body {margin:0;padding:0;}</style></head><body><div>");
        if (isDescriptionHTML(cursor)) {
            sb.append(description);
        } else {
            sb.append(description.replaceAll("\\n|\\r", "<br>"));
        }
        sb.append("</div></body></html>");
        return sb.toString();
    }

    public static String getDescription(Cursor cursor) {
        String description = DataType.read(cursor, FeedEntry.DESCRIPTION_VALUE, FeedEntry.ALIAS_PREFIX);
        if (description.equals(DEFAULT_DESCRIPTION_VALUE)) {
            description = DataType.read(cursor, FeedEntry.TITLE, FeedEntry.ALIAS_PREFIX);
        }
        return description;
    }

    public static boolean isDescriptionHTML(Cursor cursor) {
        return DataType.read(cursor, FeedEntry.DESCRIPTION_TYPE, FeedEntry.ALIAS_PREFIX).equals("text/html");
    }

    public boolean isExpired(int maxAgeDays) {
        long polledDate = getValue(POLLED_DATE);
        long expiredDate = polledDate + (maxAgeDays * 86400000);
        return expiredDate <= System.currentTimeMillis();
    }

    public static FeedEntry[] TESTDATA(Feed[] feeds) {
        long now = System.currentTimeMillis();
        long earlier = now - 600;
        return new FeedEntry[]{
           /*
           new FeedEntry(null, feeds[0].getId(), "http://foo1", "1", "", System.currentTimeMillis()-1000, DEFAULT_PUBLISHED_UPDATED_DATE, "text/html", "Foo <b>1</b>"),
           new FeedEntry(null, feeds[0].getId(), "http://foo2", "2", "", System.currentTimeMillis()-2000, DEFAULT_PUBLISHED_UPDATED_DATE, "text/html", "Foo <b>2</b>"),
           new FeedEntry(null, feeds[0].getId(), "http://foo3", "3", "", System.currentTimeMillis()-3000, DEFAULT_PUBLISHED_UPDATED_DATE, "text/html", "Foo <b>3</b>"),
           new FeedEntry(null, feeds[0].getId(), "http://foo4", "4", "", System.currentTimeMillis()-4000, DEFAULT_PUBLISHED_UPDATED_DATE, "text/html", "Foo <b>4</b>"),
           new FeedEntry(null, feeds[0].getId(), "http://foo5", "5", "", System.currentTimeMillis()-5000, DEFAULT_PUBLISHED_UPDATED_DATE, "text/html", "Foo <b>5</b>"),
           new FeedEntry(null, feeds[0].getId(), "http://foo6", "6", "", System.currentTimeMillis()-6000, DEFAULT_PUBLISHED_UPDATED_DATE, "text/html", "Foo <b>6</b>"),
           new FeedEntry(null, feeds[0].getId(), "http://foo7", "7", "", System.currentTimeMillis()-7000, DEFAULT_PUBLISHED_UPDATED_DATE, "text/html", "Foo <b>7</b>"),
           new FeedEntry(null, feeds[0].getId(), "http://foo8", "8", "", System.currentTimeMillis()-8000, DEFAULT_PUBLISHED_UPDATED_DATE, "text/html", "Foo <b>8</b>"),
           new FeedEntry(null, feeds[0].getId(), "http://foo9", "9", "", System.currentTimeMillis()-9000, DEFAULT_PUBLISHED_UPDATED_DATE, "text/html", "Foo <b>9</b>"),
           new FeedEntry(null, feeds[0].getId(), "http://foo10", "10", "", System.currentTimeMillis()-10000, DEFAULT_PUBLISHED_UPDATED_DATE, "text/html", "Foo <b>10</b>"),
           new FeedEntry(null, feeds[0].getId(), "http://foo11", "11", "", System.currentTimeMillis()-11000, DEFAULT_PUBLISHED_UPDATED_DATE, "text/html", "Foo <b>11</b>"),
           new FeedEntry(null, feeds[0].getId(), "http://foo12", "12", "", System.currentTimeMillis()-12000, DEFAULT_PUBLISHED_UPDATED_DATE, "text/html", "Foo <b>12</b>"),
           new FeedEntry(null, feeds[0].getId(), "http://foo13", "13", "", System.currentTimeMillis()-13000, DEFAULT_PUBLISHED_UPDATED_DATE, "text/html", "Foo <b>13</b>"),
           */
           /*
        new FeedEntry(null, feeds[0].getId(),
           "http://foo.xml",
           "The first item with some long text in the title",
           "", // Empty author
           earlier, earlier,
           "text/html",
           "Some <b>bold</b> text with HTML, and a few lines. " +
              "Lorem ipsum dolor sit amet, consectetuer adipiscing elit.<br>\n<br/>\nNam cursus. Morbi ut mi. Nullam enim leo, egestas id, condimentum at, laoreet mattis, massa."
        ),
        new FeedEntry(null, feeds[0].getId(),
           "http://bar.xml",
           "A second item, also with much text so we can wrap it on the UI's border for sure!",
           "Christian Bauer",
           now, now,
           "text",
           "Some text with a few lines. " +
              "Lorem ipsum dolor sit amet, consectetuer adipiscing elit.\n\nNam cursus. Morbi ut mi. Nullam enim leo, egestas id, condimentum at, laoreet mattis, massa.\n\nNam cursus. Morbi ut mi. Nullam enim leo, egestas id, condimentum at, laoreet mattis, massa.\n\nNam cursus. Morbi ut mi. Nullam enim leo, egestas id, condimentum at, laoreet mattis, massa.\n\nNam cursus. Morbi ut mi. Nullam enim leo, egestas id, condimentum at, laoreet mattis, massa.\n\nNam cursus. Morbi ut mi. Nullam enim leo, egestas id, condimentum at, laoreet mattis, massa.\n\nNam cursus. Morbi ut mi. Nullam enim leo, egestas id, condimentum at, laoreet mattis, massa."
        ),
        new FeedEntry(null, feeds[1].getId(),
           "http://baz.xml",
           "Third item has a very long title but no description, all the text is in fact in the title, so we need to render this properly.",
           DEFAULT_AUTHOR,
           DEFAULT_PUBLISHED_UPDATED_DATE, DEFAULT_PUBLISHED_UPDATED_DATE,
           DEFAULT_DESCRIPTION_TYPE, DEFAULT_DESCRIPTION_VALUE),
        new FeedEntry(null, feeds[1].getId(),
           "http://baz2.xml",
           "baz2.",
           DEFAULT_AUTHOR,
           DEFAULT_PUBLISHED_UPDATED_DATE, DEFAULT_PUBLISHED_UPDATED_DATE,
           DEFAULT_DESCRIPTION_TYPE, DEFAULT_DESCRIPTION_VALUE),
           */
        };
    }

}
