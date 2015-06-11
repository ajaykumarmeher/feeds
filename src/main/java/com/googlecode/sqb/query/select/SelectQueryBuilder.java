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

package com.googlecode.sqb.query.select;

import com.googlecode.sqb.Aliasable;
import com.googlecode.sqb.query.Column;
import com.googlecode.sqb.query.FromClause;
import com.googlecode.sqb.query.Join;
import com.googlecode.sqb.query.OrderByClause;
import com.googlecode.sqb.query.Projection;
import com.googlecode.sqb.query.Restriction;
import com.googlecode.sqb.query.SelectClause;
import com.googlecode.sqb.query.Source;
import com.googlecode.sqb.query.StringProjection;
import org.fourthline.android.feeds.database.ValueAdapter;
import com.googlecode.sqb.query.WhereClause;
import com.googlecode.sqb.query.impl.FromClauseImpl;
import com.googlecode.sqb.query.impl.JoinImpl;
import com.googlecode.sqb.query.impl.OrderByClauseImpl;
import com.googlecode.sqb.query.impl.OrderByImpl;
import com.googlecode.sqb.query.impl.SelectClauseImpl;
import com.googlecode.sqb.query.impl.WhereClauseImpl;
import com.googlecode.sqb.sql.JoinType;
import com.googlecode.sqb.sql.Order;
import com.googlecode.sqb.sql.SelectQuantifier;
import com.googlecode.sqb.util.Sql;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SelectQueryBuilder implements SelectQuery {

    private final SelectClauseImpl selectClause = new SelectClauseImpl();
    private final FromClauseImpl fromClause = new FromClauseImpl();
    private final WhereClauseImpl whereClause = new WhereClauseImpl();
    private final OrderByClauseImpl orderByClause = new OrderByClauseImpl();

    private final Map<Aliasable, String> aliases = new HashMap<Aliasable, String>();
    private final Map<Aliasable, String> aliasesView = Collections.unmodifiableMap(aliases);
    private int currentAliasIndex = 0;

    @Override
    public String getPreferredAlias() {
        return "q";
    }

    @Override
    public Map<Aliasable, String> getAliases() {
        return aliasesView;
    }

    @Override
    public void setAlias(Aliasable a, String alias) {
        aliases.put(a, Sql.escape(alias));
    }

    @Override
    public void setAliasPrefix(Aliasable[] aliasables, String prefix) {
        for (Aliasable aliasable : aliasables) {
            setAlias(aliasable, prefix + ValueAdapter.PREFIX_SEPARATOR + aliasable.getPreferredAlias());
        }
    }

    public SelectClause getSelectClause() {
        return selectClause;
    }

    public FromClause getFromClause() {
        return fromClause;
    }

    public WhereClause getWhereClause() {
        return whereClause;
    }

    public OrderByClause getOrderByClause() {
        return orderByClause;
    }

    private String addAlias(Aliasable a) {
        final String currentAlias = aliases.get(a);
        if (null != currentAlias) {
            return currentAlias;
        }

        final Collection<String> values = aliases.values();
        final String preferred = Sql.escape(a.getPreferredAlias());

        final String alias = !values.contains(preferred) ? preferred : preferred + Integer.toString(currentAliasIndex++);
        aliases.put(a, alias);
        return alias;
    }

    public SelectQueryBuilder select(Projection[] projections) {
        for (Projection projection : projections) {
            addAlias(projection);
            selectClause.addProjection(projection);
        }
        return this;
    }

    public SelectQueryBuilder select(Projection projection) {
        addAlias(projection);
        selectClause.addProjection(projection);
        return this;
    }

    public SelectQueryBuilder select(String expression) {
        final Projection projection = new StringProjection(expression);
        addAlias(projection);
        selectClause.addProjection(projection);
        return this;
    }

    public SelectQueryBuilder distinct() {
        selectClause.setQuantifier(SelectQuantifier.DISTINCT);
        return this;
    }

    public SelectQueryBuilder from(Source source) {
        addAlias(source);
        fromClause.setSource(source);
        return this;
    }

    public SelectQueryBuilder join(Source source) {
        return join(source, JoinType.CROSS, null);
    }

    public SelectQueryBuilder join(Source source, JoinType type) {
        return join(source, type, null);
    }

    public SelectQueryBuilder join(Source source, JoinType type, Restriction restriction) {
        final Join join = new JoinImpl(source, type, restriction);
        final String alias = addAlias(join);
        if (!aliases.containsKey(source)) {
            setAlias(source, alias);
        }
        fromClause.addJoin(join);
        return this;
    }

    public SelectQueryBuilder where(Restriction restriction) {
        whereClause.addRestriction(restriction);
        return this;
    }

    public SelectQueryBuilder orderBy(Column column) {
        return orderBy(column, Order.ASC);
    }

    public SelectQueryBuilder orderBy(Column column, Order order) {
        orderByClause.addOrderBy(new OrderByImpl(column, order));
        return this;
    }

}
