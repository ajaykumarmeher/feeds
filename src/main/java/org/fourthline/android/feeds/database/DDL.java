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

import android.provider.BaseColumns;
import com.googlecode.sqb.query.Column;
import com.googlecode.sqb.query.ForeignKey;
import com.googlecode.sqb.query.Table;

public class DDL {

    public static final String SPACE = " ";

    public static String createTable(Table table, Column[] columns) {
        StringBuilder sb = new StringBuilder();
        sb.append("create table ").append(table.getName()).append("(");
        for (Column column : columns) {

            sb.append(column.getName()).append(SPACE);

            sb.append(column.getDataType().getSqlType()).append(SPACE);

            if (column.getName().equals(BaseColumns._ID)) {
                sb.append("primary key autoincrement");
            } else if (column.isPrimaryKey()) {
                sb.append("primary key");
            } else if (!column.isNullable()) {
                sb.append("not null");
            }

            if (column.isUnique()) {
                sb.append(SPACE).append("unique");
            }

            ForeignKey fk;
            if ((fk = column.getForeignKey()) != null) {
                sb.append(SPACE).append("references").append(SPACE);
                sb.append(fk.getReferences().getSource().toString())
                   .append("(").append(fk.getReferences().getName()).append(")");
                if (fk.getOnDelete() != null)
                    sb.append(SPACE).append("on delete").append(SPACE).append(fk.getOnDelete().sql);
                if (fk.getOnUpdate() != null)
                    sb.append(SPACE).append("on update").append(SPACE).append(fk.getOnUpdate().sql);
            }

            sb.append(",");
        }
        if (columns.length > 0)
            sb.deleteCharAt(sb.length()-1);
        sb.append(")");
        return sb.toString();
    }
}
