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

package com.googlecode.sqb.query;

import android.content.ContentValues;
import android.database.Cursor;
import org.fourthline.android.feeds.database.ValueAdapter;

public abstract class DataType<T> extends ValueAdapter.AbstractValueAdapter<T> {

    public enum SQLiteType {
        string,
        integer,
        real
    }

    final public String sqlType;

    public DataType(String sqlType) {
        this.sqlType = sqlType;
    }

    public String getSqlType() {
        return sqlType;
    }

    public static <T> T read(Cursor cursor, Column<T> column, String prefix) {
        return column.getDataType().isNull(cursor, column, prefix)
           ? null
           : column.getDataType().readValue(cursor, column, prefix);
    }

    public static <T> T read(Cursor cursor, Column<T> column) {
        return column.getDataType().isNull(cursor, column, "")
           ? null
           : column.getDataType().readValue(cursor, column, "");
    }

    public static <T> T read(Cursor cursor, Column<T> column, int index) {
        return column.getDataType().isNull(cursor, index)
           ? null
           : column.getDataType().readValue(cursor, column, index);
    }

    public static <T> void write(ContentValues contentValues, Column<T> column, T value) {
        column.getDataType().writeValue(contentValues, column, value);
    }

    public static void writeAll(ContentValues contentValues, Column[] columns, Object... values) {
        for (int i = 0; i < columns.length; i++) {
            if (values[i] == null) continue;
            Column column = columns[i];
            write(contentValues, column, values[i]);
        }
    }

    public static final DataType<String> STRING =
       new DataType<String>(SQLiteType.string.name()) {

           @Override
           public String readValue(Cursor cursor, Column column, int index) {
               return cursor.getString(index);
           }

           @Override
           public void writeValue(ContentValues contentValues, Column column, String value) {
               contentValues.put(column.getName(), value);
           }
       };

    public static final DataType<Long> LONG =
       new DataType<Long>(SQLiteType.integer.name()) {

           @Override
           public Long readValue(Cursor cursor, Column column, int index) {
               return cursor.getLong(index);
           }

           @Override
           public void writeValue(ContentValues contentValues, Column column, Long value) {
               contentValues.put(column.getName(), value);
           }
       };

    public static final DataType<Integer> INTEGER =
       new DataType<Integer>(SQLiteType.integer.name()) {

           @Override
           public Integer readValue(Cursor cursor, Column column, int index) {
               return cursor.getInt(index);
           }

           @Override
           public void writeValue(ContentValues contentValues, Column column, Integer value) {
               contentValues.put(column.getName(), value);
           }
       };

    public static final DataType<Boolean> BOOLEAN =
       new DataType<Boolean>(SQLiteType.integer.name()) {

           @Override
           public Boolean readValue(Cursor cursor, Column column, int index) {
               return (cursor.getInt(index) == 1);
           }

           @Override
           public void writeValue(ContentValues contentValues, Column column, Boolean value) {
               contentValues.put(column.getName(), value);
           }
       };

    public static class Enum<T extends java.lang.Enum> extends DataType<T> {

        public final Class<T> enumType;

        public Enum(Class<T> enumType) {
            super(SQLiteType.string.name());
            this.enumType = enumType;
        }

        @Override
        public T readValue(Cursor cursor, Column column, int index) {
            return (T) java.lang.Enum.valueOf(enumType, cursor.getString(index));
        }

        @Override
        public void writeValue(ContentValues contentValues, Column column, T value) {
            contentValues.put(column.getName(), value.name());
        }
    }
}