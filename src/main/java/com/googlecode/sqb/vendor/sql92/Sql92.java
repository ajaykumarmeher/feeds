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

package com.googlecode.sqb.vendor.sql92;

import com.googlecode.sqb.Aliasable;
import com.googlecode.sqb.query.BinaryRestriction;
import com.googlecode.sqb.query.Column;
import com.googlecode.sqb.query.Expression;
import com.googlecode.sqb.query.Join;
import com.googlecode.sqb.query.MultiRestriction;
import com.googlecode.sqb.query.OrderBy;
import com.googlecode.sqb.query.Projection;
import com.googlecode.sqb.query.Restriction;
import com.googlecode.sqb.query.Table;
import com.googlecode.sqb.query.select.SelectQuery;
import com.googlecode.sqb.vendor.Context;
import com.googlecode.sqb.vendor.Serializer;
import com.googlecode.sqb.vendor.Vendor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Sql92 implements Vendor {

    private final SelectQuerySerializer selectQuerySerializer;
    private final Context defaultContext;

    public Sql92() {
        final Map<Class<?>, Serializer> serializers = new HashMap<Class<?>, Serializer>();
        serializers.put(Column.class, new ColumnSerializer());
        serializers.put(Table.class, new TableSerializer());
        serializers.put(Projection.class, new ProjectionSerializer());
        serializers.put(Join.class, new JoinSerializer());
        serializers.put(Restriction.class, new RestrictionSerializer());
        serializers.put(BinaryRestriction.class, new BinaryRestrictionSerializer());
        serializers.put(MultiRestriction.class, new MultiRestrictionSerializer());
        serializers.put(SelectQuery.class, new InnerSelectQuerySerializer());
        serializers.put(Expression.class, new ExpressionSerializer());
        serializers.put(OrderBy.class, new OrderBySerializer());

        selectQuerySerializer = new SelectQuerySerializer();
        defaultContext = new UpdatableContext(serializers, Collections.<Aliasable, String>emptyMap());
    }

    @Override
    public String serialize(SelectQuery query) {
        final Context context = defaultContext.clone(query.getAliases());
        return selectQuerySerializer.serialize(query, context);
    }
}
