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

import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.provider.BaseColumns;
import com.googlecode.sqb.query.Column;
import com.googlecode.sqb.query.DataType;
import com.googlecode.sqb.query.References;
import com.googlecode.sqb.query.Table;
import org.fourthline.android.feeds.Constants;
import org.fourthline.android.feeds.database.PersistentEntity;

import java.io.Serializable;

public class FeedWidgetConfig extends PersistentEntity implements Serializable {

    private static final long serialVersionUID = 5961873962274601693L;

    public static final String ENTITY = getEntityName(FeedWidgetConfig.class);

    public static final String MIME_ITEM = MIME_ITEM_PREFIX + Constants.MIME_PREFIX + ENTITY;
    public static final String MIME_DIR = MIME_DIR_PREFIX + Constants.MIME_PREFIX + ENTITY;

    public static final Uri CONTENT_URI =
       new Uri.Builder().scheme(Constants.SCHEME_CONTENT)
          .authority(FeedContent.AUTHORITY)
          .appendPath(ENTITY).build();

    public enum TextSize {
        LARGE(16),
        MEDIUM(13),
        SMALL(10);

        public float size;

        TextSize(float size) {
            this.size = size;
        }
    }

    public static final Table TABLE = References.table(getTableName(FeedWidgetConfig.class));
    public static final String ALIAS_PREFIX = "fwc";
    public static final Column<Long> _ID = References.column(TABLE, BaseColumns._ID, DataType.LONG, true);
    public static final Column<Boolean> TOOLBAR = References.column(TABLE, "TOOLBAR", DataType.BOOLEAN);
    public static final Column<Integer> BACKGROUND_COLOR = References.column(TABLE, "BACKGROUND_COLOR", DataType.INTEGER);
    public static final Column<Enum<TextSize>> TEXT_SIZE = References.column(TABLE, "TEXT_SIZE", new DataType.Enum(TextSize.class));

    public static final Column[] COLUMNS = new Column[]{
       _ID, TOOLBAR, BACKGROUND_COLOR, TEXT_SIZE
    };

    public static final boolean DEFAULT_TOOLBAR = true;
    public static final int DEFAULT_BACKGROUND_COLOR = Color.argb(220, 30, 30, 30);
    public static final TextSize DEFAULT_TEXT_SIZE = TextSize.MEDIUM;

    public FeedWidgetConfig(long id, boolean toolbar, int backgroundColor, Enum<TextSize> textSize) {
        DataType.writeAll(
           INSTANCE.getEntityValues(),
           COLUMNS,
           id, toolbar, backgroundColor, textSize
        );
    }

    public FeedWidgetConfig(Cursor c) {
        this(
           DataType.read(c, _ID),
           DataType.read(c, TOOLBAR, ALIAS_PREFIX),
           DataType.read(c, BACKGROUND_COLOR, ALIAS_PREFIX),
           DataType.read(c, TEXT_SIZE, ALIAS_PREFIX)
        );
    }

    public static FeedWidgetConfig[] TESTDATA() {
        return new FeedWidgetConfig[]{
           //new FeedWidgetConfig(null, DEFAULT_TOOLBAR, DEFAULT_BACKGROUND_COLOR, DEFAULT_TEXT_SIZE)
        };
    }

}
