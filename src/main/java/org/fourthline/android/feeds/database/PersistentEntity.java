/*
 * Copyright (C) 2011 Teleal GmbH, Switzerland
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

package org.fourthline.android.feeds.database;

import android.content.ContentValues;
import android.content.Entity;
import android.provider.BaseColumns;
import com.googlecode.sqb.query.Column;
import com.googlecode.sqb.query.DataType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class PersistentEntity {

    public static final String MIME_DIR_PREFIX = "vnd.android.cursor.dir";
    public static final String MIME_ITEM_PREFIX = "vnd.android.cursor.item";

    public static final String RESTRICTION_LIMIT_OFFSET = "limit_offset";

    final public Entity INSTANCE;

    protected PersistentEntity(Long id) {
        this(new ContentValues());
        INSTANCE.getEntityValues().put(BaseColumns._ID, id);
    }

    protected PersistentEntity() {
        this(new ContentValues());
    }

    protected PersistentEntity(Entity instance) {
        this(instance.getEntityValues());
    }

    protected PersistentEntity(ContentValues contentValues) {
        INSTANCE = new Entity(new ContentValues());
    }

    public Long getId() {
        return INSTANCE.getEntityValues().getAsLong(BaseColumns._ID);
    }

    public String getIdAsString() {
        Long id = INSTANCE.getEntityValues().getAsLong(BaseColumns._ID);
        return id != null ? Long.toString(id) : null;
    }

    static public String[] getIds(Collection<? extends PersistentEntity> col) {
        List<String> ids = new ArrayList<String>();
        for (PersistentEntity entity : col) {
            ids.add(entity.getIdAsString());
        }
        return ids.toArray(new String[ids.size()]);
    }

    public void setId(Long id) {
        if (id != null && id == -1) {
            throw new IllegalArgumentException("Entity can't have ID value -1, failed database insert?");
        }
        INSTANCE.getEntityValues().put(BaseColumns._ID, id);
    }

    public static String getEntityName(Class clazz) {
        return clazz.getSimpleName().toLowerCase();
    }

    public static String getTableName(Class clazz) {
        return clazz.getSimpleName().toUpperCase();
    }

    public <T> T getValue(Column<T> column) {
        if (column.getDataType() instanceof DataType.Enum) {
            throw new IllegalArgumentException("Can't instantiate enum, use getValueAsString()");
        }
        return (T)INSTANCE.getEntityValues().get(column.getName());
    }

    public String getValueAsString(Column column) {
        return INSTANCE.getEntityValues().getAsString(column.getName());
    }

    @Override
    public String toString() {
        return INSTANCE.toString();
    }

}
