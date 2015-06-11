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

package org.fourthline.android.feeds.database;

import android.content.ContentValues;
import android.database.Cursor;
import com.googlecode.sqb.query.Column;

public interface ValueAdapter<T> {

    public static final String PREFIX_SEPARATOR = "_";

    // No T parameter on Column, we want coercion if possible (e.g. getting a date as long)!
    public T readValue(Cursor cursor, Column column, int index);

    public T readValue(Cursor cursor, Column column, String prefix);

    public void writeValue(ContentValues contentValues, Column column, T value);

    public int getIndex(Cursor cursor, Column column, String prefix);

    public int getIndex(Cursor cursor, Column column);

    public boolean isNull(Cursor cursor, int index);

    public boolean isNull(Cursor cursor, Column column);

    public boolean isNull(Cursor cursor, Column column, String prefix);

    public static abstract class AbstractValueAdapter<T> implements ValueAdapter<T> {

        @Override
        public T readValue(Cursor cursor, Column column, String prefix) {
            return readValue(cursor, column, getIndex(cursor, column, prefix));
        }

        @Override
        public int getIndex(Cursor cursor, Column column) {
            return getIndex(cursor, column, "");
        }

        @Override
        public int getIndex(Cursor cursor, Column column, String prefix) {
            String separatedPrefix = (prefix != null && prefix.length() > 0 ? prefix + PREFIX_SEPARATOR : "");
            int index = cursor.getColumnIndex(separatedPrefix + column.getName());
            if (index == -1) {
                StringBuilder sb = new StringBuilder();
                sb.append("Cursor has column names: ");
                for (String s : cursor.getColumnNames()) {
                    sb.append(s).append(" ");
                }
                throw new IllegalArgumentException(
                   "Can't find cursor index for column '" + column + "' with prefix: '" + prefix + "', " + sb
                );
            }
            return index;
        }

        @Override
        public boolean isNull(Cursor cursor, int index) {
            return cursor.isNull(index);
        }

        @Override
        public boolean isNull(Cursor cursor, Column column) {
            return cursor.isNull(getIndex(cursor, column));
        }

        @Override
        public boolean isNull(Cursor cursor, Column column, String prefix) {
            return cursor.isNull(getIndex(cursor, column, prefix));
        }
    }
}
