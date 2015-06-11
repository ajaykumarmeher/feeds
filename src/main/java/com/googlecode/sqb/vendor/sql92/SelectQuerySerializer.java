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

import com.googlecode.sqb.query.FromClause;
import com.googlecode.sqb.query.Join;
import com.googlecode.sqb.query.OrderBy;
import com.googlecode.sqb.query.OrderByClause;
import com.googlecode.sqb.query.Projection;
import com.googlecode.sqb.query.Restriction;
import com.googlecode.sqb.query.SelectClause;
import com.googlecode.sqb.query.Source;
import com.googlecode.sqb.query.WhereClause;
import com.googlecode.sqb.query.select.SelectQuery;
import com.googlecode.sqb.vendor.Context;
import com.googlecode.sqb.vendor.QuerySerializer;

class SelectQuerySerializer implements QuerySerializer<SelectQuery> {

    @Override
    public String serialize(SelectQuery query, Context context) {
        final StringBuilder sb = new StringBuilder();

        sb.append(serializeSelect(query.getSelectClause(), context));
        sb.append(serializeFrom(query.getFromClause(), context));
        sb.append(serializeWhere(query.getWhereClause(), context));
        sb.append(serializeOrderBy(query.getOrderByClause(), context));

        return sb.toString();
    }


    private String serializeSelect(SelectClause selectClause, Context context) {
        final StringBuilder sb = new StringBuilder();
        sb.append(SELECT).append(SPACE);
        int count = 0;
        for (Projection p : selectClause.getProjections()) {
            if (count++ > 0) {
                sb.append(COMMA).append(SPACE);
            }
            sb.append(context.serialize(p));
            if (!sb.toString().endsWith("*")) {
                final String alias = context.getAlias(p);
                if (null != alias) {
                    sb.append(SPACE).append(ALIAS).append(SPACE).append(alias);
                }
            }
        }
        return sb.toString();
    }

    private String serializeFrom(FromClause fromClause, Context context) {
        final StringBuilder sb = new StringBuilder();
        sb.append(SPACE).append(FROM).append(SPACE);
        final Source source = fromClause.getSource();
        sb.append(context.serialize(source));
        final String alias = context.getAlias(source);
        if (null != alias) {
            sb.append(SPACE).append(ALIAS).append(SPACE).append(alias).append(SPACE);
        }
        for (Join join : fromClause.getJoins()) {
            sb.append(context.serialize(join)).append(SPACE);
        }
        return sb.toString();
    }

    private String serializeWhere(WhereClause whereClause, Context context) {
        final StringBuilder sb = new StringBuilder();
        final Restriction restriction = whereClause.getRestriction();
        if (null != restriction) {
            sb.append(WHERE).append(SPACE);
            sb.append(context.serialize(restriction));
        }
        return sb.toString();
    }

    private String serializeOrderBy(OrderByClause orderByClause, Context context) {
        final StringBuilder sb = new StringBuilder();
        if (orderByClause.getOrderBys().size() > 0) {
            sb.append(SPACE).append(ORDER_BY).append(SPACE);

            for (OrderBy orderBy : orderByClause.getOrderBys()) {
                sb.append(context.serialize(orderBy)).append(COMMA);
            }
            sb.deleteCharAt(sb.length()-1);
        }
        return sb.toString();
    }
}
