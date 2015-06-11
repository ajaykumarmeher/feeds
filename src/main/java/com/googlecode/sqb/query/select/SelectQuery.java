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
import com.googlecode.sqb.query.Expression;
import com.googlecode.sqb.query.FromClause;
import com.googlecode.sqb.query.OrderByClause;
import com.googlecode.sqb.query.Query;
import com.googlecode.sqb.query.SelectClause;
import com.googlecode.sqb.query.Source;
import com.googlecode.sqb.query.WhereClause;

import java.util.Map;

public interface SelectQuery extends Expression, Query, Source {

    public Map<Aliasable, String> getAliases();

    public SelectClause getSelectClause();

    public FromClause getFromClause();

    public WhereClause getWhereClause();

    public OrderByClause getOrderByClause();
}
