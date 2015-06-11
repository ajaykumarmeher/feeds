/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.googlecode.sqb.query;

import android.provider.BaseColumns;
import com.googlecode.sqb.query.impl.ColumnImpl;
import com.googlecode.sqb.query.impl.TableImpl;
import com.googlecode.sqb.util.Sql;

public class References {

    private References() {
        // private
    }

    public static Table table(String name) {
        return new TableImpl(Sql.escape(name));
    }

    public static Column<Long> columnPK(Source source) {
        return column(source, BaseColumns._ID, DataType.LONG);
    }

    public static Column<Long> columnPK(Source source, ForeignKey fk) {
        return column(source, BaseColumns._ID, DataType.LONG, fk);
    }

    public static Column<String> column(Source source, String name) {
        return column(source, name, DataType.STRING);
    }

    public static <T> Column<T> column(Source source, String name, DataType<T> dataType) {
        return column(source, name, dataType, false, false);
    }

    public static <T> Column<T> column(Source source, String name, DataType<T> dataType, ForeignKey fk) {
        return new ColumnImpl(source, Sql.escape(name), dataType, false, false, false, fk);
    }

    public static <T> Column<T> column(Source source, String name, DataType<T> dataType, boolean primaryKey) {
        return new ColumnImpl(source, Sql.escape(name), dataType, primaryKey, false, false, null);
    }

    public static <T> Column<T> column(Source source, String name, DataType<T> dataType, boolean primaryKey, ForeignKey fk) {
        return new ColumnImpl(source, Sql.escape(name), dataType, primaryKey, false, false, fk);
    }

    public static <T> Column<T> column(Source source, String name, DataType<T> dataType, boolean nullable, boolean unique) {
        return new ColumnImpl(source, Sql.escape(name), dataType, false, nullable, unique, null);
    }

    public static <T> Column<T> column(Source source, String name, DataType<T> dataType, boolean nullable, boolean unique, ForeignKey fk) {
        return new ColumnImpl(source, Sql.escape(name), dataType, false, nullable, unique, fk);
    }

    public static <T> Column<T> column(Source source, String name, DataType<T> dataType, boolean primaryKey, boolean nullable, boolean unique, ForeignKey fk) {
        return new ColumnImpl(source, Sql.escape(name), dataType, primaryKey, nullable, unique, fk);
    }

}
