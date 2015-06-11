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
package org.fourthline.android.feeds.content;

import android.content.ContentValues;
import android.net.Uri;
import com.googlecode.sqb.query.Column;
import com.googlecode.sqb.query.DataType;
import com.googlecode.sqb.query.ForeignKey;
import com.googlecode.sqb.query.References;
import com.googlecode.sqb.query.Table;
import org.fourthline.android.feeds.Constants;
import org.fourthline.android.feeds.database.PersistentEntity;

public class FeedWidgetFeed extends PersistentEntity {

    public static final String ENTITY = getEntityName(FeedWidgetFeed.class);

    public static final String MIME_ITEM = MIME_ITEM_PREFIX + Constants.MIME_PREFIX + ENTITY;
    public static final String MIME_DIR = MIME_DIR_PREFIX + Constants.MIME_PREFIX + ENTITY;

    public static final Uri CONTENT_URI =
       new Uri.Builder().scheme(Constants.SCHEME_CONTENT)
          .authority(FeedContent.AUTHORITY)
          .appendPath(ENTITY).build();

    public static final Table TABLE = References.table(getTableName(FeedWidgetFeed.class));
    public static final String ALIAS_PREFIX = "fwf";
    public static final Column<Long> _ID = References.columnPK(TABLE);
    public static final Column<Long> FEED_WIDGET_ID =
       References.column(TABLE, "FEED_WIDGET_ID", DataType.LONG, new ForeignKey(FeedWidgetConfig._ID, ForeignKey.Action.CASCADE));
    public static final Column<Long> FEED_CONFIG_ID =
       References.column(TABLE, "FEED_CONFIG_ID", DataType.LONG, new ForeignKey(FeedConfig._ID, ForeignKey.Action.CASCADE));

    public static final Column[] COLUMNS = new Column[]{
       _ID, FEED_WIDGET_ID, FEED_CONFIG_ID
    };

    public static final String[] PROJECTION_FEED_SELECTION = new String[]{"FEED_LABEL", "IS_SELECTED"};

    public FeedWidgetFeed(Long id, long feedWidgetId, long feedId) {
        super(id);
        ContentValues values = INSTANCE.getEntityValues();
        DataType.write(values, FEED_WIDGET_ID, feedWidgetId);
        DataType.write(values, FEED_CONFIG_ID, feedId);
    }

    public static FeedWidgetFeed[] TESTDATA(FeedWidgetConfig[] feedWidgetConfigs, FeedConfig[] feedConfigs) {
        return new FeedWidgetFeed[]{
/*
           new FeedWidgetFeed(null, feedWidgetConfigs[0].getId(), feedConfigs[0].getId()),
           new FeedWidgetFeed(null, feedWidgetConfigs[0].getId(), feedConfigs[1].getId())
*/
        };
    }
}
