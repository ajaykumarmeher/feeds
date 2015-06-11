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

package com.googlecode.sqb.query.impl;


import com.googlecode.sqb.query.Column;
import com.googlecode.sqb.query.DataType;
import com.googlecode.sqb.query.ForeignKey;
import com.googlecode.sqb.query.Source;

public class ColumnImpl<T> implements Column<T> {

    private final Source source;
    private final String name;
    private final DataType<T> dataType;
    protected final boolean primaryKey;
    private final boolean nullable;
    private final boolean unique;
    protected final ForeignKey foreignKey;

    public ColumnImpl(Source source, String name, DataType<T> dataType, boolean primaryKey, boolean nullable, boolean unique, ForeignKey foreignKey) {
        this.source = source;
        this.name = name;
        this.dataType = dataType;
        this.primaryKey = primaryKey;
        this.nullable = nullable;
        this.unique = unique;
        this.foreignKey = foreignKey;
    }

    @Override
    public Source getSource() {
        return source;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getPreferredAlias() {
        return name;
    }

    @Override
    public DataType<T> getDataType() {
        return dataType;
    }

    @Override
    public boolean isPrimaryKey() {
        return primaryKey;
    }

    @Override
    public boolean isNullable() {
        return nullable;
    }

    @Override
    public boolean isUnique() {
        return unique;
    }

    public ForeignKey getForeignKey() {
        return foreignKey;
    }

    @Override
    public String toString() {
        return name;
    }
}
